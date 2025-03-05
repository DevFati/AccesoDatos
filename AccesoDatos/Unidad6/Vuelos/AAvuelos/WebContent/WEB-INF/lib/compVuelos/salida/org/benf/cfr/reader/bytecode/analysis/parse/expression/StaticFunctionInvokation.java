/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.VarArgsRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.FunctionProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.annotation.Nullable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.TypeContext;

public class StaticFunctionInvokation
extends AbstractFunctionInvokation
implements FunctionProcessor,
BoxingProcessor {
    protected final List<Expression> args;
    private final JavaTypeInstance clazz;
    private Expression object;
    @Nullable
    private List<JavaTypeInstance> explicitGenerics;

    private static InferredJavaType getTypeForFunction(ConstantPoolEntryMethodRef function, List<Expression> args) {
        return new InferredJavaType(function.getMethodPrototype().getReturnType(function.getClassEntry().getTypeInstance(), args), InferredJavaType.Source.FUNCTION, true);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new StaticFunctionInvokation(this.getLoc(), this.getFunction(), cloneHelper.replaceOrClone(this.args), cloneHelper.replaceOrClone(this.object));
    }

    private StaticFunctionInvokation(BytecodeLoc loc, ConstantPoolEntryMethodRef function, List<Expression> args, Expression object) {
        super(loc, function, StaticFunctionInvokation.getTypeForFunction(function, args));
        this.args = args;
        this.clazz = function.getClassEntry().getTypeInstance();
        this.object = object;
    }

    public StaticFunctionInvokation(BytecodeLoc loc, ConstantPoolEntryMethodRef function, List<Expression> args) {
        this(loc, function, args, null);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.args, this.object);
    }

    public void forceObject(Expression object) {
        this.object = object;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (this.object != null) {
            this.object.collectTypeUsages(collector);
        }
        collector.collect(this.clazz);
        for (Expression arg : this.args) {
            arg.collectTypeUsages(collector);
        }
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        LValueRewriter.Util.rewriteArgArray(lValueRewriter, ssaIdentifiers, statementContainer, this.args);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.applyNonArgExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        this.applyExpressionRewriterToArgs(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyBackwards(this.args, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        this.applyNonArgExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void applyExpressionRewriterToArgs(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(this.args, expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public void setExplicitGenerics(List<JavaTypeInstance> types) {
        this.explicitGenerics = types;
    }

    @Override
    public List<JavaTypeInstance> getExplicitGenerics() {
        return this.explicitGenerics;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        boolean first;
        if (this.object != null) {
            d.dump(this.object).separator(".");
        } else {
            if (!d.getTypeUsageInformation().isStaticImport(this.clazz.getDeGenerifiedType(), this.getFixedName())) {
                d.dump(this.clazz, TypeContext.Static).separator(".");
            }
            if (this.explicitGenerics != null && !this.explicitGenerics.isEmpty()) {
                d.operator("<");
                first = true;
                for (JavaTypeInstance typeInstance : this.explicitGenerics) {
                    first = StringUtils.comma(first, d);
                    d.dump(typeInstance);
                }
                d.operator(">");
            }
        }
        d.methodName(this.getFixedName(), this.getMethodPrototype(), false, false).separator("(");
        first = true;
        for (Expression arg : this.args) {
            first = StringUtils.comma(first, d);
            d.dump(arg);
        }
        d.separator(")");
        return d;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        for (Expression expression : this.args) {
            expression.collectUsedLValues(lValueUsageCollector);
        }
    }

    public JavaTypeInstance getClazz() {
        return this.clazz;
    }

    @Override
    public List<Expression> getArgs() {
        return this.args;
    }

    @Override
    public void rewriteVarArgs(VarArgsRewriter varArgsRewriter) {
        MethodPrototype methodPrototype = this.getMethodPrototype();
        if (!methodPrototype.isVarArgs()) {
            return;
        }
        OverloadMethodSet overloadMethodSet = methodPrototype.getOverloadMethodSet();
        if (overloadMethodSet == null) {
            return;
        }
        GenericTypeBinder gtb = methodPrototype.getTypeBinderFor(this.args);
        varArgsRewriter.rewriteVarArgsArg(overloadMethodSet, methodPrototype, this.getArgs(), gtb);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        OverloadMethodSet overloadMethodSet = this.getMethodPrototype().getOverloadMethodSet();
        if (overloadMethodSet == null) {
            return false;
        }
        for (int x = 0; x < this.args.size(); ++x) {
            Expression arg = this.args.get(x);
            arg = boxingRewriter.rewriteExpression(arg, null, null, null);
            this.args.set(x, boxingRewriter.sugarParameterBoxing(arg, x, overloadMethodSet, null, this.getFunction().getMethodPrototype()));
        }
        return true;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (this.object != null) {
            this.object = expressionRewriter.rewriteExpression(this.object, ssaIdentifiers, statementContainer, flags);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof StaticFunctionInvokation)) {
            return false;
        }
        StaticFunctionInvokation other = (StaticFunctionInvokation)o;
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        if (!this.clazz.equals(other.clazz)) {
            return false;
        }
        return this.args.equals(other.args);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof StaticFunctionInvokation)) {
            return false;
        }
        StaticFunctionInvokation other = (StaticFunctionInvokation)o;
        if (!constraint.equivalent(this.getName(), other.getName())) {
            return false;
        }
        if (!constraint.equivalent(this.clazz, other.clazz)) {
            return false;
        }
        return constraint.equivalent(this.args, other.args);
    }
}

