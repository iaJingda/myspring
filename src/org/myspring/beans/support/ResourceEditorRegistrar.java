package org.myspring.beans.support;

import org.myspring.beans.PropertyEditorRegistrar;
import org.myspring.beans.PropertyEditorRegistry;
import org.myspring.beans.PropertyEditorRegistrySupport;
import org.myspring.beans.propertyeditors.*;
import org.myspring.core.env.PropertyResolver;
import org.myspring.core.io.ContextResource;
import org.myspring.core.io.Resource;
import org.myspring.core.io.ResourceEditor;
import org.myspring.core.io.ResourceLoader;
import org.myspring.core.io.support.ResourceArrayPropertyEditor;
import org.myspring.core.io.support.ResourcePatternResolver;
import org.myspring.core.util.ClassUtils;
import org.xml.sax.InputSource;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;

public class ResourceEditorRegistrar implements PropertyEditorRegistrar {

    private static Class<?> pathClass;

    static {
        try {
            pathClass = ClassUtils.forName("java.nio.file.Path", ResourceEditorRegistrar.class.getClassLoader());
        }
        catch (ClassNotFoundException ex) {
            // Java 7 Path class not available
            pathClass = null;
        }
    }


    private final PropertyResolver propertyResolver;

    private final ResourceLoader resourceLoader;


    public ResourceEditorRegistrar(ResourceLoader resourceLoader, PropertyResolver propertyResolver) {
        this.resourceLoader = resourceLoader;
        this.propertyResolver = propertyResolver;
    }



    @Override
    public void registerCustomEditors(PropertyEditorRegistry registry) {
        ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader, this.propertyResolver);
        doRegisterEditor(registry, Resource.class, baseEditor);
        doRegisterEditor(registry, ContextResource.class, baseEditor);
        doRegisterEditor(registry, InputStream.class, new InputStreamEditor(baseEditor));
        doRegisterEditor(registry, InputSource.class, new InputSourceEditor(baseEditor));
        doRegisterEditor(registry, File.class, new FileEditor(baseEditor));
        if (pathClass != null) {
            doRegisterEditor(registry, pathClass, new PathEditor(baseEditor));
        }
        doRegisterEditor(registry, Reader.class, new ReaderEditor(baseEditor));
        doRegisterEditor(registry, URL.class, new URLEditor(baseEditor));

        ClassLoader classLoader = this.resourceLoader.getClassLoader();
        doRegisterEditor(registry, URI.class, new URIEditor(classLoader));
        doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));
        doRegisterEditor(registry, Class[].class, new ClassArrayEditor(classLoader));

        if (this.resourceLoader instanceof ResourcePatternResolver) {
            doRegisterEditor(registry, Resource[].class,
                    new ResourceArrayPropertyEditor((ResourcePatternResolver) this.resourceLoader, this.propertyResolver));
        }
    }

    /**
     * Override default editor, if possible (since that's what we really mean to do here);
     * otherwise register as a custom editor.
     */
    private void doRegisterEditor(PropertyEditorRegistry registry, Class<?> requiredType, PropertyEditor editor) {
        if (registry instanceof PropertyEditorRegistrySupport) {
            ((PropertyEditorRegistrySupport) registry).overrideDefaultEditor(requiredType, editor);
        }
        else {
            registry.registerCustomEditor(requiredType, editor);
        }
    }

}
