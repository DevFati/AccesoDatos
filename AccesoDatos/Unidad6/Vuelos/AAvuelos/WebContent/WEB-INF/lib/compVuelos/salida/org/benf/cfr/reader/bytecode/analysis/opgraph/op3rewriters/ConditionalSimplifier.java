/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ConditionalSimplifyingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnValueStatement;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.Troolean;

class ConditionalSimplifier {
    ConditionalSimplifier() {
    }

    static void simplifyConditionals(List<Op03SimpleStatement> statements, boolean aggressive, Method method) {
        boolean boolReturn = method.getMethodPrototype().getReturnType() == RawJavaType.BOOLEAN;
        for (Op03SimpleStatement statement : statements) {
            if (!(statement.getStatement() instanceof IfStatement)) continue;
            IfStatement ifStatement = (IfStatement)statement.getStatement();
            ifStatement.simplifyCondition();
            if (!boolReturn) continue;
            ConditionalSimplifier.replaceEclipseReturn(statement, ifStatement);
        }
        if (aggressive) {
            ConditionalSimplifyingRewriter conditionalSimplifier = new ConditionalSimplifyingRewriter();
            for (Op03SimpleStatement statement : statements) {
                statement.rewrite(conditionalSimplifier);
            }
        }
    }

    private static void replaceEclipseReturn(Op03SimpleStatement statement, IfStatement ifStatement) {
        List<Op03SimpleStatement> targets = statement.getTargets();
        if (targets.size() != 2) {
            return;
        }
        Op03SimpleStatement tgt2 = targets.get(0);
        Op03SimpleStatement tgt1 = targets.get(1);
        if (tgt1.getSources().size() != 1 || tgt2.getSources().size() != 1) {
            return;
        }
        Troolean t1 = ConditionalSimplifier.isBooleanReturn(tgt1.getStatement());
        Troolean t2 = ConditionalSimplifier.isBooleanReturn(tgt2.getStatement());
        if (t1 == Troolean.NEITHER || t2 == Troolean.NEITHER || t1 == t2) {
            return;
        }
        boolean b2 = t2.boolValue(false);
        ConditionalExpression c = ifStatement.getCondition();
        if (b2) {
            c = c.getNegated().simplify();
        }
        ReturnValueStatement ret = new ReturnValueStatement(BytecodeLoc.TODO, c, RawJavaType.BOOLEAN);
        statement.replaceStatement(ret);
        tgt1.nopOut();
        tgt2.nopOut();
    }

    private static Troolean isBooleanReturn(Statement s) {
        if (!(s instanceof ReturnValueStatement)) {
            return Troolean.NEITHER;
        }
        Expression e = ((ReturnValueStatement)s).getReturnValue();
        if (Literal.TRUE.equals(e)) {
            return Troolean.TRUE;
        }
        if (Literal.FALSE.equals(e)) {
            return Troolean.FALSE;
        }
        return Troolean.NEITHER;
    }
}

