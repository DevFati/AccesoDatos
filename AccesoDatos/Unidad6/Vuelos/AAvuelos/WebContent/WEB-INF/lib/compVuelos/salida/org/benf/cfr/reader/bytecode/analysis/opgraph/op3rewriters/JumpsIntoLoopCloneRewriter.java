/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.CompareByIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.DoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.WhileStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;

public class JumpsIntoLoopCloneRewriter {
    private int maxDepth;

    JumpsIntoLoopCloneRewriter(Options options) {
        this.maxDepth = (Integer)options.getOption(OptionsImpl.AGGRESSIVE_DO_COPY);
    }

    public void rewrite(List<Op03SimpleStatement> op03SimpleParseNodes, DecompilerComments comments) {
        List<Op03SimpleStatement> addThese = ListFactory.newList();
        for (Op03SimpleStatement stm : op03SimpleParseNodes) {
            Statement statement = stm.getStatement();
            if (statement instanceof DoStatement) {
                this.refactorDo(addThese, stm, ((DoStatement)statement).getBlockIdentifier());
                continue;
            }
            if (!(statement instanceof WhileStatement)) continue;
            this.refactorWhile(addThese, stm, ((WhileStatement)statement).getBlockIdentifier());
        }
        if (!addThese.isEmpty()) {
            op03SimpleParseNodes.addAll(addThese);
            comments.addComment(DecompilerComment.IMPOSSIBLE_LOOP_WITH_COPY);
            Cleaner.sortAndRenumberInPlace(op03SimpleParseNodes);
        }
    }

    private void refactorDo(List<Op03SimpleStatement> addThese, Op03SimpleStatement stm, BlockIdentifier ident) {
        Op03SimpleStatement firstContained = stm.getTargets().get(0);
        Op03SimpleStatement possLast = this.getPossLast(firstContained, ident);
        if (possLast == null) {
            return;
        }
        if (!(possLast.getStatement() instanceof WhileStatement)) {
            return;
        }
        WhileStatement whileStatement = (WhileStatement)possLast.getStatement();
        if (whileStatement.getBlockIdentifier() != ident) {
            return;
        }
        Op03SimpleStatement afterWhile = possLast.getTargets().get(0);
        Map<Op03SimpleStatement, Op03SimpleStatement> candidates = MapFactory.newOrderedMap();
        GraphVisitor<Op03SimpleStatement> gv = this.visitCandidates(ident, possLast, candidates);
        Set<Op03SimpleStatement> visited = SetFactory.newSet(gv.getVisitedNodes());
        for (Map.Entry<Op03SimpleStatement, Op03SimpleStatement> candidate : candidates.entrySet()) {
            Op03SimpleStatement caller = candidate.getKey();
            if (caller == stm) continue;
            Op03SimpleStatement target = candidate.getValue();
            IdentityHashMap<Op03SimpleStatement, Op03SimpleStatement> copies = new IdentityHashMap<Op03SimpleStatement, Op03SimpleStatement>();
            copies.put(caller, caller);
            InstrIndex idx = caller.getIndex();
            ConditionalExpression condition = whileStatement.getCondition();
            if (condition == null) {
                condition = new BooleanExpression(Literal.TRUE);
            }
            IfStatement newConditionStatement = new IfStatement(BytecodeLoc.TODO, condition);
            Op03SimpleStatement newCondition = new Op03SimpleStatement(caller.getBlockIdentifiers(), newConditionStatement, possLast.getSSAIdentifiers(), idx);
            Op03SimpleStatement jumpToAfterWhile = new Op03SimpleStatement(caller.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), caller.getSSAIdentifiers(), idx);
            copies.put(afterWhile, jumpToAfterWhile);
            copies.put(possLast, newCondition);
            Set<Op03SimpleStatement> addSources = SetFactory.newSet(newCondition, jumpToAfterWhile);
            List<Op03SimpleStatement> copy = this.copyBlock(stm, caller, target, possLast, visited, ident, addSources, copies);
            if (copy == null || copy.isEmpty()) {
                return;
            }
            idx = copy.get(copy.size() - 1).getIndex();
            idx = idx.justAfter();
            newCondition.setIndex(idx);
            idx = idx.justAfter();
            jumpToAfterWhile.setIndex(idx);
            for (Op03SimpleStatement copied : copies.values()) {
                if (!copied.getTargets().contains(newCondition)) continue;
                newCondition.addSource(copied);
            }
            boolean usedToFall = this.handleConditionalCaller(stm, caller, target, copies);
            if (afterWhile == firstContained) {
                afterWhile = stm;
            }
            if (usedToFall && jumpToAfterWhile.getSources().isEmpty()) {
                newConditionStatement.negateCondition();
                newCondition.addTarget(stm);
                stm.addSource(newCondition);
                newCondition.addTarget(afterWhile);
                afterWhile.addSource(newCondition);
                copy.add(newCondition);
            } else {
                newCondition.addTarget(jumpToAfterWhile);
                newCondition.addTarget(stm);
                stm.addSource(newCondition);
                jumpToAfterWhile.addSource(newCondition);
                jumpToAfterWhile.addTarget(afterWhile);
                afterWhile.addSource(jumpToAfterWhile);
                copy.add(newCondition);
                copy.add(jumpToAfterWhile);
            }
            this.nopPointlessCondition(newConditionStatement, newCondition);
            addThese.addAll(copy);
        }
    }

    private void refactorWhile(List<Op03SimpleStatement> addThese, Op03SimpleStatement stm, BlockIdentifier ident) {
        Op03SimpleStatement possLast = this.getPossLast(stm, ident);
        if (possLast == null) {
            return;
        }
        Map<Op03SimpleStatement, Op03SimpleStatement> candidates = MapFactory.newOrderedMap();
        GraphVisitor<Op03SimpleStatement> gv = this.visitCandidates(ident, possLast, candidates);
        Set<Op03SimpleStatement> visited = SetFactory.newSet(gv.getVisitedNodes());
        for (Map.Entry<Op03SimpleStatement, Op03SimpleStatement> candidate : candidates.entrySet()) {
            Op03SimpleStatement caller = candidate.getKey();
            if (caller == stm) continue;
            Op03SimpleStatement target = candidate.getValue();
            IdentityHashMap<Op03SimpleStatement, Op03SimpleStatement> copies = new IdentityHashMap<Op03SimpleStatement, Op03SimpleStatement>();
            copies.put(caller, caller);
            copies.put(stm, stm);
            List<Op03SimpleStatement> copy = this.copyBlock(stm, caller, target, possLast, visited, ident, SetFactory.<Op03SimpleStatement>newSet(), copies);
            if (copy == null || copy.isEmpty()) {
                return;
            }
            boolean usedToFall = this.handleConditionalCaller(stm, caller, target, copies);
            Op03SimpleStatement lastCopy = (Op03SimpleStatement)copies.get(possLast);
            stm.addSource(lastCopy);
            addThese.addAll(copy);
        }
    }

    private boolean handleConditionalCaller(Op03SimpleStatement stm, Op03SimpleStatement caller, Op03SimpleStatement target, Map<Op03SimpleStatement, Op03SimpleStatement> copies) {
        Op03SimpleStatement targetCopy = copies.get(target);
        target.removeSource(caller);
        boolean usedToFall = false;
        if (caller.getStatement() instanceof IfStatement) {
            if (caller.getTargets().get(0) == stm) {
                usedToFall = true;
            }
            caller.removeGotoTarget(target);
            caller.getTargets().add(0, targetCopy);
            ((IfStatement)caller.getStatement()).negateCondition();
        } else {
            caller.replaceTarget(target, targetCopy);
        }
        return usedToFall;
    }

    private void nopPointlessCondition(IfStatement newConditionStatement, Op03SimpleStatement newCondition) {
        Op03SimpleStatement t1;
        Op03SimpleStatement s2;
        List<Op03SimpleStatement> targets = newCondition.getTargets();
        Op03SimpleStatement t0 = targets.get(0);
        Op03SimpleStatement s1 = Misc.followNopGotoChain(t0, false, true);
        if (s1 == (s2 = Misc.followNopGotoChain(t1 = targets.get(1), false, true)) && null != newConditionStatement.getCondition().getComputedLiteral(MapFactory.<LValue, Literal>newMap())) {
            t0.removeSource(newCondition);
            t1.removeSource(newCondition);
            newCondition.getTargets().clear();
            newCondition.getTargets().add(s1);
            s1.addSource(newCondition);
            newCondition.replaceStatement(new Nop());
        }
    }

    private Op03SimpleStatement getPossLast(Op03SimpleStatement stm, BlockIdentifier ident) {
        List<Op03SimpleStatement> lasts = stm.getSources();
        Collections.sort(lasts, new CompareByIndex(false));
        Op03SimpleStatement possLast = lasts.get(0);
        if (!possLast.getBlockIdentifiers().contains(ident)) {
            return null;
        }
        Op03SimpleStatement linearlyNext = possLast.getLinearlyNext();
        if (linearlyNext != null && linearlyNext.getBlockIdentifiers().contains(ident)) {
            return null;
        }
        return possLast;
    }

    private GraphVisitor<Op03SimpleStatement> visitCandidates(final BlockIdentifier blockIdent, Op03SimpleStatement possLast, final Map<Op03SimpleStatement, Op03SimpleStatement> candidates) {
        final Map depthMap = MapFactory.newIdentityMap();
        depthMap.put(possLast, 0);
        GraphVisitorDFS<Op03SimpleStatement> gv = new GraphVisitorDFS<Op03SimpleStatement>(possLast, new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

            @Override
            public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                int depth = (Integer)depthMap.get(arg1);
                if (depth > JumpsIntoLoopCloneRewriter.this.maxDepth) {
                    return;
                }
                for (Op03SimpleStatement source : arg1.getSources()) {
                    if (source.getBlockIdentifiers().contains(blockIdent)) {
                        depthMap.put(source, depth + 1);
                        arg2.enqueue(source);
                        continue;
                    }
                    candidates.put(source, arg1);
                }
            }
        });
        gv.process();
        return gv;
    }

    private List<Op03SimpleStatement> copyBlock(Op03SimpleStatement stm, Op03SimpleStatement caller, Op03SimpleStatement start, Op03SimpleStatement end, final Set<Op03SimpleStatement> valid, final BlockIdentifier containedIn, Set<Op03SimpleStatement> addSources, final Map<Op03SimpleStatement, Op03SimpleStatement> orig2copy) {
        Op03SimpleStatement copy;
        final List<Op03SimpleStatement> copyThese = ListFactory.newList();
        final boolean[] failed = new boolean[]{false};
        GraphVisitorDFS<Op03SimpleStatement> gv = new GraphVisitorDFS<Op03SimpleStatement>(start, new BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>>(){

            @Override
            public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
                if (orig2copy.containsKey(arg1)) {
                    return;
                }
                if (valid.contains(arg1)) {
                    copyThese.add(arg1);
                    arg2.enqueue(arg1.getTargets());
                    return;
                }
                if (arg1.getBlockIdentifiers().contains(containedIn)) {
                    failed[0] = true;
                }
            }
        });
        gv.process();
        if (failed[0]) {
            return null;
        }
        Collections.sort(copyThese, new CompareByIndex());
        List<Op03SimpleStatement> copies = ListFactory.newList();
        Set<BlockIdentifier> expectedBlocks = end.getBlockIdentifiers();
        CloneHelper cloneHelper = new CloneHelper();
        InstrIndex idx = caller.getIndex().justAfter();
        for (Op03SimpleStatement copyThis : copyThese) {
            Statement s = copyThis.getStatement();
            Set<BlockIdentifier> b = copyThis.getBlockIdentifiers();
            if (!b.equals(expectedBlocks)) {
                return null;
            }
            if (s instanceof GotoStatement && copyThis.getTargets().contains(stm)) {
                s = new GotoStatement(BytecodeLoc.TODO);
            }
            copy = new Op03SimpleStatement(caller.getBlockIdentifiers(), (Statement)s.deepClone(cloneHelper), copyThis.getSSAIdentifiers(), idx);
            orig2copy.put(copyThis, copy);
            copies.add(copy);
            idx = idx.justAfter();
        }
        for (Op03SimpleStatement copyThis : copyThese) {
            List<Op03SimpleStatement> sources = copyThis.getSources();
            List<Op03SimpleStatement> targets = copyThis.getTargets();
            copy = orig2copy.get(copyThis);
            sources = this.copyST(sources, orig2copy, false);
            if ((targets = this.copyST(targets, orig2copy, true)) == null || sources == null) {
                return null;
            }
            copy.getSources().addAll(sources);
            copy.getTargets().addAll(targets);
            for (Op03SimpleStatement target : targets) {
                if (!addSources.contains(target)) continue;
                target.addSource(copy);
            }
        }
        return copies;
    }

    private List<Op03SimpleStatement> copyST(List<Op03SimpleStatement> original, Map<Op03SimpleStatement, Op03SimpleStatement> replacements, boolean failIfMissing) {
        List<Op03SimpleStatement> res = ListFactory.newList();
        for (Op03SimpleStatement st : original) {
            Op03SimpleStatement repl = replacements.get(st);
            if (repl == null) {
                if (!failIfMissing) continue;
                return null;
            }
            res.add(repl);
        }
        return res;
    }
}

