/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.BoxingHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;

public class PrimitiveBoxingRewriter
implements ExpressionRewriter {
    @Override
    public void handleStatement(StatementContainer statementContainer) {
        Object statement = statementContainer.getStatement();
        if (statement instanceof BoxingProcessor) {
            ((BoxingProcessor)statement).rewriteBoxing(this);
        }
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        BoxingProcessor boxingProcessor;
        if (expression instanceof BoxingProcessor && (boxingProcessor = (BoxingProcessor)((Object)expression)).rewriteBoxing(this)) {
            boxingProcessor.applyNonArgExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
            return expression;
        }
        expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return expression;
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        BoxingProcessor boxingProcessor;
        if (expression instanceof BoxingProcessor && (boxingProcessor = (BoxingProcessor)((Object)expression)).rewriteBoxing(this)) {
            boxingProcessor.applyNonArgExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
            return expression;
        }
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

    public Expression sugarParameterBoxing(Expression in, int argIdx, OverloadMethodSet possibleMethods, GenericTypeBinder gtb, MethodPrototype methodPrototype) {
        Expression res = in;
        InferredJavaType outerCastType = null;
        Expression res1 = null;
        if (in instanceof CastExpression) {
            List<JavaTypeInstance> argTypes;
            boolean wasRaw = true;
            if (methodPrototype != null && argIdx <= (argTypes = methodPrototype.getArgs()).size() - 1) {
                wasRaw = argTypes.get(argIdx) instanceof RawJavaType;
            }
            outerCastType = in.getInferredJavaType();
            res1 = res = CastExpression.removeImplicitOuterType(res, gtb, wasRaw);
        }
        if (res instanceof MemberFunctionInvokation) {
            res = BoxingHelper.sugarUnboxing((MemberFunctionInvokation)res);
        } else if (res instanceof StaticFunctionInvokation) {
            res = BoxingHelper.sugarBoxing((StaticFunctionInvokation)res);
        }
        if (res == in) {
            return in;
        }
        if (!possibleMethods.callsCorrectMethod(res, argIdx, gtb)) {
            if (outerCastType != null && res.getInferredJavaType().getJavaTypeInstance().impreciseCanCastTo(outerCastType.getJavaTypeInstance(), gtb) && possibleMethods.callsCorrectMethod(res = new CastExpression(BytecodeLoc.NONE, outerCastType, res), argIdx, gtb)) {
                return res;
            }
            if (res1 != null && possibleMethods.callsCorrectMethod(res1, argIdx, gtb)) {
                return res1;
            }
            return in;
        }
        return res;
    }

    public void removeRedundantCastOnly(List<Expression> mutableIn) {
        int len = mutableIn.size();
        for (int x = 0; x < len; ++x) {
            mutableIn.set(x, this.removeRedundantCastOnly(mutableIn.get(x)));
        }
    }

    private Expression removeRedundantCastOnly(Expression in) {
        if (in instanceof CastExpression) {
            JavaTypeInstance childType;
            if (((CastExpression)in).isForced()) {
                return in;
            }
            JavaTypeInstance castType = in.getInferredJavaType().getJavaTypeInstance();
            if (castType.equals(childType = ((CastExpression)in).getChild().getInferredJavaType().getJavaTypeInstance())) {
                return this.removeRedundantCastOnly(((CastExpression)in).getChild());
            }
        }
        return in;
    }

    public Expression sugarNonParameterBoxing(Expression in, JavaTypeInstance tgtType) {
        CastExpression cast;
        Expression res = in;
        boolean recast = false;
        if (in instanceof CastExpression && ((CastExpression)in).couldBeImplicit(null)) {
            res = ((CastExpression)in).getChild();
            if (Literal.NULL.equals(res) && !tgtType.isObject()) {
                return in;
            }
            if (tgtType.isObject() && !BoxingHelper.isBoxedType(tgtType) && in.getInferredJavaType().getJavaTypeInstance().isRaw() && res.getInferredJavaType().getJavaTypeInstance().isRaw() && res.getInferredJavaType().getJavaTypeInstance() != in.getInferredJavaType().getJavaTypeInstance()) {
                return in;
            }
            recast = !(tgtType instanceof RawJavaType);
        } else if (in instanceof MemberFunctionInvokation) {
            res = BoxingHelper.sugarUnboxing((MemberFunctionInvokation)in);
        } else if (in instanceof StaticFunctionInvokation) {
            res = BoxingHelper.sugarBoxing((StaticFunctionInvokation)in);
        }
        if (res == in) {
            return in;
        }
        if (!res.getInferredJavaType().getJavaTypeInstance().implicitlyCastsTo(in.getInferredJavaType().getJavaTypeInstance(), null)) {
            return in;
        }
        if (!res.getInferredJavaType().getJavaTypeInstance().impreciseCanCastTo(tgtType, null)) {
            return in;
        }
        res = this.sugarNonParameterBoxing(res, tgtType);
        if (recast && !(cast = (CastExpression)in).isForced() && cast.getInferredJavaType().getJavaTypeInstance() instanceof RawJavaType && res.getInferredJavaType().getJavaTypeInstance() instanceof JavaRefTypeInstance) {
            res = new CastExpression(BytecodeLoc.NONE, cast.getInferredJavaType(), res);
        }
        return res;
    }

    public Expression sugarUnboxing(Expression in) {
        if (in instanceof MemberFunctionInvokation) {
            return BoxingHelper.sugarUnboxing((MemberFunctionInvokation)in);
        }
        return in;
    }

    public boolean isUnboxedType(Expression in) {
        JavaTypeInstance type = in.getInferredJavaType().getJavaTypeInstance();
        if (!(type instanceof RawJavaType)) {
            return false;
        }
        if (in instanceof AbstractMemberFunctionInvokation) {
            return false;
        }
        RawJavaType rawJavaType = (RawJavaType)type;
        return rawJavaType.isUsableType();
    }
}

