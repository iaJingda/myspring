package org.myspring.expression.spel.support;

import org.myspring.expression.EvaluationException;
import org.myspring.expression.Operation;
import org.myspring.expression.OperatorOverloader;

public class StandardOperatorOverloader implements OperatorOverloader {

    @Override
    public boolean overridesOperation(Operation operation, Object leftOperand, Object rightOperand)
            throws EvaluationException {
        return false;
    }

    @Override
    public Object operate(Operation operation, Object leftOperand, Object rightOperand) throws EvaluationException {
        throw new EvaluationException("No operation overloaded by default");
    }


}
