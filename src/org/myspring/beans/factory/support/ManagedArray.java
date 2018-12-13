package org.myspring.beans.factory.support;

import org.myspring.core.util.Assert;

public class ManagedArray extends ManagedList<Object> {

    volatile Class<?> resolvedElementType;


    /**
     * Create a new managed array placeholder.
     * @param elementTypeName the target element type as a class name
     * @param size the size of the array
     */
    public ManagedArray(String elementTypeName, int size) {
        super(size);
        Assert.notNull(elementTypeName, "elementTypeName must not be null");
        setElementTypeName(elementTypeName);
    }
}
