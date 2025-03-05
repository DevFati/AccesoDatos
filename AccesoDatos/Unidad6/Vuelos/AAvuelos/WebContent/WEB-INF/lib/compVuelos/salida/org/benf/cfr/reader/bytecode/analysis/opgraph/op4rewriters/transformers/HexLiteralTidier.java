/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LiteralHex;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;

public class HexLiteralTidier
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

    private static boolean bitOp(ArithOp op) {
        switch (op) {
            case AND: 
            case OR: 
            case XOR: {
                return true;
            }
        }
        return false;
    }

    private static Expression applyTransforms(ArithmeticOperation t) {
        if (!HexLiteralTidier.bitOp(t.getOp())) {
            return t;
        }
        Expression l = HexLiteralTidier.convertLiteral(t.getLhs());
        Expression r = HexLiteralTidier.convertLiteral(t.getRhs());
        if (l == null && r == null) {
            return t;
        }
        return new ArithmeticOperation(BytecodeLoc.TODO, l == null ? t.getLhs() : l, r == null ? t.getRhs() : r, t.getOp());
    }

    private static Expression applyTransforms(ArithmeticMutationOperation t) {
        if (!HexLiteralTidier.bitOp(t.getOp())) {
            return t;
        }
        Expression r = HexLiteralTidier.convertLiteral(t.getMutation());
        if (r == null) {
            return t;
        }
        return new ArithmeticMutationOperation(BytecodeLoc.TODO, t.getUpdatedLValue(), r, t.getOp());
    }

    private static Expression convertLiteral(Expression e) {
        if (e instanceof Literal) {
            Literal l = (Literal)e;
            TypedLiteral tl = l.getValue();
            switch (tl.getType()) {
                case Long: {
                    long lv = tl.getLongValue();
                    if (lv < 0L || lv >= 10L) break;
                    return null;
                }
                case Integer: {
                    int iv = tl.getIntValue();
                    if (iv < 0 || iv >= 10) break;
                    return null;
                }
                default: {
                    return null;
                }
            }
            return new LiteralHex(tl);
        }
        return null;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if ((expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags)) instanceof ArithmeticOperation) {
            expression = HexLiteralTidier.applyTransforms((ArithmeticOperation)expression);
        } else if (expression instanceof ArithmeticMutationOperation) {
            expression = HexLiteralTidier.applyTransforms((ArithmeticMutationOperation)expression);
        }
        return expression;
    }
}

