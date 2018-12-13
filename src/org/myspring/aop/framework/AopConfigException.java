package org.myspring.aop.framework;

import org.myspring.core.NestedRuntimeException;

public class AopConfigException  extends NestedRuntimeException {

    public AopConfigException(String msg) {
        super(msg);
    }

    public AopConfigException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
