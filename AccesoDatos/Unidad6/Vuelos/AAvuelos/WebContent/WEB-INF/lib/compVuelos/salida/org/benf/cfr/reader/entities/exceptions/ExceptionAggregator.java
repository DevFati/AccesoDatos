/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.exceptions;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.entities.exceptions.ExceptionTableEntry;
import org.benf.cfr.reader.entities.exceptions.IntervalCount;
import org.benf.cfr.reader.entities.exceptions.IntervalOverlapper;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class ExceptionAggregator {
    private final List<ExceptionGroup> exceptionsByRange = ListFactory.newList();
    private final Map<Integer, Integer> lutByOffset;
    private final List<Op01WithProcessedDataAndByteJumps> instrs;
    private final boolean aggressiveAggregate;
    private final boolean aggressiveAggregate2;
    private final boolean removedLoopingExceptions;

    private boolean canExtendTo(ExceptionTableEntry a, ExceptionTableEntry b, DecompilerComments comments) {
        int startNext = b.getBytecodeIndexFrom();
        int current = a.getBytecodeIndexTo();
        if (current > startNext) {
            return false;
        }
        boolean veryAggressive = false;
        block4: while (current < startNext) {
            Integer idx = this.lutByOffset.get(current);
            if (idx == null) {
                return false;
            }
            Op01WithProcessedDataAndByteJumps op = this.instrs.get(idx);
            JVMInstr instr = op.getJVMInstr();
            if (instr.isNoThrow()) {
                current += op.getInstructionLength();
                continue;
            }
            if (this.aggressiveAggregate) {
                switch (instr) {
                    case IASTORE: 
                    case IALOAD: 
                    case DASTORE: 
                    case DALOAD: 
                    case FASTORE: 
                    case FALOAD: 
                    case AASTORE: 
                    case AALOAD: {
                        if (!this.aggressiveAggregate2) {
                            return false;
                        }
                        veryAggressive = true;
                    }
                    case GETSTATIC: {
                        current += op.getInstructionLength();
                        continue block4;
                    }
                }
                return false;
            }
            return false;
        }
        if (veryAggressive) {
            comments.addComment(DecompilerComment.AGGRESSIVE_EXCEPTION_VERY_AGG);
        }
        return true;
    }

    private static int canExpandTryBy(int idx, List<Op01WithProcessedDataAndByteJumps> statements) {
        Op01WithProcessedDataAndByteJumps op = statements.get(idx);
        JVMInstr instr = op.getJVMInstr();
        switch (instr) {
            case GOTO: 
            case GOTO_W: 
            case RETURN: 
            case ARETURN: 
            case IRETURN: 
            case LRETURN: 
            case DRETURN: 
            case FRETURN: {
                return op.getInstructionLength();
            }
            case ALOAD: 
            case ALOAD_0: 
            case ALOAD_1: 
            case ALOAD_2: 
            case ALOAD_3: {
                Op01WithProcessedDataAndByteJumps op2 = statements.get(idx + 1);
                if (op2.getJVMInstr() != JVMInstr.MONITOREXIT) break;
                return op.getInstructionLength() + op2.getInstructionLength();
            }
        }
        return 0;
    }

    public ExceptionAggregator(List<ExceptionTableEntry> rawExceptions, BlockIdentifierFactory blockIdentifierFactory, Map<Integer, Integer> lutByOffset, List<Op01WithProcessedDataAndByteJumps> instrs, Options options, ConstantPool cp, DecompilerComments comments) {
        this.lutByOffset = lutByOffset;
        this.instrs = instrs;
        this.aggressiveAggregate = options.getOption(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG) == Troolean.TRUE;
        this.aggressiveAggregate2 = options.getOption(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG2) == Troolean.TRUE;
        List<ExceptionTableEntry> tmpExceptions = Functional.filter(rawExceptions, new ValidException());
        boolean removedLoopingExceptions = false;
        if (tmpExceptions.size() != rawExceptions.size()) {
            rawExceptions = tmpExceptions;
            removedLoopingExceptions = true;
        }
        this.removedLoopingExceptions = removedLoopingExceptions;
        if (rawExceptions.isEmpty()) {
            return;
        }
        List extended = ListFactory.newList();
        for (ExceptionTableEntry exceptionTableEntry : rawExceptions) {
            ExceptionTableEntry exceptionTableEntryOrig;
            int n = exceptionTableEntry.getBytecodeIndexTo();
            do {
                exceptionTableEntryOrig = exceptionTableEntry;
                Integer tgtIdx = lutByOffset.get(n);
                if (tgtIdx == null) continue;
                int offset = ExceptionAggregator.canExpandTryBy(tgtIdx, instrs);
                if (offset != 0) {
                    int bytecodeIndexFrom = exceptionTableEntry.getBytecodeIndexFrom();
                    int bytecodeIndexTo = exceptionTableEntry.getBytecodeIndexTo() + offset;
                    exceptionTableEntry = exceptionTableEntry.copyWithRange(bytecodeIndexFrom, bytecodeIndexTo);
                }
                n += offset;
            } while (exceptionTableEntry != exceptionTableEntryOrig);
            int handlerIndex = exceptionTableEntry.getBytecodeIndexHandler();
            n = exceptionTableEntry.getBytecodeIndexTo();
            int indexFrom = exceptionTableEntry.getBytecodeIndexFrom();
            if (indexFrom < handlerIndex && n >= handlerIndex) {
                exceptionTableEntry = exceptionTableEntry.copyWithRange(indexFrom, handlerIndex);
            }
            extended.add(exceptionTableEntry);
        }
        rawExceptions = extended;
        Map<Integer, List<ExceptionTableEntry>> grouped = Functional.groupToMapBy(rawExceptions, new UnaryFunction<ExceptionTableEntry, Integer>(){

            @Override
            public Integer invoke(ExceptionTableEntry arg) {
                return arg.getCatchType();
            }
        });
        List processedExceptions = ListFactory.newList(rawExceptions.size());
        for (List list : grouped.values()) {
            IntervalCount intervalCount = new IntervalCount();
            for (ExceptionTableEntry e : list) {
                int from = e.getBytecodeIndexFrom();
                int to = e.getBytecodeIndexTo();
                Pair<Integer, Integer> res = intervalCount.generateNonIntersection(from, to);
                if (res == null) continue;
                processedExceptions.add(new ExceptionTableEntry(res.getFirst(), res.getSecond(), e.getBytecodeIndexHandler(), e.getCatchType(), e.getPriority()));
            }
        }
        List<ByTarget> byTargetList = Functional.groupBy(processedExceptions, new Comparator<ExceptionTableEntry>(){

            @Override
            public int compare(ExceptionTableEntry exceptionTableEntry, ExceptionTableEntry exceptionTableEntry1) {
                int hd = exceptionTableEntry.getBytecodeIndexHandler() - exceptionTableEntry1.getBytecodeIndexHandler();
                if (hd != 0) {
                    return hd;
                }
                return exceptionTableEntry.getCatchType() - exceptionTableEntry1.getCatchType();
            }
        }, new UnaryFunction<List<ExceptionTableEntry>, ByTarget>(){

            @Override
            public ByTarget invoke(List<ExceptionTableEntry> arg) {
                return new ByTarget(arg);
            }
        });
        rawExceptions = ListFactory.newList();
        Map map = MapFactory.newMap();
        for (ByTarget t : byTargetList) {
            map.put(((ExceptionTableEntry)t.entries.get(0)).getBytecodeIndexHandler(), t);
        }
        for (ByTarget byTarget : byTargetList) {
            rawExceptions.addAll(byTarget.getAggregated(comments));
        }
        IntervalOverlapper intervalOverlapper = new IntervalOverlapper(rawExceptions);
        rawExceptions = intervalOverlapper.getExceptions();
        Collections.sort(rawExceptions);
        CompareExceptionTablesByRange compareExceptionTablesByStart = new CompareExceptionTablesByRange();
        ExceptionTableEntry prev = null;
        ExceptionGroup currentGroup = null;
        List rawExceptionsByRange = ListFactory.newList();
        for (ExceptionTableEntry e : rawExceptions) {
            if (prev == null || compareExceptionTablesByStart.compare(e, prev) != 0) {
                currentGroup = new ExceptionGroup(e.getBytecodeIndexFrom(), blockIdentifierFactory.getNextBlockIdentifier(BlockType.TRYBLOCK), cp);
                rawExceptionsByRange.add(currentGroup);
                prev = e;
            }
            currentGroup.add(e);
        }
        this.exceptionsByRange.addAll(rawExceptionsByRange);
    }

    public List<ExceptionGroup> getExceptionsGroups() {
        return this.exceptionsByRange;
    }

    public void removeSynchronisedHandlers(Map<Integer, Integer> lutByIdx) {
        Iterator<ExceptionGroup> groupIterator = this.exceptionsByRange.iterator();
        while (groupIterator.hasNext()) {
            ExceptionGroup group = groupIterator.next();
            group.removeSynchronisedHandlers(this.lutByOffset, lutByIdx, this.instrs);
            if (!group.getEntries().isEmpty()) continue;
            groupIterator.remove();
        }
    }

    public void aggressiveRethrowPruning() {
        Iterator<ExceptionGroup> groupIterator = this.exceptionsByRange.iterator();
        while (groupIterator.hasNext()) {
            Op01WithProcessedDataAndByteJumps handlerStartInstr;
            ExceptionGroup.Entry entry;
            int handler;
            Integer index;
            ExceptionGroup group = groupIterator.next();
            List<ExceptionGroup.Entry> entries = group.getEntries();
            if (entries.size() != 1 || (index = this.lutByOffset.get(handler = (entry = entries.get(0)).getBytecodeIndexHandler())) == null || (handlerStartInstr = this.instrs.get(index)).getJVMInstr() != JVMInstr.ATHROW) continue;
            groupIterator.remove();
        }
    }

    public void aggressiveImpossiblePruning() {
        TreeMap sortedNodes = MapFactory.newTreeMap();
        for (Op01WithProcessedDataAndByteJumps instr : this.instrs) {
            sortedNodes.put(instr.getOriginalRawOffset(), instr);
        }
        Iterator<ExceptionGroup> groupIterator = this.exceptionsByRange.iterator();
        block1: while (groupIterator.hasNext()) {
            Op01WithProcessedDataAndByteJumps item;
            ExceptionGroup group = groupIterator.next();
            int from = group.getBytecodeIndexFrom();
            int to = group.getBytecodeIndexTo();
            Integer fromKey = sortedNodes.floorKey(from);
            Collection content = sortedNodes.tailMap(fromKey, true).values();
            Iterator iterator = content.iterator();
            while (iterator.hasNext() && (item = (Op01WithProcessedDataAndByteJumps)iterator.next()).getOriginalRawOffset() < to) {
                if (item.getJVMInstr().isNoThrow()) continue;
                continue block1;
            }
            groupIterator.remove();
        }
    }

    public boolean RemovedLoopingExceptions() {
        return this.removedLoopingExceptions;
    }

    private static class ValidException
    implements Predicate<ExceptionTableEntry> {
        private ValidException() {
        }

        @Override
        public boolean test(ExceptionTableEntry in) {
            return in.getBytecodeIndexFrom() != in.getBytecodeIndexHandler();
        }
    }

    private class ByTarget {
        private final List<ExceptionTableEntry> entries;

        ByTarget(List<ExceptionTableEntry> entries) {
            this.entries = entries;
        }

        Collection<ExceptionTableEntry> getAggregated(DecompilerComments comments) {
            Collections.sort(this.entries, new CompareExceptionTablesByRange());
            List<ExceptionTableEntry> res = ListFactory.newList();
            ExceptionTableEntry held = null;
            for (ExceptionTableEntry entry : this.entries) {
                if (held == null) {
                    held = entry;
                    continue;
                }
                if (held.getBytecodeIndexTo() == entry.getBytecodeIndexFrom()) {
                    held = held.aggregateWith(entry);
                    continue;
                }
                if (held.getBytecodeIndexFrom() == entry.getBytecodeIndexFrom() && held.getBytecodeIndexTo() <= entry.getBytecodeIndexTo()) {
                    held = entry;
                    continue;
                }
                if (held.getBytecodeIndexFrom() < entry.getBytecodeIndexFrom() && entry.getBytecodeIndexFrom() < held.getBytecodeIndexTo() && entry.getBytecodeIndexTo() > held.getBytecodeIndexTo()) {
                    held = held.aggregateWithLenient(entry);
                    continue;
                }
                if (ExceptionAggregator.this.aggressiveAggregate && ExceptionAggregator.this.canExtendTo(held, entry, comments)) {
                    held = held.aggregateWithLenient(entry);
                    continue;
                }
                res.add(held);
                held = entry;
            }
            if (held != null) {
                res.add(held);
            }
            return res;
        }
    }

    private static class CompareExceptionTablesByRange
    implements Comparator<ExceptionTableEntry> {
        private CompareExceptionTablesByRange() {
        }

        @Override
        public int compare(ExceptionTableEntry exceptionTableEntry, ExceptionTableEntry exceptionTableEntry1) {
            int res = exceptionTableEntry.getBytecodeIndexFrom() - exceptionTableEntry1.getBytecodeIndexFrom();
            if (res != 0) {
                return res;
            }
            return exceptionTableEntry.getBytecodeIndexTo() - exceptionTableEntry1.getBytecodeIndexTo();
        }
    }
}

