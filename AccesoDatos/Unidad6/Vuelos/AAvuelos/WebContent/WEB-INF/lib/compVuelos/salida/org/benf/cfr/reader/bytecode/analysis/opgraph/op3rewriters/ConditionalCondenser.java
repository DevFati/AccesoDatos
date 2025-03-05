/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractAssignment;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class ConditionalCondenser {
    private boolean testEclipse;
    private boolean notInstanceOf;

    private ConditionalCondenser(boolean testEclipse, boolean notInstanceOf) {
        this.testEclipse = testEclipse;
        this.notInstanceOf = notInstanceOf;
    }

    private static boolean appropriateForIfAssignmentCollapse1(Op03SimpleStatement statement) {
        Op03SimpleStatement source;
        boolean extraCondSeen = false;
        boolean preCondAssignmentSeen = false;
        while (statement.getSources().size() == 1 && (source = statement.getSources().get(0)) != statement && !statement.getIndex().isBackJumpFrom(source)) {
            Statement contained = source.getStatement();
            if (contained instanceof AbstractAssignment) {
                preCondAssignmentSeen |= !extraCondSeen;
            } else {
                if (!(contained instanceof IfStatement)) break;
                extraCondSeen = true;
            }
            statement = source;
        }
        if (!preCondAssignmentSeen) {
            return false;
        }
        if (extraCondSeen) {
            return false;
        }
        InstrIndex statementIndex = statement.getIndex();
        for (Op03SimpleStatement source2 : statement.getSources()) {
            if (!statementIndex.isBackJumpFrom(source2)) continue;
            return true;
        }
        return false;
    }

    private static boolean appropriateForIfAssignmentCollapse2(Op03SimpleStatement statement) {
        Op03SimpleStatement source;
        boolean preCondAssignmentSeen = false;
        while (statement.getSources().size() == 1 && (source = statement.getSources().get(0)).getTargets().size() == 1) {
            Statement contained = source.getStatement();
            if (contained instanceof AbstractAssignment) {
                preCondAssignmentSeen = true;
            }
            statement = source;
        }
        return preCondAssignmentSeen;
    }

    private void collapseAssignmentsIntoConditional(Op03SimpleStatement ifStatement) {
        ConditionalExpression conditionalExpression;
        IfStatement innerIf;
        block15: {
            boolean eclipseHeuristic;
            if (!ConditionalCondenser.appropriateForIfAssignmentCollapse1(ifStatement) && !ConditionalCondenser.appropriateForIfAssignmentCollapse2(ifStatement)) {
                return;
            }
            innerIf = (IfStatement)ifStatement.getStatement();
            conditionalExpression = innerIf.getCondition();
            boolean bl = eclipseHeuristic = this.testEclipse && ifStatement.getTargets().get(1).getIndex().isBackJumpFrom(ifStatement);
            if (!eclipseHeuristic) {
                Statement opStatement;
                Op03SimpleStatement statement = ifStatement;
                Set visited = SetFactory.newSet();
                do {
                    if (statement.getSources().size() > 1) {
                        InstrIndex statementIndex = statement.getIndex();
                        for (Op03SimpleStatement source : statement.getSources()) {
                            if (!statementIndex.isBackJumpFrom(source)) continue;
                            break block15;
                        }
                    }
                    if (statement.getSources().isEmpty()) break block15;
                    if (!visited.add(statement = statement.getSources().get(0))) {
                        return;
                    }
                    opStatement = statement.getStatement();
                    if (opStatement instanceof IfStatement) break block15;
                } while (opStatement instanceof Nop || opStatement instanceof AbstractAssignment);
                return;
            }
        }
        Op03SimpleStatement previousSource = null;
        while (ifStatement.getSources().size() == 1) {
            Op03SimpleStatement source = ifStatement.getSources().get(0);
            if (source == previousSource) {
                return;
            }
            previousSource = source;
            if (!(source.getStatement() instanceof AbstractAssignment)) {
                return;
            }
            LValue lValue = source.getStatement().getCreatedLValue();
            if (lValue instanceof StackSSALabel) {
                return;
            }
            LValueUsageCollectorSimple lvc = new LValueUsageCollectorSimple();
            conditionalExpression.collectUsedLValues(lvc);
            if (!lvc.isUsed(lValue)) {
                return;
            }
            AbstractAssignment assignment = (AbstractAssignment)source.getStatement();
            AbstractAssignmentExpression assignmentExpression = assignment.getInliningExpression();
            LValueUsageCollectorSimple assignmentLVC = new LValueUsageCollectorSimple();
            assignmentExpression.collectUsedLValues(assignmentLVC);
            Set<LValue> used = SetFactory.newSet(assignmentLVC.getUsedLValues());
            used.remove(lValue);
            Set<LValue> usedComparison = SetFactory.newSet(lvc.getUsedLValues());
            SSAIdentifiers<LValue> beforeSSA = source.getSSAIdentifiers();
            SSAIdentifiers<LValue> afterSSA = ifStatement.getSSAIdentifiers();
            Set<LValue> intersection = SetUtil.intersectionOrNull(used, usedComparison);
            if (intersection != null) {
                for (LValue intersect : intersection) {
                    if (afterSSA.isValidReplacement(intersect, beforeSSA)) continue;
                    return;
                }
            }
            if (!afterSSA.isValidReplacement(lValue, beforeSSA)) {
                return;
            }
            LValueAssignmentExpressionRewriter rewriter = new LValueAssignmentExpressionRewriter(lValue, assignmentExpression, source);
            ConditionalExpression replacement = rewriter.rewriteExpression(conditionalExpression, ifStatement.getSSAIdentifiers(), (StatementContainer)ifStatement, ExpressionRewriterFlags.LVALUE);
            if (replacement == null) {
                return;
            }
            innerIf.setCondition(replacement);
        }
    }

    static void collapseAssignmentsIntoConditionals(List<Op03SimpleStatement> statements, Options options, ClassFileVersion classFileVersion) {
        List<Op03SimpleStatement> ifStatements = Functional.filter(statements, new TypeFilter<IfStatement>(IfStatement.class));
        if (ifStatements.isEmpty()) {
            return;
        }
        boolean testEclipse = (Boolean)options.getOption(OptionsImpl.ECLIPSE);
        boolean notBeforeInstanceOf = options.getOption(OptionsImpl.INSTANCEOF_PATTERN, classFileVersion);
        ConditionalCondenser c = new ConditionalCondenser(testEclipse, notBeforeInstanceOf);
        for (Op03SimpleStatement statement : ifStatements) {
            c.collapseAssignmentsIntoConditional(statement);
        }
    }
}

