/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Op03Rewriters;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CommentStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.DoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfExitingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.WhileStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;

class ReturnRewriter {
    ReturnRewriter() {
    }

    private static void replaceReturningIf(Op03SimpleStatement ifStatement, boolean aggressive) {
        Op03SimpleStatement next;
        boolean requireJustOneSource;
        Op03SimpleStatement tgt;
        if (ifStatement.getStatement().getClass() != IfStatement.class) {
            return;
        }
        IfStatement innerIf = (IfStatement)ifStatement.getStatement();
        Op03SimpleStatement origtgt = tgt = ifStatement.getTargets().get(1);
        boolean bl = requireJustOneSource = !aggressive;
        while ((next = Misc.followNopGoto(tgt, requireJustOneSource, aggressive)) != tgt) {
            tgt = next;
        }
        Statement tgtStatement = tgt.getStatement();
        if (tgtStatement instanceof ReturnStatement) {
            ifStatement.replaceStatement(new IfExitingStatement(innerIf.getLoc(), innerIf.getCondition(), tgtStatement));
            Op03SimpleStatement origfall = ifStatement.getTargets().get(0);
            origfall.setFirstStatementInThisBlock(null);
            BlockIdentifier ifBlock = innerIf.getKnownIfBlock();
            Pair<Set<Op03SimpleStatement>, Set<Op03SimpleStatement>> blockReachableAndExits = Misc.GraphVisitorBlockReachable.getBlockReachableAndExits(origfall, ifBlock);
            for (Op03SimpleStatement stm : blockReachableAndExits.getFirst()) {
                stm.getBlockIdentifiers().remove(ifBlock);
            }
        } else {
            return;
        }
        origtgt.removeSource(ifStatement);
        ifStatement.removeTarget(origtgt);
    }

    static void replaceReturningIfs(List<Op03SimpleStatement> statements, boolean aggressive) {
        List<Op03SimpleStatement> ifStatements = Functional.filter(statements, new TypeFilter<IfStatement>(IfStatement.class));
        for (Op03SimpleStatement ifStatement : ifStatements) {
            ReturnRewriter.replaceReturningIf(ifStatement, aggressive);
        }
    }

    static void propagateToReturn2(List<Op03SimpleStatement> statements) {
        boolean success = false;
        for (Op03SimpleStatement stm : statements) {
            Statement inner = stm.getStatement();
            if (!(inner instanceof ReturnStatement)) continue;
            success |= ReturnRewriter.pushReturnBack(stm);
        }
        if (success) {
            Op03Rewriters.replaceReturningIfs(statements, true);
        }
    }

    private static boolean pushReturnBack(Op03SimpleStatement returnStm) {
        ReturnStatement returnStatement = (ReturnStatement)returnStm.getStatement();
        final List<Op03SimpleStatement> replaceWithReturn = ListFactory.newList();
        new GraphVisitorDFS<Op03SimpleStatement>(returnStm.getSources(), new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

            @Override
            public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                Class<?> clazz = arg1.getStatement().getClass();
                if (clazz == CommentStatement.class || clazz == Nop.class || clazz == DoStatement.class) {
                    arg2.enqueue(arg1.getSources());
                } else if (clazz == WhileStatement.class) {
                    WhileStatement whileStatement = (WhileStatement)arg1.getStatement();
                    if (whileStatement.getCondition() == null) {
                        arg2.enqueue(arg1.getSources());
                        replaceWithReturn.add(arg1);
                    }
                } else if (clazz == GotoStatement.class) {
                    arg2.enqueue(arg1.getSources());
                    replaceWithReturn.add(arg1);
                }
            }
        }).process();
        if (replaceWithReturn.isEmpty()) {
            return false;
        }
        CloneHelper cloneHelper = new CloneHelper();
        for (Op03SimpleStatement remove : replaceWithReturn) {
            remove.replaceStatement((Statement)returnStatement.deepClone(cloneHelper));
            for (Op03SimpleStatement tgt : remove.getTargets()) {
                tgt.removeSource(remove);
            }
            remove.clearTargets();
        }
        return true;
    }
}

