/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.constantpool;

import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.entities.AbstractConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryLiteral;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolUtils;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class ConstantPoolEntryClass
extends AbstractConstantPoolEntry
implements ConstantPoolEntryLiteral {
    private static final long OFFSET_OF_NAME_INDEX = 1L;
    final int nameIndex;
    transient JavaTypeInstance javaTypeInstance = null;

    public ConstantPoolEntryClass(ConstantPool cp, ByteData data) {
        super(cp);
        this.nameIndex = data.getU2At(1L);
    }

    @Override
    public long getRawByteLength() {
        return 3L;
    }

    public String toString() {
        return "CONSTANT_Class " + this.nameIndex;
    }

    public String getTextPath() {
        return ClassNameUtils.convertFromPath(this.getClassNameString(this.nameIndex)) + ".class";
    }

    public String getFilePath() {
        return this.getClassNameString(this.nameIndex) + ".class";
    }

    private String getClassNameString(int index) {
        return this.getCp().getUTF8Entry(index).getValue();
    }

    @Override
    public void dump(Dumper d) {
        d.print("Class " + this.getClassNameString(this.nameIndex));
    }

    private JavaTypeInstance convertFromString(String rawType) {
        if (rawType.startsWith("[")) {
            return ConstantPoolUtils.decodeTypeTok(rawType, this.getCp());
        }
        return this.getCp().getClassCache().getRefClassFor(ClassNameUtils.convertFromPath(rawType));
    }

    public JavaTypeInstance getTypeInstance() {
        if (this.javaTypeInstance == null) {
            String rawType = this.getClassNameString(this.nameIndex);
            this.javaTypeInstance = this.convertFromString(rawType);
        }
        return this.javaTypeInstance;
    }

    public JavaTypeInstance getTypeInstanceKnownOuter(ConstantPoolEntryClass outer) {
        if (this.javaTypeInstance != null) {
            return this.javaTypeInstance;
        }
        String thisInnerType = this.getClassNameString(this.nameIndex);
        String thisOuterType = this.getClassNameString(outer.nameIndex);
        Pair<JavaRefTypeInstance, JavaRefTypeInstance> pair = this.getCp().getClassCache().getRefClassForInnerOuterPair(thisInnerType, thisOuterType);
        this.javaTypeInstance = pair.getFirst();
        return this.javaTypeInstance;
    }

    public JavaTypeInstance getTypeInstanceKnownInner(ConstantPoolEntryClass inner) {
        if (this.javaTypeInstance != null) {
            return this.javaTypeInstance;
        }
        String thisInnerType = this.getClassNameString(inner.nameIndex);
        String thisOuterType = this.getClassNameString(this.nameIndex);
        Pair<JavaRefTypeInstance, JavaRefTypeInstance> pair = this.getCp().getClassCache().getRefClassForInnerOuterPair(thisInnerType, thisOuterType);
        this.javaTypeInstance = pair.getSecond();
        return this.javaTypeInstance;
    }

    @Override
    public StackType getStackType() {
        return StackType.REF;
    }
}

