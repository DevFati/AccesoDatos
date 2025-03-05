/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredThrow;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Optional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredCatch
extends AbstractStructuredStatement {
    private final List<JavaRefTypeInstance> catchTypes;
    private final Op04StructuredStatement catchBlock;
    private final LValue catching;
    private final Set<BlockIdentifier> possibleTryBlocks;

    public StructuredCatch(Collection<JavaRefTypeInstance> catchTypes, Op04StructuredStatement catchBlock, LValue catching, Set<BlockIdentifier> possibleTryBlocks) {
        super(BytecodeLoc.NONE);
        this.catchTypes = catchTypes == null ? null : ListFactory.newList(catchTypes);
        this.catchBlock = catchBlock;
        this.catching = catching;
        this.possibleTryBlocks = possibleTryBlocks;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.catchTypes);
        if (!collector.isStatementRecursive()) {
            return;
        }
        this.catchBlock.collectTypeUsages(collector);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    public List<JavaRefTypeInstance> getCatchTypes() {
        return this.catchTypes;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        boolean first = true;
        dumper.keyword("catch ").separator("(");
        for (JavaRefTypeInstance catchType : this.catchTypes) {
            if (!first) {
                dumper.operator(" | ");
            }
            dumper.dump(catchType);
            first = false;
        }
        dumper.print(" ").dump(this.catching).separator(") ");
        this.catchBlock.dump(dumper);
        return dumper;
    }

    @Override
    public boolean isProperlyStructured() {
        return true;
    }

    @Override
    public boolean fallsNopToNext() {
        return true;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
        this.catchBlock.transform(transformer, scope);
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
        this.catchBlock.linearizeStatementsInto(out);
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredCatch)) {
            return false;
        }
        StructuredCatch other = (StructuredCatch)o;
        matchIterator.advance();
        return true;
    }

    public boolean isRethrow() {
        StructuredStatement statement = this.catchBlock.getStatement();
        if (!(statement instanceof Block)) {
            return false;
        }
        Block block = (Block)statement;
        Optional<Op04StructuredStatement> maybeStatement = block.getMaybeJustOneStatement();
        if (!maybeStatement.isSet()) {
            return false;
        }
        StructuredStatement inBlock = maybeStatement.getValue().getStatement();
        StructuredThrow test = new StructuredThrow(BytecodeLoc.NONE, new LValueExpression(this.catching));
        return test.equals(inBlock);
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        if (this.catching instanceof LocalVariable) {
            scopeDiscoverer.collectLocalVariableAssignment((LocalVariable)this.catching, this.getContainer(), (Expression)null);
        }
        scopeDiscoverer.processOp04Statement(this.catchBlock);
    }

    @Override
    public List<LValue> findCreatedHere() {
        return ListFactory.newImmutableList(this.catching);
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        expressionRewriter.handleStatement(this.getContainer());
    }

    public Set<BlockIdentifier> getPossibleTryBlocks() {
        return this.possibleTryBlocks;
    }

    @Override
    public boolean isRecursivelyStructured() {
        return this.catchBlock.isFullyStructured();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        StructuredCatch that = (StructuredCatch)o;
        return !(this.catching != null ? !this.catching.equals(that.catching) : that.catching != null);
    }
}

