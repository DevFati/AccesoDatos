/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.stack;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class StackEntry {
    private static final AtomicLong sid = new AtomicLong(0L);
    private final long id0;
    private final Set<Long> ids = SetFactory.newSet();
    private int artificalSourceCount = 0;
    private final StackSSALabel lValue;
    private long usageCount = 0L;
    private final StackType stackType;
    private final InferredJavaType inferredJavaType = new InferredJavaType();

    StackEntry(StackType stackType) {
        this.id0 = sid.getAndIncrement();
        this.ids.add(this.id0);
        this.lValue = new StackSSALabel(this.id0, this);
        this.stackType = stackType;
    }

    public void incrementUsage() {
        ++this.usageCount;
    }

    public void decrementUsage() {
        --this.usageCount;
    }

    public void forceUsageCount(long newCount) {
        this.usageCount = newCount;
    }

    void mergeWith(StackEntry other, Set<DecompilerComment> comments) {
        if (other.stackType != this.stackType) {
            comments.add(DecompilerComment.UNVERIFIABLE_BYTECODE_BAD_MERGE);
        }
        this.ids.addAll(other.ids);
        this.usageCount += other.usageCount;
    }

    public long getUsageCount() {
        return this.usageCount;
    }

    public int getSourceCount() {
        return this.ids.size() + this.artificalSourceCount;
    }

    public void incSourceCount() {
        ++this.artificalSourceCount;
    }

    public void decSourceCount() {
        --this.artificalSourceCount;
    }

    public List<Long> getSources() {
        return ListFactory.newList(this.ids);
    }

    public void removeSource(long x) {
        if (!this.ids.remove(x)) {
            throw new ConfusedCFRException("Attempt to remove non existent id");
        }
    }

    public String toString() {
        return "" + this.id0;
    }

    public StackSSALabel getLValue() {
        return this.lValue;
    }

    public StackType getType() {
        return this.stackType;
    }

    public InferredJavaType getInferredJavaType() {
        return this.inferredJavaType;
    }

    public int hashCode() {
        return (int)this.id0;
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof StackEntry)) {
            return false;
        }
        return this.id0 == ((StackEntry)o).id0;
    }
}

