/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMutatingAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticPostMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticPreMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

public class ArithmeticMutationOperation
extends AbstractMutatingAssignmentExpression {
    private LValue mutated;
    private final ArithOp op;
    private Expression mutation;

    public ArithmeticMutationOperation(BytecodeLoc loc, LValue mutated, Expression mutation, ArithOp op) {
        super(loc, mutated.getInferredJavaType());
        this.mutated = mutated;
        this.op = op;
        this.mutation = mutation;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.mutation);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new ArithmeticMutationOperation(this.getLoc(), cloneHelper.replaceOrClone(this.mutated), cloneHelper.replaceOrClone(this.mutation), this.op);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.mutated.collectTypeUsages(collector);
        this.mutation.collectTypeUsages(collector);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.ASSIGNMENT;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.dump(this.mutated).print(' ').operator(this.op.getShowAs() + "=").print(' ');
        this.mutation.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.NEITHER);
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        Set<LValue> fixed = statementContainer.getSSAIdentifiers().getFixedHere();
        lValueRewriter = lValueRewriter.getWithFixed(fixed);
        this.mutation = this.mutation.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.mutated = expressionRewriter.rewriteExpression(this.mutated, ssaIdentifiers, statementContainer, ExpressionRewriterFlags.LANDRVALUE);
        this.mutation = expressionRewriter.rewriteExpression(this.mutation, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.mutation = expressionRewriter.rewriteExpression(this.mutation, ssaIdentifiers, statementContainer, flags);
        this.mutated = expressionRewriter.rewriteExpression(this.mutated, ssaIdentifiers, statementContainer, ExpressionRewriterFlags.LANDRVALUE);
        return this;
    }

    @Override
    public boolean isSelfMutatingOp1(LValue lValue, ArithOp arithOp) {
        return this.mutated.equals(lValue) && this.op == arithOp && this.mutation.equals(new Literal(TypedLiteral.getInt(1)));
    }

    @Override
    public LValue getUpdatedLValue() {
        return this.mutated;
    }

    public ArithOp getOp() {
        return this.op;
    }

    public Expression getMutation() {
        return this.mutation;
    }

    @Override
    public ArithmeticPostMutationOperation getPostMutation() {
        return new ArithmeticPostMutationOperation(this.getLoc(), this.mutated, this.op);
    }

    @Override
    public ArithmeticPreMutationOperation getPreMutation() {
        return new ArithmeticPreMutationOperation(this.getLoc(), this.mutated, this.op);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        this.mutation.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ArithmeticMutationOperation)) {
            return false;
        }
        ArithmeticMutationOperation other = (ArithmeticMutationOperation)o;
        return this.mutated.equals(other.mutated) && this.op.equals((Object)other.op) && this.mutation.equals(other.mutation);
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
        ArithmeticMutationOperation other = (ArithmeticMutationOperation)o;
        if (!constraint.equivalent((Object)this.op, (Object)other.op)) {
            return false;
        }
        if (!constraint.equivalent(this.mutated, other.mutated)) {
            return false;
        }
        return constraint.equivalent(this.mutation, other.mutation);
    }
}

