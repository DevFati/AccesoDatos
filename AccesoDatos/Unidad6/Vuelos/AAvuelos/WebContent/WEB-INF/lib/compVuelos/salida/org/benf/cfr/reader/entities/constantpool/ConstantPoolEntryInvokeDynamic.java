/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.constantpool;

import org.benf.cfr.reader.entities.AbstractConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryNameAndType;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class ConstantPoolEntryInvokeDynamic
extends AbstractConstantPoolEntry {
    private static final long OFFSET_OF_BOOTSTRAP_METHOD_ATTR_INDEX = 1L;
    private static final long OFFSET_OF_NAME_AND_TYPE_INDEX = 3L;
    private final int bootstrapMethodAttrIndex;
    private final int nameAndTypeIndex;

    public ConstantPoolEntryInvokeDynamic(ConstantPool cp, ByteData data) {
        super(cp);
        this.bootstrapMethodAttrIndex = data.getU2At(1L);
        this.nameAndTypeIndex = data.getU2At(3L);
    }

    @Override
    public long getRawByteLength() {
        return 5L;
    }

    @Override
    public void dump(Dumper d) {
        d.print(this.toString());
    }

    public int getBootstrapMethodAttrIndex() {
        return this.bootstrapMethodAttrIndex;
    }

    public ConstantPoolEntryNameAndType getNameAndTypeEntry() {
        return this.getCp().getNameAndTypeEntry(this.nameAndTypeIndex);
    }

    public String toString() {
        return "InvokeDynamic value=" + this.bootstrapMethodAttrIndex + "," + this.nameAndTypeIndex;
    }
}

