package org.myspring.core.asm;

public class ClassWriter extends ClassVisitor {
    public static final int COMPUTE_MAXS = 1;

    public static final int COMPUTE_FRAMES = 2;

    static final int ACC_SYNTHETIC_ATTRIBUTE = 0x40000;

    static final int TO_ACC_SYNTHETIC = ACC_SYNTHETIC_ATTRIBUTE
            / Opcodes.ACC_SYNTHETIC;

    static final int NOARG_INSN = 0;

    static final int SBYTE_INSN = 1;

    static final int SHORT_INSN = 2;

    static final int VAR_INSN = 3;

    static final int IMPLVAR_INSN = 4;

    static final int TYPE_INSN = 5;

    static final int FIELDORMETH_INSN = 6;

    static final int ITFMETH_INSN = 7;

    static final int INDYMETH_INSN = 8;

    static final int LABEL_INSN = 9;

    static final int LABELW_INSN = 10;

    static final int LDC_INSN = 11;

    static final int LDCW_INSN = 12;

    static final int IINC_INSN = 13;

    static final int TABL_INSN = 14;
    static final int LOOK_INSN = 15;
    static final int MANA_INSN = 16;
    static final int WIDE_INSN = 17;
    static final int ASM_LABEL_INSN = 18;

    static final int ASM_LABELW_INSN = 19;

    static final int F_INSERT = 256;

    static final byte[] TYPE;

    static final int CLASS = 7;

    static final int FIELD = 9;

    static final int METH = 10;

    static final int IMETH = 11;

    static final int STR = 8;

    static final int INT = 3;

    static final int FLOAT = 4;

    static final int LONG = 5;

    static final int DOUBLE = 6;

    static final int NAME_TYPE = 12;

    static final int UTF8 = 1;

    static final int MTYPE = 16;

    static final int HANDLE = 15;

    static final int INDY = 18;

    static final int MODULE = 19;

    static final int PACKAGE = 20;

    static final int HANDLE_BASE = 20;

    static final int TYPE_NORMAL = 30;

    static final int TYPE_UNINIT = 31;

    static final int TYPE_MERGED = 32;

    static final int BSM = 33;

    ClassReader cr;

    int version;
    int index;
    final ByteVector pool;

    Item[] items;

    int threshold;

    final Item key;
    final Item key2;
    final Item key3;
    final Item key4;
    Item[] typeTable;
    private short typeCount;
    private int access;
    private int name;
    String thisName;
    private int signature;
    private int superName;
    private int interfaceCount;
    private int[] interfaces;
    private int sourceFile;
    private ByteVector sourceDebug;
    private ModuleWriter moduleWriter;
    private int enclosingMethodOwner;
    private int enclosingMethod;
    private AnnotationWriter anns;
    private AnnotationWriter ianns;
    private AnnotationWriter tanns;
    private AnnotationWriter itanns;
    private Attribute attrs;
    private int innerClassesCount;
    private ByteVector innerClasses;
    int bootstrapMethodsCount;
    ByteVector bootstrapMethods;
    FieldWriter firstField;
    FieldWriter lastField;
    MethodWriter firstMethod;
    MethodWriter lastMethod;
    private int compute;
    boolean hasAsmInsns;

    static {
        int i;
        byte[] b = new byte[221];
        String s = "AAAAAAAAAAAAAAAABCLMMDDDDDEEEEEEEEEEEEEEEEEEEEAAAAAAAADD"
                + "DDDEEEEEEEEEEEEEEEEEEEEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAANAAAAAAAAAAAAAAAAAAAAJJJJJJJJJJJJJJJJDOPAA"
                + "AAAAGGGGGGGHIFBFAAFFAARQJJKKSSSSSSSSSSSSSSSSSST";
        for (i = 0; i < b.length; ++i) {
            b[i] = (byte) (s.charAt(i) - 'A');
        }
        TYPE = b;

    }

    public ClassWriter(final int flags) {
        super(Opcodes.ASM6);
        index = 1;
        pool = new ByteVector();
        items = new Item[256];
        threshold = (int) (0.75d * items.length);
        key = new Item();
        key2 = new Item();
        key3 = new Item();
        key4 = new Item();
        this.compute = (flags & COMPUTE_FRAMES) != 0 ? MethodWriter.FRAMES
                : ((flags & COMPUTE_MAXS) != 0 ? MethodWriter.MAXS
                : MethodWriter.NOTHING);
    }

    public ClassWriter(final ClassReader classReader, final int flags) {
        this(flags);
        classReader.copyPool(this);
        this.cr = classReader;
    }

    @Override
    public final void visit(final int version, final int access,
                            final String name, final String signature, final String superName,
                            final String[] interfaces) {
        this.version = version;
        this.access = access;
        this.name = newClass(name);
        thisName = name;
        if (ClassReader.SIGNATURES && signature != null) {
            this.signature = newUTF8(signature);
        }
        this.superName = superName == null ? 0 : newClass(superName);
        if (interfaces != null && interfaces.length > 0) {
            interfaceCount = interfaces.length;
            this.interfaces = new int[interfaceCount];
            for (int i = 0; i < interfaceCount; ++i) {
                this.interfaces[i] = newClass(interfaces[i]);
            }
        }
    }

    @Override
    public final void visitSource(final String file, final String debug) {
        if (file != null) {
            sourceFile = newUTF8(file);
        }
        if (debug != null) {
            sourceDebug = new ByteVector().encodeUTF8(debug, 0,
                    Integer.MAX_VALUE);
        }
    }

    @Override
    public final ModuleVisitor visitModule(final String name,
                                           final int access, final String version) {
        return moduleWriter = new ModuleWriter(this,
                newModule(name), access,
                version == null ? 0 : newUTF8(version));
    }

    @Override
    public final void visitOuterClass(final String owner, final String name,
                                      final String desc) {
        enclosingMethodOwner = newClass(owner);
        if (name != null && desc != null) {
            enclosingMethod = newNameType(name, desc);
        }
    }

    @Override
    public final AnnotationVisitor visitAnnotation(final String desc,
                                                   final boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write type, and reserve space for values count
        bv.putShort(newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(this, true, bv, bv, 2);
        if (visible) {
            aw.next = anns;
            anns = aw;
        } else {
            aw.next = ianns;
            ianns = aw;
        }
        return aw;
    }

    @Override
    public final AnnotationVisitor visitTypeAnnotation(int typeRef,
                                                       TypePath typePath, final String desc, final boolean visible) {
        if (!ClassReader.ANNOTATIONS) {
            return null;
        }
        ByteVector bv = new ByteVector();
        // write target_type and target_info
        AnnotationWriter.putTarget(typeRef, typePath, bv);
        // write type, and reserve space for values count
        bv.putShort(newUTF8(desc)).putShort(0);
        AnnotationWriter aw = new AnnotationWriter(this, true, bv, bv,
                bv.length - 2);
        if (visible) {
            aw.next = tanns;
            tanns = aw;
        } else {
            aw.next = itanns;
            itanns = aw;
        }
        return aw;
    }

    @Override
    public final void visitAttribute(final Attribute attr) {
        attr.next = attrs;
        attrs = attr;
    }

    @Override
    public final void visitInnerClass(final String name,
                                      final String outerName, final String innerName, final int access) {
        if (innerClasses == null) {
            innerClasses = new ByteVector();
        }

        Item nameItem = newStringishItem(CLASS, name);
        if (nameItem.intVal == 0) {
            ++innerClassesCount;
            innerClasses.putShort(nameItem.index);
            innerClasses.putShort(outerName == null ? 0 : newClass(outerName));
            innerClasses.putShort(innerName == null ? 0 : newUTF8(innerName));
            innerClasses.putShort(access);
            nameItem.intVal = innerClassesCount;
        } else {
            // Compare the inner classes entry nameItem.intVal - 1 with the
            // arguments of this method and throw an exception if there is a
            // difference?
        }
    }

    @Override
    public final FieldVisitor visitField(final int access, final String name,
                                         final String desc, final String signature, final Object value) {
        return new FieldWriter(this, access, name, desc, signature, value);
    }

    @Override
    public final MethodVisitor visitMethod(final int access, final String name,
                                           final String desc, final String signature, final String[] exceptions) {
        return new MethodWriter(this, access, name, desc, signature,
                exceptions, compute);
    }

    @Override
    public final void visitEnd() {
    }
    public byte[] toByteArray() {
        if (index > 0xFFFF) {
            throw new RuntimeException("Class file too large!");
        }
        // computes the real size of the bytecode of this class
        int size = 24 + 2 * interfaceCount;
        int nbFields = 0;
        FieldWriter fb = firstField;
        while (fb != null) {
            ++nbFields;
            size += fb.getSize();
            fb = (FieldWriter) fb.fv;
        }
        int nbMethods = 0;
        MethodWriter mb = firstMethod;
        while (mb != null) {
            ++nbMethods;
            size += mb.getSize();
            mb = (MethodWriter) mb.mv;
        }
        int attributeCount = 0;
        if (bootstrapMethods != null) {
            // we put it as first attribute in order to improve a bit
            // ClassReader.copyBootstrapMethods
            ++attributeCount;
            size += 8 + bootstrapMethods.length;
            newUTF8("BootstrapMethods");
        }
        if (ClassReader.SIGNATURES && signature != 0) {
            ++attributeCount;
            size += 8;
            newUTF8("Signature");
        }
        if (sourceFile != 0) {
            ++attributeCount;
            size += 8;
            newUTF8("SourceFile");
        }
        if (sourceDebug != null) {
            ++attributeCount;
            size += sourceDebug.length + 6;
            newUTF8("SourceDebugExtension");
        }
        if (enclosingMethodOwner != 0) {
            ++attributeCount;
            size += 10;
            newUTF8("EnclosingMethod");
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            ++attributeCount;
            size += 6;
            newUTF8("Deprecated");
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((version & 0xFFFF) < Opcodes.V1_5
                    || (access & ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                ++attributeCount;
                size += 6;
                newUTF8("Synthetic");
            }
        }
        if (innerClasses != null) {
            ++attributeCount;
            size += 8 + innerClasses.length;
            newUTF8("InnerClasses");
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            ++attributeCount;
            size += 8 + anns.getSize();
            newUTF8("RuntimeVisibleAnnotations");
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            ++attributeCount;
            size += 8 + ianns.getSize();
            newUTF8("RuntimeInvisibleAnnotations");
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            ++attributeCount;
            size += 8 + tanns.getSize();
            newUTF8("RuntimeVisibleTypeAnnotations");
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            ++attributeCount;
            size += 8 + itanns.getSize();
            newUTF8("RuntimeInvisibleTypeAnnotations");
        }
        if (moduleWriter != null) {
            attributeCount += 1 + moduleWriter.attributeCount;
            size += 6 + moduleWriter.size + moduleWriter.attributesSize;
            newUTF8("Module");
        }
        if (attrs != null) {
            attributeCount += attrs.getCount();
            size += attrs.getSize(this, null, 0, -1, -1);
        }
        size += pool.length;
        // allocates a byte vector of this size, in order to avoid unnecessary
        // arraycopy operations in the ByteVector.enlarge() method
        ByteVector out = new ByteVector(size);
        out.putInt(0xCAFEBABE).putInt(version);
        out.putShort(index).putByteArray(pool.data, 0, pool.length);
        int mask = Opcodes.ACC_DEPRECATED | ACC_SYNTHETIC_ATTRIBUTE
                | ((access & ACC_SYNTHETIC_ATTRIBUTE) / TO_ACC_SYNTHETIC);
        out.putShort(access & ~mask).putShort(name).putShort(superName);
        out.putShort(interfaceCount);
        for (int i = 0; i < interfaceCount; ++i) {
            out.putShort(interfaces[i]);
        }
        out.putShort(nbFields);
        fb = firstField;
        while (fb != null) {
            fb.put(out);
            fb = (FieldWriter) fb.fv;
        }
        out.putShort(nbMethods);
        mb = firstMethod;
        while (mb != null) {
            mb.put(out);
            mb = (MethodWriter) mb.mv;
        }
        out.putShort(attributeCount);
        if (bootstrapMethods != null) {
            out.putShort(newUTF8("BootstrapMethods"));
            out.putInt(bootstrapMethods.length + 2).putShort(
                    bootstrapMethodsCount);
            out.putByteArray(bootstrapMethods.data, 0, bootstrapMethods.length);
        }
        if (ClassReader.SIGNATURES && signature != 0) {
            out.putShort(newUTF8("Signature")).putInt(2).putShort(signature);
        }
        if (sourceFile != 0) {
            out.putShort(newUTF8("SourceFile")).putInt(2).putShort(sourceFile);
        }
        if (sourceDebug != null) {
            int len = sourceDebug.length;
            out.putShort(newUTF8("SourceDebugExtension")).putInt(len);
            out.putByteArray(sourceDebug.data, 0, len);
        }
        if (moduleWriter != null) {
            out.putShort(newUTF8("Module"));
            moduleWriter.put(out);
            moduleWriter.putAttributes(out);
        }
        if (enclosingMethodOwner != 0) {
            out.putShort(newUTF8("EnclosingMethod")).putInt(4);
            out.putShort(enclosingMethodOwner).putShort(enclosingMethod);
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            out.putShort(newUTF8("Deprecated")).putInt(0);
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if ((version & 0xFFFF) < Opcodes.V1_5
                    || (access & ACC_SYNTHETIC_ATTRIBUTE) != 0) {
                out.putShort(newUTF8("Synthetic")).putInt(0);
            }
        }
        if (innerClasses != null) {
            out.putShort(newUTF8("InnerClasses"));
            out.putInt(innerClasses.length + 2).putShort(innerClassesCount);
            out.putByteArray(innerClasses.data, 0, innerClasses.length);
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            out.putShort(newUTF8("RuntimeVisibleAnnotations"));
            anns.put(out);
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            out.putShort(newUTF8("RuntimeInvisibleAnnotations"));
            ianns.put(out);
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            out.putShort(newUTF8("RuntimeVisibleTypeAnnotations"));
            tanns.put(out);
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            out.putShort(newUTF8("RuntimeInvisibleTypeAnnotations"));
            itanns.put(out);
        }
        if (attrs != null) {
            attrs.put(this, null, 0, -1, -1, out);
        }
        if (hasAsmInsns) {
            boolean hasFrames = false;
            mb = firstMethod;
            while (mb != null) {
                hasFrames |= mb.frameCount > 0;
                mb = (MethodWriter) mb.mv;
            }
            anns = null;
            ianns = null;
            attrs = null;
            moduleWriter = null;
            innerClassesCount = 0;
            innerClasses = null;
            firstField = null;
            lastField = null;
            firstMethod = null;
            lastMethod = null;
            compute = hasFrames ? MethodWriter.INSERTED_FRAMES : 0;
            hasAsmInsns = false;
            new ClassReader(out.data).accept(this,
                    (hasFrames ? ClassReader.EXPAND_FRAMES : 0)
                            | ClassReader.EXPAND_ASM_INSNS);
            return toByteArray();
        }
        return out.data;
    }
    Item newConstItem(final Object cst) {
        if (cst instanceof Integer) {
            int val = ((Integer) cst).intValue();
            return newInteger(val);
        } else if (cst instanceof Byte) {
            int val = ((Byte) cst).intValue();
            return newInteger(val);
        } else if (cst instanceof Character) {
            int val = ((Character) cst).charValue();
            return newInteger(val);
        } else if (cst instanceof Short) {
            int val = ((Short) cst).intValue();
            return newInteger(val);
        } else if (cst instanceof Boolean) {
            int val = ((Boolean) cst).booleanValue() ? 1 : 0;
            return newInteger(val);
        } else if (cst instanceof Float) {
            float val = ((Float) cst).floatValue();
            return newFloat(val);
        } else if (cst instanceof Long) {
            long val = ((Long) cst).longValue();
            return newLong(val);
        } else if (cst instanceof Double) {
            double val = ((Double) cst).doubleValue();
            return newDouble(val);
        } else if (cst instanceof String) {
            return newStringishItem(STR, (String) cst);
        } else if (cst instanceof Type) {
            Type t = (Type) cst;
            int s = t.getSort();
            if (s == Type.OBJECT) {
                return newStringishItem(CLASS, t.getInternalName());
            } else if (s == Type.METHOD) {
                return newStringishItem(MTYPE, t.getDescriptor());
            } else { // s == primitive type or array
                return newStringishItem(CLASS, t.getDescriptor());
            }
        } else if (cst instanceof Handle) {
            Handle h = (Handle) cst;
            return newHandleItem(h.tag, h.owner, h.name, h.desc, h.itf);
        } else {
            throw new IllegalArgumentException("value " + cst);
        }
    }

    public int newConst(final Object cst) {
        return newConstItem(cst).index;
    }
    public int newUTF8(final String value) {
        key.set(UTF8, value, null, null);
        Item result = get(key);
        if (result == null) {
            pool.putByte(UTF8).putUTF8(value);
            result = new Item(index++, key);
            put(result);
        }
        return result.index;
    }

    Item newStringishItem(final int type, final String value) {
        key2.set(type, value, null, null);
        Item result = get(key2);
        if (result == null) {
            pool.put12(type, newUTF8(value));
            result = new Item(index++, key2);
            put(result);
        }
        return result;
    }

    public int newClass(final String value) {
        return newStringishItem(CLASS, value).index;
    }

    public int newMethodType(final String methodDesc) {
        return newStringishItem(MTYPE, methodDesc).index;
    }

    public int newModule(final String moduleName) {
        return newStringishItem(MODULE, moduleName).index;
    }

    public int newPackage(final String packageName) {
        return newStringishItem(PACKAGE, packageName).index;
    }

    Item newHandleItem(final int tag, final String owner, final String name,
                       final String desc, final boolean itf) {
        key4.set(HANDLE_BASE + tag, owner, name, desc);
        Item result = get(key4);
        if (result == null) {
            if (tag <= Opcodes.H_PUTSTATIC) {
                put112(HANDLE, tag, newField(owner, name, desc));
            } else {
                put112(HANDLE,
                        tag,
                        newMethod(owner, name, desc, itf));
            }
            result = new Item(index++, key4);
            put(result);
        }
        return result;
    }

    @Deprecated
    public int newHandle(final int tag, final String owner, final String name,
                         final String desc) {
        return newHandle(tag, owner, name, desc, tag == Opcodes.H_INVOKEINTERFACE);
    }

    public int newHandle(final int tag, final String owner, final String name,
                         final String desc, final boolean itf) {
        return newHandleItem(tag, owner, name, desc, itf).index;
    }

    Item newInvokeDynamicItem(final String name, final String desc,
                              final Handle bsm, final Object... bsmArgs) {
        // cache for performance
        ByteVector bootstrapMethods = this.bootstrapMethods;
        if (bootstrapMethods == null) {
            bootstrapMethods = this.bootstrapMethods = new ByteVector();
        }

        int position = bootstrapMethods.length; // record current position

        int hashCode = bsm.hashCode();
        bootstrapMethods.putShort(newHandle(bsm.tag, bsm.owner, bsm.name,
                bsm.desc, bsm.isInterface()));

        int argsLength = bsmArgs.length;
        bootstrapMethods.putShort(argsLength);

        for (int i = 0; i < argsLength; i++) {
            Object bsmArg = bsmArgs[i];
            hashCode ^= bsmArg.hashCode();
            bootstrapMethods.putShort(newConst(bsmArg));
        }

        byte[] data = bootstrapMethods.data;
        int length = (1 + 1 + argsLength) << 1; // (bsm + argCount + arguments)
        hashCode &= 0x7FFFFFFF;
        Item result = items[hashCode % items.length];
        loop: while (result != null) {
            if (result.type != BSM || result.hashCode != hashCode) {
                result = result.next;
                continue;
            }

            // because the data encode the size of the argument
            // we don't need to test if these size are equals
            int resultPosition = result.intVal;
            for (int p = 0; p < length; p++) {
                if (data[position + p] != data[resultPosition + p]) {
                    result = result.next;
                    continue loop;
                }
            }
            break;
        }

        int bootstrapMethodIndex;
        if (result != null) {
            bootstrapMethodIndex = result.index;
            bootstrapMethods.length = position; // revert to old position
        } else {
            bootstrapMethodIndex = bootstrapMethodsCount++;
            result = new Item(bootstrapMethodIndex);
            result.set(position, hashCode);
            put(result);
        }

        // now, create the InvokeDynamic constant
        key3.set(name, desc, bootstrapMethodIndex);
        result = get(key3);
        if (result == null) {
            put122(INDY, bootstrapMethodIndex, newNameType(name, desc));
            result = new Item(index++, key3);
            put(result);
        }
        return result;
    }


    public int newInvokeDynamic(final String name, final String desc,
                                final Handle bsm, final Object... bsmArgs) {
        return newInvokeDynamicItem(name, desc, bsm, bsmArgs).index;
    }

    Item newFieldItem(final String owner, final String name, final String desc) {
        key3.set(FIELD, owner, name, desc);
        Item result = get(key3);
        if (result == null) {
            put122(FIELD, newClass(owner), newNameType(name, desc));
            result = new Item(index++, key3);
            put(result);
        }
        return result;
    }

    public int newField(final String owner, final String name, final String desc) {
        return newFieldItem(owner, name, desc).index;
    }

    Item newMethodItem(final String owner, final String name,
                       final String desc, final boolean itf) {
        int type = itf ? IMETH : METH;
        key3.set(type, owner, name, desc);
        Item result = get(key3);
        if (result == null) {
            put122(type, newClass(owner), newNameType(name, desc));
            result = new Item(index++, key3);
            put(result);
        }
        return result;
    }

    public int newMethod(final String owner, final String name,
                         final String desc, final boolean itf) {
        return newMethodItem(owner, name, desc, itf).index;
    }

    Item newInteger(final int value) {
        key.set(value);
        Item result = get(key);
        if (result == null) {
            pool.putByte(INT).putInt(value);
            result = new Item(index++, key);
            put(result);
        }
        return result;
    }

    Item newFloat(final float value) {
        key.set(value);
        Item result = get(key);
        if (result == null) {
            pool.putByte(FLOAT).putInt(key.intVal);
            result = new Item(index++, key);
            put(result);
        }
        return result;
    }

    Item newLong(final long value) {
        key.set(value);
        Item result = get(key);
        if (result == null) {
            pool.putByte(LONG).putLong(value);
            result = new Item(index, key);
            index += 2;
            put(result);
        }
        return result;
    }

    Item newDouble(final double value) {
        key.set(value);
        Item result = get(key);
        if (result == null) {
            pool.putByte(DOUBLE).putLong(key.longVal);
            result = new Item(index, key);
            index += 2;
            put(result);
        }
        return result;
    }

    public int newNameType(final String name, final String desc) {
        return newNameTypeItem(name, desc).index;
    }

    Item newNameTypeItem(final String name, final String desc) {
        key2.set(NAME_TYPE, name, desc, null);
        Item result = get(key2);
        if (result == null) {
            put122(NAME_TYPE, newUTF8(name), newUTF8(desc));
            result = new Item(index++, key2);
            put(result);
        }
        return result;
    }
    int addType(final String type) {
        key.set(TYPE_NORMAL, type, null, null);
        Item result = get(key);
        if (result == null) {
            result = addType(key);
        }
        return result.index;
    }

    int addUninitializedType(final String type, final int offset) {
        key.type = TYPE_UNINIT;
        key.intVal = offset;
        key.strVal1 = type;
        key.hashCode = 0x7FFFFFFF & (TYPE_UNINIT + type.hashCode() + offset);
        Item result = get(key);
        if (result == null) {
            result = addType(key);
        }
        return result.index;
    }
    private Item addType(final Item item) {
        ++typeCount;
        Item result = new Item(typeCount, item);
        put(result);
        if (typeTable == null) {
            typeTable = new Item[16];
        }
        if (typeCount == typeTable.length) {
            Item[] newTable = new Item[2 * typeTable.length];
            System.arraycopy(typeTable, 0, newTable, 0, typeTable.length);
            typeTable = newTable;
        }
        typeTable[typeCount] = result;
        return result;
    }
    int getMergedType(final int type1, final int type2) {
        key2.type = TYPE_MERGED;
        key2.longVal = type1 | (((long) type2) << 32);
        key2.hashCode = 0x7FFFFFFF & (TYPE_MERGED + type1 + type2);
        Item result = get(key2);
        if (result == null) {
            String t = typeTable[type1].strVal1;
            String u = typeTable[type2].strVal1;
            key2.intVal = addType(getCommonSuperClass(t, u));
            result = new Item((short) 0, key2);
            put(result);
        }
        return result.intVal;
    }

    protected String getCommonSuperClass(final String type1, final String type2) {
        Class<?> c, d;
        // SPRING PATCH: PREFER APPLICATION CLASSLOADER
        ClassLoader classLoader = getClassLoader();
        try {
            c = Class.forName(type1.replace('/', '.'), false, classLoader);
            d = Class.forName(type2.replace('/', '.'), false, classLoader);
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
        if (c.isAssignableFrom(d)) {
            return type1;
        }
        if (d.isAssignableFrom(c)) {
            return type2;
        }
        if (c.isInterface() || d.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                c = c.getSuperclass();
            } while (!c.isAssignableFrom(d));
            return c.getName().replace('.', '/');
        }
    }

    protected ClassLoader getClassLoader() {
        ClassLoader classLoader = null;
        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        return (classLoader != null ? classLoader : getClass().getClassLoader());
    }

    private Item get(final Item key) {
        Item i = items[key.hashCode % items.length];
        while (i != null && (i.type != key.type || !key.isEqualTo(i))) {
            i = i.next;
        }
        return i;
    }

    private void put(final Item i) {
        if (index + typeCount > threshold) {
            int ll = items.length;
            int nl = ll * 2 + 1;
            Item[] newItems = new Item[nl];
            for (int l = ll - 1; l >= 0; --l) {
                Item j = items[l];
                while (j != null) {
                    int index = j.hashCode % newItems.length;
                    Item k = j.next;
                    j.next = newItems[index];
                    newItems[index] = j;
                    j = k;
                }
            }
            items = newItems;
            threshold = (int) (nl * 0.75);
        }
        int index = i.hashCode % items.length;
        i.next = items[index];
        items[index] = i;
    }

    private void put122(final int b, final int s1, final int s2) {
        pool.put12(b, s1).putShort(s2);
    }
    private void put112(final int b1, final int b2, final int s) {
        pool.put11(b1, b2).putShort(s);
    }

}
