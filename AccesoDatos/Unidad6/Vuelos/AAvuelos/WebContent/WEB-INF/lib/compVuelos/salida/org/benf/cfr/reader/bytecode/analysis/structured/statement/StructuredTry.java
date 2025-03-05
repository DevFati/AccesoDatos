/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredFinally;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredTry
extends AbstractStructuredStatement {
    private Op04StructuredStatement tryBlock;
    private List<Op04StructuredStatement> catchBlocks = ListFactory.newList();
    private Op04StructuredStatement finallyBlock;
    private final BlockIdentifier tryBlockIdentifier;
    private List<Op04StructuredStatement> resourceBlock;

    public StructuredTry(Op04StructuredStatement tryBlock, BlockIdentifier tryBlockIdentifier) {
        super(BytecodeLoc.NONE);
        this.tryBlock = tryBlock;
        this.finallyBlock = null;
        this.tryBlockIdentifier = tryBlockIdentifier;
        this.resourceBlock = null;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    public void addResources(List<Op04StructuredStatement> resources) {
        if (this.resourceBlock == null) {
            this.resourceBlock = ListFactory.newList();
        }
        this.resourceBlock.addAll(resources);
    }

    public List<Op04StructuredStatement> getResources() {
        return this.resourceBlock;
    }

    public boolean hasResources() {
        return this.resourceBlock != null;
    }

    public Op04StructuredStatement getTryBlock() {
        return this.tryBlock;
    }

    public List<Op04StructuredStatement> getCatchBlocks() {
        return this.catchBlocks;
    }

    public void clearCatchBlocks() {
        this.catchBlocks.clear();
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("try ");
        if (this.resourceBlock != null) {
            dumper.separator("(");
            boolean first = true;
            for (Op04StructuredStatement resource : this.resourceBlock) {
                if (!first) {
                    dumper.print("     ");
                }
                resource.dump(dumper);
                first = false;
            }
            dumper.removePendingCarriageReturn();
            dumper.separator(")");
        }
        this.tryBlock.dump(dumper);
        for (Op04StructuredStatement catchBlock : this.catchBlocks) {
            catchBlock.dump(dumper);
        }
        if (this.finallyBlock != null) {
            this.finallyBlock.dump(dumper);
        }
        return dumper;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(this.tryBlock);
        collector.collectFrom(this.catchBlocks);
        collector.collectFrom(this.finallyBlock);
        collector.collectFrom(this.resourceBlock);
    }

    @Override
    public boolean isProperlyStructured() {
        return true;
    }

    @Override
    public boolean fallsNopToNext() {
        return true;
    }

    void addCatch(Op04StructuredStatement catchStatement) {
        this.catchBlocks.add(catchStatement);
    }

    public void setFinally(Op04StructuredStatement finallyBlock) {
        this.finallyBlock = finallyBlock;
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
        if (this.resourceBlock != null) {
            for (Op04StructuredStatement resource : this.resourceBlock) {
                resource.transform(transformer, scope);
            }
        }
        this.tryBlock.transform(transformer, scope);
        for (Op04StructuredStatement catchBlock : this.catchBlocks) {
            catchBlock.transform(transformer, scope);
        }
        if (this.finallyBlock != null) {
            this.finallyBlock.transform(transformer, scope);
        }
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
        if (this.resourceBlock != null) {
            for (Op04StructuredStatement resource : this.resourceBlock) {
                out.add(resource.getStatement());
            }
        }
        this.tryBlock.linearizeStatementsInto(out);
        for (Op04StructuredStatement catchBlock : this.catchBlocks) {
            catchBlock.linearizeStatementsInto(out);
        }
        if (this.finallyBlock != null) {
            this.finallyBlock.linearizeStatementsInto(out);
        }
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        if (this.resourceBlock != null) {
            scopeDiscoverer.enterBlock(this);
            for (Op04StructuredStatement resource : this.resourceBlock) {
                scopeDiscoverer.processOp04Statement(resource);
            }
        }
        scopeDiscoverer.processOp04Statement(this.tryBlock);
        for (Op04StructuredStatement catchBlock : this.catchBlocks) {
            scopeDiscoverer.processOp04Statement(catchBlock);
        }
        if (this.finallyBlock != null) {
            scopeDiscoverer.processOp04Statement(this.finallyBlock);
        }
        if (this.resourceBlock != null) {
            scopeDiscoverer.leaveBlock(this);
        }
    }

    @Override
    public boolean isRecursivelyStructured() {
        if (this.resourceBlock != null) {
            for (Op04StructuredStatement resource : this.resourceBlock) {
                if (resource.isFullyStructured()) continue;
                return false;
            }
        }
        if (!this.tryBlock.isFullyStructured()) {
            return false;
        }
        for (Op04StructuredStatement catchBlock : this.catchBlocks) {
            if (catchBlock.isFullyStructured()) continue;
            return false;
        }
        return this.finallyBlock == null || this.finallyBlock.isFullyStructured();
    }

    public Op04StructuredStatement getFinallyBlock() {
        return this.finallyBlock;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredTry)) {
            return false;
        }
        matchIterator.advance();
        return true;
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
    }

    private boolean isPointlessTry() {
        if (!this.catchBlocks.isEmpty()) {
            return false;
        }
        if (this.finallyBlock == null) {
            return true;
        }
        if (!(this.finallyBlock.getStatement() instanceof StructuredFinally)) {
            return false;
        }
        StructuredFinally structuredFinally = (StructuredFinally)this.finallyBlock.getStatement();
        Op04StructuredStatement finallyCode = structuredFinally.getCatchBlock();
        if (!(finallyCode.getStatement() instanceof Block)) {
            return false;
        }
        Block block = (Block)finallyCode.getStatement();
        return block.isEffectivelyNOP();
    }

    private boolean isJustTryCatchThrow() {
        if (this.resourceBlock != null) {
            return false;
        }
        if (this.finallyBlock != null) {
            return false;
        }
        if (this.catchBlocks.size() != 1) {
            return false;
        }
        Op04StructuredStatement catchBlock = this.catchBlocks.get(0);
        StructuredStatement catchS = catchBlock.getStatement();
        if (!(catchS instanceof StructuredCatch)) {
            return false;
        }
        StructuredCatch structuredCatch = (StructuredCatch)catchS;
        return structuredCatch.isRethrow();
    }

    @Override
    public boolean inlineable() {
        return this.isPointlessTry() || this.isJustTryCatchThrow();
    }

    public BlockIdentifier getTryBlockIdentifier() {
        return this.tryBlockIdentifier;
    }

    @Override
    public Op04StructuredStatement getInline() {
        return this.tryBlock;
    }

    public void setTryBlock(Op04StructuredStatement tryBlock) {
        this.tryBlock = tryBlock;
    }
}

