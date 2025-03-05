/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;

public class ExplicitTypeCallRewriter
extends AbstractExpressionRewriter {
    private final InnerExplicitTypeCallRewriter inner = new InnerExplicitTypeCallRewriter();

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression instanceof AbstractFunctionInvokation) {
            ((AbstractFunctionInvokation)expression).applyExpressionRewriterToArgs(this.inner, ssaIdentifiers, statementContainer, flags);
        } else if (expression instanceof ConstructorInvokationSimple) {
            expression.applyExpressionRewriter(this.inner, ssaIdentifiers, statementContainer, flags);
        }
        return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
    }

    private class InnerExplicitTypeCallRewriter
    extends AbstractExpressionRewriter {
        private InnerExplicitTypeCallRewriter() {
        }

        private Expression rewriteFunctionInvokation(AbstractFunctionInvokation invokation) {
            JavaTypeInstance usedType;
            GenericTypeBinder binder;
            JavaTypeInstance returnType;
            MethodPrototype p;
            if (invokation instanceof StaticFunctionInvokation && (p = invokation.getFunction().getMethodPrototype()).hasFormalTypeParameters() && p.getVisibleArgCount() == 0 && (returnType = p.getReturnType()) instanceof JavaGenericBaseInstance && (binder = GenericTypeBinder.extractBaseBindings((JavaGenericBaseInstance)returnType, usedType = invokation.getInferredJavaType().getJavaTypeInstance())) != null) {
                List<JavaTypeInstance> types = p.getExplicitGenericUsage(binder);
                invokation.setExplicitGenerics(types);
            }
            return invokation;
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (expression instanceof AbstractFunctionInvokation) {
                expression = this.rewriteFunctionInvokation((AbstractFunctionInvokation)expression);
            }
            return expression;
        }

        @Override
        public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            return expression;
        }
    }
}

