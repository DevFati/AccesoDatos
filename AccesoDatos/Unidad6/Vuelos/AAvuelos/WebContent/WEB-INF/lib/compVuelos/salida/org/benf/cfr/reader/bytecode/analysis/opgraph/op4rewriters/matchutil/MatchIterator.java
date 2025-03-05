/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;

public class MatchIterator<T> {
    private final List<T> data;
    private int idx;

    public MatchIterator(List<T> data) {
        this.data = data;
        this.idx = -1;
    }

    private MatchIterator(List<T> data, int idx) {
        this.data = data;
        this.idx = idx;
    }

    public T getCurrent() {
        if (this.idx < 0) {
            throw new IllegalStateException("Accessed before being advanced.");
        }
        if (this.idx >= this.data.size()) {
            throw new IllegalStateException("Out of range");
        }
        return this.data.get(this.idx);
    }

    public MatchIterator<T> copy() {
        return new MatchIterator<T>(this.data, this.idx);
    }

    void advanceTo(MatchIterator<StructuredStatement> other) {
        if (this.data != other.data) {
            throw new IllegalStateException();
        }
        this.idx = other.idx;
    }

    public boolean hasNext() {
        return this.idx < this.data.size() - 1;
    }

    private boolean isFinished() {
        return this.idx >= this.data.size();
    }

    public boolean advance() {
        if (!this.isFinished()) {
            ++this.idx;
        }
        return !this.isFinished();
    }

    public void rewind1() {
        if (this.idx > 0) {
            --this.idx;
        }
    }

    public String toString() {
        if (this.data == null) {
            return "Null data!";
        }
        if (this.isFinished()) {
            return "Finished";
        }
        StringBuilder sb = new StringBuilder();
        int dumpIdx = this.idx;
        sb.append(this.idx).append("/").append(this.data.size()).append(" ");
        if (dumpIdx == -1) {
            sb.append("(not yet advanced)");
            dumpIdx = 0;
        }
        int start = Math.max(0, dumpIdx - 3);
        int end = Math.min(this.data.size(), dumpIdx + 3);
        sb.append("[");
        if (start > 0) {
            sb.append("...");
        }
        for (int i = start; i < end; ++i) {
            if (i != start) {
                sb.append(",");
            }
            T t = this.data.get(i);
            sb.append(i).append("#").append(t.getClass().getSimpleName()).append("@").append(Integer.toHexString(t.hashCode()));
        }
        if (end < this.data.size()) {
            sb.append("...");
        }
        sb.append("]");
        return sb.toString();
    }

    public void rewind() {
        this.idx = 0;
    }
}

