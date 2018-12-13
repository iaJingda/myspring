package org.myspring.expression.spel;

import org.myspring.expression.EvaluationContext;
import org.myspring.expression.EvaluationException;

public abstract class CompiledExpression {

    public abstract Object getValue(Object target, EvaluationContext context) throws EvaluationException;

}
