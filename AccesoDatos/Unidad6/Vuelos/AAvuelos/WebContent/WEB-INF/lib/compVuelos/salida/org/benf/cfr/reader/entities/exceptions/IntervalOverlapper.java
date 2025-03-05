/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.exceptions;

import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeSet;
import org.benf.cfr.reader.entities.exceptions.ExceptionTableEntry;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class IntervalOverlapper {
    private final NavigableMap<Integer, Set<ExceptionTableEntry>> starts = MapFactory.newTreeMap();
    private final NavigableMap<Integer, Set<ExceptionTableEntry>> ends = MapFactory.newTreeMap();

    IntervalOverlapper(List<ExceptionTableEntry> entries) {
        this.processEntries(entries);
    }

    private void processEntries(List<ExceptionTableEntry> entries) {
        for (ExceptionTableEntry e : entries) {
            this.processEntry(e);
        }
    }

    private static <X> Set<X> razeValues(NavigableMap<?, Set<X>> map) {
        Set res = SetFactory.newOrderedSet();
        if (map.isEmpty()) {
            return res;
        }
        for (Set i : map.values()) {
            res.addAll(i);
        }
        return res;
    }

    private void processEntry(ExceptionTableEntry e) {
        int from = e.getBytecodeIndexFrom();
        int to = e.getBytecodeIndexTo();
        NavigableMap<Integer, Set<ExceptionTableEntry>> startedBeforeStart = this.starts.headMap(from, false);
        NavigableMap<Integer, Set<ExceptionTableEntry>> endsBeforeEnd = this.ends.headMap(to, false);
        NavigableMap<Integer, Set<ExceptionTableEntry>> endsInside = endsBeforeEnd.tailMap(from, false);
        Set<ExceptionTableEntry> overlapStartsBefore = IntervalOverlapper.razeValues(endsInside);
        overlapStartsBefore.retainAll(IntervalOverlapper.razeValues(startedBeforeStart));
        NavigableMap<Integer, Set<ExceptionTableEntry>> endsAfterEnd = this.ends.tailMap(to, false);
        NavigableMap<Integer, Set<ExceptionTableEntry>> startedAfterStart = this.starts.tailMap(from, false);
        NavigableMap<Integer, Set<ExceptionTableEntry>> startsInside = startedAfterStart.headMap(to, false);
        Set<ExceptionTableEntry> overlapEndsAfter = IntervalOverlapper.razeValues(startsInside);
        overlapEndsAfter.retainAll(IntervalOverlapper.razeValues(endsAfterEnd));
        if (overlapEndsAfter.isEmpty() && overlapStartsBefore.isEmpty()) {
            this.addEntry(e);
            return;
        }
        int remainingBlockStart = from;
        int remainingBlockTo = to;
        List output = ListFactory.newList();
        if (!overlapStartsBefore.isEmpty()) {
            Object out;
            TreeSet<Integer> blockEnds = new TreeSet<Integer>();
            for (ExceptionTableEntry e2 : overlapStartsBefore) {
                blockEnds.add(e2.getBytecodeIndexTo());
                ((Set)this.starts.get(e2.getBytecodeIndexFrom())).remove(e2);
                ((Set)this.ends.get(e2.getBytecodeIndexTo())).remove(e2);
            }
            int currentFrom = from;
            for (Integer end : blockEnds) {
                out = e.copyWithRange(currentFrom, end);
                this.addEntry((ExceptionTableEntry)out);
                output.add(out);
                currentFrom = end;
            }
            remainingBlockStart = currentFrom;
            blockEnds.add(from);
            for (ExceptionTableEntry e2 : overlapStartsBefore) {
                Integer end;
                currentFrom = e2.getBytecodeIndexFrom();
                out = blockEnds.iterator();
                while (out.hasNext() && (end = (Integer)out.next()) <= e2.getBytecodeIndexTo()) {
                    ExceptionTableEntry out2 = e2.copyWithRange(currentFrom, end);
                    this.addEntry(out2);
                    output.add(out2);
                    currentFrom = end;
                }
            }
        }
        if (!overlapEndsAfter.isEmpty()) {
            TreeSet<Integer> blockStarts = new TreeSet<Integer>();
            for (ExceptionTableEntry e2 : overlapStartsBefore) {
                blockStarts.add(e2.getBytecodeIndexFrom());
                ((Set)this.starts.get(e2.getBytecodeIndexFrom())).remove(e2);
                ((Set)this.ends.get(e2.getBytecodeIndexTo())).remove(e2);
            }
            List<Integer> revBlockStarts = ListFactory.newList(blockStarts);
            int currentTo = to;
            for (int x = revBlockStarts.size() - 1; x >= 0; --x) {
                Integer start = (Integer)revBlockStarts.get(x);
                ExceptionTableEntry out = e.copyWithRange(start, currentTo);
                this.addEntry(out);
                output.add(out);
                currentTo = start;
            }
            remainingBlockTo = currentTo;
            revBlockStarts.add(to);
            for (ExceptionTableEntry e2 : overlapStartsBefore) {
                Integer start;
                currentTo = e2.getBytecodeIndexTo();
                for (int x = revBlockStarts.size() - 1; x >= 0 && (start = (Integer)revBlockStarts.get(x)) > e2.getBytecodeIndexFrom(); --x) {
                    ExceptionTableEntry out = e.copyWithRange(start, currentTo);
                    this.addEntry(out);
                    output.add(out);
                    currentTo = start;
                }
            }
        }
        ExceptionTableEntry out = e.copyWithRange(remainingBlockStart, remainingBlockTo);
        this.addEntry(out);
        output.add(out);
    }

    private void addEntry(ExceptionTableEntry e) {
        this.add(this.starts, e.getBytecodeIndexFrom(), e);
        this.add(this.ends, e.getBytecodeIndexTo(), e);
    }

    private <A, B> void add(NavigableMap<A, Set<B>> m, A k, B v) {
        Set b = (Set)m.get(k);
        if (b == null) {
            b = SetFactory.newOrderedSet();
            m.put(k, b);
        }
        b.add(v);
    }

    public List<ExceptionTableEntry> getExceptions() {
        return ListFactory.newList(IntervalOverlapper.razeValues(this.starts));
    }
}

