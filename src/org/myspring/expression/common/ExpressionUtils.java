package org.myspring.expression.common;

import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.util.ClassUtils;
import org.myspring.expression.EvaluationContext;
import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypeConverter;
import org.myspring.expression.TypedValue;

public abstract class ExpressionUtils {
    public static <T> T convertTypedValue(EvaluationContext context, TypedValue typedValue, Class<T> targetType) {
        Object value = typedValue.getValue();
        if (targetType == null) {
            return (T) value;
        }
        if (context != null) {
            return (T) context.getTypeConverter().convertValue(
                    value, typedValue.getTypeDescriptor(), TypeDescriptor.valueOf(targetType));
        }
        if (ClassUtils.isAssignableValue(targetType, value)) {
            return (T) value;
        }
        throw new EvaluationException("Cannot convert value '" + value + "' to type '" + targetType.getName() + "'");
    }


    public static int toInt(TypeConverter typeConverter, TypedValue typedValue) {
        return (Integer) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
                TypeDescriptor.valueOf(Integer.class));
    }

    /**
     * Attempt to convert a typed value to a boolean using the supplied type converter.
     */
    public static boolean toBoolean(TypeConverter typeConverter, TypedValue typedValue) {
        return (Boolean) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
                TypeDescriptor.valueOf(Boolean.class));
    }

    /**
     * Attempt to convert a typed value to a double using the supplied type converter.
     */
    public static double toDouble(TypeConverter typeConverter, TypedValue typedValue) {
        return (Double) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
                TypeDescriptor.valueOf(Double.class));
    }

    /**
     * Attempt to convert a typed value to a long using the supplied type converter.
     */
    public static long toLong(TypeConverter typeConverter, TypedValue typedValue) {
        return (Long) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
                TypeDescriptor.valueOf(Long.class));
    }

    /**
     * Attempt to convert a typed value to a char using the supplied type converter.
     */
    public static char toChar(TypeConverter typeConverter, TypedValue typedValue) {
        return (Character) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
                TypeDescriptor.valueOf(Character.class));
    }

    /**
     * Attempt to convert a typed value to a short using the supplied type converter.
     */
    public static short toShort(TypeConverter typeConverter, TypedValue typedValue) {
        return (Short) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
                TypeDescriptor.valueOf(Short.class));
    }

    /**
     * Attempt to convert a typed value to a float using the supplied type converter.
     */
    public static float toFloat(TypeConverter typeConverter, TypedValue typedValue) {
        return (Float) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
                TypeDescriptor.valueOf(Float.class));
    }

    /**
     * Attempt to convert a typed value to a byte using the supplied type converter.
     */
    public static byte toByte(TypeConverter typeConverter, TypedValue typedValue) {
        return (Byte) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
                TypeDescriptor.valueOf(Byte.class));
    }
}
