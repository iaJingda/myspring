package org.myspring.core.convert;

import org.myspring.core.util.ObjectUtils;

public class ConversionFailedException extends ConversionException {

    private final TypeDescriptor sourceType;

    private final TypeDescriptor targetType;

    private final Object value;


    public ConversionFailedException(TypeDescriptor sourceType, TypeDescriptor targetType, Object value, Throwable cause) {
        super("Failed to convert from type [" + sourceType + "] to type [" + targetType +
                "] for value '" + ObjectUtils.nullSafeToString(value) + "'", cause);
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.value = value;
    }


    /**
     * Return the source type we tried to convert the value from.
     */
    public TypeDescriptor getSourceType() {
        return this.sourceType;
    }

    /**
     * Return the target type we tried to convert the value to.
     */
    public TypeDescriptor getTargetType() {
        return this.targetType;
    }

    /**
     * Return the offending value.
     */
    public Object getValue() {
        return this.value;
    }
}
