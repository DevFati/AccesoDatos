/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.constantpool;

import org.benf.cfr.reader.bytecode.analysis.stack.StackDelta;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.AbstractConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolUtils;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class ConstantPoolEntryNameAndType
extends AbstractConstantPoolEntry {
    private static final long OFFSET_OF_NAME_INDEX = 1L;
    private static final long OFFSET_OF_DESCRIPTOR_INDEX = 3L;
    private final int nameIndex;
    private final int descriptorIndex;
    private StackDelta[] stackDelta = new StackDelta[2];

    public ConstantPoolEntryNameAndType(ConstantPool cp, ByteData data) {
        super(cp);
        this.nameIndex = data.getU2At(1L);
        this.descriptorIndex = data.getU2At(3L);
    }

    @Override
    public long getRawByteLength() {
        return 5L;
    }

    @Override
    public void dump(Dumper d) {
        d.print("CONSTANT_NameAndType nameIndex=" + this.nameIndex + ", descriptorIndex=" + this.descriptorIndex);
    }

    public String toString() {
        return "CONSTANT_NameAndType nameIndex=" + this.nameIndex + ", descriptorIndex=" + this.descriptorIndex;
    }

    public ConstantPoolEntryUTF8 getName() {
        return this.getCp().getUTF8Entry(this.nameIndex);
    }

    public ConstantPoolEntryUTF8 getDescriptor() {
        return this.getCp().getUTF8Entry(this.descriptorIndex);
    }

    public JavaTypeInstance decodeTypeTok() {
        return ConstantPoolUtils.decodeTypeTok(this.getDescriptor().getValue(), this.getCp());
    }

    public StackDelta getStackDelta(boolean member) {
        int idx = member ? 1 : 0;
        ConstantPool cp = this.getCp();
        if (this.stackDelta[idx] == null) {
            this.stackDelta[idx] = ConstantPoolUtils.parseMethodPrototype(member, cp.getUTF8Entry(this.descriptorIndex), cp);
        }
        return this.stackDelta[idx];
    }
}

