/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.util.collections.ListFactory;

public class NegativeJumps {
    public static void rewriteNegativeJumps(List<Op03SimpleStatement> statements, boolean requireChainedConditional) {
        List removeThese = ListFactory.newList();
        for (int x = 0; x < statements.size() - 2; ++x) {
            Op03SimpleStatement yStatement;
            Statement innerZStatement;
            Op03SimpleStatement aStatement = statements.get(x);
            Statement innerAStatement = aStatement.getStatement();
            if (!(innerAStatement instanceof IfStatement)) continue;
            Op03SimpleStatement zStatement = statements.get(x + 1);
            Op03SimpleStatement xStatement = statements.get(x + 2);
            if (requireChainedConditional && !(xStatement.getStatement() instanceof IfStatement) || zStatement.getSources().size() != 1 || aStatement.getTargets().get(0) != zStatement || aStatement.getTargets().get(1) != xStatement || (innerZStatement = zStatement.getStatement()).getClass() != GotoStatement.class || (yStatement = zStatement.getTargets().get(0)) == zStatement) continue;
            aStatement.replaceTarget(xStatement, yStatement);
            aStatement.replaceTarget(zStatement, xStatement);
            yStatement.replaceSource(zStatement, aStatement);
            zStatement.getSources().clear();
            zStatement.getTargets().clear();
            zStatement.replaceStatement(new Nop());
            removeThese.add(zStatement);
            IfStatement innerAIfStatement = (IfStatement)innerAStatement;
            innerAIfStatement.negateCondition();
        }
        statements.removeAll(removeThese);
    }
}

