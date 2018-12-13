package org.myspring.beans.factory.config;

import org.myspring.beans.BeansException;
import org.myspring.beans.factory.BeanFactory;
import org.myspring.beans.factory.InjectionPoint;
import org.myspring.beans.factory.NoUniqueBeanDefinitionException;
import org.myspring.core.MethodParameter;
import org.myspring.core.ParameterNameDiscoverer;
import org.myspring.core.ResolvableType;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class DependencyDescriptor extends InjectionPoint implements Serializable {


    private final Class<?> declaringClass;

    private String methodName;

    private Class<?>[] parameterTypes;

    private int parameterIndex;

    private String fieldName;

    private final boolean required;

    private final boolean eager;

    private int nestingLevel = 1;

    private Class<?> containingClass;

    private volatile ResolvableType resolvableType;

    public DependencyDescriptor(MethodParameter methodParameter, boolean required) {
        this(methodParameter, required, true);
    }

    public DependencyDescriptor(MethodParameter methodParameter, boolean required, boolean eager) {
        super(methodParameter);
        this.declaringClass = methodParameter.getDeclaringClass();
        if (this.methodParameter.getMethod() != null) {
            this.methodName = methodParameter.getMethod().getName();
            this.parameterTypes = methodParameter.getMethod().getParameterTypes();
        }
        else {
            this.parameterTypes = methodParameter.getConstructor().getParameterTypes();
        }
        this.parameterIndex = methodParameter.getParameterIndex();
        this.containingClass = methodParameter.getContainingClass();
        this.required = required;
        this.eager = eager;
    }
    public DependencyDescriptor(DependencyDescriptor original) {
        super(original);
        this.declaringClass = original.declaringClass;
        this.methodName = original.methodName;
        this.parameterTypes = original.parameterTypes;
        this.parameterIndex = original.parameterIndex;
        this.fieldName = original.fieldName;
        this.containingClass = original.containingClass;
        this.required = original.required;
        this.eager = original.eager;
        this.nestingLevel = original.nestingLevel;
    }
    public Object resolveNotUnique(Class<?> type, Map<String, Object> matchingBeans) throws BeansException {
        throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
    }
    public Class<?> getDependencyType() {
        if (this.field != null) {
            if (this.nestingLevel > 1) {
                Type type = this.field.getGenericType();
                for (int i = 2; i <= this.nestingLevel; i++) {
                    if (type instanceof ParameterizedType) {
                        Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                        type = args[args.length - 1];
                    }
                }
                if (type instanceof Class) {
                    return (Class<?>) type;
                }
                else if (type instanceof ParameterizedType) {
                    Type arg = ((ParameterizedType) type).getRawType();
                    if (arg instanceof Class) {
                        return (Class<?>) arg;
                    }
                }
                return Object.class;
            }
            else {
                return this.field.getType();
            }
        }
        else {
            return this.methodParameter.getNestedParameterType();
        }
    }

    public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory)
            throws BeansException {

        return beanFactory.getBean(beanName, requiredType);
    }

    public boolean isRequired() {
        return this.required;
    }

    public void increaseNestingLevel() {
        this.nestingLevel++;
        this.resolvableType = null;
        if (this.methodParameter != null) {
            this.methodParameter.increaseNestingLevel();
        }
    }

    public String getDependencyName() {
        return (this.field != null ? this.field.getName() : this.methodParameter.getParameterName());
    }

    public DependencyDescriptor forFallbackMatch() {
        return new DependencyDescriptor(this) {
            @Override
            public boolean fallbackMatchAllowed() {
                return true;
            }
        };
    }
    public boolean fallbackMatchAllowed() {
        return false;
    }
    public ResolvableType getResolvableType() {
        ResolvableType resolvableType = this.resolvableType;
        if (resolvableType == null) {
            resolvableType = (this.field != null ?
                    ResolvableType.forField(this.field, this.nestingLevel, this.containingClass) :
                    ResolvableType.forMethodParameter(this.methodParameter));
            this.resolvableType = resolvableType;
        }
        return resolvableType;
    }

    public Object resolveShortcut(BeanFactory beanFactory) throws BeansException {
        return null;
    }
    public boolean isEager() {
        return this.eager;
    }

    public void initParameterNameDiscovery(ParameterNameDiscoverer parameterNameDiscoverer) {
        if (this.methodParameter != null) {
            this.methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
        }
    }
}
