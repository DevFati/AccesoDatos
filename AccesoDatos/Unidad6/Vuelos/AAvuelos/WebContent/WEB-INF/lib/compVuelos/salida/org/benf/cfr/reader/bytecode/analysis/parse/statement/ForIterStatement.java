/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredIter;
import org.benf.cfr.reader.util.output.Dumper;

public class ForIterStatement
extends AbstractStatement {
    private BlockIdentifier blockIdentifier;
    private LValue iterator;
    private Expression list;
    private LValue hiddenList;

    public ForIterStatement(BytecodeLoc loc, BlockIdentifier blockIdentifier, LValue iterator, Expression list, LValue hiddenList) {
        super(loc);
        this.blockIdentifier = blockIdentifier;
        this.iterator = iterator;
        this.list = list;
        this.hiddenList = hiddenList;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.list);
    }

    @Override
    public LValue getCreatedLValue() {
        return this.iterator;
    }

    public Expression getList() {
        return this.list;
    }

    public LValue getHiddenList() {
        return this.hiddenList;
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new ForIterStatement(this.getLoc(), this.blockIdentifier, cloneHelper.replaceOrClone(this.iterator), cloneHelper.replaceOrClone(this.list), cloneHelper.replaceOrClone(this.hiddenList));
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.keyword("for ").separator("(");
        if (this.iterator.isFinal()) {
            dumper.keyword("final ");
        }
        dumper.dump(this.iterator).separator(" : ").dump(this.list).separator(")");
        dumper.comment(" // ends " + this.getTargetStatement(1).getContainer().getLabel() + ";").newln();
        return dumper;
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        this.iterator.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, this.getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        this.iterator = expressionRewriter.rewriteExpression(this.iterator, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE);
        this.list = expressionRewriter.rewriteExpression(this.list, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        this.list.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredIter(this.getLoc(), this.blockIdentifier, this.iterator, this.list);
    }

    public BlockIdentifier getBlockIdentifier() {
        return this.blockIdentifier;
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
        ForIterStatement other = (ForIterStatement)o;
        if (!constraint.equivalent(this.iterator, other.iterator)) {
            return false;
        }
        return constraint.equivalent(this.list, other.list);
    }
}

