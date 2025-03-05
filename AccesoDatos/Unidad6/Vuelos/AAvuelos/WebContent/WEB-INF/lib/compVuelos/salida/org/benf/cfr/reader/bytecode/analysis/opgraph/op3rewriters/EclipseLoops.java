/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Op03Rewriters;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;

public class EclipseLoops {
    public static void eclipseLoopPass(List<Op03SimpleStatement> statements) {
        boolean effect = false;
        int len = statements.size() - 1;
        for (int x = 0; x < len; ++x) {
            Statement tgtInr;
            Op03SimpleStatement target;
            Op03SimpleStatement statement = statements.get(x);
            Statement inr = statement.getStatement();
            if (inr.getClass() != GotoStatement.class || (target = statement.getTargets().get(0)) == statement || target.getIndex().isBackJumpFrom(statement) || (tgtInr = target.getStatement()).getClass() != IfStatement.class) continue;
            IfStatement ifStatement = (IfStatement)tgtInr;
            Op03SimpleStatement bodyStart = statements.get(x + 1);
            if (bodyStart != ifStatement.getJumpTarget().getContainer()) continue;
            for (Op03SimpleStatement source : target.getSources()) {
                InstrIndex sourceIdx = source.getIndex();
                if (!sourceIdx.isBackJumpFrom(statement) && !sourceIdx.isBackJumpTo(target)) continue;
            }
            Op03SimpleStatement afterTest = target.getTargets().get(0);
            IfStatement topTest = new IfStatement(ifStatement.getLoc(), ifStatement.getCondition().getNegated().simplify());
            statement.replaceStatement(topTest);
            statement.replaceTarget(target, bodyStart);
            bodyStart.addSource(statement);
            statement.addTarget(afterTest);
            afterTest.replaceSource(target, statement);
            target.replaceStatement(new Nop());
            target.removeSource(statement);
            target.removeTarget(afterTest);
            target.replaceTarget(bodyStart, statement);
            target.replaceStatement(new GotoStatement(BytecodeLoc.NONE));
            bodyStart.removeSource(target);
            statement.addSource(target);
            effect = true;
        }
        if (effect) {
            Op03Rewriters.removePointlessJumps(statements);
        }
    }
}

