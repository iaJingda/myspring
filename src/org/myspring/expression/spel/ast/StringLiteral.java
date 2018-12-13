package org.myspring.expression.spel.ast;

import org.myspring.core.asm.MethodVisitor;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.CodeFlow;

public class StringLiteral extends Literal {

    private final TypedValue value;


    public StringLiteral(String payload, int pos, String value) {
        super(payload, pos);
        value = value.substring(1, value.length() - 1);
        this.value = new TypedValue(value.replaceAll("''", "'").replaceAll("\"\"", "\""));
        this.exitTypeDescriptor = "Ljava/lang/String";
    }


    @Override
    public TypedValue getLiteralValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "'" + getLiteralValue().getValue() + "'";
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
