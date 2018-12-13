package org.myspring.core.convert;

import org.myspring.core.MethodParameter;
import org.myspring.core.ResolvableType;
import org.myspring.core.lang.UsesJava8;
import org.myspring.core.util.Assert;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.ObjectUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class TypeDescriptor implements Serializable {

    static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];


    private static final boolean streamAvailable = ClassUtils.isPresent(
            "java.util.stream.Stream", TypeDescriptor.class.getClassLoader());

    private static final Map<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<Class<?>, TypeDescriptor>(32);

    private static final Class<?>[] CACHED_COMMON_TYPES = {
            boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
            double.class, Double.class, float.class, Float.class, int.class, Integer.class,
            long.class, Long.class, short.class, Short.class, String.class, Object.class};

    static {
        for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
            commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
        }
    }


    private final Class<?> type;

    private final ResolvableType resolvableType;

    private final AnnotatedElementAdapter annotatedElement;

    public TypeDescriptor(MethodParameter methodParameter) {
        this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
        this.type = this.resolvableType.resolve(methodParameter.getParameterType());
        this.annotatedElement = new AnnotatedElementAdapter(methodParameter.getParameterIndex() == -1 ?
                methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
    }

    public TypeDescriptor(Field field) {
        this.resolvableType = ResolvableType.forField(field);
        this.type = this.resolvableType.resolve(field.getType());
        this.annotatedElement = new AnnotatedElementAdapter(field.getAnnotations());
    }

    public TypeDescriptor(Property property) {
        Assert.notNull(property, "Property must not be null");
        this.resolvableType = ResolvableType.forMethodParameter(property.getMethodParameter());
        this.type = this.resolvableType.resolve(property.getType());
        this.annotatedElement = new AnnotatedElementAdapter(property.getAnnotations());
    }

    protected TypeDescriptor(ResolvableType resolvableType, Class<?> type, Annotation[] annotations) {
        this.resolvableType = resolvableType;
        this.type = (type != null ? type : resolvableType.resolve(Object.class));
        this.annotatedElement = new AnnotatedElementAdapter(annotations);
    }

    //构造方法结束=======================================================================================================


    public static TypeDescriptor nested(Property property, int nestingLevel) {
        return nested(new TypeDescriptor(property), nestingLevel);
    }
    private static TypeDescriptor nested(TypeDescriptor typeDescriptor, int nestingLevel) {
        ResolvableType nested = typeDescriptor.resolvableType;
        for (int i = 0; i < nestingLevel; i++) {
            if (Object.class == nested.getType()) {
                // Could be a collection type but we don't know about its element type,
                // so let's just assume there is an element type of type Object...
            }
            else {
                nested = nested.getNested(2);
            }
        }
        if (nested == ResolvableType.NONE) {
            return null;
        }
        return getRelatedIfResolvable(typeDescriptor, nested);
    }



    public TypeDescriptor elementTypeDescriptor(Object element) {
        return narrow(element, getElementTypeDescriptor());
    }
    private TypeDescriptor narrow(Object value, TypeDescriptor typeDescriptor) {
        if (typeDescriptor != null) {
            return typeDescriptor.narrow(value);
        }
        if (value != null) {
            return narrow(value);
        }
        return null;
    }
    public TypeDescriptor narrow(Object value) {
        if (value == null) {
            return this;
        }
        ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
        return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
    }

    public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
        boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
        if (!typesAssignable) {
            return false;
        }
        if (isArray() && typeDescriptor.isArray()) {
            return getElementTypeDescriptor().isAssignableTo(typeDescriptor.getElementTypeDescriptor());
        }
        else if (isCollection() && typeDescriptor.isCollection()) {
            return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
        }
        else if (isMap() && typeDescriptor.isMap()) {
            return isNestedAssignable(getMapKeyTypeDescriptor(), typeDescriptor.getMapKeyTypeDescriptor()) &&
                    isNestedAssignable(getMapValueTypeDescriptor(), typeDescriptor.getMapValueTypeDescriptor());
        }
        else {
            return true;
        }
    }

    public TypeDescriptor getElementTypeDescriptor() {
        if (getResolvableType().isArray()) {
            return new TypeDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
        }
        if (streamAvailable && StreamDelegate.isStream(getType())) {
            return StreamDelegate.getStreamElementType(this);
        }
        return getRelatedIfResolvable(this, getResolvableType().asCollection().getGeneric(0));
    }



    public TypeDescriptor getMapValueTypeDescriptor() {
        Assert.state(isMap(), "Not a [java.util.Map]");
        return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(1));
    }

    public TypeDescriptor getMapValueTypeDescriptor(Object mapValue) {
        return narrow(mapValue, getMapValueTypeDescriptor());
    }



    public boolean isArray() {
        return getType().isArray();
    }

    public boolean isCollection() {
        return Collection.class.isAssignableFrom(getType());
    }

    public boolean isMap() {
        return Map.class.isAssignableFrom(getType());
    }


    public TypeDescriptor getMapKeyTypeDescriptor() {
        Assert.state(isMap(), "Not a [java.util.Map]");
        return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(0));
    }

    private static TypeDescriptor getRelatedIfResolvable(TypeDescriptor source, ResolvableType type) {
        if (type.resolve() == null) {
            return null;
        }
        return new TypeDescriptor(type, null, source.getAnnotations());
    }

    public ResolvableType getResolvableType() {
        return this.resolvableType;
    }

    public Annotation[] getAnnotations() {
        return this.annotatedElement.getAnnotations();
    }

    public boolean isPrimitive() {
        return getType().isPrimitive();
    }



    private boolean isNestedAssignable(TypeDescriptor nestedTypeDescriptor, TypeDescriptor otherNestedTypeDescriptor) {
        if (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null) {
            return true;
        }
        return nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor);
    }


    public static TypeDescriptor forObject(Object source) {
        return (source != null ? valueOf(source.getClass()) : null);
    }

    public static TypeDescriptor valueOf(Class<?> type) {
        if (type == null) {
            type = Object.class;
        }
        TypeDescriptor desc = commonTypesCache.get(type);
        return (desc != null ? desc : new TypeDescriptor(ResolvableType.forClass(type), null, null));
    }



    public Class<?> getObjectType() {
        return ClassUtils.resolvePrimitiveIfNecessary(getType());
    }

    public Class<?> getType() {
        return this.type;
    }


    //内部类开始===========================================================================================================

    private class AnnotatedElementAdapter implements AnnotatedElement, Serializable {

        private final Annotation[] annotations;

        public AnnotatedElementAdapter(Annotation[] annotations) {
            this.annotations = annotations;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            for (Annotation annotation : getAnnotations()) {
                if (annotation.annotationType() == annotationClass) {
                    return true;
                }
            }
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            for (Annotation annotation : getAnnotations()) {
                if (annotation.annotationType() == annotationClass) {
                    return (T) annotation;
                }
            }
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return (this.annotations != null ? this.annotations : EMPTY_ANNOTATION_ARRAY);
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return getAnnotations();
        }

        public boolean isEmpty() {
            return ObjectUtils.isEmpty(this.annotations);
        }

        @Override
        public boolean equals(Object other) {
            return (this == other || (other instanceof AnnotatedElementAdapter &&
                    Arrays.equals(this.annotations, ((AnnotatedElementAdapter) other).annotations)));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.annotations);
        }

        @Override
        public String toString() {
            return TypeDescriptor.this.toString();
        }
    }

    /**
     * Inner class to avoid a hard dependency on Java 8.
     */
    @UsesJava8
    private static class StreamDelegate {

        public static boolean isStream(Class<?> type) {
            return Stream.class.isAssignableFrom(type);
        }

        public static TypeDescriptor getStreamElementType(TypeDescriptor source) {
            return getRelatedIfResolvable(source, source.getResolvableType().as(Stream.class).getGeneric(0));
        }
    }
}
