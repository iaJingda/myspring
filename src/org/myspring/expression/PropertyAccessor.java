package org.myspring.expression;

public interface PropertyAccessor {

    Class<?>[] getSpecificTargetClasses();

    boolean canRead(EvaluationContext context, Object target, String name) throws AccessException;

    TypedValue read(EvaluationContext context, Object target, String name) throws AccessException;

    boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException;

    void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException;

}
