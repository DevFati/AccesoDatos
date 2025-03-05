/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.bytestream;

import org.benf.cfr.reader.util.bytestream.AbstractBackedByteData;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingBackedByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingByteData;

public class OffsetBackedByteData
extends AbstractBackedByteData {
    private final int offset;

    OffsetBackedByteData(byte[] data, long offset) {
        super(data);
        this.offset = (int)offset;
    }

    @Override
    public ByteData getOffsetData(long offset) {
        return new OffsetBackedByteData(this.d, (long)this.offset + offset);
    }

    @Override
    public OffsettingByteData getOffsettingOffsetData(long offset) {
        return new OffsettingBackedByteData(this.d, (long)this.offset + offset);
    }

    @Override
    int getRealOffset(int o) {
        return o + this.offset;
    }
}

