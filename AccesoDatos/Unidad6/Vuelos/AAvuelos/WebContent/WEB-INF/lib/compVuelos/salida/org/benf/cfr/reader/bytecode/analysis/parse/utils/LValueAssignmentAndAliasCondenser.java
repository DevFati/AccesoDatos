/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArrayIndex;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.ArrayVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheckSimple;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class LValueAssignmentAndAliasCondenser
implements LValueRewriter<Statement>,
LValueAssignmentCollector<Statement> {
    private final Map<StackSSALabel, ExpressionStatementPair> found;
    private final Set<StackSSALabel> blacklisted;
    private final Set<LValue> keepConstant;
    private final Map<StackSSALabel, Expression> aliasReplacements;
    private final Map<StackSSALabel, ExpressionStatementPair> multiFound;
    private final Map<VersionedLValue, ExpressionStatementPair> mutableFound;
    private final Map<Expression, Expression> cache = MapFactory.newMap();
    private static final Set<SSAIdent> emptyFixed = SetFactory.newSet();

    public LValueAssignmentAndAliasCondenser() {
        this.found = MapFactory.newOrderedMap();
        this.blacklisted = SetFactory.newOrderedSet();
        this.keepConstant = SetFactory.newSet();
        this.aliasReplacements = MapFactory.newMap();
        this.multiFound = MapFactory.newMap();
        this.mutableFound = MapFactory.newMap();
    }

    public LValueAssignmentAndAliasCondenser(LValueAssignmentAndAliasCondenser other, Set<LValue> keepConstant) {
        this.keepConstant = keepConstant;
        this.found = other.found;
        this.blacklisted = other.blacklisted;
        this.aliasReplacements = other.aliasReplacements;
        this.multiFound = other.multiFound;
        this.mutableFound = other.mutableFound;
    }

    @Override
    public void collect(StackSSALabel lValue, StatementContainer<Statement> statementContainer, Expression value) {
        this.found.put(lValue, new ExpressionStatementPair(value, statementContainer));
    }

    @Override
    public void collectMultiUse(StackSSALabel lValue, StatementContainer<Statement> statementContainer, Expression value) {
        this.multiFound.put(lValue, new ExpressionStatementPair(value, statementContainer));
    }

    @Override
    public void collectMutatedLValue(LValue lValue, StatementContainer<Statement> statementContainer, Expression value) {
        SSAIdent version = statementContainer.getSSAIdentifiers().getSSAIdentOnExit(lValue);
        if (null != this.mutableFound.put(new VersionedLValue(lValue, version), new ExpressionStatementPair(value, statementContainer))) {
            throw new ConfusedCFRException("Duplicate versioned SSA Ident.");
        }
    }

    @Override
    public void collectLocalVariableAssignment(LocalVariable localVariable, StatementContainer<Statement> statementContainer, Expression value) {
    }

    private Set<LValue> findAssignees(Statement s) {
        if (!(s instanceof AssignmentSimple)) {
            return null;
        }
        AssignmentSimple assignmentSimple = (AssignmentSimple)s;
        Set<LValue> res = SetFactory.newSet();
        res.add(assignmentSimple.getCreatedLValue());
        Expression rvalue = assignmentSimple.getRValue();
        while (rvalue instanceof AssignmentExpression) {
            AssignmentExpression assignmentExpression = (AssignmentExpression)rvalue;
            res.add(assignmentExpression.getlValue());
            rvalue = assignmentExpression.getrValue();
        }
        return res;
    }

    @Override
    public LValueRewriter getWithFixed(Set<SSAIdent> fixed) {
        return this;
    }

    @Override
    public LValueRewriter<Statement> keepConstant(Collection<LValue> usedLValues) {
        return new LValueAssignmentAndAliasCondenser(this, SetFactory.newSet(this.keepConstant, usedLValues));
    }

    public void reset() {
        this.keepConstant.clear();
    }

    @Override
    public boolean needLR() {
        return false;
    }

    @Override
    public Expression getLValueReplacement(LValue lValue, SSAIdentifiers<LValue> ssaIdentifiers, StatementContainer<Statement> lvSc) {
        if (!(lValue instanceof StackSSALabel)) {
            return null;
        }
        StackSSALabel stackSSALabel = (StackSSALabel)lValue;
        if (!this.found.containsKey(stackSSALabel)) {
            return null;
        }
        if (this.blacklisted.contains(stackSSALabel)) {
            return null;
        }
        ExpressionStatementPair pair = this.found.get(stackSSALabel);
        StatementContainer statementContainer = pair.statementContainer;
        SSAIdentifiers<LValue> replacementIdentifiers = statementContainer == null ? null : statementContainer.getSSAIdentifiers();
        Expression res = pair.expression;
        Set<LValue> changes = null;
        if (replacementIdentifiers != null) {
            if (!this.keepConstant.isEmpty()) {
                for (LValue lValue2 : this.keepConstant) {
                    if (replacementIdentifiers.unchanged(lValue2)) continue;
                    return null;
                }
            }
            LValueUsageCollectorSimple lvcInSource = new LValueUsageCollectorSimple();
            res.collectUsedLValues(lvcInSource);
            for (LValue lValue3 : lvcInSource.getUsedLValues()) {
                if (ssaIdentifiers.isValidReplacement(lValue3, replacementIdentifiers)) continue;
                Set<LValue> assignees = this.findAssignees(lvSc.getStatement());
                if (assignees != null && assignees.contains(lValue3)) {
                    Op03SimpleStatement lv03 = (Op03SimpleStatement)lvSc;
                    for (Op03SimpleStatement source : lv03.getSources()) {
                        if (source.getSSAIdentifiers().isValidReplacementOnExit(lValue3, replacementIdentifiers)) continue;
                        return null;
                    }
                    continue;
                }
                return null;
            }
            Set<LValue> set = changes = statementContainer instanceof Op03SimpleStatement ? replacementIdentifiers.getChanges() : null;
            if (changes != null && !changes.isEmpty()) {
                Op03SimpleStatement op03SimpleStatement = (Op03SimpleStatement)statementContainer;
                for (Op03SimpleStatement target : op03SimpleStatement.getTargets()) {
                    if (target == lvSc) continue;
                    for (LValue change : changes) {
                        if (!target.getSSAIdentifiers().getSSAIdentOnEntry(change).equals(replacementIdentifiers.getSSAIdentOnExit(change))) continue;
                        return null;
                    }
                }
            }
        }
        if (statementContainer != null) {
            if (!statementContainer.getBlockIdentifiers().equals(lvSc.getBlockIdentifiers())) {
                Op03SimpleStatement lv03 = (Op03SimpleStatement)lvSc;
                Op03SimpleStatement op03SimpleStatement = lv03.getLinearlyPrevious();
                for (BlockIdentifier left : SetUtil.differenceAtakeBtoList(statementContainer.getBlockIdentifiers(), lvSc.getBlockIdentifiers())) {
                    if (left.getBlockType() != BlockType.TRYBLOCK || op03SimpleStatement != null && op03SimpleStatement.getBlockIdentifiers().contains(left)) continue;
                    return null;
                }
            }
            if (!this.isSimple(res) && this.jumpsMethods((Op03SimpleStatement)lvSc, (Op03SimpleStatement)statementContainer)) {
                return null;
            }
            lvSc.copyBlockInformationFrom(statementContainer);
            lvSc.copyBytecodeInformationFrom(statementContainer);
            statementContainer.nopOut();
        }
        if (changes != null && !changes.isEmpty()) {
            SSAIdentifiers<LValue> tgtIdents = lvSc.getSSAIdentifiers();
            for (LValue lValue4 : changes) {
                tgtIdents.setKnownIdentifierOnEntry(lValue4, replacementIdentifiers.getSSAIdentOnEntry(lValue4));
            }
        }
        stackSSALabel.getStackEntry().decrementUsage();
        if (this.aliasReplacements.containsKey(stackSSALabel)) {
            this.found.put(stackSSALabel, new ExpressionStatementPair(this.aliasReplacements.get(stackSSALabel), null));
            this.aliasReplacements.remove(stackSSALabel);
        }
        Expression prev = null;
        if (res instanceof StackValue && ((StackValue)res).getStackValue() == stackSSALabel) {
            prev = res;
        }
        while (res != null && res != prev) {
            prev = res;
            if (this.cache.containsKey(res)) {
                prev = res = this.cache.get(res);
            }
            res = res.replaceSingleUsageLValues(this, ssaIdentifiers, lvSc);
        }
        this.cache.put(new StackValue(BytecodeLoc.NONE, stackSSALabel), prev);
        return prev;
    }

    private boolean isSimple(Expression res) {
        if (res instanceof StackValue) {
            return true;
        }
        return !res.canThrow(ExceptionCheckSimple.INSTANCE);
    }

    private boolean jumpsMethods(Op03SimpleStatement lvSc, Op03SimpleStatement statementContainer) {
        if (statementContainer.getTargets().size() == 0) {
            return false;
        }
        Op03SimpleStatement cur = lvSc;
        while (cur.getSources().size() == 1) {
            Expression ee;
            if ((cur = cur.getSources().get(0)) == statementContainer) {
                return false;
            }
            if (!(cur.getStatement() instanceof ExpressionStatement) || !((ee = ((ExpressionStatement)cur.getStatement()).getExpression()) instanceof AbstractFunctionInvokation)) continue;
            return true;
        }
        return true;
    }

    @Override
    public boolean explicitlyReplaceThisLValue(LValue lValue) {
        return false;
    }

    @Override
    public void checkPostConditions(LValue lValue, Expression rValue) {
        if (!(lValue instanceof StackSSALabel)) {
            return;
        }
        StackSSALabel label = (StackSSALabel)lValue;
        if (this.aliasReplacements.containsKey(label)) {
            return;
        }
        if (!this.found.containsKey(label)) {
            return;
        }
        long count = label.getStackEntry().getUsageCount();
        if (count > 1L && !rValue.isSimple()) {
            this.blacklisted.add(label);
        }
    }

    public AliasRewriter getAliasRewriter() {
        return new AliasRewriter();
    }

    public MutationRewriterFirstPass getMutationRewriterFirstPass() {
        if (this.mutableFound.isEmpty()) {
            return null;
        }
        return new MutationRewriterFirstPass();
    }

    private static final class VersionedLValue {
        private final LValue lValue;
        private final SSAIdent ssaIdent;

        private VersionedLValue(LValue lValue, SSAIdent ssaIdent) {
            this.lValue = lValue;
            this.ssaIdent = ssaIdent;
        }

        public int hashCode() {
            return this.lValue.hashCode() + 31 * this.ssaIdent.hashCode();
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof VersionedLValue)) {
                return false;
            }
            VersionedLValue other = (VersionedLValue)o;
            return this.lValue.equals(other.lValue) && this.ssaIdent.equals(other.ssaIdent);
        }
    }

    private static class LValueStatementContainer {
        private final LValue lValue;
        private final StatementContainer statementContainer;

        private LValueStatementContainer(LValue lValue, StatementContainer statementContainer) {
            this.lValue = lValue;
            this.statementContainer = statementContainer;
        }
    }

    public class MutationRewriterSecondPass
    implements LValueRewriter<Statement> {
        private final Set<SSAIdent> fixed;
        private final Map<VersionedLValue, StatementContainer> mutableReplacable;

        private MutationRewriterSecondPass(Map<VersionedLValue, StatementContainer> mutableReplacable) {
            this.mutableReplacable = mutableReplacable;
            this.fixed = emptyFixed;
        }

        private MutationRewriterSecondPass(Map<VersionedLValue, StatementContainer> mutableReplacable, Set<SSAIdent> fixed) {
            this.mutableReplacable = mutableReplacable;
            this.fixed = fixed;
        }

        @Override
        public boolean needLR() {
            return true;
        }

        @Override
        public LValueRewriter<Statement> keepConstant(Collection<LValue> usedLValues) {
            return this;
        }

        @Override
        public Expression getLValueReplacement(LValue lValue, SSAIdentifiers<LValue> ssaIdentifiers, StatementContainer<Statement> statementContainer) {
            VersionedLValue versionedLValue;
            StatementContainer canReplaceIn;
            SSAIdent ssaIdent = ssaIdentifiers.getSSAIdentOnExit(lValue);
            if (ssaIdent != null && (canReplaceIn = this.mutableReplacable.get(versionedLValue = new VersionedLValue(lValue, ssaIdent))) == statementContainer) {
                ExpressionStatementPair replaceWith = (ExpressionStatementPair)LValueAssignmentAndAliasCondenser.this.mutableFound.get(versionedLValue);
                StatementContainer replacement = replaceWith.statementContainer;
                if (replacement == statementContainer) {
                    return null;
                }
                SSAIdentifiers<LValue> previousIdents = replacement.getSSAIdentifiers();
                Set<LValue> fixedPrevious = previousIdents.getFixedHere();
                if (SetUtil.hasIntersection(this.fixed, fixedPrevious)) {
                    return null;
                }
                SSAIdentifiers<LValue> currentIdents = statementContainer.getSSAIdentifiers();
                LValueUsageCollectorSimple collector = new LValueUsageCollectorSimple();
                ((Statement)replacement.getStatement()).collectLValueUsage(collector);
                for (LValue testSafe : collector.getUsedLValues()) {
                    if (previousIdents.isValidReplacementOnExit(testSafe, currentIdents)) continue;
                    return null;
                }
                if (!(statementContainer instanceof Op03SimpleStatement)) {
                    return null;
                }
                if (!Misc.justReachableFrom((Op03SimpleStatement)statementContainer, (Op03SimpleStatement)replacement, 5)) {
                    return null;
                }
                this.mutableReplacable.remove(versionedLValue);
                replacement.nopOut();
                currentIdents.setKnownIdentifierOnEntry(lValue, previousIdents.getSSAIdentOnEntry(lValue));
                currentIdents.fixHere(previousIdents.getFixedHere());
                return replaceWith.expression;
            }
            return null;
        }

        @Override
        public LValueRewriter getWithFixed(Set<SSAIdent> fixed) {
            return new MutationRewriterSecondPass(this.mutableReplacable, SetFactory.newSet(this.fixed, fixed));
        }

        @Override
        public boolean explicitlyReplaceThisLValue(LValue lValue) {
            return true;
        }

        @Override
        public void checkPostConditions(LValue lValue, Expression rValue) {
        }
    }

    public class MutationRewriterFirstPass
    implements LValueRewriter<Statement> {
        private final Map<VersionedLValue, Set<StatementContainer>> mutableUseFound = MapFactory.newLazyMap(new UnaryFunction<VersionedLValue, Set<StatementContainer>>(){

            @Override
            public Set<StatementContainer> invoke(VersionedLValue arg) {
                return SetFactory.newSet();
            }
        });

        @Override
        public Expression getLValueReplacement(LValue lValue, SSAIdentifiers<LValue> ssaIdentifiers, StatementContainer<Statement> statementContainer) {
            SSAIdent ssaIdent = ssaIdentifiers.getSSAIdentOnExit(lValue);
            if (ssaIdent != null) {
                VersionedLValue versionedLValue = new VersionedLValue(lValue, ssaIdent);
                if (LValueAssignmentAndAliasCondenser.this.mutableFound.containsKey(versionedLValue)) {
                    this.mutableUseFound.get(versionedLValue).add(statementContainer);
                }
            }
            return null;
        }

        @Override
        public boolean needLR() {
            return false;
        }

        @Override
        public LValueRewriter<Statement> keepConstant(Collection<LValue> usedLValues) {
            return this;
        }

        @Override
        public LValueRewriter getWithFixed(Set fixed) {
            return this;
        }

        @Override
        public boolean explicitlyReplaceThisLValue(LValue lValue) {
            return true;
        }

        @Override
        public void checkPostConditions(LValue lValue, Expression rValue) {
        }

        private StatementContainer getUniqueParent(StatementContainer start, Set<StatementContainer> seen) {
            List<Op03SimpleStatement> targets;
            Op03SimpleStatement o3current = (Op03SimpleStatement)start;
            do {
                if (seen.contains(o3current)) {
                    return o3current;
                }
                targets = o3current.getTargets();
                if (targets.size() == 1) continue;
                return null;
            } while ((o3current = targets.get(0)) != start);
            return null;
        }

        public MutationRewriterSecondPass getSecondPassRewriter() {
            Map replacableUses = MapFactory.newMap();
            for (Map.Entry<VersionedLValue, Set<StatementContainer>> entry : this.mutableUseFound.entrySet()) {
                ExpressionStatementPair definition = (ExpressionStatementPair)LValueAssignmentAndAliasCondenser.this.mutableFound.get(entry.getKey());
                StatementContainer uniqueParent = this.getUniqueParent(definition.statementContainer, entry.getValue());
                if (uniqueParent == null) continue;
                replacableUses.put(entry.getKey(), uniqueParent);
            }
            if (replacableUses.isEmpty()) {
                return null;
            }
            return new MutationRewriterSecondPass(replacableUses);
        }
    }

    public class AliasRewriter
    implements LValueRewriter<Statement> {
        private final Map<StackSSALabel, List<StatementContainer<Statement>>> usages = MapFactory.newLazyMap(new UnaryFunction<StackSSALabel, List<StatementContainer<Statement>>>(){

            @Override
            public List<StatementContainer<Statement>> invoke(StackSSALabel ignore) {
                return ListFactory.newList();
            }
        });
        private final Map<StackSSALabel, List<LValueStatementContainer>> possibleAliases = MapFactory.newLazyMap(new UnaryFunction<StackSSALabel, List<LValueStatementContainer>>(){

            @Override
            public List<LValueStatementContainer> invoke(StackSSALabel ignore) {
                return ListFactory.newList();
            }
        });

        @Override
        public LValueRewriter getWithFixed(Set<SSAIdent> fixed) {
            return this;
        }

        @Override
        public LValueRewriter<Statement> keepConstant(Collection<LValue> usedLValues) {
            return this;
        }

        @Override
        public boolean needLR() {
            return false;
        }

        @Override
        public Expression getLValueReplacement(LValue lValue, SSAIdentifiers<LValue> ssaIdentifiers, StatementContainer<Statement> statementContainer) {
            if (!(lValue instanceof StackSSALabel)) {
                return null;
            }
            StackSSALabel stackSSALabel = (StackSSALabel)lValue;
            if (!LValueAssignmentAndAliasCondenser.this.multiFound.containsKey(lValue)) {
                return null;
            }
            if (statementContainer.getStatement() instanceof AssignmentSimple) {
                ExpressionStatementPair es;
                AssignmentSimple assignmentSimple = (AssignmentSimple)statementContainer.getStatement();
                Expression rhs = assignmentSimple.getRValue();
                if (rhs instanceof StackValue) {
                    if (((StackValue)rhs).getStackValue().equals(stackSSALabel)) {
                        this.possibleAliases.get(stackSSALabel).add(new LValueStatementContainer(assignmentSimple.getCreatedLValue(), statementContainer));
                    }
                } else if (stackSSALabel.getInferredJavaType().getJavaTypeInstance() instanceof JavaArrayTypeInstance && (es = (ExpressionStatementPair)LValueAssignmentAndAliasCondenser.this.multiFound.get(stackSSALabel)) != null && es.expression instanceof LValueExpression) {
                    this.possibleAliases.get(stackSSALabel).add(new LValueStatementContainer(((LValueExpression)es.expression).getLValue(), statementContainer));
                }
            }
            this.usages.get(stackSSALabel).add(statementContainer);
            return null;
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        private LValue getAlias(StackSSALabel stackSSALabel, ExpressionStatementPair target) {
            ExpressionStatementPair mf;
            List<LValueStatementContainer> possibleAliasList = this.possibleAliases.get(stackSSALabel);
            if (possibleAliasList.isEmpty()) {
                return null;
            }
            LValue guessAlias = null;
            StatementContainer guessStatement = null;
            for (LValueStatementContainer lValueStatementContainer : possibleAliasList) {
                if (lValueStatementContainer.lValue instanceof StackSSALabel) continue;
                guessAlias = lValueStatementContainer.lValue;
                guessStatement = lValueStatementContainer.statementContainer;
                break;
            }
            if (guessAlias == null && stackSSALabel.getInferredJavaType().getJavaTypeInstance() instanceof JavaArrayTypeInstance && (mf = (ExpressionStatementPair)LValueAssignmentAndAliasCondenser.this.multiFound.get(stackSSALabel)) != null && mf.expression instanceof LValueExpression) {
                guessAlias = ((LValueExpression)mf.expression).getLValue();
                guessStatement = mf.statementContainer;
            }
            if (guessAlias == null) {
                return null;
            }
            LValue returnGuessAlias = guessAlias;
            List<LValue> checkThese = ListFactory.newList();
            if (guessAlias instanceof ArrayVariable) {
                ArrayVariable arrayVariable = (ArrayVariable)guessAlias;
                ArrayIndex arrayIndex = arrayVariable.getArrayIndex();
                Expression array = arrayIndex.getArray();
                if (!(array instanceof LValueExpression)) {
                    return null;
                }
                LValueExpression lValueArrayIndex = (LValueExpression)array;
                checkThese.add(lValueArrayIndex.getLValue());
                Expression index = arrayIndex.getIndex();
                if (index instanceof LValueExpression) {
                    checkThese.add(((LValueExpression)index).getLValue());
                } else {
                    if (!(index instanceof Literal)) return null;
                    MiscUtils.handyBreakPoint();
                }
            } else {
                checkThese.add(guessAlias);
            }
            for (StatementContainer<Statement> verifyStatement : this.usages.get(stackSSALabel)) {
                if (verifyStatement.getStatement().doesBlackListLValueReplacement(stackSSALabel, target.expression)) {
                    return null;
                }
                for (LValue checkThis : checkThese) {
                    if (guessStatement == verifyStatement || verifyStatement.getSSAIdentifiers().isValidReplacement(checkThis, guessStatement.getSSAIdentifiers())) continue;
                    return null;
                }
            }
            return returnGuessAlias;
        }

        public void inferAliases() {
            for (Map.Entry multi : LValueAssignmentAndAliasCondenser.this.multiFound.entrySet()) {
                StackSSALabel stackSSALabel = (StackSSALabel)multi.getKey();
                LValue alias = this.getAlias(stackSSALabel, (ExpressionStatementPair)multi.getValue());
                if (alias == null) continue;
                LValueAssignmentAndAliasCondenser.this.found.put(stackSSALabel, multi.getValue());
                LValueAssignmentAndAliasCondenser.this.aliasReplacements.put(stackSSALabel, new LValueExpression(alias));
            }
        }

        @Override
        public boolean explicitlyReplaceThisLValue(LValue lValue) {
            return false;
        }

        @Override
        public void checkPostConditions(LValue lValue, Expression rValue) {
        }
    }

    private static class ExpressionStatementPair {
        private final Expression expression;
        private final StatementContainer<Statement> statementContainer;

        private ExpressionStatementPair(Expression expression, StatementContainer<Statement> statementContainer) {
            this.expression = expression;
            this.statementContainer = statementContainer;
        }

        public String toString() {
            return this.statementContainer.toString();
        }
    }
}

