package org.myspring.beans.factory;

import org.myspring.core.MethodParameter;
import org.myspring.core.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;

public class InjectionPoint {

    protected MethodParameter methodParameter;

    protected Field field;

    private volatile Annotation[] fieldAnnotations;

    public InjectionPoint(MethodParameter methodParameter) {
        Assert.notNull(methodParameter, "MethodParameter must not be null");
        this.methodParameter = methodParameter;
    }


    public InjectionPoint(Field field) {
        Assert.notNull(field, "Field must not be null");
        this.field = field;
    }

    protected InjectionPoint(InjectionPoint original) {
        this.methodParameter = (original.methodParameter != null ?
                new MethodParameter(original.methodParameter) : null);
        this.field = original.field;
        this.fieldAnnotations = original.fieldAnnotations;
    }

    protected InjectionPoint() {
    }

    public MethodParameter getMethodParameter() {
        return this.methodParameter;
    }

    public Field getField() {
        return this.field;
    }

    public Annotation[] getAnnotations() {
        if (this.field != null) {
            if (this.fieldAnnotations == null) {
                this.fieldAnnotations = this.field.getAnnotations();
            }
            return this.fieldAnnotations;
        }
        else {
            return this.methodParameter.getParameterAnnotations();
        }
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return (this.field != null ? this.field.getAnnotation(annotationType) :
                this.methodParameter.getParameterAnnotation(annotationType));
    }

    public Class<?> getDeclaredType() {
        return (this.field != null ? this.field.getType() : this.methodParameter.getParameterType());
    }

    public Member getMember() {
        return (this.field != null ? this.field : this.methodParameter.getMember());
    }

    public AnnotatedElement getAnnotatedElement() {
        return (this.field != null ? this.field : this.methodParameter.getAnnotatedElement());
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        InjectionPoint otherPoint = (InjectionPoint) other;
        return (this.field != null ? this.field.equals(otherPoint.field) :
                this.methodParameter.equals(otherPoint.methodParameter));
    }

    @Override
    public int hashCode() {
        return (this.field != null ? this.field.hashCode() : this.methodParameter.hashCode());
    }

    @Override
    public String toString() {
        return (this.field != null ? "field '" + this.field.getName() + "'" : this.methodParameter.toString());
    }
}
