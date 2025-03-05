/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.exceptions;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.benf.cfr.reader.entities.exceptions.ClosedIdxExceptionEntry;
import org.benf.cfr.reader.util.collections.ListFactory;

public class IntervalCollisionRemover {
    private final TreeMap<Integer, Boolean> covered = new TreeMap();

    public List<ClosedIdxExceptionEntry> removeIllegals(ClosedIdxExceptionEntry e) {
        List<ClosedIdxExceptionEntry> res = ListFactory.newList();
        int start = e.getStart();
        int end = e.getEnd();
        while (true) {
            Map.Entry<Integer, Boolean> before;
            if ((before = this.covered.floorEntry(start)) == null || !before.getValue().booleanValue()) {
                if (before == null || before.getKey() < start) {
                    this.covered.put(start, true);
                } else {
                    this.covered.remove(start);
                }
                Map.Entry<Integer, Boolean> nextStart = this.covered.ceilingEntry(start + 1);
                if (nextStart == null || nextStart.getKey() > end) {
                    this.covered.put(end + 1, false);
                    res.add(e.withRange(start, end));
                    return res;
                }
                this.covered.remove(nextStart.getKey());
                res.add(e.withRange(start, nextStart.getKey() - 1));
                start = nextStart.getKey();
                continue;
            }
            if (before.getKey().equals(start)) {
                ++start;
            }
            Map.Entry<Integer, Boolean> nextEnd = this.covered.ceilingEntry(start);
            int nextEndIdx = nextEnd.getKey();
            if (nextEnd.getValue().booleanValue()) {
                throw new IllegalStateException();
            }
            if (nextEndIdx > end) {
                return res;
            }
            start = nextEndIdx;
        }
    }
}

