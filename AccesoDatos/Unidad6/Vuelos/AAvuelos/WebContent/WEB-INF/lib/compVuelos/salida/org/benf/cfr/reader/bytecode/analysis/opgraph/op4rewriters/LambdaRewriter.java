/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.Op04Rewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.LocalDeclarationRemover;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractNewArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.DynamicInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionCommon;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionFallback;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionNewArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewObjectArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.types.DynamicInvokeType;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodHandle;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.lambda.LambdaUtils;

public class LambdaRewriter
implements Op04Rewriter,
ExpressionRewriter {
    private final DCCommonState state;
    private final ClassFile thisClassFile;
    private final JavaTypeInstance typeInstance;
    private final Method method;
    private final LinkedList<Expression> processingStack = ListFactory.newLinkedList();

    public LambdaRewriter(DCCommonState state, Method method) {
        this.state = state;
        this.method = method;
        this.thisClassFile = method.getClassFile();
        this.typeInstance = this.thisClassFile.getClassType().getDeGenerifiedType();
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) {
            return;
        }
        for (StructuredStatement statement : structuredStatements) {
            statement.rewriteExpressions(this);
        }
    }

    @Override
    public void handleStatement(StatementContainer statementContainer) {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        try {
            MemberFunctionInvokation invoke;
            Expression res;
            this.processingStack.push(expression);
            expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
            if (expression instanceof DynamicInvokation) {
                expression = this.rewriteDynamicExpression((DynamicInvokation)expression);
            }
            if ((res = expression) instanceof CastExpression) {
                Expression child = ((CastExpression)res).getChild();
                if (child instanceof LambdaExpressionCommon) {
                    JavaTypeInstance resType = res.getInferredJavaType().getJavaTypeInstance();
                    JavaTypeInstance childType = child.getInferredJavaType().getJavaTypeInstance();
                    if (childType.implicitlyCastsTo(resType, null)) {
                        Expression expression2 = child;
                        return expression2;
                    }
                    Expression tmp = ((LambdaExpressionCommon)((Object)child)).childCastForced() ? child : new CastExpression(BytecodeLoc.NONE, child.getInferredJavaType(), child, true);
                    Expression expression3 = res = new CastExpression(BytecodeLoc.NONE, res.getInferredJavaType(), tmp);
                    return expression3;
                }
            } else if (res instanceof MemberFunctionInvokation && (invoke = (MemberFunctionInvokation)res).getObject() instanceof LambdaExpressionCommon) {
                res = invoke.withReplacedObject(new CastExpression(BytecodeLoc.NONE, invoke.getObject().getInferredJavaType(), invoke.getObject()));
            }
            Expression expression4 = res;
            return expression4;
        }
        finally {
            this.processingStack.pop();
        }
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return (ConditionalExpression)res;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    @Override
    public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    private Expression rewriteDynamicExpression(DynamicInvokation dynamicExpression) {
        List<Expression> curriedArgs = dynamicExpression.getDynamicArgs();
        Expression functionCall = dynamicExpression.getInnerInvokation();
        if (functionCall instanceof StaticFunctionInvokation) {
            Expression res = this.rewriteDynamicExpression(dynamicExpression, (StaticFunctionInvokation)functionCall, curriedArgs);
            if (res == dynamicExpression) {
                return dynamicExpression;
            }
            if (res instanceof LambdaExpression && this.processingStack.size() > 1) {
                Expression e = this.processingStack.get(1);
                this.couldBeAmbiguous(e, dynamicExpression, (LambdaExpression)res);
            }
            return res;
        }
        return dynamicExpression;
    }

    private void couldBeAmbiguous(Expression fn, Expression arg, LambdaExpression res) {
        if (!(fn instanceof AbstractFunctionInvokation) || this.thisClassFile == null) {
            return;
        }
        AbstractFunctionInvokation afi = (AbstractFunctionInvokation)fn;
        OverloadMethodSet oms = this.thisClassFile.getOverloadMethodSet(afi.getMethodPrototype());
        if (oms.size() < 2) {
            return;
        }
        List<Expression> args = afi.getArgs();
        int idx = args.indexOf(arg);
        if (idx == -1) {
            return;
        }
        List<JavaTypeInstance> types = Functional.filter(oms.getPossibleArgTypes(idx, arg.getInferredJavaType().getJavaTypeInstance()), new Predicate<JavaTypeInstance>(){

            @Override
            public boolean test(JavaTypeInstance in) {
                return in instanceof JavaRefTypeInstance || in instanceof JavaGenericRefTypeInstance;
            }
        });
        if (types.size() == 1) {
            return;
        }
        JavaTypeInstance functionArgType = afi.getMethodPrototype().getArgs().get(idx);
        res.setExplicitArgTypes(this.getExplicitLambdaTypes(functionArgType));
    }

    private List<JavaTypeInstance> getExplicitLambdaTypes(JavaTypeInstance functionArgType) {
        ClassFile classFile = null;
        try {
            classFile = this.state.getClassFile(functionArgType.getDeGenerifiedType());
        }
        catch (CannotLoadClassException cannotLoadClassException) {
            // empty catch block
        }
        if (classFile == null || !classFile.isInterface()) {
            return null;
        }
        List<Method> methods = Functional.filter(classFile.getMethods(), new Predicate<Method>(){

            @Override
            public boolean test(Method in) {
                return in.getCodeAttribute() == null;
            }
        });
        if (methods.size() != 1) {
            return null;
        }
        Method method = methods.get(0);
        List<JavaTypeInstance> args = method.getMethodPrototype().getArgs();
        if (functionArgType instanceof JavaGenericRefTypeInstance) {
            final GenericTypeBinder genericTypeBinder = classFile.getGenericTypeBinder((JavaGenericRefTypeInstance)functionArgType);
            args = Functional.map(args, new UnaryFunction<JavaTypeInstance, JavaTypeInstance>(){

                @Override
                public JavaTypeInstance invoke(JavaTypeInstance arg) {
                    return genericTypeBinder.getBindingFor(arg);
                }
            });
        }
        for (JavaTypeInstance arg : args) {
            if (arg != null) continue;
            return null;
        }
        return args;
    }

    private static Expression getLambdaVariable(Expression e) {
        if (e instanceof LValueExpression) {
            LValueExpression lValueExpression = (LValueExpression)e;
            LValue lValue = lValueExpression.getLValue();
            return new LValueExpression(lValue);
        }
        if (e instanceof NewObjectArray) {
            return e;
        }
        throw new CannotDelambaException();
    }

    private Expression rewriteDynamicExpression(DynamicInvokation dynamicExpression, StaticFunctionInvokation functionInvokation, List<Expression> curriedArgs) {
        Method lambdaMethod;
        JavaTypeInstance typeInstance = functionInvokation.getClazz();
        if (!typeInstance.getRawName().equals("java.lang.invoke.LambdaMetafactory")) {
            return dynamicExpression;
        }
        String functionName = functionInvokation.getName();
        DynamicInvokeType dynamicInvokeType = DynamicInvokeType.lookup(functionName);
        if (dynamicInvokeType == DynamicInvokeType.UNKNOWN) {
            return dynamicExpression;
        }
        List<Expression> metaFactoryArgs = functionInvokation.getArgs();
        if (metaFactoryArgs.size() != 6) {
            return dynamicExpression;
        }
        Expression arg = metaFactoryArgs.get(3);
        List<JavaTypeInstance> targetFnArgTypes = LambdaUtils.getLiteralProto(arg).getArgs();
        ConstantPoolEntryMethodHandle lambdaFnHandle = LambdaUtils.getHandle(metaFactoryArgs.get(4));
        ConstantPoolEntryMethodRef lambdaMethRef = lambdaFnHandle.getMethodRef();
        JavaTypeInstance lambdaTypeLocation = lambdaMethRef.getClassEntry().getTypeInstance();
        MethodPrototype lambdaFn = lambdaMethRef.getMethodPrototype();
        String lambdaFnName = lambdaFn.getName();
        List<JavaTypeInstance> lambdaFnArgTypes = lambdaFn.getArgs();
        if (!(lambdaTypeLocation instanceof JavaRefTypeInstance)) {
            return dynamicExpression;
        }
        JavaRefTypeInstance lambdaTypeRefLocation = (JavaRefTypeInstance)lambdaTypeLocation;
        ClassFile classFile = null;
        if (this.typeInstance.equals(lambdaTypeRefLocation)) {
            classFile = this.thisClassFile;
        } else {
            try {
                classFile = this.state.getClassFile(lambdaTypeRefLocation);
            }
            catch (CannotLoadClassException cannotLoadClassException) {
                // empty catch block
            }
        }
        boolean instance = false;
        switch (lambdaFnHandle.getReferenceKind()) {
            case INVOKE_INTERFACE: 
            case INVOKE_SPECIAL: 
            case INVOKE_VIRTUAL: {
                instance = true;
            }
        }
        if (classFile == null) {
            return new LambdaExpressionFallback(BytecodeLoc.TODO, lambdaTypeRefLocation, dynamicExpression.getInferredJavaType(), lambdaFn, targetFnArgTypes, curriedArgs, instance);
        }
        if (curriedArgs.size() + targetFnArgTypes.size() - (instance ? 1 : 0) != lambdaFnArgTypes.size()) {
            throw new IllegalStateException("Bad argument counts!");
        }
        try {
            lambdaMethod = classFile.getMethodByPrototype(lambdaFn);
        }
        catch (NoSuchMethodException ignore) {
            return dynamicExpression;
        }
        int len = curriedArgs.size();
        for (int x = 0; x < len; ++x) {
            Expression curriedArg = curriedArgs.get(x);
            JavaTypeInstance curriedArgType = curriedArg.getInferredJavaType().getJavaTypeInstance();
            if (curriedArgType.getDeGenerifiedType().equals(TypeConstants.SUPPLIER)) {
                if (curriedArg instanceof CastExpression) {
                    CastExpression castExpression = (CastExpression)curriedArg;
                    curriedArg = new CastExpression(BytecodeLoc.NONE, curriedArg.getInferredJavaType(), castExpression.getChild(), true);
                } else if (!(curriedArg instanceof LValueExpression)) {
                    curriedArg = new CastExpression(BytecodeLoc.NONE, curriedArg.getInferredJavaType(), curriedArg, true);
                }
            }
            curriedArgs.set(x, CastExpression.removeImplicit(curriedArg));
        }
        if (this.typeInstance.equals(lambdaTypeRefLocation) && lambdaMethod.testAccessFlag(AccessFlagMethod.ACC_SYNTHETIC)) {
            try {
                Op04StructuredStatement lambdaCode;
                try {
                    lambdaCode = lambdaMethod.getAnalysis();
                }
                catch (Exception e) {
                    throw new CannotDelambaException();
                }
                int nLambdaArgs = targetFnArgTypes.size();
                List replacementParameters = ListFactory.newList();
                int m = curriedArgs.size();
                for (int n = instance ? 1 : 0; n < m; ++n) {
                    replacementParameters.add(LambdaRewriter.getLambdaVariable(curriedArgs.get(n)));
                }
                List<LValue> anonymousLambdaArgs = ListFactory.newList();
                List<LocalVariable> originalParameters = lambdaMethod.getMethodPrototype().getComputedParameters();
                int offset = replacementParameters.size();
                for (int n = 0; n < nLambdaArgs; ++n) {
                    LocalVariable original = originalParameters.get(n + offset);
                    String name = original.getName().getStringName();
                    LocalVariable tmp = new LocalVariable(name, new InferredJavaType(targetFnArgTypes.get(n), InferredJavaType.Source.EXPRESSION));
                    anonymousLambdaArgs.add(tmp);
                    replacementParameters.add(new LValueExpression(tmp));
                }
                if (originalParameters.size() != replacementParameters.size()) {
                    throw new CannotDelambaException();
                }
                Map<LValue, Expression> rewrites = MapFactory.newMap();
                for (int x = 0; x < originalParameters.size(); ++x) {
                    rewrites.put(originalParameters.get(x), (Expression)replacementParameters.get(x));
                }
                List<StructuredStatement> structuredLambdaStatements = MiscStatementTools.linearise(lambdaCode);
                if (structuredLambdaStatements == null) {
                    throw new CannotDelambaException();
                }
                LambdaInternalRewriter variableRenamer = new LambdaInternalRewriter(rewrites);
                for (StructuredStatement lambdaStatement : structuredLambdaStatements) {
                    lambdaStatement.rewriteExpressions(variableRenamer);
                }
                StructuredStatement lambdaStatement = lambdaCode.getStatement();
                lambdaMethod.hideSynthetic();
                if (structuredLambdaStatements.size() == 3 && structuredLambdaStatements.get(1) instanceof StructuredReturn) {
                    StructuredReturn structuredReturn = (StructuredReturn)structuredLambdaStatements.get(1);
                    Expression expression = structuredReturn.getValue();
                    if (LambdaRewriter.isNewArrayLambda(expression, curriedArgs, anonymousLambdaArgs)) {
                        return new LambdaExpressionNewArray(structuredReturn.getCombinedLoc(), dynamicExpression.getInferredJavaType(), expression.getInferredJavaType());
                    }
                    lambdaStatement = new StructuredExpressionStatement(structuredReturn.getCombinedLoc(), expression, true);
                }
                boolean copied = this.method.copyLocalClassesFrom(lambdaMethod);
                Op04StructuredStatement placeHolder = new Op04StructuredStatement(lambdaStatement);
                StructuredScope scope = new StructuredScope();
                placeHolder.transform(new LocalDeclarationRemover(), scope);
                return new LambdaExpression(lambdaStatement.getCombinedLoc(), dynamicExpression.getInferredJavaType(), anonymousLambdaArgs, null, new StructuredStatementExpression(new InferredJavaType(lambdaMethod.getMethodPrototype().getReturnType(), InferredJavaType.Source.EXPRESSION), lambdaStatement));
            }
            catch (CannotDelambaException cannotDelambaException) {
                // empty catch block
            }
        }
        return new LambdaExpressionFallback(BytecodeLoc.TODO, lambdaTypeRefLocation, dynamicExpression.getInferredJavaType(), lambdaFn, targetFnArgTypes, curriedArgs, instance);
    }

    private static boolean isNewArrayLambda(Expression e, List<Expression> curriedArgs, List<LValue> anonymousLambdaArgs) {
        if (!curriedArgs.isEmpty()) {
            return false;
        }
        if (anonymousLambdaArgs.size() != 1) {
            return false;
        }
        if (!(e instanceof AbstractNewArray)) {
            return false;
        }
        AbstractNewArray ana = (AbstractNewArray)e;
        if (ana.getNumDims() != 1) {
            return false;
        }
        return ana.getDimSize(0).equals(new LValueExpression(anonymousLambdaArgs.get(0)));
    }

    public static class LambdaInternalRewriter
    implements ExpressionRewriter {
        private final Map<LValue, Expression> rewrites;

        LambdaInternalRewriter(Map<LValue, Expression> rewrites) {
            this.rewrites = rewrites;
        }

        @Override
        public void handleStatement(StatementContainer statementContainer) {
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            LValue lv;
            Expression rewrite;
            if (expression instanceof LValueExpression && (rewrite = this.rewrites.get(lv = ((LValueExpression)expression).getLValue())) != null) {
                return rewrite;
            }
            return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        }

        @Override
        public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
            return (ConditionalExpression)res;
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            Expression replacement = this.rewrites.get(lValue);
            if (replacement instanceof LValueExpression) {
                return ((LValueExpression)replacement).getLValue();
            }
            return lValue;
        }

        @Override
        public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            return lValue;
        }
    }

    private static class CannotDelambaException
    extends IllegalStateException {
        private CannotDelambaException() {
        }
    }
}

