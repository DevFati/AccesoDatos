/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

public class Op02RedundantStoreRewriter {
    private static final Op02RedundantStoreRewriter INSTANCE = new Op02RedundantStoreRewriter();

    private Op02RedundantStoreRewriter() {
    }

    private void removeOverwrittenStores(List<Op02WithProcessedDataAndRefs> instrs, int maxLocals) {
        List<Op02WithProcessedDataAndRefs> sources;
        int[] laststore = new int[maxLocals];
        int[] lastload = new int[maxLocals];
        int[] loadsSinceStore = new int[maxLocals];
        int lastCutOff = 0;
        int nopCount = 0;
        HashSet<Object> currentBlocks = new HashSet();
        int maxm1 = instrs.size() - 1;
        for (int x = 0; x < maxm1; ++x) {
            Op02WithProcessedDataAndRefs instr;
            block12: {
                block13: {
                    Pair<JavaTypeInstance, Integer> prevFetched;
                    int laststoreidx;
                    int storeidx;
                    block14: {
                        int lastloadidx;
                        block15: {
                            instr = instrs.get(x);
                            List<Op02WithProcessedDataAndRefs> targets = instr.getTargets();
                            sources = instr.getSources();
                            if (sources.size() != 1 || targets.size() != 1 || targets.get(0) != instrs.get(x + 1)) {
                                lastCutOff = x;
                                continue;
                            }
                            if (!SetUtil.equals(currentBlocks, instr.getContainedInTheseBlocks())) {
                                lastCutOff = x;
                                currentBlocks = new HashSet<BlockIdentifier>(instr.getContainedInTheseBlocks());
                            }
                            JVMInstr jvmInstr = instr.getInstr();
                            Pair<JavaTypeInstance, Integer> stored = instr.getStorageType();
                            if (stored == null) break block12;
                            if (jvmInstr == JVMInstr.IINC || jvmInstr == JVMInstr.IINC_WIDE) {
                                lastCutOff = x;
                                continue;
                            }
                            storeidx = stored.getSecond();
                            if (laststore[storeidx] <= lastCutOff) break block13;
                            laststoreidx = laststore[storeidx];
                            if (lastload[storeidx] <= lastCutOff || loadsSinceStore[storeidx] != 1) break block14;
                            lastloadidx = lastload[storeidx];
                            if (lastloadidx != laststoreidx + 1) break block15;
                            instrs.get(laststoreidx).nop();
                            instrs.get(lastloadidx).nop();
                            nopCount += 2;
                            break block13;
                        }
                        if (lastloadidx != laststoreidx + 2) break block13;
                        instrs.get(laststoreidx).nop();
                        instrs.get(lastloadidx).replaceInstr(JVMInstr.SWAP);
                        ++nopCount;
                        break block13;
                    }
                    if (loadsSinceStore[storeidx] == 0 && (prevFetched = instrs.get(laststoreidx - 1).getRetrieveType()) != null) {
                        if (currentBlocks.isEmpty()) {
                            instrs.get(laststoreidx).nop();
                            instrs.get(laststoreidx - 1).nop();
                            nopCount += 2;
                        } else {
                            Pair<JavaTypeInstance, Integer> thisFetched = instrs.get(x - 1).getRetrieveType();
                            if (thisFetched != null && thisFetched.getSecond().equals(prevFetched.getSecond())) {
                                int n = prevFetched.getSecond();
                                loadsSinceStore[n] = loadsSinceStore[n] - 1;
                                instrs.get(x).nop();
                                instrs.get(x - 1).nop();
                                continue;
                            }
                        }
                    }
                }
                laststore[storeidx] = x;
                loadsSinceStore[storeidx] = 0;
                continue;
            }
            Pair<JavaTypeInstance, Integer> fetched = instr.getRetrieveType();
            if (fetched == null) continue;
            int fetchidx = fetched.getSecond();
            if (laststore[fetchidx] <= lastCutOff) {
                loadsSinceStore[fetchidx] = 0;
            }
            int n = fetchidx;
            loadsSinceStore[n] = loadsSinceStore[n] + 1;
            lastload[fetchidx] = x;
        }
        if (nopCount > 0) {
            Iterator<Op02WithProcessedDataAndRefs> iterator = instrs.iterator();
            iterator.next();
            while (iterator.hasNext()) {
                List<Op02WithProcessedDataAndRefs> targets;
                Op02WithProcessedDataAndRefs instr = iterator.next();
                if (instr.getInstr() != JVMInstr.NOP || (targets = instr.getTargets()).size() != 1) continue;
                Op02WithProcessedDataAndRefs target = targets.get(0);
                targets.clear();
                target.removeSource(instr);
                sources = instr.getSources();
                for (Op02WithProcessedDataAndRefs source : sources) {
                    source.replaceTarget(instr, target);
                    target.addSource(source);
                }
                iterator.remove();
            }
        }
    }

    private void removeUnreadStores(List<Op02WithProcessedDataAndRefs> instrs) {
        Integer idx;
        Set retrieves = SetFactory.newSet();
        for (Op02WithProcessedDataAndRefs op : instrs) {
            idx = op.getRetrieveIdx();
            if (idx == null) continue;
            retrieves.add(idx);
        }
        for (Op02WithProcessedDataAndRefs op : instrs) {
            idx = op.getStoreIdx();
            if (idx == null || retrieves.contains(idx)) continue;
            int category = op.getStorageType().getFirst().getStackType().getComputationCategory();
            if (category == 2) {
                op.replaceInstr(JVMInstr.POP2);
                continue;
            }
            op.replaceInstr(JVMInstr.POP);
        }
    }

    public static void rewrite(List<Op02WithProcessedDataAndRefs> instrs, int maxLocals) {
        INSTANCE.removeUnreadStores(instrs);
        INSTANCE.removeOverwrittenStores(instrs, maxLocals);
    }
}

