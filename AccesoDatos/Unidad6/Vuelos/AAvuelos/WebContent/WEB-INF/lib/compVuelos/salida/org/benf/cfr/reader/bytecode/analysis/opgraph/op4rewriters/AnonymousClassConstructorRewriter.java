/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;

public class AnonymousClassConstructorRewriter
extends AbstractExpressionRewriter {
    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ClassFile classFile;
        if ((expression = super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags)) instanceof ConstructorInvokationAnonymousInner && (classFile = ((ConstructorInvokationAnonymousInner)expression).getClassFile()) != null) {
            block0: for (Method constructor : classFile.getConstructors()) {
                Op04StructuredStatement analysis = constructor.getAnalysis();
                if (!(analysis.getStatement() instanceof Block)) continue;
                Block block = (Block)analysis.getStatement();
                List<Op04StructuredStatement> statements = block.getBlockStatements();
                for (Op04StructuredStatement stmCont : statements) {
                    Expression e;
                    StructuredStatement stm = stmCont.getStatement();
                    if (stm instanceof StructuredComment) continue;
                    if (!(stm instanceof StructuredExpressionStatement) || !((e = ((StructuredExpressionStatement)stm).getExpression()) instanceof SuperFunctionInvokation)) continue block0;
                    stmCont.nopOut();
                    continue block0;
                }
            }
        }
        return expression;
    }
}

