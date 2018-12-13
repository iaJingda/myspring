package org.myspring.expression.spel.ast;

import org.myspring.core.asm.MethodVisitor;
import org.myspring.core.asm.Opcodes;
import org.myspring.core.util.Assert;
import org.myspring.core.util.ObjectUtils;
import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypedValue;
import org.myspring.expression.common.ExpressionUtils;
import org.myspring.expression.spel.*;
import org.myspring.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public abstract class SpelNodeImpl implements SpelNode, Opcodes {
    private static final SpelNodeImpl[] NO_CHILDREN = new SpelNodeImpl[0];


    protected int pos; // start = top 16bits, end = bottom 16bits

    protected SpelNodeImpl[] children = SpelNodeImpl.NO_CHILDREN;

    private SpelNodeImpl parent;

    protected volatile String exitTypeDescriptor;


    public SpelNodeImpl(int pos, SpelNodeImpl... operands) {
        this.pos = pos;
        // pos combines start and end so can never be zero because tokens cannot be zero length
        Assert.isTrue(pos != 0, "Pos must not be 0");
        if (!ObjectUtils.isEmpty(operands)) {
            this.children = operands;
            for (SpelNodeImpl childNode : operands) {
                childNode.parent = this;
            }
        }
    }

    @Deprecated
    protected SpelNodeImpl getPreviousChild() {
        SpelNodeImpl result = null;
        if (this.parent != null) {
            for (SpelNodeImpl child : this.parent.children) {
                if (this == child) {
                    break;
                }
                result = child;
            }
        }
        return result;
    }
    protected boolean nextChildIs(Class<?>... clazzes) {
        if (this.parent != null) {
            SpelNodeImpl[] peers = this.parent.children;
            for (int i = 0, max = peers.length; i < max; i++) {
                if (this == peers[i]) {
                    if (i + 1 >= max) {
                        return false;
                    }
                    Class<?> clazz = peers[i + 1].getClass();
                    for (Class<?> desiredClazz : clazzes) {
                        if (clazz.equals(desiredClazz)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public final Object getValue(ExpressionState expressionState) throws EvaluationException {
        if (expressionState != null) {
            return getValueInternal(expressionState).getValue();
        }
        else {
            // configuration not set - does that matter?
            return getValue(new ExpressionState(new StandardEvaluationContext()));
        }
    }

    @Override
    public final TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException {
        if (expressionState != null) {
            return getValueInternal(expressionState);
        }
        else {
            // configuration not set - does that matter?
            return getTypedValue(new ExpressionState(new StandardEvaluationContext()));
        }
    }

    @Override
    public boolean isWritable(ExpressionState expressionState) throws EvaluationException {
        return false;
    }

    @Override
    public void setValue(ExpressionState expressionState, Object newValue) throws EvaluationException {
        throw new SpelEvaluationException(getStartPosition(),
                SpelMessage.SETVALUE_NOT_SUPPORTED, getClass());
    }

    @Override
    public SpelNode getChild(int index) {
        return this.children[index];
    }

    @Override
    public int getChildCount() {
        return this.children.length;
    }

    @Override
    public Class<?> getObjectClass(Object obj) {
        if (obj == null) {
            return null;
        }
        return (obj instanceof Class ? ((Class<?>) obj) : obj.getClass());
    }

    protected final <T> T getValue(ExpressionState state, Class<T> desiredReturnType) throws EvaluationException {
        return ExpressionUtils.convertTypedValue(state.getEvaluationContext(), getValueInternal(state), desiredReturnType);
    }

    @Override
    public int getStartPosition() {
        return (this.pos >> 16);
    }

    @Override
    public int getEndPosition() {
        return (this.pos & 0xffff);
    }

    protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
        throw new SpelEvaluationException(this.pos, SpelMessage.NOT_ASSIGNABLE, toStringAST());
    }

    public boolean isCompilable() {
        return false;
    }
    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        throw new IllegalStateException(getClass().getName() +" has no generateCode(..) method");
    }

    public String getExitDescriptor() {
        return this.exitTypeDescriptor;
    }

    public abstract TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException;

    protected static void generateCodeForArguments(MethodVisitor mv, CodeFlow cf, Member member, SpelNodeImpl[] arguments) {
        String[] paramDescriptors = null;
        boolean isVarargs = false;
        if (member instanceof Constructor) {
            Constructor<?> ctor = (Constructor<?>) member;
            paramDescriptors = CodeFlow.toDescriptors(ctor.getParameterTypes());
            isVarargs = ctor.isVarArgs();
        }
        else { // Method
            Method method = (Method)member;
            paramDescriptors = CodeFlow.toDescriptors(method.getParameterTypes());
            isVarargs = method.isVarArgs();
        }
        if (isVarargs) {
            // The final parameter may or may not need packaging into an array, or nothing may
            // have been passed to satisfy the varargs and so something needs to be built.
            int p = 0; // Current supplied argument being processed
            int childCount = arguments.length;

            // Fulfill all the parameter requirements except the last one
            for (p = 0; p < paramDescriptors.length - 1; p++) {
                generateCodeForArgument(mv, cf, arguments[p], paramDescriptors[p]);
            }

            SpelNodeImpl lastChild = (childCount == 0 ? null : arguments[childCount - 1]);
            String arrayType = paramDescriptors[paramDescriptors.length - 1];
            // Determine if the final passed argument is already suitably packaged in array
            // form to be passed to the method
            if (lastChild != null && arrayType.equals(lastChild.getExitDescriptor())) {
                generateCodeForArgument(mv, cf, lastChild, paramDescriptors[p]);
            }
            else {
                arrayType = arrayType.substring(1); // trim the leading '[', may leave other '['
                // build array big enough to hold remaining arguments
                CodeFlow.insertNewArrayCode(mv, childCount - p, arrayType);
                // Package up the remaining arguments into the array
                int arrayindex = 0;
                while (p < childCount) {
                    SpelNodeImpl child = arguments[p];
                    mv.visitInsn(DUP);
                    CodeFlow.insertOptimalLoad(mv, arrayindex++);
                    generateCodeForArgument(mv, cf, child, arrayType);
                    CodeFlow.insertArrayStore(mv, arrayType);
                    p++;
                }
            }
        }
        else {
            for (int i = 0; i < paramDescriptors.length;i++) {
                generateCodeForArgument(mv, cf, arguments[i], paramDescriptors[i]);
            }
        }
    }

    protected static void generateCodeForArgument(MethodVisitor mv, CodeFlow cf, SpelNodeImpl argument, String paramDesc) {
        cf.enterCompilationScope();
        argument.generateCode(mv, cf);
        String lastDesc = cf.lastDescriptor();
        boolean primitiveOnStack = CodeFlow.isPrimitive(lastDesc);
        // Check if need to box it for the method reference?
        if (primitiveOnStack && paramDesc.charAt(0) == 'L') {
            CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
        }
        else if (paramDesc.length() == 1 && !primitiveOnStack) {
            CodeFlow.insertUnboxInsns(mv, paramDesc.charAt(0), lastDesc);
        }
        else if (!paramDesc.equals(lastDesc)) {
            // This would be unnecessary in the case of subtyping (e.g. method takes Number but Integer passed in)
            CodeFlow.insertCheckCast(mv, paramDesc);
        }
        cf.exitCompilationScope();
    }
}
