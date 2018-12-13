package org.myspring.expression.spel.ast;

import org.myspring.core.asm.MethodVisitor;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.CodeFlow;

public class IntLiteral extends Literal  {

    private final TypedValue value;


    public IntLiteral(String payload, int pos, int value) {
        super(payload, pos);
        this.value = new TypedValue(value);
        this.exitTypeDescriptor = "I";
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
        int intValue = (Integer) this.value.getValue();
        if (intValue == -1) {
            // Not sure we can get here because -1 is OpMinus
            mv.visitInsn(ICONST_M1);
        }
        else if (intValue >= 0 && intValue < 6) {
            mv.visitInsn(ICONST_0 + intValue);
        }
        else {
            mv.visitLdcInsn(intValue);
        }
        cf.pushDescriptor(this.exitTypeDescriptor);
    }

}
