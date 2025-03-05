/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entityfactories;

import java.util.List;
import org.benf.cfr.reader.util.KnowsRawSize;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingByteData;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class ContiguousEntityFactory {
    public static <X extends KnowsRawSize> long build(ByteData raw, int count, List<X> tgt, UnaryFunction<ByteData, X> func) {
        OffsettingByteData data = raw.getOffsettingOffsetData(0L);
        for (int x = 0; x < count; ++x) {
            KnowsRawSize tmp = (KnowsRawSize)func.invoke(data);
            tgt.add(tmp);
            data.advance(tmp.getRawByteLength());
        }
        return data.getOffset();
    }

    public static <X> long buildSized(ByteData raw, int count, int itemLength, List<X> tgt, UnaryFunction<ByteData, X> func) {
        OffsettingByteData data = raw.getOffsettingOffsetData(0L);
        for (int x = 0; x < count; x = (int)((short)(x + 1))) {
            X tmp = func.invoke(data);
            tgt.add(tmp);
            data.advance(itemLength);
        }
        return data.getOffset();
    }
}

