package org.myspring.beans.factory.support;

import org.myspring.beans.BeanMetadataElement;
import org.myspring.beans.Mergeable;

import java.util.ArrayList;
import java.util.List;

public class ManagedList<E> extends ArrayList<E> implements Mergeable, BeanMetadataElement {

    private Object source;

    private String elementTypeName;

    private boolean mergeEnabled;


    public ManagedList() {
    }

    public ManagedList(int initialCapacity) {
        super(initialCapacity);
    }


    /**
     * Set the configuration source {@code Object} for this metadata element.
     * <p>The exact type of the object will depend on the configuration mechanism used.
     */
    public void setSource(Object source) {
        this.source = source;
    }

    @Override
    public Object getSource() {
        return this.source;
    }

    /**
     * Set the default element type name (class name) to be used for this list.
     */
    public void setElementTypeName(String elementTypeName) {
        this.elementTypeName = elementTypeName;
    }

    /**
     * Return the default element type name (class name) to be used for this list.
     */
    public String getElementTypeName() {
        return this.elementTypeName;
    }

    /**
     * Set whether merging should be enabled for this collection,
     * in case of a 'parent' collection value being present.
     */
    public void setMergeEnabled(boolean mergeEnabled) {
        this.mergeEnabled = mergeEnabled;
    }

    @Override
    public boolean isMergeEnabled() {
        return this.mergeEnabled;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<E> merge(Object parent) {
        if (!this.mergeEnabled) {
            throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
        }
        if (parent == null) {
            return this;
        }
        if (!(parent instanceof List)) {
            throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
        }
        List<E> merged = new ManagedList<E>();
        merged.addAll((List<E>) parent);
        merged.addAll(this);
        return merged;
    }

}
