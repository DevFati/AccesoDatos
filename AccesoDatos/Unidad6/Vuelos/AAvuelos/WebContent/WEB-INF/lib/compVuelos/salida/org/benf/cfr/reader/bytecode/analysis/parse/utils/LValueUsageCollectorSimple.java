/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import java.util.Collection;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.util.collections.SetFactory;

public class LValueUsageCollectorSimple
implements LValueUsageCollector {
    private final Set<LValue> used = SetFactory.newSet();

    @Override
    public void collect(LValue lValue, ReadWrite rw) {
        this.used.add(lValue);
    }

    public Collection<LValue> getUsedLValues() {
        return this.used;
    }

    public boolean isUsed(LValue lValue) {
        return this.used.contains(lValue);
    }
}

