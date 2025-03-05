/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.CompareByIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LValueProp;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CaseStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CommentStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.stack.StackEntry;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class ConditionalRewriter {
    public static void identifyNonjumpingConditionals(List<Op03SimpleStatement> statements, BlockIdentifierFactory blockIdentifierFactory, Options options) {
        boolean success;
        Set<Op03SimpleStatement> ignoreTheseJumps = SetFactory.newSet();
        boolean reduceSimpleScope = options.getOption(OptionsImpl.REDUCE_COND_SCOPE) == Troolean.TRUE;
        do {
            success = false;
            List<Op03SimpleStatement> forwardIfs = Functional.filter(statements, new IsForwardIf());
            Collections.reverse(forwardIfs);
            for (Op03SimpleStatement forwardIf : forwardIfs) {
                if (!ConditionalRewriter.considerAsTrivialIf(forwardIf, statements) && !ConditionalRewriter.considerAsSimpleIf(forwardIf, statements, blockIdentifierFactory, ignoreTheseJumps, reduceSimpleScope) && !ConditionalRewriter.considerAsDexIf(forwardIf, statements)) continue;
                success = true;
            }
        } while (success);
    }

    private static boolean considerAsTrivialIf(Op03SimpleStatement ifStatement, List<Op03SimpleStatement> statements) {
        int idxNotTaken;
        Op03SimpleStatement takenTarget = ifStatement.getTargets().get(1);
        Op03SimpleStatement notTakenTarget = ifStatement.getTargets().get(0);
        int idxTaken = statements.indexOf(takenTarget);
        if (idxTaken != (idxNotTaken = statements.indexOf(notTakenTarget)) + 1) {
            return false;
        }
        if (takenTarget.getStatement().getClass() != GotoStatement.class || notTakenTarget.getStatement().getClass() != GotoStatement.class || takenTarget.getTargets().get(0) != notTakenTarget.getTargets().get(0)) {
            return false;
        }
        notTakenTarget.replaceStatement(new CommentStatement("empty if block"));
        return false;
    }

    private static boolean considerAsDexIf(Op03SimpleStatement ifStatement, List<Op03SimpleStatement> statements) {
        InstrIndex bIndex;
        Statement innerStatement = ifStatement.getStatement();
        if (innerStatement.getClass() != IfStatement.class) {
            return false;
        }
        IfStatement innerIfStatement = (IfStatement)innerStatement;
        int startIdx = statements.indexOf(ifStatement);
        int bidx = statements.indexOf(ifStatement.getTargets().get(1));
        if (bidx <= startIdx) {
            return false;
        }
        InstrIndex startIndex = ifStatement.getIndex();
        if (startIndex.compareTo(bIndex = ifStatement.getTargets().get(1).getIndex()) >= 0) {
            return false;
        }
        int aidx = startIdx + 1;
        int cidx = ConditionalRewriter.findOverIdx(bidx, statements);
        if (cidx == -1) {
            return false;
        }
        int didx = ConditionalRewriter.findOverIdx(cidx, statements);
        if (didx == -1) {
            return false;
        }
        if (didx <= cidx) {
            return false;
        }
        Set<Op03SimpleStatement> permittedSources = SetFactory.newSet(ifStatement);
        if (!ConditionalRewriter.isRangeOnlyReachable(aidx, bidx, cidx, statements, permittedSources)) {
            return false;
        }
        if (!ConditionalRewriter.isRangeOnlyReachable(bidx, cidx, didx, statements, permittedSources)) {
            return false;
        }
        List<Op03SimpleStatement> alist = statements.subList(aidx, bidx);
        List<Op03SimpleStatement> blist = statements.subList(bidx, cidx);
        alist.get(alist.size() - 1).nopOut();
        List<Op03SimpleStatement> ifTargets = ifStatement.getTargets();
        Op03SimpleStatement tgtA = ifTargets.get(0);
        Op03SimpleStatement tgtB = ifTargets.get(1);
        ifTargets.set(0, tgtB);
        ifTargets.set(1, tgtA);
        innerIfStatement.setCondition(innerIfStatement.getCondition().getNegated().simplify());
        List<Op03SimpleStatement> acopy = ListFactory.newList(alist);
        blist.addAll(acopy);
        alist = statements.subList(aidx, bidx);
        alist.clear();
        Cleaner.reindexInPlace(statements);
        return true;
    }

    private static int findOverIdx(int startNext, List<Op03SimpleStatement> statements) {
        Op03SimpleStatement next = statements.get(startNext);
        Op03SimpleStatement cStatement = null;
        for (int gSearch = startNext - 1; gSearch >= 0; --gSearch) {
            Op03SimpleStatement stm = statements.get(gSearch);
            Statement s = stm.getStatement();
            if (s instanceof Nop) continue;
            if (s.getClass() == GotoStatement.class) {
                Op03SimpleStatement tgtC = stm.getTargets().get(0);
                if (tgtC.getIndex().isBackJumpFrom(next)) {
                    return -1;
                }
                cStatement = tgtC;
                break;
            }
            return -1;
        }
        if (cStatement == null) {
            return -1;
        }
        int cidx = statements.indexOf(cStatement);
        return cidx;
    }

    private static boolean isRangeOnlyReachable(int startIdx, int endIdx, int tgtIdx, List<Op03SimpleStatement> statements, Set<Op03SimpleStatement> permittedSources) {
        Set reachable = SetFactory.newSet();
        Op03SimpleStatement startStatement = statements.get(startIdx);
        Op03SimpleStatement endStatement = statements.get(endIdx);
        Op03SimpleStatement tgtStatement = statements.get(tgtIdx);
        InstrIndex startIndex = startStatement.getIndex();
        InstrIndex endIndex = endStatement.getIndex();
        InstrIndex finalTgtIndex = tgtStatement.getIndex();
        reachable.add(statements.get(startIdx));
        boolean foundEnd = false;
        for (int idx = startIdx; idx < endIdx; ++idx) {
            Op03SimpleStatement stm = statements.get(idx);
            if (!reachable.contains(stm)) {
                return false;
            }
            for (Op03SimpleStatement source : stm.getSources()) {
                InstrIndex sourceIndex = source.getIndex();
                if (sourceIndex.compareTo(startIndex) < 0 && !permittedSources.contains(source)) {
                    return false;
                }
                if (sourceIndex.compareTo(endIndex) < 0) continue;
                return false;
            }
            for (Op03SimpleStatement target : stm.getTargets()) {
                InstrIndex tgtIndex = target.getIndex();
                if (tgtIndex.compareTo(startIndex) < 0) {
                    return false;
                }
                if (tgtIndex.compareTo(endIndex) >= 0) {
                    if (tgtIndex == finalTgtIndex) {
                        foundEnd = true;
                    } else {
                        return false;
                    }
                }
                reachable.add(target);
            }
        }
        return foundEnd;
    }

    private static boolean detectAndRemarkJumpIntoOther(Set<BlockIdentifier> blocksAtStart, Set<BlockIdentifier> blocksAtEnd, Op03SimpleStatement realEnd, Op03SimpleStatement ifStatement) {
        if (blocksAtEnd.size() != blocksAtStart.size() + 1) {
            return false;
        }
        List<BlockIdentifier> diff = SetUtil.differenceAtakeBtoList(blocksAtEnd, blocksAtStart);
        BlockIdentifier testBlock = diff.get(0);
        if (testBlock.getBlockType() != BlockType.SIMPLE_IF_TAKEN) {
            return false;
        }
        List<Op03SimpleStatement> realEndTargets = realEnd.getTargets();
        if (realEndTargets.size() != 1 || realEndTargets.get(0).getLinearlyPrevious() != realEnd) {
            return false;
        }
        Op03SimpleStatement afterRealEnd = realEndTargets.get(0);
        List<Op03SimpleStatement> areSources = afterRealEnd.getSources();
        if (areSources.size() != 2) {
            return false;
        }
        Op03SimpleStatement other = areSources.get(0) == realEnd ? areSources.get(1) : areSources.get(0);
        Statement otherStatement = other.getStatement();
        if (!other.getIndex().isBackJumpTo(ifStatement)) {
            return false;
        }
        if (!(otherStatement instanceof IfStatement)) {
            return false;
        }
        Pair<BlockIdentifier, BlockIdentifier> knownBlocks = ((IfStatement)otherStatement).getBlocks();
        if (knownBlocks.getFirst() != testBlock || knownBlocks.getSecond() != null) {
            return false;
        }
        ((IfStatement)otherStatement).setJumpType(JumpType.BREAK_ANONYMOUS);
        for (Op03SimpleStatement current = other.getLinearlyNext(); current != null && current.getBlockIdentifiers().contains(testBlock); current = current.getLinearlyNext()) {
            current.getBlockIdentifiers().remove(testBlock);
        }
        return true;
    }

    private static boolean considerAsSimpleIf(Op03SimpleStatement ifStatement, List<Op03SimpleStatement> statements, BlockIdentifierFactory blockIdentifierFactory, Set<Op03SimpleStatement> ignoreTheseJumps, boolean reduceSimpleScope) {
        Object beforeRealEnd;
        List<BlockIdentifier> change;
        Op03SimpleStatement realEnd;
        Set<BlockIdentifier> blocksAtEnd;
        Op03SimpleStatement takenTarget = ifStatement.getTargets().get(1);
        Op03SimpleStatement notTakenTarget = ifStatement.getTargets().get(0);
        int idxTaken = statements.indexOf(takenTarget);
        int idxNotTaken = statements.indexOf(notTakenTarget);
        IfStatement innerIfStatement = (IfStatement)ifStatement.getStatement();
        Set ignoreLocally = SetFactory.newSet();
        boolean takenAction = false;
        int idxCurrent = idxNotTaken;
        if (idxCurrent > idxTaken) {
            return false;
        }
        int idxEnd = idxTaken;
        int maybeElseEndIdx = -1;
        Op03SimpleStatement maybeElseEnd = null;
        boolean maybeSimpleIfElse = false;
        GotoStatement leaveIfBranchGoto = null;
        Op03SimpleStatement leaveIfBranchHolder = null;
        List<Op03SimpleStatement> ifBranch = ListFactory.newList();
        List<Op03SimpleStatement> elseBranch = null;
        Set<BlockIdentifier> blocksAtStart = ifStatement.getBlockIdentifiers();
        if (idxCurrent == idxEnd) {
            Op03SimpleStatement taken = new Op03SimpleStatement(blocksAtStart, new CommentStatement("empty if block"), notTakenTarget.getIndex().justBefore());
            taken.addSource(ifStatement);
            taken.addTarget(notTakenTarget);
            Op03SimpleStatement emptyTarget = ifStatement.getTargets().get(0);
            if (notTakenTarget != emptyTarget) {
                notTakenTarget.addSource(taken);
            }
            emptyTarget.replaceSource(ifStatement, taken);
            ifStatement.getTargets().set(0, taken);
            statements.add(idxTaken, taken);
            BlockIdentifier ifBlockLabel = blockIdentifierFactory.getNextBlockIdentifier(BlockType.SIMPLE_IF_TAKEN);
            taken.markFirstStatementInBlock(ifBlockLabel);
            taken.getBlockIdentifiers().add(ifBlockLabel);
            innerIfStatement.setKnownBlocks(ifBlockLabel, null);
            innerIfStatement.setJumpType(JumpType.GOTO_OUT_OF_IF);
            return true;
        }
        Set validForwardParents = SetFactory.newSet();
        validForwardParents.add(ifStatement);
        Op03SimpleStatement stmtLastBlock = statements.get(idxTaken - 1);
        Op03SimpleStatement stmtLastBlockRewrite = null;
        Statement stmtLastBlockInner = stmtLastBlock.getStatement();
        if (stmtLastBlockInner.getClass() == GotoStatement.class) {
            stmtLastBlockRewrite = stmtLastBlock;
        }
        Op03SimpleStatement statementStart = statements.get(idxCurrent);
        Predicate<BlockIdentifier> tryBlockFilter = new Predicate<BlockIdentifier>(){

            @Override
            public boolean test(BlockIdentifier in) {
                return in.getBlockType() == BlockType.TRYBLOCK;
            }
        };
        Set<BlockIdentifier> startTryBlocks = Functional.filterSet(statementStart.getBlockIdentifiers(), tryBlockFilter);
        Op03SimpleStatement statementPrev = statementStart;
        do {
            Op03SimpleStatement currentPrev;
            Op03SimpleStatement statementCurrent = statements.get(idxCurrent);
            boolean currentParent = false;
            if (idxCurrent > 0 && (currentPrev = statements.get(idxCurrent - 1)).getTargets().contains(statementCurrent) && currentPrev.getStatement().fallsToNext()) {
                currentParent = true;
            }
            InstrIndex currentIndex = statementCurrent.getIndex();
            if (!currentParent) {
                for (Op03SimpleStatement source : statementCurrent.getSources()) {
                    if (!currentIndex.isBackJumpTo(source) || validForwardParents.contains(source)) continue;
                    if (statementPrev != statementStart && !SetUtil.equals(Functional.filterSet(statementPrev.getBlockIdentifiers(), tryBlockFilter), startTryBlocks)) {
                        return false;
                    }
                    Op03SimpleStatement newJump = new Op03SimpleStatement(ifStatement.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), statementCurrent.getIndex().justBefore());
                    if (statementCurrent == ifStatement.getTargets().get(0)) continue;
                    Op03SimpleStatement oldTarget = ifStatement.getTargets().get(1);
                    newJump.addTarget(oldTarget);
                    newJump.addSource(ifStatement);
                    ifStatement.replaceTarget(oldTarget, newJump);
                    oldTarget.replaceSource(ifStatement, newJump);
                    statements.add(idxCurrent, newJump);
                    return true;
                }
            }
            validForwardParents.add(statementCurrent);
            ifBranch.add(statementCurrent);
            JumpType jumpType = statementCurrent.getJumpType();
            if (jumpType.isUnknown() && !ignoreTheseJumps.contains(statementCurrent)) {
                if (idxCurrent == idxTaken - 1) {
                    Statement mGotoStatement = statementCurrent.getStatement();
                    if (mGotoStatement.getClass() != GotoStatement.class) {
                        return false;
                    }
                    GotoStatement gotoStatement = (GotoStatement)mGotoStatement;
                    maybeElseEnd = statementCurrent.getTargets().get(0);
                    maybeElseEndIdx = statements.indexOf(maybeElseEnd);
                    if (maybeElseEnd.getIndex().compareTo(takenTarget.getIndex()) <= 0) {
                        return false;
                    }
                    leaveIfBranchHolder = statementCurrent;
                    leaveIfBranchGoto = gotoStatement;
                    maybeSimpleIfElse = true;
                } else {
                    if (stmtLastBlockRewrite == null) {
                        Op03SimpleStatement tgtContainer = statementCurrent.getTargets().get(0);
                        if (tgtContainer == takenTarget) {
                            ++idxCurrent;
                            continue;
                        }
                        return false;
                    }
                    List<Op03SimpleStatement> targets = statementCurrent.getTargets();
                    Op03SimpleStatement eventualTarget = stmtLastBlockRewrite.getTargets().get(0);
                    boolean found = false;
                    for (int x = 0; x < targets.size(); ++x) {
                        Op03SimpleStatement target = targets.get(x);
                        if (target != eventualTarget || target == stmtLastBlockRewrite) continue;
                        targets.set(x, stmtLastBlockRewrite);
                        stmtLastBlockRewrite.addSource(statementCurrent);
                        if (eventualTarget.getSources().contains(stmtLastBlockRewrite)) {
                            eventualTarget.removeSource(statementCurrent);
                        } else {
                            eventualTarget.replaceSource(statementCurrent, stmtLastBlockRewrite);
                        }
                        found = true;
                    }
                    return found;
                }
            }
            ++idxCurrent;
            statementPrev = statementCurrent;
        } while (idxCurrent != idxEnd);
        boolean reducedScope = false;
        if (maybeSimpleIfElse) {
            int test;
            elseBranch = ListFactory.newList();
            idxCurrent = idxTaken;
            if (reduceSimpleScope && (test = Misc.getFarthestReachableInRange(statements, idxCurrent, maybeElseEndIdx)) < maybeElseEndIdx - 1) {
                InstrIndex startIdx = statementStart.getIndex();
                block3: for (int x = test + 1; x < maybeElseEndIdx; ++x) {
                    Op03SimpleStatement testStm = statements.get(x);
                    for (Op03SimpleStatement source : testStm.getSources()) {
                        if (!source.getIndex().isBackJumpFrom(startIdx)) continue;
                        reducedScope = true;
                        break block3;
                    }
                }
                if (reducedScope) {
                    maybeElseEndIdx = test + 1;
                }
            }
            idxEnd = maybeElseEndIdx;
            do {
                Op03SimpleStatement statementCurrent = statements.get(idxCurrent);
                elseBranch.add(statementCurrent);
                JumpType jumpType = statementCurrent.getJumpType();
                if (!jumpType.isUnknown()) continue;
                Statement mGotoStatement = statementCurrent.getStatement();
                if (mGotoStatement.getClass() != GotoStatement.class) {
                    return false;
                }
                if (statementCurrent.getTargets().get(0) == maybeElseEnd) {
                    idxEnd = idxCurrent--;
                    leaveIfBranchHolder.replaceTarget(maybeElseEnd, statementCurrent);
                    statementCurrent.addSource(leaveIfBranchHolder);
                    maybeElseEnd.removeSource(leaveIfBranchHolder);
                    elseBranch.remove(statementCurrent);
                    takenAction = true;
                    continue;
                }
                return false;
            } while (++idxCurrent != idxEnd);
        }
        if (!(blocksAtStart.equals(blocksAtEnd = (realEnd = statements.get(idxEnd)).getBlockIdentifiers()) || blocksAtStart.size() == blocksAtEnd.size() + 1 && (change = SetUtil.differenceAtakeBtoList(blocksAtStart, blocksAtEnd)).size() == 1 && change.get(0).getBlockType() == BlockType.CASE && (takenTarget.getStatement() instanceof CaseStatement && stmtLastBlock.getBlockIdentifiers().contains(change.get(0)) || blocksAtStart.equals(((Op03SimpleStatement)(beforeRealEnd = statements.get(idxEnd - 1))).getBlockIdentifiers())) || ConditionalRewriter.detectAndRemarkJumpIntoOther(blocksAtStart, blocksAtEnd, realEnd, ifStatement))) {
            return takenAction;
        }
        DiscoveredTernary ternary = ConditionalRewriter.testForTernary(ifBranch, elseBranch, leaveIfBranchHolder);
        if (ternary != null) {
            for (Op03SimpleStatement statement : ifBranch) {
                statement.nopOut();
            }
            for (Op03SimpleStatement statement : elseBranch) {
                statement.nopOut();
            }
            ifStatement.forceSSAIdentifiers(leaveIfBranchHolder.getSSAIdentifiers());
            ConditionalExpression conditionalExpression = innerIfStatement.getCondition().getNegated().simplify();
            Expression rhs = ternary.isPointlessBoolean() ? conditionalExpression : new TernaryExpression(BytecodeLoc.TODO, conditionalExpression, ternary.e1, ternary.e2);
            ifStatement.replaceStatement(new AssignmentSimple(BytecodeLoc.TODO, ternary.lValue, rhs));
            if (ternary.lValue instanceof StackSSALabel) {
                StackSSALabel stackSSALabel = (StackSSALabel)ternary.lValue;
                StackEntry stackEntry = stackSSALabel.getStackEntry();
                stackEntry.decSourceCount();
            }
            List<Op03SimpleStatement> tmp = ListFactory.uniqueList(ifStatement.getTargets());
            ifStatement.getTargets().clear();
            ifStatement.getTargets().addAll(tmp);
            if (ifStatement.getTargets().size() != 1) {
                throw new ConfusedCFRException("If statement should only have one target after dedup");
            }
            Op03SimpleStatement joinStatement = ifStatement.getTargets().get(0);
            tmp = ListFactory.uniqueList(joinStatement.getSources());
            joinStatement.getSources().clear();
            joinStatement.getSources().addAll(tmp);
            LValueProp.condenseLValues(statements);
            return true;
        }
        BlockIdentifier elseBlockLabel = null;
        if (maybeSimpleIfElse && elseBranch.isEmpty()) {
            maybeSimpleIfElse = false;
        }
        boolean flipBlocks = false;
        if (maybeSimpleIfElse) {
            Op03SimpleStatement elseStart;
            Pair<Set<Op03SimpleStatement>, Set<Op03SimpleStatement>> reachinfo;
            Set<Op03SimpleStatement> reachableElse;
            elseBlockLabel = blockIdentifierFactory.getNextBlockIdentifier(BlockType.SIMPLE_IF_ELSE);
            Misc.markWholeBlock(elseBranch, elseBlockLabel);
            Set<Op03SimpleStatement> allIfSources = Misc.collectAllSources(ifBranch);
            allIfSources.removeAll(ifBranch);
            allIfSources.remove(ifStatement);
            if (!allIfSources.isEmpty() && (reachableElse = (reachinfo = Misc.GraphVisitorBlockReachable.getBlockReachableAndExits(elseStart = elseBranch.get(0), elseBlockLabel)).getFirst()).size() == elseBranch.size() && reachinfo.getSecond().isEmpty()) {
                innerIfStatement.negateCondition();
                List<Op03SimpleStatement> targets = ifStatement.getTargets();
                Op03SimpleStatement tgt0 = targets.get(0);
                Op03SimpleStatement tgt1 = targets.get(1);
                targets.clear();
                targets.add(tgt1);
                targets.add(tgt0);
                leaveIfBranchGoto = null;
                List<Op03SimpleStatement> oldIfBranch = ifBranch;
                ifBranch = elseBranch;
                Collections.sort(ifBranch, new CompareByIndex());
                Op03SimpleStatement last = ifBranch.get(ifBranch.size() - 1);
                InstrIndex fromHere = last.getIndex().justAfter();
                Cleaner.sortAndRenumberFromInPlace(oldIfBranch, fromHere);
                ignoreLocally.removeAll(oldIfBranch);
                flipBlocks = true;
                elseBlockLabel.setBlockType(BlockType.SIMPLE_IF_TAKEN);
            }
        }
        BlockIdentifier ifBlockLabel = blockIdentifierFactory.getNextBlockIdentifier(BlockType.SIMPLE_IF_TAKEN);
        if (flipBlocks) {
            ifBlockLabel = elseBlockLabel;
            elseBlockLabel = null;
        } else {
            if (!maybeSimpleIfElse) {
                for (Op03SimpleStatement stm : ifBranch) {
                    for (Op03SimpleStatement source : stm.getSources()) {
                        if (!source.getIndex().isBackJumpFrom(ifStatement)) continue;
                        return false;
                    }
                }
            }
            Misc.markWholeBlock(ifBranch, ifBlockLabel);
        }
        if (!reducedScope && leaveIfBranchGoto != null) {
            leaveIfBranchGoto.setJumpType(JumpType.GOTO_OUT_OF_IF);
        }
        innerIfStatement.setJumpType(JumpType.GOTO_OUT_OF_IF);
        innerIfStatement.setKnownBlocks(ifBlockLabel, elseBlockLabel);
        ignoreTheseJumps.addAll(ignoreLocally);
        if (flipBlocks) {
            Cleaner.sortAndRenumberInPlace(statements);
        }
        return true;
    }

    private static DiscoveredTernary testForTernary(List<Op03SimpleStatement> ifBranch, List<Op03SimpleStatement> elseBranch, Op03SimpleStatement leaveIfBranch) {
        if (ifBranch == null || elseBranch == null) {
            return null;
        }
        if (leaveIfBranch == null) {
            return null;
        }
        TypeFilter<Nop> notNops = new TypeFilter<Nop>(Nop.class, false);
        ifBranch = Functional.filter(ifBranch, notNops);
        switch (ifBranch.size()) {
            case 1: {
                break;
            }
            case 2: {
                if (ifBranch.get(1) == leaveIfBranch) break;
                return null;
            }
            default: {
                return null;
            }
        }
        if (ifBranch.get(0).getSources().size() != 1) {
            return null;
        }
        if ((elseBranch = Functional.filter(elseBranch, notNops)).size() != 1) {
            return null;
        }
        if (elseBranch.get(0).getSources().size() != 1) {
            return null;
        }
        Op03SimpleStatement s1 = ifBranch.get(0);
        Op03SimpleStatement s2 = elseBranch.get(0);
        if (s2.getSources().size() != 1) {
            return null;
        }
        LValue l1 = s1.getStatement().getCreatedLValue();
        LValue l2 = s2.getStatement().getCreatedLValue();
        if (l1 == null || l2 == null) {
            return null;
        }
        if (!l2.equals(l1)) {
            return null;
        }
        return new DiscoveredTernary(l1, s1.getStatement().getRValue(), s2.getStatement().getRValue());
    }

    private static class DiscoveredTernary {
        LValue lValue;
        Expression e1;
        Expression e2;

        private DiscoveredTernary(LValue lValue, Expression e1, Expression e2) {
            this.lValue = lValue;
            this.e1 = e1;
            this.e2 = e2;
        }

        private static Troolean isOneOrZeroLiteral(Expression e) {
            if (!(e instanceof Literal)) {
                return Troolean.NEITHER;
            }
            TypedLiteral typedLiteral = ((Literal)e).getValue();
            Object value = typedLiteral.getValue();
            if (!(value instanceof Integer)) {
                return Troolean.NEITHER;
            }
            int iValue = (Integer)value;
            if (iValue == 1) {
                return Troolean.TRUE;
            }
            if (iValue == 0) {
                return Troolean.FALSE;
            }
            return Troolean.NEITHER;
        }

        private boolean isPointlessBoolean() {
            if (this.e1.getInferredJavaType().getRawType() != RawJavaType.BOOLEAN || this.e2.getInferredJavaType().getRawType() != RawJavaType.BOOLEAN) {
                return false;
            }
            if (DiscoveredTernary.isOneOrZeroLiteral(this.e1) != Troolean.TRUE) {
                return false;
            }
            return DiscoveredTernary.isOneOrZeroLiteral(this.e2) == Troolean.FALSE;
        }
    }

    private static class IsForwardIf
    implements Predicate<Op03SimpleStatement> {
        private IsForwardIf() {
        }

        @Override
        public boolean test(Op03SimpleStatement in) {
            if (!(in.getStatement() instanceof IfStatement)) {
                return false;
            }
            IfStatement ifStatement = (IfStatement)in.getStatement();
            if (!ifStatement.getJumpType().isUnknown()) {
                return false;
            }
            return in.getTargets().get(1).getIndex().compareTo(in.getIndex()) > 0;
        }
    }
}

