/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredContinue;

public class ControlFlowCleaningTransformer
implements StructuredStatementTransformer,
ExpressionRewriter {
    @Override
    public void handleStatement(StatementContainer statementContainer) {
    }

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        if (scope.get(0) instanceof Block) {
            if (in instanceof StructuredBreak) {
                StructuredBreak inb = (StructuredBreak)in;
                Set<Op04StructuredStatement> falls = scope.getNextFallThrough(in);
                for (Op04StructuredStatement fall : falls) {
                    StructuredBreak stmb;
                    StructuredStatement stm = fall.getStatement();
                    if (!(stm instanceof StructuredBreak) || (stmb = (StructuredBreak)stm).getBreakBlock() != inb.getBreakBlock()) continue;
                    return StructuredComment.EMPTY_COMMENT;
                }
                return in;
            }
            if (in instanceof StructuredContinue) {
                StructuredContinue cont = (StructuredContinue)in;
                Set<Op04StructuredStatement> falls = scope.getNextFallThrough(in);
                for (Op04StructuredStatement fall : falls) {
                    StructuredContinue stmb;
                    StructuredStatement stm = fall.getStatement();
                    if (stm instanceof StructuredContinue && (stmb = (StructuredContinue)stm).getContinueTgt() == cont.getContinueTgt()) {
                        return StructuredComment.EMPTY_COMMENT;
                    }
                    if (stm instanceof StructuredComment) continue;
                    return in;
                }
                BlockIdentifier block = scope.getContinueBlock();
                if (block == cont.getContinueTgt()) {
                    return StructuredComment.EMPTY_COMMENT;
                }
                return in;
            }
        }
        in.transformStructuredChildren(this, scope);
        in.rewriteExpressions(this);
        return in;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return (ConditionalExpression)res;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    @Override
    public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }
}

