/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.ExactTypeFilter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Op03Rewriters;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CommentStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfExitingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;

public class PushThroughGoto {
    public static List<Op03SimpleStatement> pushThroughGoto(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> pathtests = Functional.filter(statements, new ExactTypeFilter<GotoStatement>(GotoStatement.class));
        boolean success = false;
        for (Op03SimpleStatement gotostm : pathtests) {
            if (!gotostm.getTargets().get(0).getIndex().isBackJumpTo(gotostm) || !PushThroughGoto.pushThroughGoto(gotostm, statements)) continue;
            success = true;
        }
        if (success) {
            statements = Cleaner.sortAndRenumber(statements);
            Op03Rewriters.rewriteNegativeJumps(statements, false);
            Op03Rewriters.rewriteNegativeJumps(statements, false);
        }
        return statements;
    }

    private static boolean pushThroughGoto(Op03SimpleStatement forwardGoto, List<Op03SimpleStatement> statements) {
        boolean abortNext;
        Set<BlockIdentifier> tgtLoopBlocks;
        if (forwardGoto.getSources().size() != 1) {
            return false;
        }
        Op03SimpleStatement tgt = forwardGoto.getTargets().get(0);
        int idx = statements.indexOf(tgt);
        if (idx == 0) {
            return false;
        }
        Op03SimpleStatement before = statements.get(idx - 1);
        if (tgt.getSources().contains(before)) {
            return false;
        }
        if (tgt.getSources().size() != 1) {
            return false;
        }
        InstrIndex beforeTgt = tgt.getIndex().justBefore();
        class IsLoopBlock
        implements Predicate<BlockIdentifier> {
            IsLoopBlock() {
            }

            @Override
            public boolean test(BlockIdentifier in) {
                BlockType blockType = in.getBlockType();
                switch (blockType) {
                    case WHILELOOP: 
                    case DOLOOP: {
                        return true;
                    }
                }
                return false;
            }
        }
        IsLoopBlock isLoopBlock = new IsLoopBlock();
        Set<BlockIdentifier> beforeLoopBlocks = SetFactory.newSet(Functional.filterSet(before.getBlockIdentifiers(), isLoopBlock));
        if (!beforeLoopBlocks.equals(tgtLoopBlocks = SetFactory.newSet(Functional.filterSet(tgt.getBlockIdentifiers(), isLoopBlock)))) {
            return false;
        }
        class IsExceptionBlock
        implements Predicate<BlockIdentifier> {
            IsExceptionBlock() {
            }

            @Override
            public boolean test(BlockIdentifier in) {
                BlockType blockType = in.getBlockType();
                switch (blockType) {
                    case TRYBLOCK: 
                    case SWITCH: 
                    case CATCHBLOCK: 
                    case CASE: {
                        return true;
                    }
                }
                return false;
            }
        }
        IsExceptionBlock exceptionFilter = new IsExceptionBlock();
        Set<BlockIdentifier> exceptionBlocks = SetFactory.newSet(Functional.filterSet(tgt.getBlockIdentifiers(), exceptionFilter));
        int nextCandidateIdx = statements.indexOf(forwardGoto) - 1;
        Op03SimpleStatement lastTarget = tgt;
        Set seen = SetFactory.newSet();
        boolean success = false;
        do {
            Op03SimpleStatement tryMoveThis;
            if (!PushThroughGoto.moveable((tryMoveThis = forwardGoto.getSources().get(0)).getStatement())) {
                return success;
            }
            if (!seen.add(tryMoveThis)) {
                return success;
            }
            if (statements.get(nextCandidateIdx) != tryMoveThis) {
                return success;
            }
            if (tryMoveThis.getTargets().size() != 1) {
                return success;
            }
            abortNext = tryMoveThis.getSources().size() != 1;
            Set<BlockIdentifier> moveEB = SetFactory.newSet(Functional.filterSet(tryMoveThis.getBlockIdentifiers(), exceptionFilter));
            if (!moveEB.equals(exceptionBlocks)) {
                return success;
            }
            forwardGoto.getSources().clear();
            for (Op03SimpleStatement beforeTryMove : tryMoveThis.getSources()) {
                beforeTryMove.replaceTarget(tryMoveThis, forwardGoto);
                forwardGoto.getSources().add(beforeTryMove);
            }
            tryMoveThis.getSources().clear();
            tryMoveThis.getSources().add(forwardGoto);
            forwardGoto.replaceTarget(lastTarget, tryMoveThis);
            tryMoveThis.replaceTarget(forwardGoto, lastTarget);
            lastTarget.replaceSource(forwardGoto, tryMoveThis);
            tryMoveThis.setIndex(beforeTgt);
            beforeTgt = beforeTgt.justBefore();
            tryMoveThis.getBlockIdentifiers().clear();
            tryMoveThis.getBlockIdentifiers().addAll(lastTarget.getBlockIdentifiers());
            lastTarget = tryMoveThis;
            --nextCandidateIdx;
            success = true;
        } while (!abortNext);
        return true;
    }

    private static boolean moveable(Statement statement) {
        Class<?> clazz = statement.getClass();
        if (clazz == Nop.class) {
            return true;
        }
        if (clazz == AssignmentSimple.class) {
            return true;
        }
        if (clazz == CommentStatement.class) {
            return true;
        }
        if (clazz == ExpressionStatement.class) {
            return true;
        }
        return clazz == IfExitingStatement.class;
    }
}

