package org.myspring.beans;

import org.myspring.core.util.Assert;
import org.myspring.core.util.ObjectUtils;

public class BeanMetadataAttribute implements BeanMetadataElement {

    private final String name;

    private final Object value;

    private Object source;

    public BeanMetadataAttribute(String name, Object value) {
        Assert.notNull(name, "Name must not be null");
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }


    public Object getValue() {
        return this.value;
    }


    public void setSource(Object source) {
        this.source = source;
    }

    @Override
    public Object getSource() {
        return this.source;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BeanMetadataAttribute)) {
            return false;
        }
        BeanMetadataAttribute otherMa = (BeanMetadataAttribute) other;
        return (this.name.equals(otherMa.name) &&
                ObjectUtils.nullSafeEquals(this.value, otherMa.value) &&
                ObjectUtils.nullSafeEquals(this.source, otherMa.source));
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
    }

    @Override
    public String toString() {
        return "metadata attribute '" + this.name + "'";
    }
}
