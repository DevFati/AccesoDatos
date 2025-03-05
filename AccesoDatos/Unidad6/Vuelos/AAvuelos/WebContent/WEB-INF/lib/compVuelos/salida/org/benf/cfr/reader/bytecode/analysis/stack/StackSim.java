/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.stack;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.stack.StackDelta;
import org.benf.cfr.reader.bytecode.analysis.stack.StackEntry;
import org.benf.cfr.reader.bytecode.analysis.stack.StackEntryHolder;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.StackTypes;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.collections.ListFactory;

public class StackSim {
    private final StackSim parent;
    private final StackEntryHolder stackEntryHolder;
    private final long depth;

    public StackSim() {
        this.depth = 0L;
        this.parent = null;
        this.stackEntryHolder = null;
    }

    private StackSim(StackSim parent, StackType stackType) {
        this.parent = parent;
        this.depth = parent.depth + 1L;
        this.stackEntryHolder = new StackEntryHolder(stackType);
    }

    public StackEntry getEntry(int depth) {
        StackSim thisSim = this;
        while (depth > 0) {
            thisSim = thisSim.getParent();
            --depth;
        }
        if (thisSim.stackEntryHolder == null) {
            throw new ConfusedCFRException("Underrun type stack");
        }
        return thisSim.stackEntryHolder.getStackEntry();
    }

    public List<StackEntryHolder> getHolders(int offset, long num) {
        StackSim thisSim = this;
        List<StackEntryHolder> res = ListFactory.newList();
        while (num > 0L) {
            if (offset > 0) {
                --offset;
            } else {
                res.add(thisSim.stackEntryHolder);
                --num;
            }
            thisSim = thisSim.getParent();
        }
        return res;
    }

    public long getDepth() {
        return this.depth;
    }

    public StackSim getChange(StackDelta delta, List<StackEntryHolder> consumed, List<StackEntryHolder> produced, Op02WithProcessedDataAndRefs instruction) {
        if (delta.isNoOp()) {
            return this;
        }
        try {
            StackSim thisSim = this;
            StackTypes consumedStack = delta.getConsumed();
            for (StackType stackType : consumedStack) {
                consumed.add(thisSim.stackEntryHolder);
                thisSim = thisSim.getParent();
            }
            StackTypes producedStack = delta.getProduced();
            for (int x = producedStack.size() - 1; x >= 0; --x) {
                thisSim = new StackSim(thisSim, (StackType)((Object)producedStack.get(x)));
            }
            StackSim thatSim = thisSim;
            for (StackType stackType : producedStack) {
                produced.add(thatSim.stackEntryHolder);
                thatSim = thatSim.getParent();
            }
            return thisSim;
        }
        catch (ConfusedCFRException e) {
            throw new ConfusedCFRException("While processing " + instruction + " : " + e.getMessage());
        }
    }

    private StackSim getParent() {
        if (this.parent == null) {
            throw new ConfusedCFRException("Stack underflow");
        }
        return this.parent;
    }

    public String toString() {
        StackSim next = this;
        StringBuilder sb = new StringBuilder();
        while (next != null && next.stackEntryHolder != null) {
            StackEntry stackEntry = next.stackEntryHolder.getStackEntry();
            sb.append(stackEntry).append('[').append((Object)stackEntry.getType()).append("] ");
            next = next.parent;
        }
        return sb.toString();
    }
}

