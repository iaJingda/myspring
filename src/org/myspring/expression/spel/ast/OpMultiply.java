package org.myspring.expression.spel.ast;

import org.myspring.core.asm.MethodVisitor;
import org.myspring.core.util.NumberUtils;
import org.myspring.expression.EvaluationException;
import org.myspring.expression.Operation;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.CodeFlow;
import org.myspring.expression.spel.ExpressionState;

import java.math.BigDecimal;
import java.math.BigInteger;

public class OpMultiply extends Operator {
    public OpMultiply(int pos, SpelNodeImpl... operands) {
        super("*", pos, operands);
    }

    @Override
    public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
        Object leftOperand = getLeftOperand().getValueInternal(state).getValue();
        Object rightOperand = getRightOperand().getValueInternal(state).getValue();

        if (leftOperand instanceof Number && rightOperand instanceof Number) {
            Number leftNumber = (Number) leftOperand;
            Number rightNumber = (Number) rightOperand;

            if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
                BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
                BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
                return new TypedValue(leftBigDecimal.multiply(rightBigDecimal));
            }
            else if (leftNumber instanceof Double || rightNumber instanceof Double) {
                this.exitTypeDescriptor = "D";
                return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
            }
            else if (leftNumber instanceof Float || rightNumber instanceof Float) {
                this.exitTypeDescriptor = "F";
                return new TypedValue(leftNumber.floatValue() * rightNumber.floatValue());
            }
            else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
                BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
                BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
                return new TypedValue(leftBigInteger.multiply(rightBigInteger));
            }
            else if (leftNumber instanceof Long || rightNumber instanceof Long) {
                this.exitTypeDescriptor = "J";
                return new TypedValue(leftNumber.longValue() * rightNumber.longValue());
            }
            else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
                this.exitTypeDescriptor = "I";
                return new TypedValue(leftNumber.intValue() * rightNumber.intValue());
            }
            else {
                // Unknown Number subtypes -> best guess is double multiplication
                return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
            }
        }

        if (leftOperand instanceof String && rightOperand instanceof Integer) {
            int repeats = (Integer) rightOperand;
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < repeats; i++) {
                result.append(leftOperand);
            }
            return new TypedValue(result.toString());
        }

        return state.operate(Operation.MULTIPLY, leftOperand, rightOperand);
    }

    @Override
    public boolean isCompilable() {
        if (!getLeftOperand().isCompilable()) {
            return false;
        }
        if (this.children.length > 1) {
            if (!getRightOperand().isCompilable()) {
                return false;
            }
        }
        return (this.exitTypeDescriptor != null);
    }

    @Override
    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        getLeftOperand().generateCode(mv, cf);
        String leftDesc = getLeftOperand().exitTypeDescriptor;
        CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, this.exitTypeDescriptor.charAt(0));
        if (this.children.length > 1) {
            cf.enterCompilationScope();
            getRightOperand().generateCode(mv, cf);
            String rightDesc = getRightOperand().exitTypeDescriptor;
            cf.exitCompilationScope();
            CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, this.exitTypeDescriptor.charAt(0));
            switch (this.exitTypeDescriptor.charAt(0)) {
                case 'I':
                    mv.visitInsn(IMUL);
                    break;
                case 'J':
                    mv.visitInsn(LMUL);
                    break;
                case 'F':
                    mv.visitInsn(FMUL);
                    break;
                case 'D':
                    mv.visitInsn(DMUL);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
            }
        }
        cf.pushDescriptor(this.exitTypeDescriptor);
    }

}
