/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AnonBreakTarget;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.WhileStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ConditionalUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredAnonymousBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredContinue;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredIf;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.output.Dumper;

public class IfStatement
extends GotoStatement {
    private static final int JUMP_NOT_TAKEN = 0;
    private static final int JUMP_TAKEN = 1;
    private ConditionalExpression condition;
    private BlockIdentifier knownIfBlock = null;
    private BlockIdentifier knownElseBlock = null;

    public IfStatement(BytecodeLoc loc, ConditionalExpression conditionalExpression) {
        super(loc);
        this.condition = conditionalExpression;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.condition);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("if").print(" ").separator("(").dump(this.condition).separator(")").print(" ");
        return super.dump(dumper);
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        Expression replacementCondition = this.condition.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, this.getContainer());
        if (replacementCondition != this.condition) {
            this.condition = (ConditionalExpression)replacementCondition;
        }
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        this.condition = expressionRewriter.rewriteExpression(this.condition, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        this.condition.collectUsedLValues(lValueUsageCollector);
    }

    public ConditionalExpression getCondition() {
        return this.condition;
    }

    public void setCondition(ConditionalExpression condition) {
        this.condition = condition;
    }

    public void simplifyCondition() {
        this.condition = ConditionalUtils.simplify(this.condition);
    }

    public void negateCondition() {
        this.condition = ConditionalUtils.simplify(this.condition.getNegated());
    }

    public void replaceWithWhileLoopStart(BlockIdentifier blockIdentifier) {
        WhileStatement replacement = new WhileStatement(this.getLoc(), ConditionalUtils.simplify(this.condition.getNegated()), blockIdentifier);
        this.getContainer().replaceStatement(replacement);
    }

    public void replaceWithWhileLoopEnd(BlockIdentifier blockIdentifier) {
        WhileStatement replacement = new WhileStatement(this.getLoc(), ConditionalUtils.simplify(this.condition), blockIdentifier);
        this.getContainer().replaceStatement(replacement);
    }

    @Override
    public Statement getJumpTarget() {
        return this.getTargetStatement(1);
    }

    @Override
    public boolean isConditional() {
        return true;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return this.condition.canThrow(caught);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        switch (this.getJumpType()) {
            case GOTO: 
            case GOTO_OUT_OF_IF: 
            case GOTO_OUT_OF_TRY: {
                return new UnstructuredIf(this.getLoc(), this.condition, this.knownIfBlock, this.knownElseBlock);
            }
            case CONTINUE: {
                return new StructuredIf(this.getLoc(), this.condition, new Op04StructuredStatement(new UnstructuredContinue(this.getLoc(), this.getTargetStartBlock())));
            }
            case BREAK: {
                return new StructuredIf(this.getLoc(), this.condition, new Op04StructuredStatement(new UnstructuredBreak(this.getLoc(), this.getJumpTarget().getContainer().getBlocksEnded())));
            }
            case BREAK_ANONYMOUS: {
                Statement target = this.getJumpTarget();
                if (!(target instanceof AnonBreakTarget)) {
                    throw new IllegalStateException("Target of anonymous break unexpected.");
                }
                AnonBreakTarget anonBreakTarget = (AnonBreakTarget)target;
                BlockIdentifier breakFrom = anonBreakTarget.getBlockIdentifier();
                Op04StructuredStatement unstructuredBreak = new Op04StructuredStatement(new UnstructuredAnonymousBreak(this.getLoc(), breakFrom));
                return new StructuredIf(this.getLoc(), this.condition, unstructuredBreak);
            }
        }
        throw new UnsupportedOperationException("Unexpected jump type in if block - " + (Object)((Object)this.getJumpType()));
    }

    public void setKnownBlocks(BlockIdentifier ifBlock, BlockIdentifier elseBlock) {
        this.knownIfBlock = ifBlock;
        this.knownElseBlock = elseBlock;
    }

    public Pair<BlockIdentifier, BlockIdentifier> getBlocks() {
        return Pair.make(this.knownIfBlock, this.knownElseBlock);
    }

    public BlockIdentifier getKnownIfBlock() {
        return this.knownIfBlock;
    }

    public boolean hasElseBlock() {
        return this.knownElseBlock != null;
    }

    public void optimiseForTypes() {
        this.condition = this.condition.optimiseForType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        IfStatement that = (IfStatement)o;
        return !(this.condition != null ? !this.condition.equals(that.condition) : that.condition != null);
    }
}

