package org.myspring.expression.spel;

import org.myspring.core.asm.MethodVisitor;
import org.myspring.core.asm.Opcodes;
import org.myspring.expression.PropertyAccessor;

public interface CompilablePropertyAccessor extends PropertyAccessor, Opcodes {

    boolean isCompilable();

    Class<?> getPropertyType();

    void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf);

}
