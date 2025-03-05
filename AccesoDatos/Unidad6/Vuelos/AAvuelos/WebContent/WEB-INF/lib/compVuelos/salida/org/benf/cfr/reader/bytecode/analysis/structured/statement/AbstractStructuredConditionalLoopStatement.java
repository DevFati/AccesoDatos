/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredBlockStatement;
import org.benf.cfr.reader.state.TypeUsageCollector;

public abstract class AbstractStructuredConditionalLoopStatement
extends AbstractStructuredBlockStatement {
    protected ConditionalExpression condition;
    protected final BlockIdentifier block;

    AbstractStructuredConditionalLoopStatement(BytecodeLoc loc, ConditionalExpression condition, BlockIdentifier block, Op04StructuredStatement body) {
        super(loc, body);
        this.condition = condition;
        this.block = block;
    }

    public BlockIdentifier getBlock() {
        return this.block;
    }

    public ConditionalExpression getCondition() {
        return this.condition;
    }

    @Override
    public BlockIdentifier getBreakableBlockOrNull() {
        return this.block;
    }

    @Override
    public boolean supportsBreak() {
        return true;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(this.condition);
        super.collectTypeUsages(collector);
    }

    public boolean isInfinite() {
        return this.condition == null;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public boolean supportsContinueBreak() {
        return true;
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        if (this.condition != null) {
            this.condition.collectUsedLValues(scopeDiscoverer);
        }
        scopeDiscoverer.processOp04Statement(this.getBody());
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
        this.getBody().linearizeStatementsInto(out);
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        if (this.condition != null) {
            this.condition = expressionRewriter.rewriteExpression(this.condition, null, (StatementContainer)this.getContainer(), null);
        }
    }
}

