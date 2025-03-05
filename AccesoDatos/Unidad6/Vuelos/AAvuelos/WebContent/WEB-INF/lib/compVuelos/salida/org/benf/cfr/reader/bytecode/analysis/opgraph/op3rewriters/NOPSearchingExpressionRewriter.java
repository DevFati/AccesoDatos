/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;

public class NOPSearchingExpressionRewriter
extends AbstractExpressionRewriter {
    private final Expression needle;
    private final Set<Expression> poison;
    private boolean found = false;
    private boolean poisoned = false;

    public NOPSearchingExpressionRewriter(Expression needle, Set<Expression> poison) {
        this.needle = needle;
        this.poison = poison;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (!this.found && this.needle.equals(expression)) {
            this.found = true;
            return expression;
        }
        if (this.poison.contains(expression)) {
            this.poisoned = true;
        }
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    public boolean isFound() {
        return this.found && !this.poisoned;
    }
}

