/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheckSimple;
import org.benf.cfr.reader.util.collections.Functional;

public class PointlessExpressions {
    static void removePointlessExpressionStatements(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> exrps = Functional.filter(statements, new TypeFilter<ExpressionStatement>(ExpressionStatement.class));
        for (Op03SimpleStatement esc : exrps) {
            ExpressionStatement es = (ExpressionStatement)esc.getStatement();
            Expression expression = es.getExpression();
            if (!PointlessExpressions.isSafeToIgnore(expression)) continue;
            esc.nopOut();
        }
        List<Op03SimpleStatement> sas = Functional.filter(statements, new TypeFilter<AssignmentSimple>(AssignmentSimple.class));
        for (Op03SimpleStatement ass : sas) {
            LValueExpression lValueExpression;
            LValue lFromR;
            Expression rValue;
            AssignmentSimple assignmentSimple = (AssignmentSimple)ass.getStatement();
            LValue lValue = assignmentSimple.getCreatedLValue();
            if (lValue instanceof FieldVariable || (rValue = assignmentSimple.getRValue()).getClass() != LValueExpression.class || !(lFromR = (lValueExpression = (LValueExpression)rValue).getLValue()).equals(lValue)) continue;
            ass.nopOut();
        }
    }

    public static boolean isSafeToIgnore(Expression expression) {
        return expression instanceof LValueExpression && !expression.canThrow(ExceptionCheckSimple.INSTANCE) || expression instanceof StackValue || expression instanceof Literal;
    }
}

