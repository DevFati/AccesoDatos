/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticPostMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticPreMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

public class AssignmentExpression
extends AbstractAssignmentExpression {
    private LValue lValue;
    private Expression rValue;

    public AssignmentExpression(BytecodeLoc loc, LValue lValue, Expression rValue) {
        super(loc, lValue.getInferredJavaType());
        this.lValue = lValue;
        this.rValue = rValue;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new AssignmentExpression(this.getLoc(), cloneHelper.replaceOrClone(this.lValue), cloneHelper.replaceOrClone(this.rValue));
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.rValue);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.lValue.collectTypeUsages(collector);
        collector.collectFrom(this.rValue);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.ASSIGNMENT;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.dump(this.lValue).operator(" = ");
        this.rValue.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.NEITHER);
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        this.rValue = this.rValue.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        this.lValue = this.lValue.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.lValue = expressionRewriter.rewriteExpression(this.lValue, ssaIdentifiers, statementContainer, ExpressionRewriterFlags.LVALUE);
        this.rValue = expressionRewriter.rewriteExpression(this.rValue, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    public Expression applyRValueOnlyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.rValue = expressionRewriter.rewriteExpression(this.rValue, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public boolean isValidStatement() {
        return true;
    }

    @Override
    public boolean isSelfMutatingOp1(LValue lValue, ArithOp arithOp) {
        return false;
    }

    @Override
    public ArithmeticPostMutationOperation getPostMutation() {
        throw new IllegalStateException();
    }

    @Override
    public ArithmeticPreMutationOperation getPreMutation() {
        throw new IllegalStateException();
    }

    @Override
    public LValue getUpdatedLValue() {
        return this.lValue;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        lValueUsageCollector.collect(this.lValue, ReadWrite.WRITE);
        this.rValue.collectUsedLValues(lValueUsageCollector);
    }

    public LValue getlValue() {
        return this.lValue;
    }

    public Expression getrValue() {
        return this.rValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        AssignmentExpression that = (AssignmentExpression)o;
        if (this.lValue != null ? !this.lValue.equals(that.lValue) : that.lValue != null) {
            return false;
        }
        return !(this.rValue != null ? !this.rValue.equals(that.rValue) : that.rValue != null);
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
        AssignmentExpression other = (AssignmentExpression)o;
        if (!constraint.equivalent(this.lValue, other.lValue)) {
            return false;
        }
        return constraint.equivalent(this.rValue, other.rValue);
    }

    @Override
    public Literal getComputedLiteral(Map<LValue, Literal> display) {
        if (!(this.lValue instanceof StackSSALabel) && !(this.lValue instanceof LocalVariable)) {
            return null;
        }
        Literal literal = this.rValue.getComputedLiteral(display);
        if (literal == null) {
            return null;
        }
        display.put(this.lValue, literal);
        return literal;
    }
}

