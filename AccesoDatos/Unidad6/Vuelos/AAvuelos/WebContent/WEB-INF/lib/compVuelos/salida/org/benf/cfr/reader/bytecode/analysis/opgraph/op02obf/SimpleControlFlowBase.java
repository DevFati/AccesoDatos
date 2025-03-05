/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op02obf;

import java.util.List;
import java.util.SortedMap;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.exceptions.ExceptionAggregator;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.util.collections.ListFactory;

public abstract class SimpleControlFlowBase {
    public void process(Method method, ExceptionAggregator exceptions, List<Op02WithProcessedDataAndRefs> op2list, SortedMap<Integer, Integer> lutByOffset) {
        List<ExceptionGroup> groups = ListFactory.newList(exceptions.getExceptionsGroups());
        for (ExceptionGroup group : groups) {
            Op02WithProcessedDataAndRefs handlerJmp = this.checkHandler(group, op2list, lutByOffset);
            if (handlerJmp == null) continue;
            Integer from = (Integer)lutByOffset.get(group.getBytecodeIndexFrom());
            Integer to = (Integer)lutByOffset.get(group.getBytecodeIndexTo());
            if (from == null || to == null || from >= op2list.size() || to >= op2list.size() || !this.checkTry(op2list, from, to, handlerJmp)) continue;
            exceptions.getExceptionsGroups().remove(group);
        }
    }

    public boolean check(ExceptionAggregator exceptions, List<Op02WithProcessedDataAndRefs> op2list, SortedMap<Integer, Integer> lutByOffset) {
        for (ExceptionGroup group : exceptions.getExceptionsGroups()) {
            Op02WithProcessedDataAndRefs handlerJmp = this.checkHandler(group, op2list, lutByOffset);
            if (handlerJmp == null) continue;
            Integer from = (Integer)lutByOffset.get(group.getBytecodeIndexFrom());
            Integer to = (Integer)lutByOffset.get(group.getBytecodeIndexTo());
            if (from == null || to == null || from >= op2list.size() || to >= op2list.size() || !this.checkTry(op2list, from, to, handlerJmp)) continue;
            return true;
        }
        return false;
    }

    protected Op02WithProcessedDataAndRefs getLastTargetIf(List<Op02WithProcessedDataAndRefs> op2list, Integer start, JVMInstr ... instrs) {
        if (start + instrs.length > op2list.size()) {
            return null;
        }
        for (int x = 0; x < instrs.length; ++x) {
            Op02WithProcessedDataAndRefs instr = op2list.get(start + x);
            if (x > 0 && instr.getSources().size() != 1 && instr.getSources().get(0) != op2list.get(start + x - 1)) {
                return null;
            }
            if (instr.getInstr() == instrs[x]) continue;
            return null;
        }
        return op2list.get(start + instrs.length - 1).getTargets().get(0);
    }

    protected Op02WithProcessedDataAndRefs getLastTargetIf(List<Op02WithProcessedDataAndRefs> op2list, Op02WithProcessedDataAndRefs current, JVMInstr ... instrs) {
        return this.getLastTargetIf(op2list, op2list.indexOf(current), instrs);
    }

    protected Op02WithProcessedDataAndRefs checkHandler(ExceptionGroup group, List<Op02WithProcessedDataAndRefs> op2list, SortedMap<Integer, Integer> lutByOffset) {
        List<ExceptionGroup.Entry> entries = group.getEntries();
        if (entries.size() != 1) {
            return null;
        }
        int handler = entries.get(0).getBytecodeIndexHandler();
        Integer tgtIdx = (Integer)lutByOffset.get(handler);
        if (tgtIdx == null) {
            return null;
        }
        Op02WithProcessedDataAndRefs op = op2list.get(tgtIdx);
        Op02WithProcessedDataAndRefs skipped = this.skipSillyHandler(op);
        if (skipped != op) {
            tgtIdx = op2list.indexOf(skipped);
        }
        return this.checkHandler(op2list, tgtIdx);
    }

    protected Op02WithProcessedDataAndRefs skipSillyHandler(Op02WithProcessedDataAndRefs op) {
        Op02WithProcessedDataAndRefs orig = op;
        while (orig != (op = this.skipOneSillyHandler(op))) {
            orig = op;
        }
        return op;
    }

    protected Op02WithProcessedDataAndRefs skipOneSillyHandler(Op02WithProcessedDataAndRefs op) {
        Op02WithProcessedDataAndRefs next;
        if (op.getInstr() == JVMInstr.DUP && (next = op.getTargets().get(0)).getInstr() == JVMInstr.IFNULL) {
            return next.getTargets().get(0);
        }
        return op;
    }

    protected abstract Op02WithProcessedDataAndRefs checkHandler(List<Op02WithProcessedDataAndRefs> var1, int var2);

    protected abstract boolean checkTry(List<Op02WithProcessedDataAndRefs> var1, int var2, int var3, Op02WithProcessedDataAndRefs var4);
}

