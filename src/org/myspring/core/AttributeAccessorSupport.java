package org.myspring.core;

import org.myspring.core.util.Assert;
import org.myspring.core.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AttributeAccessorSupport  implements AttributeAccessor, Serializable {

    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>(0);

    @Override
    public void setAttribute(String name, Object value) {
        Assert.notNull(name, "Name must not be null");
        if (value != null) {
            this.attributes.put(name, value);
        }
        else {
            removeAttribute(name);
        }
    }

    @Override
    public Object getAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return this.attributes.get(name);
    }

    @Override
    public Object removeAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return this.attributes.remove(name);
    }

    @Override
    public boolean hasAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return this.attributes.containsKey(name);
    }

    @Override
    public String[] attributeNames() {
        return StringUtils.toStringArray(this.attributes.keySet());
    }

    protected void copyAttributesFrom(AttributeAccessor source) {
        Assert.notNull(source, "Source must not be null");
        String[] attributeNames = source.attributeNames();
        for (String attributeName : attributeNames) {
            setAttribute(attributeName, source.getAttribute(attributeName));
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AttributeAccessorSupport)) {
            return false;
        }
        AttributeAccessorSupport that = (AttributeAccessorSupport) other;
        return this.attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return this.attributes.hashCode();
    }

}
