/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.CompareByIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.ExactTypeFilter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LinearScannedBlock;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CommentStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class ExceptionRewriters {
    static List<Op03SimpleStatement> eliminateCatchTemporaries(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> catches = Functional.filter(statements, new TypeFilter<CatchStatement>(CatchStatement.class));
        boolean effect = false;
        for (Op03SimpleStatement catchh : catches) {
            effect |= ExceptionRewriters.eliminateCatchTemporary(catchh);
        }
        if (effect) {
            statements = Cleaner.removeUnreachableCode(statements, false);
        }
        return statements;
    }

    private static boolean eliminateCatchTemporary(Op03SimpleStatement catchh) {
        if (catchh.getTargets().size() != 1) {
            return false;
        }
        Op03SimpleStatement maybeAssign = catchh.getTargets().get(0);
        CatchStatement catchStatement = (CatchStatement)catchh.getStatement();
        LValue catching = catchStatement.getCreatedLValue();
        if (!(catching instanceof StackSSALabel)) {
            return false;
        }
        StackSSALabel catchingSSA = (StackSSALabel)catching;
        if (catchingSSA.getStackEntry().getUsageCount() != 1L) {
            return false;
        }
        while (maybeAssign.getStatement() instanceof TryStatement) {
            maybeAssign = maybeAssign.getTargets().get(0);
        }
        WildcardMatch match = new WildcardMatch();
        if (!match.match(new AssignmentSimple(BytecodeLoc.NONE, match.getLValueWildCard("caught"), new StackValue(BytecodeLoc.NONE, catchingSSA)), maybeAssign.getStatement())) {
            return false;
        }
        catchh.replaceStatement(new CatchStatement(BytecodeLoc.TODO, catchStatement.getExceptions(), match.getLValueWildCard("caught").getMatch()));
        maybeAssign.nopOut();
        return true;
    }

    static void identifyCatchBlocks(List<Op03SimpleStatement> in, BlockIdentifierFactory blockIdentifierFactory) {
        List<Op03SimpleStatement> catchStarts = Functional.filter(in, new TypeFilter<CatchStatement>(CatchStatement.class));
        for (Op03SimpleStatement catchStart : catchStarts) {
            CatchStatement catchStatement = (CatchStatement)catchStart.getStatement();
            if (catchStatement.getCatchBlockIdent() != null) continue;
            BlockIdentifier blockIdentifier = blockIdentifierFactory.getNextBlockIdentifier(BlockType.CATCHBLOCK);
            catchStatement.setCatchBlockIdent(blockIdentifier);
            ExceptionRewriters.identifyCatchBlock(catchStart, blockIdentifier, in);
        }
    }

    private static void identifyCatchBlock(Op03SimpleStatement start, BlockIdentifier blockIdentifier, List<Op03SimpleStatement> statements) {
        Set knownMembers = SetFactory.newSet();
        Set seen = SetFactory.newSet();
        seen.add(start);
        knownMembers.add(start);
        LinkedList pendingPossibilities = ListFactory.newLinkedList();
        if (start.getTargets().size() != 1) {
            throw new ConfusedCFRException("Catch statement with multiple targets");
        }
        for (Op03SimpleStatement target : start.getTargets()) {
            pendingPossibilities.add(target);
            seen.add(target);
        }
        LazyMap<Op03SimpleStatement, Set<Op03SimpleStatement>> allows = MapFactory.newLazyMap(new UnaryFunction<Op03SimpleStatement, Set<Op03SimpleStatement>>(){

            @Override
            public Set<Op03SimpleStatement> invoke(Op03SimpleStatement ignore) {
                return SetFactory.newSet();
            }
        });
        int sinceDefinite = 0;
        while (!pendingPossibilities.isEmpty() && sinceDefinite <= pendingPossibilities.size()) {
            Op03SimpleStatement maybe = (Op03SimpleStatement)pendingPossibilities.removeFirst();
            boolean definite = true;
            for (Op03SimpleStatement op03SimpleStatement : maybe.getSources()) {
                if (knownMembers.contains(op03SimpleStatement) || op03SimpleStatement.getIndex().isBackJumpTo(maybe)) continue;
                definite = false;
                ((Set)allows.get(op03SimpleStatement)).add(maybe);
            }
            if (definite) {
                sinceDefinite = 0;
                knownMembers.add(maybe);
                Set allowedBy = (Set)allows.get(maybe);
                pendingPossibilities.addAll(allowedBy);
                allowedBy.clear();
                for (Op03SimpleStatement target : maybe.getTargets()) {
                    if (seen.contains(target)) continue;
                    seen.add(target);
                    if (!target.getIndex().isBackJumpTo(start)) continue;
                    pendingPossibilities.add(target);
                }
                continue;
            }
            ++sinceDefinite;
            pendingPossibilities.add(maybe);
        }
        knownMembers.remove(start);
        if (knownMembers.isEmpty()) {
            List<Op03SimpleStatement> targets = start.getTargets();
            if (targets.size() != 1) {
                throw new ConfusedCFRException("Synthetic catch block has multiple targets");
            }
            knownMembers.add(ExceptionRewriters.insertBlockPadding("empty catch block", start, targets.get(0), blockIdentifier, statements));
        }
        List knownMemberList = ListFactory.newList(knownMembers);
        Collections.sort(knownMemberList, new CompareByIndex());
        List<Op03SimpleStatement> truncatedKnownMembers = ListFactory.newList();
        List list = ListFactory.newList();
        int l = statements.size();
        for (int x = statements.indexOf(knownMemberList.get(0)); x < l; ++x) {
            Op03SimpleStatement statement = statements.get(x);
            if (statement.isAgreedNop()) {
                list.add(statement);
                continue;
            }
            if (!knownMembers.contains(statement)) break;
            truncatedKnownMembers.add(statement);
            if (list.isEmpty()) continue;
            truncatedKnownMembers.addAll(list);
            list.clear();
        }
        for (Op03SimpleStatement inBlock : truncatedKnownMembers) {
            inBlock.getBlockIdentifiers().add(blockIdentifier);
        }
        Op03SimpleStatement first = start.getTargets().get(0);
        first.markFirstStatementInBlock(blockIdentifier);
    }

    static void combineTryCatchBlocks(List<Op03SimpleStatement> in) {
        List<Op03SimpleStatement> tries = ExceptionRewriters.getTries(in);
        for (Op03SimpleStatement tryStatement : tries) {
            ExceptionRewriters.combineTryCatchBlocks(tryStatement);
        }
    }

    private static List<Op03SimpleStatement> getTries(List<Op03SimpleStatement> in) {
        return Functional.filter(in, new TypeFilter<TryStatement>(TryStatement.class));
    }

    private static void combineTryCatchBlocks(Op03SimpleStatement tryStatement) {
        Set allStatements = SetFactory.newSet();
        TryStatement innerTryStatement = (TryStatement)tryStatement.getStatement();
        allStatements.addAll(Misc.GraphVisitorBlockReachable.getBlockReachable(tryStatement, innerTryStatement.getBlockIdentifier()));
        for (Op03SimpleStatement target : tryStatement.getTargets()) {
            if (!(target.getStatement() instanceof CatchStatement)) continue;
            CatchStatement catchStatement = (CatchStatement)target.getStatement();
            allStatements.addAll(Misc.GraphVisitorBlockReachable.getBlockReachable(target, catchStatement.getCatchBlockIdent()));
        }
        Set<BlockIdentifier> tryBlocks = tryStatement.getBlockIdentifiers();
        if ((tryBlocks = SetFactory.newSet(Functional.filter(tryBlocks, new Predicate<BlockIdentifier>(){

            @Override
            public boolean test(BlockIdentifier in) {
                return in.getBlockType() == BlockType.TRYBLOCK || in.getBlockType() == BlockType.CATCHBLOCK;
            }
        }))).isEmpty()) {
            return;
        }
        List<Op03SimpleStatement> orderedStatements = ListFactory.newList(allStatements);
        Collections.sort(orderedStatements, new CompareByIndex(false));
        for (Op03SimpleStatement statement : orderedStatements) {
            for (BlockIdentifier ident : tryBlocks) {
                if (statement.getBlockIdentifiers().contains(ident) || !statement.getSources().contains(statement.getLinearlyPrevious()) || !statement.getLinearlyPrevious().getBlockIdentifiers().contains(ident)) continue;
                statement.addPossibleExitFor(ident);
            }
            statement.getBlockIdentifiers().addAll(tryBlocks);
        }
    }

    private static Op03SimpleStatement insertBlockPadding(String comment, Op03SimpleStatement insertAfter, Op03SimpleStatement insertBefore, BlockIdentifier blockIdentifier, List<Op03SimpleStatement> statements) {
        Op03SimpleStatement between = new Op03SimpleStatement(insertAfter.getBlockIdentifiers(), new CommentStatement(comment), insertAfter.getIndex().justAfter());
        insertAfter.replaceTarget(insertBefore, between);
        insertBefore.replaceSource(insertAfter, between);
        between.addSource(insertAfter);
        between.addTarget(insertBefore);
        between.getBlockIdentifiers().add(blockIdentifier);
        statements.add(between);
        return between;
    }

    static void extractExceptionMiddle(List<Op03SimpleStatement> in) {
        List<Op03SimpleStatement> tryStatements = Functional.filter(in, new ExactTypeFilter<TryStatement>(TryStatement.class));
        if (tryStatements.isEmpty()) {
            return;
        }
        Collections.reverse(tryStatements);
        for (Op03SimpleStatement tryStatement : tryStatements) {
            SingleExceptionAddressing trycatch;
            if (tryStatement.getTargets().size() != 2 || (trycatch = ExceptionRewriters.getSingleTryCatch(tryStatement, in)) == null) continue;
            if (ExceptionRewriters.extractExceptionMiddle(tryStatement, in, trycatch)) {
                Cleaner.sortAndRenumberInPlace(in);
                trycatch.tryBlock.reindex(in);
                trycatch.catchBlock.reindex(in);
            }
            ExceptionRewriters.extractCatchEnd(in, trycatch);
        }
    }

    public static void handleEmptyTries(List<Op03SimpleStatement> in) {
        Map<BlockIdentifier, Op03SimpleStatement> firstByBlock = null;
        boolean effect = false;
        List<Op03SimpleStatement> tries = ExceptionRewriters.getTries(in);
        for (Op03SimpleStatement tryStatement : tries) {
            BlockIdentifier block = ((TryStatement)tryStatement.getStatement()).getBlockIdentifier();
            Op03SimpleStatement tgtStm = tryStatement.getTargets().get(0);
            if (tgtStm.getBlockIdentifiers().contains(block)) continue;
            if (firstByBlock == null) {
                firstByBlock = ExceptionRewriters.getFirstByBlock(in);
            }
            if (firstByBlock.containsKey(block)) continue;
            Op03SimpleStatement newStm = new Op03SimpleStatement(tryStatement.getBlockIdentifiers(), new CommentStatement("empty try"), tryStatement.getIndex().justAfter());
            newStm.getBlockIdentifiers().add(block);
            newStm.addSource(tryStatement);
            newStm.addTarget(tgtStm);
            tgtStm.replaceSource(tryStatement, newStm);
            tryStatement.replaceTarget(tgtStm, newStm);
            effect = true;
        }
        if (effect) {
            Cleaner.sortAndRenumberInPlace(in);
        }
    }

    private static Map<BlockIdentifier, Op03SimpleStatement> getFirstByBlock(List<Op03SimpleStatement> in) {
        Map<BlockIdentifier, Op03SimpleStatement> res = MapFactory.newMap();
        for (Op03SimpleStatement stm : in) {
            for (BlockIdentifier i : stm.getBlockIdentifiers()) {
                if (res.containsKey(i)) continue;
                res.put(i, stm);
            }
        }
        return res;
    }

    private static SingleExceptionAddressing getSingleTryCatch(Op03SimpleStatement trystm, List<Op03SimpleStatement> statements) {
        TryStatement tryStatement;
        BlockIdentifier tryBlockIdent;
        int idx = statements.indexOf(trystm);
        LinearScannedBlock tryBlock = ExceptionRewriters.getLinearScannedBlock(statements, idx, trystm, tryBlockIdent = (tryStatement = (TryStatement)trystm.getStatement()).getBlockIdentifier(), true);
        if (tryBlock == null) {
            return null;
        }
        Op03SimpleStatement catchs = trystm.getTargets().get(1);
        Statement testCatch = catchs.getStatement();
        if (!(testCatch instanceof CatchStatement)) {
            return null;
        }
        CatchStatement catchStatement = (CatchStatement)testCatch;
        BlockIdentifier catchBlockIdent = catchStatement.getCatchBlockIdent();
        LinearScannedBlock catchBlock = ExceptionRewriters.getLinearScannedBlock(statements, statements.indexOf(catchs), catchs, catchBlockIdent, true);
        if (catchBlock == null) {
            return null;
        }
        if (!catchBlock.isAfter(tryBlock)) {
            return null;
        }
        return new SingleExceptionAddressing(tryBlockIdent, catchBlockIdent, tryBlock, catchBlock);
    }

    private static boolean extractExceptionMiddle(Op03SimpleStatement trystm, List<Op03SimpleStatement> statements, SingleExceptionAddressing trycatch) {
        Op03SimpleStatement stm;
        int x;
        LinearScannedBlock tryBlock = trycatch.tryBlock;
        LinearScannedBlock catchBlock = trycatch.catchBlock;
        BlockIdentifier tryBlockIdent = trycatch.tryBlockIdent;
        BlockIdentifier catchBlockIdent = trycatch.catchBlockIdent;
        int catchLast = catchBlock.getIdxLast();
        if (catchLast < statements.size() - 1) {
            Op03SimpleStatement afterCatchBlock = statements.get(catchLast + 1);
            for (Op03SimpleStatement source : afterCatchBlock.getSources()) {
                if (!source.getBlockIdentifiers().contains(catchBlockIdent)) continue;
                return false;
            }
        }
        if (catchBlock.immediatelyFollows(tryBlock)) {
            return false;
        }
        Set<BlockIdentifier> expected = trystm.getBlockIdentifiers();
        Set middle = SetFactory.newSet();
        List<Op03SimpleStatement> toMove = ListFactory.newList();
        for (x = tryBlock.getIdxLast() + 1; x < catchBlock.getIdxFirst(); ++x) {
            stm = statements.get(x);
            middle.add(stm);
            toMove.add(stm);
        }
        for (x = tryBlock.getIdxLast() + 1; x < catchBlock.getIdxFirst(); ++x) {
            stm = statements.get(x);
            if (!stm.getBlockIdentifiers().containsAll(expected)) {
                return false;
            }
            for (Op03SimpleStatement source : stm.getSources()) {
                Set<BlockIdentifier> sourceBlocks;
                if (!source.getIndex().isBackJumpTo(stm) || (sourceBlocks = source.getBlockIdentifiers()).contains(tryBlockIdent) || sourceBlocks.contains(catchBlockIdent)) continue;
                return false;
            }
        }
        InstrIndex afterIdx = catchBlock.getLast().getIndex().justAfter();
        for (Op03SimpleStatement move : toMove) {
            move.setIndex(afterIdx);
            afterIdx = afterIdx.justAfter();
        }
        return true;
    }

    private static void extractCatchEnd(List<Op03SimpleStatement> statements, SingleExceptionAddressing trycatch) {
        LinearScannedBlock tryBlock = trycatch.tryBlock;
        BlockIdentifier tryBlockIdent = trycatch.tryBlockIdent;
        BlockIdentifier catchBlockIdent = trycatch.catchBlockIdent;
        Op03SimpleStatement possibleAfterBlock = null;
        if (trycatch.catchBlock.getIdxLast() < statements.size() - 1) {
            Op03SimpleStatement afterCatch = statements.get(trycatch.catchBlock.getIdxLast() + 1);
            for (Op03SimpleStatement op03SimpleStatement : afterCatch.getSources()) {
                if (!op03SimpleStatement.getBlockIdentifiers().contains(tryBlockIdent)) continue;
                return;
            }
        }
        for (int x = tryBlock.getIdxFirst() + 1; x <= tryBlock.getIdxLast(); ++x) {
            List<Op03SimpleStatement> targets = statements.get(x).getTargets();
            for (Op03SimpleStatement target : targets) {
                if (!target.getBlockIdentifiers().contains(catchBlockIdent)) continue;
                if (possibleAfterBlock == null) {
                    possibleAfterBlock = target;
                    continue;
                }
                if (target == possibleAfterBlock) continue;
                return;
            }
        }
        if (possibleAfterBlock == null) {
            return;
        }
        Set<BlockIdentifier> tryStartBlocks = trycatch.tryBlock.getFirst().getBlockIdentifiers();
        Set<BlockIdentifier> possibleBlocks = possibleAfterBlock.getBlockIdentifiers();
        if (possibleBlocks.size() != tryStartBlocks.size() + 1) {
            return;
        }
        if (!possibleBlocks.containsAll(tryStartBlocks)) {
            return;
        }
        if (!possibleBlocks.contains(catchBlockIdent)) {
            return;
        }
        int n = statements.indexOf(possibleAfterBlock);
        LinearScannedBlock unmarkBlock = ExceptionRewriters.getLinearScannedBlock(statements, n, possibleAfterBlock, catchBlockIdent, false);
        if (unmarkBlock == null) {
            return;
        }
        for (int x = unmarkBlock.getIdxFirst(); x <= unmarkBlock.getIdxLast(); ++x) {
            statements.get(x).getBlockIdentifiers().remove(catchBlockIdent);
        }
    }

    private static LinearScannedBlock getLinearScannedBlock(List<Op03SimpleStatement> statements, int idx, Op03SimpleStatement stm, BlockIdentifier blockIdentifier, boolean prefix) {
        Op03SimpleStatement nstm;
        Set found = SetFactory.newSet();
        int nextIdx = idx + (prefix ? 1 : 0);
        if (prefix) {
            found.add(stm);
        }
        int cnt = statements.size();
        while ((nstm = statements.get(nextIdx)).getBlockIdentifiers().contains(blockIdentifier)) {
            found.add(nstm);
            if (++nextIdx < cnt) continue;
        }
        Set<Op03SimpleStatement> reachable = Misc.GraphVisitorBlockReachable.getBlockReachable(stm, blockIdentifier);
        if (!reachable.equals(found)) {
            return null;
        }
        --nextIdx;
        if (reachable.isEmpty()) {
            return null;
        }
        return new LinearScannedBlock(stm, statements.get(nextIdx), idx, nextIdx);
    }

    private static class SingleExceptionAddressing {
        BlockIdentifier tryBlockIdent;
        BlockIdentifier catchBlockIdent;
        LinearScannedBlock tryBlock;
        LinearScannedBlock catchBlock;

        private SingleExceptionAddressing(BlockIdentifier tryBlockIdent, BlockIdentifier catchBlockIdent, LinearScannedBlock tryBlock, LinearScannedBlock catchBlock) {
            this.tryBlockIdent = tryBlockIdent;
            this.catchBlockIdent = catchBlockIdent;
            this.tryBlock = tryBlock;
            this.catchBlock = catchBlock;
        }
    }
}

