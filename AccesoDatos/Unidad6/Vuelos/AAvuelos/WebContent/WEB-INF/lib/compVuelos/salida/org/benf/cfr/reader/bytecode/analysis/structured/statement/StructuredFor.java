/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMutatingAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.ScopeDiscoverInfoCache;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredBlockStatement;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredFor
extends AbstractStructuredBlockStatement {
    private ConditionalExpression condition;
    private AssignmentSimple initial;
    private List<AbstractAssignmentExpression> assignments;
    private final BlockIdentifier block;
    private boolean isCreator;

    public StructuredFor(BytecodeLoc loc, ConditionalExpression condition, AssignmentSimple initial, List<AbstractAssignmentExpression> assignments, Op04StructuredStatement body, BlockIdentifier block) {
        super(loc, body);
        this.condition = condition;
        this.initial = initial;
        this.assignments = assignments;
        this.block = block;
        this.isCreator = false;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(this.condition);
        collector.collectFrom(this.assignments);
        super.collectTypeUsages(collector);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.assignments, this.condition, this.initial);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (this.block.hasForeignReferences()) {
            dumper.label(this.block.getName(), true);
        }
        dumper.keyword("for ").separator("(");
        if (this.initial != null) {
            if (this.isCreator) {
                LValue.Creation.dump(dumper, this.initial.getCreatedLValue()).operator(" = ").dump(this.initial.getRValue()).separator(";");
            } else {
                dumper.dump(this.initial);
            }
            dumper.removePendingCarriageReturn();
        } else {
            dumper.separator(";");
        }
        dumper.print(" ").dump(this.condition).separator("; ");
        boolean first = true;
        for (Expression expression : this.assignments) {
            first = StringUtils.comma(first, dumper);
            dumper.dump(expression);
        }
        dumper.separator(") ");
        this.getBody().dump(dumper);
        return dumper;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
        this.getBody().linearizeStatementsInto(out);
    }

    @Override
    public BlockIdentifier getBreakableBlockOrNull() {
        return this.block;
    }

    @Override
    public boolean supportsBreak() {
        return true;
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        scopeDiscoverer.enterBlock(this);
        for (Expression expression : this.assignments) {
            expression.collectUsedLValues(scopeDiscoverer);
        }
        this.condition.collectUsedLValues(scopeDiscoverer);
        if (this.initial != null) {
            Expression expression;
            LValue lValue = this.initial.getCreatedLValue();
            Expression rhs = expression = this.initial.getRValue();
            LValue lv2 = lValue;
            do {
                scopeDiscoverer.collect(lv2, ReadWrite.READ);
                if (rhs instanceof AssignmentExpression) {
                    AssignmentExpression assignmentExpression = (AssignmentExpression)rhs;
                    lv2 = assignmentExpression.getlValue();
                    rhs = assignmentExpression.getrValue();
                    continue;
                }
                lv2 = null;
                rhs = null;
            } while (lv2 != null);
            lValue.collectLValueAssignments(expression, this.getContainer(), scopeDiscoverer);
        }
        scopeDiscoverer.processOp04Statement(this.getBody());
        scopeDiscoverer.leaveBlock(this);
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {
        LValue lValue = null;
        if (this.initial != null) {
            lValue = this.initial.getCreatedLValue();
        }
        if (!scopedEntity.equals(lValue)) {
            throw new IllegalStateException("Being asked to define something I can't define.");
        }
        this.isCreator = true;
    }

    @Override
    public boolean canDefine(LValue scopedEntity, ScopeDiscoverInfoCache factCache) {
        LValue lValue = null;
        if (this.initial != null) {
            lValue = this.initial.getCreatedLValue();
        }
        if (scopedEntity == null) {
            return false;
        }
        return scopedEntity.equals(lValue);
    }

    @Override
    public List<LValue> findCreatedHere() {
        if (!this.isCreator) {
            return null;
        }
        if (this.initial == null) {
            return null;
        }
        LValue created = this.initial.getCreatedLValue();
        if (!(created instanceof LocalVariable)) {
            return null;
        }
        return ListFactory.newImmutableList(created);
    }

    @Override
    public String suggestName(LocalVariable createdHere, Predicate<String> testNameUsedFn) {
        String[] poss;
        JavaTypeInstance loopType = createdHere.getInferredJavaType().getJavaTypeInstance();
        if (!(this.assignments.get(0) instanceof AbstractMutatingAssignmentExpression)) {
            return null;
        }
        if (!(loopType instanceof RawJavaType)) {
            return null;
        }
        RawJavaType rawJavaType = (RawJavaType)loopType;
        switch (rawJavaType) {
            case INT: 
            case SHORT: 
            case LONG: {
                break;
            }
            default: {
                return null;
            }
        }
        for (String posss : poss = new String[]{"i", "j", "k"}) {
            if (testNameUsedFn.test(posss)) continue;
            return posss;
        }
        return "i";
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        this.condition = expressionRewriter.rewriteExpression(this.condition, null, (StatementContainer)this.getContainer(), null);
        this.initial.rewriteExpressions(expressionRewriter, null);
        for (int x = 0; x < this.assignments.size(); ++x) {
            this.assignments.set(x, (AbstractAssignmentExpression)expressionRewriter.rewriteExpression(this.assignments.get(x), null, (StatementContainer)this.getContainer(), null));
        }
    }

    public BlockIdentifier getBlock() {
        return this.block;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredFor)) {
            return false;
        }
        StructuredFor other = (StructuredFor)o;
        if (!this.initial.equals(other.initial)) {
            return false;
        }
        if (this.condition == null ? other.condition != null : !this.condition.equals(other.condition)) {
            return false;
        }
        if (!this.assignments.equals(other.assignments)) {
            return false;
        }
        if (!this.block.equals(other.block)) {
            return false;
        }
        matchIterator.advance();
        return true;
    }
}

