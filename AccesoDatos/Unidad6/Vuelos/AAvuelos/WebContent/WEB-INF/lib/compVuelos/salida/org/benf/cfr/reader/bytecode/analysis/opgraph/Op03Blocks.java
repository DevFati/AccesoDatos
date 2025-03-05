/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.ExactTypeFilter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CaseStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.FinallyStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheckSimple;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;

public class Op03Blocks {
    private static List<Block3> doTopSort(List<Block3> in) {
        LinkedHashSet<Block3> allBlocks = new LinkedHashSet<Block3>(in);
        TreeSet<Block3> ready = new TreeSet<Block3>();
        ready.add(in.get(0));
        List<Block3> output = ListFactory.newList(in.size());
        while (!allBlocks.isEmpty()) {
            Block3 forceChild;
            Block3 probnext;
            Set<BlockIdentifier> probNextBlocks;
            Block3 next;
            if (!ready.isEmpty()) {
                next = (Block3)SetUtil.getSingle(ready);
                ready.remove(next);
            } else {
                next = SetUtil.getSingle(allBlocks);
            }
            allBlocks.remove(next);
            output.add(next);
            Set<BlockIdentifier> fromSet = next.getEnd().getBlockIdentifiers();
            TreeSet<Block3> notReadyInThis = null;
            for (Block3 child : next.targets) {
                child.sources.remove(next);
                if (child.sources.isEmpty()) {
                    if (!allBlocks.contains(child)) continue;
                    ready.add(child);
                    continue;
                }
                if (!child.getStart().getBlockIdentifiers().equals(fromSet)) continue;
                if (notReadyInThis == null) {
                    notReadyInThis = new TreeSet<Block3>();
                }
                notReadyInThis.add(child);
            }
            if (ready.isEmpty() || !fromSet.containsAll(probNextBlocks = (probnext = (Block3)SetUtil.getSingle(ready)).getStart().getBlockIdentifiers()) || probNextBlocks.equals(fromSet) || notReadyInThis == null || notReadyInThis.isEmpty() || !allBlocks.contains(forceChild = (Block3)SetUtil.getSingle(notReadyInThis))) continue;
            boolean canForce = true;
            for (Block3 forceSource : forceChild.sources) {
                if (!forceChild.startIndex.isBackJumpFrom(forceSource.startIndex) || forceSource.getStart().getBlockIdentifiers().containsAll(fromSet)) continue;
                canForce = false;
                break;
            }
            if (!canForce) continue;
            forceChild.sources.clear();
            ready.add(forceChild);
        }
        return output;
    }

    private static void apply0TargetBlockHeuristic(List<Block3> blocks) {
        for (int idx = blocks.size() - 1; idx >= 0; --idx) {
            Op03SimpleStatement lastop;
            Block3 fallThrough;
            Block3 block = blocks.get(idx);
            if (!block.targets.isEmpty()) continue;
            boolean move = false;
            Block3 lastSource = block;
            for (Block3 source : block.sources) {
                if (lastSource.compareTo(source) >= 0) continue;
                move = true;
                lastSource = source;
            }
            if (!move || idx > 0 && block.sources.contains(fallThrough = blocks.get(idx - 1)) && lastSource != fallThrough && (lastop = fallThrough.getEnd()).getStatement().fallsToNext()) continue;
            block.startIndex = lastSource.startIndex.justAfter();
            blocks.add(blocks.indexOf(lastSource) + 1, block);
            blocks.remove(idx);
        }
    }

    private static void removeAliases(Set<BlockIdentifier> in, Map<BlockIdentifier, BlockIdentifier> aliases) {
        Set toRemove = SetFactory.newSet();
        for (BlockIdentifier i : in) {
            BlockIdentifier alias = aliases.get(i);
            if (alias == null || !in.contains(alias)) continue;
            toRemove.add(i);
            toRemove.add(alias);
        }
        in.removeAll(toRemove);
    }

    private static Map<BlockIdentifier, BlockIdentifier> getTryBlockAliases(List<Op03SimpleStatement> statements) {
        Map<BlockIdentifier, BlockIdentifier> tryBlockAliases = MapFactory.newMap();
        List<Op03SimpleStatement> catchStatements = Functional.filter(statements, new TypeFilter<CatchStatement>(CatchStatement.class));
        block0: for (Op03SimpleStatement catchStatementCtr : catchStatements) {
            CatchStatement catchStatement = (CatchStatement)catchStatementCtr.getStatement();
            List<ExceptionGroup.Entry> caught = catchStatement.getExceptions();
            if (caught.isEmpty()) continue;
            ExceptionGroup.Entry first = caught.get(0);
            JavaRefTypeInstance catchType = first.getCatchType();
            BlockIdentifier tryBlockMain = first.getTryBlockIdentifier();
            List<BlockIdentifier> possibleAliases = ListFactory.newList();
            int len = caught.size();
            for (int x = 1; x < len; ++x) {
                ExceptionGroup.Entry entry = caught.get(x);
                if (!entry.getCatchType().equals(catchType)) continue block0;
                BlockIdentifier tryBlockIdent = entry.getTryBlockIdentifier();
                possibleAliases.add(tryBlockIdent);
            }
            for (Op03SimpleStatement source : catchStatementCtr.getSources()) {
                if (!source.getBlockIdentifiers().contains(tryBlockMain)) continue;
                continue block0;
            }
            for (BlockIdentifier alias : possibleAliases) {
                BlockIdentifier last = tryBlockAliases.put(alias, tryBlockMain);
                if (last == null || last == tryBlockMain) continue;
                MiscUtils.handyBreakPoint();
            }
        }
        return tryBlockAliases;
    }

    private static void applyKnownBlocksHeuristic(List<Block3> blocks, Map<BlockIdentifier, BlockIdentifier> tryBlockAliases) {
        Map lastByBlock = MapFactory.newMap();
        for (Block3 block : blocks) {
            for (BlockIdentifier blockIdentifier : block.getStart().getBlockIdentifiers()) {
                lastByBlock.put(blockIdentifier, block);
            }
        }
        Block3 linPrev = null;
        for (Block3 block : blocks) {
            Block3 source;
            Op03SimpleStatement end;
            Set<BlockIdentifier> endIdents;
            Op03SimpleStatement start = block.getStart();
            Set<BlockIdentifier> startIdents = start.getBlockIdentifiers();
            boolean needLinPrev = false;
            if (linPrev != null && block.sources.contains(linPrev) && !(endIdents = (end = (source = linPrev).getEnd()).getBlockIdentifiers()).equals(startIdents)) {
                Set<BlockIdentifier> diffs = SetUtil.difference(endIdents, startIdents);
                BlockIdentifier newTryBlock = null;
                if (block.getStart().getStatement() instanceof TryStatement && !diffs.add(newTryBlock = ((TryStatement)block.getStart().getStatement()).getBlockIdentifier())) {
                    newTryBlock = null;
                }
                Op03Blocks.removeAliases(diffs, tryBlockAliases);
                for (BlockIdentifier blk : diffs) {
                    if (blk.getBlockType() == BlockType.CASE || blk.getBlockType() == BlockType.SWITCH || blk == newTryBlock) continue;
                    needLinPrev = true;
                    break;
                }
            }
            if (needLinPrev) {
                block.addSource(linPrev);
                linPrev.addTarget(block);
            } else {
                Op03SimpleStatement blockStart = block.getStart();
                Statement statement = blockStart.getStatement();
                if (statement instanceof FinallyStatement) {
                    for (Block3 source2 : ListFactory.newList(block.sources)) {
                        TryStatement tryStatement;
                        Block3 lastDep;
                        Statement last = source2.getEnd().getStatement();
                        if (!(last instanceof TryStatement) || (lastDep = (Block3)lastByBlock.get((tryStatement = (TryStatement)last).getBlockIdentifier())) == null) continue;
                        block.addSource(lastDep);
                        lastDep.addTarget(block);
                    }
                }
            }
            linPrev = block;
        }
    }

    private static List<Block3> buildBasicBlocks(List<Op03SimpleStatement> statements) {
        final List<Block3> blocks = ListFactory.newList();
        final Map starts = MapFactory.newMap();
        final Map ends = MapFactory.newMap();
        GraphVisitorDFS<Op03SimpleStatement> gv = new GraphVisitorDFS<Op03SimpleStatement>(statements.get(0), new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

            @Override
            public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                Op03SimpleStatement next;
                Block3 block = new Block3(arg1);
                starts.put(arg1, block);
                while (arg1.getTargets().size() == 1 && (next = arg1.getTargets().get(0)).getSources().size() == 1 && arg1.getBlockIdentifiers().equals(next.getBlockIdentifiers())) {
                    arg1 = next;
                    block.append(arg1);
                }
                blocks.add(block);
                ends.put(arg1, block);
                arg2.enqueue(arg1.getTargets());
            }
        });
        gv.process();
        Collections.sort(blocks);
        for (Block3 block : blocks) {
            Op03SimpleStatement start = block.getStart();
            List<Op03SimpleStatement> prevList = start.getSources();
            List<Block3> prevBlocks = ListFactory.newList(prevList.size());
            for (Op03SimpleStatement prev : prevList) {
                Block3 prevEnd = (Block3)ends.get(prev);
                if (prevEnd == null) {
                    throw new IllegalStateException("Topological sort failed, explicitly disable");
                }
                prevBlocks.add(prevEnd);
            }
            Op03SimpleStatement end = block.getEnd();
            List<Op03SimpleStatement> afterList = end.getTargets();
            List<Block3> postBlocks = ListFactory.newList(afterList.size());
            for (Op03SimpleStatement after : afterList) {
                postBlocks.add((Block3)starts.get(after));
            }
            block.addSources(prevBlocks);
            block.addTargets(postBlocks);
            if (!(end.getStatement() instanceof TryStatement)) continue;
            List<Block3> depends = ListFactory.newList();
            for (Block3 tgt : postBlocks) {
                tgt.addSources(depends);
                for (Block3 depend : depends) {
                    depend.addTarget(tgt);
                }
                depends.add(tgt);
            }
        }
        return blocks;
    }

    private static boolean detectMoves(List<Block3> blocks, Options options) {
        Map opLocations = MapFactory.newIdentityMap();
        Map<Block3, Integer> idxLut = MapFactory.newIdentityMap();
        int len = blocks.size();
        for (int i = 0; i < len; ++i) {
            Block3 blk = blocks.get(i);
            idxLut.put(blk, i);
            for (Op03SimpleStatement stm : blk.getContent()) {
                opLocations.put(stm, blk);
            }
        }
        BlockIdentifierFactory blockIdentifierFactory = new BlockIdentifierFactory();
        List blockMembers = ListFactory.newList();
        int len2 = blocks.size();
        for (int i = 0; i < len2; ++i) {
            blockMembers.add(SetFactory.newOrderedSet());
        }
        Map firstByBlock = MapFactory.newMap();
        Map lastByBlock = MapFactory.newMap();
        int len3 = blocks.size();
        for (int i = 0; i < len3; ++i) {
            Block3 block = blocks.get(i);
            Block3 lastBackJump = block.getLastUnconditionalBackjumpToHere(idxLut);
            if (lastBackJump == null) continue;
            BlockIdentifier bid = blockIdentifierFactory.getNextBlockIdentifier(BlockType.DOLOOP);
            int last = idxLut.get(lastBackJump);
            for (int x = i + 1; x <= last; ++x) {
                ((Set)blockMembers.get(x)).add(bid);
            }
            firstByBlock.put(bid, block);
            lastByBlock.put(bid, lastBackJump);
        }
        boolean effect = false;
        if (options.getOption(OptionsImpl.FORCE_TOPSORT_EXTRA) == Troolean.TRUE) {
            int len4 = blocks.size();
            block5: for (int i = 0; i < len4; ++i) {
                Set inThese;
                Block3 block = blocks.get(i);
                if (!block.targets.isEmpty() || (inThese = (Set)blockMembers.get(i)).isEmpty()) continue;
                for (Block3 source : block.originalSources) {
                    Set sourceInThese;
                    int j = idxLut.get(source);
                    if (j >= i || (sourceInThese = (Set)blockMembers.get(j)).containsAll(inThese)) continue;
                    Set<BlockIdentifier> tmp = SetFactory.newSet(inThese);
                    tmp.removeAll(sourceInThese);
                    List<Block3> newSources = ListFactory.newList();
                    for (BlockIdentifier jumpedInto : tmp) {
                        if (firstByBlock.get(jumpedInto) == block) continue;
                        newSources.add((Block3)lastByBlock.get(jumpedInto));
                    }
                    if (newSources.isEmpty()) continue;
                    block.addSources(newSources);
                    effect = true;
                    continue block5;
                }
            }
        } else {
            block8: for (Map.Entry entry : firstByBlock.entrySet()) {
                Op03SimpleStatement tgt;
                Block3 blktgt;
                List<Op03SimpleStatement> tgts;
                BlockIdentifier ident = (BlockIdentifier)entry.getKey();
                Block3 block = (Block3)entry.getValue();
                Op03SimpleStatement first = block.getStart();
                Statement statement = first.getStatement();
                if (!(statement instanceof IfStatement) || (tgts = first.getTargets()).size() != 2 || !((Set)blockMembers.get(idxLut.get(blktgt = (Block3)opLocations.get(tgt = tgts.get(1))))).contains(ident) || lastByBlock.get(ident) == blktgt) continue;
                Set<Block3> origSources = SetFactory.newOrderedSet(blktgt.originalSources);
                origSources.remove(block);
                for (Block3 src : origSources) {
                    if ((!((Set)blockMembers.get(idxLut.get(src))).contains(ident) || !src.startIndex.isBackJumpFrom(blktgt.startIndex)) && !src.startIndex.isBackJumpFrom(block.startIndex)) continue;
                    blktgt.addSource((Block3)lastByBlock.get(ident));
                    effect = true;
                    continue block8;
                }
            }
        }
        if (effect) {
            for (Block3 block : blocks) {
                block.copySources();
            }
        }
        return effect;
    }

    private static void stripTryBlockAliases(List<Op03SimpleStatement> out, Map<BlockIdentifier, BlockIdentifier> tryBlockAliases) {
        BlockIdentifier tryBlock;
        Map tries = MapFactory.newMap();
        Set<Op03SimpleStatement> remove = SetFactory.newOrderedSet();
        Set<BlockIdentifier> blocksToRemove = SetFactory.newOrderedSet();
        blocksToRemove.addAll(tryBlockAliases.keySet());
        int len = out.size();
        for (int x = 1; x < len; ++x) {
            Op03SimpleStatement s = out.get(x);
            if (s.getStatement().getClass() != TryStatement.class) continue;
            TryStatement tryStatement = (TryStatement)s.getStatement();
            tryBlock = tryStatement.getBlockIdentifier();
            tries.put(tryBlock, s);
            Op03SimpleStatement prev = out.get(x - 1);
            BlockIdentifier alias = tryBlockAliases.get(tryBlock);
            if (alias == null || !prev.getBlockIdentifiers().contains(alias)) continue;
            remove.add(s);
            blocksToRemove.remove(tryBlock);
        }
        if (!blocksToRemove.isEmpty()) {
            Map<BlockIdentifier, Integer> actualStarts = Op03Blocks.findFirstInBlock(out, blocksToRemove);
            for (Map.Entry<BlockIdentifier, Integer> entry : actualStarts.entrySet()) {
                int x = entry.getValue();
                tryBlock = entry.getKey();
                Op03SimpleStatement tryStm = (Op03SimpleStatement)tries.get(tryBlock);
                if (tryStm == null) continue;
                Op03SimpleStatement prev = out.get(x - 1);
                BlockIdentifier alias = tryBlockAliases.get(tryBlock);
                if (alias == null || !prev.getBlockIdentifiers().contains(alias)) continue;
                remove.add(tryStm);
                blocksToRemove.remove(tryBlock);
            }
        }
        for (Op03SimpleStatement removeThis : remove) {
            TryStatement removeTry = (TryStatement)removeThis.getStatement();
            BlockIdentifier blockIdentifier = removeTry.getBlockIdentifier();
            BlockIdentifier alias = tryBlockAliases.get(blockIdentifier);
            List<Op03SimpleStatement> targets = removeThis.getTargets();
            Op03SimpleStatement naturalTarget = targets.get(0);
            for (Op03SimpleStatement target : targets) {
                target.removeSource(removeThis);
            }
            for (Op03SimpleStatement source : removeThis.getSources()) {
                source.replaceTarget(removeThis, naturalTarget);
                naturalTarget.addSource(source);
            }
            removeThis.clear();
            for (Op03SimpleStatement statement : out) {
                statement.replaceBlockIfIn(blockIdentifier, alias);
            }
        }
    }

    private static Map<BlockIdentifier, Integer> findFirstInBlock(List<Op03SimpleStatement> statements, Set<BlockIdentifier> mutableMissing) {
        int size = statements.size();
        Map<BlockIdentifier, Integer> res = MapFactory.newMap();
        for (int x = 0; x < size; ++x) {
            Op03SimpleStatement stm = statements.get(x);
            Set<BlockIdentifier> stmBlocks = stm.getBlockIdentifiers();
            Iterator<BlockIdentifier> toFind = mutableMissing.iterator();
            while (toFind.hasNext()) {
                BlockIdentifier block = toFind.next();
                if (!stmBlocks.contains(block)) continue;
                toFind.remove();
                res.put(block, x);
                if (!mutableMissing.isEmpty()) continue;
                return res;
            }
        }
        return res;
    }

    public static List<Op03SimpleStatement> combineTryBlocks(List<Op03SimpleStatement> statements) {
        Map<BlockIdentifier, BlockIdentifier> tryBlockAliases = Op03Blocks.getTryBlockAliases(statements);
        Op03Blocks.stripTryBlockAliases(statements, tryBlockAliases);
        return Cleaner.removeUnreachableCode(statements, true);
    }

    private static boolean canCombineBlockSets(Block3 from, Block3 to) {
        Statement stm;
        Set<BlockIdentifier> toBlocks;
        Set<BlockIdentifier> fromBlocks = from.getStart().getBlockIdentifiers();
        if (fromBlocks.equals(toBlocks = to.getStart().getBlockIdentifiers())) {
            return true;
        }
        fromBlocks = from.getEnd().getBlockIdentifiers();
        if (fromBlocks.equals(toBlocks)) {
            return true;
        }
        if (fromBlocks.size() == toBlocks.size() - 1 && (stm = from.getEnd().getStatement()) instanceof CaseStatement) {
            BlockIdentifier caseBlock = ((CaseStatement)stm).getCaseBlock();
            List<BlockIdentifier> diff = SetUtil.differenceAtakeBtoList(toBlocks, fromBlocks);
            if (diff.size() == 1 && diff.get(0) == caseBlock) {
                return true;
            }
        }
        return false;
    }

    private static void sanitiseBlocks(List<Block3> blocks) {
        for (Block3 block : blocks) {
            block.sources.remove(block);
            block.targets.remove(block);
        }
    }

    private static List<Block3> invertJoinZeroTargetJumps(List<Block3> blocks) {
        Map seenPrevBlock = MapFactory.newMap();
        boolean effect = false;
        int len = blocks.size();
        for (int x = 0; x < len; ++x) {
            Op03SimpleStatement notTaken;
            Block3 notTakenPrevBlock;
            Op03SimpleStatement taken;
            Block3 source;
            Op03SimpleStatement sourceEnd;
            Statement statement;
            Block3 block = blocks.get(x);
            if (block.sources.size() != 1 || block.targets.size() != 0) continue;
            if (x > 0) {
                seenPrevBlock.put(block.getStart(), blocks.get(x - 1));
            }
            if ((statement = (sourceEnd = (source = SetUtil.getSingle(block.sources)).getEnd()).getStatement()).getClass() != IfStatement.class) continue;
            IfStatement ifStatement = (IfStatement)statement;
            List<Op03SimpleStatement> targets = sourceEnd.getTargets();
            if (targets.size() != 2 || (taken = targets.get(1)) != block.getStart() || !sourceEnd.getBlockIdentifiers().equals(taken.getBlockIdentifiers()) || (notTakenPrevBlock = (Block3)seenPrevBlock.get(notTaken = targets.get(0))) == source) continue;
            ifStatement.setCondition(ifStatement.getCondition().getNegated());
            source.getContent().addAll(block.getContent());
            block.getContent().clear();
            block.sources.clear();
            source.targets.remove(block);
            Op03SimpleStatement newGoto = new Op03SimpleStatement(sourceEnd.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), sourceEnd.getIndex().justAfter());
            source.getContent().add(newGoto);
            sourceEnd.replaceTarget(taken, newGoto);
            sourceEnd.replaceTarget(notTaken, taken);
            notTaken.replaceSource(sourceEnd, newGoto);
            newGoto.addSource(sourceEnd);
            newGoto.addTarget(notTaken);
            blocks.set(x, null);
            effect = true;
        }
        if (effect) {
            blocks = Functional.filter(blocks, new Functional.NotNull());
        }
        return blocks;
    }

    private static List<Block3> combineNeighbouringBlocks(List<Block3> blocks) {
        boolean reloop;
        while (reloop = Op03Blocks.moveSingleOutOrderBlocks(blocks = Op03Blocks.combineNeighbouringBlocksPass1(blocks))) {
        }
        return blocks;
    }

    private static List<Block3> combineSingleCaseBackBlock(List<Block3> blocks) {
        IdentityHashMap<Block3, Integer> idx = new IdentityHashMap<Block3, Integer>();
        boolean effect = false;
        int len = blocks.size();
        for (int x = 0; x < len; ++x) {
            List content;
            Block3 target;
            Integer tgtIdx;
            Block3 block = blocks.get(x);
            idx.put(block, x);
            if (block.targets.size() != 1 || block.content.size() != 2 || (tgtIdx = (Integer)idx.get(target = SetUtil.getSingle(block.targets))) == null || target.sources.size() != 1 || (content = block.content).get(0).getStatement().getClass() != CaseStatement.class || content.get(1).getStatement().getClass() != GotoStatement.class) continue;
            Set<BlockIdentifier> containedIn = content.get(1).getBlockIdentifiers();
            content = target.getContent();
            for (Op03SimpleStatement statement : content) {
                statement.getBlockIdentifiers().addAll(containedIn);
            }
            block.content.addAll(content);
            target.sources.remove(block);
            block.targets.remove(target);
            for (Block3 tgt2 : target.targets) {
                tgt2.sources.remove(target);
                tgt2.sources.add(block);
                block.targets.add(tgt2);
                tgt2.resetSources();
            }
            target.targets.clear();
            blocks.set(tgtIdx, null);
            effect = true;
        }
        if (effect) {
            blocks = Functional.filter(blocks, new Functional.NotNull());
        }
        return blocks;
    }

    private static boolean moveSingleOutOrderBlocks(List<Block3> blocks) {
        IdentityHashMap<Block3, Integer> idx = new IdentityHashMap<Block3, Integer>();
        int len = blocks.size();
        for (int x = 0; x < len; ++x) {
            idx.put(blocks.get(x), x);
        }
        boolean effect = false;
        int len2 = blocks.size();
        block1: for (int x = 0; x < len2; ++x) {
            int idxtgt;
            Block3 block = blocks.get(x);
            if (block.sources.size() != 1 || block.targets.size() != 1) continue;
            Block3 source = SetUtil.getSingle(block.sources);
            Block3 target = SetUtil.getSingle(block.targets);
            int idxsrc = (Integer)idx.get(source);
            if (idxsrc != (idxtgt = ((Integer)idx.get(target)).intValue()) - 1 || idxtgt >= x) continue;
            List statements = source.getContent();
            Op03SimpleStatement prev = null;
            for (int y = statements.size() - 1; y >= 0; --y) {
                Op03SimpleStatement stm = (Op03SimpleStatement)statements.get(y);
                Statement statement = stm.getStatement();
                if (!statement.fallsToNext()) {
                    List<Op03SimpleStatement> stmTargets = stm.getTargets();
                    if (stmTargets.size() == 2 && stmTargets.get(0) == prev && stmTargets.get(1) == block.getStart()) break;
                    continue block1;
                }
                prev = stm;
            }
            if (!Op03Blocks.canCombineBlockSets(source, block)) {
                BlockIdentifier blk;
                List<BlockIdentifier> diff;
                Set<BlockIdentifier> srcBlocks = source.getEnd().getBlockIdentifiers();
                Set<BlockIdentifier> midBlocks = block.getStart().getBlockIdentifiers();
                if (srcBlocks.size() != midBlocks.size() + 1 || (diff = SetUtil.differenceAtakeBtoList(srcBlocks, midBlocks)).size() != 1 || (blk = diff.get(0)).getBlockType() != BlockType.TRYBLOCK) continue;
                for (Op03SimpleStatement op : block.getContent()) {
                    if (!op.getStatement().canThrow(ExceptionCheckSimple.INSTANCE)) continue;
                    continue block1;
                }
                block.getStart().markBlock(blk);
            }
            blocks.remove(x);
            int curridx = blocks.indexOf(source);
            blocks.add(curridx + 1, block);
            block.startIndex = source.startIndex.justAfter();
            Op03Blocks.patch(source, block);
            effect = true;
        }
        return effect;
    }

    private static List<Block3> combineNeighbouringBlocksPass1(List<Block3> blocks) {
        Block3 curr = blocks.get(0);
        int curridx = 0;
        int len = blocks.size();
        block0: for (int i = 1; i < len; ++i) {
            Block3 next = blocks.get(i);
            if (next == null) continue;
            if (next.sources.size() == 1 && SetUtil.getSingle(next.sources) == curr && next.getStart().getSources().contains(curr.getEnd()) && Op03Blocks.canCombineBlockSets(curr, next)) {
                Op03SimpleStatement lastCurr = curr.getEnd();
                Op03SimpleStatement firstNext = next.getStart();
                if (lastCurr.getStatement().getClass() == GotoStatement.class && !lastCurr.getTargets().isEmpty() && lastCurr.getTargets().get(0) == firstNext) {
                    lastCurr.nopOut();
                }
                curr.content.addAll(next.content);
                curr.targets.remove(next);
                for (Block3 target : next.targets) {
                    target.sources.remove(next);
                    target.sources.add(curr);
                }
                next.sources.clear();
                curr.targets.addAll(next.targets);
                next.targets.clear();
                curr.sources.remove(curr);
                curr.targets.remove(curr);
                blocks.set(i, null);
                for (int j = curridx - 1; j >= 0; --j) {
                    Block3 tmp = blocks.get(j);
                    if (tmp == null) continue;
                    curr = tmp;
                    curridx = j;
                    i = j;
                    continue block0;
                }
                continue;
            }
            curr = next;
            curridx = i;
        }
        for (Block3 block : blocks) {
            if (block == null) continue;
            block.resetSources();
        }
        return Functional.filter(blocks, new Functional.NotNull());
    }

    public static List<Op03SimpleStatement> topologicalSort(List<Op03SimpleStatement> statements, DecompilerComments comments, Options options) {
        List<Block3> blocks = Op03Blocks.buildBasicBlocks(statements);
        Op03Blocks.apply0TargetBlockHeuristic(blocks);
        Map<BlockIdentifier, BlockIdentifier> tryBlockAliases = Op03Blocks.getTryBlockAliases(statements);
        Op03Blocks.applyKnownBlocksHeuristic(blocks, tryBlockAliases);
        Op03Blocks.sanitiseBlocks(blocks);
        blocks = Op03Blocks.invertJoinZeroTargetJumps(blocks);
        blocks = Op03Blocks.combineNeighbouringBlocks(blocks);
        blocks = Op03Blocks.combineSingleCaseBackBlock(blocks);
        if (options.getOption(OptionsImpl.FORCE_TOPSORT_EXTRA) == Troolean.TRUE) {
            blocks = Op03Blocks.addTryEndDependencies(blocks);
        }
        blocks = Op03Blocks.doTopSort(blocks);
        boolean redo = false;
        redo = Op03Blocks.detectMoves(blocks, options);
        if (options.getOption(OptionsImpl.FORCE_TOPSORT_NOPULL) != Troolean.TRUE) {
            boolean bl = redo = Op03Blocks.addCatchEndDependencies(blocks) || redo;
        }
        if (redo) {
            Collections.sort(blocks);
            blocks = Op03Blocks.doTopSort(blocks);
        }
        int len = blocks.size();
        for (int i = 0; i < len - 1; ++i) {
            Block3 thisBlock = blocks.get(i);
            Block3 nextBlock = blocks.get(i + 1);
            Op03Blocks.patch(thisBlock, nextBlock);
        }
        Op03Blocks.patch(blocks.get(blocks.size() - 1), null);
        List<Op03SimpleStatement> outStatements = ListFactory.newList();
        for (Block3 outBlock : blocks) {
            outStatements.addAll(outBlock.getContent());
        }
        Cleaner.reindexInPlace(outStatements);
        boolean patched = false;
        int origLen = outStatements.size() - 1;
        for (int x = 0; x < origLen; ++x) {
            Op03SimpleStatement stm = outStatements.get(x);
            if (stm.getStatement().getClass() != IfStatement.class) continue;
            List<Op03SimpleStatement> targets = stm.getTargets();
            Op03SimpleStatement next = outStatements.get(x + 1);
            if (targets.get(0) == next) {
                MiscUtils.handyBreakPoint();
                continue;
            }
            if (targets.get(1) == next) {
                IfStatement ifStatement = (IfStatement)stm.getStatement();
                ifStatement.setCondition(ifStatement.getCondition().getNegated().simplify());
                Op03SimpleStatement a = targets.get(0);
                Op03SimpleStatement b = targets.get(1);
                targets.set(0, b);
                targets.set(1, a);
                continue;
            }
            patched = true;
            Op03SimpleStatement extra = new Op03SimpleStatement(stm.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), stm.getSSAIdentifiers(), stm.getIndex().justAfter());
            Op03SimpleStatement target0 = targets.get(0);
            extra.addSource(stm);
            extra.addTarget(target0);
            stm.replaceTarget(target0, extra);
            target0.replaceSource(stm, extra);
            outStatements.add(extra);
        }
        if (patched) {
            outStatements = Cleaner.sortAndRenumber(outStatements);
        }
        Op03Blocks.stripTryBlockAliases(outStatements, tryBlockAliases);
        if (((Boolean)options.getOption(OptionsImpl.ALLOW_CORRECTING)).booleanValue() && Op03Blocks.stripBackExceptions(outStatements)) {
            comments.addComment(DecompilerComment.TRY_BACKEDGE_REMOVED);
        }
        return Cleaner.removeUnreachableCode(outStatements, true);
    }

    private static boolean addCatchEndDependencies(List<Block3> blocks) {
        Block3 block;
        int x;
        boolean effect = false;
        Map lastIn = MapFactory.newMap();
        int size = blocks.size();
        for (int x2 = size - 1; x2 >= 0; --x2) {
            Block3 block2 = blocks.get(x2);
            Set<BlockIdentifier> inBlocks = block2.getStart().getBlockIdentifiers();
            for (BlockIdentifier inBlock : inBlocks) {
                if (lastIn.containsKey(inBlock)) continue;
                lastIn.put(inBlock, x2);
            }
        }
        List identifiersByBlock = ListFactory.newList();
        for (x = 0; x < size; ++x) {
            block = blocks.get(x);
            Op03SimpleStatement ostm = block.getStart();
            Set<BlockIdentifier> idents = ostm.getBlockIdentifiers();
            if (ostm.getStatement() instanceof CatchStatement) {
                idents = SetFactory.newSet(idents);
                idents.add(((CatchStatement)ostm.getStatement()).getCatchBlockIdent());
            }
            identifiersByBlock.add(idents);
        }
        block3: for (x = 0; x < size; ++x) {
            block = blocks.get(x);
            if (!(block.getEnd().getStatement() instanceof TryStatement)) continue;
            TryStatement tryStm = (TryStatement)block.getEnd().getStatement();
            BlockIdentifier tryBlockIdent = tryStm.getBlockIdentifier();
            Set<BlockIdentifier> catchBlockIdents = SetFactory.newSet();
            for (Op03SimpleStatement target : block.getEnd().getTargets()) {
                if (!(target.getStatement() instanceof CatchStatement)) continue;
                catchBlockIdents.add(((CatchStatement)target.getStatement()).getCatchBlockIdent());
            }
            Integer lastTry = (Integer)lastIn.get(tryBlockIdent);
            if (lastTry == null) continue;
            int idx = x;
            for (BlockIdentifier catchIdent : catchBlockIdents) {
                Integer thisIdx = (Integer)lastIn.get(catchIdent);
                if (thisIdx == null) continue block3;
                idx = Math.max(idx, thisIdx);
            }
            Set<BlockIdentifier> allBlockIdents = SetFactory.newSet(catchBlockIdents);
            allBlockIdents.add(tryBlockIdent);
            Block3 last = blocks.get(idx);
            for (int y = x + 1; y <= idx; ++y) {
                Block3 yBlock = blocks.get(y);
                if (SetUtil.hasIntersection((Set)identifiersByBlock.get(y), allBlockIdents)) continue;
                yBlock.addSource(last);
                last.addTarget(yBlock);
                effect = true;
            }
        }
        return effect;
    }

    private static List<Block3> addTryEndDependencies(List<Block3> blocks) {
        LazyMap<BlockIdentifier, List<Block3>> tryContent = MapFactory.newLazyMap(new UnaryFunction<BlockIdentifier, List<Block3>>(){

            @Override
            public List<Block3> invoke(BlockIdentifier arg) {
                return ListFactory.newList();
            }
        });
        for (Block3 block : blocks) {
            for (BlockIdentifier blockIdentifier : block.getStart().getBlockIdentifiers()) {
                if (blockIdentifier.getBlockType() != BlockType.TRYBLOCK) continue;
                ((List)tryContent.get(blockIdentifier)).add(block);
            }
        }
        for (Block3 block : blocks) {
            Statement blockStatement = block.getStart().getStatement();
            if (!(blockStatement instanceof CatchStatement)) continue;
            CatchStatement catchStatement = (CatchStatement)blockStatement;
            for (Map.Entry entry : tryContent.entrySet()) {
                if (!catchStatement.hasCatchBlockFor((BlockIdentifier)entry.getKey())) continue;
                for (Block3 b2 : (List)entry.getValue()) {
                    block.addSource(b2);
                    b2.addTarget(block);
                }
            }
        }
        return blocks;
    }

    private static boolean stripBackExceptions(List<Op03SimpleStatement> statements) {
        boolean res = false;
        List<Op03SimpleStatement> tryStatements = Functional.filter(statements, new ExactTypeFilter<TryStatement>(TryStatement.class));
        for (Op03SimpleStatement statement : tryStatements) {
            TryStatement tryStatement = (TryStatement)statement.getStatement();
            if (statement.getTargets().isEmpty()) continue;
            Op03SimpleStatement fallThrough = statement.getTargets().get(0);
            List<Op03SimpleStatement> backTargets = Functional.filter(statement.getTargets(), new Misc.IsForwardJumpTo(statement.getIndex()));
            boolean thisRes = false;
            for (Op03SimpleStatement backTarget : backTargets) {
                Statement backTargetStatement = backTarget.getStatement();
                if (backTargetStatement.getClass() != CatchStatement.class) continue;
                CatchStatement catchStatement = (CatchStatement)backTargetStatement;
                catchStatement.getExceptions().removeAll(tryStatement.getEntries());
                backTarget.removeSource(statement);
                statement.removeTarget(backTarget);
                thisRes = true;
            }
            if (!thisRes) continue;
            res = true;
            List<Op03SimpleStatement> remainingTargets = statement.getTargets();
            if (remainingTargets.size() != 1 || remainingTargets.get(0) != fallThrough) continue;
            statement.nopOut();
        }
        return res;
    }

    private static void patch(Block3 a, Block3 b) {
        List<Op03SimpleStatement> content = a.content;
        Op03SimpleStatement last = content.get(content.size() - 1);
        Statement statement = last.getStatement();
        if (last.getTargets().isEmpty() || !statement.fallsToNext()) {
            return;
        }
        Op03SimpleStatement fallThroughTarget = last.getTargets().get(0);
        if (b != null && fallThroughTarget == b.getStart()) {
            return;
        }
        Op03SimpleStatement newGoto = new Op03SimpleStatement(last.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), last.getIndex().justAfter());
        a.append(newGoto);
        last.replaceTarget(fallThroughTarget, newGoto);
        newGoto.addSource(last);
        newGoto.addTarget(fallThroughTarget);
        fallThroughTarget.replaceSource(last, newGoto);
    }

    private static class Block3
    implements Comparable<Block3> {
        InstrIndex startIndex;
        List<Op03SimpleStatement> content = ListFactory.newList();
        Set<Block3> sources = SetFactory.newOrderedSet();
        Set<Block3> originalSources = SetFactory.newOrderedSet();
        Set<Block3> targets = SetFactory.newOrderedSet();

        Block3(Op03SimpleStatement s) {
            this.startIndex = s.getIndex();
            this.content.add(s);
        }

        public void append(Op03SimpleStatement s) {
            this.content.add(s);
        }

        public Op03SimpleStatement getStart() {
            return this.content.get(0);
        }

        public Op03SimpleStatement getEnd() {
            return this.content.get(this.content.size() - 1);
        }

        void addSources(List<Block3> sources) {
            for (Block3 source : sources) {
                if (source != null) continue;
                throw new IllegalStateException();
            }
            this.sources.addAll(sources);
            this.originalSources.addAll(sources);
        }

        public void addSource(Block3 source) {
            this.sources.add(source);
            this.originalSources.add(source);
        }

        public void setTargets(List<Block3> targets) {
            this.targets.clear();
            this.targets.addAll(targets);
        }

        void addTargets(List<Block3> targets) {
            for (Block3 source : targets) {
                if (source != null) continue;
                throw new IllegalStateException();
            }
            this.targets.addAll(targets);
        }

        public void addTarget(Block3 source) {
            this.targets.add(source);
        }

        @Override
        public int compareTo(Block3 other) {
            return this.startIndex.compareTo(other.startIndex);
        }

        public String toString() {
            return "(" + this.content.size() + ")[" + this.sources.size() + "/" + this.originalSources.size() + "," + this.targets.size() + "] " + this.startIndex + this.getStart().toString();
        }

        private List<Op03SimpleStatement> getContent() {
            return this.content;
        }

        void copySources() {
            this.sources.clear();
            this.sources.addAll(this.originalSources);
        }

        void resetSources() {
            this.originalSources.clear();
            this.originalSources.addAll(this.sources);
        }

        Block3 getLastUnconditionalBackjumpToHere(Map<Block3, Integer> idxLut) {
            int thisIdx = idxLut.get(this);
            int best = -1;
            Block3 bestSource = null;
            for (Block3 source : this.originalSources) {
                int idxSource;
                if (source.getEnd().getStatement().getClass() != GotoStatement.class || (idxSource = idxLut.get(source).intValue()) <= best || idxSource <= thisIdx) continue;
                bestSource = source;
                best = idxSource;
            }
            return bestSource;
        }
    }
}

