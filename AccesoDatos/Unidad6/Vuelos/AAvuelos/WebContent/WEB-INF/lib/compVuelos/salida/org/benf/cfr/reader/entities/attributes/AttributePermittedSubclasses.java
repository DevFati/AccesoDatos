/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributePermittedSubclasses
extends Attribute {
    public static final String ATTRIBUTE_NAME = "PermittedSubclasses";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private static final long OFFSET_OF_ENTRY_COUNT = 6L;
    private static final long OFFSET_OF_ENTRIES = 8L;
    private final int length;
    private final List<JavaTypeInstance> entries;

    public AttributePermittedSubclasses(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(2L);
        int numEntries = raw.getU2At(6L);
        this.entries = ListFactory.newList();
        long offset = 8L;
        for (int x = 0; x < numEntries; ++x) {
            int entryIdx = raw.getU2At(offset);
            ConstantPoolEntryClass cpec = cp.getClassEntry(entryIdx);
            this.entries.add(cpec.getTypeInstance());
            offset += 2L;
        }
    }

    public List<JavaTypeInstance> getPermitted() {
        return this.entries;
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print(ATTRIBUTE_NAME);
    }

    @Override
    public long getRawByteLength() {
        return 6L + (long)this.length;
    }

    public String toString() {
        return ATTRIBUTE_NAME;
    }
}

