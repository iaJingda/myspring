package org.myspring.beans;

import org.myspring.core.util.Assert;
import org.myspring.core.util.ObjectUtils;

import java.io.PrintStream;
import java.io.PrintWriter;

public class PropertyBatchUpdateException extends BeansException  {

    private PropertyAccessException[] propertyAccessExceptions;


    /**
     * Create a new PropertyBatchUpdateException.
     * @param propertyAccessExceptions the List of PropertyAccessExceptions
     */
    public PropertyBatchUpdateException(PropertyAccessException[] propertyAccessExceptions) {
        super(null);
        Assert.notEmpty(propertyAccessExceptions, "At least 1 PropertyAccessException required");
        this.propertyAccessExceptions = propertyAccessExceptions;
    }


    /**
     * If this returns 0, no errors were encountered during binding.
     */
    public final int getExceptionCount() {
        return this.propertyAccessExceptions.length;
    }

    /**
     * Return an array of the propertyAccessExceptions stored in this object.
     * <p>Will return the empty array (not {@code null}) if there were no errors.
     */
    public final PropertyAccessException[] getPropertyAccessExceptions() {
        return this.propertyAccessExceptions;
    }

    /**
     * Return the exception for this field, or {@code null} if there isn't any.
     */
    public PropertyAccessException getPropertyAccessException(String propertyName) {
        for (PropertyAccessException pae : this.propertyAccessExceptions) {
            if (ObjectUtils.nullSafeEquals(propertyName, pae.getPropertyName())) {
                return pae;
            }
        }
        return null;
    }


    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Failed properties: ");
        for (int i = 0; i < this.propertyAccessExceptions.length; i++) {
            sb.append(this.propertyAccessExceptions[i].getMessage());
            if (i < this.propertyAccessExceptions.length - 1) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append("; nested PropertyAccessExceptions (");
        sb.append(getExceptionCount()).append(") are:");
        for (int i = 0; i < this.propertyAccessExceptions.length; i++) {
            sb.append('\n').append("PropertyAccessException ").append(i + 1).append(": ");
            sb.append(this.propertyAccessExceptions[i]);
        }
        return sb.toString();
    }

    @Override
    public void printStackTrace(PrintStream ps) {
        synchronized (ps) {
            ps.println(getClass().getName() + "; nested PropertyAccessException details (" +
                    getExceptionCount() + ") are:");
            for (int i = 0; i < this.propertyAccessExceptions.length; i++) {
                ps.println("PropertyAccessException " + (i + 1) + ":");
                this.propertyAccessExceptions[i].printStackTrace(ps);
            }
        }
    }

    @Override
    public void printStackTrace(PrintWriter pw) {
        synchronized (pw) {
            pw.println(getClass().getName() + "; nested PropertyAccessException details (" +
                    getExceptionCount() + ") are:");
            for (int i = 0; i < this.propertyAccessExceptions.length; i++) {
                pw.println("PropertyAccessException " + (i + 1) + ":");
                this.propertyAccessExceptions[i].printStackTrace(pw);
            }
        }
    }

    @Override
    public boolean contains(Class<?> exType) {
        if (exType == null) {
            return false;
        }
        if (exType.isInstance(this)) {
            return true;
        }
        for (PropertyAccessException pae : this.propertyAccessExceptions) {
            if (pae.contains(exType)) {
                return true;
            }
        }
        return false;
    }

}
