/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Collections;
import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionFallback;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.FormalTypeParameter;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.state.TypeUsageCollector;

public abstract class AbstractConstructorInvokation
extends AbstractExpression
implements BoxingProcessor {
    private final ConstantPoolEntryMethodRef function;
    private final MethodPrototype methodPrototype;
    private final List<Expression> args;

    AbstractConstructorInvokation(BytecodeLoc loc, InferredJavaType inferredJavaType, ConstantPoolEntryMethodRef function, List<Expression> args) {
        super(loc, inferredJavaType);
        this.args = args;
        this.function = function;
        this.methodPrototype = function.getMethodPrototype();
    }

    AbstractConstructorInvokation(BytecodeLoc loc, AbstractConstructorInvokation other, CloneHelper cloneHelper) {
        super(loc, other.getInferredJavaType());
        this.args = cloneHelper.replaceOrClone(other.args);
        this.function = other.function;
        this.methodPrototype = other.methodPrototype;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.methodPrototype.collectTypeUsages(collector);
        for (Expression arg : this.args) {
            arg.collectTypeUsages(collector);
        }
        super.collectTypeUsages(collector);
    }

    public List<Expression> getArgs() {
        return this.args;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        LValueRewriter.Util.rewriteArgArray(lValueRewriter, ssaIdentifiers, statementContainer, this.args);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(this.args, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyBackwards(this.args, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    public JavaTypeInstance getTypeInstance() {
        return this.getInferredJavaType().getJavaTypeInstance();
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        for (Expression expression : this.args) {
            expression.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof AbstractConstructorInvokation)) {
            return false;
        }
        AbstractConstructorInvokation other = (AbstractConstructorInvokation)o;
        if (!this.getTypeInstance().equals(other.getTypeInstance())) {
            return false;
        }
        return this.args.equals(other.args);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof AbstractConstructorInvokation)) {
            return false;
        }
        AbstractConstructorInvokation other = (AbstractConstructorInvokation)o;
        if (!constraint.equivalent(this.getTypeInstance(), other.getTypeInstance())) {
            return false;
        }
        return constraint.equivalent(this.args, other.args);
    }

    final OverloadMethodSet getOverloadMethodSet() {
        OverloadMethodSet overloadMethodSet = this.methodPrototype.getOverloadMethodSet();
        if (overloadMethodSet == null) {
            return null;
        }
        JavaTypeInstance objectType = this.getInferredJavaType().getJavaTypeInstance();
        if (objectType instanceof JavaGenericRefTypeInstance) {
            JavaGenericRefTypeInstance genericType = (JavaGenericRefTypeInstance)objectType;
            return overloadMethodSet.specialiseTo(genericType);
        }
        return overloadMethodSet;
    }

    @Override
    public boolean isValidStatement() {
        return true;
    }

    protected final MethodPrototype getMethodPrototype() {
        return this.methodPrototype;
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        List<Expression> args = this.getArgs();
        if (args.isEmpty()) {
            return false;
        }
        OverloadMethodSet overloadMethodSet = this.getOverloadMethodSet();
        if (overloadMethodSet == null) {
            boxingRewriter.removeRedundantCastOnly(args);
            return false;
        }
        GenericTypeBinder gtb = this.methodPrototype.getTypeBinderFor(args);
        boolean callsCorrectEntireMethod = overloadMethodSet.callsCorrectEntireMethod(args, gtb);
        for (int x = 0; x < args.size(); ++x) {
            Expression arg = args.get(x);
            if (!callsCorrectEntireMethod && !overloadMethodSet.callsCorrectMethod(arg, x, null)) {
                JavaTypeInstance argType = overloadMethodSet.getArgType(x, arg.getInferredJavaType().getJavaTypeInstance());
                boolean ignore = false;
                if (argType instanceof JavaGenericBaseInstance) {
                    ignore = ((JavaGenericBaseInstance)argType).hasForeignUnbound(this.function.getCp(), 0, false, Collections.<String, FormalTypeParameter>emptyMap());
                }
                if (!ignore) {
                    boolean bl = ignore = arg instanceof LambdaExpression || arg instanceof LambdaExpressionFallback;
                }
                if (!ignore) {
                    arg = new CastExpression(BytecodeLoc.NONE, new InferredJavaType(argType, InferredJavaType.Source.EXPRESSION, true), arg);
                }
            }
            arg = boxingRewriter.rewriteExpression(arg, null, null, null);
            arg = boxingRewriter.sugarParameterBoxing(arg, x, overloadMethodSet, null, this.methodPrototype);
            args.set(x, arg);
        }
        return true;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
    }
}

