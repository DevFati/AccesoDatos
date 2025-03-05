/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

class IsolatedStackValue {
    IsolatedStackValue() {
    }

    static void nopIsolatedStackValues(List<Op03SimpleStatement> statements) {
        Set blackList = SetFactory.newSet();
        Map consumptions = MapFactory.newMap();
        Map assignments = MapFactory.newMap();
        for (Op03SimpleStatement op03SimpleStatement : statements) {
            StackSSALabel stackValue;
            Statement stm = op03SimpleStatement.getStatement();
            if (stm instanceof ExpressionStatement) {
                StackValue sv;
                StackSSALabel stackValue2;
                Expression expression = ((ExpressionStatement)stm).getExpression();
                if (!(expression instanceof StackValue) || consumptions.put(stackValue2 = (sv = (StackValue)expression).getStackValue(), op03SimpleStatement) == null && stackValue2.getStackEntry().getUsageCount() <= 1L) continue;
                blackList.add(stackValue2);
                continue;
            }
            if (!(stm instanceof AssignmentSimple) || !(stm.getCreatedLValue() instanceof StackSSALabel) || assignments.put(stackValue = (StackSSALabel)stm.getCreatedLValue(), op03SimpleStatement) == null) continue;
            blackList.add(stackValue);
        }
        for (Map.Entry entry : consumptions.entrySet()) {
            StackSSALabel label = (StackSSALabel)entry.getKey();
            Op03SimpleStatement assign = (Op03SimpleStatement)assignments.get(label);
            if (blackList.contains(label) || assign == null) continue;
            ((Op03SimpleStatement)entry.getValue()).replaceStatement(new Nop());
            assign.replaceStatement(new ExpressionStatement(assign.getStatement().getRValue()));
        }
    }
}

