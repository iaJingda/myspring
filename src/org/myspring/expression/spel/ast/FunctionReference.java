package org.myspring.expression.spel.ast;

import org.myspring.core.MethodParameter;
import org.myspring.core.asm.MethodVisitor;
import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.util.Assert;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.ReflectionUtils;
import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypeConverter;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.CodeFlow;
import org.myspring.expression.spel.ExpressionState;
import org.myspring.expression.spel.SpelEvaluationException;
import org.myspring.expression.spel.SpelMessage;
import org.myspring.expression.spel.support.ReflectionHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class FunctionReference extends SpelNodeImpl {

    private final String name;

    // Captures the most recently used method for the function invocation *if* the method
    // can safely be used for compilation (i.e. no argument conversion is going on)
    private volatile Method method;


    public FunctionReference(String functionName, int pos, SpelNodeImpl... arguments) {
        super(pos, arguments);
        this.name = functionName;
    }


    @Override
    public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
        TypedValue value = state.lookupVariable(this.name);
        if (value == TypedValue.NULL) {
            throw new SpelEvaluationException(getStartPosition(), SpelMessage.FUNCTION_NOT_DEFINED, this.name);
        }
        if (!(value.getValue() instanceof Method)) {
            // Possibly a static Java method registered as a function
            throw new SpelEvaluationException(
                    SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, this.name, value.getClass());
        }

        try {
            return executeFunctionJLRMethod(state, (Method) value.getValue());
        }
        catch (SpelEvaluationException ex) {
            ex.setPosition(getStartPosition());
            throw ex;
        }
    }

    /**
     * Execute a function represented as a {@code java.lang.reflect.Method}.
     * @param state the expression evaluation state
     * @param method the method to invoke
     * @return the return value of the invoked Java method
     * @throws EvaluationException if there is any problem invoking the method
     */
    private TypedValue executeFunctionJLRMethod(ExpressionState state, Method method) throws EvaluationException {
        Object[] functionArgs = getArguments(state);

        if (!method.isVarArgs()) {
            int declaredParamCount = method.getParameterTypes().length;
            if (declaredParamCount != functionArgs.length) {
                throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
                        functionArgs.length, declaredParamCount);
            }
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new SpelEvaluationException(getStartPosition(),
                    SpelMessage.FUNCTION_MUST_BE_STATIC, ClassUtils.getQualifiedMethodName(method), this.name);
        }

        // Convert arguments if necessary and remap them for varargs if required
        TypeConverter converter = state.getEvaluationContext().getTypeConverter();
        boolean argumentConversionOccurred = ReflectionHelper.convertAllArguments(converter, functionArgs, method);
        if (method.isVarArgs()) {
            functionArgs = ReflectionHelper.setupArgumentsForVarargsInvocation(
                    method.getParameterTypes(), functionArgs);
        }
        boolean compilable = false;

        try {
            ReflectionUtils.makeAccessible(method);
            Object result = method.invoke(method.getClass(), functionArgs);
            compilable = !argumentConversionOccurred;
            return new TypedValue(result, new TypeDescriptor(new MethodParameter(method, -1)).narrow(result));
        }
        catch (Exception ex) {
            throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
                    this.name, ex.getMessage());
        }
        finally {
            if (compilable) {
                this.exitTypeDescriptor = CodeFlow.toDescriptor(method.getReturnType());
                this.method = method;
            }
            else {
                this.exitTypeDescriptor = null;
                this.method = null;
            }
        }
    }

    @Override
    public String toStringAST() {
        StringBuilder sb = new StringBuilder("#").append(this.name);
        sb.append("(");
        for (int i = 0; i < getChildCount(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(getChild(i).toStringAST());
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Compute the arguments to the function, they are the children of this expression node.
     * @return an array of argument values for the function call
     */
    private Object[] getArguments(ExpressionState state) throws EvaluationException {
        // Compute arguments to the function
        Object[] arguments = new Object[getChildCount()];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = this.children[i].getValueInternal(state).getValue();
        }
        return arguments;
    }

    @Override
    public boolean isCompilable() {
        Method method = this.method;
        if (method == null) {
            return false;
        }
        int methodModifiers = method.getModifiers();
        if (!Modifier.isStatic(methodModifiers) || !Modifier.isPublic(methodModifiers) ||
                !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            return false;
        }
        for (SpelNodeImpl child : this.children) {
            if (!child.isCompilable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        Method method = this.method;
        Assert.state(method != null, "No method handle");
        String classDesc = method.getDeclaringClass().getName().replace('.', '/');
        generateCodeForArguments(mv, cf, method, this.children);
        mv.visitMethodInsn(INVOKESTATIC, classDesc, method.getName(),
                CodeFlow.createSignatureDescriptor(method), false);
        cf.pushDescriptor(this.exitTypeDescriptor);
    }

}
