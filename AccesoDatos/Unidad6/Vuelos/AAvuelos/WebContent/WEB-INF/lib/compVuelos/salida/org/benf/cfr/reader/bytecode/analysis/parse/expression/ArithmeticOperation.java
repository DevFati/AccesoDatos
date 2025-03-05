/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMutatingAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticMonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticPreMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ComparisonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NotOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.LiteralFolding;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionVisitor;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.BasicExceptions;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

public class ArithmeticOperation
extends AbstractExpression
implements BoxingProcessor {
    private Expression lhs;
    private Expression rhs;
    private final ArithOp op;

    public ArithmeticOperation(BytecodeLoc loc, Expression lhs, Expression rhs, ArithOp op) {
        super(loc, ArithmeticOperation.inferredType(lhs.getInferredJavaType(), rhs.getInferredJavaType(), op));
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
    }

    public ArithmeticOperation(BytecodeLoc loc, InferredJavaType knownType, Expression lhs, Expression rhs, ArithOp op) {
        super(loc, knownType);
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
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
    public Expression deepClone(CloneHelper cloneHelper) {
        return new ArithmeticOperation(this.getLoc(), cloneHelper.replaceOrClone(this.lhs), cloneHelper.replaceOrClone(this.rhs), this.op);
    }

    private static InferredJavaType inferredType(InferredJavaType a, InferredJavaType b, ArithOp op) {
        InferredJavaType.useInArithOp(a, b, op);
        RawJavaType rawJavaType = a.getRawType();
        if (rawJavaType.getStackType().equals((Object)StackType.INT)) {
            switch (op) {
                case AND: 
                case OR: 
                case XOR: {
                    if (rawJavaType.equals(RawJavaType.BOOLEAN)) break;
                }
                default: {
                    rawJavaType = RawJavaType.INT;
                }
            }
        }
        return new InferredJavaType(rawJavaType, InferredJavaType.Source.OPERATION);
    }

    @Override
    public Precedence getPrecedence() {
        return this.op.getPrecedence();
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        this.lhs.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.TRUE);
        d.operator(" " + this.op.getShowAs() + " ");
        this.rhs.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.FALSE);
        return d;
    }

    @Override
    public Literal getComputedLiteral(Map<LValue, Literal> display) {
        if (!(this.getInferredJavaType().getJavaTypeInstance() instanceof RawJavaType)) {
            return null;
        }
        Literal l = this.lhs.getComputedLiteral(display);
        if (l == null) {
            return null;
        }
        Literal r = this.rhs.getComputedLiteral(display);
        if (r == null) {
            return null;
        }
        return LiteralFolding.foldArithmetic((RawJavaType)this.getInferredJavaType().getJavaTypeInstance(), l, r, this.op);
    }

    private boolean isLValueExprFor(LValueExpression expression, LValue lValue) {
        LValue contained = expression.getLValue();
        return lValue.equals(contained);
    }

    public boolean isLiteralFunctionOf(LValue lValue) {
        if (this.lhs instanceof LValueExpression && this.rhs instanceof Literal) {
            return this.isLValueExprFor((LValueExpression)this.lhs, lValue);
        }
        if (this.rhs instanceof LValueExpression && this.lhs instanceof Literal) {
            return this.isLValueExprFor((LValueExpression)this.rhs, lValue);
        }
        return false;
    }

    public boolean isXorM1() {
        return this.op == ArithOp.XOR && this.rhs.equals(Literal.MINUS_ONE);
    }

    public Expression getReplacementXorM1() {
        return new ArithmeticMonOperation(this.getLoc(), this.lhs, ArithOp.NEG);
    }

    public boolean isMutationOf(LValue lValue) {
        if (!(this.lhs instanceof LValueExpression)) {
            return false;
        }
        if (!this.isLValueExprFor((LValueExpression)this.lhs, lValue)) {
            return false;
        }
        return !this.op.isTemporary();
    }

    public AbstractMutatingAssignmentExpression getMutationOf(LValue lValue) {
        if (!this.isMutationOf(lValue)) {
            throw new ConfusedCFRException("Can't get a mutation where none exists");
        }
        if (this.lhs.getInferredJavaType().getJavaTypeInstance() != RawJavaType.BOOLEAN && Literal.equalsAnyOne(this.rhs)) {
            switch (this.op) {
                case PLUS: 
                case MINUS: {
                    return new ArithmeticPreMutationOperation(this.getLoc(), lValue, this.op);
                }
            }
        }
        return new ArithmeticMutationOperation(this.getLoc(), lValue, this.rhs, this.op);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        if (lValueRewriter.needLR()) {
            this.lhs = this.lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
            this.rhs = this.rhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        } else {
            this.rhs = this.rhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
            this.lhs = this.lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
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
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        this.lhs.collectUsedLValues(lValueUsageCollector);
        this.rhs.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public boolean canPushDownInto() {
        return this.op.isTemporary();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ArithmeticOperation)) {
            return false;
        }
        ArithmeticOperation other = (ArithmeticOperation)o;
        if (this.op != other.op) {
            return false;
        }
        if (!this.lhs.equals(other.lhs)) {
            return false;
        }
        return this.rhs.equals(other.rhs);
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
        ArithmeticOperation other = (ArithmeticOperation)o;
        if (this.op != other.op) {
            return false;
        }
        if (!constraint.equivalent(this.lhs, other.lhs)) {
            return false;
        }
        return constraint.equivalent(this.rhs, other.rhs);
    }

    private static boolean returnsTrueForNaN(CompOp from, int on, boolean nanG) {
        if (on == 0) {
            if (nanG) {
                switch (from) {
                    case GTE: 
                    case GT: {
                        return true;
                    }
                }
            } else {
                switch (from) {
                    case LT: 
                    case LTE: {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean canNegateAroundNaN(CompOp from, int on) {
        if (on == 0) {
            switch (from) {
                case EQ: 
                case NE: {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private static CompOp rewriteXCMPCompOp(CompOp from, int on) {
        if (on == 0) {
            return from;
        }
        if (on < 0) {
            switch (from) {
                case LT: {
                    throw new IllegalStateException("Bad CMP");
                }
                case LTE: {
                    return CompOp.LT;
                }
                case GTE: {
                    throw new IllegalStateException("Bad CMP");
                }
                case GT: {
                    return CompOp.GTE;
                }
                case EQ: {
                    return CompOp.LT;
                }
                case NE: {
                    return CompOp.GTE;
                }
            }
            throw new IllegalStateException("Unknown enum");
        }
        switch (from) {
            case LT: {
                return CompOp.LTE;
            }
            case LTE: {
                throw new IllegalStateException("Bad CMP");
            }
            case GTE: {
                return CompOp.GT;
            }
            case GT: {
                throw new IllegalStateException("Bad CMP");
            }
            case EQ: {
                return CompOp.GT;
            }
            case NE: {
                return CompOp.LTE;
            }
        }
        throw new IllegalStateException("Unknown enum");
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return this.lhs.canThrow(caught) || this.rhs.canThrow(caught) || this.op.canThrow(this.getInferredJavaType(), caught, BasicExceptions.instances);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        this.lhs = boxingRewriter.sugarUnboxing(this.lhs);
        this.rhs = boxingRewriter.sugarUnboxing(this.rhs);
        return false;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
    }

    public Expression getLhs() {
        return this.lhs;
    }

    public Expression getRhs() {
        return this.rhs;
    }

    public ArithOp getOp() {
        return this.op;
    }

    @Override
    public <T> T visit(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Expression pushDown(Expression toPush, Expression parent) {
        if (!(parent instanceof ComparisonOperation)) {
            return null;
        }
        if (!this.op.isTemporary()) {
            return null;
        }
        if (!(toPush instanceof Literal)) {
            throw new ConfusedCFRException("Pushing with a non-literal as pushee.");
        }
        ComparisonOperation comparisonOperation = (ComparisonOperation)parent;
        CompOp compOp = comparisonOperation.getOp();
        Literal literal = (Literal)toPush;
        TypedLiteral typedLiteral = literal.getValue();
        if (typedLiteral.getType() != TypedLiteral.LiteralType.Integer) {
            throw new ConfusedCFRException("<xCMP> , non integer!");
        }
        int litVal = (Integer)typedLiteral.getValue();
        switch (litVal) {
            case -1: 
            case 0: 
            case 1: {
                break;
            }
            default: {
                throw new ConfusedCFRException("Invalid literal value " + litVal + " in xCMP");
            }
        }
        boolean acceptsNaN = false;
        boolean canNegate = true;
        switch (this.op) {
            case DCMPG: 
            case FCMPG: {
                acceptsNaN = ArithmeticOperation.returnsTrueForNaN(compOp, litVal, true);
                canNegate = ArithmeticOperation.canNegateAroundNaN(compOp, litVal);
                break;
            }
            case DCMPL: 
            case FCMPL: {
                acceptsNaN = ArithmeticOperation.returnsTrueForNaN(compOp, litVal, false);
                canNegate = ArithmeticOperation.canNegateAroundNaN(compOp, litVal);
                break;
            }
            case LCMP: {
                break;
            }
            default: {
                throw new ConfusedCFRException("Shouldn't be here.");
            }
        }
        compOp = ArithmeticOperation.rewriteXCMPCompOp(compOp, litVal);
        if (acceptsNaN) {
            AbstractExpression comp = new ComparisonOperation(this.getLoc(), this.lhs, this.rhs, compOp.getInverted(), false);
            comp = new NotOperation(this.getLoc(), (ConditionalExpression)((Object)comp));
            return comp;
        }
        return new ComparisonOperation(this.getLoc(), this.lhs, this.rhs, compOp, canNegate);
    }
}

