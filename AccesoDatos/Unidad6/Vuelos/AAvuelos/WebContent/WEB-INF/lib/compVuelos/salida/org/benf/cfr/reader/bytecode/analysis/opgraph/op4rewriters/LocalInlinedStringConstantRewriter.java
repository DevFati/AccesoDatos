/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.Op04Rewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;

public class LocalInlinedStringConstantRewriter
extends AbstractExpressionRewriter
implements Op04Rewriter {
    private final Map<String, Expression> rewrites;

    public LocalInlinedStringConstantRewriter(Map<String, Expression> rewrites) {
        this.rewrites = rewrites;
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) {
            return;
        }
        for (StructuredStatement statement : structuredStatements) {
            statement.rewriteExpressions(this);
        }
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        String str;
        Expression replacement;
        Literal exp;
        Object val;
        if (expression instanceof Literal && expression.getInferredJavaType().getJavaTypeInstance() == TypeConstants.STRING && (val = (exp = (Literal)expression).getValue().getValue()) instanceof String && (replacement = this.rewrites.get(str = (String)val)) != null) {
            return replacement;
        }
        expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return expression;
    }
}

