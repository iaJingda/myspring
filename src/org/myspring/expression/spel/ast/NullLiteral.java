package org.myspring.expression.spel.ast;

import org.myspring.core.asm.MethodVisitor;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.CodeFlow;

public class NullLiteral  extends Literal  {

    public NullLiteral(int pos) {
        super(null, pos);
        this.exitTypeDescriptor = "Ljava/lang/Object";
    }

    @Override
    public TypedValue getLiteralValue() {
        return TypedValue.NULL;
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public boolean isCompilable() {
        return true;
    }

    @Override
    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        mv.visitInsn(ACONST_NULL);
        cf.pushDescriptor(this.exitTypeDescriptor);
    }
}
