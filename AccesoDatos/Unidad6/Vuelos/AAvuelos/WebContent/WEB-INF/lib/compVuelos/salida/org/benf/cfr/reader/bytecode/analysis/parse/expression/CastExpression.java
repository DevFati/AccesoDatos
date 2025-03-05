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
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.LiteralFolding;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaWildcardTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

public class CastExpression
extends AbstractExpression
implements BoxingProcessor {
    private Expression child;
    private boolean forced;

    public CastExpression(BytecodeLoc loc, InferredJavaType knownType, Expression child) {
        super(loc, knownType);
        InferredJavaType childInferredJavaType = child.getInferredJavaType();
        if (knownType.getJavaTypeInstance() == RawJavaType.LONG && childInferredJavaType.getJavaTypeInstance() == RawJavaType.BOOLEAN) {
            childInferredJavaType.forceType(RawJavaType.INT, true);
        }
        this.child = child;
        this.forced = false;
    }

    public CastExpression(BytecodeLoc loc, InferredJavaType knownType, Expression child, boolean forced) {
        super(loc, knownType);
        this.child = child;
        this.forced = forced;
    }

    public boolean isForced() {
        return this.forced;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.child);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new CastExpression(this.getLoc(), this.getInferredJavaType(), cloneHelper.replaceOrClone(this.child), this.forced);
    }

    @Override
    public Literal getComputedLiteral(Map<LValue, Literal> display) {
        if (!(this.getInferredJavaType().getJavaTypeInstance() instanceof RawJavaType)) {
            return null;
        }
        Literal computedChild = this.child.getComputedLiteral(display);
        if (computedChild == null) {
            return null;
        }
        return LiteralFolding.foldCast(computedChild, (RawJavaType)this.getInferredJavaType().getJavaTypeInstance());
    }

    public boolean couldBeImplicit(GenericTypeBinder gtb) {
        if (this.forced) {
            return false;
        }
        JavaTypeInstance childType = this.child.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance tgtType = this.getInferredJavaType().getJavaTypeInstance();
        return childType.implicitlyCastsTo(tgtType, gtb);
    }

    private boolean couldBeImplicit(JavaTypeInstance tgtType, GenericTypeBinder gtb) {
        if (this.forced) {
            return false;
        }
        JavaTypeInstance childType = this.child.getInferredJavaType().getJavaTypeInstance();
        return childType.implicitlyCastsTo(tgtType, gtb);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.getInferredJavaType().getJavaTypeInstance());
        this.child.collectTypeUsages(collector);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.UNARY_OTHER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        JavaTypeInstance castType = this.getInferredJavaType().getJavaTypeInstance();
        while (castType instanceof JavaWildcardTypeInstance) {
            castType = ((JavaWildcardTypeInstance)castType).getUnderlyingType();
        }
        if (castType.getInnerClassHereInfo().isAnonymousClass()) {
            d.dump(this.child);
            return d;
        }
        if (castType == RawJavaType.NULL) {
            this.child.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.NEITHER);
        } else {
            d.separator("(").dump(castType).separator(")");
            this.child.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.NEITHER);
        }
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        this.child = this.child.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.child = expressionRewriter.rewriteExpression(this.child, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        this.child.collectUsedLValues(lValueUsageCollector);
    }

    public Expression getChild() {
        return this.child;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CastExpression)) {
            return false;
        }
        CastExpression other = (CastExpression)o;
        if (!this.getInferredJavaType().getJavaTypeInstance().equals(other.getInferredJavaType().getJavaTypeInstance())) {
            return false;
        }
        return this.child.equals(other.child);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        JavaTypeInstance childType;
        if (this.isForced()) {
            return false;
        }
        JavaTypeInstance thisType = this.getInferredJavaType().getJavaTypeInstance();
        while (this.child instanceof CastExpression) {
            CastExpression childCast = (CastExpression)this.child;
            childType = childCast.getInferredJavaType().getJavaTypeInstance();
            Expression grandChild = childCast.child;
            JavaTypeInstance grandChildType = grandChild.getInferredJavaType().getJavaTypeInstance();
            if (Literal.NULL.equals(grandChild) && !thisType.isObject() && childType.isObject()) break;
            if (grandChildType.implicitlyCastsTo(childType, null) && childType.implicitlyCastsTo(thisType, null)) {
                this.child = childCast.child;
                continue;
            }
            if (!(grandChildType instanceof RawJavaType) || !(childType instanceof RawJavaType) || !(thisType instanceof RawJavaType) || grandChildType.implicitlyCastsTo(childType, null) || childType.implicitlyCastsTo(thisType, null)) break;
            this.child = childCast.child;
        }
        Expression newchild = boxingRewriter.sugarNonParameterBoxing(this.child, thisType);
        childType = this.child.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance newChildType = newchild.getInferredJavaType().getJavaTypeInstance();
        if (this.child != newchild && newChildType.implicitlyCastsTo(childType, null) && childType.implicitlyCastsTo(thisType, null) == newChildType.implicitlyCastsTo(thisType, null)) {
            this.child = newchild;
        }
        return false;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        CastExpression other = (CastExpression)o;
        if (!constraint.equivalent(this.getInferredJavaType().getJavaTypeInstance(), other.getInferredJavaType().getJavaTypeInstance())) {
            return false;
        }
        return constraint.equivalent(this.child, other.child);
    }

    public static Expression removeImplicit(Expression e) {
        while (e instanceof CastExpression && ((CastExpression)e).couldBeImplicit(null)) {
            e = ((CastExpression)e).getChild();
        }
        return e;
    }

    public static Expression removeImplicitOuterType(Expression e, GenericTypeBinder gtb, boolean rawArg) {
        JavaTypeInstance t = e.getInferredJavaType().getJavaTypeInstance();
        while (e instanceof CastExpression && ((CastExpression)e).couldBeImplicit(gtb) && ((CastExpression)e).couldBeImplicit(t, gtb)) {
            Expression newE = ((CastExpression)e).getChild();
            if (!rawArg) {
                boolean wasRaw = e.getInferredJavaType().getJavaTypeInstance() instanceof RawJavaType;
                boolean isRaw = newE.getInferredJavaType().getJavaTypeInstance() instanceof RawJavaType;
                if (wasRaw && !isRaw) break;
            }
            e = newE;
        }
        return e;
    }

    public static Expression tryRemoveCast(Expression e) {
        Expression ce;
        if (e instanceof CastExpression && (ce = ((CastExpression)e).getChild()).getInferredJavaType().getJavaTypeInstance().implicitlyCastsTo(e.getInferredJavaType().getJavaTypeInstance(), null)) {
            e = ce;
        }
        return e;
    }
}

