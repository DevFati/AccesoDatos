/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.CompareByIndex;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.WhileStatement;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;

public class Cleaner {
    public static List<Op03SimpleStatement> removeUnreachableCode(List<Op03SimpleStatement> statements, final boolean checkBackJumps) {
        final Set reachable = SetFactory.newSet();
        reachable.add(statements.get(0));
        GraphVisitorDFS<Op03SimpleStatement> gv = new GraphVisitorDFS<Op03SimpleStatement>(statements.get(0), new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

            @Override
            public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                reachable.add(arg1);
                arg2.enqueue(arg1.getTargets());
                for (Op03SimpleStatement source : arg1.getSources()) {
                    if (source.getTargets().contains(arg1)) continue;
                    throw new IllegalStateException("Inconsistent graph " + source + " does not have a target of " + arg1);
                }
                for (Op03SimpleStatement test : arg1.getTargets()) {
                    Statement argContained = arg1.getStatement();
                    if (checkBackJumps && !(argContained instanceof JumpingStatement) && !(argContained instanceof WhileStatement) && test.getIndex().isBackJumpFrom(arg1)) {
                        throw new IllegalStateException("Backjump on non jumping statement " + arg1);
                    }
                    if (test.getSources().contains(arg1)) continue;
                    throw new IllegalStateException("Inconsistent graph " + test + " does not have a source " + arg1);
                }
            }
        });
        gv.process();
        List<Op03SimpleStatement> result = ListFactory.newList();
        for (Op03SimpleStatement statement : statements) {
            if (!reachable.contains(statement)) continue;
            result.add(statement);
        }
        for (Op03SimpleStatement res1 : result) {
            List<Op03SimpleStatement> sources = ListFactory.newList(res1.getSources());
            for (Op03SimpleStatement source : sources) {
                if (reachable.contains(source)) continue;
                res1.removeSource(source);
            }
        }
        return result;
    }

    public static List<Op03SimpleStatement> sortAndRenumber(List<Op03SimpleStatement> statements) {
        boolean nonNopSeen = false;
        List<Op03SimpleStatement> result = ListFactory.newList();
        for (Op03SimpleStatement statement : statements) {
            boolean thisIsNop = statement.isAgreedNop();
            if (!nonNopSeen) {
                result.add(statement);
                if (thisIsNop) continue;
                nonNopSeen = true;
                continue;
            }
            if (thisIsNop) continue;
            result.add(statement);
        }
        Cleaner.sortAndRenumberInPlace(result);
        return result;
    }

    static void sortAndRenumberFromInPlace(List<Op03SimpleStatement> statements, InstrIndex start) {
        Collections.sort(statements, new CompareByIndex());
        for (Op03SimpleStatement statement : statements) {
            statement.setIndex(start);
            start = start.justAfter();
        }
    }

    static void sortAndRenumberInPlace(List<Op03SimpleStatement> statements) {
        Collections.sort(statements, new CompareByIndex());
        Cleaner.reindexInPlace(statements);
    }

    public static void reindexInPlace(List<Op03SimpleStatement> statements) {
        int newIndex = 0;
        Op03SimpleStatement prev = null;
        for (Op03SimpleStatement statement : statements) {
            statement.setLinearlyPrevious(prev);
            statement.setLinearlyNext(null);
            if (prev != null) {
                prev.setLinearlyNext(statement);
            }
            statement.setIndex(new InstrIndex(newIndex++));
            prev = statement;
        }
    }

    public static void reLinkInPlace(List<Op03SimpleStatement> statements) {
        Op03SimpleStatement prev = null;
        for (Op03SimpleStatement statement : statements) {
            statement.setLinearlyPrevious(prev);
            statement.setLinearlyNext(null);
            if (prev != null) {
                prev.setLinearlyNext(statement);
            }
            prev = statement;
        }
    }
}

