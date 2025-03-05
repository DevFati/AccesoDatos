/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractAssignment;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.DoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfExitingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.WhileStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierUtils;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

class WhileRewriter {
    WhileRewriter() {
    }

    private static void rewriteDoWhileTruePredAsWhile(Op03SimpleStatement end, List<Op03SimpleStatement> statements) {
        WhileStatement whileStatement = (WhileStatement)end.getStatement();
        if (null != whileStatement.getCondition()) {
            return;
        }
        List<Op03SimpleStatement> endTargets = end.getTargets();
        if (endTargets.size() != 1) {
            return;
        }
        Op03SimpleStatement loopStart = endTargets.get(0);
        Statement loopBodyStartStatement = loopStart.getStatement();
        BlockIdentifier whileBlockIdentifier = whileStatement.getBlockIdentifier();
        Op03SimpleStatement doStart = null;
        for (Op03SimpleStatement source : loopStart.getSources()) {
            DoStatement doStatement;
            Statement statement = source.getStatement();
            if (statement.getClass() != DoStatement.class || (doStatement = (DoStatement)statement).getBlockIdentifier() != whileBlockIdentifier) continue;
            doStart = source;
            break;
        }
        if (doStart == null) {
            return;
        }
        if (loopBodyStartStatement.getClass() == IfStatement.class) {
            return;
        }
        if (loopBodyStartStatement.getClass() == IfExitingStatement.class) {
            IfExitingStatement ifExitingStatement = (IfExitingStatement)loopBodyStartStatement;
            Statement exitStatement = ifExitingStatement.getExitStatement();
            ConditionalExpression conditionalExpression = ifExitingStatement.getCondition();
            WhileStatement replacementWhile = new WhileStatement(BytecodeLoc.TODO, conditionalExpression.getNegated(), whileBlockIdentifier);
            GotoStatement endGoto = new GotoStatement(BytecodeLoc.TODO);
            endGoto.setJumpType(JumpType.CONTINUE);
            end.replaceStatement(endGoto);
            Op03SimpleStatement after = new Op03SimpleStatement(doStart.getBlockIdentifiers(), exitStatement, end.getIndex().justAfter());
            int endIdx = statements.indexOf(end);
            if (endIdx < statements.size() - 2) {
                Op03SimpleStatement shuffled = statements.get(endIdx + 1);
                for (Op03SimpleStatement shuffledSource : shuffled.getSources()) {
                    JumpingStatement jumpingStatement;
                    if (!(shuffledSource.getStatement() instanceof JumpingStatement) || (jumpingStatement = (JumpingStatement)shuffledSource.getStatement()).getJumpType() != JumpType.BREAK) continue;
                    jumpingStatement.setJumpType(JumpType.GOTO);
                }
            }
            statements.add(endIdx + 1, after);
            doStart.addTarget(after);
            after.addSource(doStart);
            doStart.replaceStatement(replacementWhile);
            Op03SimpleStatement afterLoopStart = loopStart.getTargets().get(0);
            doStart.replaceTarget(loopStart, afterLoopStart);
            afterLoopStart.replaceSource(loopStart, doStart);
            loopStart.removeSource(doStart);
            loopStart.removeTarget(afterLoopStart);
            for (Op03SimpleStatement otherSource : loopStart.getSources()) {
                otherSource.replaceTarget(loopStart, doStart);
                doStart.addSource(otherSource);
            }
            loopStart.getSources().clear();
            loopStart.nopOut();
            whileBlockIdentifier.setBlockType(BlockType.WHILELOOP);
            return;
        }
    }

    static void rewriteDoWhileTruePredAsWhile(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> doWhileEnds = Functional.filter(statements, new Predicate<Op03SimpleStatement>(){

            @Override
            public boolean test(Op03SimpleStatement in) {
                return in.getStatement() instanceof WhileStatement && ((WhileStatement)in.getStatement()).getBlockIdentifier().getBlockType() == BlockType.UNCONDITIONALDOLOOP;
            }
        });
        if (doWhileEnds.isEmpty()) {
            return;
        }
        for (Op03SimpleStatement whileEnd : doWhileEnds) {
            WhileRewriter.rewriteDoWhileTruePredAsWhile(whileEnd, statements);
        }
    }

    private static Set<LValue> findForInvariants(Op03SimpleStatement start, BlockIdentifier whileLoop) {
        Set<LValue> res = SetFactory.newOrderedSet();
        Op03SimpleStatement current = start;
        while (current.getBlockIdentifiers().contains(whileLoop)) {
            AbstractAssignment assignment;
            if (current.getStatement() instanceof AbstractAssignment && (assignment = (AbstractAssignment)current.getStatement()).isSelfMutatingOperation()) {
                SSAIdent expected;
                LValue lValue = assignment.getCreatedLValue();
                SSAIdent after = current.getSSAIdentifiers().getSSAIdentOnExit(lValue);
                if (!after.equals(expected = start.getSSAIdentifiers().getSSAIdentOnEntry(lValue))) break;
                res.add(lValue);
            }
            if (current.getSources().size() > 1) break;
            Op03SimpleStatement next = current.getSources().get(0);
            if (!current.getIndex().isBackJumpTo(next)) break;
            current = next;
        }
        return res;
    }

    private static void rewriteWhileAsFor(Op03SimpleStatement statement, boolean aggcapture) {
        List<Op03SimpleStatement> backSources = Functional.filter(statement.getSources(), new Misc.IsBackJumpTo(statement.getIndex()));
        WhileStatement whileStatement = (WhileStatement)statement.getStatement();
        ConditionalExpression condition = whileStatement.getCondition();
        Set<LValue> loopVariablePossibilities = condition.getLoopLValues();
        if (loopVariablePossibilities.isEmpty()) {
            return;
        }
        BlockIdentifier whileBlockIdentifier = whileStatement.getBlockIdentifier();
        Set<LValue> reverseOrderedMutatedPossibilities = null;
        for (Op03SimpleStatement source : backSources) {
            Set<LValue> incrPoss = WhileRewriter.findForInvariants(source, whileBlockIdentifier);
            if (reverseOrderedMutatedPossibilities == null) {
                reverseOrderedMutatedPossibilities = incrPoss;
            } else {
                reverseOrderedMutatedPossibilities.retainAll(incrPoss);
            }
            if (!reverseOrderedMutatedPossibilities.isEmpty()) continue;
            return;
        }
        if (reverseOrderedMutatedPossibilities == null || reverseOrderedMutatedPossibilities.isEmpty()) {
            return;
        }
        loopVariablePossibilities.retainAll(reverseOrderedMutatedPossibilities);
        if (loopVariablePossibilities.isEmpty()) {
            return;
        }
        Op03SimpleStatement loopVariableOp = null;
        LValue loopVariable = null;
        for (LValue loopVariablePoss : loopVariablePossibilities) {
            Op03SimpleStatement initialValue = WhileRewriter.findMovableAssignment(statement, loopVariablePoss);
            if (initialValue == null || loopVariableOp != null && !initialValue.getIndex().isBackJumpTo(loopVariableOp)) continue;
            loopVariableOp = initialValue;
            loopVariable = loopVariablePoss;
        }
        if (loopVariable == null) {
            return;
        }
        AssignmentSimple initalAssignmentSimple = null;
        List<AbstractAssignmentExpression> postUpdates = ListFactory.newList();
        List<List> usedMutatedPossibilities = ListFactory.newList();
        boolean usesLoopVar = false;
        for (LValue otherMutant : reverseOrderedMutatedPossibilities) {
            List<Op03SimpleStatement> othermutations = WhileRewriter.getMutations(backSources, otherMutant, whileBlockIdentifier);
            if (othermutations == null) continue;
            if (!loopVariablePossibilities.contains(otherMutant) && !aggcapture) break;
            if (otherMutant.equals(loopVariable)) {
                usesLoopVar = true;
            }
            AbstractAssignmentExpression postUpdate2 = ((AbstractAssignment)((Op03SimpleStatement)othermutations.get(0)).getStatement()).getInliningExpression();
            postUpdates.add(postUpdate2);
            usedMutatedPossibilities.add(othermutations);
        }
        if (!usesLoopVar) {
            return;
        }
        Collections.reverse(postUpdates);
        for (List lst : usedMutatedPossibilities) {
            for (Op03SimpleStatement op : lst) {
                op.nopOut();
            }
        }
        if (loopVariableOp != null) {
            initalAssignmentSimple = (AssignmentSimple)loopVariableOp.getStatement();
            loopVariableOp.nopOut();
        }
        whileBlockIdentifier.setBlockType(BlockType.FORLOOP);
        whileStatement.replaceWithForLoop(initalAssignmentSimple, postUpdates);
        for (Op03SimpleStatement source : backSources) {
            if (!source.getBlockIdentifiers().contains(whileBlockIdentifier)) continue;
            List<Op03SimpleStatement> ssources = ListFactory.newList(source.getSources());
            for (Op03SimpleStatement ssource : ssources) {
                JumpingStatement jumpingStatement;
                Statement sstatement;
                if (!ssource.getBlockIdentifiers().contains(whileBlockIdentifier) || !((sstatement = ssource.getStatement()) instanceof JumpingStatement) || (jumpingStatement = (JumpingStatement)sstatement).getJumpTarget().getContainer() != source) continue;
                ((JumpingStatement)sstatement).setJumpType(JumpType.CONTINUE);
                ssource.replaceTarget(source, statement);
                statement.addSource(ssource);
                source.removeSource(ssource);
            }
        }
    }

    private static Op03SimpleStatement findMovableAssignment(Op03SimpleStatement start, LValue lValue) {
        Op03SimpleStatement current = Misc.findSingleBackSource(start);
        if (current == null) {
            return null;
        }
        do {
            AssignmentSimple assignmentSimple;
            if (current.getStatement() instanceof AssignmentSimple && (assignmentSimple = (AssignmentSimple)current.getStatement()).getCreatedLValue().equals(lValue)) {
                Expression rhs = assignmentSimple.getRValue();
                LValueUsageCollectorSimple lValueUsageCollector = new LValueUsageCollectorSimple();
                rhs.collectUsedLValues(lValueUsageCollector);
                if (SSAIdentifierUtils.isMovableUnder(lValueUsageCollector.getUsedLValues(), lValue, start.getSSAIdentifiers(), current.getSSAIdentifiers())) {
                    return current;
                }
                return null;
            }
            if (current.getSources().size() == 1) continue;
            return null;
        } while ((current = current.getSources().get(0)) != null);
        return null;
    }

    private static Op03SimpleStatement getForInvariant(Op03SimpleStatement start, LValue invariant, BlockIdentifier whileLoop) {
        Op03SimpleStatement current = start;
        while (current.getBlockIdentifiers().contains(whileLoop)) {
            AbstractAssignment assignment;
            LValue assigned;
            if (current.getStatement() instanceof AbstractAssignment && invariant.equals(assigned = (assignment = (AbstractAssignment)current.getStatement()).getCreatedLValue()) && assignment.isSelfMutatingOperation()) {
                return current;
            }
            if (current.getSources().size() > 1) break;
            Op03SimpleStatement next = current.getSources().get(0);
            if (!current.getIndex().isBackJumpTo(next)) break;
            current = next;
        }
        throw new ConfusedCFRException("Shouldn't be able to get here.");
    }

    private static List<Op03SimpleStatement> getMutations(List<Op03SimpleStatement> backSources, LValue loopVariable, BlockIdentifier whileBlockIdentifier) {
        List<Op03SimpleStatement> mutations = ListFactory.newList();
        for (Op03SimpleStatement source : backSources) {
            Op03SimpleStatement incrStatement = WhileRewriter.getForInvariant(source, loopVariable, whileBlockIdentifier);
            mutations.add(incrStatement);
        }
        Op03SimpleStatement baseline = (Op03SimpleStatement)mutations.get(0);
        for (Op03SimpleStatement incrStatement : mutations) {
            if (baseline.equals(incrStatement)) continue;
            return null;
        }
        return mutations;
    }

    static void rewriteWhilesAsFors(Options options, List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> whileStarts = Functional.filter(statements, new Predicate<Op03SimpleStatement>(){

            @Override
            public boolean test(Op03SimpleStatement in) {
                return in.getStatement() instanceof WhileStatement && ((WhileStatement)in.getStatement()).getBlockIdentifier().getBlockType() == BlockType.WHILELOOP;
            }
        });
        boolean aggcapture = options.getOption(OptionsImpl.FOR_LOOP_CAPTURE) == Troolean.TRUE;
        for (Op03SimpleStatement whileStart : whileStarts) {
            WhileRewriter.rewriteWhileAsFor(whileStart, aggcapture);
        }
    }
}

