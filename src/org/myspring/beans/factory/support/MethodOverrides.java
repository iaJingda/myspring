package org.myspring.beans.factory.support;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class MethodOverrides {

    private final Set<MethodOverride> overrides =
            Collections.synchronizedSet(new LinkedHashSet<MethodOverride>(0));

    private volatile boolean modified = false;


    /**
     * Create new MethodOverrides.
     */
    public MethodOverrides() {
    }

    /**
     * Deep copy constructor.
     */
    public MethodOverrides(MethodOverrides other) {
        addOverrides(other);
    }


    /**
     * Copy all given method overrides into this object.
     */
    public void addOverrides(MethodOverrides other) {
        if (other != null) {
            this.modified = true;
            this.overrides.addAll(other.overrides);
        }
    }

    /**
     * Add the given method override.
     */
    public void addOverride(MethodOverride override) {
        this.modified = true;
        this.overrides.add(override);
    }

    /**
     * Return all method overrides contained by this object.
     * @return Set of MethodOverride objects
     * @see MethodOverride
     */
    public Set<MethodOverride> getOverrides() {
        this.modified = true;
        return this.overrides;
    }

    /**
     * Return whether the set of method overrides is empty.
     */
    public boolean isEmpty() {
        return (!this.modified || this.overrides.isEmpty());
    }

    /**
     * Return the override for the given method, if any.
     * @param method method to check for overrides for
     * @return the method override, or {@code null} if none
     */
    public MethodOverride getOverride(Method method) {
        if (!this.modified) {
            return null;
        }
        synchronized (this.overrides) {
            MethodOverride match = null;
            for (MethodOverride candidate : this.overrides) {
                if (candidate.matches(method)) {
                    match = candidate;
                }
            }
            return match;
        }
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MethodOverrides)) {
            return false;
        }
        MethodOverrides that = (MethodOverrides) other;
        return this.overrides.equals(that.overrides);

    }

    @Override
    public int hashCode() {
        return this.overrides.hashCode();
    }
}