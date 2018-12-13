package org.myspring.beans.factory;

import org.myspring.beans.BeansException;
import org.myspring.core.util.ClassUtils;

public class BeanNotOfRequiredTypeException  extends BeansException {

    /** The name of the instance that was of the wrong type */
    private String beanName;

    /** The required type */
    private Class<?> requiredType;

    /** The offending type */
    private Class<?> actualType;


    public BeanNotOfRequiredTypeException(String beanName, Class<?> requiredType, Class<?> actualType) {
        super("Bean named '" + beanName + "' is expected to be of type '" + ClassUtils.getQualifiedName(requiredType) +
                "' but was actually of type '" + ClassUtils.getQualifiedName(actualType) + "'");
        this.beanName = beanName;
        this.requiredType = requiredType;
        this.actualType = actualType;
    }


    /**
     * Return the name of the instance that was of the wrong type.
     */
    public String getBeanName() {
        return this.beanName;
    }

    /**
     * Return the expected type for the bean.
     */
    public Class<?> getRequiredType() {
        return this.requiredType;
    }

    /**
     * Return the actual type of the instance found.
     */
    public Class<?> getActualType() {
        return this.actualType;
    }
}
