/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.LinkedList;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;

public class LambdaCleaner
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

    private static LambdaExpression rebuildLambda(LambdaExpression e, Expression body) {
        return new LambdaExpression(BytecodeLoc.TODO, e.getInferredJavaType(), e.getArgs(), e.explicitArgTypes(), body);
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        StructuredStatementExpression structuredStatementExpression;
        StructuredStatement content;
        LambdaExpression lambda;
        Expression result;
        if (expression instanceof LambdaExpression && (result = (lambda = (LambdaExpression)expression).getResult()) instanceof StructuredStatementExpression && (content = (structuredStatementExpression = (StructuredStatementExpression)result).getContent()) instanceof Block) {
            Block block = (Block)content;
            Pair<Boolean, Op04StructuredStatement> singleStatement = block.getOneStatementIfPresent();
            if (singleStatement.getSecond() != null) {
                StructuredStatement statement = singleStatement.getSecond().getStatement();
                if (statement instanceof StructuredReturn) {
                    expression = LambdaCleaner.rebuildLambda(lambda, ((StructuredReturn)statement).getValue());
                } else if (statement instanceof StructuredExpressionStatement) {
                    expression = LambdaCleaner.rebuildLambda(lambda, ((StructuredExpressionStatement)statement).getExpression());
                }
            } else if (singleStatement.getFirst().booleanValue()) {
                StructuredStatementExpression empty = new StructuredStatementExpression(expression.getInferredJavaType(), new Block(new LinkedList<Op04StructuredStatement>(), true));
                expression = LambdaCleaner.rebuildLambda(lambda, empty);
            }
        }
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }
}

