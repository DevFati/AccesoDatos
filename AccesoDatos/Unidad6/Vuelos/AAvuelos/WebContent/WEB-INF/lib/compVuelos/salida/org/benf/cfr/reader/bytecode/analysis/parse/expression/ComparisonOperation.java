/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NotOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ConditionalUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class ComparisonOperation
extends AbstractExpression
implements ConditionalExpression,
BoxingProcessor {
    private Expression lhs;
    private Expression rhs;
    private final CompOp op;
    private final boolean canNegate;

    public ComparisonOperation(BytecodeLoc loc, Expression lhs, Expression rhs, CompOp op) {
        this(loc, lhs, rhs, op, true);
    }

    public ComparisonOperation(BytecodeLoc loc, Expression lhs, Expression rhs, CompOp op, boolean canNegate) {
        super(loc, new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION));
        this.canNegate = canNegate;
        this.lhs = lhs;
        this.rhs = rhs;
        boolean lLiteral = lhs instanceof Literal;
        boolean rLiteral = rhs instanceof Literal;
        InferredJavaType.compareAsWithoutCasting(lhs.getInferredJavaType(), rhs.getInferredJavaType(), lLiteral, rLiteral);
        this.op = op;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new ComparisonOperation(this.getLoc(), cloneHelper.replaceOrClone(this.lhs), cloneHelper.replaceOrClone(this.rhs), this.op, this.canNegate);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.lhs, this.rhs);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.lhs.collectTypeUsages(collector);
        this.rhs.collectTypeUsages(collector);
    }

    @Override
    public int getSize(Precedence outerPrecedence) {
        return 3;
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
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        Expression res;
        if (lValueRewriter.needLR()) {
            this.lhs = this.lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
            this.rhs = this.rhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        } else {
            this.rhs = this.rhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
            this.lhs = this.lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        }
        if (this.lhs.canPushDownInto()) {
            if (this.rhs.canPushDownInto()) {
                throw new ConfusedCFRException("2 sides of a comparison support pushdown?");
            }
            Expression res2 = this.lhs.pushDown(this.rhs, this);
            if (res2 != null) {
                return res2;
            }
        } else if (this.rhs.canPushDownInto() && (res = this.rhs.pushDown(this.lhs, this.getNegated())) != null) {
            return res;
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

    @Override
    public ConditionalExpression getNegated() {
        if (!this.canNegate) {
            return new NotOperation(this.getLoc(), this);
        }
        return new ComparisonOperation(this.getLoc(), this.lhs, this.rhs, this.op.getInverted());
    }

    public CompOp getOp() {
        return this.op;
    }

    @Override
    public ConditionalExpression getDemorganApplied(boolean amNegating) {
        if (!amNegating) {
            return this;
        }
        return this.getNegated();
    }

    @Override
    public ConditionalExpression getRightDeep() {
        return this;
    }

    private void addIfLValue(Expression expression, Set<LValue> res) {
        if (expression instanceof LValueExpression) {
            res.add(((LValueExpression)expression).getLValue());
        }
    }

    @Override
    public Set<LValue> getLoopLValues() {
        Set<LValue> res = SetFactory.newSet();
        this.addIfLValue(this.lhs, res);
        this.addIfLValue(this.rhs, res);
        return res;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        this.lhs.collectUsedLValues(lValueUsageCollector);
        this.rhs.collectUsedLValues(lValueUsageCollector);
    }

    private static BooleanComparisonType isBooleanComparison(Expression a, Expression b, CompOp op) {
        switch (op) {
            case EQ: 
            case NE: {
                break;
            }
            default: {
                return BooleanComparisonType.NOT;
            }
        }
        if (a.getInferredJavaType().getJavaTypeInstance().getRawTypeOfSimpleType() != RawJavaType.BOOLEAN) {
            return BooleanComparisonType.NOT;
        }
        if (!(b instanceof Literal)) {
            return BooleanComparisonType.NOT;
        }
        Literal literal = (Literal)b;
        TypedLiteral lit = literal.getValue();
        if (lit.getType() != TypedLiteral.LiteralType.Integer) {
            return BooleanComparisonType.NOT;
        }
        int i = (Integer)lit.getValue();
        if (i < 0 || i > 1) {
            return BooleanComparisonType.NOT;
        }
        if (op == CompOp.NE) {
            i = 1 - i;
        }
        if (i == 0) {
            return BooleanComparisonType.NEGATED;
        }
        return BooleanComparisonType.AS_IS;
    }

    private ConditionalExpression getConditionalExpression(Expression booleanExpression, BooleanComparisonType booleanComparisonType) {
        ConditionalExpression res = booleanExpression instanceof ConditionalExpression ? (ConditionalExpression)booleanExpression : new BooleanExpression(booleanExpression);
        if (booleanComparisonType == BooleanComparisonType.NEGATED) {
            res = res.getNegated();
        }
        return res;
    }

    @Override
    public ConditionalExpression optimiseForType() {
        BooleanComparisonType bct = ComparisonOperation.isBooleanComparison(this.lhs, this.rhs, this.op);
        if (bct.isValid()) {
            return this.getConditionalExpression(this.lhs, bct);
        }
        bct = ComparisonOperation.isBooleanComparison(this.rhs, this.lhs, this.op);
        if (bct.isValid()) {
            return this.getConditionalExpression(this.rhs, bct);
        }
        return this;
    }

    public Expression getLhs() {
        return this.lhs;
    }

    public Expression getRhs() {
        return this.rhs;
    }

    @Override
    public ConditionalExpression simplify() {
        return ConditionalUtils.simplify(this);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        switch (this.op) {
            case EQ: 
            case NE: {
                if (boxingRewriter.isUnboxedType(this.lhs)) {
                    this.rhs = boxingRewriter.sugarUnboxing(this.rhs);
                    return false;
                }
                if (!boxingRewriter.isUnboxedType(this.rhs)) break;
                this.lhs = boxingRewriter.sugarUnboxing(this.lhs);
                return false;
            }
            default: {
                this.lhs = boxingRewriter.sugarUnboxing(this.lhs);
                this.rhs = boxingRewriter.sugarUnboxing(this.rhs);
            }
        }
        return false;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ComparisonOperation)) {
            return false;
        }
        ComparisonOperation other = (ComparisonOperation)o;
        return this.op == other.op && this.lhs.equals(other.lhs) && this.rhs.equals(other.rhs);
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return this.lhs.canThrow(caught) || this.rhs.canThrow(caught);
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
        ComparisonOperation other = (ComparisonOperation)o;
        if (!constraint.equivalent((Object)this.op, (Object)other.op)) {
            return false;
        }
        if (!constraint.equivalent(this.lhs, other.lhs)) {
            return false;
        }
        return constraint.equivalent(this.rhs, other.rhs);
    }

    @Override
    public Literal getComputedLiteral(Map<LValue, Literal> display) {
        Literal lV = this.lhs.getComputedLiteral(display);
        Literal rV = this.rhs.getComputedLiteral(display);
        if (lV == null || rV == null) {
            return null;
        }
        TypedLiteral l = lV.getValue();
        TypedLiteral r = rV.getValue();
        switch (this.op) {
            case EQ: {
                return l.equals(r) ? Literal.TRUE : Literal.FALSE;
            }
            case NE: {
                return l.equals(r) ? Literal.FALSE : Literal.TRUE;
            }
        }
        JavaTypeInstance type = l.getInferredJavaType().getJavaTypeInstance();
        if (!type.equals(r.getInferredJavaType().getJavaTypeInstance())) {
            return null;
        }
        if (type.getStackType() == StackType.INT) {
            int lv = l.getIntValue();
            int rv = r.getIntValue();
            switch (this.op) {
                case LT: {
                    return lv < rv ? Literal.TRUE : Literal.FALSE;
                }
                case LTE: {
                    return lv <= rv ? Literal.TRUE : Literal.FALSE;
                }
                case GT: {
                    return lv > rv ? Literal.TRUE : Literal.FALSE;
                }
                case GTE: {
                    return lv >= rv ? Literal.TRUE : Literal.FALSE;
                }
            }
        }
        return null;
    }

    private static enum BooleanComparisonType {
        NOT(false),
        AS_IS(true),
        NEGATED(true);

        private final boolean isValid;

        private BooleanComparisonType(boolean isValid) {
            this.isValid = isValid;
        }

        public boolean isValid() {
            return this.isValid;
        }
    }
}

