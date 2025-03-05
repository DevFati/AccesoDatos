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
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.LiteralFolding;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

public class ArithmeticMonOperation
extends AbstractExpression {
    private Expression lhs;
    private final ArithOp op;

    private static InferredJavaType inferredType(InferredJavaType orig) {
        if (orig.getJavaTypeInstance() != RawJavaType.BOOLEAN) {
            return orig;
        }
        InferredJavaType res = new InferredJavaType(RawJavaType.INT, InferredJavaType.Source.OPERATION);
        orig.useInArithOp(res, RawJavaType.INT, true);
        return res;
    }

    public ArithmeticMonOperation(BytecodeLoc loc, Expression lhs, ArithOp op) {
        super(loc, ArithmeticMonOperation.inferredType(lhs.getInferredJavaType()));
        this.lhs = lhs;
        this.op = op;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.lhs);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.lhs.collectTypeUsages(collector);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new ArithmeticMonOperation(this.getLoc(), cloneHelper.replaceOrClone(this.lhs), this.op);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.UNARY_OTHER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.print(this.op.getShowAs());
        this.lhs.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.NEITHER);
        return d;
    }

    @Override
    public Literal getComputedLiteral(Map<LValue, Literal> display) {
        Literal l = this.lhs.getComputedLiteral(display);
        if (l == null) {
            return null;
        }
        if (!(this.getInferredJavaType().getJavaTypeInstance() instanceof RawJavaType)) {
            return null;
        }
        return LiteralFolding.foldArithmetic((RawJavaType)this.getInferredJavaType().getJavaTypeInstance(), l, this.op);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        this.lhs = this.lhs.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.lhs = expressionRewriter.rewriteExpression(this.lhs, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        this.lhs.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ArithmeticMonOperation that = (ArithmeticMonOperation)o;
        if (this.lhs != null ? !this.lhs.equals(that.lhs) : that.lhs != null) {
            return false;
        }
        return this.op == that.op;
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ArithmeticMonOperation other = (ArithmeticMonOperation)o;
        if (!constraint.equivalent(this.lhs, other.lhs)) {
            return false;
        }
        return constraint.equivalent((Object)this.op, (Object)other.op);
    }
}

