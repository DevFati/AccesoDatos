/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionVisitor;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.collections.SetFactory;

public class LValueTypeClashCheck
implements LValueScopeDiscoverer,
StructuredStatementTransformer {
    private final Set<Integer> clashes = SetFactory.newSet();

    @Override
    public void processOp04Statement(Op04StructuredStatement statement) {
        statement.getStatement().traceLocalVariableScope(this);
    }

    @Override
    public void enterBlock(StructuredStatement structuredStatement) {
    }

    @Override
    public void leaveBlock(StructuredStatement structuredStatement) {
    }

    @Override
    public void mark(StatementContainer<StructuredStatement> mark) {
    }

    @Override
    public boolean ifCanDefine() {
        return false;
    }

    @Override
    public void collect(StackSSALabel lValue, StatementContainer<StructuredStatement> statementContainer, Expression value) {
        this.collectExpression(lValue, value);
    }

    @Override
    public void collectMultiUse(StackSSALabel lValue, StatementContainer<StructuredStatement> statementContainer, Expression value) {
        this.collectExpression(lValue, value);
    }

    @Override
    public void collectMutatedLValue(LValue lValue, StatementContainer<StructuredStatement> statementContainer, Expression value) {
        this.collectExpression(lValue, value);
    }

    @Override
    public void collectLocalVariableAssignment(LocalVariable localVariable, StatementContainer<StructuredStatement> statementContainer, Expression value) {
        this.collectExpression(localVariable, value);
    }

    @Override
    public void collect(LValue lValue, ReadWrite rw) {
        this.collectExpression(lValue, null);
    }

    public void collectExpression(LValue lValue, Expression value) {
        lValue.collectLValueUsage(this);
        if (!(lValue instanceof LocalVariable)) {
            return;
        }
        int idx = ((LocalVariable)lValue).getIdx();
        InferredJavaType inferredJavaType = lValue.getInferredJavaType();
        if (inferredJavaType != null) {
            StackType lStack;
            JavaTypeInstance javaTypeInstance = inferredJavaType.getJavaTypeInstance();
            if (inferredJavaType.isClash() || javaTypeInstance == RawJavaType.REF) {
                this.clashes.add(idx);
                return;
            }
            if (value != null && (lStack = javaTypeInstance.getStackType()) == StackType.INT) {
                JavaTypeInstance valueType = value.getInferredJavaType().getJavaTypeInstance();
                if (valueType.getStackType() != StackType.INT) {
                    return;
                }
                if (!valueType.implicitlyCastsTo(javaTypeInstance, null)) {
                    this.clashes.add(idx);
                    return;
                }
                if (!(javaTypeInstance instanceof RawJavaType)) {
                    return;
                }
                Check check = new Check((RawJavaType)javaTypeInstance);
                check.rewriteExpression(value, null, null, null);
                if (!check.ok) {
                    this.clashes.add(idx);
                    return;
                }
            }
        }
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.traceLocalVariableScope(this);
        in.transformStructuredChildren(this, scope);
        return in;
    }

    @Override
    public boolean descendLambdas() {
        return false;
    }

    public Set<Integer> getClashes() {
        return this.clashes;
    }

    private static class Check
    extends AbstractExpressionRewriter {
        private boolean ok = true;
        private RawJavaType javaTypeInstance;
        private Visitor visitor = new Visitor();

        Check(RawJavaType javaTypeInstance) {
            this.javaTypeInstance = javaTypeInstance;
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            expression.visit(this.visitor);
            return expression;
        }

        private class Visitor
        extends AbstractExpressionVisitor<Void> {
            private Visitor() {
            }

            @Override
            public Void visit(Literal l) {
                if (!l.getValue().checkIntegerUsage(Check.this.javaTypeInstance)) {
                    Check.this.ok = false;
                }
                return null;
            }

            @Override
            public Void visit(TernaryExpression e) {
                Check.this.rewriteExpression(e.getLhs(), null, null, null);
                Check.this.rewriteExpression(e.getRhs(), null, null, null);
                return null;
            }

            @Override
            public Void visit(ArithmeticOperation e) {
                if (!e.getOp().isBoolSafe() && Check.this.javaTypeInstance == RawJavaType.BOOLEAN) {
                    Check.this.ok = false;
                    return null;
                }
                e.applyExpressionRewriter(Check.this, null, null, null);
                return null;
            }
        }
    }
}

