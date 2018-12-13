package org.myspring.expression;

import org.myspring.core.convert.TypeDescriptor;

public interface TypeConverter {

    boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType);

    Object convertValue(Object value, TypeDescriptor sourceType, TypeDescriptor targetType);


}
