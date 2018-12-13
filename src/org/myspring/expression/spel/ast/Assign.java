package org.myspring.expression.spel.ast;

import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.ExpressionState;

public class Assign extends SpelNodeImpl {

    public Assign(int pos,SpelNodeImpl... operands) {
        super(pos,operands);
    }


    @Override
    public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
        TypedValue newValue = this.children[1].getValueInternal(state);
        getChild(0).setValue(state, newValue.getValue());
        return newValue;
    }

    @Override
    public String toStringAST() {
        return getChild(0).toStringAST() + "=" + getChild(1).toStringAST();
    }
}
