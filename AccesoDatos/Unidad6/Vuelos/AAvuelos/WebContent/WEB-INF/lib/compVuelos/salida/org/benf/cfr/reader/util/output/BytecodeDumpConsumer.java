/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.util.Collection;
import java.util.NavigableMap;
import org.benf.cfr.reader.entities.Method;

public interface BytecodeDumpConsumer {
    public void accept(Collection<Item> var1);

    public static interface Item {
        public Method getMethod();

        public NavigableMap<Integer, Integer> getBytecodeLocs();
    }
}

