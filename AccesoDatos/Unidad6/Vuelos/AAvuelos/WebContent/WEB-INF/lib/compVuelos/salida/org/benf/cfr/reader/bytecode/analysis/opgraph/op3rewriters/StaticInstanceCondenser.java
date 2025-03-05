/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

public class StaticInstanceCondenser {
    public static final StaticInstanceCondenser INSTANCE = new StaticInstanceCondenser();

    public void rewrite(List<Op03SimpleStatement> statements) {
        for (Op03SimpleStatement stm : statements) {
            if (!(stm.getStatement() instanceof ExpressionStatement)) continue;
            this.consider(stm);
        }
    }

    private void consider(Op03SimpleStatement stm) {
        ExpressionStatement es = (ExpressionStatement)stm.getStatement();
        Expression e = es.getExpression();
        if (!(e instanceof LValueExpression)) {
            return;
        }
        if (stm.getTargets().size() != 1) {
            return;
        }
        JavaTypeInstance typ = e.getInferredJavaType().getJavaTypeInstance();
        Op03SimpleStatement next = Misc.followNopGoto(stm.getTargets().get(0), true, false);
        Rewriter rewriter = new Rewriter(e, typ);
        next.rewrite(rewriter);
        if (rewriter.success) {
            stm.nopOut();
        }
    }

    private static class Rewriter
    extends AbstractExpressionRewriter {
        JavaTypeInstance typ;
        Expression object;
        boolean done = false;
        boolean success = false;

        Rewriter(Expression object, JavaTypeInstance typ) {
            this.object = object;
            this.typ = typ;
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (this.done) {
                return expression;
            }
            if (expression instanceof StaticFunctionInvokation) {
                StaticFunctionInvokation sfe = (StaticFunctionInvokation)expression;
                JavaTypeInstance staticType = sfe.getClazz();
                if (staticType.equals(this.typ)) {
                    sfe.forceObject(this.object);
                    this.success = true;
                }
                this.done = true;
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }
}

