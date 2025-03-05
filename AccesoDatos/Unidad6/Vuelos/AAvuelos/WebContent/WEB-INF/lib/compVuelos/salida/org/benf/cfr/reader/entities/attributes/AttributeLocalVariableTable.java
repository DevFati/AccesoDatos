/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.attributes.LocalVariableEntry;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeLocalVariableTable
extends Attribute {
    public static final String ATTRIBUTE_NAME = "LocalVariableTable";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_ENTRY_COUNT = 6L;
    private static final long OFFSET_OF_ENTRIES = 8L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private final List<LocalVariableEntry> localVariableEntryList = ListFactory.newList();
    private final int length;

    public AttributeLocalVariableTable(ByteData raw) {
        this.length = raw.getS4At(2L);
        int numLocalVariables = raw.getU2At(6L);
        long offset = 8L;
        for (int x = 0; x < numLocalVariables; ++x) {
            int startPc = raw.getU2At(offset);
            int length = raw.getU2At(offset + 2L);
            int nameIndex = raw.getU2At(offset + 4L);
            int descriptorIndex = raw.getU2At(offset + 6L);
            int index = raw.getU2At(offset + 8L);
            this.localVariableEntryList.add(new LocalVariableEntry(startPc, length, nameIndex, descriptorIndex, index));
            offset += 10L;
        }
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d;
    }

    public List<LocalVariableEntry> getLocalVariableEntryList() {
        return this.localVariableEntryList;
    }

    @Override
    public long getRawByteLength() {
        return 6L + (long)this.length;
    }
}

