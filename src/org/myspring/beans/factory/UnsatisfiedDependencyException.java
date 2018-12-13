package org.myspring.beans.factory;

import org.myspring.beans.BeansException;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.StringUtils;

public class UnsatisfiedDependencyException  extends BeanCreationException  {

    private InjectionPoint injectionPoint;


    public UnsatisfiedDependencyException(
            String resourceDescription, String beanName, String propertyName, String msg) {

        super(resourceDescription, beanName,
                "Unsatisfied dependency expressed through bean property '" + propertyName + "'" +
                        (StringUtils.hasLength(msg) ? ": " + msg : ""));
    }

    /**
     * Create a new UnsatisfiedDependencyException.
     * @param resourceDescription description of the resource that the bean definition came from
     * @param beanName the name of the bean requested
     * @param propertyName the name of the bean property that couldn't be satisfied
     * @param ex the bean creation exception that indicated the unsatisfied dependency
     */
    public UnsatisfiedDependencyException(
            String resourceDescription, String beanName, String propertyName, BeansException ex) {

        this(resourceDescription, beanName, propertyName, "");
        initCause(ex);
    }

    /**
     * Create a new UnsatisfiedDependencyException.
     * @param resourceDescription description of the resource that the bean definition came from
     * @param beanName the name of the bean requested
     * @param injectionPoint the injection point (field or method/constructor parameter)
     * @param msg the detail message
     * @since 4.3
     */
    public UnsatisfiedDependencyException(
            String resourceDescription, String beanName, InjectionPoint injectionPoint, String msg) {

        super(resourceDescription, beanName,
                "Unsatisfied dependency expressed through " + injectionPoint +
                        (StringUtils.hasLength(msg) ? ": " + msg : ""));
        this.injectionPoint = injectionPoint;
    }

    /**
     * Create a new UnsatisfiedDependencyException.
     * @param resourceDescription description of the resource that the bean definition came from
     * @param beanName the name of the bean requested
     * @param injectionPoint the injection point (field or method/constructor parameter)
     * @param ex the bean creation exception that indicated the unsatisfied dependency
     * @since 4.3
     */
    public UnsatisfiedDependencyException(
            String resourceDescription, String beanName, InjectionPoint injectionPoint, BeansException ex) {

        this(resourceDescription, beanName, injectionPoint, "");
        initCause(ex);
    }

    /**
     * Create a new UnsatisfiedDependencyException.
     * @param resourceDescription description of the resource that the bean definition came from
     * @param beanName the name of the bean requested
     * @param ctorArgIndex the index of the constructor argument that couldn't be satisfied
     * @param ctorArgType the type of the constructor argument that couldn't be satisfied
     * @param msg the detail message
     * @deprecated in favor of {@link #UnsatisfiedDependencyException(String, String, InjectionPoint, String)}
     */
    @Deprecated
    public UnsatisfiedDependencyException(
            String resourceDescription, String beanName, int ctorArgIndex, Class<?> ctorArgType, String msg) {

        super(resourceDescription, beanName,
                "Unsatisfied dependency expressed through constructor argument with index " +
                        ctorArgIndex + " of type [" + ClassUtils.getQualifiedName(ctorArgType) + "]" +
                        (msg != null ? ": " + msg : ""));
    }


    @Deprecated
    public UnsatisfiedDependencyException(
            String resourceDescription, String beanName, int ctorArgIndex, Class<?> ctorArgType, BeansException ex) {

        this(resourceDescription, beanName, ctorArgIndex, ctorArgType, (ex != null ? ex.getMessage() : ""));
        initCause(ex);
    }


    /**
     * Return the injection point (field or method/constructor parameter), if known.
     * @since 4.3
     */
    public InjectionPoint getInjectionPoint() {
        return this.injectionPoint;
    }

}
