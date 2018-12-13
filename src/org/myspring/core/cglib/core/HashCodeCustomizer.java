package org.myspring.core.cglib.core;


import org.myspring.core.asm.Type;

public interface HashCodeCustomizer extends KeyFactoryCustomizer {
    /**
     * Customizes calculation of hashcode
     * @param e code emitter
     * @param type parameter type
     */
    boolean customize(CodeEmitter e, Type type);
}
