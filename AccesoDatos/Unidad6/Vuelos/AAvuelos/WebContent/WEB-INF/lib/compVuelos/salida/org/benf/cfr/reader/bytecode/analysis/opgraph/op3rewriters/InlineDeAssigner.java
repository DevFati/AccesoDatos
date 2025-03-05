/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.WhileStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class InlineDeAssigner {
    private InlineDeAssigner() {
    }

    private static void rewrite(Deassigner deassigner, Op03SimpleStatement container, List<Op03SimpleStatement> added) {
        List<AssignmentExpression> assignmentExpressions = deassigner.extracted;
        if (assignmentExpressions.isEmpty()) {
            return;
        }
        Collections.reverse(assignmentExpressions);
        InstrIndex index = container.getIndex();
        Op03SimpleStatement last = container;
        List<Op03SimpleStatement> sources = ListFactory.newList(container.getSources());
        container.getSources().clear();
        for (AssignmentExpression expression : assignmentExpressions) {
            index = index.justBefore();
            AssignmentSimple assignmentSimple = new AssignmentSimple(BytecodeLoc.TODO, expression.getlValue(), expression.getrValue());
            Op03SimpleStatement newAssign = new Op03SimpleStatement(container.getBlockIdentifiers(), assignmentSimple, index);
            added.add(newAssign);
            newAssign.addTarget(last);
            last.addSource(newAssign);
            last = newAssign;
        }
        for (Op03SimpleStatement source : sources) {
            source.replaceTarget(container, last);
            last.addSource(source);
        }
    }

    private void deAssign(AssignmentSimple assignmentSimple, Op03SimpleStatement container, List<Op03SimpleStatement> added) {
        Expression rhs = assignmentSimple.getRValue();
        if (rhs instanceof LValueExpression || rhs instanceof Literal) {
            return;
        }
        Deassigner deassigner = new Deassigner();
        LinkedList<LValue> lValues = ListFactory.newLinkedList();
        while (rhs instanceof AssignmentExpression) {
            AssignmentExpression assignmentExpression = (AssignmentExpression)rhs;
            lValues.addFirst(assignmentExpression.getlValue());
            rhs = assignmentExpression.getrValue();
        }
        Expression rhs2 = deassigner.rewriteExpression(rhs, container.getSSAIdentifiers(), (StatementContainer)container, ExpressionRewriterFlags.RVALUE);
        if (deassigner.extracted.isEmpty()) {
            return;
        }
        for (LValue outer : lValues) {
            rhs2 = new AssignmentExpression(BytecodeLoc.TODO, outer, rhs2);
        }
        assignmentSimple.setRValue(rhs2);
        InlineDeAssigner.rewrite(deassigner, container, added);
    }

    private void deAssign(Op03SimpleStatement container, List<Op03SimpleStatement> added) {
        Deassigner deassigner = new Deassigner();
        container.rewrite(deassigner);
        InlineDeAssigner.rewrite(deassigner, container, added);
    }

    public static void extractAssignments(List<Op03SimpleStatement> statements) {
        InlineDeAssigner inlineDeAssigner = new InlineDeAssigner();
        List<Op03SimpleStatement> newStatements = ListFactory.newList();
        for (Op03SimpleStatement statement : statements) {
            if (statement.getSources().size() != 1) continue;
            Statement stmt = statement.getStatement();
            Class<?> clazz = stmt.getClass();
            if (clazz == AssignmentSimple.class) {
                inlineDeAssigner.deAssign((AssignmentSimple)stmt, statement, newStatements);
                continue;
            }
            if (clazz == WhileStatement.class) {
                MiscUtils.handyBreakPoint();
                continue;
            }
            inlineDeAssigner.deAssign(statement, newStatements);
        }
        if (newStatements.isEmpty()) {
            return;
        }
        statements.addAll(newStatements);
        Cleaner.sortAndRenumberInPlace(statements);
    }

    private class Deassigner
    extends AbstractExpressionRewriter {
        Set<LValue> read = SetFactory.newSet();
        Set<LValue> write = SetFactory.newSet();
        List<AssignmentExpression> extracted = ListFactory.newList();
        boolean noFurther = false;

        private Deassigner() {
        }

        private Expression tryExtractAssignment(AssignmentExpression assignmentExpression) {
            LValue lValue = assignmentExpression.getlValue();
            if (this.read.contains(lValue) || this.write.contains(lValue)) {
                return assignmentExpression;
            }
            LValueUsageCollectorSimple lValueUsageCollectorSimple = new LValueUsageCollectorSimple();
            assignmentExpression.getrValue().collectUsedLValues(lValueUsageCollectorSimple);
            for (LValue lValue1 : lValueUsageCollectorSimple.getUsedLValues()) {
                if (!this.write.contains(lValue1)) continue;
                return assignmentExpression;
            }
            this.extracted.add(assignmentExpression);
            return new LValueExpression(lValue);
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (this.noFurther) {
                return expression;
            }
            if (expression instanceof ConditionalExpression) {
                return this.rewriteExpression((ConditionalExpression)expression, ssaIdentifiers, statementContainer, flags);
            }
            if (expression instanceof AssignmentExpression) {
                AssignmentExpression assignmentExpression = (AssignmentExpression)expression;
                assignmentExpression.applyRValueOnlyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
                return this.tryExtractAssignment((AssignmentExpression)expression);
            }
            if (expression instanceof TernaryExpression) {
                TernaryExpression ternaryExpression = (TernaryExpression)expression;
                expression = ternaryExpression.applyConditionOnlyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
                this.noFurther = true;
                return expression;
            }
            Expression result = super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
            if (expression instanceof AbstractFunctionInvokation) {
                this.noFurther = true;
            }
            return result;
        }

        @Override
        public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (this.noFurther) {
                return expression;
            }
            if (expression instanceof BooleanOperation) {
                BooleanOperation booleanOperation = (BooleanOperation)expression;
                ConditionalExpression lhs = booleanOperation.getLhs();
                ConditionalExpression lhs2 = this.rewriteExpression(lhs, ssaIdentifiers, statementContainer, flags);
                if (lhs2 != lhs) {
                    return new BooleanOperation(BytecodeLoc.TODO, lhs2, booleanOperation.getRhs(), booleanOperation.getOp());
                }
                this.noFurther = true;
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            switch (flags) {
                case LVALUE: {
                    this.write.add(lValue);
                    break;
                }
                case RVALUE: {
                    this.read.add(lValue);
                    break;
                }
                case LANDRVALUE: {
                    this.write.add(lValue);
                    this.read.add(lValue);
                }
            }
            return lValue;
        }

        @Override
        public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            return (StackSSALabel)this.rewriteExpression((LValue)lValue, ssaIdentifiers, statementContainer, flags);
        }
    }
}

