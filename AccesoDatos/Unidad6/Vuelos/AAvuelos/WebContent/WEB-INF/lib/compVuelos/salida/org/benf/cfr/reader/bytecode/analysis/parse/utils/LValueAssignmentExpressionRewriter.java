/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;

public class LValueAssignmentExpressionRewriter
extends AbstractExpressionRewriter {
    private final LValue lValue;
    private final AbstractAssignmentExpression lValueReplacement;
    private final Op03SimpleStatement source;
    private boolean terminated = false;

    public LValueAssignmentExpressionRewriter(LValue lValue, AbstractAssignmentExpression lValueReplacement, Op03SimpleStatement source) {
        this.lValue = lValue;
        this.lValueReplacement = lValueReplacement;
        this.source = source;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (this.terminated) {
            return expression;
        }
        if (expression instanceof BooleanOperation) {
            return ((BooleanOperation)expression).applyLHSOnlyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        }
        if (expression instanceof TernaryExpression) {
            return ((TernaryExpression)expression).applyConditionOnlyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        }
        if (expression instanceof LValueExpression) {
            LValue lValue = ((LValueExpression)expression).getLValue();
            if (!lValue.equals(this.lValue)) {
                return expression;
            }
            if (!ssaIdentifiers.isValidReplacement(lValue, statementContainer.getSSAIdentifiers())) {
                return expression;
            }
            SSAIdent ssaIdentOnEntry = this.source.getSSAIdentifiers().getSSAIdentOnEntry(lValue);
            statementContainer.getSSAIdentifiers().setKnownIdentifierOnEntry(lValue, ssaIdentOnEntry);
            this.source.nopOut();
            this.terminated = true;
            return this.lValueReplacement;
        }
        Expression res = super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        if (expression instanceof AbstractFunctionInvokation) {
            this.terminated = true;
        }
        return res;
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (this.terminated) {
            return expression;
        }
        return (ConditionalExpression)this.rewriteExpression((Expression)expression, ssaIdentifiers, statementContainer, flags);
    }
}

