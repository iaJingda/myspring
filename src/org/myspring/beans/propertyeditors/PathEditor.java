package org.myspring.beans.propertyeditors;

import org.myspring.core.io.Resource;
import org.myspring.core.io.ResourceEditor;
import org.myspring.core.io.ResourceLoader;
import org.myspring.core.lang.UsesJava7;
import org.myspring.core.util.Assert;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

@UsesJava7
public class PathEditor extends PropertyEditorSupport {

    private final ResourceEditor resourceEditor;


    /**
     * Create a new PathEditor, using the default ResourceEditor underneath.
     */
    public PathEditor() {
        this.resourceEditor = new ResourceEditor();
    }

    /**
     * Create a new PathEditor, using the given ResourceEditor underneath.
     * @param resourceEditor the ResourceEditor to use
     */
    public PathEditor(ResourceEditor resourceEditor) {
        Assert.notNull(resourceEditor, "ResourceEditor must not be null");
        this.resourceEditor = resourceEditor;
    }


    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        boolean nioPathCandidate = !text.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX);
        if (nioPathCandidate && !text.startsWith("/")) {
            try {
                URI uri = new URI(text);
                if (uri.getScheme() != null) {
                    nioPathCandidate = false;
                    // Let's try NIO file system providers via Paths.get(URI)
                    setValue(Paths.get(uri).normalize());
                    return;
                }
            }
            catch (URISyntaxException ex) {
                // Not a valid URI: Let's try as Spring resource location.
            }
            catch (FileSystemNotFoundException ex) {
                // URI scheme not registered for NIO:
                // Let's try URL protocol handlers via Spring's resource mechanism.
            }
        }

        this.resourceEditor.setAsText(text);
        Resource resource = (Resource) this.resourceEditor.getValue();
        if (resource == null) {
            setValue(null);
        }
        else if (!resource.exists() && nioPathCandidate) {
            setValue(Paths.get(text).normalize());
        }
        else {
            try {
                setValue(resource.getFile().toPath());
            }
            catch (IOException ex) {
                throw new IllegalArgumentException("Failed to retrieve file for " + resource, ex);
            }
        }
    }

    @Override
    public String getAsText() {
        Path value = (Path) getValue();
        return (value != null ? value.toString() : "");
    }

}

