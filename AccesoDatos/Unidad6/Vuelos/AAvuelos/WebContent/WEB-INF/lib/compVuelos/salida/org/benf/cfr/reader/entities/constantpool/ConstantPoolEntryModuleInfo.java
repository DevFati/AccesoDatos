/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.constantpool;

import org.benf.cfr.reader.entities.AbstractConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class ConstantPoolEntryModuleInfo
extends AbstractConstantPoolEntry {
    private static final long OFFSET_OF_NAME_INDEX = 1L;
    private final int nameIndex;

    ConstantPoolEntryModuleInfo(ConstantPool cp, ByteData data) {
        super(cp);
        this.nameIndex = data.getU2At(1L);
    }

    @Override
    public ConstantPool getCp() {
        return super.getCp();
    }

    @Override
    public long getRawByteLength() {
        return 3L;
    }

    @Override
    public void dump(Dumper d) {
        d.print(this.toString());
    }

    public ConstantPoolEntryUTF8 getName() {
        return this.getCp().getUTF8Entry(this.nameIndex);
    }

    public String toString() {
        return "NameIndex value=" + this.nameIndex;
    }
}

