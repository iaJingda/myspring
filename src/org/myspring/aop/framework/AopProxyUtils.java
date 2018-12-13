package org.myspring.aop.framework;

import org.myspring.aop.SpringProxy;
import org.myspring.aop.TargetClassAware;
import org.myspring.aop.TargetSource;
import org.myspring.aop.support.AopUtils;
import org.myspring.aop.target.SingletonTargetSource;
import org.myspring.core.DecoratingProxy;
import org.myspring.core.util.Assert;
import org.myspring.core.util.ObjectUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public abstract class AopProxyUtils {

    public static Object getSingletonTarget(Object candidate) {
        if (candidate instanceof Advised) {
            TargetSource targetSource = ((Advised) candidate).getTargetSource();
            if (targetSource instanceof SingletonTargetSource) {
                return ((SingletonTargetSource) targetSource).getTarget();
            }
        }
        return null;
    }
    public static Class<?> ultimateTargetClass(Object candidate) {
        Assert.notNull(candidate, "Candidate object must not be null");
        Object current = candidate;
        Class<?> result = null;
        while (current instanceof TargetClassAware) {
            result = ((TargetClassAware) current).getTargetClass();
            current = getSingletonTarget(current);
        }
        if (result == null) {
            result = (AopUtils.isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
        }
        return result;
    }

    public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
        return completeProxiedInterfaces(advised, false);
    }

    static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
        Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
        if (specifiedInterfaces.length == 0) {
            // No user-specified interfaces: check whether target class is an interface.
            Class<?> targetClass = advised.getTargetClass();
            if (targetClass != null) {
                if (targetClass.isInterface()) {
                    advised.setInterfaces(targetClass);
                }
                else if (Proxy.isProxyClass(targetClass)) {
                    advised.setInterfaces(targetClass.getInterfaces());
                }
                specifiedInterfaces = advised.getProxiedInterfaces();
            }
        }
        boolean addSpringProxy = !advised.isInterfaceProxied(SpringProxy.class);
        boolean addAdvised = !advised.isOpaque() && !advised.isInterfaceProxied(Advised.class);
        boolean addDecoratingProxy = (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class));
        int nonUserIfcCount = 0;
        if (addSpringProxy) {
            nonUserIfcCount++;
        }
        if (addAdvised) {
            nonUserIfcCount++;
        }
        if (addDecoratingProxy) {
            nonUserIfcCount++;
        }
        Class<?>[] proxiedInterfaces = new Class<?>[specifiedInterfaces.length + nonUserIfcCount];
        System.arraycopy(specifiedInterfaces, 0, proxiedInterfaces, 0, specifiedInterfaces.length);
        int index = specifiedInterfaces.length;
        if (addSpringProxy) {
            proxiedInterfaces[index] = SpringProxy.class;
            index++;
        }
        if (addAdvised) {
            proxiedInterfaces[index] = Advised.class;
            index++;
        }
        if (addDecoratingProxy) {
            proxiedInterfaces[index] = DecoratingProxy.class;
        }
        return proxiedInterfaces;
    }

    /**
     * Extract the user-specified interfaces that the given proxy implements,
     * i.e. all non-Advised interfaces that the proxy implements.
     * @param proxy the proxy to analyze (usually a JDK dynamic proxy)
     * @return all user-specified interfaces that the proxy implements,
     * in the original order (never {@code null} or empty)
     * @see Advised
     */
    public static Class<?>[] proxiedUserInterfaces(Object proxy) {
        Class<?>[] proxyInterfaces = proxy.getClass().getInterfaces();
        int nonUserIfcCount = 0;
        if (proxy instanceof SpringProxy) {
            nonUserIfcCount++;
        }
        if (proxy instanceof Advised) {
            nonUserIfcCount++;
        }
        if (proxy instanceof DecoratingProxy) {
            nonUserIfcCount++;
        }
        Class<?>[] userInterfaces = new Class<?>[proxyInterfaces.length - nonUserIfcCount];
        System.arraycopy(proxyInterfaces, 0, userInterfaces, 0, userInterfaces.length);
        Assert.notEmpty(userInterfaces, "JDK proxy must implement one or more interfaces");
        return userInterfaces;
    }

    /**
     * Check equality of the proxies behind the given AdvisedSupport objects.
     * Not the same as equality of the AdvisedSupport objects:
     * rather, equality of interfaces, advisors and target sources.
     */
    public static boolean equalsInProxy(AdvisedSupport a, AdvisedSupport b) {
        return (a == b ||
                (equalsProxiedInterfaces(a, b) && equalsAdvisors(a, b) && a.getTargetSource().equals(b.getTargetSource())));
    }

    /**
     * Check equality of the proxied interfaces behind the given AdvisedSupport objects.
     */
    public static boolean equalsProxiedInterfaces(AdvisedSupport a, AdvisedSupport b) {
        return Arrays.equals(a.getProxiedInterfaces(), b.getProxiedInterfaces());
    }

    /**
     * Check equality of the advisors behind the given AdvisedSupport objects.
     */
    public static boolean equalsAdvisors(AdvisedSupport a, AdvisedSupport b) {
        return Arrays.equals(a.getAdvisors(), b.getAdvisors());
    }


    /**
     * Adapt the given arguments to the target signature in the given method,
     * if necessary: in particular, if a given vararg argument array does not
     * match the array type of the declared vararg parameter in the method.
     * @param method the target method
     * @param arguments the given arguments
     * @return a cloned argument array, or the original if no adaptation is needed
     * @since 4.2.3
     */
    static Object[] adaptArgumentsIfNecessary(Method method, Object... arguments) {
        if (method.isVarArgs() && !ObjectUtils.isEmpty(arguments)) {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == arguments.length) {
                int varargIndex = paramTypes.length - 1;
                Class<?> varargType = paramTypes[varargIndex];
                if (varargType.isArray()) {
                    Object varargArray = arguments[varargIndex];
                    if (varargArray instanceof Object[] && !varargType.isInstance(varargArray)) {
                        Object[] newArguments = new Object[arguments.length];
                        System.arraycopy(arguments, 0, newArguments, 0, varargIndex);
                        Class<?> targetElementType = varargType.getComponentType();
                        int varargLength = Array.getLength(varargArray);
                        Object newVarargArray = Array.newInstance(targetElementType, varargLength);
                        System.arraycopy(varargArray, 0, newVarargArray, 0, varargLength);
                        newArguments[varargIndex] = newVarargArray;
                        return newArguments;
                    }
                }
            }
        }
        return arguments;
    }

}
