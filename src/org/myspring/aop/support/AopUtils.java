package org.myspring.aop.support;

import org.myspring.aop.*;
import org.myspring.core.BridgeMethodResolver;
import org.myspring.core.MethodIntrospector;
import org.myspring.core.util.Assert;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class AopUtils {

    public static boolean isAopProxy(Object object) {
        return (object instanceof SpringProxy &&
                (Proxy.isProxyClass(object.getClass()) || ClassUtils.isCglibProxyClass(object.getClass())));
    }

    public static boolean isJdkDynamicProxy(Object object) {
        return (object instanceof SpringProxy && Proxy.isProxyClass(object.getClass()));
    }

    public static boolean isCglibProxy(Object object) {
        return (object instanceof SpringProxy && ClassUtils.isCglibProxy(object));
    }

    public static Class<?> getTargetClass(Object candidate) {
        Assert.notNull(candidate, "Candidate object must not be null");
        Class<?> result = null;
        if (candidate instanceof TargetClassAware) {
            result = ((TargetClassAware) candidate).getTargetClass();
        }
        if (result == null) {
            result = (isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
        }
        return result;
    }
    public static Method selectInvocableMethod(Method method, Class<?> targetType) {
        Method methodToUse = MethodIntrospector.selectInvocableMethod(method, targetType);
        if (Modifier.isPrivate(methodToUse.getModifiers()) && !Modifier.isStatic(methodToUse.getModifiers()) &&
                SpringProxy.class.isAssignableFrom(targetType)) {
            throw new IllegalStateException(String.format(
                    "Need to invoke method '%s' found on proxy for target class '%s' but cannot " +
                            "be delegated to target bean. Switch its visibility to package or protected.",
                    method.getName(), method.getDeclaringClass().getSimpleName()));
        }
        return methodToUse;
    }

    public static boolean isEqualsMethod(Method method) {
        return ReflectionUtils.isEqualsMethod(method);
    }

    /**
     * Determine whether the given method is a "hashCode" method.
     * @see java.lang.Object#hashCode
     */
    public static boolean isHashCodeMethod(Method method) {
        return ReflectionUtils.isHashCodeMethod(method);
    }

    /**
     * Determine whether the given method is a "toString" method.
     * @see java.lang.Object#toString()
     */
    public static boolean isToStringMethod(Method method) {
        return ReflectionUtils.isToStringMethod(method);
    }

    /**
     * Determine whether the given method is a "finalize" method.
     * @see java.lang.Object#finalize()
     */
    public static boolean isFinalizeMethod(Method method) {
        return (method != null && method.getName().equals("finalize") &&
                method.getParameterTypes().length == 0);
    }

    public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {
        Method resolvedMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
        // If we are dealing with method with generic parameters, find the original method.
        return BridgeMethodResolver.findBridgedMethod(resolvedMethod);
    }

    /**
     * Can the given pointcut apply at all on the given class?
     * <p>This is an important test as it can be used to optimize
     * out a pointcut for a class.
     * @param pc the static or dynamic pointcut to check
     * @param targetClass the class to test
     * @return whether the pointcut can apply on any method
     */
    public static boolean canApply(Pointcut pc, Class<?> targetClass) {
        return canApply(pc, targetClass, false);
    }

    /**
     * Can the given pointcut apply at all on the given class?
     * <p>This is an important test as it can be used to optimize
     * out a pointcut for a class.
     * @param pc the static or dynamic pointcut to check
     * @param targetClass the class to test
     * @param hasIntroductions whether or not the advisor chain
     * for this bean includes any introductions
     * @return whether the pointcut can apply on any method
     */
    public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
        Assert.notNull(pc, "Pointcut must not be null");
        if (!pc.getClassFilter().matches(targetClass)) {
            return false;
        }

        MethodMatcher methodMatcher = pc.getMethodMatcher();
        if (methodMatcher == MethodMatcher.TRUE) {
            // No need to iterate the methods if we're matching any method anyway...
            return true;
        }

        IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
        if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
            introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
        }

        Set<Class<?>> classes = new LinkedHashSet<Class<?>>(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
        classes.add(targetClass);
        for (Class<?> clazz : classes) {
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
            for (Method method : methods) {
                if ((introductionAwareMethodMatcher != null &&
                        introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions)) ||
                        methodMatcher.matches(method, targetClass)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Can the given advisor apply at all on the given class?
     * This is an important test as it can be used to optimize
     * out a advisor for a class.
     * @param advisor the advisor to check
     * @param targetClass class we're testing
     * @return whether the pointcut can apply on any method
     */
    public static boolean canApply(Advisor advisor, Class<?> targetClass) {
        return canApply(advisor, targetClass, false);
    }

    /**
     * Can the given advisor apply at all on the given class?
     * <p>This is an important test as it can be used to optimize out a advisor for a class.
     * This version also takes into account introductions (for IntroductionAwareMethodMatchers).
     * @param advisor the advisor to check
     * @param targetClass class we're testing
     * @param hasIntroductions whether or not the advisor chain for this bean includes
     * any introductions
     * @return whether the pointcut can apply on any method
     */
    public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
        if (advisor instanceof IntroductionAdvisor) {
            return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
        }
        else if (advisor instanceof PointcutAdvisor) {
            PointcutAdvisor pca = (PointcutAdvisor) advisor;
            return canApply(pca.getPointcut(), targetClass, hasIntroductions);
        }
        else {
            // It doesn't have a pointcut so we assume it applies.
            return true;
        }
    }

    /**
     * Determine the sublist of the {@code candidateAdvisors} list
     * that is applicable to the given class.
     * @param candidateAdvisors the Advisors to evaluate
     * @param clazz the target class
     * @return sublist of Advisors that can apply to an object of the given class
     * (may be the incoming List as-is)
     */
    public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
        if (candidateAdvisors.isEmpty()) {
            return candidateAdvisors;
        }
        List<Advisor> eligibleAdvisors = new LinkedList<Advisor>();
        for (Advisor candidate : candidateAdvisors) {
            if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
                eligibleAdvisors.add(candidate);
            }
        }
        boolean hasIntroductions = !eligibleAdvisors.isEmpty();
        for (Advisor candidate : candidateAdvisors) {
            if (candidate instanceof IntroductionAdvisor) {
                // already processed
                continue;
            }
            if (canApply(candidate, clazz, hasIntroductions)) {
                eligibleAdvisors.add(candidate);
            }
        }
        return eligibleAdvisors;
    }


    public static Object invokeJoinpointUsingReflection(Object target, Method method, Object[] args)
            throws Throwable {

        // Use reflection to invoke the method.
        try {
            ReflectionUtils.makeAccessible(method);
            return method.invoke(target, args);
        }
        catch (InvocationTargetException ex) {
            // Invoked method threw a checked exception.
            // We must rethrow it. The client won't see the interceptor.
            throw ex.getTargetException();
        }
        catch (IllegalArgumentException ex) {
            throw new AopInvocationException("AOP configuration seems to be invalid: tried calling method [" +
                    method + "] on target [" + target + "]", ex);
        }
        catch (IllegalAccessException ex) {
            throw new AopInvocationException("Could not access method [" + method + "]", ex);
        }
    }

}
