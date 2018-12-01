package org.myspring.core.env;

import org.myspring.core.convert.ConversionException;
import org.myspring.core.util.ClassUtils;

public class PropertySourcesPropertyResolver extends AbstractPropertyResolver {

    private final PropertySources propertySources;

    public PropertySourcesPropertyResolver(PropertySources propertySources) {
        this.propertySources = propertySources;
    }

    @Override
    public boolean containsProperty(String key) {
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                if (propertySource.containsProperty(key)) {
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public String getProperty(String key) {
        return getProperty(key, String.class, true);
    }
    @Override
    public <T> T getProperty(String key, Class<T> targetValueType) {
        return getProperty(key, targetValueType, true);
    }

    @Override
    protected String getPropertyAsRawString(String key) {
        return getProperty(key, String.class, false);
    }

    protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Searching for key '" + key + "' in PropertySource '" +
                            propertySource.getName() + "'");
                }
                Object value = propertySource.getProperty(key);
                if (value != null) {
                    if (resolveNestedPlaceholders && value instanceof String) {
                        value = resolveNestedPlaceholders((String) value);
                    }
                    logKeyFound(key, propertySource, value);
                    return convertValueIfNecessary(value, targetValueType);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Could not find key '" + key + "' in any property source");
        }
        return null;
    }

    @Override
    @Deprecated
    public <T> Class<T> getPropertyAsClass(String key, Class<T> targetValueType) {
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("Searching for key '%s' in [%s]", key, propertySource.getName()));
                }
                Object value = propertySource.getProperty(key);
                if (value != null) {
                    logKeyFound(key, propertySource, value);
                    Class<?> clazz;
                    if (value instanceof String) {
                        try {
                            clazz = ClassUtils.forName((String) value, null);
                        }
                        catch (Exception ex) {
                            throw new ClassConversionException((String) value, targetValueType, ex);
                        }
                    }
                    else if (value instanceof Class) {
                        clazz = (Class<?>) value;
                    }
                    else {
                        clazz = value.getClass();
                    }
                    if (!targetValueType.isAssignableFrom(clazz)) {
                        throw new ClassConversionException(clazz, targetValueType);
                    }
                    @SuppressWarnings("unchecked")
                    Class<T> targetClass = (Class<T>) clazz;
                    return targetClass;
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Could not find key '%s' in any property source", key));
        }
        return null;
    }

    protected void logKeyFound(String key, PropertySource<?> propertySource, Object value) {
        if (logger.isDebugEnabled()) {
            logger.debug("Found key '" + key + "' in PropertySource '" + propertySource.getName() +
                    "' with value of type " + value.getClass().getSimpleName());
        }
    }

    private static class ClassConversionException extends ConversionException {

        public ClassConversionException(Class<?> actual, Class<?> expected) {
            super(String.format("Actual type %s is not assignable to expected type %s",
                    actual.getName(), expected.getName()));
        }

        public ClassConversionException(String actual, Class<?> expected, Exception ex) {
            super(String.format("Could not find/load class %s during attempt to convert to %s",
                    actual, expected.getName()), ex);
        }
    }

}
