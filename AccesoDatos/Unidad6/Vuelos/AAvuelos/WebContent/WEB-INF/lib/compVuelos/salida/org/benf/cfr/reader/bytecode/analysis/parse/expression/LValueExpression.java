/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

public class LValueExpression
extends AbstractExpression {
    private LValue lValue;

    public LValueExpression(LValue lValue) {
        super(BytecodeLoc.NONE, lValue.getInferredJavaType());
        this.lValue = lValue;
    }

    public LValueExpression(BytecodeLoc loc, LValue lValue) {
        super(loc, lValue.getInferredJavaType());
        this.lValue = lValue;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new LValueExpression(this.getLoc(), cloneHelper.replaceOrClone(this.lValue));
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.lValue.collectTypeUsages(collector);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        Expression replacement;
        if (lValueRewriter.explicitlyReplaceThisLValue(this.lValue) && (replacement = lValueRewriter.getLValueReplacement(this.lValue, ssaIdentifiers, statementContainer)) != null) {
            return replacement;
        }
        this.lValue = this.lValue.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.lValue = expressionRewriter.rewriteExpression(this.lValue, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        return this.lValue.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.NEITHER);
    }

    public LValue getLValue() {
        return this.lValue;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        lValueUsageCollector.collect(this.lValue, ReadWrite.READ);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LValueExpression)) {
            return false;
        }
        return this.lValue.equals(((LValueExpression)o).getLValue());
    }

    public int hashCode() {
        return this.lValue.hashCode();
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return this.lValue.canThrow(caught);
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
        LValueExpression other = (LValueExpression)o;
        return constraint.equivalent(this.lValue, other.lValue);
    }

    @Override
    public Literal getComputedLiteral(Map<LValue, Literal> display) {
        return display.get(this.lValue);
    }

    public static Expression of(LValue lValue) {
        if (lValue instanceof StackSSALabel) {
            return new StackValue(BytecodeLoc.NONE, (StackSSALabel)lValue);
        }
        return new LValueExpression(lValue);
    }
}

