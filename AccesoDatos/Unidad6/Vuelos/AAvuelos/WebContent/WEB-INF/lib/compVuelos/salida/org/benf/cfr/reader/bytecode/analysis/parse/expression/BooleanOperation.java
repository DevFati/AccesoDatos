/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BoolOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NotOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ConditionalUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class BooleanOperation
extends AbstractExpression
implements ConditionalExpression {
    private ConditionalExpression lhs;
    private ConditionalExpression rhs;
    private BoolOp op;

    public BooleanOperation(BytecodeLoc loc, ConditionalExpression lhs, ConditionalExpression rhs, BoolOp op) {
        super(loc, new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION));
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new BooleanOperation(this.getLoc(), (ConditionalExpression)cloneHelper.replaceOrClone(this.lhs), (ConditionalExpression)cloneHelper.replaceOrClone(this.rhs), this.op);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.lhs, this.rhs);
    }

    public ConditionalExpression getLhs() {
        return this.lhs;
    }

    public ConditionalExpression getRhs() {
        return this.rhs;
    }

    public BoolOp getOp() {
        return this.op;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.lhs.collectTypeUsages(collector);
        this.rhs.collectTypeUsages(collector);
    }

    @Override
    public int getSize(Precedence outerPrecedence) {
        Precedence precedence = this.getPrecedence();
        int initial = outerPrecedence.compareTo(precedence) < 0 ? 2 : 0;
        return initial + this.lhs.getSize(precedence) + 2 + this.rhs.getSize(precedence);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        if (lValueRewriter.needLR()) {
            this.lhs = (ConditionalExpression)this.lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
            this.rhs = (ConditionalExpression)this.rhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        } else {
            this.rhs = (ConditionalExpression)this.rhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
            this.lhs = (ConditionalExpression)this.lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        }
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.lhs = expressionRewriter.rewriteExpression(this.lhs, ssaIdentifiers, statementContainer, flags);
        this.rhs = expressionRewriter.rewriteExpression(this.rhs, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.rhs = expressionRewriter.rewriteExpression(this.rhs, ssaIdentifiers, statementContainer, flags);
        this.lhs = expressionRewriter.rewriteExpression(this.lhs, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    public Expression applyLHSOnlyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.lhs = expressionRewriter.rewriteExpression(this.lhs, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Precedence getPrecedence() {
        return this.op.getPrecedence();
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        this.lhs.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.TRUE);
        d.print(" ").operator(this.op.getShowAs()).print(" ");
        this.rhs.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.FALSE);
        return d;
    }

    @Override
    public ConditionalExpression getNegated() {
        return new NotOperation(this.getLoc(), this);
    }

    @Override
    public ConditionalExpression getDemorganApplied(boolean amNegating) {
        return new BooleanOperation(this.getLoc(), this.lhs.getDemorganApplied(amNegating), this.rhs.getDemorganApplied(amNegating), amNegating ? this.op.getDemorgan() : this.op);
    }

    @Override
    public ConditionalExpression getRightDeep() {
        while (this.lhs instanceof BooleanOperation) {
            BooleanOperation lbool = (BooleanOperation)this.lhs;
            if (lbool.op != this.op) break;
            ConditionalExpression a = lbool.lhs;
            ConditionalExpression b = lbool.rhs;
            ConditionalExpression c = this.rhs;
            this.lhs = a;
            lbool.lhs = b;
            lbool.rhs = c;
            this.rhs = lbool;
        }
        this.lhs = this.lhs.getRightDeep();
        this.rhs = this.rhs.getRightDeep();
        return this;
    }

    public static ConditionalExpression makeRightDeep(List<ConditionalExpression> c, BoolOp op) {
        ConditionalExpression res = c.get(c.size() - 1);
        for (int x = c.size() - 2; x >= 0; --x) {
            res = new BooleanOperation(BytecodeLoc.NONE, c.get(x), res, op);
        }
        return res;
    }

    @Override
    public Set<LValue> getLoopLValues() {
        Set<LValue> res = SetFactory.newSet();
        res.addAll(this.lhs.getLoopLValues());
        res.addAll(this.rhs.getLoopLValues());
        return res;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        this.lhs.collectUsedLValues(lValueUsageCollector);
        this.rhs.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public ConditionalExpression optimiseForType() {
        this.lhs = this.lhs.optimiseForType();
        this.rhs = this.rhs.optimiseForType();
        return this;
    }

    @Override
    public ConditionalExpression simplify() {
        return ConditionalUtils.simplify(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BooleanOperation)) {
            return false;
        }
        BooleanOperation that = (BooleanOperation)o;
        if (!this.lhs.equals(that.lhs)) {
            return false;
        }
        if (this.op != that.op) {
            return false;
        }
        return this.rhs.equals(that.rhs);
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
        BooleanOperation other = (BooleanOperation)o;
        if (this.op != other.op && !constraint.equivalent(this.lhs, other.lhs)) {
            return false;
        }
        return constraint.equivalent(this.rhs, other.rhs);
    }

    private static Boolean getComputed(Expression e, Map<LValue, Literal> display) {
        Literal lv = e.getComputedLiteral(display);
        if (lv == null) {
            return null;
        }
        return lv.getValue().getMaybeBoolValue();
    }

    @Override
    public Literal getComputedLiteral(Map<LValue, Literal> display) {
        Boolean lb = BooleanOperation.getComputed(this.lhs, display);
        if (lb == null) {
            return null;
        }
        switch (this.op) {
            case AND: {
                Boolean rb = BooleanOperation.getComputed(this.rhs, display);
                if (rb == null) {
                    return null;
                }
                return lb != false && rb != false ? Literal.TRUE : Literal.FALSE;
            }
            case OR: {
                if (lb.booleanValue()) {
                    return Literal.TRUE;
                }
                Boolean rb = BooleanOperation.getComputed(this.rhs, display);
                if (rb == null) {
                    return null;
                }
                return rb != false ? Literal.TRUE : Literal.FALSE;
            }
        }
        return null;
    }
}

