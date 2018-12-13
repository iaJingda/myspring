package org.myspring.expression.spel.ast;

import org.myspring.core.asm.MethodVisitor;
import org.myspring.core.asm.Type;
import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.CodeFlow;
import org.myspring.expression.spel.ExpressionState;
import org.myspring.expression.spel.SpelEvaluationException;
import org.myspring.expression.spel.SpelMessage;
import org.myspring.expression.spel.support.BooleanTypedValue;

public class OperatorInstanceof extends Operator {

    private Class<?> type;


    public OperatorInstanceof(int pos, SpelNodeImpl... operands) {
        super("instanceof", pos, operands);
    }



    @Override
    public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
        SpelNodeImpl rightOperand = getRightOperand();
        TypedValue left = getLeftOperand().getValueInternal(state);
        TypedValue right = rightOperand.getValueInternal(state);
        Object leftValue = left.getValue();
        Object rightValue = right.getValue();
        BooleanTypedValue result;
        if (rightValue == null || !(rightValue instanceof Class)) {
            throw new SpelEvaluationException(getRightOperand().getStartPosition(),
                    SpelMessage.INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND,
                    (rightValue == null ? "null" : rightValue.getClass().getName()));
        }
        Class<?> rightClass = (Class<?>) rightValue;
        if (leftValue == null) {
            result = BooleanTypedValue.FALSE;  // null is not an instanceof anything
        }
        else {
            result = BooleanTypedValue.forValue(rightClass.isAssignableFrom(leftValue.getClass()));
        }
        this.type = rightClass;
        if (rightOperand instanceof TypeReference) {
            // Can only generate bytecode where the right operand is a direct type reference,
            // not if it is indirect (for example when right operand is a variable reference)
            this.exitTypeDescriptor = "Z";
        }
        return result;
    }

    @Override
    public boolean isCompilable() {
        return (this.exitTypeDescriptor != null && getLeftOperand().isCompilable());
    }

    @Override
    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        getLeftOperand().generateCode(mv, cf);
        CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor());
        if (this.type.isPrimitive()) {
            // always false - but left operand code always driven
            // in case it had side effects
            mv.visitInsn(POP);
            mv.visitInsn(ICONST_0); // value of false
        }
        else {
            mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(this.type));
        }
        cf.pushDescriptor(this.exitTypeDescriptor);
    }

}
