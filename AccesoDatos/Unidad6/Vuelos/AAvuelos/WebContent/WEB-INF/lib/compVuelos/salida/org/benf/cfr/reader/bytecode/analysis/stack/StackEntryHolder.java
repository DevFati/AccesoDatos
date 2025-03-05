/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.stack;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.stack.StackEntry;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.util.DecompilerComment;

public class StackEntryHolder {
    private StackEntry stackEntry;

    StackEntryHolder(StackType stackType) {
        this.stackEntry = new StackEntry(stackType);
    }

    public void mergeWith(StackEntryHolder other, Set<DecompilerComment> comments) {
        this.stackEntry.mergeWith(other.stackEntry, comments);
        other.stackEntry = this.stackEntry;
    }

    public String toString() {
        return this.stackEntry.toString();
    }

    public StackEntry getStackEntry() {
        return this.stackEntry;
    }
}

