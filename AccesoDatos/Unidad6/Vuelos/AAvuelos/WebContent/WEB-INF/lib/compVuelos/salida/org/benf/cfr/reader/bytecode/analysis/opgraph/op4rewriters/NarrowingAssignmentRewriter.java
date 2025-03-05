/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ExpressionReplacingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.Op04Rewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;

public class NarrowingAssignmentRewriter
implements Op04Rewriter {
    @Override
    public void rewrite(Op04StructuredStatement root) {
        List<StructuredStatement> statements = MiscStatementTools.linearise(root);
        if (statements == null) {
            return;
        }
        for (StructuredStatement s : statements) {
            CastExpression exp;
            Expression rhs;
            StructuredAssignment ass;
            LValue lValue;
            RawJavaType raw;
            if (!(s instanceof StructuredAssignment) || (raw = RawJavaType.getUnboxedTypeFor((lValue = (ass = (StructuredAssignment)s).getLvalue()).getInferredJavaType().getJavaTypeInstance())) == null || !((rhs = ass.getRvalue()) instanceof CastExpression) || !(exp = (CastExpression)rhs).isForced() || exp.getInferredJavaType().getRawType() != raw) continue;
            s.rewriteExpressions(new ExpressionReplacingRewriter(exp, exp.getChild()));
        }
    }
}

