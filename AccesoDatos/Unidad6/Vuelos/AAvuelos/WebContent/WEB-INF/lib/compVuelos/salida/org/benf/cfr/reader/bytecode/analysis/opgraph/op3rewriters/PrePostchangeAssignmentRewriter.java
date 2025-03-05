/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMutatingAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticPostMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentPreMutation;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.functors.Predicate;

class PrePostchangeAssignmentRewriter {
    PrePostchangeAssignmentRewriter() {
    }

    private static boolean pushPreChange(Op03SimpleStatement preChange, boolean back) {
        AssignmentPreMutation mutation = (AssignmentPreMutation)preChange.getStatement();
        Op03SimpleStatement current = preChange;
        LValue mutatedLValue = mutation.getCreatedLValue();
        LValueExpression lvalueExpression = new LValueExpression(mutatedLValue);
        UsageWatcher usageWatcher = new UsageWatcher(mutatedLValue);
        Op03SimpleStatement lastCurr = null;
        do {
            AssignmentSimple assignmentSimple;
            List<Op03SimpleStatement> candidates;
            if (lastCurr == current) {
                return false;
            }
            List<Op03SimpleStatement> list = candidates = back ? current.getSources() : current.getTargets();
            if (candidates.size() != 1) {
                return false;
            }
            lastCurr = current;
            current = candidates.get(0);
            Statement innerStatement = current.getStatement();
            if (innerStatement instanceof AssignmentSimple && (assignmentSimple = (AssignmentSimple)innerStatement).getRValue().equals(lvalueExpression)) {
                SSAIdentifiers<LValue> assignIdents;
                LValue tgt = assignmentSimple.getCreatedLValue();
                tgt.applyExpressionRewriter(usageWatcher, null, current, ExpressionRewriterFlags.LVALUE);
                if (usageWatcher.isFound()) {
                    return false;
                }
                SSAIdentifiers<LValue> preChangeIdents = preChange.getSSAIdentifiers();
                if (!preChangeIdents.isValidReplacement(tgt, assignIdents = current.getSSAIdentifiers())) {
                    return false;
                }
                if (back) {
                    assignIdents.setKnownIdentifierOnExit(mutatedLValue, preChangeIdents.getSSAIdentOnExit(mutatedLValue));
                } else {
                    assignIdents.setKnownIdentifierOnEntry(mutatedLValue, preChangeIdents.getSSAIdentOnEntry(mutatedLValue));
                }
                current.replaceStatement(new AssignmentSimple(innerStatement.getLoc(), tgt, back ? mutation.getPostMutation() : mutation.getPreMutation()));
                preChange.nopOut();
                return true;
            }
            current.rewrite(usageWatcher);
        } while (!usageWatcher.isFound());
        return false;
    }

    static void pushPreChangeBack(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> assignments = Functional.filter(statements, new TypeFilter<AssignmentPreMutation>(AssignmentPreMutation.class));
        if ((assignments = Functional.filter(assignments, new StatementCanBePostMutation())).isEmpty()) {
            return;
        }
        for (Op03SimpleStatement assignment : assignments) {
            if (PrePostchangeAssignmentRewriter.pushPreChange(assignment, true)) continue;
            PrePostchangeAssignmentRewriter.pushPreChange(assignment, false);
        }
    }

    private static boolean replacePreChangeAssignment(Op03SimpleStatement statement) {
        AssignmentSimple assignmentSimple = (AssignmentSimple)statement.getStatement();
        LValue lValue = assignmentSimple.getCreatedLValue();
        Expression rValue = assignmentSimple.getRValue();
        if (!(rValue instanceof ArithmeticOperation)) {
            return false;
        }
        ArithmeticOperation arithmeticOperation = (ArithmeticOperation)rValue;
        if (!arithmeticOperation.isMutationOf(lValue)) {
            return false;
        }
        AbstractMutatingAssignmentExpression mutationOperation = arithmeticOperation.getMutationOf(lValue);
        AssignmentPreMutation res = new AssignmentPreMutation(assignmentSimple.getLoc(), lValue, mutationOperation);
        statement.replaceStatement(res);
        return true;
    }

    private static void replacePostChangeAssignment(Op03SimpleStatement statement) {
        AssignmentSimple assignmentSimple = (AssignmentSimple)statement.getStatement();
        LValue postIncLValue = assignmentSimple.getCreatedLValue();
        if (statement.getSources().size() != 1) {
            return;
        }
        Op03SimpleStatement prior = statement.getSources().get(0);
        Statement statementPrior = prior.getStatement();
        if (!(statementPrior instanceof AssignmentSimple)) {
            return;
        }
        AssignmentSimple assignmentSimplePrior = (AssignmentSimple)statementPrior;
        LValue tmp = assignmentSimplePrior.getCreatedLValue();
        if (!(tmp instanceof StackSSALabel)) {
            return;
        }
        if (!assignmentSimplePrior.getRValue().equals(new LValueExpression(postIncLValue))) {
            return;
        }
        StackSSALabel tmpStackVar = (StackSSALabel)tmp;
        StackValue stackValue = new StackValue(assignmentSimplePrior.getLoc(), tmpStackVar);
        Expression incrRValue = assignmentSimple.getRValue();
        if (!(incrRValue instanceof ArithmeticOperation)) {
            return;
        }
        ArithmeticOperation arithOp = (ArithmeticOperation)incrRValue;
        ArithOp op = arithOp.getOp();
        if (!op.equals((Object)ArithOp.PLUS) && !op.equals((Object)ArithOp.MINUS)) {
            return;
        }
        Expression lhs = arithOp.getLhs();
        Expression rhs = arithOp.getRhs();
        if (((Object)stackValue).equals(lhs)) {
            if (!Literal.equalsAnyOne(rhs)) {
                return;
            }
        } else if (((Object)stackValue).equals(rhs)) {
            if (!Literal.equalsAnyOne(lhs)) {
                return;
            }
            if (op.equals((Object)ArithOp.MINUS)) {
                return;
            }
        } else {
            return;
        }
        ArithmeticPostMutationOperation postMutationOperation = new ArithmeticPostMutationOperation(assignmentSimple.getLoc(), postIncLValue, op);
        prior.nopOut();
        statement.replaceStatement(new AssignmentSimple(assignmentSimple.getLoc(), tmp, postMutationOperation));
    }

    static void replacePrePostChangeAssignments(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> assignments = Functional.filter(statements, new TypeFilter<AssignmentSimple>(AssignmentSimple.class));
        for (Op03SimpleStatement assignment : assignments) {
            if (PrePostchangeAssignmentRewriter.replacePreChangeAssignment(assignment)) continue;
            PrePostchangeAssignmentRewriter.replacePostChangeAssignment(assignment);
        }
    }

    private static class StatementCanBePostMutation
    implements Predicate<Op03SimpleStatement> {
        private StatementCanBePostMutation() {
        }

        @Override
        public boolean test(Op03SimpleStatement in) {
            LValue lValue;
            AssignmentPreMutation assignmentPreMutation = (AssignmentPreMutation)in.getStatement();
            return assignmentPreMutation.isSelfMutatingOp1(lValue = assignmentPreMutation.getCreatedLValue(), ArithOp.PLUS) || assignmentPreMutation.isSelfMutatingOp1(lValue, ArithOp.MINUS);
        }
    }

    private static class UsageWatcher
    extends AbstractExpressionRewriter {
        private final LValue needle;
        boolean found = false;

        private UsageWatcher(LValue needle) {
            this.needle = needle;
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (this.needle.equals(lValue)) {
                this.found = true;
            }
            return super.rewriteExpression(lValue, ssaIdentifiers, statementContainer, flags);
        }

        public boolean isFound() {
            return this.found;
        }
    }
}

