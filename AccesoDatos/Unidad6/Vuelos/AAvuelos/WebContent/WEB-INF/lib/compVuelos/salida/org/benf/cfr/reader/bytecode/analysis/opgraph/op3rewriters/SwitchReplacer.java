/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.CompareByIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ComparisonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CaseStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CommentStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.RawSwitchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.SwitchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.bytecode.opcode.DecodedSwitch;
import org.benf.cfr.reader.bytecode.opcode.DecodedSwitchEntry;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.CannotPerformDecode;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;
import org.benf.cfr.reader.util.output.Dumpable;

public class SwitchReplacer {
    public static void replaceRawSwitches(Method method, List<Op03SimpleStatement> in, BlockIdentifierFactory blockIdentifierFactory, Options options, DecompilerComments comments, BytecodeMeta bytecodeMeta) {
        List<Op03SimpleStatement> switchStatements = Functional.filter(in, new TypeFilter<RawSwitchStatement>(RawSwitchStatement.class));
        List<Op03SimpleStatement> switches = ListFactory.newList();
        for (Op03SimpleStatement switchStatement : switchStatements) {
            Op03SimpleStatement switchToProcess = SwitchReplacer.replaceRawSwitch(method, switchStatement, in, blockIdentifierFactory, options);
            if (switchToProcess == null) continue;
            switches.add(switchToProcess);
        }
        Collections.sort(in, new CompareByIndex());
        boolean pullCodeIntoCase = (Boolean)options.getOption(OptionsImpl.PULL_CODE_CASE);
        boolean allowMalformedSwitch = options.getOption(OptionsImpl.ALLOW_MALFORMED_SWITCH) == Troolean.TRUE;
        for (Op03SimpleStatement switchStatement : switches) {
            switchStatement = SwitchReplacer.examineSwitchContiguity(switchStatement, in, pullCodeIntoCase, allowMalformedSwitch, comments, bytecodeMeta);
            SwitchReplacer.moveJumpsToCaseStatements(switchStatement);
            SwitchReplacer.moveJumpsToTerminalIfEmpty(switchStatement, in);
        }
    }

    public static List<Op03SimpleStatement> rewriteDuff(List<Op03SimpleStatement> statements, VariableFactory vf, DecompilerComments decompilerComments, Options options) {
        List<Op03SimpleStatement> switchStatements = Functional.filter(statements, new TypeFilter<SwitchStatement>(SwitchStatement.class));
        boolean effect = false;
        for (Op03SimpleStatement switchStatement : switchStatements) {
            if (!SwitchReplacer.rewriteDuff(switchStatement, statements, vf, decompilerComments)) continue;
            effect = true;
        }
        if (effect) {
            decompilerComments.addComment(DecompilerComment.DUFF_HANDLING);
            statements = Cleaner.sortAndRenumber(statements);
        }
        return statements;
    }

    private static Op03SimpleStatement replaceRawSwitch(Method method, Op03SimpleStatement swatch, List<Op03SimpleStatement> in, BlockIdentifierFactory blockIdentifierFactory, Options options) {
        List<Op03SimpleStatement> targets = swatch.getTargets();
        RawSwitchStatement switchStatement = (RawSwitchStatement)swatch.getStatement();
        DecodedSwitch switchData = switchStatement.getSwitchData();
        BlockIdentifier switchBlockIdentifier = blockIdentifierFactory.getNextBlockIdentifier(BlockType.SWITCH);
        Op03SimpleStatement oneTarget = targets.get(0);
        boolean mismatch = false;
        for (int x = 1; x < targets.size(); ++x) {
            Op03SimpleStatement target = targets.get(x);
            if (target == oneTarget) continue;
            mismatch = true;
            break;
        }
        if (!mismatch) {
            int idx = in.indexOf(swatch);
            if (idx + 1 >= in.size() || oneTarget != in.get(idx + 1)) {
                swatch.replaceStatement(new GotoStatement(BytecodeLoc.TODO));
                return null;
            }
            swatch.replaceStatement(switchStatement.getSwitchStatement(switchBlockIdentifier));
            BlockIdentifier defBlock = blockIdentifierFactory.getNextBlockIdentifier(BlockType.CASE);
            Op03SimpleStatement defStm = new Op03SimpleStatement(swatch.getBlockIdentifiers(), new CaseStatement(BytecodeLoc.TODO, ListFactory.<Expression>newList(), switchStatement.getSwitchOn().getInferredJavaType(), switchBlockIdentifier, defBlock), swatch.getIndex().justAfter());
            swatch.replaceTarget(oneTarget, defStm);
            oneTarget.replaceSource(swatch, defStm);
            defStm.addSource(swatch);
            defStm.addTarget(oneTarget);
            defStm.getBlockIdentifiers().add(switchBlockIdentifier);
            in.add(defStm);
            return null;
        }
        if (options.getOption(OptionsImpl.FORCE_TOPSORT_EXTRA) == Troolean.TRUE) {
            SwitchReplacer.tryInlineRawSwitchContent(swatch, in);
        }
        List<DecodedSwitchEntry> entries = switchData.getJumpTargets();
        InferredJavaType caseType = switchStatement.getSwitchOn().getInferredJavaType();
        Map firstPrev = MapFactory.newMap();
        InstrIndex nextIntermed = swatch.getIndex().justAfter();
        int len = targets.size();
        for (int x = 0; x < len; ++x) {
            Op03SimpleStatement target = targets.get(x);
            InstrIndex tindex = target.getIndex();
            if (firstPrev.containsKey(tindex)) {
                target = (Op03SimpleStatement)firstPrev.get(tindex);
            }
            List<Expression> expression = ListFactory.newList();
            List<Integer> vals = entries.get(x).getValue();
            for (Integer val : vals) {
                if (val == null) continue;
                expression.add(new Literal(TypedLiteral.getInt(val)));
            }
            Set<BlockIdentifier> blocks = SetFactory.newSet(target.getBlockIdentifiers());
            blocks.add(switchBlockIdentifier);
            BlockIdentifier caseIdentifier = blockIdentifierFactory.getNextBlockIdentifier(BlockType.CASE);
            Op03SimpleStatement caseStatement = new Op03SimpleStatement(blocks, new CaseStatement(BytecodeLoc.TODO, expression, caseType, switchBlockIdentifier, caseIdentifier), target.getIndex().justBefore());
            Iterator<Op03SimpleStatement> iterator = target.getSources().iterator();
            while (iterator.hasNext()) {
                Op03SimpleStatement source = iterator.next();
                if (swatch.getIndex().isBackJumpTo(source) || source.getIndex().isBackJumpTo(target)) continue;
                source.replaceTarget(target, caseStatement);
                caseStatement.addSource(source);
                iterator.remove();
            }
            if (caseStatement.getSources().isEmpty()) {
                caseStatement.setIndex(nextIntermed);
                nextIntermed = nextIntermed.justAfter();
                Op03SimpleStatement intermediateGoto = new Op03SimpleStatement(blocks, new GotoStatement(BytecodeLoc.TODO), nextIntermed);
                nextIntermed = nextIntermed.justAfter();
                intermediateGoto.addSource(caseStatement);
                intermediateGoto.addTarget(target);
                caseStatement.addTarget(intermediateGoto);
                target.replaceSource(swatch, intermediateGoto);
                swatch.replaceTarget(target, caseStatement);
                caseStatement.addSource(swatch);
                in.add(caseStatement);
                in.add(intermediateGoto);
                continue;
            }
            target.addSource(caseStatement);
            caseStatement.addTarget(target);
            in.add(caseStatement);
            firstPrev.put(tindex, caseStatement);
        }
        Cleaner.sortAndRenumberInPlace(in);
        SwitchReplacer.buildSwitchCases(swatch, targets, switchBlockIdentifier, in, false);
        swatch.replaceStatement(switchStatement.getSwitchStatement(switchBlockIdentifier));
        Collections.sort(swatch.getTargets(), new CompareByIndex());
        return swatch;
    }

    private static void buildSwitchCases(Op03SimpleStatement swatch, List<Op03SimpleStatement> targets, BlockIdentifier switchBlockIdentifier, List<Op03SimpleStatement> in, boolean forcedOrder) {
        targets = ListFactory.newList(targets);
        Collections.sort(targets, new CompareByIndex());
        Set caseIdentifiers = SetFactory.newSet();
        Set<Op03SimpleStatement> caseTargets = SetFactory.newSet(targets);
        Map lastStatementBefore = MapFactory.newMap();
        for (Op03SimpleStatement target : targets) {
            CaseStatement caseStatement = (CaseStatement)target.getStatement();
            BlockIdentifier caseBlock = caseStatement.getCaseBlock();
            if (!caseStatement.isDefault()) {
                target.markBlock(switchBlockIdentifier);
            }
            NodeReachable nodeReachable = new NodeReachable(caseTargets, target, swatch, forcedOrder);
            GraphVisitorDFS<Op03SimpleStatement> gv = new GraphVisitorDFS<Op03SimpleStatement>(target, nodeReachable);
            gv.process();
            List<Op03SimpleStatement> backReachable = Functional.filter(nodeReachable.reaches, new Misc.IsForwardJumpTo(target.getIndex()));
            if (backReachable.isEmpty() || backReachable.size() != 1) continue;
            Op03SimpleStatement backTarget = backReachable.get(0);
            boolean contiguous = SwitchReplacer.blockIsContiguous(in, target, nodeReachable.inBlock);
            if (target.getSources().size() != 1) {
                if (!contiguous) continue;
                for (Op03SimpleStatement reachable : nodeReachable.inBlock) {
                    reachable.markBlock(switchBlockIdentifier);
                    if (caseTargets.contains(reachable) || SetUtil.hasIntersection(reachable.getBlockIdentifiers(), caseIdentifiers)) continue;
                    reachable.markBlock(caseBlock);
                }
                continue;
            }
            if (!contiguous) continue;
            InstrIndex prev = (InstrIndex)lastStatementBefore.get(backTarget);
            if (prev == null) {
                prev = backTarget.getIndex().justBefore();
            }
            int idx = in.indexOf(target) + nodeReachable.inBlock.size() - 1;
            int i = 0;
            int len = nodeReachable.inBlock.size();
            while (i < len) {
                in.get(idx).setIndex(prev);
                prev = prev.justBefore();
                ++i;
                --idx;
            }
            lastStatementBefore.put(backTarget, prev);
        }
    }

    public static void rebuildSwitches(List<Op03SimpleStatement> statements, Options options, DecompilerComments comments, BytecodeMeta bytecodeMeta) {
        SwitchStatement switchStatementInr;
        List<Op03SimpleStatement> switchStatements = Functional.filter(statements, new TypeFilter<SwitchStatement>(SwitchStatement.class));
        for (Op03SimpleStatement switchStatement : switchStatements) {
            switchStatementInr = (SwitchStatement)switchStatement.getStatement();
            BlockIdentifier switchBlock = switchStatementInr.getSwitchBlock();
            int len = statements.size();
            for (int idx = statements.indexOf(switchStatement.getTargets().get(0)); idx < len; ++idx) {
                Op03SimpleStatement statement = statements.get(idx);
                if (statement.getBlockIdentifiers().contains(switchBlock)) continue;
                for (Op03SimpleStatement src : statement.getSources()) {
                    Statement srcStatement;
                    if (!src.getBlockIdentifiers().contains(switchBlock) || !((srcStatement = src.getStatement()) instanceof GotoStatement) || ((GotoStatement)srcStatement).getJumpType() != JumpType.BREAK) continue;
                    ((GotoStatement)srcStatement).setJumpType(JumpType.GOTO);
                }
                break;
            }
            Set allBlocks = SetFactory.newSet();
            allBlocks.add(switchBlock);
            for (Op03SimpleStatement target : switchStatement.getTargets()) {
                Statement stmTgt = target.getStatement();
                if (!(stmTgt instanceof CaseStatement)) continue;
                allBlocks.add(((CaseStatement)stmTgt).getCaseBlock());
            }
            for (Op03SimpleStatement stm : statements) {
                stm.getBlockIdentifiers().removeAll(allBlocks);
            }
        }
        for (Op03SimpleStatement switchStatement : switchStatements) {
            switchStatementInr = (SwitchStatement)switchStatement.getStatement();
            SwitchReplacer.buildSwitchCases(switchStatement, switchStatement.getTargets(), switchStatementInr.getSwitchBlock(), statements, true);
        }
        boolean pullCodeIntoCase = (Boolean)options.getOption(OptionsImpl.PULL_CODE_CASE);
        boolean allowMalformedSwitch = options.getOption(OptionsImpl.ALLOW_MALFORMED_SWITCH) == Troolean.TRUE;
        for (Op03SimpleStatement switchStatement : switchStatements) {
            SwitchReplacer.examineSwitchContiguity(switchStatement, statements, pullCodeIntoCase, allowMalformedSwitch, comments, bytecodeMeta);
            SwitchReplacer.moveJumpsToTerminalIfEmpty(switchStatement, statements);
        }
    }

    private static boolean blockIsContiguous(List<Op03SimpleStatement> in, Op03SimpleStatement start, Set<Op03SimpleStatement> blockContent) {
        int idx = in.indexOf(start);
        int len = blockContent.size();
        if (idx + blockContent.size() > in.size()) {
            return false;
        }
        int found = 1;
        while (found < len) {
            Op03SimpleStatement next = in.get(idx);
            if (!blockContent.contains(next)) {
                return false;
            }
            ++found;
            ++idx;
        }
        return true;
    }

    private static Op03SimpleStatement examineSwitchContiguity(Op03SimpleStatement switchStatement, List<Op03SimpleStatement> statements, boolean pullCodeIntoCase, boolean allowMalformedSwitch, DecompilerComments comments, BytecodeMeta bytecodeMeta) {
        Op03SimpleStatement lastInThis;
        int breakTarget;
        Dumpable statement;
        Set forwardTargets = SetFactory.newSet();
        List<Op03SimpleStatement> targets = ListFactory.newList(switchStatement.getTargets());
        Collections.sort(targets, new CompareByIndex());
        Op03SimpleStatement firstCase = targets.get(0);
        int idxFirstCase = statements.indexOf(firstCase);
        if (idxFirstCase != statements.indexOf(switchStatement) + 1 && (switchStatement = SwitchReplacer.moveSwitch(switchStatement, statements, firstCase, idxFirstCase, targets)) == null) {
            throw new ConfusedCFRException("First case is not immediately after switch.");
        }
        BlockIdentifier switchBlock = ((SwitchStatement)switchStatement.getStatement()).getSwitchBlock();
        int indexLastInLastBlock = 0;
        for (int x = 0; x < targets.size() - 1; ++x) {
            Op03SimpleStatement lastStatement;
            int indexLastInThis;
            Op03SimpleStatement thisCase = targets.get(x);
            Op03SimpleStatement nextCase = targets.get(x + 1);
            int indexThisCase = statements.indexOf(thisCase);
            int indexNextCase = statements.indexOf(nextCase);
            InstrIndex nextCaseIndex = nextCase.getIndex();
            Statement maybeCaseStatement = thisCase.getStatement();
            if (!(maybeCaseStatement instanceof CaseStatement)) continue;
            CaseStatement caseStatement = (CaseStatement)maybeCaseStatement;
            BlockIdentifier caseBlock = caseStatement.getCaseBlock();
            indexLastInLastBlock = indexLastInThis = Misc.getFarthestReachableInRange(statements, indexThisCase, indexNextCase);
            for (int y = indexThisCase + 1; y <= indexLastInThis; ++y) {
                statement = statements.get(y);
                statement.markBlock(caseBlock);
                statement.markBlock(switchBlock);
                if (!statement.getJumpType().isUnknown()) continue;
                for (Op03SimpleStatement innerTarget : statement.getTargets()) {
                    if (!nextCaseIndex.isBackJumpFrom(innerTarget = Misc.followNopGoto(innerTarget, false, false)) || innerTarget.getBlockIdentifiers().contains(switchBlock)) continue;
                    forwardTargets.add(innerTarget);
                }
            }
            if (!pullCodeIntoCase || (lastStatement = statements.get(indexLastInThis)).getStatement().getClass() != GotoStatement.class) continue;
            Set<BlockIdentifier> others = SetFactory.newSet(caseBlock, switchBlock);
            Op03SimpleStatement last = lastStatement;
            Op03SimpleStatement tgt = last.getTargets().get(0);
            InstrIndex moveTo = last.getIndex().justAfter();
            while (tgt.getSources().size() == 1 && tgt.getTargets().size() == 1 && SetUtil.difference(lastStatement.getBlockIdentifiers(), tgt.getBlockIdentifiers()).equals(others)) {
                tgt.setIndex(moveTo);
                moveTo = moveTo.justAfter();
                tgt.getBlockIdentifiers().addAll(others);
                last = tgt;
                tgt = last.getTargets().get(0);
            }
            if (last == lastStatement || last.getStatement().getClass() == GotoStatement.class) continue;
            Op03SimpleStatement newGoto = new Op03SimpleStatement(last.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), moveTo);
            Op03SimpleStatement originalTgt = last.getTargets().get(0);
            last.replaceTarget(originalTgt, newGoto);
            originalTgt.replaceSource(last, newGoto);
            newGoto.addTarget(originalTgt);
            newGoto.addSource(last);
            statements.add(newGoto);
        }
        Op03SimpleStatement lastCase = targets.get(targets.size() - 1);
        int indexLastCase = statements.indexOf(lastCase);
        BlockIdentifier caseBlock = null;
        int indexLastInThis = 0;
        boolean retieEnd = false;
        if (!forwardTargets.isEmpty()) {
            List lstFwdTargets = ListFactory.newList(forwardTargets);
            Collections.sort(lstFwdTargets, new CompareByIndex());
            Op03SimpleStatement afterCaseGuess = (Op03SimpleStatement)lstFwdTargets.get(0);
            int indexAfterCase = statements.indexOf(afterCaseGuess);
            CaseStatement caseStatement = (CaseStatement)lastCase.getStatement();
            caseBlock = caseStatement.getCaseBlock();
            try {
                indexLastInThis = Misc.getFarthestReachableInRange(statements, indexLastCase, indexAfterCase);
            }
            catch (CannotPerformDecode e) {
                forwardTargets.clear();
            }
            if (indexLastInThis != indexAfterCase - 1) {
                retieEnd = true;
            }
        }
        if (forwardTargets.isEmpty()) {
            for (int y = idxFirstCase; y <= indexLastInLastBlock; ++y) {
                Op03SimpleStatement statement2 = statements.get(y);
                statement2.markBlock(switchBlock);
            }
            if (indexLastCase != indexLastInLastBlock + 1) {
                Op03SimpleStatement lastInBlock = statements.get(indexLastInLastBlock);
                Op03SimpleStatement target = statements.get(indexLastCase);
                boolean handled = false;
                Statement targetStatement = target.getStatement();
                if (targetStatement instanceof CaseStatement) {
                    caseBlock = ((CaseStatement)targetStatement).getCaseBlock();
                    Op03SimpleStatement t2 = target.getTargets().get(0);
                    statement = t2.getStatement();
                    if (statement instanceof ReturnStatement || statement instanceof GotoStatement) {
                        Op03SimpleStatement dupCase = new Op03SimpleStatement(switchStatement.getBlockIdentifiers(), targetStatement, lastInBlock.getIndex().justAfter());
                        indexLastCase = indexLastInLastBlock + 1;
                        statements.add(indexLastCase, dupCase);
                        target.removeSource(switchStatement);
                        target.nopOut();
                        switchStatement.replaceTarget(target, dupCase);
                        dupCase.addSource(switchStatement);
                        Op03SimpleStatement dupStm = new Op03SimpleStatement(dupCase.getBlockIdentifiers(), (Statement)statement, dupCase.getIndex().justAfter());
                        statements.add(indexLastCase + 1, dupStm);
                        dupCase.addTarget(dupStm);
                        dupStm.addSource(dupCase);
                        dupStm.getBlockIdentifiers().addAll(Arrays.asList(caseBlock, switchBlock));
                        lastCase = dupCase;
                        for (Op03SimpleStatement tgt : t2.getTargets()) {
                            dupStm.addTarget(tgt);
                            tgt.addSource(dupStm);
                        }
                        handled = true;
                    }
                }
                if (!handled) {
                    if (allowMalformedSwitch) {
                        comments.addComment(DecompilerComment.MALFORMED_SWITCH);
                    } else {
                        bytecodeMeta.set(BytecodeMeta.CodeInfoFlag.MALFORMED_SWITCH);
                        throw new ConfusedCFRException("Extractable last case doesn't follow previous, and can't clone.");
                    }
                }
            }
            lastCase.markBlock(switchBlock);
            breakTarget = indexLastCase + 1;
        } else {
            Op03SimpleStatement statement3;
            int y;
            int validatedLastInThis = SwitchReplacer.checkPreSwitchJump(statements, switchStatement, indexLastCase + 1, indexLastInThis);
            if (validatedLastInThis != indexLastInThis) {
                indexLastInThis = validatedLastInThis;
            }
            for (y = indexLastCase + 1; y <= indexLastInThis; ++y) {
                statement3 = statements.get(y);
                statement3.markBlock(caseBlock);
            }
            for (y = idxFirstCase; y <= indexLastInThis; ++y) {
                statement3 = statements.get(y);
                statement3.markBlock(switchBlock);
            }
            breakTarget = indexLastInThis + 1;
        }
        Op03SimpleStatement breakStatementTarget = statements.get(breakTarget);
        if (retieEnd && (lastInThis = statements.get(indexLastInThis)).getStatement().getClass() == GotoStatement.class) {
            Set<BlockIdentifier> blockIdentifiers = SetFactory.newSet(lastInThis.getBlockIdentifiers());
            blockIdentifiers.remove(caseBlock);
            blockIdentifiers.remove(switchBlock);
            Op03SimpleStatement retie = new Op03SimpleStatement(blockIdentifiers, new GotoStatement(BytecodeLoc.TODO), lastInThis.getIndex().justAfter());
            Op03SimpleStatement target = lastInThis.getTargets().get(0);
            Iterator<Op03SimpleStatement> iterator = target.getSources().iterator();
            while (iterator.hasNext()) {
                Op03SimpleStatement source = iterator.next();
                if (!source.getBlockIdentifiers().contains(switchBlock)) continue;
                iterator.remove();
                retie.addSource(source);
                source.replaceTarget(target, retie);
            }
            if (!retie.getSources().isEmpty()) {
                retie.addTarget(target);
                target.addSource(retie);
                statements.add(breakTarget, retie);
                breakStatementTarget = retie;
            }
        }
        if (breakStatementTarget.getBlockIdentifiers().contains(switchBlock)) {
            return switchStatement;
        }
        Set<Op03SimpleStatement> sources = Misc.followNopGotoBackwards(breakStatementTarget);
        for (Op03SimpleStatement breakSource : sources) {
            Op03SimpleStatement originalTarget;
            if (!breakSource.getBlockIdentifiers().contains(switchBlock) || !breakSource.getJumpType().isUnknown()) continue;
            JumpingStatement jumpingStatement = (JumpingStatement)breakSource.getStatement();
            if (jumpingStatement.getClass() == IfStatement.class) {
                if (breakSource.getTargets().size() != 2) continue;
                originalTarget = breakSource.getTargets().get(1);
            } else {
                if (breakSource.getTargets().size() != 1) continue;
                originalTarget = breakSource.getTargets().get(0);
            }
            if (originalTarget != breakStatementTarget) {
                if (originalTarget == breakSource.getLinearlyNext() && originalTarget.getStatement() instanceof CaseStatement && !((CaseStatement)originalTarget.getStatement()).isDefault() && originalTarget.getSources().contains(switchStatement)) {
                    breakSource.replaceStatement(new Nop());
                    continue;
                }
                breakSource.replaceTarget(originalTarget, breakStatementTarget);
                originalTarget.removeSource(breakSource);
                breakStatementTarget.addSource(breakSource);
            }
            ((JumpingStatement)breakSource.getStatement()).setJumpType(JumpType.BREAK);
        }
        return switchStatement;
    }

    private static int checkPreSwitchJump(List<Op03SimpleStatement> statements, Op03SimpleStatement switchStm, int idxLastStart, int idxLastEnd) {
        InstrIndex idx = switchStm.getIndex();
        for (int x = idxLastStart + 1; x <= idxLastEnd; ++x) {
            Op03SimpleStatement stm = statements.get(x);
            for (Op03SimpleStatement source : stm.getSources()) {
                if (!idx.isBackJumpTo(source)) continue;
                return x - 1;
            }
        }
        return idxLastEnd;
    }

    private static Op03SimpleStatement moveSwitch(Op03SimpleStatement switchStatement, List<Op03SimpleStatement> statements, Op03SimpleStatement firstCase, int idxFirstCase, List<Op03SimpleStatement> targets) {
        if (idxFirstCase == 0) {
            return null;
        }
        Statement swstm = switchStatement.getStatement();
        Op03SimpleStatement pre = statements.get(idxFirstCase - 1);
        if (pre != firstCase.getLinearlyPrevious()) {
            return null;
        }
        if (pre.getStatement() instanceof SwitchStatement || pre.getTargets().contains(firstCase)) {
            return null;
        }
        switchStatement.replaceStatement(new GotoStatement(BytecodeLoc.NONE));
        Op03SimpleStatement gotostm = switchStatement;
        gotostm.getTargets().clear();
        switchStatement = new Op03SimpleStatement(switchStatement.getBlockIdentifiers(), swstm, switchStatement.getSSAIdentifiers(), firstCase.getIndex().justBefore());
        statements.add(switchStatement);
        gotostm.addTarget(switchStatement);
        switchStatement.addSource(gotostm);
        for (Op03SimpleStatement target : targets) {
            target.replaceSource(gotostm, switchStatement);
        }
        switchStatement.getTargets().addAll(targets);
        return switchStatement;
    }

    private static void moveJumpsToCaseStatements(Op03SimpleStatement switchStatement) {
        SwitchStatement switchStmt = (SwitchStatement)switchStatement.getStatement();
        BlockIdentifier switchBlock = switchStmt.getSwitchBlock();
        for (Op03SimpleStatement caseStatement : switchStatement.getTargets()) {
            CaseStatement caseStmt;
            if (!(caseStatement.getStatement() instanceof CaseStatement) || switchBlock != (caseStmt = (CaseStatement)caseStatement.getStatement()).getSwitchBlock()) continue;
            BlockIdentifier caseBlock = caseStmt.getCaseBlock();
            Op03SimpleStatement target = caseStatement.getTargets().get(0);
            Iterator<Op03SimpleStatement> targetSourceIt = target.getSources().iterator();
            while (targetSourceIt.hasNext()) {
                Set<BlockIdentifier> blockIdentifiers;
                Op03SimpleStatement src = targetSourceIt.next();
                if (src == caseStatement || (blockIdentifiers = src.getBlockIdentifiers()).contains(caseBlock) || !blockIdentifiers.contains(switchBlock)) continue;
                targetSourceIt.remove();
                src.replaceTarget(target, caseStatement);
                caseStatement.addSource(src);
            }
        }
    }

    private static void moveJumpsToTerminalIfEmpty(Op03SimpleStatement switchStatement, List<Op03SimpleStatement> statements) {
        Op03SimpleStatement followingTrans;
        SwitchStatement swatch = (SwitchStatement)switchStatement.getStatement();
        Op03SimpleStatement lastTgt = switchStatement.getTargets().get(switchStatement.getTargets().size() - 1);
        BlockIdentifier switchBlock = swatch.getSwitchBlock();
        if (!lastTgt.getBlockIdentifiers().contains(switchBlock)) {
            return;
        }
        if (lastTgt.getTargets().size() != 1) {
            return;
        }
        if (lastTgt.getSources().size() == 1) {
            return;
        }
        Op03SimpleStatement following = lastTgt.getTargets().get(0);
        if (following.getBlockIdentifiers().contains(switchBlock)) {
            return;
        }
        List<Op03SimpleStatement> forwardJumpSources = Functional.filter(lastTgt.getSources(), new Misc.IsForwardJumpTo(lastTgt.getIndex()));
        if (forwardJumpSources.size() > 1) {
            SwitchReplacer.moveInternalJumpsToTerminal(switchStatement, statements, lastTgt, following, forwardJumpSources);
        }
        if ((followingTrans = Misc.followNopGotoChain(following, false, true)) != following) {
            SwitchReplacer.tightenJumpsToTerminal(statements, switchBlock, following, followingTrans);
        }
    }

    private static void tightenJumpsToTerminal(List<Op03SimpleStatement> statements, BlockIdentifier switchBlock, Op03SimpleStatement following, Op03SimpleStatement followingTrans) {
        List<Op03SimpleStatement> tsource = ListFactory.newList(followingTrans.getSources());
        boolean acted = false;
        for (Op03SimpleStatement source : tsource) {
            if (!source.getBlockIdentifiers().contains(switchBlock)) continue;
            followingTrans.removeSource(source);
            source.replaceTarget(followingTrans, following);
            following.addSource(source);
            acted = true;
        }
        if (acted) {
            Statement followingStatement = following.getStatement();
            if (followingStatement instanceof Nop) {
                following.replaceStatement(new CommentStatement(""));
            } else if (followingStatement.getClass() == GotoStatement.class) {
                following.replaceStatement(new CommentStatement(""));
                Op03SimpleStatement force = new Op03SimpleStatement(following.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), following.getSSAIdentifiers(), following.getIndex().justAfter());
                Op03SimpleStatement followingTgt = following.getTargets().get(0);
                followingTgt.replaceSource(following, force);
                following.replaceTarget(followingTgt, force);
                force.addSource(following);
                force.addTarget(followingTgt);
                statements.add(force);
            }
        }
    }

    private static void moveInternalJumpsToTerminal(Op03SimpleStatement switchStatement, List<Op03SimpleStatement> statements, Op03SimpleStatement lastTgt, Op03SimpleStatement following, List<Op03SimpleStatement> forwardJumpSources) {
        int idx = statements.indexOf(lastTgt);
        if (idx == 0) {
            return;
        }
        if (idx >= statements.size() - 1) {
            return;
        }
        if (statements.get(idx + 1) != following) {
            return;
        }
        for (Op03SimpleStatement forwardJumpSource : forwardJumpSources) {
            JumpingStatement jumpingStatement;
            JumpType jumpType;
            if (forwardJumpSource == switchStatement) continue;
            forwardJumpSource.replaceTarget(lastTgt, following);
            lastTgt.removeSource(forwardJumpSource);
            following.addSource(forwardJumpSource);
            Statement forwardJump = forwardJumpSource.getStatement();
            if (!(forwardJump instanceof JumpingStatement) || !(jumpType = (jumpingStatement = (JumpingStatement)forwardJump).getJumpType()).isUnknown()) continue;
            jumpingStatement.setJumpType(JumpType.BREAK);
        }
    }

    private static int getDefault(DecodedSwitch decodedSwitch) {
        List<DecodedSwitchEntry> jumpTargets = decodedSwitch.getJumpTargets();
        for (int idx = 0; idx < jumpTargets.size(); ++idx) {
            DecodedSwitchEntry entry = jumpTargets.get(idx);
            if (!entry.hasDefault()) continue;
            return idx;
        }
        return -1;
    }

    private static void tryInlineRawSwitchContent(Op03SimpleStatement switchStatement, List<Op03SimpleStatement> statements) {
        RawSwitchStatement rawSwitch = (RawSwitchStatement)switchStatement.getStatement();
        int defaultIdx = SwitchReplacer.getDefault(rawSwitch.getSwitchData());
        if (defaultIdx < 0) {
            return;
        }
        Op03SimpleStatement defaultTarget = switchStatement.getTargets().get(defaultIdx);
        Op03SimpleStatement ultTarget = Misc.followNopGotoChain(defaultTarget, true, false);
        Set<Op03SimpleStatement> seen = SetFactory.newSet(switchStatement);
        List<NodesReachedUntil> reachedUntils = ListFactory.newList();
        for (Op03SimpleStatement target : switchStatement.getTargets()) {
            NodesReachedUntil nodesReachedUntil = new NodesReachedUntil(target, ultTarget, seen);
            reachedUntils.add(nodesReachedUntil);
            GraphVisitorDFS<Op03SimpleStatement> gv = new GraphVisitorDFS<Op03SimpleStatement>(target, nodesReachedUntil);
            gv.process();
            if (!nodesReachedUntil.found) {
                return;
            }
            if (nodesReachedUntil.hitBanned) {
                return;
            }
            Set reachedNodes = nodesReachedUntil.reaches;
            for (Op03SimpleStatement reached : reachedNodes) {
                for (Op03SimpleStatement source : reached.getSources()) {
                    if (reachedNodes.contains(source) || source == switchStatement) continue;
                    return;
                }
            }
            if (!SwitchReplacer.blockIsContiguous(statements, target, reachedNodes)) {
                return;
            }
            seen.addAll(reachedNodes);
        }
        InstrIndex targetIndex = switchStatement.getIndex();
        Op03SimpleStatement lastStatement = null;
        Op03SimpleStatement firstStatement = null;
        for (NodesReachedUntil reached : reachedUntils) {
            Op03SimpleStatement start = reached.start;
            int idx = statements.indexOf(start);
            int max = idx + reached.reaches.size();
            for (int x = idx; x < max; ++x) {
                targetIndex = targetIndex.justAfter();
                lastStatement = statements.get(x);
                if (firstStatement == null) {
                    firstStatement = lastStatement;
                }
                lastStatement.setIndex(targetIndex);
            }
        }
        if (lastStatement == null) {
            return;
        }
        if (ultTarget.getIndex().isBackJumpFrom(switchStatement)) {
            targetIndex = targetIndex.justAfter();
            Op03SimpleStatement ultTargetNew = new Op03SimpleStatement(lastStatement.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), targetIndex);
            statements.add(ultTargetNew);
            ultTargetNew.addTarget(ultTarget);
            ultTarget.addSource(ultTargetNew);
            List<Op03SimpleStatement> ultSources = ListFactory.newList(ultTarget.getSources());
            seen.add(switchStatement);
            for (Op03SimpleStatement source : ultSources) {
                if (!seen.contains(source)) continue;
                ultTarget.removeSource(source);
                source.replaceTarget(ultTarget, ultTargetNew);
                ultTargetNew.addSource(source);
            }
        }
        Set<BlockIdentifier> firstBlocks = firstStatement.getBlockIdentifiers();
        List<BlockIdentifier> newInFirst = SetUtil.differenceAtakeBtoList(firstBlocks, switchStatement.getBlockIdentifiers());
        Cleaner.sortAndRenumberInPlace(statements);
        switchStatement.getBlockIdentifiers().addAll(newInFirst);
    }

    private static boolean rewriteDuff(Op03SimpleStatement switchStatement, List<Op03SimpleStatement> statements, VariableFactory vf, DecompilerComments decompilerComments) {
        BlockIdentifier switchBlock = ((SwitchStatement)switchStatement.getStatement()).getSwitchBlock();
        boolean indexLastInLastBlock = false;
        List<Op03SimpleStatement> targets = ListFactory.newList(switchStatement.getTargets());
        Collections.sort(targets, new CompareByIndex());
        BlockIdentifier prevBlock = null;
        BlockIdentifier nextBlock = null;
        Map badSrcMap = MapFactory.newOrderedMap();
        for (Op03SimpleStatement cas : targets) {
            Statement casStm = cas.getStatement();
            if (!(casStm instanceof CaseStatement)) continue;
            CaseStatement caseStm = (CaseStatement)casStm;
            BlockIdentifier caseBlock = caseStm.getCaseBlock();
            prevBlock = nextBlock;
            nextBlock = caseBlock;
            List badSources = null;
            for (Op03SimpleStatement op03SimpleStatement : cas.getSources()) {
                if (op03SimpleStatement == switchStatement || op03SimpleStatement.getBlockIdentifiers().contains(caseBlock) || op03SimpleStatement.getBlockIdentifiers().contains(prevBlock) || caseStm.isDefault()) continue;
                if (badSources == null) {
                    badSources = ListFactory.newList();
                }
                badSources.add(op03SimpleStatement);
            }
            if (badSources == null) continue;
            badSrcMap.put(cas, badSources);
        }
        if (badSrcMap.isEmpty()) {
            return false;
        }
        LValue intermed = vf.tempVariable(new InferredJavaType(RawJavaType.INT, InferredJavaType.Source.TRANSFORM));
        Set<Integer> iVals = SetFactory.newSortedSet();
        for (Op03SimpleStatement cas : targets) {
            Statement casStm = cas.getStatement();
            if (!(casStm instanceof CaseStatement)) continue;
            CaseStatement caseStm = (CaseStatement)casStm;
            List<Expression> values = caseStm.getValues();
            for (Expression e2 : values) {
                Literal l = e2.getComputedLiteral(MapFactory.<LValue, Literal>newMap());
                if (l == null) {
                    return false;
                }
                iVals.add(l.getValue().getIntValue());
            }
        }
        Integer prev = null;
        Integer testValue = null;
        if (!iVals.contains(0)) {
            testValue = 0;
        } else {
            for (Integer i : iVals) {
                if (prev == null) {
                    if (i > Integer.MIN_VALUE) {
                        testValue = Integer.MIN_VALUE;
                        break;
                    }
                    prev = i;
                    continue;
                }
                if (prev - i <= 1) continue;
                testValue = i - 1;
                break;
            }
        }
        if (testValue == null) {
            return false;
        }
        Literal testVal = new Literal(TypedLiteral.getInt(testValue));
        Op03SimpleStatement newPreSwitch = new Op03SimpleStatement(switchStatement.getBlockIdentifiers(), new AssignmentSimple(BytecodeLoc.NONE, intermed, testVal), switchStatement.getIndex().justBefore());
        statements.add(newPreSwitch);
        List<Op03SimpleStatement> switchStatementSources = switchStatement.getSources();
        for (Op03SimpleStatement source : switchStatementSources) {
            source.replaceTarget(switchStatement, newPreSwitch);
            newPreSwitch.addSource(source);
        }
        newPreSwitch.addTarget(switchStatement);
        switchStatementSources.clear();
        switchStatementSources.add(newPreSwitch);
        SwitchStatement switchStatement2 = (SwitchStatement)switchStatement.getStatement();
        Expression e = switchStatement2.getSwitchOn();
        e = new TernaryExpression(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.NONE, new LValueExpression(intermed), testVal, CompOp.EQ), e, new LValueExpression(intermed));
        switchStatement2.setSwitchOn(e);
        Set<Op03SimpleStatement> switchContent = Misc.GraphVisitorBlockReachable.getBlockReachable(switchStatement, switchBlock);
        Op03SimpleStatement last = Misc.getLastInRangeByIndex(switchContent);
        Op03SimpleStatement afterLast = last.getLinearlyNext();
        if (afterLast == null) {
            return false;
        }
        Op03SimpleStatement newPostSwitch = new Op03SimpleStatement(afterLast.getBlockIdentifiers(), new IfStatement(BytecodeLoc.NONE, new BooleanExpression(Literal.TRUE)), afterLast.getIndex().justBefore());
        newPostSwitch.addTarget(afterLast);
        newPostSwitch.addTarget(switchStatement);
        afterLast.addSource(newPostSwitch);
        switchStatement.addSource(newPostSwitch);
        statements.add(newPostSwitch);
        Op03SimpleStatement newBreak = new Op03SimpleStatement(newPostSwitch.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.NONE), newPostSwitch.getIndex().justBefore());
        List<Op03SimpleStatement> postSources = ListFactory.newList(afterLast.getSources());
        for (Op03SimpleStatement op03SimpleStatement : postSources) {
            if (!op03SimpleStatement.getBlockIdentifiers().contains(switchBlock)) continue;
            op03SimpleStatement.replaceTarget(afterLast, newBreak);
            newBreak.addSource(op03SimpleStatement);
            afterLast.removeSource(op03SimpleStatement);
        }
        if (!newBreak.getSources().isEmpty()) {
            newBreak.addTarget(afterLast);
            afterLast.addSource(newBreak);
            statements.add(newBreak);
        }
        for (Map.Entry entry : badSrcMap.entrySet()) {
            Op03SimpleStatement cas = (Op03SimpleStatement)entry.getKey();
            CaseStatement caseStatement = (CaseStatement)cas.getStatement();
            List<Expression> values = caseStatement.getValues();
            if (values.isEmpty()) continue;
            Expression oneValue = values.get(0);
            for (Op03SimpleStatement src : (List)entry.getValue()) {
                cas.removeSource(src);
                Op03SimpleStatement newPreJump = new Op03SimpleStatement(src.getBlockIdentifiers(), new AssignmentSimple(BytecodeLoc.NONE, intermed, oneValue), src.getIndex().justBefore());
                statements.add(newPreJump);
                List<Op03SimpleStatement> srcSources = src.getSources();
                for (Op03SimpleStatement srcSrc : srcSources) {
                    srcSrc.replaceTarget(src, newPreJump);
                    newPreJump.addSource(srcSrc);
                }
                srcSources.clear();
                srcSources.add(newPreJump);
                newPreJump.addTarget(src);
                src.replaceTarget(cas, newPostSwitch);
                newPostSwitch.addSource(src);
            }
        }
        return true;
    }

    private static class NodesReachedUntil
    implements BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>> {
        private final Op03SimpleStatement start;
        private final Op03SimpleStatement target;
        private final Set<Op03SimpleStatement> banned;
        private boolean found = false;
        private boolean hitBanned = false;
        private final Set<Op03SimpleStatement> reaches = SetFactory.newSet();

        private NodesReachedUntil(Op03SimpleStatement start, Op03SimpleStatement target, Set<Op03SimpleStatement> banned) {
            this.start = start;
            this.target = target;
            this.banned = banned;
        }

        @Override
        public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
            if (arg1 == this.target) {
                this.found = true;
                return;
            }
            if (this.banned.contains(arg1)) {
                this.hitBanned = true;
                return;
            }
            if (this.reaches.add(arg1)) {
                arg2.enqueue(arg1.getTargets());
            }
        }
    }

    private static class NodeReachable
    implements BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>> {
        private final Set<Op03SimpleStatement> otherCases;
        private final Op03SimpleStatement switchStatement;
        private final Op03SimpleStatement start;
        private final boolean forcedOrder;
        private final List<Op03SimpleStatement> reaches = ListFactory.newList();
        private final Set<Op03SimpleStatement> inBlock = SetFactory.newSet();

        private NodeReachable(Set<Op03SimpleStatement> otherCases, Op03SimpleStatement start, Op03SimpleStatement switchStatement, boolean forcedOrder) {
            this.otherCases = otherCases;
            this.switchStatement = switchStatement;
            this.start = start;
            this.forcedOrder = forcedOrder;
        }

        @Override
        public void call(Op03SimpleStatement arg1, GraphVisitor<Op03SimpleStatement> arg2) {
            if (arg1 == this.switchStatement) {
                return;
            }
            if (arg1.getIndex().isBackJumpFrom(this.start) && (this.forcedOrder || arg1.getIndex().isBackJumpFrom(this.switchStatement))) {
                return;
            }
            if (arg1 != this.start && this.otherCases.contains(arg1)) {
                this.reaches.add(arg1);
                return;
            }
            this.inBlock.add(arg1);
            arg2.enqueue(arg1.getTargets());
        }
    }
}

