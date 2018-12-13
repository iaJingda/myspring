package org.myspring.beans.factory.support;

import org.myspring.beans.BeanMetadataElement;
import org.myspring.beans.factory.ObjectFactory;
import org.myspring.beans.factory.config.TypedStringValue;
import org.myspring.core.util.Assert;
import org.myspring.core.util.ClassUtils;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

abstract class AutowireUtils {

    public static void sortConstructors(Constructor<?>[] constructors) {
        Arrays.sort(constructors, new Comparator<Constructor<?>>() {
            @Override
            public int compare(Constructor<?> c1, Constructor<?> c2) {
                boolean p1 = Modifier.isPublic(c1.getModifiers());
                boolean p2 = Modifier.isPublic(c2.getModifiers());
                if (p1 != p2) {
                    return (p1 ? -1 : 1);
                }
                int c1pl = c1.getParameterTypes().length;
                int c2pl = c2.getParameterTypes().length;
                return (c1pl < c2pl ? 1 : (c1pl > c2pl ? -1 : 0));
            }
        });
    }

    public static void sortFactoryMethods(Method[] factoryMethods) {
        Arrays.sort(factoryMethods, new Comparator<Method>() {
            @Override
            public int compare(Method fm1, Method fm2) {
                boolean p1 = Modifier.isPublic(fm1.getModifiers());
                boolean p2 = Modifier.isPublic(fm2.getModifiers());
                if (p1 != p2) {
                    return (p1 ? -1 : 1);
                }
                int c1pl = fm1.getParameterTypes().length;
                int c2pl = fm2.getParameterTypes().length;
                return (c1pl < c2pl ? 1 : (c1pl > c2pl ? -1 : 0));
            }
        });
    }

    public static boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
        Method wm = pd.getWriteMethod();
        if (wm == null) {
            return false;
        }
        if (!wm.getDeclaringClass().getName().contains("$$")) {
            // Not a CGLIB method so it's OK.
            return false;
        }
        // It was declared by CGLIB, but we might still want to autowire it
        // if it was actually declared by the superclass.
        Class<?> superclass = wm.getDeclaringClass().getSuperclass();
        return !ClassUtils.hasMethod(superclass, wm.getName(), wm.getParameterTypes());
    }

    public static boolean isSetterDefinedInInterface(PropertyDescriptor pd, Set<Class<?>> interfaces) {
        Method setter = pd.getWriteMethod();
        if (setter != null) {
            Class<?> targetClass = setter.getDeclaringClass();
            for (Class<?> ifc : interfaces) {
                if (ifc.isAssignableFrom(targetClass) &&
                        ClassUtils.hasMethod(ifc, setter.getName(), setter.getParameterTypes())) {
                    return true;
                }
            }
        }
        return false;
    }
    public static Object resolveAutowiringValue(Object autowiringValue, Class<?> requiredType) {
        if (autowiringValue instanceof ObjectFactory && !requiredType.isInstance(autowiringValue)) {
            ObjectFactory<?> factory = (ObjectFactory<?>) autowiringValue;
            if (autowiringValue instanceof Serializable && requiredType.isInterface()) {
                autowiringValue = Proxy.newProxyInstance(requiredType.getClassLoader(),
                        new Class<?>[] {requiredType}, new ObjectFactoryDelegatingInvocationHandler(factory));
            }
            else {
                return factory.getObject();
            }
        }
        return autowiringValue;
    }
    public static Class<?> resolveReturnTypeForFactoryMethod(Method method, Object[] args, ClassLoader classLoader) {
        Assert.notNull(method, "Method must not be null");
        Assert.notNull(args, "Argument array must not be null");
        Assert.notNull(classLoader, "ClassLoader must not be null");

        TypeVariable<Method>[] declaredTypeVariables = method.getTypeParameters();
        Type genericReturnType = method.getGenericReturnType();
        Type[] methodParameterTypes = method.getGenericParameterTypes();
        Assert.isTrue(args.length == methodParameterTypes.length, "Argument array does not match parameter count");

        // Ensure that the type variable (e.g., T) is declared directly on the method
        // itself (e.g., via <T>), not on the enclosing class or interface.
        boolean locallyDeclaredTypeVariableMatchesReturnType = false;
        for (TypeVariable<Method> currentTypeVariable : declaredTypeVariables) {
            if (currentTypeVariable.equals(genericReturnType)) {
                locallyDeclaredTypeVariableMatchesReturnType = true;
                break;
            }
        }

        if (locallyDeclaredTypeVariableMatchesReturnType) {
            for (int i = 0; i < methodParameterTypes.length; i++) {
                Type methodParameterType = methodParameterTypes[i];
                Object arg = args[i];
                if (methodParameterType.equals(genericReturnType)) {
                    if (arg instanceof TypedStringValue) {
                        TypedStringValue typedValue = ((TypedStringValue) arg);
                        if (typedValue.hasTargetType()) {
                            return typedValue.getTargetType();
                        }
                        try {
                            return typedValue.resolveTargetType(classLoader);
                        }
                        catch (ClassNotFoundException ex) {
                            throw new IllegalStateException("Failed to resolve value type [" +
                                    typedValue.getTargetTypeName() + "] for factory method argument", ex);
                        }
                    }
                    // Only consider argument type if it is a simple value...
                    if (arg != null && !(arg instanceof BeanMetadataElement)) {
                        return arg.getClass();
                    }
                    return method.getReturnType();
                }
                else if (methodParameterType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    for (Type typeArg : actualTypeArguments) {
                        if (typeArg.equals(genericReturnType)) {
                            if (arg instanceof Class) {
                                return (Class<?>) arg;
                            }
                            else {
                                String className = null;
                                if (arg instanceof String) {
                                    className = (String) arg;
                                }
                                else if (arg instanceof TypedStringValue) {
                                    TypedStringValue typedValue = ((TypedStringValue) arg);
                                    String targetTypeName = typedValue.getTargetTypeName();
                                    if (targetTypeName == null || Class.class.getName().equals(targetTypeName)) {
                                        className = typedValue.getValue();
                                    }
                                }
                                if (className != null) {
                                    try {
                                        return ClassUtils.forName(className, classLoader);
                                    }
                                    catch (ClassNotFoundException ex) {
                                        throw new IllegalStateException("Could not resolve class name [" + arg +
                                                "] for factory method argument", ex);
                                    }
                                }
                                // Consider adding logic to determine the class of the typeArg, if possible.
                                // For now, just fall back...
                                return method.getReturnType();
                            }
                        }
                    }
                }
            }
        }

        // Fall back...
        return method.getReturnType();
    }

    private static class ObjectFactoryDelegatingInvocationHandler implements InvocationHandler, Serializable {

        private final ObjectFactory<?> objectFactory;

        public ObjectFactoryDelegatingInvocationHandler(ObjectFactory<?> objectFactory) {
            this.objectFactory = objectFactory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("equals")) {
                // Only consider equal when proxies are identical.
                return (proxy == args[0]);
            }
            else if (methodName.equals("hashCode")) {
                // Use hashCode of proxy.
                return System.identityHashCode(proxy);
            }
            else if (methodName.equals("toString")) {
                return this.objectFactory.toString();
            }
            try {
                return method.invoke(this.objectFactory.getObject(), args);
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }
}
