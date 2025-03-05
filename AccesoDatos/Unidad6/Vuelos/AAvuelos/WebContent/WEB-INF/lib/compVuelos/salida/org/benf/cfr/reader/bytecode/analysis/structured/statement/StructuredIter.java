/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.ScopeDiscoverInfoCache;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredBlockStatement;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredIter
extends AbstractStructuredBlockStatement {
    private final BlockIdentifier block;
    private LValue iterator;
    private Expression list;

    StructuredIter(BytecodeLoc loc, BlockIdentifier block, LValue iterator, Expression list, Op04StructuredStatement body) {
        super(loc, body);
        this.block = block;
        this.iterator = iterator;
        this.list = list;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.iterator.collectTypeUsages(collector);
        this.list.collectTypeUsages(collector);
        super.collectTypeUsages(collector);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.list);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (this.block.hasForeignReferences()) {
            dumper.label(this.block.getName(), true);
        }
        dumper.keyword("for ").separator("(");
        if (this.iterator.isFinal()) {
            dumper.keyword("final ");
        }
        LValue.Creation.dump(dumper, this.iterator).separator(" : ").dump(this.list).separator(") ");
        this.getBody().dump(dumper);
        return dumper;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
        this.getBody().linearizeStatementsInto(out);
    }

    @Override
    public BlockIdentifier getBreakableBlockOrNull() {
        return this.block;
    }

    @Override
    public boolean supportsContinueBreak() {
        return true;
    }

    @Override
    public boolean supportsBreak() {
        return true;
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        scopeDiscoverer.enterBlock(this);
        this.list.collectUsedLValues(scopeDiscoverer);
        this.iterator.collectLValueAssignments(null, this.getContainer(), scopeDiscoverer);
        scopeDiscoverer.processOp04Statement(this.getBody());
        scopeDiscoverer.leaveBlock(this);
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {
    }

    @Override
    public boolean alwaysDefines(LValue scopedEntity) {
        if (scopedEntity == null) {
            return false;
        }
        return scopedEntity.equals(this.iterator);
    }

    @Override
    public boolean canDefine(LValue scopedEntity, ScopeDiscoverInfoCache factCache) {
        if (scopedEntity == null) {
            return false;
        }
        return scopedEntity.equals(this.iterator);
    }

    @Override
    public List<LValue> findCreatedHere() {
        if (!(this.iterator instanceof LocalVariable)) {
            return null;
        }
        return ListFactory.newImmutableList(this.iterator);
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        this.iterator = expressionRewriter.rewriteExpression(this.iterator, null, (StatementContainer)this.getContainer(), null);
        this.list = expressionRewriter.rewriteExpression(this.list, null, (StatementContainer)this.getContainer(), null);
    }
}

