package org.myspring.core.io.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.myspring.core.env.PropertyResolver;
import org.myspring.core.env.StandardEnvironment;
import org.myspring.core.io.Resource;
import org.myspring.core.util.Assert;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ResourceArrayPropertyEditor   extends PropertyEditorSupport {
    private static final Log logger = LogFactory.getLog(ResourceArrayPropertyEditor.class);

    private final ResourcePatternResolver resourcePatternResolver;

    private PropertyResolver propertyResolver;

    private final boolean ignoreUnresolvablePlaceholders;



    public ResourceArrayPropertyEditor() {
        this(new PathMatchingResourcePatternResolver(), null, true);
    }


    public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver, PropertyResolver propertyResolver) {
        this(resourcePatternResolver, propertyResolver, true);
    }


    public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver,
                                       PropertyResolver propertyResolver, boolean ignoreUnresolvablePlaceholders) {

        Assert.notNull(resourcePatternResolver, "ResourcePatternResolver must not be null");
        this.resourcePatternResolver = resourcePatternResolver;
        this.propertyResolver = propertyResolver;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }


    /**
     * Treat the given text as a location pattern and convert it to a Resource array.
     */
    @Override
    public void setAsText(String text) {
        String pattern = resolvePath(text).trim();
        try {
            setValue(this.resourcePatternResolver.getResources(pattern));
        }
        catch (IOException ex) {
            throw new IllegalArgumentException(
                    "Could not resolve resource location pattern [" + pattern + "]: " + ex.getMessage());
        }
    }

    /**
     * Treat the given value as a collection or array and convert it to a Resource array.
     * Considers String elements as location patterns and takes Resource elements as-is.
     */
    @Override
    public void setValue(Object value) throws IllegalArgumentException {
        if (value instanceof Collection || (value instanceof Object[] && !(value instanceof Resource[]))) {
            Collection<?> input = (value instanceof Collection ? (Collection<?>) value : Arrays.asList((Object[]) value));
            List<Resource> merged = new ArrayList<Resource>();
            for (Object element : input) {
                if (element instanceof String) {
                    // A location pattern: resolve it into a Resource array.
                    // Might point to a single resource or to multiple resources.
                    String pattern = resolvePath((String) element).trim();
                    try {
                        Resource[] resources = this.resourcePatternResolver.getResources(pattern);
                        for (Resource resource : resources) {
                            if (!merged.contains(resource)) {
                                merged.add(resource);
                            }
                        }
                    }
                    catch (IOException ex) {
                        // ignore - might be an unresolved placeholder or non-existing base directory
                        if (logger.isDebugEnabled()) {
                            logger.debug("Could not retrieve resources for pattern '" + pattern + "'", ex);
                        }
                    }
                }
                else if (element instanceof Resource) {
                    // A Resource object: add it to the result.
                    Resource resource = (Resource) element;
                    if (!merged.contains(resource)) {
                        merged.add(resource);
                    }
                }
                else {
                    throw new IllegalArgumentException("Cannot convert element [" + element + "] to [" +
                            Resource.class.getName() + "]: only location String and Resource object supported");
                }
            }
            super.setValue(merged.toArray(new Resource[merged.size()]));
        }

        else {
            // An arbitrary value: probably a String or a Resource array.
            // setAsText will be called for a String; a Resource array will be used as-is.
            super.setValue(value);
        }
    }

    /**
     * Resolve the given path, replacing placeholders with
     * corresponding system property values if necessary.
     * @param path the original file path
     * @return the resolved file path
     * @see PropertyResolver#resolvePlaceholders
     * @see PropertyResolver#resolveRequiredPlaceholders(String)
     */
    protected String resolvePath(String path) {
        if (this.propertyResolver == null) {
            this.propertyResolver = new StandardEnvironment();
        }
        return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) :
                this.propertyResolver.resolveRequiredPlaceholders(path));
    }

}
