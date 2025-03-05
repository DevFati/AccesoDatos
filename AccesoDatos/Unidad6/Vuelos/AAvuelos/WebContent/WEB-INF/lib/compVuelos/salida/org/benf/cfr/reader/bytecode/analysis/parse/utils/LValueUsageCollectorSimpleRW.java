/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.util.collections.SetFactory;

public class LValueUsageCollectorSimpleRW
implements LValueUsageCollector {
    private final Set<LValue> read = SetFactory.newSet();
    private final Set<LValue> write = SetFactory.newSet();

    @Override
    public void collect(LValue lValue, ReadWrite rw) {
        switch (rw) {
            case READ: {
                this.read.add(lValue);
                break;
            }
            case READ_WRITE: {
                this.read.add(lValue);
            }
            case WRITE: {
                this.write.add(lValue);
            }
        }
    }

    public Set<LValue> getRead() {
        return this.read;
    }

    public Set<LValue> getWritten() {
        return this.write;
    }
}

