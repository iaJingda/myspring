package org.myspring.expression;

public interface ConstructorExecutor {

    TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException;

}
