/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.bytestream;

import org.benf.cfr.reader.util.bytestream.ByteData;

public interface OffsettingByteData
extends ByteData {
    public void advance(long var1);

    public long getOffset();
}

