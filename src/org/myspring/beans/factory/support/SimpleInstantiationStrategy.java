package org.myspring.beans.factory.support;

import org.myspring.beans.BeanInstantiationException;
import org.myspring.beans.BeanUtils;
import org.myspring.beans.factory.BeanFactory;
import org.myspring.beans.factory.config.ConfigurableBeanFactory;
import org.myspring.core.util.ReflectionUtils;
import org.myspring.core.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

public class SimpleInstantiationStrategy implements InstantiationStrategy  {

    private static final ThreadLocal<Method> currentlyInvokedFactoryMethod = new ThreadLocal<Method>();

    public static Method getCurrentlyInvokedFactoryMethod() {
        return currentlyInvokedFactoryMethod.get();
    }


    @Override
    public Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner) {
        // Don't override the class with CGLIB if no overrides.
        if (bd.getMethodOverrides().isEmpty()) {
            Constructor<?> constructorToUse;
            synchronized (bd.constructorArgumentLock) {
                constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
                if (constructorToUse == null) {
                    final Class<?> clazz = bd.getBeanClass();
                    if (clazz.isInterface()) {
                        throw new BeanInstantiationException(clazz, "Specified class is an interface");
                    }
                    try {
                        if (System.getSecurityManager() != null) {
                            constructorToUse = AccessController.doPrivileged(new PrivilegedExceptionAction<Constructor<?>>() {
                                @Override
                                public Constructor<?> run() throws Exception {
                                    return clazz.getDeclaredConstructor((Class[]) null);
                                }
                            });
                        }
                        else {
                            constructorToUse =	clazz.getDeclaredConstructor((Class[]) null);
                        }
                        bd.resolvedConstructorOrFactoryMethod = constructorToUse;
                    }
                    catch (Throwable ex) {
                        throw new BeanInstantiationException(clazz, "No default constructor found", ex);
                    }
                }
            }
            return BeanUtils.instantiateClass(constructorToUse);
        }
        else {
            // Must generate CGLIB subclass.
            return instantiateWithMethodInjection(bd, beanName, owner);
        }
    }

    /**
     * Subclasses can override this method, which is implemented to throw
     * UnsupportedOperationException, if they can instantiate an object with
     * the Method Injection specified in the given RootBeanDefinition.
     * Instantiation should use a no-arg constructor.
     */
    protected Object instantiateWithMethodInjection(RootBeanDefinition bd, String beanName, BeanFactory owner) {
        throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner,
                              final Constructor<?> ctor, Object... args) {

        if (bd.getMethodOverrides().isEmpty()) {
            if (System.getSecurityManager() != null) {
                // use own privileged to change accessibility (when security is on)
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        ReflectionUtils.makeAccessible(ctor);
                        return null;
                    }
                });
            }
            return BeanUtils.instantiateClass(ctor, args);
        }
        else {
            return instantiateWithMethodInjection(bd, beanName, owner, ctor, args);
        }
    }

    /**
     * Subclasses can override this method, which is implemented to throw
     * UnsupportedOperationException, if they can instantiate an object with
     * the Method Injection specified in the given RootBeanDefinition.
     * Instantiation should use the given constructor and parameters.
     */
    protected Object instantiateWithMethodInjection(RootBeanDefinition bd, String beanName, BeanFactory owner,
                                                    Constructor<?> ctor, Object... args) {

        throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner,
                              Object factoryBean, final Method factoryMethod, Object... args) {

        try {
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        ReflectionUtils.makeAccessible(factoryMethod);
                        return null;
                    }
                });
            }
            else {
                ReflectionUtils.makeAccessible(factoryMethod);
            }

            Method priorInvokedFactoryMethod = currentlyInvokedFactoryMethod.get();
            try {
                currentlyInvokedFactoryMethod.set(factoryMethod);
                return factoryMethod.invoke(factoryBean, args);
            }
            finally {
                if (priorInvokedFactoryMethod != null) {
                    currentlyInvokedFactoryMethod.set(priorInvokedFactoryMethod);
                }
                else {
                    currentlyInvokedFactoryMethod.remove();
                }
            }
        }
        catch (IllegalArgumentException ex) {
            throw new BeanInstantiationException(factoryMethod,
                    "Illegal arguments to factory method '" + factoryMethod.getName() + "'; " +
                            "args: " + StringUtils.arrayToCommaDelimitedString(args), ex);
        }
        catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(factoryMethod,
                    "Cannot access factory method '" + factoryMethod.getName() + "'; is it public?", ex);
        }
        catch (InvocationTargetException ex) {
            String msg = "Factory method '" + factoryMethod.getName() + "' threw exception";
            if (bd.getFactoryBeanName() != null && owner instanceof ConfigurableBeanFactory &&
                    ((ConfigurableBeanFactory) owner).isCurrentlyInCreation(bd.getFactoryBeanName())) {
                msg = "Circular reference involving containing bean '" + bd.getFactoryBeanName() + "' - consider " +
                        "declaring the factory method as static for independence from its containing instance. " + msg;
            }
            throw new BeanInstantiationException(factoryMethod, msg, ex.getTargetException());
        }
    }

}
