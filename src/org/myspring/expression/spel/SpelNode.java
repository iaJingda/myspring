package org.myspring.expression.spel;

import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypedValue;

public interface SpelNode {

    Object getValue(ExpressionState expressionState) throws EvaluationException;

    TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException;

    boolean isWritable(ExpressionState expressionState) throws EvaluationException;

    void setValue(ExpressionState expressionState, Object newValue) throws EvaluationException;

    String toStringAST();

    int getChildCount();

    SpelNode getChild(int index);


    Class<?> getObjectClass(Object obj);

    int getStartPosition();

    int getEndPosition();

}
