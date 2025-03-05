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
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionFallback;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.FunctionProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.state.TypeUsageCollector;

public abstract class AbstractMemberFunctionInvokation
extends AbstractFunctionInvokation
implements FunctionProcessor,
BoxingProcessor {
    private final ConstantPool cp;
    private final List<Expression> args;
    private Expression object;
    private final List<Boolean> nulls;

    AbstractMemberFunctionInvokation(BytecodeLoc loc, ConstantPool cp, ConstantPoolEntryMethodRef function, Expression object, JavaTypeInstance bestType, List<Expression> args, List<Boolean> nulls) {
        super(loc, function, new InferredJavaType(function.getMethodPrototype().getReturnType(bestType, args), InferredJavaType.Source.FUNCTION, true));
        this.object = object;
        this.args = args;
        this.nulls = nulls;
        this.cp = cp;
    }

    AbstractMemberFunctionInvokation(BytecodeLoc loc, ConstantPool cp, ConstantPoolEntryMethodRef function, Expression object, List<Expression> args, List<Boolean> nulls) {
        this(loc, cp, function, object, object.getInferredJavaType().getJavaTypeInstance(), args, nulls);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.args, new HasByteCodeLoc[0]);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (Expression arg : this.args) {
            arg.collectTypeUsages(collector);
        }
        this.getMethodPrototype().collectTypeUsages(collector);
        collector.collectFrom(this.object);
        super.collectTypeUsages(collector);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        if (lValueRewriter.needLR()) {
            this.object = this.object.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
            LValueRewriter.Util.rewriteArgArray(lValueRewriter, ssaIdentifiers, statementContainer, this.args);
        } else {
            LValueRewriter.Util.rewriteArgArray(lValueRewriter, ssaIdentifiers, statementContainer, this.args);
            this.object = this.object.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        }
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.object = expressionRewriter.rewriteExpression(this.object, ssaIdentifiers, statementContainer, flags);
        this.applyExpressionRewriterToArgs(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(this.args, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        this.object = expressionRewriter.rewriteExpression(this.object, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void applyExpressionRewriterToArgs(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(this.args, expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public void setExplicitGenerics(List<JavaTypeInstance> types) {
    }

    @Override
    public List<JavaTypeInstance> getExplicitGenerics() {
        return null;
    }

    public Expression getObject() {
        return this.object;
    }

    public JavaTypeInstance getClassTypeInstance() {
        return this.getFunction().getClassEntry().getTypeInstance();
    }

    @Override
    public List<Expression> getArgs() {
        return this.args;
    }

    public List<Boolean> getNulls() {
        return this.nulls;
    }

    public Expression getAppropriatelyCastArgument(int idx) {
        return this.getMethodPrototype().getAppropriatelyCastedArgument(this.args.get(idx), idx);
    }

    public ConstantPool getCp() {
        return this.cp;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        this.object.collectUsedLValues(lValueUsageCollector);
        for (Expression expression : this.args) {
            expression.collectUsedLValues(lValueUsageCollector);
        }
    }

    private OverloadMethodSet getOverloadMethodSet() {
        JavaTypeInstance objectType = this.object.getInferredJavaType().getJavaTypeInstance();
        OverloadMethodSet overloadMethodSet = this.getOverloadMethodSetInner(objectType);
        if (overloadMethodSet == null) {
            overloadMethodSet = this.getMethodPrototype().getOverloadMethodSet();
        }
        if (overloadMethodSet == null) {
            return null;
        }
        if (objectType instanceof JavaGenericRefTypeInstance) {
            JavaGenericRefTypeInstance genericType = (JavaGenericRefTypeInstance)objectType;
            return overloadMethodSet.specialiseTo(genericType);
        }
        return overloadMethodSet;
    }

    protected OverloadMethodSet getOverloadMethodSetInner(JavaTypeInstance objectType) {
        JavaTypeInstance deGenerifiedObjectType = objectType.getDeGenerifiedType();
        JavaTypeInstance protoClassType = this.getFunction().getMethodPrototype().getClassType();
        if (protoClassType == null || deGenerifiedObjectType != protoClassType.getDeGenerifiedType()) {
            ClassFile classFile;
            OverloadMethodSet overloadMethodSet = this.getMethodPrototype().getOverloadMethodSet();
            if (deGenerifiedObjectType instanceof JavaRefTypeInstance && (classFile = ((JavaRefTypeInstance)deGenerifiedObjectType).getClassFile()) != null) {
                overloadMethodSet = classFile.getOverloadMethodSet(this.getMethodPrototype());
            }
            return overloadMethodSet;
        }
        return null;
    }

    @Override
    public void rewriteVarArgs(VarArgsRewriter varArgsRewriter) {
        MethodPrototype methodPrototype = this.getMethodPrototype();
        if (!methodPrototype.isVarArgs()) {
            return;
        }
        OverloadMethodSet overloadMethodSet = this.getOverloadMethodSet();
        if (overloadMethodSet == null) {
            return;
        }
        GenericTypeBinder gtb = methodPrototype.getTypeBinderFor(this.args);
        varArgsRewriter.rewriteVarArgsArg(overloadMethodSet, methodPrototype, this.args, gtb);
    }

    private Expression insertCastOrIgnore(Expression arg, OverloadMethodSet overloadMethodSet, int x) {
        JavaTypeInstance argType = overloadMethodSet.getArgType(x, arg.getInferredJavaType().getJavaTypeInstance());
        boolean ignore = false;
        if (argType instanceof JavaGenericBaseInstance) {
            ignore = ((JavaGenericBaseInstance)argType).hasForeignUnbound(this.cp, 0, false, null);
        }
        if (!ignore) {
            boolean bl = ignore = arg instanceof LambdaExpression || arg instanceof LambdaExpressionFallback;
        }
        if (!ignore) {
            return new CastExpression(BytecodeLoc.NONE, new InferredJavaType(argType, InferredJavaType.Source.EXPRESSION, true), arg);
        }
        return arg;
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        Expression arg;
        int x;
        if (this.args.isEmpty()) {
            return false;
        }
        OverloadMethodSet overloadMethodSet = this.getOverloadMethodSet();
        if (overloadMethodSet == null) {
            boxingRewriter.removeRedundantCastOnly(this.args);
            return false;
        }
        MethodPrototype methodPrototype = this.getMethodPrototype();
        GenericTypeBinder gtb = methodPrototype.getTypeBinderFor(this.args);
        boolean callsCorrectEntireMethod = overloadMethodSet.callsCorrectEntireMethod(this.args, gtb);
        boolean nullsPresent = false;
        for (x = 0; x < this.args.size(); ++x) {
            arg = this.args.get(x);
            if (!callsCorrectEntireMethod && !overloadMethodSet.callsCorrectMethod(arg, x, gtb)) {
                arg = this.insertCastOrIgnore(arg, overloadMethodSet, x);
            }
            arg = boxingRewriter.rewriteExpression(arg, null, null, null);
            arg = boxingRewriter.sugarParameterBoxing(arg, x, overloadMethodSet, gtb, methodPrototype);
            nullsPresent |= AbstractMemberFunctionInvokation.isResolveNull(arg);
            this.args.set(x, arg);
        }
        if (nullsPresent && !(callsCorrectEntireMethod = overloadMethodSet.callsCorrectEntireMethod(this.args, gtb))) {
            for (x = 0; x < this.args.size(); ++x) {
                arg = this.args.get(x);
                if (!AbstractMemberFunctionInvokation.isResolveNull(arg)) continue;
                arg = this.insertCastOrIgnore(arg, overloadMethodSet, x);
                this.args.set(x, arg);
            }
        }
        return true;
    }

    private static boolean isResolveNull(Expression arg) {
        return Literal.NULL.equals(arg) || arg.getInferredJavaType().getJavaTypeInstance() == RawJavaType.NULL;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.object = expressionRewriter.rewriteExpression(this.object, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return caught.checkAgainst(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractMemberFunctionInvokation)) {
            return false;
        }
        AbstractMemberFunctionInvokation other = (AbstractMemberFunctionInvokation)o;
        if (!this.object.equals(other.object)) {
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
        if (!(o instanceof AbstractMemberFunctionInvokation)) {
            return false;
        }
        AbstractMemberFunctionInvokation other = (AbstractMemberFunctionInvokation)o;
        if (!constraint.equivalent(this.object, other.object)) {
            return false;
        }
        return constraint.equivalent(this.args, other.args);
    }
}

