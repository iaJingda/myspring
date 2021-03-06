package org.myspring.core;

import org.myspring.core.lang.UsesJava7;
import org.myspring.core.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;

@UsesJava7
public class OverridingClassLoader extends DecoratingClassLoader {

    public static final String[] DEFAULT_EXCLUDED_PACKAGES = new String[]
            {"java.", "javax.", "sun.", "oracle.", "javassist.", "org.aspectj.", "net.sf.cglib."};

    private static final String CLASS_FILE_SUFFIX = ".class";

    static {
        if (parallelCapableClassLoaderAvailable) {
            ClassLoader.registerAsParallelCapable();
        }
    }


    private final ClassLoader overrideDelegate;


    /**
     * Create a new OverridingClassLoader for the given ClassLoader.
     * @param parent the ClassLoader to build an overriding ClassLoader for
     */
    public OverridingClassLoader(ClassLoader parent) {
        this(parent, null);
    }

    /**
     * Create a new OverridingClassLoader for the given ClassLoader.
     * @param parent the ClassLoader to build an overriding ClassLoader for
     * @param overrideDelegate the ClassLoader to delegate to for overriding
     * @since 4.3
     */
    public OverridingClassLoader(ClassLoader parent, ClassLoader overrideDelegate) {
        super(parent);
        this.overrideDelegate = overrideDelegate;
        for (String packageName : DEFAULT_EXCLUDED_PACKAGES) {
            excludePackage(packageName);
        }
    }


    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (this.overrideDelegate != null && isEligibleForOverriding(name)) {
            return this.overrideDelegate.loadClass(name);
        }
        return super.loadClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (isEligibleForOverriding(name)) {
            Class<?> result = loadClassForOverriding(name);
            if (result != null) {
                if (resolve) {
                    resolveClass(result);
                }
                return result;
            }
        }
        return super.loadClass(name, resolve);
    }

    /**
     * Determine whether the specified class is eligible for overriding
     * by this class loader.
     * @param className the class name to check
     * @return whether the specified class is eligible
     * @see #isExcluded
     */
    protected boolean isEligibleForOverriding(String className) {
        return !isExcluded(className);
    }

    /**
     * Load the specified class for overriding purposes in this ClassLoader.
     * <p>The default implementation delegates to {@link #findLoadedClass},
     * {@link #loadBytesForClass} and {@link #defineClass}.
     * @param name the name of the class
     * @return the Class object, or {@code null} if no class defined for that name
     * @throws ClassNotFoundException if the class for the given name couldn't be loaded
     */
    protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
        Class<?> result = findLoadedClass(name);
        if (result == null) {
            byte[] bytes = loadBytesForClass(name);
            if (bytes != null) {
                result = defineClass(name, bytes, 0, bytes.length);
            }
        }
        return result;
    }

    /**
     * Load the defining bytes for the given class,
     * to be turned into a Class object through a {@link #defineClass} call.
     * <p>The default implementation delegates to {@link #openStreamForClass}
     * and {@link #transformIfNecessary}.
     * @param name the name of the class
     * @return the byte content (with transformers already applied),
     * or {@code null} if no class defined for that name
     * @throws ClassNotFoundException if the class for the given name couldn't be loaded
     */
    protected byte[] loadBytesForClass(String name) throws ClassNotFoundException {
        InputStream is = openStreamForClass(name);
        if (is == null) {
            return null;
        }
        try {
            // Load the raw bytes.
            byte[] bytes = FileCopyUtils.copyToByteArray(is);
            // Transform if necessary and use the potentially transformed bytes.
            return transformIfNecessary(name, bytes);
        }
        catch (IOException ex) {
            throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
        }
    }

    /**
     * Open an InputStream for the specified class.
     * <p>The default implementation loads a standard class file through
     * the parent ClassLoader's {@code getResourceAsStream} method.
     * @param name the name of the class
     * @return the InputStream containing the byte code for the specified class
     */
    protected InputStream openStreamForClass(String name) {
        String internalName = name.replace('.', '/') + CLASS_FILE_SUFFIX;
        return getParent().getResourceAsStream(internalName);
    }


    /**
     * Transformation hook to be implemented by subclasses.
     * <p>The default implementation simply returns the given bytes as-is.
     * @param name the fully-qualified name of the class being transformed
     * @param bytes the raw bytes of the class
     * @return the transformed bytes (never {@code null};
     * same as the input bytes if the transformation produced no changes)
     */
    protected byte[] transformIfNecessary(String name, byte[] bytes) {
        return bytes;
    }

}
