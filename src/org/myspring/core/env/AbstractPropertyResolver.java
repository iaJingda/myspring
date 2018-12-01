package org.myspring.core.env;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.myspring.core.convert.ConversionService;
import org.myspring.core.convert.support.ConfigurableConversionService;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.PropertyPlaceholderHelper;
import org.myspring.core.util.SystemPropertyUtils;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver {

    protected final Log logger = LogFactory.getLog(getClass());

    private volatile ConfigurableConversionService conversionService;

    private PropertyPlaceholderHelper nonStrictHelper;

    private PropertyPlaceholderHelper strictHelper;

    private boolean ignoreUnresolvableNestedPlaceholders = false;

    private String placeholderPrefix = SystemPropertyUtils.PLACEHOLDER_PREFIX;

    private String placeholderSuffix = SystemPropertyUtils.PLACEHOLDER_SUFFIX;

    private String valueSeparator = SystemPropertyUtils.VALUE_SEPARATOR;

    private final Set<String> requiredProperties = new LinkedHashSet<String>();

    protected String resolveNestedPlaceholders(String value) {
        return (this.ignoreUnresolvableNestedPlaceholders ?
                resolvePlaceholders(value) : resolveRequiredPlaceholders(value));
    }

    protected <T> T convertValueIfNecessary(Object value, Class<T> targetType) {
        if (targetType == null) {
            return (T) value;
        }
        ConversionService conversionServiceToUse = this.conversionService;
        if (conversionServiceToUse == null) {
            // Avoid initialization of shared DefaultConversionService if
            // no standard type conversion is needed in the first place...
            if (ClassUtils.isAssignableValue(targetType, value)) {
                return (T) value;
            }
            conversionServiceToUse = DefaultConversionService.getSharedInstance();
        }
        return conversionServiceToUse.convert(value, targetType);
    }


}
