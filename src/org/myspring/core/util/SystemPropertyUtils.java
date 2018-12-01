package org.myspring.core.util;

public class SystemPropertyUtils {

    /** Prefix for system property placeholders: "${" */
    public static final String PLACEHOLDER_PREFIX = "${";

    /** Suffix for system property placeholders: "}" */
    public static final String PLACEHOLDER_SUFFIX = "}";

    /** Value separator for system property placeholders: ":" */
    public static final String VALUE_SEPARATOR = ":";

    private static final PropertyPlaceholderHelper strictHelper =
            new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, false);

    private static final PropertyPlaceholderHelper nonStrictHelper =
            new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, true);

    public static String resolvePlaceholders(String text) {
        return resolvePlaceholders(text, false);
    }

    public static String resolvePlaceholders(String text, boolean ignoreUnresolvablePlaceholders) {
        PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
        return helper.replacePlaceholders(text, new SystemPropertyPlaceholderResolver(text));
    }

    private static class SystemPropertyPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

        private final String text;

        public SystemPropertyPlaceholderResolver(String text) {
            this.text = text;
        }

        @Override
        public String resolvePlaceholder(String placeholderName) {
            try {
                String propVal = System.getProperty(placeholderName);
                if (propVal == null) {
                    // Fall back to searching the system environment.
                    propVal = System.getenv(placeholderName);
                }
                return propVal;
            }
            catch (Throwable ex) {
                System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" +
                        this.text + "] as system property: " + ex);
                return null;
            }
        }
    }
}
