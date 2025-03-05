/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import java.util.Collections;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
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
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredSwitch;
import org.benf.cfr.reader.util.output.Dumper;

public class SwitchStatement
extends AbstractStatement {
    private Expression switchOn;
    private final BlockIdentifier switchBlock;
    private boolean safeExpression = false;

    SwitchStatement(BytecodeLoc loc, Expression switchOn, BlockIdentifier switchBlock) {
        super(loc);
        this.switchOn = switchOn;
        this.switchBlock = switchBlock;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.switchOn);
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        SwitchStatement res = new SwitchStatement(this.getLoc(), cloneHelper.replaceOrClone(this.switchOn), this.switchBlock);
        res.safeExpression = this.safeExpression;
        return res;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("switch (").dump(this.switchOn).print(") { // " + this.switchBlock).newln();
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        this.switchOn = this.switchOn.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, this.getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        this.switchOn = expressionRewriter.rewriteExpression(this.switchOn, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        this.switchOn.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredSwitch(this.getLoc(), this.switchOn, this.switchBlock, this.safeExpression);
    }

    public Expression getSwitchOn() {
        return this.switchOn;
    }

    public void setSwitchOn(Expression switchOn) {
        this.switchOn = switchOn;
    }

    public BlockIdentifier getSwitchBlock() {
        return this.switchBlock;
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
        SwitchStatement other = (SwitchStatement)o;
        return constraint.equivalent(this.switchOn, other.switchOn);
    }

    @Override
    public boolean fallsToNext() {
        return false;
    }

    @Override
    public Set<LValue> wantsLifetimeHint() {
        if (this.switchOn instanceof LValueExpression) {
            return Collections.singleton(((LValueExpression)this.switchOn).getLValue());
        }
        return null;
    }

    @Override
    public void setLifetimeHint(LValue lv, boolean usedInChildren) {
        if (!usedInChildren) {
            this.safeExpression = true;
        }
    }
}

