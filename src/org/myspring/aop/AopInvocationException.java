package org.myspring.aop;

import org.myspring.core.NestedRuntimeException;

public class AopInvocationException extends NestedRuntimeException {

    public AopInvocationException(String msg) {
        super(msg);
    }

    public AopInvocationException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
