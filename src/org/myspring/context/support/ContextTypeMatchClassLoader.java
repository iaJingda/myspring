package org.myspring.context.support;

import org.myspring.core.DecoratingClassLoader;
import org.myspring.core.OverridingClassLoader;
import org.myspring.core.SmartClassLoader;
import org.myspring.core.lang.UsesJava7;
import org.myspring.core.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UsesJava7
class ContextTypeMatchClassLoader extends DecoratingClassLoader implements SmartClassLoader {

    static {
        if (parallelCapableClassLoaderAvailable) {
            ClassLoader.registerAsParallelCapable();
        }
    }


    private static Method findLoadedClassMethod;

    static {
        try {
            findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
        }
        catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Invalid [java.lang.ClassLoader] class: no 'findLoadedClass' method defined!");
        }
    }


    /** Cache for byte array per class name */
    private final Map<String, byte[]> bytesCache = new ConcurrentHashMap<String, byte[]>(256);


    public ContextTypeMatchClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return new ContextOverridingClassLoader(getParent()).loadClass(name);
    }

    @Override
    public boolean isClassReloadable(Class<?> clazz) {
        return (clazz.getClassLoader() instanceof ContextOverridingClassLoader);
    }


    /**
     * ClassLoader to be created for each loaded class.
     * Caches class file content but redefines class for each call.
     */
    private class ContextOverridingClassLoader extends OverridingClassLoader {

        public ContextOverridingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected boolean isEligibleForOverriding(String className) {
            if (isExcluded(className) || ContextTypeMatchClassLoader.this.isExcluded(className)) {
                return false;
            }
            ReflectionUtils.makeAccessible(findLoadedClassMethod);
            ClassLoader parent = getParent();
            while (parent != null) {
                if (ReflectionUtils.invokeMethod(findLoadedClassMethod, parent, className) != null) {
                    return false;
                }
                parent = parent.getParent();
            }
            return true;
        }

        @Override
        protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
            byte[] bytes = bytesCache.get(name);
            if (bytes == null) {
                bytes = loadBytesForClass(name);
                if (bytes != null) {
                    bytesCache.put(name, bytes);
                }
                else {
                    return null;
                }
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
