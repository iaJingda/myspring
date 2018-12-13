package org.myspring.aop.aopalliance.intercept;

import java.lang.reflect.AccessibleObject;

public interface Joinpoint {

    Object proceed() throws Throwable;

    /**
     * Return the object that holds the current joinpoint's static part.
     * <p>For instance, the target object for an invocation.
     * @return the object (can be null if the accessible object is static)
     */
    Object getThis();

    /**
     * Return the static part of this joinpoint.
     * <p>The static part is an accessible object on which a chain of
     * interceptors are installed.
     */
    AccessibleObject getStaticPart();
}
