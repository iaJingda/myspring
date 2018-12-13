package org.myspring.beans.propertyeditors;

import org.myspring.core.io.ClassPathResource;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.ResourceUtils;
import org.myspring.core.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class URIEditor extends PropertyEditorSupport {

    private final ClassLoader classLoader;

    private final boolean encode;



    /**
     * Create a new, encoding URIEditor, converting "classpath:" locations into
     * standard URIs (not trying to resolve them into physical resources).
     */
    public URIEditor() {
        this(true);
    }

    /**
     * Create a new URIEditor, converting "classpath:" locations into
     * standard URIs (not trying to resolve them into physical resources).
     * @param encode indicates whether Strings will be encoded or not
     * @since 3.0
     */
    public URIEditor(boolean encode) {
        this.classLoader = null;
        this.encode = encode;
    }

    /**
     * Create a new URIEditor, using the given ClassLoader to resolve
     * "classpath:" locations into physical resource URLs.
     * @param classLoader the ClassLoader to use for resolving "classpath:" locations
     * (may be {@code null} to indicate the default ClassLoader)
     */
    public URIEditor(ClassLoader classLoader) {
        this(classLoader, true);
    }

    /**
     * Create a new URIEditor, using the given ClassLoader to resolve
     * "classpath:" locations into physical resource URLs.
     * @param classLoader the ClassLoader to use for resolving "classpath:" locations
     * (may be {@code null} to indicate the default ClassLoader)
     * @param encode indicates whether Strings will be encoded or not
     * @since 3.0
     */
    public URIEditor(ClassLoader classLoader, boolean encode) {
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
        this.encode = encode;
    }


    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasText(text)) {
            String uri = text.trim();
            if (this.classLoader != null && uri.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
                ClassPathResource resource = new ClassPathResource(
                        uri.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length()), this.classLoader);
                try {
                    setValue(resource.getURI());
                }
                catch (IOException ex) {
                    throw new IllegalArgumentException("Could not retrieve URI for " + resource + ": " + ex.getMessage());
                }
            }
            else {
                try {
                    setValue(createURI(uri));
                }
                catch (URISyntaxException ex) {
                    throw new IllegalArgumentException("Invalid URI syntax: " + ex);
                }
            }
        }
        else {
            setValue(null);
        }
    }

    /**
     * Create a URI instance for the given user-specified String value.
     * <p>The default implementation encodes the value into a RFC-2396 compliant URI.
     * @param value the value to convert into a URI instance
     * @return the URI instance
     * @throws java.net.URISyntaxException if URI conversion failed
     */
    protected URI createURI(String value) throws URISyntaxException {
        int colonIndex = value.indexOf(':');
        if (this.encode && colonIndex != -1) {
            int fragmentIndex = value.indexOf('#', colonIndex + 1);
            String scheme = value.substring(0, colonIndex);
            String ssp = value.substring(colonIndex + 1, (fragmentIndex > 0 ? fragmentIndex : value.length()));
            String fragment = (fragmentIndex > 0 ? value.substring(fragmentIndex + 1) : null);
            return new URI(scheme, ssp, fragment);
        }
        else {
            // not encoding or the value contains no scheme - fallback to default
            return new URI(value);
        }
    }


    @Override
    public String getAsText() {
        URI value = (URI) getValue();
        return (value != null ? value.toString() : "");
    }
}
