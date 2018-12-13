package org.myspring.aop;

public interface ClassFilter {

    boolean matches(Class<?> clazz);


    /**
     * Canonical instance of a ClassFilter that matches all classes.
     */
    ClassFilter TRUE = TrueClassFilter.INSTANCE;
}
