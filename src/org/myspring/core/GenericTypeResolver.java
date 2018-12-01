package org.myspring.core;

import org.myspring.core.util.Assert;
import org.myspring.core.util.ConcurrentReferenceHashMap;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

public abstract class GenericTypeResolver {

    private static final Map<Class<?>, Map<TypeVariable, Type>> typeVariableCache =
            new ConcurrentReferenceHashMap<Class<?>, Map<TypeVariable, Type>>();

    @Deprecated
    public static Type getTargetType(MethodParameter methodParameter) {
        Assert.notNull(methodParameter, "MethodParameter must not be null");
        return methodParameter.getGenericParameterType();
    }


    public static Class<?> resolveParameterType(MethodParameter methodParameter, Class<?> implementationClass) {
        Assert.notNull(methodParameter, "MethodParameter must not be null");
        Assert.notNull(implementationClass, "Class must not be null");
        methodParameter.setContainingClass(implementationClass);
        ResolvableType.resolveMethodParameter(methodParameter);
        return methodParameter.getParameterType();
    }


    public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
        Assert.notNull(method, "Method must not be null");
        Assert.notNull(clazz, "Class must not be null");
        return ResolvableType.forMethodReturnType(method, clazz).resolve(method.getReturnType());
    }



}
