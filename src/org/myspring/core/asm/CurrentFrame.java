package org.myspring.core.asm;

class CurrentFrame extends Frame {

    @Override
    void execute(int opcode, int arg, ClassWriter cw, Item item) {
        super.execute(opcode, arg, cw, item);
        Frame successor = new Frame();
        merge(cw, successor, 0);
        set(successor);
        owner.inputStackTop = 0;
    }


}
