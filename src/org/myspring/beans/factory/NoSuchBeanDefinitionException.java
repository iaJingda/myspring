package org.myspring.beans.factory;

import org.myspring.beans.BeansException;
import org.myspring.core.ResolvableType;

public class NoSuchBeanDefinitionException extends BeansException {
    private String beanName;

    private ResolvableType resolvableType;

    public NoSuchBeanDefinitionException(String name) {
        super("No bean named '" + name + "' available");
        this.beanName = name;
    }

    public NoSuchBeanDefinitionException(String name, String message) {
        super("No bean named '" + name + "' available: " + message);
        this.beanName = name;
    }

    public NoSuchBeanDefinitionException(Class<?> type) {

        this(ResolvableType.forClass(type));
    }
}
