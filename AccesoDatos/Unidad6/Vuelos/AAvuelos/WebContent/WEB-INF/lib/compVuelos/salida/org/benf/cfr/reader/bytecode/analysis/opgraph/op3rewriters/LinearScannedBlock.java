/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;

public class LinearScannedBlock {
    private Op03SimpleStatement first;
    private Op03SimpleStatement last;
    private int idxFirst;
    private int idxLast;

    public LinearScannedBlock(Op03SimpleStatement first, Op03SimpleStatement last, int idxFirst, int idxLast) {
        this.first = first;
        this.last = last;
        this.idxFirst = idxFirst;
        this.idxLast = idxLast;
    }

    public Op03SimpleStatement getFirst() {
        return this.first;
    }

    public Op03SimpleStatement getLast() {
        return this.last;
    }

    public int getIdxFirst() {
        return this.idxFirst;
    }

    public int getIdxLast() {
        return this.idxLast;
    }

    public boolean isAfter(LinearScannedBlock other) {
        return this.idxFirst > other.idxLast;
    }

    public boolean immediatelyFollows(LinearScannedBlock other) {
        return this.idxFirst == other.idxLast + 1;
    }

    public void reindex(List<Op03SimpleStatement> in) {
        if (in.get(this.idxFirst) != this.first) {
            this.idxFirst = in.indexOf(this.first);
        }
        if (in.get(this.idxLast) != this.last) {
            this.idxLast = in.indexOf(this.last);
        }
    }
}

