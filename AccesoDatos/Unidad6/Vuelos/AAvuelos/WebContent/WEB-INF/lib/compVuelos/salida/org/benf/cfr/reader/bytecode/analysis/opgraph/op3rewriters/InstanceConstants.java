/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.entities.classfilehelpers.ConstantLinks;
import org.benf.cfr.reader.state.DCCommonState;

public class InstanceConstants {
    public static final InstanceConstants INSTANCE = new InstanceConstants();

    public void rewrite(JavaRefTypeInstance thisType, List<Op03SimpleStatement> op03SimpleParseNodes, DCCommonState state) {
        for (Op03SimpleStatement stm : op03SimpleParseNodes) {
            this.rewrite1(thisType, stm, state);
        }
    }

    private void rewrite1(JavaRefTypeInstance thisType, Op03SimpleStatement stm, DCCommonState state) {
        Expression b;
        Statement s = stm.getStatement();
        if (!(s instanceof AssignmentSimple)) {
            return;
        }
        AssignmentSimple ass = (AssignmentSimple)s;
        LValue a = ass.getCreatedLValue();
        Expression e = ass.getRValue();
        if (e instanceof MemberFunctionInvokation) {
            MemberFunctionInvokation m = (MemberFunctionInvokation)e;
            b = m.getObject();
            if (!m.getMethodPrototype().getName().equals("getClass") || !m.getArgs().isEmpty()) {
                return;
            }
        } else if (e instanceof StaticFunctionInvokation) {
            StaticFunctionInvokation sf = (StaticFunctionInvokation)e;
            if (!sf.getClazz().equals(TypeConstants.OBJECTS)) {
                return;
            }
            if (!sf.getMethodPrototype().getName().equals("requireNonNull") || sf.getArgs().size() != 1) {
                return;
            }
            b = sf.getArgs().get(0);
        } else {
            return;
        }
        if (stm.getTargets().size() != 1) {
            return;
        }
        Op03SimpleStatement pop = stm.getTargets().get(0);
        if (pop.getSources().size() != 1) {
            return;
        }
        ExpressionStatement expectedPop = new ExpressionStatement(LValueExpression.of(a));
        if (!pop.getStatement().equals(expectedPop)) {
            return;
        }
        if (pop.getTargets().size() != 1) {
            return;
        }
        Op03SimpleStatement ldc = pop.getTargets().get(0);
        if (ldc.getSources().size() != 1) {
            return;
        }
        s = ldc.getStatement();
        if (!(s instanceof AssignmentSimple)) {
            return;
        }
        AssignmentSimple ldcS = (AssignmentSimple)s;
        Expression rhs = ldcS.getRValue();
        if (!(rhs instanceof Literal)) {
            return;
        }
        Literal lit = (Literal)rhs;
        JavaTypeInstance searchType = b.getInferredJavaType().getJavaTypeInstance().getDeGenerifiedType();
        if (!(searchType instanceof JavaRefTypeInstance)) {
            return;
        }
        JavaRefTypeInstance refSearchType = (JavaRefTypeInstance)searchType;
        Object litValue = lit.getValue().getValue();
        if (litValue == null) {
            return;
        }
        Map<Object, Expression> visibleInstanceConstants = ConstantLinks.getVisibleInstanceConstants(thisType, refSearchType, b, state);
        Expression rvalue = visibleInstanceConstants.get(litValue);
        if (rvalue == null) {
            return;
        }
        BytecodeLoc loc = BytecodeLoc.combineShallow(stm.getStatement(), pop.getStatement(), ldc.getStatement());
        stm.nopOut();
        pop.nopOut();
        ldc.replaceStatement(new AssignmentSimple(loc, ldcS.getCreatedLValue(), rvalue));
    }
}

