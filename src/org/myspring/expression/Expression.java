package org.myspring.expression;

import org.myspring.core.convert.TypeDescriptor;

public interface Expression {
    String getExpressionString();

    Object getValue() throws EvaluationException;

    <T> T getValue(Class<T> desiredResultType) throws EvaluationException;

    Object getValue(Object rootObject) throws EvaluationException;

    <T> T getValue(Object rootObject, Class<T> desiredResultType) throws EvaluationException;

    Object getValue(EvaluationContext context) throws EvaluationException;

    Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException;

    <T> T getValue(EvaluationContext context, Class<T> desiredResultType) throws EvaluationException;

    <T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType)
            throws EvaluationException;

    Class<?> getValueType() throws EvaluationException;

    Class<?> getValueType(Object rootObject) throws EvaluationException;

    Class<?> getValueType(EvaluationContext context) throws EvaluationException;

    Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException;

    TypeDescriptor getValueTypeDescriptor() throws EvaluationException;

    TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException;

    TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException;

    TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException;

    boolean isWritable(Object rootObject) throws EvaluationException;

    boolean isWritable(EvaluationContext context) throws EvaluationException;

    boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException;

    void setValue(Object rootObject, Object value) throws EvaluationException;

    void setValue(EvaluationContext context, Object value) throws EvaluationException;

    void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException;


}
