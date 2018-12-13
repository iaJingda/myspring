package org.myspring.expression.spel.ast;

import org.myspring.core.asm.MethodVisitor;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.CodeFlow;

public class LongLiteral extends Literal {
    private final TypedValue value;


    public LongLiteral(String payload, int pos, long value) {
        super(payload, pos);
        this.value = new TypedValue(value);
        this.exitTypeDescriptor = "J";
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
