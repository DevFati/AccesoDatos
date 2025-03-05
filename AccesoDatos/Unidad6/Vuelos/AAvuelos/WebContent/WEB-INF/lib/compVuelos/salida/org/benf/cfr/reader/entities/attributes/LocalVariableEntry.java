/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

public class LocalVariableEntry {
    private final int startPc;
    private final int length;
    private final int nameIndex;
    private final int descriptorIndex;
    private final int index;

    public LocalVariableEntry(int startPc, int length, int nameIndex, int descriptorIndex, int index) {
        this.startPc = startPc;
        this.length = length;
        this.nameIndex = nameIndex;
        this.descriptorIndex = descriptorIndex;
        this.index = index;
    }

    public int getStartPc() {
        return this.startPc;
    }

    public int getEndPc() {
        return this.startPc + this.length;
    }

    public int getLength() {
        return this.length;
    }

    public int getNameIndex() {
        return this.nameIndex;
    }

    public int getDescriptorIndex() {
        return this.descriptorIndex;
    }

    public int getIndex() {
        return this.index;
    }
}

