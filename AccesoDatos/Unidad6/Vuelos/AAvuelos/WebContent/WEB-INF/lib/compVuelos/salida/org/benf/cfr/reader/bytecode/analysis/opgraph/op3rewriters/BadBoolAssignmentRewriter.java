/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;

public class BadBoolAssignmentRewriter
extends AbstractExpressionRewriter {
    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression instanceof ArithmeticOperation) {
            ArithmeticOperation op = (ArithmeticOperation)expression;
            JavaTypeInstance resType = op.getInferredJavaType().getJavaTypeInstance();
            RawJavaType rawRes = resType.getRawTypeOfSimpleType();
            if (resType.getStackType() == StackType.INT && resType != RawJavaType.BOOLEAN) {
                InferredJavaType l = op.getLhs().getInferredJavaType();
                InferredJavaType r = op.getRhs().getInferredJavaType();
                if (l.getJavaTypeInstance() == RawJavaType.BOOLEAN) {
                    l.useInArithOp(r, rawRes, true);
                }
                if (r.getJavaTypeInstance() == RawJavaType.BOOLEAN) {
                    r.useInArithOp(l, rawRes, true);
                }
            }
        }
        return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
    }
}

