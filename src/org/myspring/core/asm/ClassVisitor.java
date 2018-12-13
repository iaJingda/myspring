package org.myspring.core.asm;

public  abstract class ClassVisitor {

    protected final int api;

    protected ClassVisitor cv;

    public ClassVisitor(final int api) {
        this(api, null);
    }

    public ClassVisitor(final int api, final ClassVisitor cv) {
        if (api < Opcodes.ASM4 || api > Opcodes.ASM6) {
            throw new IllegalArgumentException();
        }
        this.api = api;
        this.cv = cv;
    }
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        if (cv != null) {
            cv.visit(version, access, name, signature, superName, interfaces);
        }
    }

    public void visitSource(String source, String debug) {
        if (cv != null) {
            cv.visitSource(source, debug);
        }
    }

    public ModuleVisitor visitModule(String name, int access, String version) {
        if (api < Opcodes.ASM6) {
            throw new RuntimeException();
        }
        if (cv != null) {
            return cv.visitModule(name, access, version);
        }
        return null;
    }

    public void visitOuterClass(String owner, String name, String desc) {
        if (cv != null) {
            cv.visitOuterClass(owner, name, desc);
        }
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (cv != null) {
            return cv.visitAnnotation(desc, visible);
        }
        return null;
    }

    public AnnotationVisitor visitTypeAnnotation(int typeRef,
                                                 TypePath typePath, String desc, boolean visible) {
		/* SPRING PATCH: REMOVED FOR COMPATIBILITY WITH CGLIB 3.1
        if (api < Opcodes.ASM5) {
            throw new RuntimeException();
        }
        */
        if (cv != null) {
            return cv.visitTypeAnnotation(typeRef, typePath, desc, visible);
        }
        return null;
    }

    public void visitAttribute(Attribute attr) {
        if (cv != null) {
            cv.visitAttribute(attr);
        }
    }

    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
        if (cv != null) {
            cv.visitInnerClass(name, outerName, innerName, access);
        }
    }

    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        if (cv != null) {
            return cv.visitField(access, name, desc, signature, value);
        }
        return null;
    }
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        if (cv != null) {
            return cv.visitMethod(access, name, desc, signature, exceptions);
        }
        return null;
    }

    public void visitEnd() {
        if (cv != null) {
            cv.visitEnd();
        }
    }


}
