package org.myspring.core.env;

import org.myspring.core.util.ObjectUtils;

public abstract class EnumerablePropertySource<T> extends PropertySource<T> {
    public EnumerablePropertySource(String name, T source) {
        super(name, source);
    }

    protected EnumerablePropertySource(String name) {
        super(name);
    }


    /**
     * Return whether this {@code PropertySource} contains a property with the given name.
     * <p>This implementation checks for the presence of the given name within the
     * {@link #getPropertyNames()} array.
     * @param name the name of the property to find
     */
    @Override
    public boolean containsProperty(String name) {
        return ObjectUtils.containsElement(getPropertyNames(), name);
    }

    /**
     * Return the names of all properties contained by the
     * {@linkplain #getSource() source} object (never {@code null}).
     */
    public abstract String[] getPropertyNames();
}
