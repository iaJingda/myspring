package org.myspring.core.annotation;

import org.myspring.core.NestedRuntimeException;

public class AnnotationConfigurationException extends NestedRuntimeException {

    public AnnotationConfigurationException(String message) {
        super(message);
    }


    public AnnotationConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
