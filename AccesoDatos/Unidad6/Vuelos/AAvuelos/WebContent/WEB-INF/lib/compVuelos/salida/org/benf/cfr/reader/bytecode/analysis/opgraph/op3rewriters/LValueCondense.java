/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMutatingAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;

public class LValueCondense {
    public static void condenseLValueChain1(List<Op03SimpleStatement> statements) {
        for (Op03SimpleStatement statement : statements) {
            Statement stm2;
            Op03SimpleStatement statement2;
            Statement stm = statement.getStatement();
            if (!(stm instanceof AssignmentSimple) || statement.getTargets().size() != 1 || (statement2 = statement.getTargets().get(0)).getSources().size() != 1 || !((stm2 = statement2.getStatement()) instanceof AssignmentSimple)) continue;
            LValueCondense.applyLValueSwap((AssignmentSimple)stm, (AssignmentSimple)stm2, statement, statement2);
        }
    }

    private static void applyLValueSwap(AssignmentSimple a1, AssignmentSimple a2, Op03SimpleStatement stm1, Op03SimpleStatement stm2) {
        Expression r2;
        Expression r1 = a1.getRValue();
        if (!r1.equals(r2 = a2.getRValue())) {
            return;
        }
        LValue l1 = a1.getCreatedLValue();
        LValue l2 = a2.getCreatedLValue();
        if (l1 instanceof StackSSALabel && !(l2 instanceof StackSSALabel)) {
            stm1.replaceStatement(a2);
            stm2.replaceStatement(new AssignmentSimple(BytecodeLoc.TODO, l1, new LValueExpression(l2)));
        }
    }

    public static void condenseLValueChain2(List<Op03SimpleStatement> statements) {
        for (Op03SimpleStatement statement : statements) {
            Statement stm2;
            Op03SimpleStatement statement2;
            Statement stm = statement.getStatement();
            if (!(stm instanceof AssignmentSimple) || statement.getTargets().size() != 1 || (statement2 = statement.getTargets().get(0)).getSources().size() != 1 || !((stm2 = statement2.getStatement()) instanceof AssignmentSimple)) continue;
            LValueCondense.applyLValueCondense((AssignmentSimple)stm, (AssignmentSimple)stm2, statement, statement2);
        }
    }

    private static void applyLValueCondense(AssignmentSimple a1, AssignmentSimple a2, Op03SimpleStatement stm1, Op03SimpleStatement stm2) {
        Expression r1 = a1.getRValue();
        Expression r2 = a2.getRValue();
        LValue l1 = a1.getCreatedLValue();
        LValue l2 = a2.getCreatedLValue();
        if (!r2.equals(new LValueExpression(l1))) {
            return;
        }
        Expression newRhs = null;
        if (r1 instanceof ArithmeticOperation && ((ArithmeticOperation)r1).isMutationOf(l1)) {
            ArithmeticOperation ar1 = (ArithmeticOperation)r1;
            AbstractMutatingAssignmentExpression me = ar1.getMutationOf(l1);
            newRhs = me;
        }
        if (newRhs == null) {
            newRhs = new AssignmentExpression(BytecodeLoc.TODO, l1, r1);
        }
        if (newRhs.getInferredJavaType().getJavaTypeInstance() != l2.getInferredJavaType().getJavaTypeInstance()) {
            return;
        }
        stm2.replaceStatement(new AssignmentSimple(BytecodeLoc.TODO, l2, newRhs));
        stm1.nopOut();
    }
}

