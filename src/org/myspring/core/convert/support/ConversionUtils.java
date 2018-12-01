package org.myspring.core.convert.support;

import org.myspring.core.convert.ConversionFailedException;
import org.myspring.core.convert.ConversionService;
import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.convert.converter.GenericConverter;

abstract class ConversionUtils {

    public static Object invokeConverter(GenericConverter converter, Object source, TypeDescriptor sourceType,
                                         TypeDescriptor targetType) {

        try {
            return converter.convert(source, sourceType, targetType);
        }
        catch (ConversionFailedException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new ConversionFailedException(sourceType, targetType, source, ex);
        }
    }

    public static boolean canConvertElements(TypeDescriptor sourceElementType, TypeDescriptor targetElementType,
                                             ConversionService conversionService) {

        if (targetElementType == null) {
            // yes
            return true;
        }
        if (sourceElementType == null) {
            // maybe
            return true;
        }
        if (conversionService.canConvert(sourceElementType, targetElementType)) {
            // yes
            return true;
        }
        if (sourceElementType.getType().isAssignableFrom(targetElementType.getType())) {
            // maybe
            return true;
        }
        // no
        return false;
    }

    public static Class<?> getEnumType(Class<?> targetType) {
        Class<?> enumType = targetType;
        while (enumType != null && !enumType.isEnum()) {
            enumType = enumType.getSuperclass();
        }
        if (enumType == null) {
            throw new IllegalArgumentException(
                    "The target type " + targetType.getName() + " does not refer to an enum");
        }
        return enumType;
    }
}
