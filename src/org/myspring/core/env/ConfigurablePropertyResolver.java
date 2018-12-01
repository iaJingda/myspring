package org.myspring.core.env;

import org.myspring.core.convert.support.ConfigurableConversionService;

public interface ConfigurablePropertyResolver  extends PropertyResolver {

    ConfigurableConversionService getConversionService();

    void setConversionService(ConfigurableConversionService conversionService);

    void setPlaceholderPrefix(String placeholderPrefix);

    void setPlaceholderSuffix(String placeholderSuffix);

    void setValueSeparator(String valueSeparator);

    void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

    void setRequiredProperties(String... requiredProperties);


    void validateRequiredProperties() throws MissingRequiredPropertiesException;

}
