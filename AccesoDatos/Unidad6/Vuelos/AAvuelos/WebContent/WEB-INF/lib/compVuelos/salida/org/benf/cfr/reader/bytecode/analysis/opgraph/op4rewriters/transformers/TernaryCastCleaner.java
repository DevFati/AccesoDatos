/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.BoxingHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;

public class TernaryCastCleaner
extends AbstractExpressionRewriter
implements StructuredStatementTransformer {
    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        in.rewriteExpressions(this);
        return in;
    }

    private static Expression applyTransforms(TernaryExpression t) {
        InferredJavaType inferredJavaType = t.getInferredJavaType();
        ConditionalExpression condition = t.getCondition();
        Expression lhs = t.getLhs();
        Expression rhs = t.getRhs();
        if (inferredJavaType.getJavaTypeInstance().getStackType() != StackType.REF) {
            if (condition instanceof BooleanExpression && ((BooleanExpression)condition).getInner().getInferredJavaType().getJavaTypeInstance() != RawJavaType.BOOLEAN && lhs == Literal.INT_ONE && rhs == Literal.INT_ZERO) {
                BooleanExpression b = (BooleanExpression)condition;
                return b.getInner();
            }
            if (lhs instanceof Literal) {
                lhs = ((Literal)lhs).appropriatelyCasted(inferredJavaType);
                return new TernaryExpression(BytecodeLoc.TODO, inferredJavaType, condition, lhs, rhs);
            }
            if (rhs instanceof Literal) {
                rhs = ((Literal)rhs).appropriatelyCasted(inferredJavaType);
                return new TernaryExpression(BytecodeLoc.TODO, inferredJavaType, condition, lhs, rhs);
            }
            return t;
        }
        if (BoxingHelper.isBoxedTypeInclNumber(lhs.getInferredJavaType().getJavaTypeInstance()) && BoxingHelper.isBoxedTypeInclNumber(rhs.getInferredJavaType().getJavaTypeInstance()) && !BoxingHelper.isBoxedType(t.getInferredJavaType().getJavaTypeInstance())) {
            InferredJavaType typ = t.getInferredJavaType();
            return new TernaryExpression(BytecodeLoc.TODO, t.getInferredJavaType(), condition, new CastExpression(BytecodeLoc.NONE, typ, lhs), new CastExpression(BytecodeLoc.NONE, typ, rhs));
        }
        return t;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if ((expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags)) instanceof TernaryExpression) {
            expression = TernaryCastCleaner.applyTransforms((TernaryExpression)expression);
        }
        return expression;
    }
}

