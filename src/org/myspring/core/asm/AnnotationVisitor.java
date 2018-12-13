package org.myspring.core.asm;

public abstract class AnnotationVisitor {

    protected final int api;

    protected AnnotationVisitor av;

    public AnnotationVisitor(final int api) {
        this(api, null);
    }

    public AnnotationVisitor(final int api, final AnnotationVisitor av) {
        if (api < Opcodes.ASM4 || api > Opcodes.ASM6) {
            throw new IllegalArgumentException();
        }
        this.api = api;
        this.av = av;
    }

    public void visit(String name, Object value) {
        if (av != null) {
            av.visit(name, value);
        }
    }

    public void visitEnum(String name, String desc, String value) {
        if (av != null) {
            av.visitEnum(name, desc, value);
        }
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
        if (av != null) {
            return av.visitAnnotation(name, desc);
        }
        return null;
    }

    public AnnotationVisitor visitArray(String name) {
        if (av != null) {
            return av.visitArray(name);
        }
        return null;
    }

    public void visitEnd() {
        if (av != null) {
            av.visitEnd();
        }
    }



}
