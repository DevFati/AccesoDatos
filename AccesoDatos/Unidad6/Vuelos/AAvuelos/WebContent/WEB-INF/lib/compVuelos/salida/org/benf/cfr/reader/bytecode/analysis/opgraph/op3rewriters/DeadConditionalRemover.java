/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.util.collections.MapFactory;

public class DeadConditionalRemover {
    public static final DeadConditionalRemover INSTANCE = new DeadConditionalRemover();

    public List<Op03SimpleStatement> rewrite(List<Op03SimpleStatement> statements) {
        boolean effect = false;
        for (Op03SimpleStatement stm : statements) {
            if (!(stm.getStatement() instanceof IfStatement) || !this.rewrite(stm)) continue;
            effect = true;
        }
        if (effect) {
            return Cleaner.removeUnreachableCode(statements, false);
        }
        return statements;
    }

    private boolean rewrite(Op03SimpleStatement stm) {
        IfStatement ifs = (IfStatement)stm.getStatement();
        Map<LValue, Literal> effects = MapFactory.newMap();
        Literal val = ifs.getCondition().getComputedLiteral(effects);
        if (val == null || !effects.isEmpty()) {
            return false;
        }
        Op03SimpleStatement removeTarget = null;
        AbstractStatement replacement = null;
        if (Literal.TRUE.equals(val)) {
            removeTarget = stm.getTargets().get(0);
            replacement = new GotoStatement(BytecodeLoc.TODO);
        } else if (Literal.FALSE.equals(val)) {
            removeTarget = stm.getTargets().get(1);
            replacement = new Nop();
        }
        if (removeTarget == null) {
            return false;
        }
        removeTarget.removeSource(stm);
        stm.replaceStatement(replacement);
        stm.removeGotoTarget(removeTarget);
        return true;
    }
}

