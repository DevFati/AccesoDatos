/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.MonitorStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredSynchronized;
import org.benf.cfr.reader.util.output.Dumper;

public class MonitorEnterStatement
extends MonitorStatement {
    private Expression monitor;
    private final BlockIdentifier blockIdentifier;

    public MonitorEnterStatement(BytecodeLoc loc, Expression monitor, BlockIdentifier blockIdentifier) {
        super(loc);
        this.monitor = monitor;
        this.blockIdentifier = blockIdentifier;
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new MonitorEnterStatement(this.getLoc(), cloneHelper.replaceOrClone(this.monitor), this.blockIdentifier);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.monitor);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("MONITORENTER : ").dump(this.monitor);
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        this.monitor = this.monitor.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, this.getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        this.monitor = expressionRewriter.rewriteExpression(this.monitor, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        this.monitor.collectUsedLValues(lValueUsageCollector);
    }

    public Expression getMonitor() {
        return this.monitor;
    }

    public BlockIdentifier getBlockIdentifier() {
        return this.blockIdentifier;
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredSynchronized(this.getLoc(), this.monitor, this.blockIdentifier);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        MonitorEnterStatement other = (MonitorEnterStatement)o;
        return constraint.equivalent(this.monitor, other.monitor);
    }
}

