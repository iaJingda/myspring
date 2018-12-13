package org.myspring.beans.factory.support;

import org.myspring.beans.FatalBeanException;

public class BeanDefinitionValidationException extends FatalBeanException {

    public BeanDefinitionValidationException(String msg) {
        super(msg);
    }

    public BeanDefinitionValidationException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
