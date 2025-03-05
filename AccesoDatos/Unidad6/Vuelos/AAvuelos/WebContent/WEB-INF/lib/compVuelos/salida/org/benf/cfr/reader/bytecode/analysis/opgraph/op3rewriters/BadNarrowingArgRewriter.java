/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;

public class BadNarrowingArgRewriter
extends AbstractExpressionRewriter {
    private final InternalBadNarrowingRewriter internalBadNarrowingRewriter = new InternalBadNarrowingRewriter();

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression instanceof AbstractFunctionInvokation) {
            AbstractFunctionInvokation functionInvokation = (AbstractFunctionInvokation)expression;
            functionInvokation.applyExpressionRewriterToArgs(this.internalBadNarrowingRewriter, ssaIdentifiers, statementContainer, flags);
        }
        return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
    }

    private class InternalBadNarrowingRewriter
    extends AbstractExpressionRewriter {
        private InternalBadNarrowingRewriter() {
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            Expression rwExpression = expression;
            if (expression instanceof CastExpression) {
                CastExpression castExpression = (CastExpression)expression;
                if (!castExpression.isForced()) {
                    rwExpression = this.rewriteLiteral(expression, castExpression.getChild(), castExpression.getInferredJavaType());
                }
            } else {
                rwExpression = this.rewriteLiteral(expression, expression, expression.getInferredJavaType());
            }
            return rwExpression;
        }

        private Expression rewriteLiteral(Expression original, Expression possibleLiteral, InferredJavaType tgtType) {
            Literal literal;
            TypedLiteral tl;
            if (possibleLiteral instanceof Literal && (tl = (literal = (Literal)possibleLiteral).getValue()).getType() == TypedLiteral.LiteralType.Integer) {
                switch (tgtType.getRawType()) {
                    case BYTE: 
                    case SHORT: {
                        return new CastExpression(BytecodeLoc.NONE, tgtType, possibleLiteral, true);
                    }
                }
            }
            return original;
        }
    }
}

