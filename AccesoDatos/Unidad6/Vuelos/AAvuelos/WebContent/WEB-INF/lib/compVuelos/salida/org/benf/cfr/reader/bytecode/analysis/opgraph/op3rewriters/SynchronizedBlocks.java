/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CommentStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.FinallyStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.MonitorEnterStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.MonitorExitStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ThrowStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;

public class SynchronizedBlocks {
    public static void findSynchronizedBlocks(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> enters = Functional.filter(statements, new TypeFilter<MonitorEnterStatement>(MonitorEnterStatement.class));
        for (Op03SimpleStatement enter : enters) {
            MonitorEnterStatement monitorEnterStatement = (MonitorEnterStatement)enter.getStatement();
            SynchronizedBlocks.findSynchronizedRange(enter, monitorEnterStatement.getMonitor());
        }
    }

    private static void findSynchronizedRange(Op03SimpleStatement start, Expression monitorEnterExpression) {
        final Expression monitor = SynchronizedBlocks.removeCasts(monitorEnterExpression);
        final Set<Op03SimpleStatement> addToBlock = SetFactory.newSet();
        final Map foundExits = MapFactory.newOrderedMap();
        final Set<Op03SimpleStatement> extraNodes = SetFactory.newSet();
        final Set leaveExitsMutex = SetFactory.newSet();
        GraphVisitorDFS<Op03SimpleStatement> marker = new GraphVisitorDFS<Op03SimpleStatement>(start.getTargets(), new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

            @Override
            public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                MonitorExitStatement monitorExitStatement;
                Expression exitMonitor;
                TryStatement tryStatement;
                Set<Expression> tryMonitors;
                Statement statement = arg1.getStatement();
                if (statement instanceof TryStatement && (tryMonitors = (tryStatement = (TryStatement)statement).getMonitors()).contains(monitor)) {
                    leaveExitsMutex.add(tryStatement.getBlockIdentifier());
                    List<Op03SimpleStatement> tgts = arg1.getTargets();
                    int len = tgts.size();
                    for (int x = 1; x < len; ++x) {
                        Statement innerS = tgts.get(x).getStatement();
                        if (innerS instanceof CatchStatement) {
                            leaveExitsMutex.add(((CatchStatement)innerS).getCatchBlockIdent());
                            continue;
                        }
                        if (!(innerS instanceof FinallyStatement)) continue;
                        leaveExitsMutex.add(((FinallyStatement)innerS).getFinallyBlockIdent());
                    }
                }
                if (statement instanceof MonitorExitStatement && monitor.equals(SynchronizedBlocks.removeCasts(exitMonitor = (monitorExitStatement = (MonitorExitStatement)statement).getMonitor()))) {
                    Statement targetStatement;
                    foundExits.put(arg1, monitorExitStatement);
                    addToBlock.add(arg1);
                    if (arg1.getTargets().size() == 1 && ((targetStatement = (arg1 = arg1.getTargets().get(0)).getStatement()) instanceof ThrowStatement || targetStatement instanceof ReturnStatement || targetStatement instanceof Nop || targetStatement instanceof GotoStatement)) {
                        extraNodes.add(arg1);
                    }
                    return;
                }
                addToBlock.add(arg1);
                if (SetUtil.hasIntersection(arg1.getBlockIdentifiers(), leaveExitsMutex)) {
                    for (Op03SimpleStatement tgt : arg1.getTargets()) {
                        if (!SetUtil.hasIntersection(tgt.getBlockIdentifiers(), leaveExitsMutex)) continue;
                        arg2.enqueue(tgt);
                    }
                } else {
                    arg2.enqueue(arg1.getTargets());
                }
            }
        });
        marker.process();
        addToBlock.remove(start);
        Set<Op03SimpleStatement> requiredComments = SetFactory.newSet();
        Iterator foundExitIter = foundExits.keySet().iterator();
        while (foundExitIter.hasNext()) {
            final Op03SimpleStatement foundExit = (Op03SimpleStatement)foundExitIter.next();
            final Set<BlockIdentifier> exitBlocks = SetFactory.newSet(foundExit.getBlockIdentifiers());
            exitBlocks.removeAll(start.getBlockIdentifiers());
            final List<Op03SimpleStatement> added = ListFactory.newList();
            GraphVisitorDFS<Op03SimpleStatement> graphVisitorDFS = new GraphVisitorDFS<Op03SimpleStatement>(foundExit, new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

                @Override
                public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                    if (SetUtil.hasIntersection(exitBlocks, arg1.getBlockIdentifiers())) {
                        if (arg1 == foundExit) {
                            arg2.enqueue(arg1.getTargets());
                        } else if (addToBlock.add(arg1)) {
                            added.add(arg1);
                            arg2.enqueue(arg1.getTargets());
                        }
                    }
                }
            });
            graphVisitorDFS.process();
            if (!SynchronizedBlocks.anyOpHasEffect(added)) continue;
            requiredComments.add(foundExit);
            foundExitIter.remove();
        }
        MonitorEnterStatement monitorEnterStatement = (MonitorEnterStatement)start.getStatement();
        BlockIdentifier blockIdentifier = monitorEnterStatement.getBlockIdentifier();
        for (Op03SimpleStatement op03SimpleStatement : addToBlock) {
            op03SimpleStatement.getBlockIdentifiers().add(blockIdentifier);
        }
        for (Map.Entry entry : foundExits.entrySet()) {
            Expression exit = ((MonitorExitStatement)entry.getValue()).getMonitor();
            Op03SimpleStatement exitStm = (Op03SimpleStatement)entry.getKey();
            if (monitor.equals(exit)) {
                exitStm.nopOut();
                continue;
            }
            exitStm.replaceStatement(new ExpressionStatement(exit));
        }
        for (Op03SimpleStatement op03SimpleStatement : requiredComments) {
            op03SimpleStatement.replaceStatement(new CommentStatement("MONITOREXIT " + op03SimpleStatement));
        }
        for (Op03SimpleStatement op03SimpleStatement : extraNodes) {
            boolean allParents = true;
            for (Op03SimpleStatement source : op03SimpleStatement.getSources()) {
                if (source.getBlockIdentifiers().contains(blockIdentifier)) continue;
                allParents = false;
            }
            if (!allParents) continue;
            op03SimpleStatement.getBlockIdentifiers().add(blockIdentifier);
        }
    }

    private static Expression removeCasts(Expression e) {
        while (e instanceof CastExpression) {
            e = ((CastExpression)e).getChild();
        }
        return e;
    }

    private static boolean anyOpHasEffect(List<Op03SimpleStatement> ops) {
        for (Op03SimpleStatement op : ops) {
            Statement stm = op.getStatement();
            Class<?> stmcls = stm.getClass();
            if (stmcls == GotoStatement.class || stmcls == ThrowStatement.class || stmcls == CommentStatement.class || stm instanceof ReturnStatement) continue;
            return true;
        }
        return false;
    }
}

