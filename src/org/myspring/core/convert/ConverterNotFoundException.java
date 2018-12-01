package org.myspring.core.convert;

public class ConverterNotFoundException extends ConversionException {

    private final TypeDescriptor sourceType;

    private final TypeDescriptor targetType;


    /**
     * Create a new conversion executor not found exception.
     * @param sourceType the source type requested to convert from
     * @param targetType the target type requested to convert to
     */
    public ConverterNotFoundException(TypeDescriptor sourceType, TypeDescriptor targetType) {
        super("No converter found capable of converting from type [" + sourceType + "] to type [" + targetType + "]");
        this.sourceType = sourceType;
        this.targetType = targetType;
    }


    /**
     * Return the source type that was requested to convert from.
     */
    public TypeDescriptor getSourceType() {
        return this.sourceType;
    }

    /**
     * Return the target type that was requested to convert to.
     */
    public TypeDescriptor getTargetType() {
        return this.targetType;
    }
}
