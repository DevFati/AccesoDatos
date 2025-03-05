/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.BoxingHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArrayIndex;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArrayLength;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BoolOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ComparisonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ForIterStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ForStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.WhileStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;

public class IterLoopRewriter {
    private static Pair<ConditionalExpression, ConditionalExpression> getSplitAnd(ConditionalExpression cnd) {
        if (!(cnd instanceof BooleanOperation)) {
            return Pair.make(cnd, null);
        }
        BooleanOperation op = (BooleanOperation)cnd;
        if (op.getOp() != BoolOp.AND) {
            return Pair.make(cnd, null);
        }
        return Pair.make(op.getLhs(), op.getRhs());
    }

    private static void rewriteArrayForLoop(final Op03SimpleStatement loop, List<Op03SimpleStatement> statements) {
        Op03SimpleStatement realLoopStart;
        Op03SimpleStatement preceeding = Misc.findSingleBackSource(loop);
        if (preceeding == null) {
            return;
        }
        ForStatement forStatement = (ForStatement)loop.getStatement();
        WildcardMatch wildcardMatch = new WildcardMatch();
        if (!wildcardMatch.match(new AssignmentSimple(BytecodeLoc.TODO, wildcardMatch.getLValueWildCard("iter"), new Literal(TypedLiteral.getInt(0))), forStatement.getInitial())) {
            return;
        }
        LValue originalLoopVariable = wildcardMatch.getLValueWildCard("iter").getMatch();
        List<AbstractAssignmentExpression> assignments = forStatement.getAssignments();
        if (assignments.size() != 1) {
            return;
        }
        AbstractAssignmentExpression assignment = assignments.get(0);
        boolean incrMatch = assignment.isSelfMutatingOp1(originalLoopVariable, ArithOp.PLUS);
        if (!incrMatch) {
            return;
        }
        ConditionalExpression condition = forStatement.getCondition();
        Pair<ConditionalExpression, ConditionalExpression> condpr = IterLoopRewriter.getSplitAnd(condition);
        if (!wildcardMatch.match(new ComparisonOperation(BytecodeLoc.TODO, new LValueExpression(originalLoopVariable), new LValueExpression(wildcardMatch.getLValueWildCard("bound")), CompOp.LT), condpr.getFirst())) {
            return;
        }
        LValue originalLoopBound = wildcardMatch.getLValueWildCard("bound").getMatch();
        if (!wildcardMatch.match(new AssignmentSimple(BytecodeLoc.TODO, originalLoopBound, new ArrayLength(BytecodeLoc.TODO, new LValueExpression(wildcardMatch.getLValueWildCard("array")))), preceeding.getStatement())) {
            return;
        }
        LValue originalArray = wildcardMatch.getLValueWildCard("array").getMatch();
        Expression arrayStatement = new LValueExpression(originalArray);
        Op03SimpleStatement prepreceeding = null;
        if (preceeding.getSources().size() == 1 && wildcardMatch.match(new AssignmentSimple(BytecodeLoc.NONE, originalArray, wildcardMatch.getExpressionWildCard("value")), preceeding.getSources().get(0).getStatement())) {
            prepreceeding = preceeding.getSources().get(0);
            arrayStatement = wildcardMatch.getExpressionWildCard("value").getMatch();
        }
        Op03SimpleStatement loopStart = realLoopStart = loop.getTargets().get(0);
        if (condpr.getSecond() != null) {
            IfStatement fakeLoopStm = new IfStatement(BytecodeLoc.TODO, condpr.getSecond().getNegated());
            fakeLoopStm.setJumpType(JumpType.BREAK);
            loopStart = new Op03SimpleStatement(loopStart.getBlockIdentifiers(), fakeLoopStm, loopStart.getIndex().justBefore());
        }
        WildcardMatch.LValueWildcard sugariterWC = wildcardMatch.getLValueWildCard("sugariter");
        ArrayIndex arrIndex = new ArrayIndex(BytecodeLoc.TODO, new LValueExpression(originalArray), new LValueExpression(originalLoopVariable));
        boolean hiddenIter = false;
        if (!wildcardMatch.match(new AssignmentSimple(BytecodeLoc.TODO, sugariterWC, arrIndex), loopStart.getStatement())) {
            Set<Expression> poison = SetFactory.newSet(new LValueExpression(originalLoopVariable));
            if (!Misc.findHiddenIter(loopStart.getStatement(), sugariterWC, arrIndex, poison)) {
                return;
            }
            hiddenIter = true;
        }
        LValue sugarIter = sugariterWC.getMatch();
        final BlockIdentifier forBlock = forStatement.getBlockIdentifier();
        List<Op03SimpleStatement> statementsInBlock = Functional.filter(statements, new Predicate<Op03SimpleStatement>(){

            @Override
            public boolean test(Op03SimpleStatement in) {
                return in.getBlockIdentifiers().contains(forBlock);
            }
        });
        LValueUsageCollectorSimple usageCollector = new LValueUsageCollectorSimple();
        final Set<LValue> cantUpdate = SetFactory.newSet(originalArray, originalLoopBound, originalLoopVariable);
        for (Op03SimpleStatement inBlock : statementsInBlock) {
            if (inBlock == loopStart) continue;
            Statement inStatement = inBlock.getStatement();
            inStatement.collectLValueUsage(usageCollector);
            for (LValue cantUse : cantUpdate) {
                if (!usageCollector.isUsed(cantUse)) continue;
                return;
            }
            LValue updated = inStatement.getCreatedLValue();
            if (updated == null || !cantUpdate.contains(updated)) continue;
            return;
        }
        final AtomicBoolean res = new AtomicBoolean();
        GraphVisitorDFS<Op03SimpleStatement> graphVisitor = new GraphVisitorDFS<Op03SimpleStatement>(loop, new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

            @Override
            public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                if (loop != arg1 && !arg1.getBlockIdentifiers().contains(forBlock)) {
                    AssignmentSimple assignmentSimple;
                    Statement inStatement = arg1.getStatement();
                    if (inStatement instanceof AssignmentSimple && cantUpdate.contains((assignmentSimple = (AssignmentSimple)inStatement).getCreatedLValue())) {
                        return;
                    }
                    LValueUsageCollectorSimple usageCollector = new LValueUsageCollectorSimple();
                    inStatement.collectLValueUsage(usageCollector);
                    for (LValue cantUse : cantUpdate) {
                        if (!usageCollector.isUsed(cantUse)) continue;
                        res.set(true);
                        return;
                    }
                }
                for (Op03SimpleStatement target : arg1.getTargets()) {
                    arg2.enqueue(target);
                }
            }
        });
        graphVisitor.process();
        if (res.get()) {
            return;
        }
        loop.replaceStatement(new ForIterStatement(forStatement.getCombinedLoc(), forBlock, sugarIter, arrayStatement, originalArray));
        if (loopStart != realLoopStart) {
            if (hiddenIter) {
                loop.replaceTarget(realLoopStart, loopStart);
                realLoopStart.replaceSource(loop, loopStart);
                loopStart.addSource(loop);
                loopStart.addTarget(realLoopStart);
                Op03SimpleStatement endStm = loop.getTargets().get(1);
                loopStart.addTarget(endStm);
                endStm.addSource(loopStart);
                Misc.replaceHiddenIter(loopStart.getStatement(), sugariterWC.getMatch(), arrIndex);
                statements.add(statements.indexOf(realLoopStart), loopStart);
            }
        } else if (hiddenIter) {
            Misc.replaceHiddenIter(loopStart.getStatement(), sugariterWC.getMatch(), arrIndex);
        } else {
            loopStart.nopOut();
        }
        preceeding.nopOut();
        if (prepreceeding != null) {
            prepreceeding.nopOut();
        }
    }

    public static void rewriteArrayForLoops(List<Op03SimpleStatement> statements) {
        for (Op03SimpleStatement loop : Functional.filter(statements, new TypeFilter<ForStatement>(ForStatement.class))) {
            IterLoopRewriter.rewriteArrayForLoop(loop, statements);
        }
    }

    private static void rewriteIteratorWhileLoop(final Op03SimpleStatement loop, List<Op03SimpleStatement> statements) {
        LValue sugarIter;
        WildcardMatch.MemberFunctionInvokationWildcard nextCall;
        Op03SimpleStatement realLoopStart;
        List<JavaTypeInstance> types;
        WhileStatement whileStatement = (WhileStatement)loop.getStatement();
        Op03SimpleStatement preceeding = Misc.findSingleBackSource(loop);
        if (preceeding == null) {
            return;
        }
        WildcardMatch wildcardMatch = new WildcardMatch();
        ConditionalExpression condition = whileStatement.getCondition();
        Pair<ConditionalExpression, ConditionalExpression> condpr = IterLoopRewriter.getSplitAnd(condition);
        if (!wildcardMatch.match(new BooleanExpression(wildcardMatch.getMemberFunction("hasnextfn", "hasNext", new LValueExpression(wildcardMatch.getLValueWildCard("iterable")))), condpr.getFirst())) {
            return;
        }
        final LValue iterable = wildcardMatch.getLValueWildCard("iterable").getMatch();
        JavaTypeInstance iterableType = iterable.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance iterableContentType = null;
        if (iterableType instanceof JavaGenericRefTypeInstance && (types = ((JavaGenericRefTypeInstance)iterableType).getGenericTypes()).size() == 1) {
            iterableContentType = types.get(0);
        }
        Op03SimpleStatement loopStart = realLoopStart = loop.getTargets().get(0);
        if (condpr.getSecond() != null) {
            IfStatement fakeLoopStm = new IfStatement(BytecodeLoc.TODO, condpr.getSecond().getNegated());
            fakeLoopStm.setJumpType(JumpType.BREAK);
            loopStart = new Op03SimpleStatement(loopStart.getBlockIdentifiers(), fakeLoopStm, loopStart.getIndex().justBefore());
        }
        boolean hiddenIter = false;
        WildcardMatch.LValueWildcard sugariterWC = wildcardMatch.getLValueWildCard("sugariter");
        if (!(wildcardMatch.match(new AssignmentSimple(BytecodeLoc.NONE, sugariterWC, nextCall = wildcardMatch.getMemberFunction("nextfn", "next", new LValueExpression(wildcardMatch.getLValueWildCard("iterable")))), loopStart.getStatement()) || wildcardMatch.match(new AssignmentSimple(BytecodeLoc.NONE, sugariterWC, wildcardMatch.getCastExpressionWildcard("cast", nextCall)), loopStart.getStatement()) || iterableContentType != null && BoxingHelper.isBoxedType(iterableContentType) && wildcardMatch.match(new AssignmentSimple(BytecodeLoc.NONE, sugariterWC, wildcardMatch.getMemberFunction("unbox", BoxingHelper.getUnboxingMethodName(iterableContentType), nextCall)), loopStart.getStatement()))) {
            Set<Expression> poison = SetFactory.newSet(new LValueExpression(iterable));
            if (!Misc.findHiddenIter(loopStart.getStatement(), sugariterWC, nextCall, poison)) {
                return;
            }
            hiddenIter = true;
        }
        if (!(sugarIter = wildcardMatch.getLValueWildCard("sugariter").getMatch()).validIterator()) {
            return;
        }
        if (!wildcardMatch.match(new AssignmentSimple(BytecodeLoc.NONE, wildcardMatch.getLValueWildCard("iterable"), wildcardMatch.getMemberFunction("iterator", "iterator", wildcardMatch.getExpressionWildCard("iteratorsource"))), preceeding.getStatement())) {
            return;
        }
        Expression iterSource = wildcardMatch.getExpressionWildCard("iteratorsource").getMatch();
        final BlockIdentifier blockIdentifier = whileStatement.getBlockIdentifier();
        List<Op03SimpleStatement> statementsInBlock = Functional.filter(statements, new Predicate<Op03SimpleStatement>(){

            @Override
            public boolean test(Op03SimpleStatement in) {
                return in.getBlockIdentifiers().contains(blockIdentifier);
            }
        });
        LValueUsageCollectorSimple usageCollector = new LValueUsageCollectorSimple();
        for (Op03SimpleStatement inBlock : statementsInBlock) {
            if (inBlock == loopStart) continue;
            Statement inStatement = inBlock.getStatement();
            inStatement.collectLValueUsage(usageCollector);
            if (usageCollector.isUsed(iterable)) {
                return;
            }
            LValue updated = inStatement.getCreatedLValue();
            if (updated == null || !updated.equals(iterable)) continue;
            return;
        }
        JavaTypeInstance iteratorSourceType = iterSource.getInferredJavaType().getJavaTypeInstance();
        BindingSuperContainer supers = iteratorSourceType.getBindingSupers();
        if (supers != null && !supers.containsBase(TypeConstants.ITERABLE)) {
            return;
        }
        final AtomicBoolean res = new AtomicBoolean();
        GraphVisitorDFS<Op03SimpleStatement> graphVisitor = new GraphVisitorDFS<Op03SimpleStatement>(loop, new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

            @Override
            public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                if (loop != arg1 && !arg1.getBlockIdentifiers().contains(blockIdentifier)) {
                    AssignmentSimple assignmentSimple;
                    Statement inStatement = arg1.getStatement();
                    if (inStatement instanceof AssignmentSimple && iterable.equals((assignmentSimple = (AssignmentSimple)inStatement).getCreatedLValue())) {
                        return;
                    }
                    LValueUsageCollectorSimple usageCollector = new LValueUsageCollectorSimple();
                    inStatement.collectLValueUsage(usageCollector);
                    if (usageCollector.isUsed(iterable)) {
                        res.set(true);
                        return;
                    }
                }
                for (Op03SimpleStatement target : arg1.getTargets()) {
                    arg2.enqueue(target);
                }
            }
        });
        graphVisitor.process();
        if (res.get()) {
            return;
        }
        loop.replaceStatement(new ForIterStatement(whileStatement.getCombinedLoc(), blockIdentifier, sugarIter, iterSource, null));
        if (loopStart != realLoopStart) {
            if (hiddenIter) {
                loop.replaceTarget(realLoopStart, loopStart);
                realLoopStart.replaceSource(loop, loopStart);
                loopStart.addSource(loop);
                loopStart.addTarget(realLoopStart);
                Op03SimpleStatement endStm = loop.getTargets().get(1);
                loopStart.addTarget(endStm);
                endStm.addSource(loopStart);
                Misc.replaceHiddenIter(loopStart.getStatement(), sugariterWC.getMatch(), nextCall);
                statements.add(statements.indexOf(realLoopStart), loopStart);
            }
        } else if (hiddenIter) {
            Misc.replaceHiddenIter(loopStart.getStatement(), sugariterWC.getMatch(), nextCall);
        } else {
            loopStart.nopOut();
        }
        preceeding.nopOut();
    }

    public static void rewriteIteratorWhileLoops(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> loops = Functional.filter(statements, new TypeFilter<WhileStatement>(WhileStatement.class));
        for (Op03SimpleStatement loop : loops) {
            IterLoopRewriter.rewriteIteratorWhileLoop(loop, statements);
        }
    }
}

