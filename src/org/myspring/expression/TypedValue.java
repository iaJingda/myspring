package org.myspring.expression;

import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.util.ObjectUtils;

public class TypedValue {
    public static final TypedValue NULL = new TypedValue(null);

    private final Object value;

    private TypeDescriptor typeDescriptor;

    public TypedValue(Object value) {
        this.value = value;
        this.typeDescriptor = null;  // initialized when/if requested
    }

    public TypedValue(Object value, TypeDescriptor typeDescriptor) {
        this.value = value;
        this.typeDescriptor = typeDescriptor;
    }

    public Object getValue() {
        return this.value;
    }

    public TypeDescriptor getTypeDescriptor() {
        if (this.typeDescriptor == null && this.value != null) {
            this.typeDescriptor = TypeDescriptor.forObject(this.value);
        }
        return this.typeDescriptor;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TypedValue)) {
            return false;
        }
        TypedValue otherTv = (TypedValue) other;
        // Avoid TypeDescriptor initialization if not necessary
        return (ObjectUtils.nullSafeEquals(this.value, otherTv.value) &&
                ((this.typeDescriptor == null && otherTv.typeDescriptor == null) ||
                        ObjectUtils.nullSafeEquals(getTypeDescriptor(), otherTv.getTypeDescriptor())));
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.value);
    }

    @Override
    public String toString() {
        return "TypedValue: '" + this.value + "' of [" + getTypeDescriptor() + "]";
    }
}
