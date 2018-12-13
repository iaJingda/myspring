package org.myspring.core.asm;

public final class Handle {

    final int tag;

    final String owner;
    final String name;

    final String desc;
    final boolean itf;
    @Deprecated
    public Handle(int tag, String owner, String name, String desc) {
        this(tag, owner, name, desc, tag == Opcodes.H_INVOKEINTERFACE);
    }

    public Handle(int tag, String owner, String name, String desc, boolean itf) {
        this.tag = tag;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.itf = itf;
    }

    public int getTag() {
        return tag;
    }
    public String getOwner() {
        return owner;
    }
    public String getName() {
        return name;
    }
    public String getDesc() {
        return desc;
    }
    public boolean isInterface() {
        return itf;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Handle)) {
            return false;
        }
        Handle h = (Handle) obj;
        return tag == h.tag && itf == h.itf && owner.equals(h.owner)
                && name.equals(h.name) && desc.equals(h.desc);
    }

    @Override
    public int hashCode() {
        return tag + (itf? 64: 0) + owner.hashCode() * name.hashCode() * desc.hashCode();
    }

    @Override
    public String toString() {
        return owner + '.' + name + desc + " (" + tag + (itf? " itf": "") + ')';
    }
}
