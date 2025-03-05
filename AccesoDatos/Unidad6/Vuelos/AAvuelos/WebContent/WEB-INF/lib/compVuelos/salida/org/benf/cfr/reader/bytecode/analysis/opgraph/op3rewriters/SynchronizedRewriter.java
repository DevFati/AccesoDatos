/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Op03Rewriters;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.FinallyStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.MonitorExitStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ThrowStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

class SynchronizedRewriter {
    SynchronizedRewriter() {
    }

    static void removeSynchronizedCatchBlocks(Options options, List<Op03SimpleStatement> in) {
        if (!((Boolean)options.getOption(OptionsImpl.TIDY_MONITORS)).booleanValue()) {
            return;
        }
        List<Op03SimpleStatement> catchStarts = Functional.filter(in, new FindBlockStarts(BlockType.CATCHBLOCK));
        if (catchStarts.isEmpty()) {
            return;
        }
        boolean effect = false;
        for (Op03SimpleStatement catchStart : catchStarts) {
            effect = SynchronizedRewriter.removeSynchronizedCatchBlock(catchStart, in) || effect;
        }
        if (effect) {
            Op03Rewriters.removePointlessJumps(in);
        }
    }

    private static boolean removeSynchronizedCatchBlock(Op03SimpleStatement start, List<Op03SimpleStatement> statements) {
        Op03SimpleStatement rethrow;
        Op03SimpleStatement variableAss;
        Op03SimpleStatement monitorExit;
        BlockIdentifier block = start.getFirstStatementInThisBlock();
        if (start.getSources().size() != 1) {
            return false;
        }
        Op03SimpleStatement catchStatementContainer = start.getSources().get(0);
        if (catchStatementContainer.getSources().size() != 1) {
            return false;
        }
        Statement catchOrFinally = catchStatementContainer.getStatement();
        boolean isFinally = false;
        if (catchOrFinally instanceof CatchStatement) {
            CatchStatement catchStatement = (CatchStatement)catchStatementContainer.getStatement();
            List<ExceptionGroup.Entry> exceptions = catchStatement.getExceptions();
            if (exceptions.size() != 1) {
                return false;
            }
            ExceptionGroup.Entry exception = exceptions.get(0);
            if (!exception.isJustThrowable()) {
                return false;
            }
        } else if (catchOrFinally instanceof FinallyStatement) {
            isFinally = true;
        } else {
            return false;
        }
        if (!SynchronizedRewriter.verifyLinearBlock(start, block, 2)) {
            return false;
        }
        if (isFinally) {
            monitorExit = start;
            variableAss = null;
            rethrow = null;
        } else {
            variableAss = start;
            monitorExit = start.getTargets().get(0);
            if (monitorExit.getTargets().size() != 1) {
                return false;
            }
            rethrow = monitorExit.getTargets().get(0);
        }
        WildcardMatch wildcardMatch = new WildcardMatch();
        if (!isFinally && !wildcardMatch.match(new AssignmentSimple(BytecodeLoc.NONE, wildcardMatch.getLValueWildCard("var"), wildcardMatch.getExpressionWildCard("e")), variableAss.getStatement())) {
            return false;
        }
        if (!wildcardMatch.match(new MonitorExitStatement(BytecodeLoc.NONE, wildcardMatch.getExpressionWildCard("lock")), monitorExit.getStatement())) {
            return false;
        }
        if (!isFinally && !wildcardMatch.match(new ThrowStatement(BytecodeLoc.NONE, new LValueExpression(wildcardMatch.getLValueWildCard("var"))), rethrow.getStatement())) {
            return false;
        }
        Op03SimpleStatement tryStatementContainer = catchStatementContainer.getSources().get(0);
        if (isFinally) {
            MonitorExitStatement monitorExitStatement = (MonitorExitStatement)monitorExit.getStatement();
            TryStatement tryStatement = (TryStatement)tryStatementContainer.getStatement();
            tryStatement.addExitMutex(monitorExitStatement.getMonitor());
        }
        tryStatementContainer.removeTarget(catchStatementContainer);
        catchStatementContainer.removeSource(tryStatementContainer);
        catchStatementContainer.nopOut();
        if (!isFinally) {
            variableAss.nopOut();
        }
        monitorExit.nopOut();
        if (!isFinally) {
            for (Op03SimpleStatement target : rethrow.getTargets()) {
                target.removeSource(rethrow);
                rethrow.removeTarget(target);
            }
            rethrow.nopOut();
        }
        if (tryStatementContainer.getTargets().size() == 1 && !isFinally) {
            TryStatement tryStatement = (TryStatement)tryStatementContainer.getStatement();
            BlockIdentifier tryBlock = tryStatement.getBlockIdentifier();
            tryStatementContainer.nopOut();
            for (Op03SimpleStatement statement : statements) {
                statement.getBlockIdentifiers().remove(tryBlock);
            }
        }
        return true;
    }

    private static boolean verifyLinearBlock(Op03SimpleStatement current, BlockIdentifier block, int num) {
        while (num >= 0) {
            if (num > 0) {
                if (current.getStatement() instanceof Nop && current.getTargets().size() == 0) break;
                if (current.getTargets().size() != 1) {
                    return false;
                }
                if (!current.getBlockIdentifiers().contains(block)) {
                    return false;
                }
                current = current.getTargets().get(0);
            } else if (!current.getBlockIdentifiers().contains(block)) {
                return false;
            }
            --num;
        }
        for (Op03SimpleStatement target : current.getTargets()) {
            if (!target.getBlockIdentifiers().contains(block)) continue;
            return false;
        }
        return true;
    }

    private static final class FindBlockStarts
    implements Predicate<Op03SimpleStatement> {
        private final BlockType blockType;

        FindBlockStarts(BlockType blockType) {
            this.blockType = blockType;
        }

        @Override
        public boolean test(Op03SimpleStatement in) {
            BlockIdentifier blockIdentifier = in.getFirstStatementInThisBlock();
            if (blockIdentifier == null) {
                return false;
            }
            return blockIdentifier.getBlockType() == this.blockType;
        }
    }
}

