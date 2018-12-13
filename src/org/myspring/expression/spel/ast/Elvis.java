package org.myspring.expression.spel.ast;

import org.myspring.core.asm.Label;
import org.myspring.core.asm.MethodVisitor;
import org.myspring.core.util.StringUtils;
import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.CodeFlow;
import org.myspring.expression.spel.ExpressionState;

public class Elvis extends SpelNodeImpl  {
    public Elvis(int pos, SpelNodeImpl... args) {
        super(pos, args);
    }

    @Override
    public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
        TypedValue value = this.children[0].getValueInternal(state);
        // If this check is changed, the generateCode method will need changing too
        if (!StringUtils.isEmpty(value.getValue())) {
            return value;
        }
        else {
            TypedValue result = this.children[1].getValueInternal(state);
            computeExitTypeDescriptor();
            return result;
        }
    }

    @Override
    public String toStringAST() {
        return getChild(0).toStringAST() + " ?: " + getChild(1).toStringAST();
    }

    @Override
    public boolean isCompilable() {
        SpelNodeImpl condition = this.children[0];
        SpelNodeImpl ifNullValue = this.children[1];
        return (condition.isCompilable() && ifNullValue.isCompilable() &&
                condition.exitTypeDescriptor != null && ifNullValue.exitTypeDescriptor != null);
    }

    @Override
    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        // exit type descriptor can be null if both components are literal expressions
        computeExitTypeDescriptor();
        cf.enterCompilationScope();
        this.children[0].generateCode(mv, cf);
        CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor().charAt(0));
        cf.exitCompilationScope();
        Label elseTarget = new Label();
        Label endOfIf = new Label();
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNULL, elseTarget);
        // Also check if empty string, as per the code in the interpreted version
        mv.visitInsn(DUP);
        mv.visitLdcInsn("");
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z",false);
        mv.visitJumpInsn(IFEQ, endOfIf);  // if not empty, drop through to elseTarget
        mv.visitLabel(elseTarget);
        mv.visitInsn(POP);
        cf.enterCompilationScope();
        this.children[1].generateCode(mv, cf);
        if (!CodeFlow.isPrimitive(this.exitTypeDescriptor)) {
            CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor().charAt(0));
        }
        cf.exitCompilationScope();
        mv.visitLabel(endOfIf);
        cf.pushDescriptor(this.exitTypeDescriptor);
    }

    private void computeExitTypeDescriptor() {
        if (this.exitTypeDescriptor == null && this.children[0].exitTypeDescriptor != null &&
                this.children[1].exitTypeDescriptor != null) {
            String conditionDescriptor = this.children[0].exitTypeDescriptor;
            String ifNullValueDescriptor = this.children[1].exitTypeDescriptor;
            if (conditionDescriptor.equals(ifNullValueDescriptor)) {
                this.exitTypeDescriptor = conditionDescriptor;
            }
            else {
                // Use the easiest to compute common super type
                this.exitTypeDescriptor = "Ljava/lang/Object";
            }
        }
    }

}
