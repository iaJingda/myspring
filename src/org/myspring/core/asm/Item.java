package org.myspring.core.asm;

final class Item {

    int index;
    int type;
    int intVal;
    long longVal;
    String strVal1;
    String strVal2;
    String strVal3;
    int hashCode;
    Item next;

    Item() {
    }

    Item(final int index) {
        this.index = index;
    }

    Item(final int index, final Item i) {
        this.index = index;
        type = i.type;
        intVal = i.intVal;
        longVal = i.longVal;
        strVal1 = i.strVal1;
        strVal2 = i.strVal2;
        strVal3 = i.strVal3;
        hashCode = i.hashCode;
    }

    void set(final int intVal) {
        this.type = ClassWriter.INT;
        this.intVal = intVal;
        this.hashCode = 0x7FFFFFFF & (type + intVal);
    }
    void set(final long longVal) {
        this.type = ClassWriter.LONG;
        this.longVal = longVal;
        this.hashCode = 0x7FFFFFFF & (type + (int) longVal);
    }

    void set(final float floatVal) {
        this.type = ClassWriter.FLOAT;
        this.intVal = Float.floatToRawIntBits(floatVal);
        this.hashCode = 0x7FFFFFFF & (type + (int) floatVal);
    }
    void set(final double doubleVal) {
        this.type = ClassWriter.DOUBLE;
        this.longVal = Double.doubleToRawLongBits(doubleVal);
        this.hashCode = 0x7FFFFFFF & (type + (int) doubleVal);
    }

    void set(final int type, final String strVal1, final String strVal2,
             final String strVal3) {
        this.type = type;
        this.strVal1 = strVal1;
        this.strVal2 = strVal2;
        this.strVal3 = strVal3;
        switch (type) {
            case ClassWriter.CLASS:
                this.intVal = 0;     // intVal of a class must be zero, see visitInnerClass
            case ClassWriter.UTF8:
            case ClassWriter.STR:
            case ClassWriter.MTYPE:
            case ClassWriter.MODULE:
            case ClassWriter.PACKAGE:
            case ClassWriter.TYPE_NORMAL:
                hashCode = 0x7FFFFFFF & (type + strVal1.hashCode());
                return;
            case ClassWriter.NAME_TYPE: {
                hashCode = 0x7FFFFFFF & (type + strVal1.hashCode()
                        * strVal2.hashCode());
                return;
            }
            // ClassWriter.FIELD:
            // ClassWriter.METH:
            // ClassWriter.IMETH:
            // ClassWriter.HANDLE_BASE + 1..9
            default:
                hashCode = 0x7FFFFFFF & (type + strVal1.hashCode()
                        * strVal2.hashCode() * strVal3.hashCode());
        }
    }

    void set(String name, String desc, int bsmIndex) {
        this.type = ClassWriter.INDY;
        this.longVal = bsmIndex;
        this.strVal1 = name;
        this.strVal2 = desc;
        this.hashCode = 0x7FFFFFFF & (ClassWriter.INDY + bsmIndex
                * strVal1.hashCode() * strVal2.hashCode());
    }

    void set(int position, int hashCode) {
        this.type = ClassWriter.BSM;
        this.intVal = position;
        this.hashCode = hashCode;
    }

    boolean isEqualTo(final Item i) {
        switch (type) {
            case ClassWriter.UTF8:
            case ClassWriter.STR:
            case ClassWriter.CLASS:
            case ClassWriter.MODULE:
            case ClassWriter.PACKAGE:
            case ClassWriter.MTYPE:
            case ClassWriter.TYPE_NORMAL:
                return i.strVal1.equals(strVal1);
            case ClassWriter.TYPE_MERGED:
            case ClassWriter.LONG:
            case ClassWriter.DOUBLE:
                return i.longVal == longVal;
            case ClassWriter.INT:
            case ClassWriter.FLOAT:
                return i.intVal == intVal;
            case ClassWriter.TYPE_UNINIT:
                return i.intVal == intVal && i.strVal1.equals(strVal1);
            case ClassWriter.NAME_TYPE:
                return i.strVal1.equals(strVal1) && i.strVal2.equals(strVal2);
            case ClassWriter.INDY: {
                return i.longVal == longVal && i.strVal1.equals(strVal1)
                        && i.strVal2.equals(strVal2);
            }
            // case ClassWriter.FIELD:
            // case ClassWriter.METH:
            // case ClassWriter.IMETH:
            // case ClassWriter.HANDLE_BASE + 1..9
            default:
                return i.strVal1.equals(strVal1) && i.strVal2.equals(strVal2)
                        && i.strVal3.equals(strVal3);
        }
    }


}
