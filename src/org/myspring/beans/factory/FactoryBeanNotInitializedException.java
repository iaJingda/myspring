package org.myspring.beans.factory;

import org.myspring.beans.FatalBeanException;

public class FactoryBeanNotInitializedException extends FatalBeanException {

    public FactoryBeanNotInitializedException() {
        super("FactoryBean is not fully initialized yet");
    }

    public FactoryBeanNotInitializedException(String msg) {
        super(msg);
    }

}
