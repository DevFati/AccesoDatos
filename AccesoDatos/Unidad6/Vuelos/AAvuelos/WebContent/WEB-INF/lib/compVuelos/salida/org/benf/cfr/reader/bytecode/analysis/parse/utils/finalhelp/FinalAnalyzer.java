/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.CompareByIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CaseStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CommentStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.FinallyStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnValueStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ThrowStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp.FinallyCatchBody;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp.FinallyGraphHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp.PeerTries;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp.Result;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheckSimple;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;

public class FinalAnalyzer {
    public static void identifyFinally(Method method, final Op03SimpleStatement in, List<Op03SimpleStatement> allStatements, BlockIdentifierFactory blockIdentifierFactory, Set<Op03SimpleStatement> analysedTries) {
        if (!(in.getStatement() instanceof TryStatement)) {
            return;
        }
        analysedTries.add(in);
        TryStatement tryStatement = (TryStatement)in.getStatement();
        final BlockIdentifier tryBlockIdentifier = tryStatement.getBlockIdentifier();
        final List<Op03SimpleStatement> targets = in.getTargets();
        List<Op03SimpleStatement> catchStarts = Functional.filter(targets, new TypeFilter<CatchStatement>(CatchStatement.class));
        Set<Op03SimpleStatement> possibleCatches = SetFactory.newOrderedSet();
        for (Op03SimpleStatement catchS : catchStarts) {
            CatchStatement catchStatement = (CatchStatement)catchS.getStatement();
            List<ExceptionGroup.Entry> exceptions = catchStatement.getExceptions();
            for (ExceptionGroup.Entry exception : exceptions) {
                JavaRefTypeInstance catchType;
                if (exception.getExceptionGroup().getTryBlockIdentifier() != tryBlockIdentifier || !"java.lang.Throwable".equals((catchType = exception.getCatchType()).getRawName())) continue;
                possibleCatches.add(catchS);
            }
        }
        if (possibleCatches.isEmpty()) {
            return;
        }
        Op03SimpleStatement possibleFinallyCatch = FinalAnalyzer.findPossibleFinallyCatch(possibleCatches);
        FinallyCatchBody finallyCatchBody = FinallyCatchBody.build(possibleFinallyCatch, allStatements);
        if (finallyCatchBody == null) {
            return;
        }
        FinallyGraphHelper finallyGraphHelper = new FinallyGraphHelper(finallyCatchBody);
        PeerTries peerTries = new PeerTries(possibleFinallyCatch);
        peerTries.add(in);
        Set<Result> results = SetFactory.newOrderedSet();
        Set peerTrySeen = SetFactory.newOrderedSet();
        while (peerTries.hasNext()) {
            Op03SimpleStatement tryS = peerTries.removeNext();
            if (!peerTrySeen.add(tryS) || FinalAnalyzer.identifyFinally2(tryS, peerTries, finallyGraphHelper, results)) continue;
            return;
        }
        if (results.isEmpty()) {
            return;
        }
        if (results.size() == 1) {
            return;
        }
        List<Op03SimpleStatement> originalTryTargets = ListFactory.newList(SetFactory.newOrderedSet(in.getTargets()));
        Collections.sort(originalTryTargets, new CompareByIndex());
        Op03SimpleStatement lastCatch = originalTryTargets.get(originalTryTargets.size() - 1);
        if (!(lastCatch.getStatement() instanceof CatchStatement)) {
            return;
        }
        List<PeerTries.PeerTrySet> triesByLevel = peerTries.getPeerTryGroups();
        Set catchBlocksToNop = SetFactory.newOrderedSet();
        Set blocksToRemoveCompletely = SetFactory.newSet();
        Set protectedStatements = SetFactory.newSet();
        for (Result result : results) {
            protectedStatements.add(result.getAfterEnd());
            protectedStatements.add(result.getStart());
        }
        final PeerTries.PeerTrySet originalTryGroupPeers = triesByLevel.get(0);
        for (final PeerTries.PeerTrySet peerSet : triesByLevel) {
            for (Op03SimpleStatement peerTry : peerSet.getPeerTries()) {
                if (peerTry == in) {
                    peerTry.removeTarget(possibleFinallyCatch);
                    possibleFinallyCatch.removeSource(peerTry);
                    continue;
                }
                if (!(peerTry.getStatement() instanceof TryStatement)) continue;
                TryStatement peerTryStmt = (TryStatement)peerTry.getStatement();
                final BlockIdentifier oldBlockIdent = peerTryStmt.getBlockIdentifier();
                List<Op03SimpleStatement> handlers = ListFactory.newList(peerTry.getTargets());
                int len = handlers.size();
                for (int x = 1; x < len; ++x) {
                    Op03SimpleStatement tgt = handlers.get(x);
                    tgt.removeSource(peerTry);
                    peerTry.removeTarget(tgt);
                    CatchStatement catchStatement = (CatchStatement)tgt.getStatement();
                    final BlockIdentifier catchBlockIdent = catchStatement.getCatchBlockIdent();
                    catchStatement.removeCatchBlockFor(oldBlockIdent);
                    List<Op03SimpleStatement> catchSources = tgt.getSources();
                    Set unionBlocks = SetFactory.newOrderedSet();
                    for (Op03SimpleStatement op03SimpleStatement : catchSources) {
                        unionBlocks.addAll(op03SimpleStatement.getBlockIdentifiers());
                    }
                    final Set<BlockIdentifier> previousTgtBlocks = SetFactory.newOrderedSet(tgt.getBlockIdentifiers());
                    previousTgtBlocks.removeAll(unionBlocks);
                    tgt.getBlockIdentifiers().removeAll(previousTgtBlocks);
                    if (!previousTgtBlocks.isEmpty()) {
                        tgt.getBlockIdentifiers().removeAll(previousTgtBlocks);
                        GraphVisitorDFS<Op03SimpleStatement> graphVisitorDFS = new GraphVisitorDFS<Op03SimpleStatement>(tgt.getTargets(), new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

                            @Override
                            public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                                if (arg1.getBlockIdentifiers().contains(catchBlockIdent)) {
                                    arg1.getBlockIdentifiers().removeAll(previousTgtBlocks);
                                    arg2.enqueue(arg1.getTargets());
                                }
                            }
                        });
                        graphVisitorDFS.process();
                    }
                    if (!tgt.getSources().isEmpty()) continue;
                    catchBlocksToNop.add(tgt);
                }
                if (protectedStatements.contains(peerTry)) {
                    peerTry.replaceStatement(new Nop());
                } else {
                    peerTry.nopOut();
                }
                if (peerSet.equals(originalTryGroupPeers)) {
                    peerTry.getBlockIdentifiers().add(tryBlockIdentifier);
                }
                GraphVisitorDFS<Op03SimpleStatement> gvpeer = new GraphVisitorDFS<Op03SimpleStatement>(handlers.get(0), new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

                    @Override
                    public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                        Set<BlockIdentifier> blockIdentifiers = arg1.getBlockIdentifiers();
                        if (blockIdentifiers.remove(oldBlockIdent)) {
                            if (peerSet == originalTryGroupPeers) {
                                blockIdentifiers.add(tryBlockIdentifier);
                                if (arg1.getTargets().contains(in)) {
                                    arg1.replaceTarget(in, (Op03SimpleStatement)targets.get(0));
                                    ((Op03SimpleStatement)targets.get(0)).addSource(arg1);
                                    in.removeSource(arg1);
                                }
                            }
                            arg2.enqueue(arg1.getTargets());
                            arg2.enqueue(arg1.getLinearlyNext());
                        }
                    }
                });
                gvpeer.process();
                blocksToRemoveCompletely.add(oldBlockIdent);
            }
        }
        CatchStatement catchStatement = (CatchStatement)lastCatch.getStatement();
        BlockIdentifier lastCatchIdent = catchStatement.getCatchBlockIdent();
        int found = -1;
        for (int x = allStatements.size() - 1; x >= 0; --x) {
            if (!allStatements.get(x).getBlockIdentifiers().contains(lastCatchIdent)) continue;
            found = x;
            break;
        }
        if (found == -1) {
            throw new IllegalStateException("Last catch has completely empty body");
        }
        Op03SimpleStatement lastCatchContentStatement = allStatements.get(found);
        InstrIndex newIdx = lastCatchContentStatement.getIndex().justAfter();
        Result cloneThis = results.iterator().next();
        List<Op03SimpleStatement> oldFinallyBody = ListFactory.newList(cloneThis.getToRemove());
        Collections.sort(oldFinallyBody, new CompareByIndex());
        List newFinallyBody = ListFactory.newList();
        Set<BlockIdentifier> oldStartBlocks = SetFactory.newOrderedSet(oldFinallyBody.get(0).getBlockIdentifiers());
        Set<BlockIdentifier> extraBlocks = SetFactory.newOrderedSet(in.getBlockIdentifiers());
        BlockIdentifier finallyBlock = blockIdentifierFactory.getNextBlockIdentifier(BlockType.CATCHBLOCK);
        FinallyStatement finallyStatement = new FinallyStatement(BytecodeLoc.TODO, finallyBlock);
        Op03SimpleStatement finallyOp = new Op03SimpleStatement(extraBlocks, finallyStatement, newIdx);
        newIdx = newIdx.justAfter();
        newFinallyBody.add(finallyOp);
        extraBlocks.add(finallyBlock);
        Map old2new = MapFactory.newOrderedMap();
        for (Op03SimpleStatement op03SimpleStatement : oldFinallyBody) {
            Statement statement = op03SimpleStatement.getStatement();
            Set<BlockIdentifier> newblocks = SetFactory.newOrderedSet(op03SimpleStatement.getBlockIdentifiers());
            newblocks.removeAll(oldStartBlocks);
            newblocks.addAll(extraBlocks);
            Iterator<Op03SimpleStatement> newOp = new Op03SimpleStatement(newblocks, statement, op03SimpleStatement.getSSAIdentifiers(), newIdx);
            newFinallyBody.add(newOp);
            newIdx = newIdx.justAfter();
            old2new.put(op03SimpleStatement, newOp);
        }
        if (newFinallyBody.size() > 1) {
            ((Op03SimpleStatement)newFinallyBody.get(1)).markFirstStatementInBlock(finallyBlock);
        }
        Op03SimpleStatement endRewrite = null;
        for (Result r : results) {
            Op03SimpleStatement rAfterEnd = r.getAfterEnd();
            if (rAfterEnd == null || !rAfterEnd.getIndex().isBackJumpFrom(r.getStart())) continue;
            endRewrite = new Op03SimpleStatement(extraBlocks, new GotoStatement(BytecodeLoc.TODO), newIdx);
            endRewrite.addTarget(rAfterEnd);
            rAfterEnd.addSource(endRewrite);
            break;
        }
        if (endRewrite == null) {
            endRewrite = new Op03SimpleStatement(extraBlocks, new CommentStatement(""), newIdx);
        }
        newFinallyBody.add(endRewrite);
        for (Op03SimpleStatement old : oldFinallyBody) {
            Op03SimpleStatement newOp = (Op03SimpleStatement)old2new.get(old);
            for (Op03SimpleStatement src : old.getSources()) {
                Op03SimpleStatement newSrc = (Op03SimpleStatement)old2new.get(src);
                if (newSrc == null) continue;
                newOp.addSource(newSrc);
            }
            for (Op03SimpleStatement tgt : old.getTargets()) {
                Op03SimpleStatement newTgt = (Op03SimpleStatement)old2new.get(tgt);
                if (newTgt == null) {
                    if (Misc.followNopGotoChain(tgt, false, false) == cloneThis.getAfterEnd()) {
                        endRewrite.addSource(newOp);
                        newTgt = endRewrite;
                    } else {
                        if (!(newOp.getStatement() instanceof JumpingStatement)) continue;
                        newTgt = tgt;
                        tgt.addSource(newOp);
                    }
                }
                newOp.addTarget(newTgt);
            }
        }
        if (newFinallyBody.size() >= 2) {
            Op03SimpleStatement op03SimpleStatement = (Op03SimpleStatement)newFinallyBody.get(1);
            op03SimpleStatement.addSource(finallyOp);
            finallyOp.addTarget(op03SimpleStatement);
        }
        for (Result result : results) {
            Op03SimpleStatement start = result.getStart();
            Set<Op03SimpleStatement> toRemove = result.getToRemove();
            Op03SimpleStatement afterEnd = result.getAfterEnd();
            List<Op03SimpleStatement> startSources = ListFactory.newList(start.getSources());
            for (Op03SimpleStatement op03SimpleStatement : startSources) {
                if (toRemove.contains(op03SimpleStatement)) continue;
                if (afterEnd != null) {
                    boolean canDirect;
                    boolean bl = canDirect = op03SimpleStatement.getStatement() instanceof JumpingStatement || op03SimpleStatement.getIndex().isBackJumpFrom(afterEnd);
                    if (canDirect && op03SimpleStatement.getStatement().getClass() == IfStatement.class && start == op03SimpleStatement.getTargets().get(0)) {
                        canDirect = false;
                    }
                    if (canDirect) {
                        op03SimpleStatement.replaceTarget(start, afterEnd);
                        afterEnd.addSource(op03SimpleStatement);
                        continue;
                    }
                    Op03SimpleStatement afterSource = new Op03SimpleStatement(op03SimpleStatement.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), op03SimpleStatement.getIndex().justAfter());
                    afterEnd.addSource(afterSource);
                    afterSource.addTarget(afterEnd);
                    afterSource.addSource(op03SimpleStatement);
                    op03SimpleStatement.replaceTarget(start, afterSource);
                    allStatements.add(afterSource);
                    continue;
                }
                Statement sourceStatement = op03SimpleStatement.getStatement();
                if (sourceStatement.getClass() == GotoStatement.class) {
                    op03SimpleStatement.replaceStatement(new Nop());
                    op03SimpleStatement.removeTarget(start);
                    continue;
                }
                if (sourceStatement.getClass() == IfStatement.class) {
                    Op03SimpleStatement tgtNop = new Op03SimpleStatement(op03SimpleStatement.getBlockIdentifiers(), new Nop(), start.getIndex().justBefore());
                    op03SimpleStatement.replaceTarget(start, tgtNop);
                    tgtNop.addSource(op03SimpleStatement);
                    allStatements.add(tgtNop);
                    continue;
                }
                JavaTypeInstance returnType = method.getMethodPrototype().getReturnType();
                if (returnType == RawJavaType.VOID) {
                    op03SimpleStatement.removeTarget(start);
                    continue;
                }
                if (sourceStatement instanceof AssignmentSimple) {
                    AssignmentSimple sourceAssignment = (AssignmentSimple)sourceStatement;
                    LValue lValue = sourceAssignment.getCreatedLValue();
                    JavaTypeInstance lValueType = lValue.getInferredJavaType().getJavaTypeInstance();
                    if (lValueType.implicitlyCastsTo(lValueType, null)) {
                        Op03SimpleStatement afterSource = new Op03SimpleStatement(op03SimpleStatement.getBlockIdentifiers(), new ReturnValueStatement(BytecodeLoc.TODO, new LValueExpression(lValue), returnType), op03SimpleStatement.getIndex().justAfter());
                        op03SimpleStatement.replaceTarget(start, afterSource);
                        afterSource.addSource(op03SimpleStatement);
                        allStatements.add(afterSource);
                        continue;
                    }
                    op03SimpleStatement.removeTarget(start);
                    continue;
                }
                op03SimpleStatement.removeTarget(start);
            }
            Set checkSources = SetFactory.newOrderedSet();
            for (Op03SimpleStatement remove : toRemove) {
                for (Op03SimpleStatement source3 : remove.getSources()) {
                    source3.getTargets().remove(remove);
                    checkSources.add(source3);
                }
                for (Op03SimpleStatement target : remove.getTargets()) {
                    target.getSources().remove(remove);
                }
                remove.getSources().clear();
                remove.getTargets().clear();
                remove.nopOut();
            }
            for (Op03SimpleStatement source4 : checkSources) {
                if (source4.getTargets().size() != 1) continue;
                int orig = allStatements.indexOf(source4);
                Op03SimpleStatement origTarget = source4.getTargets().get(0);
                int origTargetIdx = allStatements.indexOf(origTarget);
                if (origTargetIdx <= orig + 1) continue;
                Set<BlockIdentifier> blockIdentifiers = source4.getBlockIdentifiers();
                if (source4.getStatement() instanceof CatchStatement) {
                    blockIdentifiers = SetFactory.newSet(blockIdentifiers);
                    blockIdentifiers.add(((CatchStatement)source4.getStatement()).getCatchBlockIdent());
                }
                Op03SimpleStatement tmpJump = new Op03SimpleStatement(blockIdentifiers, new GotoStatement(BytecodeLoc.TODO), source4.getIndex().justAfter());
                source4.replaceTarget(origTarget, tmpJump);
                tmpJump.addSource(source4);
                tmpJump.addTarget(origTarget);
                origTarget.replaceSource(source4, tmpJump);
                allStatements.add(tmpJump);
            }
            if (afterEnd == null) continue;
            List<Op03SimpleStatement> list = ListFactory.newList(afterEnd.getSources());
            for (Op03SimpleStatement source5 : list) {
                if (!toRemove.contains(source5)) continue;
                afterEnd.removeSource(source5);
            }
        }
        for (Op03SimpleStatement topTry : originalTryGroupPeers.getPeerTries()) {
            Statement topStatement = topTry.getStatement();
            if (!(topStatement instanceof TryStatement)) continue;
            TryStatement topTryStatement = (TryStatement)topStatement;
            final BlockIdentifier topTryIdent = topTryStatement.getBlockIdentifier();
            final Set<Op03SimpleStatement> peerTryExits = SetFactory.newOrderedSet();
            GraphVisitorDFS<Op03SimpleStatement> gv2 = new GraphVisitorDFS<Op03SimpleStatement>(topTry.getTargets().get(0), new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

                @Override
                public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                    block3: {
                        if (arg1.getBlockIdentifiers().contains(topTryIdent)) {
                            arg2.enqueue(arg1.getTargets());
                            return;
                        }
                        if (!arg1.getTargets().isEmpty() && !arg1.getStatement().canThrow(ExceptionCheckSimple.INSTANCE)) {
                            for (Op03SimpleStatement tgt : arg1.getTargets()) {
                                if (tgt.getBlockIdentifiers().contains(topTryIdent)) continue;
                                break block3;
                            }
                            arg1.getBlockIdentifiers().add(topTryIdent);
                            arg2.enqueue(arg1.getTargets());
                            return;
                        }
                    }
                    peerTryExits.add(arg1);
                }
            });
            gv2.process();
            block22: for (Op03SimpleStatement peerTryExit : peerTryExits) {
                for (Op03SimpleStatement source : peerTryExit.getSources()) {
                    if (source.getBlockIdentifiers().contains(topTryIdent)) continue;
                    continue block22;
                }
                if (!peerTryExit.getIndex().isBackJumpFrom(finallyOp)) continue;
                peerTryExit.getBlockIdentifiers().add(topTryIdent);
            }
        }
        for (Op03SimpleStatement stm : allStatements) {
            stm.getBlockIdentifiers().removeAll(blocksToRemoveCompletely);
        }
        in.addTarget(finallyOp);
        finallyOp.addSource(in);
        allStatements.addAll(newFinallyBody);
    }

    private static boolean identifyFinally2(Op03SimpleStatement in, PeerTries peerTries, FinallyGraphHelper finallyGraphHelper, Set<Result> results) {
        if (!(in.getStatement() instanceof TryStatement)) {
            return false;
        }
        TryStatement tryStatement = (TryStatement)in.getStatement();
        final BlockIdentifier tryBlockIdentifier = tryStatement.getBlockIdentifier();
        List<Op03SimpleStatement> targets = in.getTargets();
        List<Op03SimpleStatement> catchStarts = Functional.filter(targets, new TypeFilter<CatchStatement>(CatchStatement.class));
        Set possibleCatches = SetFactory.newOrderedSet();
        Set<Op03SimpleStatement> recTries = SetFactory.newOrderedSet();
        for (Op03SimpleStatement op03SimpleStatement : catchStarts) {
            CatchStatement catchStatement = (CatchStatement)op03SimpleStatement.getStatement();
            List<ExceptionGroup.Entry> exceptions = catchStatement.getExceptions();
            for (ExceptionGroup.Entry exception : exceptions) {
                if (exception.getExceptionGroup().getTryBlockIdentifier() != tryBlockIdentifier) continue;
                JavaRefTypeInstance catchType = exception.getCatchType();
                if ("java.lang.Throwable".equals(catchType.getRawName())) {
                    possibleCatches.add(op03SimpleStatement);
                    continue;
                }
                Op03SimpleStatement catchTgt = op03SimpleStatement.getTargets().get(0);
                if (catchTgt.getStatement().getClass() != TryStatement.class) continue;
                recTries.add(catchTgt);
            }
        }
        if (possibleCatches.isEmpty()) {
            return false;
        }
        boolean result = false;
        for (Op03SimpleStatement recTry : recTries) {
            result |= FinalAnalyzer.identifyFinally2(recTry, peerTries, finallyGraphHelper, results);
        }
        final Set<Op03SimpleStatement> set = SetFactory.newOrderedSet();
        int attempt = 0;
        while (attempt < 2) {
            final int attemptCopy = attempt++;
            GraphVisitorDFS<Op03SimpleStatement> gv = new GraphVisitorDFS<Op03SimpleStatement>(in.getTargets().get(0), new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

                @Override
                public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                    if (arg1.getBlockIdentifiers().contains(tryBlockIdentifier)) {
                        if (arg1.isPossibleExitFor(tryBlockIdentifier) && attemptCopy == 1) {
                            set.add(arg1);
                        }
                        arg2.enqueue(arg1.getTargets());
                        Op03SimpleStatement linNext = arg1.getLinearlyNext();
                        if (linNext != null && linNext.getBlockIdentifiers().contains(tryBlockIdentifier)) {
                            arg2.enqueue(linNext);
                        }
                    } else {
                        if (arg1.getStatement() instanceof CaseStatement) {
                            arg1 = arg1.getTargets().get(0);
                        }
                        set.add(arg1);
                    }
                }
            });
            gv.process();
            if (!set.isEmpty()) break;
        }
        FinalAnalyzer.addPeerTries(set, peerTries);
        Set<BlockIdentifier> guessPeerTryBlocks = peerTries.getGuessPeerTryBlocks();
        Set<Op03SimpleStatement> guessPeerTryStarts = peerTries.getGuessPeerTryStarts();
        for (Op03SimpleStatement legitExitStart : set) {
            Result legitExitResult = finallyGraphHelper.match(legitExitStart);
            if (legitExitResult.isFail()) {
                Map<BlockIdentifier, Op03SimpleStatement> guessPeerTryMap;
                Op03SimpleStatement tryStart;
                Set<BlockIdentifier> exitBlocks = legitExitStart.getBlockIdentifiers();
                Set<BlockIdentifier> exitStartPeerBlocks = SetUtil.intersectionOrNull(guessPeerTryBlocks, exitBlocks);
                if (exitStartPeerBlocks != null && exitStartPeerBlocks.size() == 1 && (tryStart = (guessPeerTryMap = peerTries.getGuessPeerTryMap()).get(exitStartPeerBlocks.iterator().next())) != null) {
                    peerTries.add(tryStart);
                    continue;
                }
                boolean ok = false;
                boolean allowDirect = !legitExitStart.getStatement().canThrow(ExceptionCheckSimple.INSTANCE);
                Set<Op03SimpleStatement> addPeerTries = SetFactory.newOrderedSet();
                if (allowDirect) {
                    ok = true;
                    for (Op03SimpleStatement target : legitExitStart.getTargets()) {
                        Map<BlockIdentifier, Op03SimpleStatement> guessPeerTryMap2;
                        Op03SimpleStatement tryStart2;
                        if (guessPeerTryStarts.contains(target)) {
                            addPeerTries.add(target);
                            continue;
                        }
                        exitStartPeerBlocks = SetUtil.intersectionOrNull(guessPeerTryBlocks, target.getBlockIdentifiers());
                        if (exitStartPeerBlocks != null && exitStartPeerBlocks.size() == 1 && (tryStart2 = (guessPeerTryMap2 = peerTries.getGuessPeerTryMap()).get(exitStartPeerBlocks.iterator().next())) != null) {
                            peerTries.add(tryStart2);
                            continue;
                        }
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    for (Op03SimpleStatement addPeerTry : addPeerTries) {
                        peerTries.add(addPeerTry);
                    }
                    continue;
                }
                return result;
            }
            results.add(legitExitResult);
        }
        List<Op03SimpleStatement> tryTargets = in.getTargets();
        int len = tryTargets.size();
        for (int x = 1; x < len; ++x) {
            Op03SimpleStatement tryCatch = tryTargets.get(x);
            if (FinalAnalyzer.verifyCatchFinally(tryCatch, finallyGraphHelper, peerTries, results)) continue;
            return result;
        }
        return true;
    }

    private static void addPeerTries(Collection<Op03SimpleStatement> possibleFinally, PeerTries peerTries) {
        Set res = SetFactory.newOrderedSet();
        for (Op03SimpleStatement possible : possibleFinally) {
            if (possible.getStatement() instanceof TryStatement && possible.getTargets().contains(peerTries.getOriginalFinally())) {
                peerTries.add(possible);
                continue;
            }
            res.add(possible);
        }
        possibleFinally.clear();
        possibleFinally.addAll(res);
    }

    private static boolean verifyCatchFinally(Op03SimpleStatement in, FinallyGraphHelper finallyGraphHelper, PeerTries peerTries, Set<Result> results) {
        Result res;
        if (!(in.getStatement() instanceof CatchStatement)) {
            return false;
        }
        if (in.getTargets().size() != 1) {
            return false;
        }
        CatchStatement catchStatement = (CatchStatement)in.getStatement();
        final BlockIdentifier catchBlockIdent = catchStatement.getCatchBlockIdent();
        Op03SimpleStatement firstStatementInCatch = in.getTargets().get(0);
        final List statementsInCatch = ListFactory.newList();
        final Set<Op03SimpleStatement> targetsOutsideCatch = SetFactory.newOrderedSet();
        final Set<Op03SimpleStatement> directExitsFromCatch = SetFactory.newOrderedSet();
        final LazyMap<Op03SimpleStatement, Set<Op03SimpleStatement>> exitParents = MapFactory.newLazyMap(new UnaryFunction<Op03SimpleStatement, Set<Op03SimpleStatement>>(){

            @Override
            public Set<Op03SimpleStatement> invoke(Op03SimpleStatement arg) {
                return SetFactory.newOrderedSet();
            }
        });
        GraphVisitorDFS<Op03SimpleStatement> gv = new GraphVisitorDFS<Op03SimpleStatement>(firstStatementInCatch, new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

            @Override
            public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                if (arg1.getBlockIdentifiers().contains(catchBlockIdent)) {
                    statementsInCatch.add(arg1);
                    arg2.enqueue(arg1.getTargets());
                    for (Op03SimpleStatement tgt : arg1.getTargets()) {
                        ((Set)exitParents.get(tgt)).add(arg1);
                    }
                    Statement statement = arg1.getStatement();
                    if (statement instanceof ReturnStatement) {
                        directExitsFromCatch.add(arg1);
                    }
                } else {
                    targetsOutsideCatch.add(arg1);
                }
            }
        });
        gv.process();
        for (Op03SimpleStatement outsideCatch : targetsOutsideCatch) {
            directExitsFromCatch.addAll((Collection)exitParents.get(outsideCatch));
        }
        Op03SimpleStatement finallyCodeStart = finallyGraphHelper.getFinallyCatchBody().getCatchCodeStart();
        if (finallyCodeStart == null) {
            return false;
        }
        final Statement finallyStartStatement = finallyCodeStart.getStatement();
        List<Op03SimpleStatement> possibleFinalStarts = Functional.filter(statementsInCatch, new Predicate<Op03SimpleStatement>(){

            @Override
            public boolean test(Op03SimpleStatement in) {
                return in.getStatement().getClass() == finallyStartStatement.getClass();
            }
        });
        List<Result> possibleFinallyBlocks = ListFactory.newList();
        for (Op03SimpleStatement op03SimpleStatement : possibleFinalStarts) {
            Result res2 = finallyGraphHelper.match(op03SimpleStatement);
            if (res2.isFail()) continue;
            possibleFinallyBlocks.add(res2);
        }
        Map matchedFinallyBlockMap = MapFactory.newOrderedMap();
        for (Result res2 : possibleFinallyBlocks) {
            for (Op03SimpleStatement b : res2.getToRemove()) {
                matchedFinallyBlockMap.put(b, res2);
            }
        }
        List<Op03SimpleStatement> list = Functional.filter(statementsInCatch, new TypeFilter<TryStatement>(TryStatement.class));
        FinalAnalyzer.addPeerTries(list, peerTries);
        if (finallyGraphHelper.getFinallyCatchBody().hasThrowOp()) {
            for (Op03SimpleStatement exit : directExitsFromCatch) {
                res = (Result)matchedFinallyBlockMap.get(exit);
                if (res != null) continue;
                for (Op03SimpleStatement source : exit.getSources()) {
                    res = (Result)matchedFinallyBlockMap.get(source);
                    if (res == null) {
                        if (exit.getStatement() instanceof ThrowStatement) continue;
                        return false;
                    }
                    results.add(res);
                }
            }
        } else {
            for (Op03SimpleStatement exit : directExitsFromCatch) {
                res = (Result)matchedFinallyBlockMap.get(exit);
                if (res == null) {
                    if (exit.getStatement() instanceof ThrowStatement) continue;
                    return false;
                }
                results.add(res);
            }
        }
        return true;
    }

    private static Op03SimpleStatement findPossibleFinallyCatch(Set<Op03SimpleStatement> possibleCatches) {
        List<Op03SimpleStatement> tmp = ListFactory.newList(possibleCatches);
        Collections.sort(tmp, new CompareByIndex());
        Op03SimpleStatement catchS = tmp.get(tmp.size() - 1);
        return catchS;
    }
}

