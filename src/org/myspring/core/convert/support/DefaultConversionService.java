package org.myspring.core.convert.support;

import org.myspring.core.convert.ConversionService;
import org.myspring.core.convert.converter.ConverterRegistry;
import org.myspring.core.util.ClassUtils;

import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

public class DefaultConversionService extends GenericConversionService {

    /** Java 8's java.util.Optional class available? */
    private static final boolean javaUtilOptionalClassAvailable =
            ClassUtils.isPresent("java.util.Optional", DefaultConversionService.class.getClassLoader());

    /** Java 8's java.time package available? */
    private static final boolean jsr310Available =
            ClassUtils.isPresent("java.time.ZoneId", DefaultConversionService.class.getClassLoader());

    /** Java 8's java.util.stream.Stream class available? */
    private static final boolean streamAvailable = ClassUtils.isPresent(
            "java.util.stream.Stream", DefaultConversionService.class.getClassLoader());

    private static volatile DefaultConversionService sharedInstance;


    public DefaultConversionService() {
        // addDefaultConverters(this);
    }

    public static ConversionService getSharedInstance() {
        if (sharedInstance == null) {
            synchronized (DefaultConversionService.class) {
                if (sharedInstance == null) {
                    sharedInstance = new DefaultConversionService();
                }
            }
        }
        return sharedInstance;
    }
    /*
    public static void addDefaultConverters(ConverterRegistry converterRegistry) {
        addScalarConverters(converterRegistry);
        addCollectionConverters(converterRegistry);

        converterRegistry.addConverter(new ByteBufferConverter((ConversionService) converterRegistry));
        if (jsr310Available) {
            Jsr310ConverterRegistrar.registerJsr310Converters(converterRegistry);
        }

        converterRegistry.addConverter(new ObjectToObjectConverter());
        converterRegistry.addConverter(new IdToEntityConverter((ConversionService) converterRegistry));
        converterRegistry.addConverter(new FallbackObjectToStringConverter());
        if (javaUtilOptionalClassAvailable) {
            converterRegistry.addConverter(new ObjectToOptionalConverter((ConversionService) converterRegistry));
        }
    }

    public static void addCollectionConverters(ConverterRegistry converterRegistry) {
        ConversionService conversionService = (ConversionService) converterRegistry;

        converterRegistry.addConverter(new ArrayToCollectionConverter(conversionService));
        converterRegistry.addConverter(new CollectionToArrayConverter(conversionService));

        converterRegistry.addConverter(new ArrayToArrayConverter(conversionService));
        converterRegistry.addConverter(new CollectionToCollectionConverter(conversionService));
        converterRegistry.addConverter(new MapToMapConverter(conversionService));

        converterRegistry.addConverter(new ArrayToStringConverter(conversionService));
        converterRegistry.addConverter(new StringToArrayConverter(conversionService));

        converterRegistry.addConverter(new ArrayToObjectConverter(conversionService));
        converterRegistry.addConverter(new ObjectToArrayConverter(conversionService));

        converterRegistry.addConverter(new CollectionToStringConverter(conversionService));
        converterRegistry.addConverter(new StringToCollectionConverter(conversionService));

        converterRegistry.addConverter(new CollectionToObjectConverter(conversionService));
        converterRegistry.addConverter(new ObjectToCollectionConverter(conversionService));

        if (streamAvailable) {
            converterRegistry.addConverter(new StreamConverter(conversionService));
        }
    }

    private static void addScalarConverters(ConverterRegistry converterRegistry) {
        converterRegistry.addConverterFactory(new NumberToNumberConverterFactory());

        converterRegistry.addConverterFactory(new StringToNumberConverterFactory());
        converterRegistry.addConverter(Number.class, String.class, new ObjectToStringConverter());

        converterRegistry.addConverter(new StringToCharacterConverter());
        converterRegistry.addConverter(Character.class, String.class, new ObjectToStringConverter());

        converterRegistry.addConverter(new NumberToCharacterConverter());
        converterRegistry.addConverterFactory(new CharacterToNumberFactory());

        converterRegistry.addConverter(new StringToBooleanConverter());
        converterRegistry.addConverter(Boolean.class, String.class, new ObjectToStringConverter());

        converterRegistry.addConverterFactory(new StringToEnumConverterFactory());
        converterRegistry.addConverter(new EnumToStringConverter((ConversionService) converterRegistry));

        converterRegistry.addConverterFactory(new IntegerToEnumConverterFactory());
        converterRegistry.addConverter(new EnumToIntegerConverter((ConversionService) converterRegistry));

        converterRegistry.addConverter(new StringToLocaleConverter());
        converterRegistry.addConverter(Locale.class, String.class, new ObjectToStringConverter());

        converterRegistry.addConverter(new StringToCharsetConverter());
        converterRegistry.addConverter(Charset.class, String.class, new ObjectToStringConverter());

        converterRegistry.addConverter(new StringToCurrencyConverter());
        converterRegistry.addConverter(Currency.class, String.class, new ObjectToStringConverter());

        converterRegistry.addConverter(new StringToPropertiesConverter());
        converterRegistry.addConverter(new PropertiesToStringConverter());

        converterRegistry.addConverter(new StringToUUIDConverter());
        converterRegistry.addConverter(UUID.class, String.class, new ObjectToStringConverter());
    }

    private static final class Jsr310ConverterRegistrar {

        public static void registerJsr310Converters(ConverterRegistry converterRegistry) {
            converterRegistry.addConverter(new StringToTimeZoneConverter());
            converterRegistry.addConverter(new ZoneIdToTimeZoneConverter());
            converterRegistry.addConverter(new ZonedDateTimeToCalendarConverter());
        }
    }

    */

}
