/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredBlockStatement;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.annotation.Nullable;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredCase
extends AbstractStructuredBlockStatement {
    private List<Expression> values;
    private final BlockIdentifier blockIdentifier;
    @Nullable
    private final InferredJavaType inferredJavaTypeOfSwitch;
    private final boolean enumSwitch;

    public StructuredCase(BytecodeLoc loc, List<Expression> values, InferredJavaType inferredJavaTypeOfSwitch, Op04StructuredStatement body, BlockIdentifier blockIdentifier) {
        this(loc, values, inferredJavaTypeOfSwitch, body, blockIdentifier, false);
    }

    public StructuredCase(BytecodeLoc loc, List<Expression> values, InferredJavaType inferredJavaTypeOfSwitch, Op04StructuredStatement body, BlockIdentifier blockIdentifier, boolean enumSwitch) {
        super(loc, body);
        this.blockIdentifier = blockIdentifier;
        this.enumSwitch = enumSwitch;
        this.inferredJavaTypeOfSwitch = inferredJavaTypeOfSwitch;
        if (inferredJavaTypeOfSwitch != null && inferredJavaTypeOfSwitch.getJavaTypeInstance() == RawJavaType.CHAR) {
            for (Expression value : values) {
                if (!(value instanceof Literal)) continue;
                TypedLiteral typedLiteral = ((Literal)value).getValue();
                typedLiteral.getInferredJavaType().useAsWithoutCasting(inferredJavaTypeOfSwitch.getJavaTypeInstance());
            }
        }
        this.values = values;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (this.inferredJavaTypeOfSwitch != null) {
            collector.collect(this.inferredJavaTypeOfSwitch.getJavaTypeInstance());
        }
        collector.collectFrom(this.values);
        super.collectTypeUsages(collector);
    }

    private static StaticVariable getEnumStatic(Expression expression) {
        if (!(expression instanceof LValueExpression)) {
            return null;
        }
        LValue lValue = ((LValueExpression)expression).getLValue();
        if (!(lValue instanceof StaticVariable)) {
            return null;
        }
        return (StaticVariable)lValue;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (this.values.isEmpty()) {
            dumper.keyword("default").separator(": ");
        } else {
            int len = this.values.size();
            int last = len - 1;
            for (int x = 0; x < len; ++x) {
                StaticVariable enumStatic;
                Expression value = this.values.get(x);
                if (this.enumSwitch && (enumStatic = StructuredCase.getEnumStatic(value)) != null) {
                    dumper.keyword("case ").fieldName(enumStatic.getFieldName(), enumStatic.getOwningClassType(), false, true, false).separator(": ");
                    if (x == last) continue;
                    dumper.newln();
                    continue;
                }
                dumper.keyword("case ").dump(value).separator(": ");
                if (x == last) continue;
                dumper.newln();
            }
        }
        this.getBody().dump(dumper);
        return dumper;
    }

    @Override
    public boolean isProperlyStructured() {
        return true;
    }

    public List<Expression> getValues() {
        return this.values;
    }

    public BlockIdentifier getBlockIdentifier() {
        return this.blockIdentifier;
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
        this.getBody().linearizeStatementsInto(out);
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        for (Expression expression : this.values) {
            expression.collectUsedLValues(scopeDiscoverer);
        }
        scopeDiscoverer.processOp04Statement(this.getBody());
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredCase)) {
            return false;
        }
        StructuredCase other = (StructuredCase)o;
        if (!this.values.equals(other.values)) {
            return false;
        }
        if (!this.blockIdentifier.equals(other.blockIdentifier)) {
            return false;
        }
        matchIterator.advance();
        return true;
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
    }

    public boolean isDefault() {
        return this.values.isEmpty();
    }
}

