/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.bytestream;

import org.benf.cfr.reader.util.bytestream.AbstractBackedByteData;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.bytestream.OffsetBackedByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingBackedByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingByteData;

public class BaseByteData
extends AbstractBackedByteData {
    public BaseByteData(byte[] data) {
        super(data);
    }

    @Override
    public ByteData getOffsetData(long offset) {
        return new OffsetBackedByteData(this.d, offset);
    }

    @Override
    public OffsettingByteData getOffsettingOffsetData(long offset) {
        return new OffsettingBackedByteData(this.d, offset);
    }

    @Override
    int getRealOffset(int offset) {
        return offset;
    }
}

