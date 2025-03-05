/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionCommon;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaIntersectionTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.output.Dumper;

public class LambdaExpression
extends AbstractExpression
implements LambdaExpressionCommon {
    private List<LValue> args;
    private List<JavaTypeInstance> explicitArgTypes;
    private Expression result;

    public LambdaExpression(BytecodeLoc loc, InferredJavaType castJavaType, List<LValue> args, List<JavaTypeInstance> explicitArgType, Expression result) {
        super(loc, castJavaType);
        this.args = args;
        this.explicitArgTypes = explicitArgType;
        this.result = result;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.result);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new LambdaExpression(this.getLoc(), this.getInferredJavaType(), cloneHelper.replaceOrClone(this.args), this.explicitArgTypes(), cloneHelper.replaceOrClone(this.result));
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(this.args);
        collector.collect(this.explicitArgTypes);
        this.result.collectTypeUsages(collector);
    }

    public void setExplicitArgTypes(List<JavaTypeInstance> types) {
        if (types == null || types.size() == this.args.size()) {
            this.explicitArgTypes = types;
        }
    }

    public List<JavaTypeInstance> explicitArgTypes() {
        return this.explicitArgTypes;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        for (int x = 0; x < this.args.size(); ++x) {
            this.args.set(x, expressionRewriter.rewriteExpression(this.args.get(x), ssaIdentifiers, statementContainer, flags));
        }
        this.result = expressionRewriter.rewriteExpression(this.result, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.result = expressionRewriter.rewriteExpression(this.result, ssaIdentifiers, statementContainer, flags);
        for (int x = this.args.size() - 1; x >= 0; --x) {
            this.args.set(x, expressionRewriter.rewriteExpression(this.args.get(x), ssaIdentifiers, statementContainer, flags));
        }
        return this;
    }

    @Override
    public boolean childCastForced() {
        return this.getInferredJavaType().getJavaTypeInstance() instanceof JavaIntersectionTypeInstance;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        boolean multi = this.args.size() != 1;
        boolean first = true;
        if (this.childCastForced()) {
            d.separator("(").dump(this.getInferredJavaType().getJavaTypeInstance()).separator(")");
        }
        if (this.explicitArgTypes != null && this.explicitArgTypes.size() == this.args.size()) {
            d.separator("(");
            for (int i = 0; i < this.args.size(); ++i) {
                LValue lValue = this.args.get(i);
                JavaTypeInstance explicitType = this.explicitArgTypes.get(i);
                first = StringUtils.comma(first, d);
                if (explicitType != null) {
                    d.dump(explicitType).print(" ");
                }
                d.dump(lValue);
            }
            d.separator(")");
        } else {
            if (multi) {
                d.separator("(");
            }
            for (LValue lValue : this.args) {
                first = StringUtils.comma(first, d);
                d.dump(lValue);
            }
            if (multi) {
                d.separator(")");
            }
        }
        d.print(" -> ").dump(this.result);
        d.removePendingCarriageReturn();
        return d;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        if (lValueUsageCollector instanceof LValueScopeDiscoverer && ((LValueScopeDiscoverer)lValueUsageCollector).descendLambdas()) {
            LValueScopeDiscoverer discover = (LValueScopeDiscoverer)lValueUsageCollector;
            this.result.collectUsedLValues(discover);
        }
    }

    public List<LValue> getArgs() {
        return this.args;
    }

    public Expression getResult() {
        return this.result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        LambdaExpression that = (LambdaExpression)o;
        if (this.args != null ? !this.args.equals(that.args) : that.args != null) {
            return false;
        }
        return !(this.result != null ? !this.result.equals(that.result) : that.result != null);
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
        LambdaExpression other = (LambdaExpression)o;
        if (!constraint.equivalent(this.args, other.args)) {
            return false;
        }
        return constraint.equivalent(this.result, other.result);
    }
}

