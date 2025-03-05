/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.bytestream;

import org.benf.cfr.reader.util.bytestream.AbstractBackedByteData;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.bytestream.OffsetBackedByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingByteData;

public class OffsettingBackedByteData
extends AbstractBackedByteData
implements OffsettingByteData {
    private final int originalOffset;
    private int mutableOffset;

    OffsettingBackedByteData(byte[] data, long offset) {
        super(data);
        this.originalOffset = (int)offset;
        this.mutableOffset = 0;
    }

    @Override
    public void advance(long offset) {
        this.mutableOffset = (int)((long)this.mutableOffset + offset);
    }

    @Override
    public long getOffset() {
        return this.mutableOffset;
    }

    @Override
    public ByteData getOffsetData(long offset) {
        return new OffsetBackedByteData(this.d, (long)(this.originalOffset + this.mutableOffset) + offset);
    }

    @Override
    public OffsettingByteData getOffsettingOffsetData(long offset) {
        return new OffsettingBackedByteData(this.d, (long)(this.originalOffset + this.mutableOffset) + offset);
    }

    @Override
    int getRealOffset(int offset) {
        return this.originalOffset + this.mutableOffset + offset;
    }
}

