/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.util.collections.SetUtil;

public class PointlessJumps {
    public static void removePointlessJumps(List<Op03SimpleStatement> statements) {
        Statement innerStatement;
        int x;
        int size = statements.size() - 1;
        for (x = 0; x < size - 1; ++x) {
            Op03SimpleStatement a = statements.get(x);
            Op03SimpleStatement b = statements.get(x + 1);
            if (a.getStatement().getClass() != GotoStatement.class || b.getStatement().getClass() != GotoStatement.class || a.getTargets().get(0) != b.getTargets().get(0) || !a.getBlockIdentifiers().equals(b.getBlockIdentifiers())) continue;
            Op03SimpleStatement realTgt = a.getTargets().get(0);
            realTgt.removeSource(a);
            a.replaceTarget(realTgt, b);
            b.addSource(a);
            a.nopOut();
        }
        for (x = 0; x < size - 1; ++x) {
            Op03SimpleStatement maybeJump = statements.get(x);
            if (maybeJump.getStatement().getClass() != GotoStatement.class || maybeJump.getJumpType() == JumpType.BREAK || maybeJump.getTargets().size() != 1 || maybeJump.getTargets().get(0) != statements.get(x + 1)) continue;
            if (maybeJump.getBlockIdentifiers().equals(statements.get(x + 1).getBlockIdentifiers())) {
                maybeJump.nopOut();
                continue;
            }
            Set<BlockIdentifier> changes = SetUtil.difference(maybeJump.getBlockIdentifiers(), statements.get(x + 1).getBlockIdentifiers());
            boolean ok = true;
            for (BlockIdentifier change : changes) {
                if (!change.getBlockType().isLoop()) continue;
                ok = false;
                break;
            }
            if (!ok) continue;
            maybeJump.nopOut();
        }
        for (Op03SimpleStatement statement : statements) {
            JumpingStatement jumpInnerPrior;
            Statement jumpingInnerPriorTarget;
            Op03SimpleStatement prior;
            Statement innerPrior;
            innerStatement = statement.getStatement();
            if (!(innerStatement instanceof JumpingStatement) || statement.getSources().size() != 1 || statement.getTargets().size() != 1 || !((innerPrior = (prior = statement.getSources().get(0)).getStatement()) instanceof JumpingStatement) || (jumpingInnerPriorTarget = (jumpInnerPrior = (JumpingStatement)innerPrior).getJumpTarget()) != innerStatement || !PointlessJumps.movableJump(jumpInnerPrior.getJumpType())) continue;
            statement.nopOut();
        }
        for (int x2 = statements.size() - 1; x2 >= 0; --x2) {
            IfStatement ifStatement;
            Op03SimpleStatement ultimateTarget;
            Op03SimpleStatement target;
            Op03SimpleStatement statement;
            statement = statements.get(x2);
            innerStatement = statement.getStatement();
            if (innerStatement.getClass() == GotoStatement.class) {
                GotoStatement innerGoto = (GotoStatement)innerStatement;
                if (innerGoto.getJumpType() == JumpType.BREAK || (target = statement.getTargets().get(0)) == (ultimateTarget = Misc.followNopGotoChain(target, false, false))) continue;
                ultimateTarget = PointlessJumps.maybeMoveTarget(ultimateTarget, statement, statements);
                target.removeSource(statement);
                statement.replaceTarget(target, ultimateTarget);
                ultimateTarget.addSource(statement);
                continue;
            }
            if (innerStatement.getClass() != IfStatement.class || !PointlessJumps.movableJump((ifStatement = (IfStatement)innerStatement).getJumpType()) || (target = statement.getTargets().get(1)) == (ultimateTarget = Misc.followNopGotoChain(target, false, false))) continue;
            ultimateTarget = PointlessJumps.maybeMoveTarget(ultimateTarget, statement, statements);
            target.removeSource(statement);
            statement.replaceTarget(target, ultimateTarget);
            ultimateTarget.addSource(statement);
        }
    }

    private static Op03SimpleStatement maybeMoveTarget(Op03SimpleStatement expectedRetarget, Op03SimpleStatement source, List<Op03SimpleStatement> statements) {
        int startIdx;
        if (expectedRetarget.getBlockIdentifiers().equals(source.getBlockIdentifiers())) {
            return expectedRetarget;
        }
        int idx = startIdx = statements.indexOf(expectedRetarget);
        Op03SimpleStatement maybe = null;
        while (idx > 0 && statements.get(--idx).getStatement() instanceof TryStatement && !(maybe = statements.get(idx)).getBlockIdentifiers().equals(source.getBlockIdentifiers())) {
        }
        if (maybe == null) {
            return expectedRetarget;
        }
        return maybe;
    }

    private static boolean movableJump(JumpType jumpType) {
        switch (jumpType) {
            case BREAK: 
            case GOTO_OUT_OF_IF: 
            case CONTINUE: {
                return false;
            }
        }
        return true;
    }
}

