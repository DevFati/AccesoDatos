/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.StatementEquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BoolOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnStatement;
import org.benf.cfr.reader.util.collections.Functional;

public class CondenseConditionals {
    public static boolean condenseConditionals(List<Op03SimpleStatement> statements) {
        boolean effect = false;
        for (int x = 0; x < statements.size(); ++x) {
            boolean retry;
            do {
                boolean takenJumpBy1;
                retry = false;
                Op03SimpleStatement op03SimpleStatement = statements.get(x);
                Statement inner = op03SimpleStatement.getStatement();
                if (!(inner instanceof IfStatement)) continue;
                Op03SimpleStatement fallThrough = op03SimpleStatement.getTargets().get(0);
                Op03SimpleStatement taken = op03SimpleStatement.getTargets().get(1);
                Statement fallthroughInner = fallThrough.getStatement();
                Statement takenInner = taken.getStatement();
                boolean bl = takenJumpBy1 = x < statements.size() - 2 && statements.get(x + 2) == taken;
                if (fallthroughInner instanceof IfStatement) {
                    Op03SimpleStatement sndIf = fallThrough;
                    Op03SimpleStatement sndTaken = sndIf.getTargets().get(1);
                    Op03SimpleStatement sndFallThrough = sndIf.getTargets().get(0);
                    retry = CondenseConditionals.condenseIfs(op03SimpleStatement, sndIf, taken, sndTaken, sndFallThrough, false);
                } else if (fallthroughInner.getClass() == GotoStatement.class && takenJumpBy1 && takenInner instanceof IfStatement) {
                    Op03SimpleStatement negatedTaken = fallThrough.getTargets().get(0);
                    Op03SimpleStatement sndIf = statements.get(x + 2);
                    Op03SimpleStatement sndTaken = sndIf.getTargets().get(1);
                    Op03SimpleStatement sndFallThrough = sndIf.getTargets().get(0);
                    retry = CondenseConditionals.condenseIfs(op03SimpleStatement, sndIf, negatedTaken, sndTaken, sndFallThrough, true);
                }
                if (!retry) continue;
                effect = true;
                while (statements.get(--x).isAgreedNop() && x > 0) {
                }
            } while (retry);
        }
        return effect;
    }

    private static boolean condenseIfs(Op03SimpleStatement if1, Op03SimpleStatement if2, Op03SimpleStatement taken1, Op03SimpleStatement taken2, Op03SimpleStatement fall2, boolean negated1) {
        boolean negate1;
        BoolOp resOp;
        if (if2.getSources().size() != 1) {
            return false;
        }
        if (taken1 == fall2) {
            resOp = BoolOp.AND;
            negate1 = true;
        } else if (taken1 == taken2) {
            resOp = BoolOp.OR;
            negate1 = false;
        } else {
            Statement fall2stm = fall2.getStatement();
            if (fall2stm.getClass() == GotoStatement.class && fall2.getTargets().get(0) == taken1) {
                resOp = BoolOp.AND;
                negate1 = true;
            } else {
                return false;
            }
        }
        IfStatement ifStatement1 = (IfStatement)if1.getStatement();
        IfStatement ifStatement2 = (IfStatement)if2.getStatement();
        ConditionalExpression cond1 = ifStatement1.getCondition();
        ConditionalExpression cond2 = ifStatement2.getCondition();
        if (negated1) {
            boolean bl = negate1 = !negate1;
        }
        if (negate1) {
            cond1 = cond1.getNegated();
        }
        ConditionalExpression combined = new BooleanOperation(BytecodeLoc.combineShallow(ifStatement1, ifStatement2), cond1, cond2, resOp);
        combined = combined.simplify();
        if2.replaceStatement(new IfStatement(BytecodeLoc.NONE, combined));
        for (Op03SimpleStatement target1 : if1.getTargets()) {
            target1.removeSource(if1);
        }
        if1.getTargets().clear();
        for (Op03SimpleStatement source1 : if2.getSources()) {
            source1.removeGotoTarget(if2);
        }
        if2.getSources().clear();
        if1.getTargets().add(if2);
        if2.getSources().add(if1);
        if1.nopOutConditional();
        return true;
    }

    public static boolean condenseConditionals2(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> ifStatements = Functional.filter(statements, new TypeFilter<IfStatement>(IfStatement.class));
        boolean result = false;
        for (Op03SimpleStatement ifStatement : ifStatements) {
            if (CondenseConditionals.condenseConditional2_type1(ifStatement, statements)) {
                result = true;
                continue;
            }
            if (CondenseConditionals.condenseConditional2_type2(ifStatement)) {
                result = true;
                continue;
            }
            if (!CondenseConditionals.condenseConditional2_type3(ifStatement, statements)) continue;
            result = true;
        }
        return result;
    }

    private static boolean condenseConditional2_type3(Op03SimpleStatement ifStatement, List<Op03SimpleStatement> allStatements) {
        Op03SimpleStatement s1c = ifStatement;
        Statement s1 = s1c.getStatement();
        if (s1.getClass() != IfStatement.class) {
            return false;
        }
        Op03SimpleStatement s4c = ifStatement.getTargets().get(1);
        Op03SimpleStatement s2c = ifStatement.getTargets().get(0);
        Statement s2 = s2c.getStatement();
        if (s2.getClass() != IfStatement.class) {
            return false;
        }
        Statement s4 = s4c.getStatement();
        if (s4.getClass() != IfStatement.class) {
            return false;
        }
        Op03SimpleStatement s3c = s2c.getTargets().get(0);
        Statement s3 = s3c.getStatement();
        if (s3.getClass() != GotoStatement.class) {
            return false;
        }
        Op03SimpleStatement s5c = s2c.getTargets().get(1);
        Op03SimpleStatement y = s3c.getTargets().get(0);
        if (s4c.getTargets().get(1) != y) {
            return false;
        }
        if (s4c.getTargets().get(0) != s5c) {
            return false;
        }
        if (s2c.getSources().size() != 1) {
            return false;
        }
        if (s3c.getSources().size() != 1) {
            return false;
        }
        if (s4c.getSources().size() != 1) {
            return false;
        }
        IfStatement is1 = (IfStatement)s1;
        IfStatement is2 = (IfStatement)s2;
        IfStatement is4 = (IfStatement)s4;
        BooleanExpression cond = new BooleanExpression(new TernaryExpression(s1.getLoc(), is1.getCondition(), is4.getCondition(), is2.getCondition().getNegated()));
        s1c.replaceStatement(new IfStatement(BytecodeLoc.combineShallow(s1, s2, s3, s4), cond));
        s1c.replaceTarget(s4c, y);
        y.replaceSource(s4c, s1c);
        s2c.replaceStatement(new GotoStatement(BytecodeLoc.NONE));
        s2c.removeGotoTarget(s3c);
        s3c.removeSource(s2c);
        s3c.clear();
        s4c.clear();
        int idx = allStatements.indexOf(s1c);
        if (allStatements.size() > idx + 5 && allStatements.get(idx + 1) == s2c && allStatements.get(idx + 2) == s3c && allStatements.get(idx + 3) == s4c && allStatements.get(idx + 4) == s5c) {
            s5c.replaceSource(s2c, s1c);
            s1c.replaceTarget(s2c, s5c);
            s2c.clear();
        }
        return true;
    }

    private static boolean condenseConditional2_type2(Op03SimpleStatement ifStatement) {
        Statement innerStatement = ifStatement.getStatement();
        if (!(innerStatement instanceof IfStatement)) {
            return false;
        }
        IfStatement innerIf = (IfStatement)innerStatement;
        Op03SimpleStatement tgt1 = ifStatement.getTargets().get(0);
        Op03SimpleStatement tgt2 = ifStatement.getTargets().get(1);
        if (tgt1.getSources().size() != 1) {
            return false;
        }
        if (tgt2.getSources().size() != 1) {
            return false;
        }
        if (tgt1.getTargets().size() != 1) {
            return false;
        }
        if (tgt2.getTargets().size() != 1) {
            return false;
        }
        Op03SimpleStatement evTgt = tgt1.getTargets().get(0);
        evTgt = Misc.followNopGoto(evTgt, true, false);
        Op03SimpleStatement oneSource = tgt1;
        if (!evTgt.getSources().contains(oneSource)) {
            oneSource = oneSource.getTargets().get(0);
            if (!evTgt.getSources().contains(oneSource)) {
                return false;
            }
        }
        if (evTgt.getSources().size() < 2) {
            return false;
        }
        if (tgt2.getTargets().get(0) != evTgt) {
            return false;
        }
        Statement stm1 = tgt1.getStatement();
        Statement stm2 = tgt2.getStatement();
        if (!(stm1 instanceof AssignmentSimple) || !(stm2 instanceof AssignmentSimple)) {
            return false;
        }
        AssignmentSimple a1 = (AssignmentSimple)stm1;
        AssignmentSimple a2 = (AssignmentSimple)stm2;
        LValue lv = a1.getCreatedLValue();
        if (!lv.equals(a2.getCreatedLValue())) {
            return false;
        }
        ConditionalExpression condition = innerIf.getCondition().getNegated();
        condition = condition.simplify();
        ifStatement.replaceStatement(new AssignmentSimple(innerIf.getLoc(), lv, new TernaryExpression(innerIf.getLoc(), condition, a1.getRValue(), a2.getRValue())));
        ifStatement.getSSAIdentifiers().consumeEntry(evTgt.getSSAIdentifiers());
        oneSource.replaceStatement(new Nop());
        oneSource.removeTarget(evTgt);
        tgt2.replaceStatement(new Nop());
        tgt2.removeTarget(evTgt);
        evTgt.removeSource(oneSource);
        evTgt.removeSource(tgt2);
        evTgt.getSources().add(ifStatement);
        for (Op03SimpleStatement tgt : ifStatement.getTargets()) {
            tgt.removeSource(ifStatement);
        }
        ifStatement.getTargets().clear();
        ifStatement.addTarget(evTgt);
        tgt1.replaceStatement(new Nop());
        if (lv instanceof StackSSALabel) {
            ((StackSSALabel)lv).getStackEntry().decSourceCount();
        }
        return true;
    }

    private static boolean condenseConditional2_type1(Op03SimpleStatement ifStatement, List<Op03SimpleStatement> allStatements) {
        block14: {
            Op03SimpleStatement next;
            Op03SimpleStatement nontaken3rewrite;
            Op03SimpleStatement nontaken2rewrite;
            if (!(ifStatement.getStatement() instanceof IfStatement)) {
                return false;
            }
            Op03SimpleStatement taken1 = ifStatement.getTargets().get(1);
            Op03SimpleStatement nottaken1 = ifStatement.getTargets().get(0);
            if (!(nottaken1.getStatement() instanceof IfStatement)) {
                return false;
            }
            Op03SimpleStatement ifStatement2 = nottaken1;
            Op03SimpleStatement taken2 = ifStatement2.getTargets().get(1);
            Op03SimpleStatement nottaken2 = ifStatement2.getTargets().get(0);
            Op03SimpleStatement nottaken2Immed = nottaken2;
            if (nottaken2Immed.getSources().size() != 1) {
                return false;
            }
            nottaken2 = Misc.followNopGotoChain(nottaken2, true, false);
            while ((nontaken2rewrite = Misc.followNopGoto(nottaken2, true, false)) != nottaken2) {
                nottaken2 = nontaken2rewrite;
            }
            if (!(taken1.getStatement() instanceof IfStatement)) {
                return false;
            }
            if (taken1.getSources().size() != 1) {
                return false;
            }
            Op03SimpleStatement ifStatement3 = taken1;
            Op03SimpleStatement taken3 = ifStatement3.getTargets().get(1);
            Op03SimpleStatement nottaken3 = ifStatement3.getTargets().get(0);
            Op03SimpleStatement notTaken3Source = ifStatement3;
            while ((nontaken3rewrite = Misc.followNopGoto(nottaken3, true, false)) != nottaken3) {
                notTaken3Source = nottaken3;
                nottaken3 = nontaken3rewrite;
            }
            if (nottaken2 != nottaken3) {
                if (nottaken2.getStatement() instanceof ReturnStatement) {
                    if (!nottaken2.getStatement().equivalentUnder(nottaken3.getStatement(), new StatementEquivalenceConstraint(nottaken2, nottaken3))) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            if (taken2 != taken3) {
                return false;
            }
            IfStatement if1 = (IfStatement)ifStatement.getStatement();
            IfStatement if2 = (IfStatement)ifStatement2.getStatement();
            IfStatement if3 = (IfStatement)ifStatement3.getStatement();
            ConditionalExpression newCond = new BooleanExpression(new TernaryExpression(BytecodeLoc.combineShallow(if1, if2, if3), if1.getCondition().getNegated().simplify(), if2.getCondition().getNegated().simplify(), if3.getCondition().getNegated().simplify())).getNegated();
            ifStatement.replaceTarget(taken1, taken3);
            taken3.addSource(ifStatement);
            taken3.removeSource(ifStatement2);
            taken3.removeSource(ifStatement3);
            nottaken1.getSources().remove(ifStatement);
            nottaken2Immed.replaceSource(ifStatement2, ifStatement);
            ifStatement.replaceTarget(nottaken1, nottaken2Immed);
            nottaken3.removeSource(notTaken3Source);
            ifStatement2.replaceStatement(new Nop());
            ifStatement3.replaceStatement(new Nop());
            ifStatement2.removeTarget(taken3);
            ifStatement3.removeTarget(taken3);
            ifStatement.replaceStatement(new IfStatement(BytecodeLoc.NONE, newCond));
            if (nottaken2Immed.getSources().size() != 1 || !nottaken2Immed.getSources().get(0).getIndex().isBackJumpFrom(nottaken2Immed) || nottaken2Immed.getStatement().getClass() != GotoStatement.class) break block14;
            Op03SimpleStatement nottaken2ImmedTgt = nottaken2Immed.getTargets().get(0);
            int idx = allStatements.indexOf(nottaken2Immed);
            int idx2 = idx + 1;
            while ((next = allStatements.get(idx2)).getStatement() instanceof Nop) {
                ++idx2;
            }
            if (next == nottaken2ImmedTgt) {
                nottaken2ImmedTgt.replaceSource(nottaken2Immed, ifStatement);
                ifStatement.replaceTarget(nottaken2Immed, nottaken2ImmedTgt);
            }
        }
        return true;
    }
}

