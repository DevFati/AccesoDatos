/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ForStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredWhile;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.output.Dumper;

public class WhileStatement
extends AbstractStatement {
    private ConditionalExpression condition;
    private BlockIdentifier blockIdentifier;

    public WhileStatement(BytecodeLoc loc, ConditionalExpression conditionalExpression, BlockIdentifier blockIdentifier) {
        super(loc);
        this.condition = conditionalExpression;
        this.blockIdentifier = blockIdentifier;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.condition);
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new WhileStatement(this.getLoc(), (ConditionalExpression)cloneHelper.replaceOrClone(this.condition), this.blockIdentifier);
    }

    private int getBackJumpIndex() {
        return this.condition == null ? 0 : 1;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("while (");
        if (this.condition == null) {
            dumper.print("true");
        } else {
            dumper.dump(this.condition);
        }
        dumper.print(") ");
        dumper.print(" // ends " + this.getTargetStatement(this.getBackJumpIndex()).getContainer().getLabel() + ";").newln();
        return dumper;
    }

    public void replaceWithForLoop(AssignmentSimple initial, List<AbstractAssignmentExpression> assignment) {
        if (this.condition == null) {
            throw new UnsupportedOperationException();
        }
        ForStatement forStatement = new ForStatement(this.getLoc(), this.condition, this.blockIdentifier, initial, assignment);
        this.getContainer().replaceStatement(forStatement);
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        if (this.condition == null) {
            return;
        }
        Expression replacementCondition = this.condition.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, this.getContainer());
        if (replacementCondition != this.condition) {
            throw new ConfusedCFRException("Can't yet support replacing conditions");
        }
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        if (this.condition == null) {
            return;
        }
        this.condition = expressionRewriter.rewriteExpression(this.condition, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        if (this.condition != null) {
            this.condition.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredWhile(this.getLoc(), this.condition, this.blockIdentifier, this.getTargetStatement(this.getBackJumpIndex()).getContainer().getBlocksEnded());
    }

    public BlockIdentifier getBlockIdentifier() {
        return this.blockIdentifier;
    }

    public ConditionalExpression getCondition() {
        return this.condition;
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
        WhileStatement other = (WhileStatement)o;
        return constraint.equivalent(this.condition, other.condition);
    }
}

