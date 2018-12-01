package org.myspring.core.convert;

import org.myspring.core.NestedRuntimeException;

public abstract class ConversionException extends NestedRuntimeException {

    public ConversionException(String message) {
        super(message);
    }

    /**
     * Construct a new conversion exception.
     * @param message the exception message
     * @param cause the cause
     */
    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

}
