package org.myspring.aop;

import java.io.Serializable;
import java.lang.reflect.Method;

class TrueMethodMatcher implements MethodMatcher, Serializable {
    public static final TrueMethodMatcher INSTANCE = new TrueMethodMatcher();


    /**
     * Enforce Singleton pattern.
     */
    private TrueMethodMatcher() {
    }


    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return true;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass, Object... args) {
        // Should never be invoked as isRuntime returns false.
        throw new UnsupportedOperationException();
    }


    @Override
    public String toString() {
        return "MethodMatcher.TRUE";
    }

    /**
     * Required to support serialization. Replaces with canonical
     * instance on deserialization, protecting Singleton pattern.
     * Alternative to overriding {@code equals()}.
     */
    private Object readResolve() {
        return INSTANCE;
    }
}
