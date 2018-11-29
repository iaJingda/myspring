package org.myspring.core.util;

import java.lang.reflect.Proxy;

public abstract class ClassUtils {
    public static final String ARRAY_SUFFIX = "[]";

    public static String getQualifiedName(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isArray()) {
            return getQualifiedNameForArray(clazz);
        }
        else {
            return clazz.getName();
        }
    }

    private static String getQualifiedNameForArray(Class<?> clazz) {
        StringBuilder result = new StringBuilder();
        while (clazz.isArray()) {
            clazz = clazz.getComponentType();
            result.append(ARRAY_SUFFIX);
        }
        result.insert(0, clazz.getName());
        return result.toString();
    }

    public static String getDescriptiveType(Object value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (Proxy.isProxyClass(clazz)) {
            StringBuilder result = new StringBuilder(clazz.getName());
            result.append(" implementing ");
            Class<?>[] ifcs = clazz.getInterfaces();
            for (int i = 0; i < ifcs.length; i++) {
                result.append(ifcs[i].getName());
                if (i < ifcs.length - 1) {
                    result.append(',');
                }
            }
            return result.toString();
        }
        else if (clazz.isArray()) {
            return getQualifiedNameForArray(clazz);
        }
        else {
            return clazz.getName();
        }
    }
}
