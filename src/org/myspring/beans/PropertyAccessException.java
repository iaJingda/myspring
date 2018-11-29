package org.myspring.beans;


import org.myspring.core.ErrorCoded;

import java.beans.PropertyChangeEvent;

@SuppressWarnings({"serial", "deprecation"})
public abstract class PropertyAccessException extends BeansException implements ErrorCoded {

    private transient PropertyChangeEvent propertyChangeEvent;

    public PropertyAccessException(PropertyChangeEvent propertyChangeEvent, String msg, Throwable cause) {
        super(msg, cause);
        this.propertyChangeEvent = propertyChangeEvent;
    }

    public PropertyAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PropertyChangeEvent getPropertyChangeEvent() {
        return this.propertyChangeEvent;
    }


    public String getPropertyName() {
        return (this.propertyChangeEvent != null ? this.propertyChangeEvent.getPropertyName() : null);
    }

    public Object getValue() {
        return (this.propertyChangeEvent != null ? this.propertyChangeEvent.getNewValue() : null);
    }

    @Override
    public abstract String getErrorCode();
}
