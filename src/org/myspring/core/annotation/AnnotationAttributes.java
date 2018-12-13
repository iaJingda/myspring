package org.myspring.core.annotation;

import org.myspring.core.util.Assert;
import org.myspring.core.util.ObjectUtils;
import org.myspring.core.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnnotationAttributes extends LinkedHashMap<String, Object> {

    private static final String UNKNOWN = "unknown";

    private final Class<? extends Annotation> annotationType;

    private final String displayName;

    boolean validated = false;

    public AnnotationAttributes() {
        this.annotationType = null;
        this.displayName = UNKNOWN;
    }
    public AnnotationAttributes(int initialCapacity) {
        super(initialCapacity);
        this.annotationType = null;
        this.displayName = UNKNOWN;
    }

    public AnnotationAttributes(Class<? extends Annotation> annotationType) {
        Assert.notNull(annotationType, "'annotationType' must not be null");
        this.annotationType = annotationType;
        this.displayName = annotationType.getName();
    }
    public AnnotationAttributes(String annotationType, ClassLoader classLoader) {
        Assert.notNull(annotationType, "'annotationType' must not be null");
        this.annotationType = getAnnotationType(annotationType, classLoader);
        this.displayName = annotationType;
    }

    private static Class<? extends Annotation> getAnnotationType(String annotationType, ClassLoader classLoader) {
        if (classLoader != null) {
            try {
                return (Class<? extends Annotation>) classLoader.loadClass(annotationType);
            }
            catch (ClassNotFoundException ex) {
                // Annotation Class not resolvable
            }
        }
        return null;
    }
    public AnnotationAttributes(Map<String, Object> map) {
        super(map);
        this.annotationType = null;
        this.displayName = UNKNOWN;
    }

    public AnnotationAttributes(AnnotationAttributes other) {
        super(other);
        this.annotationType = other.annotationType;
        this.displayName = other.displayName;
        this.validated = other.validated;
    }

    public Class<? extends Annotation> annotationType() {
        return this.annotationType;
    }
    public String getString(String attributeName) {
        return getRequiredAttribute(attributeName, String.class);
    }

    @Deprecated
    public String getAliasedString(String attributeName, Class<? extends Annotation> annotationType,
                                   Object annotationSource) {

        return getRequiredAttributeWithAlias(attributeName, annotationType, annotationSource, String.class);
    }

    public String[] getStringArray(String attributeName) {
        return getRequiredAttribute(attributeName, String[].class);
    }

    @Deprecated
    public String[] getAliasedStringArray(String attributeName, Class<? extends Annotation> annotationType,
                                          Object annotationSource) {

        return getRequiredAttributeWithAlias(attributeName, annotationType, annotationSource, String[].class);
    }

    public boolean getBoolean(String attributeName) {
        return getRequiredAttribute(attributeName, Boolean.class);
    }

    public <N extends Number> N getNumber(String attributeName) {
        return (N) getRequiredAttribute(attributeName, Number.class);
    }

    public <E extends Enum<?>> E getEnum(String attributeName) {
        return (E) getRequiredAttribute(attributeName, Enum.class);
    }

    public <T> Class<? extends T> getClass(String attributeName) {
        return getRequiredAttribute(attributeName, Class.class);
    }

    public Class<?>[] getClassArray(String attributeName) {
        return getRequiredAttribute(attributeName, Class[].class);
    }

    @Deprecated
    public Class<?>[] getAliasedClassArray(String attributeName, Class<? extends Annotation> annotationType,
                                           Object annotationSource) {

        return getRequiredAttributeWithAlias(attributeName, annotationType, annotationSource, Class[].class);
    }
    public AnnotationAttributes getAnnotation(String attributeName) {
        return getRequiredAttribute(attributeName, AnnotationAttributes.class);
    }

    public <A extends Annotation> A getAnnotation(String attributeName, Class<A> annotationType) {
        return getRequiredAttribute(attributeName, annotationType);
    }

    public AnnotationAttributes[] getAnnotationArray(String attributeName) {
        return getRequiredAttribute(attributeName, AnnotationAttributes[].class);
    }

    public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
        Object array = Array.newInstance(annotationType, 0);
        return (A[]) getRequiredAttribute(attributeName, array.getClass());
    }

    private <T> T getRequiredAttribute(String attributeName, Class<T> expectedType) {
        Assert.hasText(attributeName, "'attributeName' must not be null or empty");
        Object value = get(attributeName);
        assertAttributePresence(attributeName, value);
        assertNotException(attributeName, value);
        if (!expectedType.isInstance(value) && expectedType.isArray() &&
                expectedType.getComponentType().isInstance(value)) {
            Object array = Array.newInstance(expectedType.getComponentType(), 1);
            Array.set(array, 0, value);
            value = array;
        }
        assertAttributeType(attributeName, value, expectedType);
        return (T) value;
    }


    private <T> T getRequiredAttributeWithAlias(String attributeName, Class<? extends Annotation> annotationType,
                                                Object annotationSource, Class<T> expectedType) {

        Assert.hasText(attributeName, "'attributeName' must not be null or empty");
        Assert.notNull(annotationType, "'annotationType' must not be null");
        Assert.notNull(expectedType, "'expectedType' must not be null");

        T attributeValue = getAttribute(attributeName, expectedType);

        List<String> aliasNames = AnnotationUtils.getAttributeAliasMap(annotationType).get(attributeName);
        if (aliasNames != null) {
            for (String aliasName : aliasNames) {
                T aliasValue = getAttribute(aliasName, expectedType);
                boolean attributeEmpty = ObjectUtils.isEmpty(attributeValue);
                boolean aliasEmpty = ObjectUtils.isEmpty(aliasValue);

                if (!attributeEmpty && !aliasEmpty && !ObjectUtils.nullSafeEquals(attributeValue, aliasValue)) {
                    String elementName = (annotationSource == null ? "unknown element" : annotationSource.toString());
                    String msg = String.format("In annotation [%s] declared on [%s], attribute [%s] and its " +
                                    "alias [%s] are present with values of [%s] and [%s], but only one is permitted.",
                            annotationType.getName(), elementName, attributeName, aliasName,
                            ObjectUtils.nullSafeToString(attributeValue), ObjectUtils.nullSafeToString(aliasValue));
                    throw new AnnotationConfigurationException(msg);
                }

                // If we expect an array and the current tracked value is null but the
                // current alias value is non-null, then replace the current null value
                // with the non-null value (which may be an empty array).
                if (expectedType.isArray() && attributeValue == null && aliasValue != null) {
                    attributeValue = aliasValue;
                }
                // Else: if we're not expecting an array, we can rely on the behavior of
                // ObjectUtils.isEmpty().
                else if (attributeEmpty && !aliasEmpty) {
                    attributeValue = aliasValue;
                }
            }
            assertAttributePresence(attributeName, aliasNames, attributeValue);
        }

        return attributeValue;
    }

    private <T> T getAttribute(String attributeName, Class<T> expectedType) {
        Object value = get(attributeName);
        if (value != null) {
            assertNotException(attributeName, value);
            assertAttributeType(attributeName, value, expectedType);
        }
        return (T) value;
    }

    private void assertAttributePresence(String attributeName, Object attributeValue) {
        if (attributeValue == null) {
            throw new IllegalArgumentException(String.format(
                    "Attribute '%s' not found in attributes for annotation [%s]", attributeName, this.displayName));
        }
    }

    private void assertAttributePresence(String attributeName, List<String> aliases, Object attributeValue) {
        if (attributeValue == null) {
            throw new IllegalArgumentException(String.format(
                    "Neither attribute '%s' nor one of its aliases %s was found in attributes for annotation [%s]",
                    attributeName, aliases, this.displayName));
        }
    }

    private void assertNotException(String attributeName, Object attributeValue) {
        if (attributeValue instanceof Exception) {
            throw new IllegalArgumentException(String.format(
                    "Attribute '%s' for annotation [%s] was not resolvable due to exception [%s]",
                    attributeName, this.displayName, attributeValue), (Exception) attributeValue);
        }
    }

    private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
        if (!expectedType.isInstance(attributeValue)) {
            throw new IllegalArgumentException(String.format(
                    "Attribute '%s' is of type [%s], but [%s] was expected in attributes for annotation [%s]",
                    attributeName, attributeValue.getClass().getSimpleName(), expectedType.getSimpleName(),
                    this.displayName));
        }
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        Object obj = get(key);
        if (obj == null) {
            obj = put(key, value);
        }
        return obj;
    }

    @Override
    public String toString() {
        Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
        StringBuilder sb = new StringBuilder("{");
        while (entries.hasNext()) {
            Map.Entry<String, Object> entry = entries.next();
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(valueToString(entry.getValue()));
            sb.append(entries.hasNext() ? ", " : "");
        }
        sb.append("}");
        return sb.toString();
    }

    private String valueToString(Object value) {
        if (value == this) {
            return "(this Map)";
        }
        if (value instanceof Object[]) {
            return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
        }
        return String.valueOf(value);
    }

    public static AnnotationAttributes fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        if (map instanceof AnnotationAttributes) {
            return (AnnotationAttributes) map;
        }
        return new AnnotationAttributes(map);
    }


}
