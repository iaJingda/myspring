package org.myspring.beans.propertyeditors;

import org.myspring.core.io.Resource;
import org.myspring.core.io.ResourceEditor;
import org.myspring.core.io.support.EncodedResource;
import org.myspring.core.util.Assert;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

public class ReaderEditor extends PropertyEditorSupport {
    private final ResourceEditor resourceEditor;


    /**
     * Create a new ReaderEditor, using the default ResourceEditor underneath.
     */
    public ReaderEditor() {
        this.resourceEditor = new ResourceEditor();
    }

    /**
     * Create a new ReaderEditor, using the given ResourceEditor underneath.
     * @param resourceEditor the ResourceEditor to use
     */
    public ReaderEditor(ResourceEditor resourceEditor) {
        Assert.notNull(resourceEditor, "ResourceEditor must not be null");
        this.resourceEditor = resourceEditor;
    }


    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        this.resourceEditor.setAsText(text);
        Resource resource = (Resource) this.resourceEditor.getValue();
        try {
            setValue(resource != null ? new EncodedResource(resource).getReader() : null);
        }
        catch (IOException ex) {
            throw new IllegalArgumentException("Failed to retrieve Reader for " + resource, ex);
        }
    }

    /**
     * This implementation returns {@code null} to indicate that
     * there is no appropriate text representation.
     */
    @Override
    public String getAsText() {
        return null;
    }

}
