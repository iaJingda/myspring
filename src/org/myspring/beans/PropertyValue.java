package org.myspring.beans;

import org.myspring.core.util.Assert;
import org.myspring.core.util.ObjectUtils;

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

    public String getName() {
        return this.name;
    }

    /**
     * Return the value of the property.
     * <p>Note that type conversion will <i>not</i> have occurred here.
     * It is the responsibility of the BeanWrapper implementation to
     * perform type conversion.
     */
    public Object getValue() {
        return this.value;
    }

    /**
     * Return the original PropertyValue instance for this value holder.
     * @return the original PropertyValue (either a source of this
     * value holder or this value holder itself).
     */
    public PropertyValue getOriginalPropertyValue() {
        PropertyValue original = this;
        Object source = getSource();
        while (source instanceof PropertyValue && source != original) {
            original = (PropertyValue) source;
            source = original.getSource();
        }
        return original;
    }

    /**
     * Set whether this is an optional value, that is, to be ignored
     * when no corresponding property exists on the target class.
     * @since 3.0
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * Return whether this is an optional value, that is, to be ignored
     * when no corresponding property exists on the target class.
     * @since 3.0
     */
    public boolean isOptional() {
        return this.optional;
    }

    /**
     * Return whether this holder contains a converted value already ({@code true}),
     * or whether the value still needs to be converted ({@code false}).
     */
    public synchronized boolean isConverted() {
        return this.converted;
    }

    /**
     * Set the converted value of the constructor argument,
     * after processed type conversion.
     */
    public synchronized void setConvertedValue(Object value) {
        this.converted = true;
        this.convertedValue = value;
    }

    /**
     * Return the converted value of the constructor argument,
     * after processed type conversion.
     */
    public synchronized Object getConvertedValue() {
        return this.convertedValue;
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PropertyValue)) {
            return false;
        }
        PropertyValue otherPv = (PropertyValue) other;
        return (this.name.equals(otherPv.name) &&
                ObjectUtils.nullSafeEquals(this.value, otherPv.value) &&
                ObjectUtils.nullSafeEquals(getSource(), otherPv.getSource()));
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
    }

    @Override
    public String toString() {
        return "bean property '" + this.name + "'";
    }


}
