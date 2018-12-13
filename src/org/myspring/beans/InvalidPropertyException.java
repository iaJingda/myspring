package org.myspring.beans;

public class InvalidPropertyException extends FatalBeanException  {
    private Class<?> beanClass;

    private String propertyName;

    public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg) {
        this(beanClass, propertyName, msg, null);
    }
    public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
        super("Invalid property '" + propertyName + "' of bean class [" + beanClass.getName() + "]: " + msg, cause);
        this.beanClass = beanClass;
        this.propertyName = propertyName;
    }


    /**
     * Return the offending bean class.
     */
    public Class<?> getBeanClass() {
        return beanClass;
    }

    /**
     * Return the name of the offending property.
     */
    public String getPropertyName() {
        return propertyName;
    }
}
