package org.myspring.expression.spel.ast;

import org.myspring.core.asm.MethodVisitor;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.CodeFlow;

public class FloatLiteral extends Literal {
    private final TypedValue value;


    public FloatLiteral(String payload, int pos, float value) {
        super(payload, pos);
        this.value = new TypedValue(value);
        this.exitTypeDescriptor = "F";
    }


    @Override
    public TypedValue getLiteralValue() {
        return this.value;
    }

    @Override
    public boolean isCompilable() {
        return true;
    }

    @Override
    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        mv.visitLdcInsn(this.value.getValue());
        cf.pushDescriptor(this.exitTypeDescriptor);
    }

}
