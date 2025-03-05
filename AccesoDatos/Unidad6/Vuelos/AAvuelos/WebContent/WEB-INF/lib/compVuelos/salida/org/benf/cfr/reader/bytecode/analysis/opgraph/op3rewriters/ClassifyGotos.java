/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;

class ClassifyGotos {
    ClassifyGotos() {
    }

    static void classifyGotos(List<Op03SimpleStatement> in) {
        Op03SimpleStatement stm;
        List<Pair> gotos = ListFactory.newList();
        Map<BlockIdentifier, Op03SimpleStatement> tryStatementsByBlock = MapFactory.newMap();
        Map<BlockIdentifier, List<BlockIdentifier>> catchStatementsByBlock = MapFactory.newMap();
        LazyMap<BlockIdentifier, Set<BlockIdentifier>> catchToTries = MapFactory.newLazyMap(new UnaryFunction<BlockIdentifier, Set<BlockIdentifier>>(){

            @Override
            public Set<BlockIdentifier> invoke(BlockIdentifier arg) {
                return SetFactory.newOrderedSet();
            }
        });
        int len = in.size();
        for (int x = 0; x < len; ++x) {
            GotoStatement gotoStatement;
            stm = in.get(x);
            Statement statement = stm.getStatement();
            Class<?> clz = statement.getClass();
            if (clz == TryStatement.class) {
                TryStatement tryStatement = (TryStatement)statement;
                BlockIdentifier tryBlockIdent = tryStatement.getBlockIdentifier();
                tryStatementsByBlock.put(tryBlockIdent, stm);
                List<Op03SimpleStatement> targets = stm.getTargets();
                List catchBlocks = ListFactory.newList();
                catchStatementsByBlock.put(tryStatement.getBlockIdentifier(), catchBlocks);
                int len2 = targets.size();
                for (int y = 1; y < len2; ++y) {
                    Statement statement2 = targets.get(y).getStatement();
                    if (statement2.getClass() != CatchStatement.class) continue;
                    BlockIdentifier catchBlockIdent = ((CatchStatement)statement2).getCatchBlockIdent();
                    catchBlocks.add(catchBlockIdent);
                    ((Set)catchToTries.get(catchBlockIdent)).add(tryBlockIdent);
                }
                continue;
            }
            if (clz != GotoStatement.class || !(gotoStatement = (GotoStatement)statement).getJumpType().isUnknown()) continue;
            gotos.add(Pair.make(stm, x));
        }
        if (!tryStatementsByBlock.isEmpty()) {
            for (Pair goto_ : gotos) {
                int idx;
                stm = (Op03SimpleStatement)goto_.getFirst();
                if (ClassifyGotos.classifyTryLeaveGoto(stm, idx = ((Integer)goto_.getSecond()).intValue(), tryStatementsByBlock.keySet(), tryStatementsByBlock, catchStatementsByBlock, in)) continue;
                ClassifyGotos.classifyCatchLeaveGoto(stm, idx, tryStatementsByBlock.keySet(), tryStatementsByBlock, catchStatementsByBlock, catchToTries, in);
            }
        }
    }

    private static boolean classifyTryLeaveGoto(Op03SimpleStatement gotoStm, int idx, Set<BlockIdentifier> tryBlockIdents, Map<BlockIdentifier, Op03SimpleStatement> tryStatementsByBlock, Map<BlockIdentifier, List<BlockIdentifier>> catchStatementByBlock, List<Op03SimpleStatement> in) {
        Set<BlockIdentifier> blocks = gotoStm.getBlockIdentifiers();
        return ClassifyGotos.classifyTryCatchLeaveGoto(gotoStm, blocks, idx, tryBlockIdents, tryStatementsByBlock, catchStatementByBlock, in);
    }

    private static void classifyCatchLeaveGoto(Op03SimpleStatement gotoStm, int idx, Set<BlockIdentifier> tryBlockIdents, Map<BlockIdentifier, Op03SimpleStatement> tryStatementsByBlock, Map<BlockIdentifier, List<BlockIdentifier>> catchStatementByBlock, Map<BlockIdentifier, Set<BlockIdentifier>> catchBlockToTryBlocks, List<Op03SimpleStatement> in) {
        Set<BlockIdentifier> inBlocks = gotoStm.getBlockIdentifiers();
        Set<BlockIdentifier> blocks = SetFactory.newOrderedSet();
        for (BlockIdentifier block : inBlocks) {
            if (!catchBlockToTryBlocks.containsKey(block)) continue;
            Set<BlockIdentifier> catchToTries = catchBlockToTryBlocks.get(block);
            blocks.addAll(catchToTries);
        }
        ClassifyGotos.classifyTryCatchLeaveGoto(gotoStm, blocks, idx, tryBlockIdents, tryStatementsByBlock, catchStatementByBlock, in);
    }

    private static boolean classifyTryCatchLeaveGoto(Op03SimpleStatement gotoStm, Set<BlockIdentifier> blocks, int idx, Set<BlockIdentifier> tryBlockIdents, Map<BlockIdentifier, Op03SimpleStatement> tryStatementsByBlock, Map<BlockIdentifier, List<BlockIdentifier>> catchStatementByBlock, List<Op03SimpleStatement> in) {
        if (idx >= in.size() - 1) {
            return false;
        }
        GotoStatement gotoStatement = (GotoStatement)gotoStm.getStatement();
        Set<BlockIdentifier> tryBlocks = SetUtil.intersectionOrNull(blocks, tryBlockIdents);
        if (tryBlocks == null) {
            return false;
        }
        Op03SimpleStatement after = in.get(idx + 1);
        Set<BlockIdentifier> afterBlocks = SetUtil.intersectionOrNull(after.getBlockIdentifiers(), tryBlockIdents);
        if (afterBlocks != null) {
            tryBlocks.removeAll(afterBlocks);
        }
        if (tryBlocks.size() != 1) {
            return false;
        }
        BlockIdentifier left = tryBlocks.iterator().next();
        Op03SimpleStatement tryStatement = tryStatementsByBlock.get(left);
        if (tryStatement == null) {
            return false;
        }
        List<BlockIdentifier> catchForThis = catchStatementByBlock.get(left);
        if (catchForThis == null) {
            return false;
        }
        Op03SimpleStatement gotoTgt = gotoStm.getTargets().get(0);
        Set<BlockIdentifier> gotoTgtIdents = gotoTgt.getBlockIdentifiers();
        if (SetUtil.hasIntersection(gotoTgtIdents, catchForThis)) {
            return false;
        }
        int idxtgt = in.indexOf(gotoTgt);
        if (idxtgt == 0) {
            return false;
        }
        Op03SimpleStatement prev = in.get(idxtgt - 1);
        if (!SetUtil.hasIntersection(prev.getBlockIdentifiers(), catchForThis)) {
            if (catchForThis.size() == 1 && after.getStatement() instanceof CatchStatement) {
                CatchStatement catchTest;
                Op03SimpleStatement catchSucc;
                boolean emptyCatch;
                boolean bl = emptyCatch = after == prev;
                if (!emptyCatch && Misc.followNopGotoChain(catchSucc = after.getTargets().get(0), false, true) == Misc.followNopGotoChain(prev, false, true)) {
                    emptyCatch = true;
                }
                if (emptyCatch && (catchTest = (CatchStatement)after.getStatement()).getCatchBlockIdent() == catchForThis.get(0)) {
                    gotoStatement.setJumpType(JumpType.GOTO_OUT_OF_TRY);
                    return true;
                }
            }
            return false;
        }
        gotoStatement.setJumpType(JumpType.GOTO_OUT_OF_TRY);
        return true;
    }

    static void classifyAnonymousBlockGotos(List<Op03SimpleStatement> in, boolean agressive) {
        int agressiveOffset = agressive ? 1 : 0;
        for (Op03SimpleStatement statement : in) {
            Op03SimpleStatement targetStatement;
            boolean isForwardJump;
            JumpingStatement jumpingStatement;
            JumpType jumpType;
            Statement inner = statement.getStatement();
            if (!(inner instanceof JumpingStatement) || (jumpType = (jumpingStatement = (JumpingStatement)inner).getJumpType()) != JumpType.GOTO || !(isForwardJump = (targetStatement = (Op03SimpleStatement)jumpingStatement.getJumpTarget().getContainer()).getIndex().isBackJumpTo(statement))) continue;
            Set<BlockIdentifier> targetBlocks = targetStatement.getBlockIdentifiers();
            Set<BlockIdentifier> srcBlocks = statement.getBlockIdentifiers();
            if (targetBlocks.size() >= srcBlocks.size() + agressiveOffset || !srcBlocks.containsAll(targetBlocks)) continue;
            srcBlocks = Functional.filterSet(srcBlocks, new Predicate<BlockIdentifier>(){

                @Override
                public boolean test(BlockIdentifier in) {
                    BlockType blockType = in.getBlockType();
                    if (blockType == BlockType.CASE) {
                        return false;
                    }
                    return blockType != BlockType.SWITCH;
                }
            });
            if (targetBlocks.size() >= srcBlocks.size() + agressiveOffset || !srcBlocks.containsAll(targetBlocks)) continue;
            jumpingStatement.setJumpType(JumpType.BREAK_ANONYMOUS);
        }
    }
}

