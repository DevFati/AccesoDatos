/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredFor;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class ForStatement
extends AbstractStatement {
    private ConditionalExpression condition;
    private BlockIdentifier blockIdentifier;
    private AssignmentSimple initial;
    private List<AbstractAssignmentExpression> assignments;

    ForStatement(BytecodeLoc loc, ConditionalExpression conditionalExpression, BlockIdentifier blockIdentifier, AssignmentSimple initial, List<AbstractAssignmentExpression> assignments) {
        super(loc);
        this.condition = conditionalExpression;
        this.blockIdentifier = blockIdentifier;
        this.initial = initial;
        this.assignments = assignments;
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        List<AbstractAssignmentExpression> assigns = ListFactory.newList();
        for (AbstractAssignmentExpression ae : this.assignments) {
            assigns.add((AbstractAssignmentExpression)cloneHelper.replaceOrClone(ae));
        }
        return new ForStatement(this.getLoc(), (ConditionalExpression)cloneHelper.replaceOrClone(this.condition), this.blockIdentifier, (AssignmentSimple)this.initial.deepClone(cloneHelper), assigns);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.assignments, this.condition, this.initial);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.keyword("for ").separator("(");
        if (this.initial != null) {
            dumper.dump(this.initial);
        }
        dumper.print("; ").dump(this.condition).print("; ");
        boolean first = true;
        for (AbstractAssignmentExpression assignment : this.assignments) {
            first = StringUtils.comma(first, dumper);
            dumper.dump(assignment);
        }
        dumper.separator(") ");
        dumper.comment(" // ends " + this.getTargetStatement(1).getContainer().getLabel() + ";").newln();
        return dumper;
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        for (AbstractAssignmentExpression assignment : this.assignments) {
            assignment.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, this.getContainer());
        }
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        this.condition = expressionRewriter.rewriteExpression(this.condition, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE);
        int len = this.assignments.size();
        for (int i = 0; i < len; ++i) {
            this.assignments.set(i, (AbstractAssignmentExpression)expressionRewriter.rewriteExpression(this.assignments.get(i), ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE));
        }
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        this.condition.collectUsedLValues(lValueUsageCollector);
        for (AbstractAssignmentExpression assignment : this.assignments) {
            assignment.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredFor(this.getLoc(), this.condition, this.blockIdentifier, this.initial, this.assignments);
    }

    public BlockIdentifier getBlockIdentifier() {
        return this.blockIdentifier;
    }

    public ConditionalExpression getCondition() {
        return this.condition;
    }

    public AssignmentSimple getInitial() {
        return this.initial;
    }

    public List<AbstractAssignmentExpression> getAssignments() {
        return this.assignments;
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        ForStatement other = (ForStatement)o;
        if (!constraint.equivalent(this.condition, other.condition)) {
            return false;
        }
        if (!constraint.equivalent(this.initial, other.initial)) {
            return false;
        }
        return constraint.equivalent(this.assignments, other.assignments);
    }
}

