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
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class SwitchExpression
extends AbstractExpression {
    private Expression value;
    private List<Branch> cases;

    public SwitchExpression(BytecodeLoc loc, InferredJavaType inferredJavaType, Expression value, List<Branch> cases) {
        super(loc, inferredJavaType);
        this.value = value;
        this.cases = cases;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof SwitchExpression)) {
            return false;
        }
        SwitchExpression other = (SwitchExpression)o;
        return this.cases.equals(other.cases) && this.value.equals(other.value);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.keyword("switch ").separator("(");
        d.dump(this.value);
        d.separator(") ").separator("{");
        d.newln();
        d.indent(1);
        for (Branch item : this.cases) {
            boolean first = true;
            List<Expression> cases = item.cases;
            if (cases.isEmpty()) {
                d.keyword("default");
            } else {
                d.keyword("case ");
                for (Expression e : cases) {
                    first = StringUtils.comma(first, d);
                    d.dump(e);
                }
            }
            d.operator(" -> ").dump(item.value);
            if (item.value instanceof StructuredStatementExpression) continue;
            d.endCodeln();
        }
        d.indent(-1);
        d.separator("}");
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        boolean changed = false;
        Expression newValue = expressionRewriter.rewriteExpression(this.value, ssaIdentifiers, statementContainer, flags);
        if (newValue != this.value) {
            changed = true;
        }
        List<Branch> out = ListFactory.newList();
        for (Branch case1 : this.cases) {
            Branch newBranch = case1.rewrite(expressionRewriter, ssaIdentifiers, statementContainer, flags);
            if (newBranch != case1) {
                changed = true;
                out.add(newBranch);
                continue;
            }
            out.add(case1);
        }
        if (changed) {
            return new SwitchExpression(this.getLoc(), this.getInferredJavaType(), newValue, out);
        }
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        this.value.collectUsedLValues(lValueUsageCollector);
        for (Branch case1 : this.cases) {
            for (Expression item : case1.cases) {
                item.collectUsedLValues(lValueUsageCollector);
            }
            case1.value.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof SwitchExpression)) {
            return false;
        }
        SwitchExpression other = (SwitchExpression)o;
        if (other.cases.size() != this.cases.size()) {
            return false;
        }
        for (int i = 0; i < this.cases.size(); ++i) {
            Branch p1 = this.cases.get(i);
            Branch p2 = other.cases.get(i);
            if (!constraint.equivalent(p1.cases, p2.cases)) {
                return false;
            }
            if (constraint.equivalent(p1.value, p2.value)) continue;
            return false;
        }
        return constraint.equivalent(this.value, other.value);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        List<Branch> res = ListFactory.newList();
        for (Branch case1 : this.cases) {
            res.add(new Branch(cloneHelper.replaceOrClone(case1.cases), cloneHelper.replaceOrClone(case1.value)));
        }
        return new SwitchExpression(this.getLoc(), this.getInferredJavaType(), cloneHelper.replaceOrClone(this.value), res);
    }

    public static class Branch {
        List<Expression> cases;
        Expression value;

        public Branch(List<Expression> cases, Expression value) {
            this.cases = cases;
            this.value = value;
        }

        private Branch rewrite(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            boolean thisChanged = false;
            List<Expression> newCases = ListFactory.newList();
            for (Expression exp : this.cases) {
                Expression newExp = expressionRewriter.rewriteExpression(exp, ssaIdentifiers, statementContainer, flags);
                if (newExp != exp) {
                    thisChanged = true;
                }
                newCases.add(newExp);
            }
            Expression newValue = expressionRewriter.rewriteExpression(this.value, ssaIdentifiers, statementContainer, flags);
            if (newValue != this.value) {
                thisChanged = true;
            }
            if (!thisChanged) {
                return this;
            }
            return new Branch(newCases, newValue);
        }
    }
}

