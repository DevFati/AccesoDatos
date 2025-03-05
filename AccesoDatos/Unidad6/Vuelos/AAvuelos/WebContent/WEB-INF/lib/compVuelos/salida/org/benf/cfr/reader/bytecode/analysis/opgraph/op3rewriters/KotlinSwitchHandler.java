/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.CompareByIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ComparisonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.RawSwitchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.opcode.DecodedSwitch;
import org.benf.cfr.reader.bytecode.opcode.DecodedSwitchEntry;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class KotlinSwitchHandler {
    public static List<Op03SimpleStatement> extractStringSwitches(List<Op03SimpleStatement> in, BytecodeMeta bytecodeMeta) {
        List<Op03SimpleStatement> switchStatements = Functional.filter(in, new TypeFilter<RawSwitchStatement>(RawSwitchStatement.class));
        boolean action = false;
        for (Op03SimpleStatement swatch : switchStatements) {
            action |= KotlinSwitchHandler.extractStringSwitch(swatch, in, bytecodeMeta);
        }
        if (!action) {
            return in;
        }
        return Cleaner.sortAndRenumber(in);
    }

    private static boolean extractStringSwitch(final Op03SimpleStatement swatch, List<Op03SimpleStatement> in, BytecodeMeta bytecodeMeta) {
        Expression switchOn2;
        Op03SimpleStatement defaultTran;
        Statement defaultStm;
        RawSwitchStatement rawSwitchStatement = (RawSwitchStatement)swatch.getStatement();
        Expression switchOn = rawSwitchStatement.getSwitchOn();
        WildcardMatch wcm = new WildcardMatch();
        WildcardMatch.ExpressionWildcard testObj = wcm.getExpressionWildCard("obj");
        WildcardMatch.MemberFunctionInvokationWildcard test = wcm.getMemberFunction("test", "hashCode", testObj);
        if (!test.equals(switchOn)) {
            return false;
        }
        Expression obj = testObj.getMatch();
        Set<Expression> aliases = SetFactory.newSet();
        aliases.add(obj);
        if (swatch.getSources().size() == 1) {
            Op03SimpleStatement backptr = swatch;
            do {
                Statement backTest;
                if ((backTest = (backptr = backptr.getSources().get(0)).getStatement()) instanceof Nop) continue;
                if (!(backTest instanceof AssignmentSimple)) break;
                AssignmentSimple backAss = (AssignmentSimple)backTest;
                LValueExpression lValue = new LValueExpression(backAss.getCreatedLValue());
                Expression rValue = backAss.getRValue();
                if (aliases.contains(lValue)) {
                    aliases.add(rValue);
                    break;
                }
                if (!aliases.contains(rValue)) break;
                aliases.add(lValue);
                break;
            } while (backptr.getSources().size() == 1);
        }
        WildcardMatch.AnyOneOfExpression matchObj = new WildcardMatch.AnyOneOfExpression(aliases);
        DecodedSwitch switchData = rawSwitchStatement.getSwitchData();
        List<DecodedSwitchEntry> jumpTargets = switchData.getJumpTargets();
        List<Op03SimpleStatement> targets = swatch.getTargets();
        if (jumpTargets.size() != targets.size()) {
            return false;
        }
        int defaultBranchIdx = -1;
        for (int x = 0; x < jumpTargets.size(); ++x) {
            if (!jumpTargets.get(x).hasDefault()) continue;
            defaultBranchIdx = x;
            break;
        }
        if (defaultBranchIdx == -1) {
            return false;
        }
        Op03SimpleStatement defaultTarget = targets.get(defaultBranchIdx);
        Op03SimpleStatement afterDefault = Misc.followNopGotoChain(defaultTarget, false, true);
        WildcardMatch.MemberFunctionInvokationWildcard eqFn = wcm.getMemberFunction("equals", "equals", matchObj, new CastExpression(BytecodeLoc.NONE, new InferredJavaType(TypeConstants.OBJECT, InferredJavaType.Source.UNKNOWN), wcm.getExpressionWildCard("value")));
        IfStatement testIf = new IfStatement(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.NONE, eqFn, Literal.FALSE, CompOp.EQ));
        IfStatement testNotIf = new IfStatement(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.NONE, eqFn, Literal.FALSE, CompOp.NE));
        final Set<Op03SimpleStatement> reTargetSet = SetFactory.newIdentitySet();
        Map<Op03SimpleStatement, DistinctSwitchTarget> reTargets = MapFactory.newIdentityLazyMap(new UnaryFunction<Op03SimpleStatement, DistinctSwitchTarget>(){

            @Override
            public DistinctSwitchTarget invoke(Op03SimpleStatement arg) {
                reTargetSet.add(arg);
                return new DistinctSwitchTarget(reTargetSet.size());
            }
        });
        List<List> matchesFound = ListFactory.newList();
        List<Pair> transitiveDefaultSources = ListFactory.newList();
        for (int x = 0; x < jumpTargets.size(); ++x) {
            Op03SimpleStatement caseStart = targets.get(x);
            DecodedSwitchEntry switchEntry = jumpTargets.get(x);
            if (switchEntry.hasDefault()) continue;
            Op03SimpleStatement currentCaseLoc = caseStart;
            List found = ListFactory.newList();
            while (true) {
                Op03SimpleStatement nextTest;
                List<Op03SimpleStatement> nextStatements;
                TypedLiteral literal;
                Expression value;
                Op03SimpleStatement nextCaseLoc = null;
                Statement maybeIf = currentCaseLoc.getStatement();
                if (maybeIf.getClass() == GotoStatement.class) {
                    if (currentCaseLoc.getTargets().get(0) == defaultTarget) break;
                    return false;
                }
                wcm.reset();
                if (testIf.equals(maybeIf)) {
                    value = wcm.getExpressionWildCard("value").getMatch();
                    if (value instanceof Literal && (literal = ((Literal)value).getValue()).getType() == TypedLiteral.LiteralType.String) {
                        nextStatements = currentCaseLoc.getTargets();
                        nextTest = nextStatements.get(1);
                        Op03SimpleStatement stringMatchJump = nextStatements.get(0);
                        if (stringMatchJump.getStatement().getClass() == GotoStatement.class) {
                            Op03SimpleStatement op03SimpleStatement = stringMatchJump.getTargets().get(0);
                            OriginalSwitchLookupInfo match = new OriginalSwitchLookupInfo(currentCaseLoc, stringMatchJump, literal, op03SimpleStatement);
                            found.add(match);
                            reTargets.get(op03SimpleStatement).add(match);
                            nextCaseLoc = nextTest;
                            if (nextCaseLoc == defaultTarget) {
                                transitiveDefaultSources.add(Pair.make(currentCaseLoc, defaultTarget));
                            }
                        }
                    }
                } else if (testNotIf.equals(maybeIf) && (value = wcm.getExpressionWildCard("value").getMatch()) instanceof Literal && (literal = ((Literal)value).getValue()).getType() == TypedLiteral.LiteralType.String) {
                    nextStatements = currentCaseLoc.getTargets();
                    nextTest = nextStatements.get(0);
                    Op03SimpleStatement stringMatch = nextStatements.get(1);
                    OriginalSwitchLookupInfo originalSwitchLookupInfo = new OriginalSwitchLookupInfo(currentCaseLoc, null, literal, stringMatch);
                    found.add(originalSwitchLookupInfo);
                    reTargets.get(stringMatch).add(originalSwitchLookupInfo);
                    if (nextTest == defaultTarget) {
                        transitiveDefaultSources.add(Pair.make(currentCaseLoc, defaultTarget));
                        nextCaseLoc = nextTest;
                    } else if (nextTest.getStatement().getClass() == GotoStatement.class) {
                        Op03SimpleStatement nextTarget = Misc.followNopGotoChainUntil(nextTest, defaultTarget, true, false);
                        if (nextTarget == defaultTarget || nextTarget == afterDefault) {
                            transitiveDefaultSources.add(Pair.make(nextTest, nextTest.getTargets().get(0)));
                            nextCaseLoc = nextTarget;
                        } else {
                            nextCaseLoc = nextTest;
                        }
                    }
                }
                if (nextCaseLoc == defaultTarget || nextCaseLoc == afterDefault) break;
                if (nextCaseLoc == null) {
                    return false;
                }
                currentCaseLoc = nextCaseLoc;
            }
            matchesFound.add(found);
        }
        LValue foundValue = null;
        for (Op03SimpleStatement retarget : reTargetSet) {
            Statement reStatement = retarget.getStatement();
            if (!(reStatement instanceof AssignmentSimple)) continue;
            foundValue = reStatement.getCreatedLValue();
            break;
        }
        if (foundValue != null && (defaultStm = (defaultTran = Misc.followNopGotoChain(defaultTarget, true, false)).getStatement()) instanceof RawSwitchStatement && (switchOn2 = ((RawSwitchStatement)defaultStm).getSwitchOn()) != null && switchOn2.equals(new LValueExpression(foundValue))) {
            return false;
        }
        List<Op03SimpleStatement> secondSwitchTargets = ListFactory.newList(reTargets.keySet());
        Collections.sort(secondSwitchTargets, new CompareByIndex());
        List<Op03SimpleStatement> fwds = Functional.filter(secondSwitchTargets, new Predicate<Op03SimpleStatement>(){

            @Override
            public boolean test(Op03SimpleStatement in) {
                return in.getIndex().isBackJumpTo(swatch);
            }
        });
        if (fwds.isEmpty()) {
            return false;
        }
        Op03SimpleStatement firstCase2 = fwds.get(0);
        for (List matches : matchesFound) {
            for (Iterator<Pair> match : matches) {
                if (((OriginalSwitchLookupInfo)((Object)match)).stringMatchJump != null) continue;
                Op03SimpleStatement ifTest = ((OriginalSwitchLookupInfo)((Object)match)).ifTest;
                IfStatement statement = (IfStatement)ifTest.getStatement();
                statement.setCondition(statement.getCondition().getNegated());
                Op03SimpleStatement stringTgt = ifTest.getTargets().get(1);
                Op03SimpleStatement fallThrough = ifTest.getTargets().get(0);
                Op03SimpleStatement op03SimpleStatement = new Op03SimpleStatement(fallThrough.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), ifTest.getIndex().justAfter());
                in.add(op03SimpleStatement);
                stringTgt.replaceSource(ifTest, op03SimpleStatement);
                op03SimpleStatement.addTarget(stringTgt);
                op03SimpleStatement.addSource(ifTest);
                ifTest.getTargets().set(0, op03SimpleStatement);
                ifTest.getTargets().set(1, fallThrough);
                ((OriginalSwitchLookupInfo)((Object)match)).stringMatchJump = op03SimpleStatement;
            }
        }
        LocalVariable lValue = new LocalVariable("tmp", new InferredJavaType(RawJavaType.INT, InferredJavaType.Source.UNKNOWN));
        LValueExpression lValueExpr = new LValueExpression(lValue);
        List switchTargets = ListFactory.newList();
        for (Op03SimpleStatement target : secondSwitchTargets) {
            DistinctSwitchTarget distinctSwitchTarget = reTargets.get(target);
            List<Integer> tmp2 = ListFactory.newList();
            tmp2.add(distinctSwitchTarget.idx);
            DecodedSwitchEntry entry = new DecodedSwitchEntry(tmp2, -1);
            switchTargets.add(entry);
            for (OriginalSwitchLookupInfo originalSwitchLookupInfo : distinctSwitchTarget.entries) {
                Op03SimpleStatement from = originalSwitchLookupInfo.stringMatchJump;
                target.removeSource(from);
                from.removeGotoTarget(target);
            }
        }
        for (Pair defaultSourceAndImmediate : transitiveDefaultSources) {
            Op03SimpleStatement defaultSource = (Op03SimpleStatement)defaultSourceAndImmediate.getFirst();
            Op03SimpleStatement localTarget = (Op03SimpleStatement)defaultSourceAndImmediate.getSecond();
            localTarget.removeSource(defaultSource);
            defaultSource.removeGotoTarget(localTarget);
        }
        List<Integer> defaultSecondary = ListFactory.newList();
        defaultSecondary.add(null);
        switchTargets.add(new DecodedSwitchEntry(defaultSecondary, -1));
        FakeSwitch info = new FakeSwitch(switchTargets);
        RawSwitchStatement secondarySwitch = new RawSwitchStatement(BytecodeLoc.TODO, lValueExpr, info);
        Op03SimpleStatement secondarySwitchStm = new Op03SimpleStatement(firstCase2.getBlockIdentifiers(), secondarySwitch, firstCase2.getIndex().justBefore());
        for (Op03SimpleStatement op03SimpleStatement : secondSwitchTargets) {
            secondarySwitchStm.addTarget(op03SimpleStatement);
            op03SimpleStatement.addSource(secondarySwitchStm);
        }
        secondarySwitchStm.addTarget(defaultTarget);
        defaultTarget.addSource(secondarySwitchStm);
        in.add(secondarySwitchStm);
        Op03SimpleStatement nopHolder = new Op03SimpleStatement(firstCase2.getBlockIdentifiers(), new Nop(), secondarySwitchStm.getIndex().justBefore());
        for (Pair defaultSourceAndImmediate : transitiveDefaultSources) {
            Op03SimpleStatement defaultSource = (Op03SimpleStatement)defaultSourceAndImmediate.getFirst();
            defaultSource.addTarget(nopHolder);
            nopHolder.addSource(defaultSource);
        }
        for (Op03SimpleStatement target : secondSwitchTargets) {
            DistinctSwitchTarget distinctSwitchTarget = reTargets.get(target);
            for (OriginalSwitchLookupInfo originalSwitchLookupInfo : distinctSwitchTarget.entries) {
                Op03SimpleStatement from = originalSwitchLookupInfo.stringMatchJump;
                AssignmentSimple assign = new AssignmentSimple(BytecodeLoc.TODO, lValue, new Literal(TypedLiteral.getInt(distinctSwitchTarget.idx)));
                from.replaceStatement(assign);
                Op03SimpleStatement newJmp = new Op03SimpleStatement(from.getBlockIdentifiers(), new GotoStatement(BytecodeLoc.TODO), from.getIndex().justAfter());
                from.addTarget(newJmp);
                newJmp.addSource(from);
                newJmp.addTarget(nopHolder);
                in.add(newJmp);
                nopHolder.addSource(newJmp);
            }
        }
        in.add(nopHolder);
        nopHolder.addTarget(secondarySwitchStm);
        secondarySwitchStm.addSource(nopHolder);
        defaultTarget.removeSource(swatch);
        swatch.replaceTarget(defaultTarget, nopHolder);
        nopHolder.addSource(swatch);
        Op03SimpleStatement op03SimpleStatement = new Op03SimpleStatement(swatch.getBlockIdentifiers(), new AssignmentSimple(BytecodeLoc.TODO, lValue, new Literal(TypedLiteral.getInt(-1))), swatch.getIndex().justBefore());
        List<Op03SimpleStatement> swatchFrom = swatch.getSources();
        for (Op03SimpleStatement from : swatchFrom) {
            from.replaceTarget(swatch, op03SimpleStatement);
            op03SimpleStatement.addSource(from);
        }
        op03SimpleStatement.addTarget(swatch);
        swatch.getSources().clear();
        swatch.addSource(op03SimpleStatement);
        in.add(op03SimpleStatement);
        bytecodeMeta.set(BytecodeMeta.CodeInfoFlag.STRING_SWITCHES);
        return true;
    }

    private static class FakeSwitch
    implements DecodedSwitch {
        private final List<DecodedSwitchEntry> entry;

        private FakeSwitch(List<DecodedSwitchEntry> entry) {
            this.entry = entry;
        }

        @Override
        public List<DecodedSwitchEntry> getJumpTargets() {
            return this.entry;
        }
    }

    private static class OriginalSwitchLookupInfo {
        Op03SimpleStatement ifTest;
        Op03SimpleStatement stringMatchJump;
        public TypedLiteral literal;
        public Op03SimpleStatement target;

        OriginalSwitchLookupInfo(Op03SimpleStatement ifTest, Op03SimpleStatement stringMatchJump, TypedLiteral literal, Op03SimpleStatement target) {
            this.ifTest = ifTest;
            this.stringMatchJump = stringMatchJump;
            this.literal = literal;
            this.target = target;
        }
    }

    private static class DistinctSwitchTarget {
        List<OriginalSwitchLookupInfo> entries = ListFactory.newList();
        final int idx;

        private DistinctSwitchTarget(int idx) {
            this.idx = idx;
        }

        void add(OriginalSwitchLookupInfo item) {
            this.entries.add(item);
        }
    }
}

