/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.rewriters;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;

public interface ExpressionRewriter {
    public Expression rewriteExpression(Expression var1, SSAIdentifiers var2, StatementContainer var3, ExpressionRewriterFlags var4);

    public ConditionalExpression rewriteExpression(ConditionalExpression var1, SSAIdentifiers var2, StatementContainer var3, ExpressionRewriterFlags var4);

    public LValue rewriteExpression(LValue var1, SSAIdentifiers var2, StatementContainer var3, ExpressionRewriterFlags var4);

    public StackSSALabel rewriteExpression(StackSSALabel var1, SSAIdentifiers var2, StatementContainer var3, ExpressionRewriterFlags var4);

    public void handleStatement(StatementContainer var1);
}

