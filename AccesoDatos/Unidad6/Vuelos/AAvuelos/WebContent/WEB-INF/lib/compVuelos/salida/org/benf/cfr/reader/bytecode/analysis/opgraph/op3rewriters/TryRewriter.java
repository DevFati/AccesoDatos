/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.FinallyStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheckImpl;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

class TryRewriter {
    TryRewriter() {
    }

    private static void extendTryBlock(Op03SimpleStatement tryStatement, List<Op03SimpleStatement> in, DCCommonState dcCommonState) {
        TryStatement tryStatementInner = (TryStatement)tryStatement.getStatement();
        BlockIdentifier tryBlockIdent = tryStatementInner.getBlockIdentifier();
        Op03SimpleStatement lastStatement = null;
        Op03SimpleStatement currentStatement = tryStatement.getTargets().get(0);
        int x = in.indexOf(currentStatement);
        List<Op03SimpleStatement> jumps = ListFactory.newList();
        while (currentStatement.getBlockIdentifiers().contains(tryBlockIdent)) {
            if (++x >= in.size()) {
                return;
            }
            lastStatement = currentStatement;
            if (currentStatement.getStatement() instanceof JumpingStatement) {
                jumps.add(currentStatement);
            }
            currentStatement = in.get(x);
        }
        Set<JavaRefTypeInstance> caught = SetFactory.newSet();
        List<Op03SimpleStatement> targets = tryStatement.getTargets();
        int len = targets.size();
        for (int i = 1; i < len; ++i) {
            Statement statement = targets.get(i).getStatement();
            if (!(statement instanceof CatchStatement)) continue;
            CatchStatement catchStatement = (CatchStatement)statement;
            List<ExceptionGroup.Entry> list = catchStatement.getExceptions();
            for (ExceptionGroup.Entry entry : list) {
                caught.add(entry.getCatchType());
            }
        }
        ExceptionCheckImpl exceptionCheck = new ExceptionCheckImpl(dcCommonState, caught);
        block3: while (!currentStatement.getStatement().canThrow(exceptionCheck)) {
            Set validBlocks = SetFactory.newSet();
            validBlocks.add(tryBlockIdent);
            int len2 = tryStatement.getTargets().size();
            for (int i = 1; i < len2; ++i) {
                Op03SimpleStatement op03SimpleStatement = tryStatement.getTargets().get(i);
                Statement tgtStatement = op03SimpleStatement.getStatement();
                if (tgtStatement instanceof CatchStatement) {
                    validBlocks.add(((CatchStatement)tgtStatement).getCatchBlockIdent());
                    continue;
                }
                if (tgtStatement instanceof FinallyStatement) {
                    validBlocks.add(((FinallyStatement)tgtStatement).getFinallyBlockIdent());
                    continue;
                }
                return;
            }
            boolean foundSource = false;
            for (Op03SimpleStatement op03SimpleStatement : currentStatement.getSources()) {
                if (!SetUtil.hasIntersection(validBlocks, op03SimpleStatement.getBlockIdentifiers())) {
                    return;
                }
                if (!op03SimpleStatement.getBlockIdentifiers().contains(tryBlockIdent)) continue;
                foundSource = true;
            }
            if (!foundSource) {
                return;
            }
            currentStatement.getBlockIdentifiers().add(tryBlockIdent);
            if (++x >= in.size()) break;
            Op03SimpleStatement nextStatement = in.get(x);
            if (!currentStatement.getTargets().contains(nextStatement)) {
                for (Op03SimpleStatement source2 : nextStatement.getSources()) {
                    if (source2.getBlockIdentifiers().contains(tryBlockIdent)) continue;
                    break block3;
                }
            }
            lastStatement = currentStatement;
            if (currentStatement.getStatement() instanceof JumpingStatement) {
                jumps.add(currentStatement);
            }
            currentStatement = nextStatement;
        }
        if (lastStatement != null && lastStatement.getTargets().isEmpty()) {
            Set outTargets = SetFactory.newSet();
            for (Op03SimpleStatement jump : jumps) {
                int idx;
                JumpingStatement jumpingStatement = (JumpingStatement)jump.getStatement();
                int n = idx = jumpingStatement.isConditional() ? 1 : 0;
                if (idx >= jump.getTargets().size()) {
                    return;
                }
                Op03SimpleStatement jumpTarget = jump.getTargets().get(idx);
                if (jumpTarget.getIndex().isBackJumpFrom(jump) || jumpTarget.getBlockIdentifiers().contains(tryBlockIdent)) continue;
                outTargets.add(jumpTarget);
            }
            if (outTargets.size() == 1) {
                Op03SimpleStatement replace = (Op03SimpleStatement)outTargets.iterator().next();
                Op03SimpleStatement newJump = new Op03SimpleStatement(lastStatement.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), lastStatement.getIndex().justAfter());
                newJump.addTarget(replace);
                replace.addSource(newJump);
                for (Op03SimpleStatement jump : jumps) {
                    if (!jump.getTargets().contains(replace)) continue;
                    jump.replaceTarget(replace, newJump);
                    newJump.addSource(jump);
                    replace.removeSource(jump);
                }
                in.add(in.indexOf(lastStatement) + 1, newJump);
            }
        }
    }

    static void extendTryBlocks(DCCommonState dcCommonState, List<Op03SimpleStatement> in) {
        List<Op03SimpleStatement> tries = Functional.filter(in, new TypeFilter<TryStatement>(TryStatement.class));
        for (Op03SimpleStatement tryStatement : tries) {
            TryRewriter.extendTryBlock(tryStatement, in, dcCommonState);
        }
    }

    static void combineTryCatchEnds(List<Op03SimpleStatement> in) {
        List<Op03SimpleStatement> tries = Functional.filter(in, new TypeFilter<TryStatement>(TryStatement.class));
        for (Op03SimpleStatement tryStatement : tries) {
            TryRewriter.combineTryCatchEnds(tryStatement, in);
        }
    }

    private static Op03SimpleStatement getLastContiguousBlockStatement(BlockIdentifier blockIdentifier, List<Op03SimpleStatement> in, Op03SimpleStatement preBlock) {
        if (preBlock.getTargets().isEmpty()) {
            return null;
        }
        Op03SimpleStatement currentStatement = preBlock.getTargets().get(0);
        int x = in.indexOf(currentStatement);
        if (!currentStatement.getBlockIdentifiers().contains(blockIdentifier)) {
            return null;
        }
        Op03SimpleStatement last = currentStatement;
        while (currentStatement.getBlockIdentifiers().contains(blockIdentifier) && ++x < in.size()) {
            last = currentStatement;
            currentStatement = in.get(x);
        }
        return last;
    }

    private static void combineTryCatchEnds(Op03SimpleStatement tryStatement, List<Op03SimpleStatement> in) {
        TryStatement innerTryStatement = (TryStatement)tryStatement.getStatement();
        List<Op03SimpleStatement> lastStatements = ListFactory.newList();
        lastStatements.add(TryRewriter.getLastContiguousBlockStatement(innerTryStatement.getBlockIdentifier(), in, tryStatement));
        int len = tryStatement.getTargets().size();
        for (int x = 1; x < len; ++x) {
            Op03SimpleStatement statementContainer = tryStatement.getTargets().get(x);
            Statement statement = statementContainer.getStatement();
            if (!(statement instanceof CatchStatement)) {
                if (statement instanceof FinallyStatement) {
                    return;
                }
                return;
            }
            lastStatements.add(TryRewriter.getLastContiguousBlockStatement(((CatchStatement)statement).getCatchBlockIdent(), in, statementContainer));
        }
        if (lastStatements.size() <= 1) {
            return;
        }
        for (Op03SimpleStatement last : lastStatements) {
            if (last == null) {
                return;
            }
            if (last.getStatement().getClass() == GotoStatement.class) continue;
            return;
        }
        Op03SimpleStatement target = lastStatements.get(0).getTargets().get(0);
        for (Op03SimpleStatement last : lastStatements) {
            if (last.getTargets().get(0) == target) continue;
            return;
        }
        Op03SimpleStatement finalStatement = lastStatements.get(lastStatements.size() - 1);
        int beforeTgt = in.indexOf(finalStatement);
        Op03SimpleStatement proxy = new Op03SimpleStatement(tryStatement.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), finalStatement.getIndex().justAfter());
        in.add(beforeTgt + 1, proxy);
        proxy.addTarget(target);
        target.addSource(proxy);
        Set seen = SetFactory.newSet();
        for (Op03SimpleStatement last : lastStatements) {
            if (!seen.add(last)) continue;
            GotoStatement gotoStatement = (GotoStatement)last.getStatement();
            gotoStatement.setJumpType(JumpType.END_BLOCK);
            last.replaceTarget(target, proxy);
            target.removeSource(last);
            proxy.addSource(last);
        }
    }

    private static void extractExceptionJumps(Op03SimpleStatement tryi, List<Op03SimpleStatement> in) {
        List<Op03SimpleStatement> tryTargets = tryi.getTargets();
        Op03SimpleStatement uniqueForwardTarget = null;
        Set relevantBlocks = SetFactory.newSet();
        Op03SimpleStatement lastEnd = null;
        int lpidx = 0;
        for (Op03SimpleStatement tgt : tryTargets) {
            Object block;
            if ((block = TryRewriter.getBlockStart((lpidx++ == 0 ? tryi : tgt).getStatement())) == null) {
                return;
            }
            relevantBlocks.add(block);
            Op03SimpleStatement op03SimpleStatement = TryRewriter.getLastContiguousBlockStatement((BlockIdentifier)block, in, tgt);
            if (op03SimpleStatement == null) {
                return;
            }
            if (op03SimpleStatement.getStatement().getClass() == GotoStatement.class) {
                Op03SimpleStatement lastTgt = op03SimpleStatement.getTargets().get(0);
                if (uniqueForwardTarget == null) {
                    uniqueForwardTarget = lastTgt;
                } else if (uniqueForwardTarget != lastTgt) {
                    return;
                }
            }
            lastEnd = op03SimpleStatement;
        }
        if (uniqueForwardTarget == null) {
            return;
        }
        if (!uniqueForwardTarget.getBlockIdentifiers().equals(tryi.getBlockIdentifiers())) {
            return;
        }
        int idx = in.indexOf(lastEnd);
        if (idx >= in.size() - 1) {
            return;
        }
        Op03SimpleStatement next = in.get(idx + 1);
        if (next == uniqueForwardTarget) {
            return;
        }
        for (Op03SimpleStatement op03SimpleStatement : next.getSources()) {
            if (!SetUtil.hasIntersection(op03SimpleStatement.getBlockIdentifiers(), relevantBlocks)) continue;
            return;
        }
        LinkedList<Op03SimpleStatement> blockSources = ListFactory.newLinkedList();
        for (Op03SimpleStatement source : uniqueForwardTarget.getSources()) {
            if (!SetUtil.hasIntersection(source.getBlockIdentifiers(), relevantBlocks)) continue;
            blockSources.add(source);
        }
        Op03SimpleStatement op03SimpleStatement = new Op03SimpleStatement(next.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), next.getIndex().justBefore());
        for (Op03SimpleStatement source : blockSources) {
            Statement srcStatement = source.getStatement();
            if (srcStatement instanceof GotoStatement) {
                ((GotoStatement)srcStatement).setJumpType(JumpType.GOTO_OUT_OF_TRY);
            }
            uniqueForwardTarget.removeSource(source);
            source.replaceTarget(uniqueForwardTarget, op03SimpleStatement);
            op03SimpleStatement.addSource(source);
        }
        op03SimpleStatement.addTarget(uniqueForwardTarget);
        uniqueForwardTarget.addSource(op03SimpleStatement);
        in.add(idx + 1, op03SimpleStatement);
    }

    private static BlockIdentifier getBlockStart(Statement statement) {
        Class<?> clazz = statement.getClass();
        if (clazz == TryStatement.class) {
            TryStatement tryStatement = (TryStatement)statement;
            return tryStatement.getBlockIdentifier();
        }
        if (clazz == CatchStatement.class) {
            CatchStatement catchStatement = (CatchStatement)statement;
            return catchStatement.getCatchBlockIdent();
        }
        if (clazz == FinallyStatement.class) {
            FinallyStatement finallyStatement = (FinallyStatement)statement;
            return finallyStatement.getFinallyBlockIdent();
        }
        return null;
    }

    static void extractExceptionJumps(List<Op03SimpleStatement> in) {
        List<Op03SimpleStatement> tries = Functional.filter(in, new TypeFilter<TryStatement>(TryStatement.class));
        for (Op03SimpleStatement tryi : tries) {
            TryRewriter.extractExceptionJumps(tryi, in);
        }
    }

    private static void rewriteTryBackJump(Op03SimpleStatement stm) {
        InstrIndex idx = stm.getIndex();
        TryStatement tryStatement = (TryStatement)stm.getStatement();
        Op03SimpleStatement firstbody = stm.getTargets().get(0);
        BlockIdentifier blockIdentifier = tryStatement.getBlockIdentifier();
        Iterator<Op03SimpleStatement> sourceIter = stm.getSources().iterator();
        while (sourceIter.hasNext()) {
            Op03SimpleStatement source = sourceIter.next();
            if (!idx.isBackJumpFrom(source) || !source.getBlockIdentifiers().contains(blockIdentifier)) continue;
            source.replaceTarget(stm, firstbody);
            firstbody.addSource(source);
            sourceIter.remove();
        }
    }

    static void rewriteTryBackJumps(List<Op03SimpleStatement> in) {
        List<Op03SimpleStatement> tries = Functional.filter(in, new TypeFilter<TryStatement>(TryStatement.class));
        for (Op03SimpleStatement trystm : tries) {
            TryRewriter.rewriteTryBackJump(trystm);
        }
    }
}

