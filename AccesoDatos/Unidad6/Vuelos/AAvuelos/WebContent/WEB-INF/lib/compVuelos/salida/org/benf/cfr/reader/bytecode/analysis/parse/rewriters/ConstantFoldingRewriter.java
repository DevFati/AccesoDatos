/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.rewriters;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.util.collections.MapFactory;

public class ConstantFoldingRewriter
extends AbstractExpressionRewriter {
    public static final ConstantFoldingRewriter INSTANCE = new ConstantFoldingRewriter();
    private static final Map<LValue, Literal> DISPLAY_MAP = MapFactory.newMap();

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        JavaTypeInstance type = expression.getInferredJavaType().getJavaTypeInstance();
        if (type instanceof RawJavaType) {
            RawJavaType rawType = (RawJavaType)type;
            if (!rawType.isNumber()) {
                return expression;
            }
        } else {
            return expression;
        }
        Literal computed = expression.getComputedLiteral(this.getDisplayMap());
        if (computed != null) {
            expression = computed;
        }
        return expression;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        lValue.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return lValue;
    }

    private Map<LValue, Literal> getDisplayMap() {
        return DISPLAY_MAP;
    }
}

