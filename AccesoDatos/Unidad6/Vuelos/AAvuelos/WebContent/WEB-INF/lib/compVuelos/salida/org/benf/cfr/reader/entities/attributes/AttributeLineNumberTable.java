/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.NavigableMap;
import java.util.TreeMap;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeLineNumberTable
extends Attribute {
    public static final String ATTRIBUTE_NAME = "LineNumberTable";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private static final long OFFSET_OF_ENTRY_COUNT = 6L;
    private static final long OFFSET_OF_ENTRIES = 8L;
    private final int length;
    private final NavigableMap<Integer, Integer> entries = new TreeMap<Integer, Integer>();

    public AttributeLineNumberTable(ByteData raw) {
        this.length = raw.getS4At(2L);
        int numLineNumbers = raw.getU2At(6L);
        if (numLineNumbers * 2 <= this.length) {
            long offset = 8L;
            int x = 0;
            while (x < numLineNumbers) {
                int startPc = raw.getU2At(offset);
                int lineNumber = raw.getU2At(offset + 2L);
                this.entries.put(startPc, lineNumber);
                ++x;
                offset += 4L;
            }
        }
    }

    public boolean hasEntries() {
        return !this.entries.isEmpty();
    }

    public int getStartLine() {
        return this.entries.firstEntry().getValue();
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

    public NavigableMap<Integer, Integer> getEntries() {
        return this.entries;
    }
}

