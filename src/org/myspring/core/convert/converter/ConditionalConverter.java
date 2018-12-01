package org.myspring.core.convert.converter;

import org.myspring.core.convert.TypeDescriptor;

public interface ConditionalConverter {

    boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);
}
