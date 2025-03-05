/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.exceptions;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.collections.MapFactory;

public class IntervalCount {
    private final TreeMap<Integer, Boolean> op = MapFactory.newTreeMap();

    public Pair<Integer, Integer> generateNonIntersection(Integer from, Integer to) {
        boolean braOutside;
        if (to < from) {
            return null;
        }
        Map.Entry<Integer, Boolean> prevEntry = this.op.floorEntry(from);
        Boolean previous = prevEntry == null ? null : prevEntry.getValue();
        boolean bl = braOutside = previous == null || previous == false;
        if (braOutside) {
            this.op.put(from, true);
        } else {
            from = prevEntry.getKey();
            Map.Entry<Integer, Boolean> nextEntry = this.op.ceilingEntry(from + 1);
            if (nextEntry == null) {
                throw new IllegalStateException("Internal exception pattern invalid");
            }
            if (!nextEntry.getValue().booleanValue() && nextEntry.getKey() >= to) {
                return null;
            }
        }
        NavigableMap<Integer, Boolean> afterMap = this.op.tailMap(from, false);
        Set afterSet = afterMap.entrySet();
        Iterator afterIter = afterSet.iterator();
        while (afterIter.hasNext()) {
            boolean isKet;
            Map.Entry next = afterIter.next();
            Integer end = (Integer)next.getKey();
            boolean bl2 = isKet = Boolean.FALSE == next.getValue();
            if (end > to) {
                if (isKet) {
                    return Pair.make(from, end);
                }
                this.op.put(to, false);
                return Pair.make(from, to);
            }
            if (end.equals(to)) {
                if (isKet) {
                    return Pair.make(from, end);
                }
                afterIter.remove();
                return Pair.make(from, to);
            }
            afterIter.remove();
        }
        this.op.put(to, false);
        return Pair.make(from, to);
    }
}

