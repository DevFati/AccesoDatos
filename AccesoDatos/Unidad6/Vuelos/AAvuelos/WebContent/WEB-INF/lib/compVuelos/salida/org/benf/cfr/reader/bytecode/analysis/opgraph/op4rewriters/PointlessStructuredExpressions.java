/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.PointlessExpressions;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;

public class PointlessStructuredExpressions {
    public static void removePointlessExpression(StructuredStatement stm) {
        Expression e;
        StructuredAssignment ass;
        LValue lv;
        if (stm instanceof StructuredAssignment && (lv = (ass = (StructuredAssignment)stm).getLvalue()).isFakeIgnored() && PointlessExpressions.isSafeToIgnore(e = ass.getRvalue())) {
            stm.getContainer().nopOut();
        }
    }
}

