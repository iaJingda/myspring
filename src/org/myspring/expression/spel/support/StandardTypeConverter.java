package org.myspring.expression.spel.support;

import org.myspring.core.convert.ConversionException;
import org.myspring.core.convert.ConversionService;
import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.convert.support.DefaultConversionService;
import org.myspring.core.util.Assert;
import org.myspring.expression.TypeConverter;
import org.myspring.expression.spel.SpelEvaluationException;
import org.myspring.expression.spel.SpelMessage;

public class StandardTypeConverter implements TypeConverter {

    private final ConversionService conversionService;

    public StandardTypeConverter() {
        this.conversionService = DefaultConversionService.getSharedInstance();
    }

    public StandardTypeConverter(ConversionService conversionService) {
        Assert.notNull(conversionService, "ConversionService must not be null");
        this.conversionService = conversionService;
    }

    @Override
    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.conversionService.canConvert(sourceType, targetType);
    }

    @Override
    public Object convertValue(Object value, TypeDescriptor sourceType, TypeDescriptor targetType) {
        try {
            return this.conversionService.convert(value, sourceType, targetType);
        }
        catch (ConversionException ex) {
            throw new SpelEvaluationException(ex, SpelMessage.TYPE_CONVERSION_ERROR,
                    (sourceType != null ? sourceType.toString() : (value != null ? value.getClass().getName() : "null")),
                    targetType.toString());
        }
    }

}
