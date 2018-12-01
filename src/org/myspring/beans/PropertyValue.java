package org.myspring.beans;

import org.myspring.core.util.Assert;

import java.io.Serializable;

public class PropertyValue extends BeanMetadataAttributeAccessor implements Serializable {

    private final String name;

    private final Object value;

    private boolean optional = false;

    private boolean converted = false;

    private Object convertedValue;

    volatile Boolean conversionNecessary;

    /** Package-visible field for caching the resolved property path tokens */
    transient volatile Object resolvedTokens;

    public PropertyValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }
    public String getName() {
        return this.name;
    }
    public Object getValue() {
        return this.value;
    }

    public boolean isOptional() {
        return this.optional;
    }

    public PropertyValue(PropertyValue original) {
        Assert.notNull(original, "Original must not be null");
        this.name = original.getName();
        this.value = original.getValue();
        this.optional = original.isOptional();
        this.converted = original.converted;
        this.convertedValue = original.convertedValue;
        this.conversionNecessary = original.conversionNecessary;
        this.resolvedTokens = original.resolvedTokens;
        setSource(original.getSource());
        copyAttributesFrom(original);
    }

    public PropertyValue(PropertyValue original, Object newValue) {
        Assert.notNull(original, "Original must not be null");
        this.name = original.getName();
        this.value = newValue;
        this.optional = original.isOptional();
        this.conversionNecessary = original.conversionNecessary;
        this.resolvedTokens = original.resolvedTokens;
        setSource(original);
        copyAttributesFrom(original);
    }


}
