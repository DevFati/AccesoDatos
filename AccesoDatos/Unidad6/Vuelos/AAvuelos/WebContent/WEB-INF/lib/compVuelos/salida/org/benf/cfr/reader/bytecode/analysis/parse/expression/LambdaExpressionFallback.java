/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionCommon;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class LambdaExpressionFallback
extends AbstractExpression
implements LambdaExpressionCommon {
    private JavaTypeInstance callClassType;
    private MethodPrototype lambdaFn;
    private List<JavaTypeInstance> targetFnArgTypes;
    private List<Expression> curriedArgs;
    private boolean instance;
    private final boolean methodRef;

    private String lambdaFnName() {
        String lambdaFnName = this.lambdaFn.getName();
        return lambdaFnName.equals("<init>") ? "new" : lambdaFnName;
    }

    public LambdaExpressionFallback(BytecodeLoc loc, JavaTypeInstance callClassType, InferredJavaType castJavaType, MethodPrototype lambdaFn, List<JavaTypeInstance> targetFnArgTypes, List<Expression> curriedArgs, boolean instance) {
        super(loc, castJavaType);
        this.callClassType = callClassType;
        this.lambdaFn = lambdaFn;
        this.targetFnArgTypes = targetFnArgTypes;
        this.curriedArgs = curriedArgs;
        this.instance = instance;
        boolean isMethodRef = false;
        switch (curriedArgs.size()) {
            case 0: {
                isMethodRef = true;
                if (!instance) break;
                this.instance = false;
                break;
            }
            case 1: {
                if (!instance || !lambdaFn.isInstanceMethod()) break;
                JavaTypeInstance thisType = lambdaFn.getClassType().getDeGenerifiedType();
                JavaTypeInstance curriedType = curriedArgs.get(0).getInferredJavaType().getJavaTypeInstance().getDeGenerifiedType();
                if (!curriedType.implicitlyCastsTo(thisType, null)) break;
                isMethodRef = true;
            }
        }
        this.methodRef = isMethodRef;
    }

    private LambdaExpressionFallback(BytecodeLoc loc, InferredJavaType inferredJavaType, boolean methodRef, boolean instance, List<Expression> curriedArgs, List<JavaTypeInstance> targetFnArgTypes, MethodPrototype lambdaFn, JavaTypeInstance callClassType) {
        super(loc, inferredJavaType);
        this.methodRef = methodRef;
        this.instance = instance;
        this.curriedArgs = curriedArgs;
        this.targetFnArgTypes = targetFnArgTypes;
        this.lambdaFn = lambdaFn;
        this.callClassType = callClassType;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.curriedArgs, new HasByteCodeLoc[0]);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new LambdaExpressionFallback(this.getLoc(), this.getInferredJavaType(), this.methodRef, this.instance, cloneHelper.replaceOrClone(this.curriedArgs), this.targetFnArgTypes, this.lambdaFn, this.callClassType);
    }

    @Override
    public boolean childCastForced() {
        return false;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.targetFnArgTypes);
        collector.collectFrom(this.curriedArgs);
        collector.collect(this.callClassType);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(this.curriedArgs, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyBackwards(this.curriedArgs, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.LAMBDA;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        boolean special;
        String name = this.lambdaFnName();
        boolean bl = special = name == "new";
        if (this.methodRef) {
            if (this.instance) {
                this.curriedArgs.get(0).dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.TRUE).print("::").methodName(name, this.lambdaFn, special, false);
            } else {
                d.dump(this.callClassType).print("::").methodName(name, this.lambdaFn, special, false);
            }
        } else {
            int x;
            boolean multi;
            int n = this.targetFnArgTypes.size();
            boolean bl2 = multi = n != 1;
            if (multi) {
                d.separator("(");
            }
            List args = ListFactory.newList(n);
            for (int x2 = 0; x2 < n; ++x2) {
                if (x2 > 0) {
                    d.separator(", ");
                }
                String arg = "arg_" + x2;
                args.add(arg);
                d.identifier(arg, arg, true);
            }
            if (multi) {
                d.separator(")");
            }
            d.operator(" -> ");
            if (this.instance) {
                this.curriedArgs.get(0).dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.TRUE).separator(".").methodName(name, this.lambdaFn, special, false);
            } else {
                d.dump(this.callClassType).print('.').methodName(name, this.lambdaFn, special, false);
            }
            d.separator("(");
            boolean first = true;
            int cnt = this.curriedArgs.size();
            for (x = this.instance ? 1 : 0; x < cnt; ++x) {
                Expression c = this.curriedArgs.get(x);
                first = StringUtils.comma(first, d);
                d.dump(c);
            }
            for (x = 0; x < n; ++x) {
                first = StringUtils.comma(first, d);
                String arg = (String)args.get(x);
                d.identifier(arg, arg, false);
            }
            d.separator(")");
        }
        return d;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        LambdaExpressionFallback that = (LambdaExpressionFallback)o;
        if (this.methodRef != that.methodRef) {
            return false;
        }
        if (this.instance != that.instance) {
            return false;
        }
        if (this.callClassType != null ? !this.callClassType.equals(that.callClassType) : that.callClassType != null) {
            return false;
        }
        if (this.curriedArgs != null ? !this.curriedArgs.equals(that.curriedArgs) : that.curriedArgs != null) {
            return false;
        }
        if (this.lambdaFn != null ? !this.lambdaFn.equals(that.lambdaFn) : that.lambdaFn != null) {
            return false;
        }
        return !(this.targetFnArgTypes != null ? !this.targetFnArgTypes.equals(that.targetFnArgTypes) : that.targetFnArgTypes != null);
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
        LambdaExpressionFallback other = (LambdaExpressionFallback)o;
        if (this.instance != other.instance) {
            return false;
        }
        if (this.methodRef != other.methodRef) {
            return false;
        }
        if (!constraint.equivalent(this.lambdaFn, other.lambdaFn)) {
            return false;
        }
        return constraint.equivalent(this.curriedArgs, other.curriedArgs);
    }
}

