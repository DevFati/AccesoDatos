/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Triplet;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredBreak
extends AbstractStructuredStatement {
    private final BlockIdentifier breakBlock;
    private final boolean localBreak;

    public StructuredBreak(BytecodeLoc loc, BlockIdentifier breakBlock, boolean localBreak) {
        super(loc);
        this.breakBlock = breakBlock;
        this.localBreak = localBreak;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (this.localBreak) {
            dumper.keyword("break").print(";");
        } else {
            dumper.keyword("break ").print(this.breakBlock.getName() + ";");
        }
        dumper.newln();
        return dumper;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    public boolean isLocalBreak() {
        return this.localBreak;
    }

    public BlockIdentifier getBreakBlock() {
        return this.breakBlock;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredBreak)) {
            return false;
        }
        StructuredBreak other = (StructuredBreak)o;
        if (!this.breakBlock.equals(other.breakBlock)) {
            return false;
        }
        matchIterator.advance();
        return true;
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
    }

    public StructuredBreak maybeTightenToLocal(Stack<Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>>> scopes) {
        if (this.localBreak) {
            return this;
        }
        if (scopes.isEmpty()) {
            return this;
        }
        Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>> local = scopes.peek();
        if (local.getSecond() == this.breakBlock) {
            return this;
        }
        for (int i = scopes.size() - 2; i >= 0; --i) {
            Set actualNext;
            Triplet scope = (Triplet)scopes.get(i);
            if (scope.getSecond() != this.breakBlock) continue;
            Set<Op04StructuredStatement> localNext = local.getThird();
            if (localNext.containsAll(actualNext = (Set)scope.getThird())) {
                this.breakBlock.releaseForeignRef();
                return new StructuredBreak(this.getLoc(), local.getSecond(), true);
            }
            return this;
        }
        return this;
    }
}

