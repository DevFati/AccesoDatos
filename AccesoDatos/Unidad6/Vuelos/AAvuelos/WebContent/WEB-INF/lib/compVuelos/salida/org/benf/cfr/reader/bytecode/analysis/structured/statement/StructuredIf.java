/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.CanRemovePointlessBlock;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InstanceOfAssignRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.ScopeDiscoverInfoCache;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssert;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.ElseBlock;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredIf
extends AbstractStructuredStatement
implements CanRemovePointlessBlock {
    ConditionalExpression conditionalExpression;
    Op04StructuredStatement ifTaken;
    Op04StructuredStatement elseBlock;

    public StructuredIf(BytecodeLoc loc, ConditionalExpression conditionalExpression, Op04StructuredStatement ifTaken) {
        this(loc, conditionalExpression, ifTaken, null);
    }

    public StructuredIf(BytecodeLoc loc, ConditionalExpression conditionalExpression, Op04StructuredStatement ifTaken, Op04StructuredStatement elseBlock) {
        super(loc);
        this.conditionalExpression = conditionalExpression;
        this.ifTaken = ifTaken;
        this.elseBlock = elseBlock;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.conditionalExpression.collectTypeUsages(collector);
        collector.collectFrom(this.ifTaken);
        collector.collectFrom(this.elseBlock);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.conditionalExpression);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.keyword("if ").separator("(").dump(this.conditionalExpression).separator(") ");
        this.ifTaken.dump(dumper);
        if (this.elseBlock != null) {
            dumper.removePendingCarriageReturn();
            dumper.print(" else ");
            this.elseBlock.dump(dumper);
        }
        return dumper;
    }

    public boolean hasElseBlock() {
        return this.elseBlock != null;
    }

    public ConditionalExpression getConditionalExpression() {
        return this.conditionalExpression;
    }

    public Op04StructuredStatement getIfTaken() {
        return this.ifTaken;
    }

    @Override
    public StructuredStatement informBlockHeirachy(Vector<BlockIdentifier> blockIdentifiers) {
        this.ifTaken.informBlockMembership(blockIdentifiers);
        if (this.elseBlock != null) {
            this.elseBlock.informBlockMembership(blockIdentifiers);
        }
        return null;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
        this.ifTaken.transform(transformer, scope);
        if (this.elseBlock != null) {
            this.elseBlock.transform(transformer, scope);
        }
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
        this.ifTaken.linearizeStatementsInto(out);
        if (this.elseBlock != null) {
            out.add(new ElseBlock());
            this.elseBlock.linearizeStatementsInto(out);
        }
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        boolean ifCanDefine = scopeDiscoverer.ifCanDefine();
        if (ifCanDefine) {
            scopeDiscoverer.enterBlock(this);
        }
        this.conditionalExpression.collectUsedLValues(scopeDiscoverer);
        scopeDiscoverer.processOp04Statement(this.ifTaken);
        if (this.elseBlock != null) {
            scopeDiscoverer.processOp04Statement(this.elseBlock);
        }
        if (ifCanDefine) {
            scopeDiscoverer.leaveBlock(this);
        }
    }

    @Override
    public boolean canDefine(LValue scopedEntity, ScopeDiscoverInfoCache factCache) {
        Boolean hasInstanceOf = factCache.get(this);
        if (hasInstanceOf == null) {
            hasInstanceOf = InstanceOfAssignRewriter.hasInstanceOf(this.conditionalExpression);
            factCache.put(this, hasInstanceOf);
        }
        if (!hasInstanceOf.booleanValue()) {
            return false;
        }
        return new InstanceOfAssignRewriter(scopedEntity).isMatchFor(this.conditionalExpression);
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {
        this.conditionalExpression = new InstanceOfAssignRewriter(scopedEntity).rewriteDefining(this.conditionalExpression);
    }

    @Override
    public boolean isRecursivelyStructured() {
        if (!this.ifTaken.isFullyStructured()) {
            return false;
        }
        return this.elseBlock == null || this.elseBlock.isFullyStructured();
    }

    @Override
    public boolean fallsNopToNext() {
        return true;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredIf)) {
            return false;
        }
        StructuredIf other = (StructuredIf)o;
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

    public StructuredStatement convertToAssertion(StructuredAssert structuredAssert) {
        if (this.elseBlock == null) {
            return structuredAssert;
        }
        LinkedList<Op04StructuredStatement> list = ListFactory.newLinkedList();
        list.add(new Op04StructuredStatement(structuredAssert));
        list.add(this.elseBlock);
        return new Block(list, false);
    }

    @Override
    public void removePointlessBlocks(StructuredScope scope) {
        if (this.elseBlock != null && this.elseBlock.getStatement().isEffectivelyNOP()) {
            this.elseBlock = null;
        }
    }
}

