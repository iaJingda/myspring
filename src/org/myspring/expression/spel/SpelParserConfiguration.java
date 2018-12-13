package org.myspring.expression.spel;

import org.myspring.core.SpringProperties;

public class SpelParserConfiguration {

    private static final SpelCompilerMode defaultCompilerMode;

    static {
        String compilerMode = SpringProperties.getProperty("spring.expression.compiler.mode");
        defaultCompilerMode = (compilerMode != null ?
                SpelCompilerMode.valueOf(compilerMode.toUpperCase()) : SpelCompilerMode.OFF);
    }


    private final SpelCompilerMode compilerMode;

    private final ClassLoader compilerClassLoader;

    private final boolean autoGrowNullReferences;

    private final boolean autoGrowCollections;

    private final int maximumAutoGrowSize;

    public SpelParserConfiguration() {
        this(null, null, false, false, Integer.MAX_VALUE);
    }

    public SpelParserConfiguration(SpelCompilerMode compilerMode, ClassLoader compilerClassLoader) {
        this(compilerMode, compilerClassLoader, false, false, Integer.MAX_VALUE);
    }

    public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
        this(null, null, autoGrowNullReferences, autoGrowCollections, Integer.MAX_VALUE);
    }

    public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {
        this(null, null, autoGrowNullReferences, autoGrowCollections, maximumAutoGrowSize);
    }

    public SpelParserConfiguration(SpelCompilerMode compilerMode, ClassLoader compilerClassLoader,
                                   boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {

        this.compilerMode = (compilerMode != null ? compilerMode : defaultCompilerMode);
        this.compilerClassLoader = compilerClassLoader;
        this.autoGrowNullReferences = autoGrowNullReferences;
        this.autoGrowCollections = autoGrowCollections;
        this.maximumAutoGrowSize = maximumAutoGrowSize;
    }

    public SpelCompilerMode getCompilerMode() {
        return this.compilerMode;
    }

    /**
     * Return the ClassLoader to use as the basis for expression compilation.
     */
    public ClassLoader getCompilerClassLoader() {
        return this.compilerClassLoader;
    }

    /**
     * Return {@code true} if {@code null} references should be automatically grown.
     */
    public boolean isAutoGrowNullReferences() {
        return this.autoGrowNullReferences;
    }

    /**
     * Return {@code true} if collections should be automatically grown.
     */
    public boolean isAutoGrowCollections() {
        return this.autoGrowCollections;
    }

    /**
     * Return the maximum size that a collection can auto grow.
     */
    public int getMaximumAutoGrowSize() {
        return this.maximumAutoGrowSize;
    }
}
