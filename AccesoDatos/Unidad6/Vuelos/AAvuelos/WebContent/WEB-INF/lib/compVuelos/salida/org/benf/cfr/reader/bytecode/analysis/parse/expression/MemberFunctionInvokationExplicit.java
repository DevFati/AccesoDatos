/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractFunctionInvokationExplicit;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.output.Dumper;

public class MemberFunctionInvokationExplicit
extends AbstractFunctionInvokationExplicit {
    private Expression object;

    MemberFunctionInvokationExplicit(BytecodeLoc loc, InferredJavaType res, JavaTypeInstance clazz, Expression object, String method, List<Expression> args) {
        super(loc, res, clazz, method, args);
        this.object = object;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.object);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof MemberFunctionInvokationExplicit)) {
            return false;
        }
        MemberFunctionInvokationExplicit other = (MemberFunctionInvokationExplicit)o;
        return this.getClazz().equals(other.getClazz()) && this.object.equals(other.object) && this.getMethod().equals(other.getMethod()) && this.getArgs().equals(other.getArgs());
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.dump(this.object).separator(".").print(this.getMethod()).separator("(");
        boolean first = true;
        for (Expression arg : this.getArgs()) {
            first = StringUtils.comma(first, d);
            d.dump(arg);
        }
        d.separator(")");
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        this.object = this.object.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return super.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.object = this.object.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        super.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        super.applyReverseExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        this.object = this.object.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        this.object.collectUsedLValues(lValueUsageCollector);
        super.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof MemberFunctionInvokationExplicit)) {
            return false;
        }
        MemberFunctionInvokationExplicit other = (MemberFunctionInvokationExplicit)o;
        if (!constraint.equivalent(this.object, other.object)) {
            return false;
        }
        if (!constraint.equivalent(this.getMethod(), other.getMethod())) {
            return false;
        }
        if (!constraint.equivalent(this.getClazz(), other.getClazz())) {
            return false;
        }
        return constraint.equivalent(this.getArgs(), other.getArgs());
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new MemberFunctionInvokationExplicit(this.getLoc(), this.getInferredJavaType(), this.getClazz(), this.object, this.getMethod(), cloneHelper.replaceOrClone(this.getArgs()));
    }
}

