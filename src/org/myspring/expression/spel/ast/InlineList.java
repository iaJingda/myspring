package org.myspring.expression.spel.ast;

import org.myspring.core.asm.ClassWriter;
import org.myspring.core.asm.MethodVisitor;
import org.myspring.expression.EvaluationException;
import org.myspring.expression.TypedValue;
import org.myspring.expression.spel.CodeFlow;
import org.myspring.expression.spel.ExpressionState;
import org.myspring.expression.spel.SpelNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InlineList  extends SpelNodeImpl  {

    private TypedValue constant = null;  // TODO must be immutable list


    public InlineList(int pos, SpelNodeImpl... args) {
        super(pos, args);
        checkIfConstant();
    }

    private void checkIfConstant() {
        boolean isConstant = true;
        for (int c = 0, max = getChildCount(); c < max; c++) {
            SpelNode child = getChild(c);
            if (!(child instanceof Literal)) {
                if (child instanceof InlineList) {
                    InlineList inlineList = (InlineList) child;
                    if (!inlineList.isConstant()) {
                        isConstant = false;
                    }
                }
                else {
                    isConstant = false;
                }
            }
        }
        if (isConstant) {
            List<Object> constantList = new ArrayList<Object>();
            int childcount = getChildCount();
            for (int c = 0; c < childcount; c++) {
                SpelNode child = getChild(c);
                if ((child instanceof Literal)) {
                    constantList.add(((Literal) child).getLiteralValue().getValue());
                }
                else if (child instanceof InlineList) {
                    constantList.add(((InlineList) child).getConstantValue());
                }
            }
            this.constant = new TypedValue(Collections.unmodifiableList(constantList));
        }
    }

    @Override
    public TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException {
        if (this.constant != null) {
            return this.constant;
        }
        else {
            List<Object> returnValue = new ArrayList<Object>();
            int childCount = getChildCount();
            for (int c = 0; c < childCount; c++) {
                returnValue.add(getChild(c).getValue(expressionState));
            }
            return new TypedValue(returnValue);
        }
    }

    @Override
    public String toStringAST() {
        StringBuilder sb = new StringBuilder("{");
        // String ast matches input string, not the 'toString()' of the resultant collection, which would use []
        int count = getChildCount();
        for (int c = 0; c < count; c++) {
            if (c > 0) {
                sb.append(",");
            }
            sb.append(getChild(c).toStringAST());
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Return whether this list is a constant value.
     */
    public boolean isConstant() {
        return (this.constant != null);
    }

    @SuppressWarnings("unchecked")
    public List<Object> getConstantValue() {
        return (List<Object>) this.constant.getValue();
    }

    @Override
    public boolean isCompilable() {
        return isConstant();
    }

    @Override
    public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
        final String constantFieldName = "inlineList$" + codeflow.nextFieldId();
        final String className = codeflow.getClassName();

        codeflow.registerNewField(new CodeFlow.FieldAdder() {
            public void generateField(ClassWriter cw, CodeFlow codeflow) {
                cw.visitField(ACC_PRIVATE|ACC_STATIC|ACC_FINAL, constantFieldName, "Ljava/util/List;", null, null);
            }
        });

        codeflow.registerNewClinit(new CodeFlow.ClinitAdder() {
            public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
                generateClinitCode(className, constantFieldName, mv, codeflow, false);
            }
        });

        mv.visitFieldInsn(GETSTATIC, className, constantFieldName, "Ljava/util/List;");
        codeflow.pushDescriptor("Ljava/util/List");
    }

    void generateClinitCode(String clazzname, String constantFieldName, MethodVisitor mv, CodeFlow codeflow, boolean nested) {
        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        if (!nested) {
            mv.visitFieldInsn(PUTSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
        }
        int childCount = getChildCount();
        for (int c = 0; c < childCount; c++) {
            if (!nested) {
                mv.visitFieldInsn(GETSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
            }
            else {
                mv.visitInsn(DUP);
            }
            // The children might be further lists if they are not constants. In this
            // situation do not call back into generateCode() because it will register another clinit adder.
            // Instead, directly build the list here:
            if (children[c] instanceof InlineList) {
                ((InlineList)children[c]).generateClinitCode(clazzname, constantFieldName, mv, codeflow, true);
            }
            else {
                children[c].generateCode(mv, codeflow);
                if (CodeFlow.isPrimitive(codeflow.lastDescriptor())) {
                    CodeFlow.insertBoxIfNecessary(mv, codeflow.lastDescriptor().charAt(0));
                }
            }
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
        }
    }

}
