/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.Op04Rewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.AbstractMatchResultIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchOneOf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.ResetAfterTest;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMutatingAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticPostMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticPreMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

public class SyntheticAccessorRewriter
extends AbstractExpressionRewriter
implements Op04Rewriter {
    private final DCCommonState state;
    private final JavaTypeInstance thisClassType;
    private final ExpressionRewriter visbilityRewriter = new VisibiliyDecreasingRewriter();
    private static final String RETURN_LVALUE = "returnlvalue";
    private static final String MUTATION1 = "mutation1";
    private static final String MUTATION2 = "mutation2";
    private static final String MUTATION3 = "mutation3";
    private static final String ASSIGNMENT1 = "assignment1";
    private static final String PRE_INC = "preinc";
    private static final String POST_INC = "postinc";
    private static final String PRE_DEC = "predec";
    private static final String POST_DEC = "postdec";
    private static final String SUPER_INVOKE = "superinv";
    private static final String SUPER_RETINVOKE = "superretinv";
    private static final String STA_SUB1 = "ssub1";
    private static final String STA_FUN1 = "sfun1";

    public SyntheticAccessorRewriter(DCCommonState state, JavaTypeInstance thisClassType) {
        this.state = state;
        this.thisClassType = thisClassType;
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
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if ((expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags)) instanceof StaticFunctionInvokation) {
            expression = this.rewriteFunctionExpression((StaticFunctionInvokation)expression);
        }
        return expression;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    private Expression rewriteFunctionExpression(StaticFunctionInvokation functionInvokation) {
        Expression res = this.rewriteFunctionExpression2(functionInvokation);
        if (res == null) {
            return functionInvokation;
        }
        return res;
    }

    private static boolean validRelationship(JavaTypeInstance type1, JavaTypeInstance type2) {
        Set<JavaTypeInstance> parents1 = SetFactory.newSet();
        type1.getInnerClassHereInfo().collectTransitiveDegenericParents(parents1);
        parents1.add(type1);
        Set<JavaTypeInstance> parents2 = SetFactory.newSet();
        type2.getInnerClassHereInfo().collectTransitiveDegenericParents(parents2);
        parents2.add(type2);
        boolean res = SetUtil.hasIntersection(parents1, parents2);
        return res;
    }

    private Expression rewriteFunctionExpression2(StaticFunctionInvokation functionInvokation) {
        Method otherMethod;
        JavaTypeInstance tgtType = functionInvokation.getClazz();
        if (!SyntheticAccessorRewriter.validRelationship(this.thisClassType, tgtType)) {
            return null;
        }
        ClassFile otherClass = this.state.getClassFile(tgtType);
        JavaTypeInstance otherType = otherClass.getClassType();
        MethodPrototype otherPrototype = functionInvokation.getFunction().getMethodPrototype();
        List<Expression> appliedArgs = functionInvokation.getArgs();
        try {
            otherMethod = otherClass.getMethodByPrototype(otherPrototype);
        }
        catch (NoSuchMethodException e) {
            return null;
        }
        if (!otherMethod.testAccessFlag(AccessFlagMethod.ACC_STATIC)) {
            return null;
        }
        if (!otherMethod.testAccessFlag(AccessFlagMethod.ACC_SYNTHETIC)) {
            return null;
        }
        if (!otherMethod.hasCodeAttribute()) {
            return null;
        }
        Op04StructuredStatement otherCode = otherMethod.getAnalysis();
        if (otherCode == null) {
            return null;
        }
        List<LocalVariable> methodArgs = otherMethod.getMethodPrototype().getComputedParameters();
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(otherCode);
        if (structuredStatements == null) {
            return null;
        }
        Expression res = this.tryRewriteAccessor(structuredStatements, otherType, appliedArgs, methodArgs);
        if (res == null) {
            res = this.tryRewriteFunctionCall(structuredStatements, otherType, appliedArgs, methodArgs);
        }
        if (res != null) {
            otherMethod.hideSynthetic();
            return this.visbilityRewriter.rewriteExpression(res, null, null, null);
        }
        return null;
    }

    private Expression tryRewriteAccessor(List<StructuredStatement> structuredStatements, JavaTypeInstance otherType, List<Expression> appliedArgs, List<LocalVariable> methodArgs) {
        AbstractMutatingAssignmentExpression res;
        StaticVariable staticVariable;
        WildcardMatch wcm = new WildcardMatch();
        ArrayList<Expression> methodExprs = new ArrayList<Expression>();
        for (int x = 1; x < methodArgs.size(); ++x) {
            methodExprs.add(new LValueExpression(methodArgs.get(x)));
        }
        MatchSequence matcher = new MatchSequence(new BeginBlock(null), new MatchOneOf(new ResetAfterTest(wcm, RETURN_LVALUE, new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("lvalue")), null)), new ResetAfterTest(wcm, MUTATION1, new MatchSequence(new StructuredAssignment(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), wcm.getExpressionWildCard("rvalue")), new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("lvalue")), null))), new ResetAfterTest(wcm, ASSIGNMENT1, new MatchSequence(new StructuredAssignment(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), wcm.getExpressionWildCard("rvalue")))), new ResetAfterTest(wcm, MUTATION2, new MatchSequence(new StructuredAssignment(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), wcm.getExpressionWildCard("rvalue")), new StructuredReturn(BytecodeLoc.NONE, wcm.getExpressionWildCard("rvalue"), null))), new ResetAfterTest(wcm, MUTATION3, new StructuredReturn(BytecodeLoc.NONE, wcm.getArithmeticMutationWildcard("mutation", wcm.getLValueWildCard("lvalue"), wcm.getExpressionWildCard("rvalue")), null)), new ResetAfterTest(wcm, PRE_INC, new StructuredReturn(BytecodeLoc.NONE, new ArithmeticPreMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), ArithOp.PLUS), null)), new ResetAfterTest(wcm, PRE_DEC, new StructuredReturn(BytecodeLoc.NONE, new ArithmeticPreMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), ArithOp.MINUS), null)), new ResetAfterTest(wcm, POST_INC, new StructuredReturn(BytecodeLoc.NONE, new ArithmeticPostMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), ArithOp.PLUS), null)), new ResetAfterTest(wcm, POST_DEC, new StructuredReturn(BytecodeLoc.NONE, new ArithmeticPostMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), ArithOp.MINUS), null)), new ResetAfterTest(wcm, POST_INC, new MatchSequence(new StructuredExpressionStatement(BytecodeLoc.NONE, new ArithmeticPostMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), ArithOp.PLUS), false), new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("lvalue")), null))), new ResetAfterTest(wcm, POST_INC, new MatchSequence(new StructuredAssignment(BytecodeLoc.NONE, wcm.getStackLabelWildcard("tmp"), new LValueExpression(wcm.getLValueWildCard("lvalue"))), new StructuredAssignment(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), new ArithmeticOperation(BytecodeLoc.NONE, new StackValue(BytecodeLoc.NONE, wcm.getStackLabelWildcard("tmp")), new Literal(TypedLiteral.getInt(1)), ArithOp.PLUS)), new StructuredReturn(BytecodeLoc.NONE, new StackValue(BytecodeLoc.NONE, wcm.getStackLabelWildcard("tmp")), null))), new ResetAfterTest(wcm, POST_DEC, new MatchSequence(new StructuredExpressionStatement(BytecodeLoc.NONE, new ArithmeticPostMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), ArithOp.MINUS), false), new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("lvalue")), null))), new ResetAfterTest(wcm, SUPER_INVOKE, new StructuredExpressionStatement(BytecodeLoc.NONE, wcm.getSuperFunction("super", methodExprs), false)), new ResetAfterTest(wcm, SUPER_RETINVOKE, new StructuredReturn(BytecodeLoc.NONE, wcm.getSuperFunction("super", methodExprs), null))), new EndBlock(null));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);
        AccessorMatchCollector accessorMatchCollector = new AccessorMatchCollector();
        mi.advance();
        if (!matcher.match(mi, (MatchResultCollector)accessorMatchCollector)) {
            return null;
        }
        if (accessorMatchCollector.matchType == null) {
            return null;
        }
        boolean isStatic = accessorMatchCollector.lValue instanceof StaticVariable;
        if (isStatic && !otherType.equals((staticVariable = (StaticVariable)accessorMatchCollector.lValue).getOwningClassType())) {
            return null;
        }
        String matchType = accessorMatchCollector.matchType;
        Map<LValue, LValue> lValueReplacements = MapFactory.newMap();
        Map<Expression, Expression> expressionReplacements = MapFactory.newMap();
        for (int x = 0; x < methodArgs.size(); ++x) {
            LocalVariable methodArg = methodArgs.get(x);
            Expression appliedArg = appliedArgs.get(x);
            if (appliedArg instanceof LValueExpression) {
                LValue appliedLvalue = ((LValueExpression)appliedArg).getLValue();
                lValueReplacements.put(methodArg, appliedLvalue);
            }
            appliedArg = this.getCastFriendArg(otherType, methodArg, appliedArg);
            expressionReplacements.put(new LValueExpression(methodArg), appliedArg);
        }
        CloneHelper cloneHelper = new CloneHelper(expressionReplacements, lValueReplacements);
        if (matchType.equals(MUTATION1) || matchType.equals(MUTATION2) || matchType.equals(ASSIGNMENT1)) {
            AssignmentExpression assignmentExpression = new AssignmentExpression(BytecodeLoc.TODO, accessorMatchCollector.lValue, accessorMatchCollector.rValue);
            return cloneHelper.replaceOrClone(assignmentExpression);
        }
        if (matchType.equals(MUTATION3)) {
            ArithmeticMutationOperation mutation = new ArithmeticMutationOperation(BytecodeLoc.TODO, accessorMatchCollector.lValue, accessorMatchCollector.rValue, accessorMatchCollector.op);
            return cloneHelper.replaceOrClone(mutation);
        }
        if (matchType.equals(RETURN_LVALUE)) {
            return cloneHelper.replaceOrClone(new LValueExpression(accessorMatchCollector.lValue));
        }
        if (matchType.equals(PRE_DEC)) {
            res = new ArithmeticPreMutationOperation(BytecodeLoc.TODO, accessorMatchCollector.lValue, ArithOp.MINUS);
            return cloneHelper.replaceOrClone(res);
        }
        if (matchType.equals(PRE_INC)) {
            res = new ArithmeticPreMutationOperation(BytecodeLoc.TODO, accessorMatchCollector.lValue, ArithOp.PLUS);
            return cloneHelper.replaceOrClone(res);
        }
        if (matchType.equals(POST_DEC)) {
            res = new ArithmeticPostMutationOperation(BytecodeLoc.TODO, accessorMatchCollector.lValue, ArithOp.MINUS);
            return cloneHelper.replaceOrClone(res);
        }
        if (matchType.equals(POST_INC)) {
            res = new ArithmeticPostMutationOperation(BytecodeLoc.TODO, accessorMatchCollector.lValue, ArithOp.PLUS);
            return cloneHelper.replaceOrClone(res);
        }
        if (matchType.equals(SUPER_INVOKE) || matchType.equals(SUPER_RETINVOKE)) {
            SuperFunctionInvokation invoke = (SuperFunctionInvokation)accessorMatchCollector.rValue;
            SuperFunctionInvokation newInvoke = (SuperFunctionInvokation)cloneHelper.replaceOrClone(invoke);
            newInvoke = newInvoke.withCustomName(otherType);
            return newInvoke;
        }
        throw new IllegalStateException();
    }

    private Expression tryRewriteFunctionCall(List<StructuredStatement> structuredStatements, JavaTypeInstance otherType, List<Expression> appliedArgs, List<LocalVariable> methodArgs) {
        WildcardMatch wcm = new WildcardMatch();
        String MEM_SUB1 = "msub1";
        String MEM_FUN1 = "mfun1";
        MatchSequence matcher = new MatchSequence(new BeginBlock(null), new MatchOneOf(new ResetAfterTest(wcm, MEM_SUB1, new StructuredExpressionStatement(BytecodeLoc.NONE, wcm.getMemberFunction("func", null, false, (Expression)new LValueExpression(wcm.getLValueWildCard("lvalue")), null), false)), new ResetAfterTest(wcm, STA_SUB1, new StructuredExpressionStatement(BytecodeLoc.NONE, wcm.getStaticFunction("func", otherType, null, null, (List<Expression>)null), false)), new ResetAfterTest(wcm, MEM_FUN1, new StructuredReturn(BytecodeLoc.NONE, wcm.getMemberFunction("func", null, false, (Expression)new LValueExpression(wcm.getLValueWildCard("lvalue")), null), null)), new ResetAfterTest(wcm, STA_FUN1, new StructuredReturn(BytecodeLoc.NONE, wcm.getStaticFunction("func", otherType, null, null, (List<Expression>)null), null))), new EndBlock(null));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);
        FuncMatchCollector funcMatchCollector = new FuncMatchCollector();
        mi.advance();
        if (!matcher.match(mi, (MatchResultCollector)funcMatchCollector)) {
            return null;
        }
        if (funcMatchCollector.matchType == null) {
            return null;
        }
        Map<LValue, LValue> lValueReplacements = MapFactory.newMap();
        Map<Expression, Expression> expressionReplacements = MapFactory.newMap();
        for (int x = 0; x < methodArgs.size(); ++x) {
            LocalVariable methodArg = methodArgs.get(x);
            Expression appliedArg = appliedArgs.get(x);
            if (appliedArg instanceof LValueExpression) {
                LValue appliedLvalue = ((LValueExpression)appliedArg).getLValue();
                lValueReplacements.put(methodArg, appliedLvalue);
            }
            appliedArg = this.getCastFriendArg(otherType, methodArg, appliedArg);
            expressionReplacements.put(new LValueExpression(methodArg), appliedArg);
        }
        CloneHelper cloneHelper = new CloneHelper(expressionReplacements, lValueReplacements);
        return cloneHelper.replaceOrClone(funcMatchCollector.functionInvokation);
    }

    private Expression getCastFriendArg(JavaTypeInstance otherType, LocalVariable methodArg, Expression appliedArg) {
        if (methodArg.getInferredJavaType().getJavaTypeInstance().equals(otherType) && !appliedArg.getInferredJavaType().getJavaTypeInstance().equals(otherType)) {
            appliedArg = new CastExpression(BytecodeLoc.NONE, methodArg.getInferredJavaType(), appliedArg);
        }
        return appliedArg;
    }

    private class VisibiliyDecreasingRewriter
    extends AbstractExpressionRewriter {
        private VisibiliyDecreasingRewriter() {
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (lValue instanceof StaticVariable) {
                StaticVariable sv = (StaticVariable)lValue;
                JavaTypeInstance owning = sv.getOwningClassType();
                if (!SyntheticAccessorRewriter.this.thisClassType.getInnerClassHereInfo().isTransitiveInnerClassOf(owning)) {
                    return sv.getNonSimpleCopy();
                }
            }
            return lValue;
        }
    }

    private class FuncMatchCollector
    extends AbstractMatchResultIterator {
        String matchType;
        LValue lValue;
        StaticFunctionInvokation staticFunctionInvokation;
        MemberFunctionInvokation memberFunctionInvokation;
        Expression functionInvokation;

        private FuncMatchCollector() {
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            this.matchType = name;
            if (this.matchType.equals(SyntheticAccessorRewriter.STA_FUN1) || this.matchType.endsWith(SyntheticAccessorRewriter.STA_SUB1)) {
                this.staticFunctionInvokation = wcm.getStaticFunction("func").getMatch();
                this.functionInvokation = this.staticFunctionInvokation;
            } else {
                this.memberFunctionInvokation = wcm.getMemberFunction("func").getMatch();
                this.functionInvokation = this.memberFunctionInvokation;
                this.lValue = wcm.getLValueWildCard("lvalue").getMatch();
            }
        }
    }

    private class AccessorMatchCollector
    extends AbstractMatchResultIterator {
        String matchType;
        LValue lValue;
        Expression rValue;
        ArithOp op;

        private AccessorMatchCollector() {
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            this.matchType = name;
            this.lValue = wcm.getLValueWildCard("lvalue").getMatch();
            if (this.matchType.equals(SyntheticAccessorRewriter.MUTATION1) || this.matchType.equals(SyntheticAccessorRewriter.MUTATION2) || this.matchType.equals(SyntheticAccessorRewriter.ASSIGNMENT1)) {
                this.rValue = wcm.getExpressionWildCard("rvalue").getMatch();
            }
            if (this.matchType.equals(SyntheticAccessorRewriter.MUTATION3)) {
                this.rValue = wcm.getExpressionWildCard("rvalue").getMatch();
                this.op = wcm.getArithmeticMutationWildcard("mutation").getOp().getMatch();
            }
            if (this.matchType.equals(SyntheticAccessorRewriter.SUPER_INVOKE) || this.matchType.equals(SyntheticAccessorRewriter.SUPER_RETINVOKE)) {
                this.rValue = wcm.getSuperFunction("super").getMatch();
            }
        }
    }
}

