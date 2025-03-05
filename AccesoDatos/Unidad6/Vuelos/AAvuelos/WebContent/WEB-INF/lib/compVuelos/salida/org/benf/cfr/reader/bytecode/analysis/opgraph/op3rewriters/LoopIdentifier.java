/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.CompareByIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.SwitchUtils;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CaseStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CommentStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.DoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnNothingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.WhileStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.util.CannotPerformDecode;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class LoopIdentifier {
    public static void identifyLoops1(Method method, List<Op03SimpleStatement> statements, BlockIdentifierFactory blockIdentifierFactory) {
        List<Op03SimpleStatement> pathtests = Functional.filter(statements, new TypeFilter<GotoStatement>(GotoStatement.class));
        for (Op03SimpleStatement start : pathtests) {
            LoopIdentifier.considerAsPathologicalLoop(start, statements);
        }
        List<Op03SimpleStatement> backjumps = Functional.filter(statements, new Misc.HasBackJump());
        List<Op03SimpleStatement> starts = Functional.uniqAll(Functional.map(backjumps, new Misc.GetBackJump()));
        Map<BlockIdentifier, Op03SimpleStatement> blockEndsCache = MapFactory.newMap();
        Collections.sort(starts, new CompareByIndex());
        List<LoopResult> loopResults = ListFactory.newList();
        Set<BlockIdentifier> relevantBlocks = SetFactory.newSet();
        for (Op03SimpleStatement start : starts) {
            BlockIdentifier blockIdentifier = LoopIdentifier.considerAsWhileLoopStart(method, start, statements, blockIdentifierFactory, blockEndsCache);
            if (blockIdentifier == null) {
                blockIdentifier = LoopIdentifier.considerAsDoLoopStart(start, statements, blockIdentifierFactory, blockEndsCache);
            }
            if (blockIdentifier == null) continue;
            loopResults.add(new LoopResult(blockIdentifier, start));
            relevantBlocks.add(blockIdentifier);
        }
        if (loopResults.isEmpty()) {
            return;
        }
        Collections.reverse(loopResults);
        LoopIdentifier.fixLoopOverlaps(statements, loopResults, relevantBlocks);
    }

    private static void fixLoopOverlaps(List<Op03SimpleStatement> statements, List<LoopResult> loopResults, Set<BlockIdentifier> relevantBlocks) {
        LazyMap<BlockIdentifier, List<BlockIdentifier>> requiredExtents = MapFactory.newLazyMap(new UnaryFunction<BlockIdentifier, List<BlockIdentifier>>(){

            @Override
            public List<BlockIdentifier> invoke(BlockIdentifier arg) {
                return ListFactory.newList();
            }
        });
        Map lastForBlock = MapFactory.newMap();
        for (LoopResult loopResult : loopResults) {
            Set<BlockIdentifier> backIn;
            final Op03SimpleStatement start = loopResult.blockStart;
            final BlockIdentifier testBlockIdentifier = loopResult.blockIdentifier;
            Set<BlockIdentifier> startIn = SetUtil.intersectionOrNull(start.getBlockIdentifiers(), relevantBlocks);
            List<Op03SimpleStatement> backSources = Functional.filter(start.getSources(), new Predicate<Op03SimpleStatement>(){

                @Override
                public boolean test(Op03SimpleStatement in) {
                    return in.getBlockIdentifiers().contains(testBlockIdentifier) && in.getIndex().isBackJumpTo(start);
                }
            });
            if (backSources.isEmpty()) continue;
            Collections.sort(backSources, new CompareByIndex());
            Op03SimpleStatement lastBackSource = backSources.get(backSources.size() - 1);
            lastForBlock.put(testBlockIdentifier, lastBackSource);
            if (startIn == null || (backIn = SetUtil.intersectionOrNull(lastBackSource.getBlockIdentifiers(), relevantBlocks)) == null || backIn.containsAll(startIn)) continue;
            Set<BlockIdentifier> startMissing = SetFactory.newSet(startIn);
            startMissing.removeAll(backIn);
            for (BlockIdentifier missing : startMissing) {
                ((List)requiredExtents.get(missing)).add(testBlockIdentifier);
            }
        }
        if (requiredExtents.isEmpty()) {
            return;
        }
        List<BlockIdentifier> extendBlocks = ListFactory.newList(requiredExtents.keySet());
        Collections.sort(extendBlocks, new Comparator<BlockIdentifier>(){

            @Override
            public int compare(BlockIdentifier blockIdentifier, BlockIdentifier blockIdentifier2) {
                return blockIdentifier.getIndex() - blockIdentifier2.getIndex();
            }
        });
        CompareByIndex comparator = new CompareByIndex();
        for (BlockIdentifier extendThis : extendBlocks) {
            List possibleEnds = (List)requiredExtents.get(extendThis);
            if (possibleEnds.isEmpty()) continue;
            List possibleEndOps = ListFactory.newList();
            for (BlockIdentifier end : possibleEnds) {
                possibleEndOps.add(lastForBlock.get(end));
            }
            Collections.sort(possibleEndOps, comparator);
            Op03SimpleStatement extendTo = (Op03SimpleStatement)possibleEndOps.get(possibleEndOps.size() - 1);
            Op03SimpleStatement oldEnd = (Op03SimpleStatement)lastForBlock.get(extendThis);
            if (oldEnd == null) continue;
            int start = statements.indexOf(oldEnd);
            int end = statements.indexOf(extendTo);
            for (int x = start; x <= end; ++x) {
                statements.get(x).getBlockIdentifiers().add(extendThis);
            }
            LoopIdentifier.rewriteEndLoopOverlapStatement(oldEnd, extendThis);
        }
    }

    private static void rewriteEndLoopOverlapStatement(Op03SimpleStatement oldEnd, BlockIdentifier loopBlock) {
        Statement statement = oldEnd.getStatement();
        Class<?> clazz = statement.getClass();
        if (clazz == WhileStatement.class) {
            WhileStatement whileStatement = (WhileStatement)statement;
            ConditionalExpression condition = whileStatement.getCondition();
            if (oldEnd.getTargets().size() == 2) {
                IfStatement repl = new IfStatement(BytecodeLoc.TODO, condition);
                repl.setKnownBlocks(loopBlock, null);
                repl.setJumpType(JumpType.CONTINUE);
                oldEnd.replaceStatement(repl);
                if (oldEnd.getThisComparisonBlock() == loopBlock) {
                    oldEnd.clearThisComparisonBlock();
                }
            } else if (oldEnd.getTargets().size() == 1 && condition == null) {
                GotoStatement repl = new GotoStatement(BytecodeLoc.TODO);
                repl.setJumpType(JumpType.CONTINUE);
                oldEnd.replaceStatement(repl);
                if (oldEnd.getThisComparisonBlock() == loopBlock) {
                    oldEnd.clearThisComparisonBlock();
                }
            }
        }
    }

    private static void considerAsPathologicalLoop(Op03SimpleStatement start, List<Op03SimpleStatement> statements) {
        if (start.getStatement().getClass() != GotoStatement.class) {
            return;
        }
        if (start.getTargets().get(0) != start) {
            return;
        }
        Op03SimpleStatement next = new Op03SimpleStatement(start.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), start.getIndex().justAfter());
        start.replaceStatement(new CommentStatement("Infinite loop"));
        start.replaceTarget(start, next);
        start.replaceSource(start, next);
        next.addSource(start);
        next.addTarget(start);
        statements.add(statements.indexOf(start) + 1, next);
    }

    private static BlockIdentifier considerAsDoLoopStart(Op03SimpleStatement start, List<Op03SimpleStatement> statements, BlockIdentifierFactory blockIdentifierFactory, Map<BlockIdentifier, Op03SimpleStatement> postBlockCache) {
        Op03SimpleStatement postBlock;
        Op03SimpleStatement source;
        int endIdx;
        int startIdx;
        final InstrIndex startIndex = start.getIndex();
        List<Op03SimpleStatement> backJumpSources = start.getSources();
        if (backJumpSources.isEmpty()) {
            throw new ConfusedCFRException("Node doesn't have ANY sources! " + start);
        }
        backJumpSources = Functional.filter(backJumpSources, new Predicate<Op03SimpleStatement>(){

            @Override
            public boolean test(Op03SimpleStatement in) {
                return in.getIndex().compareTo(startIndex) >= 0;
            }
        });
        Collections.sort(backJumpSources, new CompareByIndex());
        if (backJumpSources.isEmpty()) {
            throw new ConfusedCFRException("Node should have back jump sources.");
        }
        int lastJumpIdx = backJumpSources.size() - 1;
        Op03SimpleStatement lastJump = backJumpSources.get(lastJumpIdx);
        boolean conditional = false;
        boolean wasConditional = false;
        if (lastJump.getStatement() instanceof IfStatement) {
            conditional = true;
            wasConditional = true;
            IfStatement ifStatement = (IfStatement)lastJump.getStatement();
            if (ifStatement.getJumpTarget().getContainer() != start) {
                return null;
            }
            for (int x = 0; x < lastJumpIdx; ++x) {
                ConditionalExpression thisCond;
                Op03SimpleStatement prevJump = backJumpSources.get(x);
                Statement prevJumpStatement = prevJump.getStatement();
                if (prevJumpStatement.getClass() == GotoStatement.class) {
                    conditional = false;
                    break;
                }
                if (!(prevJumpStatement instanceof IfStatement)) continue;
                IfStatement backJumpIf = (IfStatement)prevJumpStatement;
                ConditionalExpression prevCond = ifStatement.getCondition();
                if (prevCond.equals(thisCond = backJumpIf.getCondition())) continue;
                conditional = false;
                break;
            }
        }
        if ((startIdx = statements.indexOf(start)) >= (endIdx = statements.indexOf(lastJump))) {
            return null;
        }
        BlockIdentifier blockIdentifier = blockIdentifierFactory.getNextBlockIdentifier(conditional ? BlockType.DOLOOP : BlockType.UNCONDITIONALDOLOOP);
        if (!start.getBlockIdentifiers().equals(lastJump.getBlockIdentifiers()) && start.getStatement() instanceof CaseStatement) {
            return null;
        }
        try {
            LoopIdentifier.validateAndAssignLoopIdentifier(statements, startIdx, endIdx + 1, blockIdentifier, start);
        }
        catch (CannotPerformDecode e) {
            return null;
        }
        Op03SimpleStatement doStatement = new Op03SimpleStatement(start.getBlockIdentifiers(), new DoStatement(BytecodeLoc.TODO, blockIdentifier), start.getIndex().justBefore());
        doStatement.getBlockIdentifiers().remove(blockIdentifier);
        List<Op03SimpleStatement> startSources = ListFactory.newList(start.getSources());
        for (Op03SimpleStatement source2 : startSources) {
            if (source2.getBlockIdentifiers().contains(blockIdentifier)) continue;
            source2.replaceTarget(start, doStatement);
            start.removeSource(source2);
            doStatement.addSource(source2);
        }
        if (doStatement.getSources().isEmpty() && (source = LoopIdentifier.getCloseFwdJumpInto(start, blockIdentifier, statements, startIdx, endIdx + 1)) != null && source.getStatement().getClass() == GotoStatement.class) {
            source.replaceStatement(new IfStatement(BytecodeLoc.NONE, BooleanExpression.TRUE));
            source.getTargets().add(0, doStatement);
            doStatement.addSource(source);
        }
        doStatement.addTarget(start);
        start.addSource(doStatement);
        if (conditional) {
            postBlock = lastJump.getTargets().get(0);
        } else {
            int newIdx;
            if (wasConditional) {
                IfStatement ifStatement = (IfStatement)lastJump.getStatement();
                ifStatement.negateCondition();
                ifStatement.setJumpType(JumpType.BREAK);
                Op03SimpleStatement oldFallthrough = lastJump.getTargets().get(0);
                Op03SimpleStatement oldTaken = lastJump.getTargets().get(1);
                Op03SimpleStatement newBackJump = new Op03SimpleStatement(lastJump.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), lastJump.getIndex().justAfter());
                lastJump.getTargets().set(0, newBackJump);
                lastJump.getTargets().set(1, oldFallthrough);
                oldTaken.replaceSource(lastJump, newBackJump);
                newBackJump.addSource(lastJump);
                newBackJump.addTarget(oldTaken);
                statements.add(statements.indexOf(oldFallthrough), newBackJump);
                lastJump = newBackJump;
            }
            if ((newIdx = statements.indexOf(lastJump) + 1) >= statements.size()) {
                postBlock = new Op03SimpleStatement(SetFactory.<BlockIdentifier>newSet(), new ReturnNothingStatement(BytecodeLoc.TODO), lastJump.getIndex().justAfter());
                statements.add(postBlock);
            } else {
                postBlock = statements.get(newIdx);
            }
        }
        if (start.getFirstStatementInThisBlock() != null) {
            BlockIdentifier outer = Misc.findOuterBlock(start.getFirstStatementInThisBlock(), blockIdentifier, statements);
            if (blockIdentifier == outer) {
                throw new UnsupportedOperationException();
            }
            doStatement.setFirstStatementInThisBlock(start.getFirstStatementInThisBlock());
            start.setFirstStatementInThisBlock(blockIdentifier);
        }
        if (!conditional) {
            Set<BlockIdentifier> lastContent = SetFactory.newSet(lastJump.getBlockIdentifiers());
            lastContent.removeAll(start.getBlockIdentifiers());
            Set<BlockIdentifier> internalTryBlocks = SetFactory.newOrderedSet(Functional.filterSet(lastContent, new Predicate<BlockIdentifier>(){

                @Override
                public boolean test(BlockIdentifier in) {
                    return in.getBlockType() == BlockType.TRYBLOCK;
                }
            }));
            if (!internalTryBlocks.isEmpty()) {
                int postBlockIdx;
                int lastPostBlock = postBlockIdx = statements.indexOf(postBlock);
                while (lastPostBlock + 1 < statements.size()) {
                    int currentIdx;
                    Op03SimpleStatement stm = statements.get(lastPostBlock);
                    if (!(stm.getStatement() instanceof CatchStatement)) break;
                    CatchStatement catchStatement = (CatchStatement)stm.getStatement();
                    BlockIdentifier catchBlockIdent = catchStatement.getCatchBlockIdent();
                    List<BlockIdentifier> tryBlocks = Functional.map(catchStatement.getExceptions(), new UnaryFunction<ExceptionGroup.Entry, BlockIdentifier>(){

                        @Override
                        public BlockIdentifier invoke(ExceptionGroup.Entry arg) {
                            return arg.getTryBlockIdentifier();
                        }
                    });
                    if (!internalTryBlocks.containsAll(tryBlocks)) break;
                    for (currentIdx = lastPostBlock + 1; currentIdx < statements.size() && statements.get(currentIdx).getBlockIdentifiers().contains(catchBlockIdent); ++currentIdx) {
                    }
                    lastPostBlock = currentIdx;
                }
                if (lastPostBlock != postBlockIdx) {
                    Op03SimpleStatement newBackJump;
                    if (!lastJump.getTargets().contains(start)) {
                        throw new ConfusedCFRException("Nonsensical loop would be emitted - failure");
                    }
                    Op03SimpleStatement afterNewJump = null;
                    if (lastPostBlock >= statements.size()) {
                        Op03SimpleStatement beforeNewJump = statements.get(lastPostBlock - 1);
                        newBackJump = new Op03SimpleStatement(SetFactory.<BlockIdentifier>newSet(), new GotoStatement(BytecodeLoc.TODO), beforeNewJump.getIndex().justAfter());
                    } else {
                        afterNewJump = statements.get(lastPostBlock);
                        newBackJump = new Op03SimpleStatement(afterNewJump.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), afterNewJump.getIndex().justBefore());
                    }
                    newBackJump.addTarget(start);
                    newBackJump.addSource(lastJump);
                    lastJump.replaceTarget(start, newBackJump);
                    start.replaceSource(lastJump, newBackJump);
                    Op03SimpleStatement preNewJump = statements.get(lastPostBlock - 1);
                    if (afterNewJump != null && afterNewJump.getSources().contains(preNewJump)) {
                        Op03SimpleStatement interstit = new Op03SimpleStatement(preNewJump.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), newBackJump.getIndex().justBefore());
                        preNewJump.replaceTarget(afterNewJump, interstit);
                        afterNewJump.replaceSource(preNewJump, interstit);
                        interstit.addSource(preNewJump);
                        interstit.addTarget(afterNewJump);
                        statements.add(lastPostBlock, interstit);
                        ++lastPostBlock;
                    }
                    statements.add(lastPostBlock, newBackJump);
                    lastJump = newBackJump;
                    postBlock = afterNewJump;
                    for (int idx = postBlockIdx; idx <= lastPostBlock; ++idx) {
                        statements.get(idx).markBlock(blockIdentifier);
                    }
                }
            }
        }
        statements.add(statements.indexOf(start), doStatement);
        lastJump.markBlockStatement(blockIdentifier, null, lastJump, statements);
        start.markFirstStatementInBlock(blockIdentifier);
        postBlockCache.put(blockIdentifier, postBlock);
        return blockIdentifier;
    }

    static Op03SimpleStatement getCloseFwdJumpInto(Op03SimpleStatement start, BlockIdentifier blockIdentifier, List<Op03SimpleStatement> statements, int startIdx, int lastIdx) {
        Op03SimpleStatement linearlyPrevious = start.getLinearlyPrevious();
        if (LoopIdentifier.containsTargetInBlock(linearlyPrevious, blockIdentifier)) {
            return linearlyPrevious;
        }
        Op03SimpleStatement earliest = null;
        for (int x = startIdx; x < lastIdx; ++x) {
            Op03SimpleStatement stm = statements.get(x);
            if (!stm.getBlockIdentifiers().contains(blockIdentifier)) continue;
            for (Op03SimpleStatement source : stm.getSources()) {
                if (!source.getIndex().isBackJumpFrom(start) || source.getBlockIdentifiers().contains(blockIdentifier) || earliest != null && !earliest.getIndex().isBackJumpFrom(source)) continue;
                earliest = source;
            }
        }
        return earliest;
    }

    static boolean containsTargetInBlock(Op03SimpleStatement stm, BlockIdentifier block) {
        for (Op03SimpleStatement tgt : stm.getTargets()) {
            if (!tgt.getBlockIdentifiers().contains(block)) continue;
            return true;
        }
        return false;
    }

    private static BlockIdentifier considerAsWhileLoopStart(Method method, Op03SimpleStatement start, List<Op03SimpleStatement> statements, BlockIdentifierFactory blockIdentifierFactory, Map<BlockIdentifier, Op03SimpleStatement> postBlockCache) {
        int lastIdx;
        final InstrIndex startIndex = start.getIndex();
        List<Op03SimpleStatement> backJumpSources = start.getSources();
        backJumpSources = Functional.filter(backJumpSources, new Predicate<Op03SimpleStatement>(){

            @Override
            public boolean test(Op03SimpleStatement in) {
                return in.getIndex().compareTo(startIndex) >= 0;
            }
        });
        Collections.sort(backJumpSources, new CompareByIndex());
        Op03SimpleStatement conditional = LoopIdentifier.findFirstConditional(start);
        if (conditional == null) {
            return null;
        }
        Op03SimpleStatement lastJump = backJumpSources.get(backJumpSources.size() - 1);
        List<Op03SimpleStatement> conditionalTargets = conditional.getTargets();
        Op03SimpleStatement loopBreak = conditionalTargets.get(1);
        if (loopBreak == conditional && start == conditional) {
            Statement stm = conditional.getStatement();
            if (!(stm instanceof IfStatement)) {
                return null;
            }
            IfStatement ifStatement = (IfStatement)stm;
            ifStatement.negateCondition();
            Op03SimpleStatement backJump = new Op03SimpleStatement(conditional.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), conditional.getIndex().justAfter());
            Op03SimpleStatement notTaken = conditional.getTargets().get(0);
            conditional.replaceTarget(notTaken, backJump);
            conditional.replaceSource(conditional, backJump);
            conditional.replaceTarget(conditional, notTaken);
            backJump.addSource(conditional);
            backJump.addTarget(conditional);
            statements.add(statements.indexOf(conditional) + 1, backJump);
            loopBreak = notTaken;
        }
        if (loopBreak.getIndex().compareTo(lastJump.getIndex()) <= 0 && loopBreak.getIndex().compareTo(startIndex) >= 0) {
            return null;
        }
        if (start != conditional) {
            return null;
        }
        int idxConditional = statements.indexOf(start);
        int idxAfterEnd = statements.indexOf(loopBreak);
        if (idxAfterEnd < idxConditional) {
            Op03SimpleStatement startOfOuterLoop = statements.get(idxAfterEnd);
            if (startOfOuterLoop.getThisComparisonBlock() == null) {
                return null;
            }
            Op03SimpleStatement endOfOuter = postBlockCache.get(startOfOuterLoop.getThisComparisonBlock());
            if (endOfOuter == null) {
                throw new ConfusedCFRException("BlockIdentifier doesn't exist in blockEndsCache");
            }
            idxAfterEnd = statements.indexOf(endOfOuter);
        }
        if (idxConditional >= idxAfterEnd) {
            return null;
        }
        BlockIdentifier blockIdentifier = blockIdentifierFactory.getNextBlockIdentifier(BlockType.WHILELOOP);
        try {
            lastIdx = LoopIdentifier.validateAndAssignLoopIdentifier(statements, idxConditional + 1, idxAfterEnd, blockIdentifier, start);
        }
        catch (CannotPerformDecode e) {
            return null;
        }
        Op03SimpleStatement lastInBlock = statements.get(lastIdx);
        Op03SimpleStatement blockEnd = statements.get(idxAfterEnd);
        start.markBlockStatement(blockIdentifier, lastInBlock, blockEnd, statements);
        statements.get(idxConditional + 1).markFirstStatementInBlock(blockIdentifier);
        postBlockCache.put(blockIdentifier, blockEnd);
        if (lastInBlock.getStatement().fallsToNext() && lastInBlock.getTargets().size() == 1) {
            Op03SimpleStatement afterFallThrough = new Op03SimpleStatement(lastInBlock.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), lastInBlock.getIndex().justAfter());
            SwitchUtils.checkFixNewCase(afterFallThrough, lastInBlock);
            Op03SimpleStatement tgt = lastInBlock.getTargets().get(0);
            lastInBlock.replaceTarget(tgt, afterFallThrough);
            tgt.replaceSource(lastInBlock, afterFallThrough);
            afterFallThrough.addSource(lastInBlock);
            afterFallThrough.addTarget(tgt);
            statements.add(afterFallThrough);
            lastInBlock = afterFallThrough;
        }
        Op03SimpleStatement afterLastInBlock = lastIdx + 1 < statements.size() ? statements.get(lastIdx + 1) : null;
        loopBreak = conditional.getTargets().get(1);
        if (afterLastInBlock != null && afterLastInBlock != loopBreak) {
            Op03SimpleStatement newAfterLast = new Op03SimpleStatement(afterLastInBlock.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), lastInBlock.getIndex().justAfter());
            conditional.replaceTarget(loopBreak, newAfterLast);
            newAfterLast.addSource(conditional);
            loopBreak.replaceSource(conditional, newAfterLast);
            newAfterLast.addTarget(loopBreak);
            statements.add(newAfterLast);
        }
        return blockIdentifier;
    }

    private static Op03SimpleStatement findFirstConditional(Op03SimpleStatement start) {
        Set visited = SetFactory.newSet();
        do {
            Statement innerStatement;
            if ((innerStatement = start.getStatement()) instanceof IfStatement) {
                return start;
            }
            List<Op03SimpleStatement> targets = start.getTargets();
            if (targets.size() != 1) {
                return null;
            }
            start = targets.get(0);
            if (visited.contains(start)) {
                return null;
            }
            visited.add(start);
        } while (start != null);
        return null;
    }

    private static int validateAndAssignLoopIdentifier(List<Op03SimpleStatement> statements, int idxTestStart, int idxAfterEnd, BlockIdentifier blockIdentifier, Op03SimpleStatement start) {
        int last = Misc.getFarthestReachableInRange(statements, idxTestStart, idxAfterEnd);
        Op03SimpleStatement discoveredLast = statements.get(last);
        Set<BlockIdentifier> lastBlocks = SetFactory.newSet(discoveredLast.getBlockIdentifiers());
        lastBlocks.removeAll(start.getBlockIdentifiers());
        Set<BlockIdentifier> catches = SetFactory.newSet(Functional.filterSet(lastBlocks, new Predicate<BlockIdentifier>(){

            @Override
            public boolean test(BlockIdentifier in) {
                BlockType type = in.getBlockType();
                return type == BlockType.CATCHBLOCK || type == BlockType.SWITCH;
            }
        }));
        Set<BlockIdentifier> originalCatches = SetFactory.newSet(catches);
        int newlast = last;
        while (true) {
            BlockIdentifier catchBlockIdent;
            if (!catches.isEmpty()) {
                Op03SimpleStatement stm = statements.get(newlast);
                catches.retainAll(stm.getBlockIdentifiers());
                if (!catches.isEmpty()) {
                    last = newlast;
                    if (newlast < statements.size() - 1) {
                        ++newlast;
                        continue;
                    }
                }
            }
            for (int x = idxTestStart; x <= last; ++x) {
                statements.get(x).markBlock(blockIdentifier);
            }
            Op03SimpleStatement newlastStm = statements.get(newlast);
            if (newlastStm.getStatement() instanceof CatchStatement && !originalCatches.contains(catchBlockIdent = ((CatchStatement)newlastStm.getStatement()).getCatchBlockIdent())) {
                for (Op03SimpleStatement source : newlastStm.getSources()) {
                    if (source.getBlockIdentifiers().contains(blockIdentifier)) continue;
                    return last;
                }
                originalCatches.add(catchBlockIdent);
                catches.add(catchBlockIdent);
                idxTestStart = last;
                ++newlast;
            }
            if (catches.isEmpty() || newlast >= statements.size() - 1) break;
        }
        return last;
    }

    private static class LoopResult {
        final BlockIdentifier blockIdentifier;
        final Op03SimpleStatement blockStart;

        private LoopResult(BlockIdentifier blockIdentifier, Op03SimpleStatement blockStart) {
            this.blockIdentifier = blockIdentifier;
            this.blockStart = blockStart;
        }
    }
}

