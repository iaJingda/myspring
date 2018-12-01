package org.myspring.core.env;

import org.myspring.core.util.Assert;

import java.util.Map;

public class SystemEnvironmentPropertySource extends MapPropertySource {

    /**
     * Create a new {@code SystemEnvironmentPropertySource} with the given name and
     * delegating to the given {@code MapPropertySource}.
     */
    public SystemEnvironmentPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }


    /**
     * Return {@code true} if a property with the given name or any underscore/uppercase variant
     * thereof exists in this property source.
     */
    @Override
    public boolean containsProperty(String name) {
        return (getProperty(name) != null);
    }

    /**
     * This implementation returns {@code true} if a property with the given name or
     * any underscore/uppercase variant thereof exists in this property source.
     */
    @Override
    public Object getProperty(String name) {
        String actualName = resolvePropertyName(name);
        if (logger.isDebugEnabled() && !name.equals(actualName)) {
            logger.debug("PropertySource '" + getName() + "' does not contain property '" + name +
                    "', but found equivalent '" + actualName + "'");
        }
        return super.getProperty(actualName);
    }

    /**
     * Check to see if this property source contains a property with the given name, or
     * any underscore / uppercase variation thereof. Return the resolved name if one is
     * found or otherwise the original name. Never returns {@code null}.
     */
    private String resolvePropertyName(String name) {
        Assert.notNull(name, "Property name must not be null");
        String resolvedName = checkPropertyName(name);
        if (resolvedName != null) {
            return resolvedName;
        }
        String uppercasedName = name.toUpperCase();
        if (!name.equals(uppercasedName)) {
            resolvedName = checkPropertyName(uppercasedName);
            if (resolvedName != null) {
                return resolvedName;
            }
        }
        return name;
    }

    private String checkPropertyName(String name) {
        // Check name as-is
        if (containsKey(name)) {
            return name;
        }
        // Check name with just dots replaced
        String noDotName = name.replace('.', '_');
        if (!name.equals(noDotName) && containsKey(noDotName)) {
            return noDotName;
        }
        // Check name with just hyphens replaced
        String noHyphenName = name.replace('-', '_');
        if (!name.equals(noHyphenName) && containsKey(noHyphenName)) {
            return noHyphenName;
        }
        // Check name with dots and hyphens replaced
        String noDotNoHyphenName = noDotName.replace('-', '_');
        if (!noDotName.equals(noDotNoHyphenName) && containsKey(noDotNoHyphenName)) {
            return noDotNoHyphenName;
        }
        // Give up
        return null;
    }

    private boolean containsKey(String name) {
        return (isSecurityManagerPresent() ? this.source.keySet().contains(name) : this.source.containsKey(name));
    }

    protected boolean isSecurityManagerPresent() {
        return (System.getSecurityManager() != null);
    }
}
