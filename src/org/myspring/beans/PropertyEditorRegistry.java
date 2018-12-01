package org.myspring.beans;

import java.beans.PropertyEditor;

public interface PropertyEditorRegistry {

    void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

    void registerCustomEditor(Class<?> requiredType, String propertyPath, PropertyEditor propertyEditor);

    PropertyEditor findCustomEditor(Class<?> requiredType, String propertyPath);




}
