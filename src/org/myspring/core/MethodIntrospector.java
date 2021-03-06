package org.myspring.core;

import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public abstract class MethodIntrospector {

    public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {
        final Map<Method, T> methodMap = new LinkedHashMap<Method, T>();
        Set<Class<?>> handlerTypes = new LinkedHashSet<Class<?>>();
        Class<?> specificHandlerType = null;

        if (!Proxy.isProxyClass(targetType)) {
            handlerTypes.add(targetType);
            specificHandlerType = targetType;
        }
        handlerTypes.addAll(Arrays.asList(targetType.getInterfaces()));

        for (Class<?> currentHandlerType : handlerTypes) {
            final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);

            ReflectionUtils.doWithMethods(currentHandlerType, new ReflectionUtils.MethodCallback() {
                @Override
                public void doWith(Method method) {
                    Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
                    T result = metadataLookup.inspect(specificMethod);
                    if (result != null) {
                        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
                        if (bridgedMethod == specificMethod || metadataLookup.inspect(bridgedMethod) == null) {
                            methodMap.put(specificMethod, result);
                        }
                    }
                }
            }, ReflectionUtils.USER_DECLARED_METHODS);
        }

        return methodMap;
    }

    /**
     * Select methods on the given target type based on a filter.
     * <p>Callers define methods of interest through the {@code MethodFilter} parameter.
     * @param targetType the target type to search methods on
     * @param methodFilter a {@code MethodFilter} to help
     * recognize handler methods of interest
     * @return the selected methods, or an empty set in case of no match
     */
    public static Set<Method> selectMethods(Class<?> targetType, final ReflectionUtils.MethodFilter methodFilter) {
        return selectMethods(targetType, new MetadataLookup<Boolean>() {
            @Override
            public Boolean inspect(Method method) {
                return (methodFilter.matches(method) ? Boolean.TRUE : null);
            }
        }).keySet();
    }

    /**
     * Select an invocable method on the target type: either the given method itself
     * if actually exposed on the target type, or otherwise a corresponding method
     * on one of the target type's interfaces or on the target type itself.
     * <p>Matches on user-declared interfaces will be preferred since they are likely
     * to contain relevant metadata that corresponds to the method on the target class.
     * @param method the method to check
     * @param targetType the target type to search methods on
     * (typically an interface-based JDK proxy)
     * @return a corresponding invocable method on the target type
     * @throws IllegalStateException if the given method is not invocable on the given
     * target type (typically due to a proxy mismatch)
     */
    public static Method selectInvocableMethod(Method method, Class<?> targetType) {
        if (method.getDeclaringClass().isAssignableFrom(targetType)) {
            return method;
        }
        try {
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> ifc : targetType.getInterfaces()) {
                try {
                    return ifc.getMethod(methodName, parameterTypes);
                }
                catch (NoSuchMethodException ex) {
                    // Alright, not on this interface then...
                }
            }
            // A final desperate attempt on the proxy class itself...
            return targetType.getMethod(methodName, parameterTypes);
        }
        catch (NoSuchMethodException ex) {
            throw new IllegalStateException(String.format(
                    "Need to invoke method '%s' declared on target class '%s', " +
                            "but not found in any interface(s) of the exposed proxy type. " +
                            "Either pull the method up to an interface or switch to CGLIB " +
                            "proxies by enforcing proxy-target-class mode in your configuration.",
                    method.getName(), method.getDeclaringClass().getSimpleName()));
        }
    }


    /**
     * A callback interface for metadata lookup on a given method.
     * @param <T> the type of metadata returned
     */
    public interface MetadataLookup<T> {

        /**
         * Perform a lookup on the given method and return associated metadata, if any.
         * @param method the method to inspect
         * @return non-null metadata to be associated with a method if there is a match,
         * or {@code null} for no match
         */
        T inspect(Method method);
    }

}
