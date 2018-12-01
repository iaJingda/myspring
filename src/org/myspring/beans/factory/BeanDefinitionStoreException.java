package org.myspring.beans.factory;

import org.myspring.beans.FatalBeanException;

public class BeanDefinitionStoreException extends FatalBeanException {

    private String resourceDescription;

    private String beanName;

    public BeanDefinitionStoreException(String msg) {
        super(msg);
    }

    public BeanDefinitionStoreException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public BeanDefinitionStoreException(String resourceDescription, String msg) {
        super(msg);
        this.resourceDescription = resourceDescription;
    }

    public BeanDefinitionStoreException(String resourceDescription, String msg, Throwable cause) {
        super(msg, cause);
        this.resourceDescription = resourceDescription;
    }

    public BeanDefinitionStoreException(String resourceDescription, String beanName, String msg) {
        this(resourceDescription, beanName, msg, null);
    }

    public BeanDefinitionStoreException(String resourceDescription, String beanName, String msg, Throwable cause) {
        super("Invalid bean definition with name '" + beanName + "' defined in " + resourceDescription + ": " + msg,
                cause);
        this.resourceDescription = resourceDescription;
        this.beanName = beanName;
    }

    public String getResourceDescription() {
        return this.resourceDescription;
    }


    public String getBeanName() {
        return this.beanName;
    }

}
