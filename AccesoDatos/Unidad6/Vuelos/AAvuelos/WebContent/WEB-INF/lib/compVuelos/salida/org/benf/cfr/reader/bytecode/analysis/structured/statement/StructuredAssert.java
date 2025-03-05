/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import java.util.Vector;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredAssert
extends AbstractStructuredStatement {
    private ConditionalExpression conditionalExpression;
    private Expression arg;

    private StructuredAssert(BytecodeLoc loc, ConditionalExpression conditionalExpression, Expression arg) {
        super(loc);
        this.conditionalExpression = conditionalExpression;
        this.arg = arg;
    }

    public static StructuredAssert mkStructuredAssert(BytecodeLoc loc, ConditionalExpression conditionalExpression, Expression arg) {
        return new StructuredAssert(loc, conditionalExpression, CastExpression.tryRemoveCast(arg));
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.conditionalExpression, this.arg);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.conditionalExpression.collectTypeUsages(collector);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("assert (").dump(this.conditionalExpression).separator(")");
        if (this.arg != null) {
            dumper.print(" : ").dump(this.arg);
        }
        dumper.endCodeln();
        return dumper;
    }

    @Override
    public StructuredStatement informBlockHeirachy(Vector<BlockIdentifier> blockIdentifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        this.conditionalExpression.collectUsedLValues(scopeDiscoverer);
    }

    @Override
    public boolean isRecursivelyStructured() {
        return true;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredAssert)) {
            return false;
        }
        StructuredAssert other = (StructuredAssert)o;
        if (!this.conditionalExpression.equals(other.conditionalExpression)) {
            return false;
        }
        matchIterator.advance();
        return true;
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        this.conditionalExpression = expressionRewriter.rewriteExpression(this.conditionalExpression, null, (StatementContainer)this.getContainer(), null);
    }
}

