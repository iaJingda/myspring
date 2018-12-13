package org.myspring.aop.target;

import org.myspring.aop.TargetSource;
import org.myspring.core.util.Assert;
import org.myspring.core.util.ObjectUtils;

import java.io.Serializable;

public class SingletonTargetSource implements TargetSource, Serializable {

    private static final long serialVersionUID = 9031246629662423738L;


    /** Target cached and invoked using reflection */
    private final Object target;


    /**
     * Create a new SingletonTargetSource for the given target.
     * @param target the target object
     */
    public SingletonTargetSource(Object target) {
        Assert.notNull(target, "Target object must not be null");
        this.target = target;
    }


    @Override
    public Class<?> getTargetClass() {
        return this.target.getClass();
    }

    @Override
    public Object getTarget() {
        return this.target;
    }

    @Override
    public void releaseTarget(Object target) {
        // nothing to do
    }

    @Override
    public boolean isStatic() {
        return true;
    }


    /**
     * Two invoker interceptors are equal if they have the same target or if the
     * targets or the targets are equal.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SingletonTargetSource)) {
            return false;
        }
        SingletonTargetSource otherTargetSource = (SingletonTargetSource) other;
        return this.target.equals(otherTargetSource.target);
    }

    /**
     * SingletonTargetSource uses the hash code of the target object.
     */
    @Override
    public int hashCode() {
        return this.target.hashCode();
    }

    @Override
    public String toString() {
        return "SingletonTargetSource for target object [" + ObjectUtils.identityToString(this.target) + "]";
    }
}
