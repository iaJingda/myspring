package org.myspring.expression.spel.support;

import org.myspring.core.MethodParameter;
import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.util.ReflectionUtils;
import org.myspring.expression.AccessException;
import org.myspring.expression.EvaluationContext;
import org.myspring.expression.MethodExecutor;
import org.myspring.expression.TypedValue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectiveMethodExecutor implements MethodExecutor {
    private final Method method;

    private final Integer varargsPosition;

    private boolean computedPublicDeclaringClass = false;

    private Class<?> publicDeclaringClass;

    private boolean argumentConversionOccurred = false;

    public ReflectiveMethodExecutor(Method method) {
        this.method = method;
        if (method.isVarArgs()) {
            Class<?>[] paramTypes = method.getParameterTypes();
            this.varargsPosition = paramTypes.length - 1;
        }
        else {
            this.varargsPosition = null;
        }
    }

    public Method getMethod() {
        return this.method;
    }


    public Class<?> getPublicDeclaringClass() {
        if (!this.computedPublicDeclaringClass) {
            this.publicDeclaringClass = discoverPublicClass(this.method, this.method.getDeclaringClass());
            this.computedPublicDeclaringClass = true;
        }
        return this.publicDeclaringClass;
    }

    private Class<?> discoverPublicClass(Method method, Class<?> clazz) {
        if (Modifier.isPublic(clazz.getModifiers())) {
            try {
                clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
                return clazz;
            }
            catch (NoSuchMethodException ex) {
                // Continue below...
            }
        }
        Class<?>[] ifcs = clazz.getInterfaces();
        for (Class<?> ifc: ifcs) {
            discoverPublicClass(method, ifc);
        }
        if (clazz.getSuperclass() != null) {
            return discoverPublicClass(method, clazz.getSuperclass());
        }
        return null;
    }

    public boolean didArgumentConversionOccur() {
        return this.argumentConversionOccurred;
    }

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        try {
            if (arguments != null) {
                this.argumentConversionOccurred = ReflectionHelper.convertArguments(
                        context.getTypeConverter(), arguments, this.method, this.varargsPosition);
                if (this.method.isVarArgs()) {
                    arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(
                            this.method.getParameterTypes(), arguments);
                }
            }
            ReflectionUtils.makeAccessible(this.method);
            Object value = this.method.invoke(target, arguments);
            return new TypedValue(value, new TypeDescriptor(new MethodParameter(this.method, -1)).narrow(value));
        }
        catch (Exception ex) {
            throw new AccessException("Problem invoking method: " + this.method, ex);
        }
    }
}
