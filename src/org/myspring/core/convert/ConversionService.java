package org.myspring.core.convert;

public interface ConversionService {

    boolean canConvert(Class<?> sourceType, Class<?> targetType);


    boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType);

    <T> T convert(Object source, Class<T> targetType);


    Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);

}
