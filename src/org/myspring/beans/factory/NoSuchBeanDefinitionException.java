package org.myspring.beans.factory;

import org.myspring.beans.BeansException;
import org.myspring.core.ResolvableType;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.StringUtils;

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

    public NoSuchBeanDefinitionException(Class<?> type, String message) {
        this(ResolvableType.forClass(type), message);
    }

    public NoSuchBeanDefinitionException(ResolvableType type) {
        super("No qualifying bean of type '" + type + "' available");
        this.resolvableType = type;
    }

    public NoSuchBeanDefinitionException(ResolvableType type, String message) {
        super("No qualifying bean of type '" + type + "' available: " + message);
        this.resolvableType = type;
    }

    @Deprecated
    public NoSuchBeanDefinitionException(Class<?> type, String dependencyDescription, String message) {
        super("No qualifying bean" + (!StringUtils.hasLength(dependencyDescription) ?
                " of type '" + ClassUtils.getQualifiedName(type) + "'" : "") + " found for dependency" +
                (StringUtils.hasLength(dependencyDescription) ? " [" + dependencyDescription + "]" : "") +
                ": " + message);
        this.resolvableType = ResolvableType.forClass(type);
    }

    public String getBeanName() {
        return this.beanName;
    }


    public Class<?> getBeanType() {
        return (this.resolvableType != null ? this.resolvableType.resolve() : null);
    }


    public ResolvableType getResolvableType() {
        return this.resolvableType;
    }


    public int getNumberOfBeansFound() {
        return 0;
    }
}
