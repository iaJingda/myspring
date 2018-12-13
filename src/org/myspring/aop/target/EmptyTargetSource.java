package org.myspring.aop.target;

import org.myspring.aop.TargetSource;
import org.myspring.core.util.ObjectUtils;

import java.io.Serializable;

public class EmptyTargetSource  implements TargetSource, Serializable {
    private static final long serialVersionUID = 3680494563553489691L;


    //---------------------------------------------------------------------
    // Static factory methods
    //---------------------------------------------------------------------

    /**
     * The canonical (Singleton) instance of this {@link EmptyTargetSource}.
     */
    public static final EmptyTargetSource INSTANCE = new EmptyTargetSource(null, true);


    /**
     * Return an EmptyTargetSource for the given target Class.
     * @param targetClass the target Class (may be {@code null})
     * @see #getTargetClass()
     */
    public static EmptyTargetSource forClass(Class<?> targetClass) {
        return forClass(targetClass, true);
    }

    /**
     * Return an EmptyTargetSource for the given target Class.
     * @param targetClass the target Class (may be {@code null})
     * @param isStatic whether the TargetSource should be marked as static
     * @see #getTargetClass()
     */
    public static EmptyTargetSource forClass(Class<?> targetClass, boolean isStatic) {
        return (targetClass == null && isStatic ? INSTANCE : new EmptyTargetSource(targetClass, isStatic));
    }


    //---------------------------------------------------------------------
    // Instance implementation
    //---------------------------------------------------------------------

    private final Class<?> targetClass;

    private final boolean isStatic;


    /**
     * Create a new instance of the {@link EmptyTargetSource} class.
     * <p>This constructor is {@code private} to enforce the
     * Singleton pattern / factory method pattern.
     * @param targetClass the target class to expose (may be {@code null})
     * @param isStatic whether the TargetSource is marked as static
     */
    private EmptyTargetSource(Class<?> targetClass, boolean isStatic) {
        this.targetClass = targetClass;
        this.isStatic = isStatic;
    }

    /**
     * Always returns the specified target Class, or {@code null} if none.
     */
    @Override
    public Class<?> getTargetClass() {
        return this.targetClass;
    }

    /**
     * Always returns {@code true}.
     */
    @Override
    public boolean isStatic() {
        return this.isStatic;
    }

    /**
     * Always returns {@code null}.
     */
    @Override
    public Object getTarget() {
        return null;
    }

    /**
     * Nothing to release.
     */
    @Override
    public void releaseTarget(Object target) {
    }


    /**
     * Returns the canonical instance on deserialization in case
     * of no target class, thus protecting the Singleton pattern.
     */
    private Object readResolve() {
        return (this.targetClass == null && this.isStatic ? INSTANCE : this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EmptyTargetSource)) {
            return false;
        }
        EmptyTargetSource otherTs = (EmptyTargetSource) other;
        return (ObjectUtils.nullSafeEquals(this.targetClass, otherTs.targetClass) && this.isStatic == otherTs.isStatic);
    }

    @Override
    public int hashCode() {
        return EmptyTargetSource.class.hashCode() * 13 + ObjectUtils.nullSafeHashCode(this.targetClass);
    }

    @Override
    public String toString() {
        return "EmptyTargetSource: " +
                (this.targetClass != null ? "target class [" + this.targetClass.getName() + "]" : "no target class") +
                ", " + (this.isStatic ? "static" : "dynamic");
    }

}
