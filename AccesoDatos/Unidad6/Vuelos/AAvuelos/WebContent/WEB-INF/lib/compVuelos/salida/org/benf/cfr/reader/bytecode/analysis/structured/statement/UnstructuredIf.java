/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.LinkedList;
import java.util.Vector;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ConditionalUtils;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredGoto;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.Optional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class UnstructuredIf
extends AbstractUnStructuredStatement {
    private ConditionalExpression conditionalExpression;
    private Op04StructuredStatement setIfBlock;
    private BlockIdentifier knownIfBlock;
    private BlockIdentifier knownElseBlock;

    public UnstructuredIf(BytecodeLoc loc, ConditionalExpression conditionalExpression, BlockIdentifier knownIfBlock, BlockIdentifier knownElseBlock) {
        super(loc);
        this.conditionalExpression = conditionalExpression;
        this.knownIfBlock = knownIfBlock;
        this.knownElseBlock = knownElseBlock;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.conditionalExpression);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(this.conditionalExpression);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("** if (").dump(this.conditionalExpression).print(") goto " + this.getContainer().getTargetLabel(1)).newln();
        if (this.setIfBlock != null) {
            dumper.dump(this.setIfBlock);
        }
        return dumper;
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier == this.knownIfBlock) {
            if (this.knownElseBlock == null) {
                Op04StructuredStatement fakeElse = new Op04StructuredStatement(new UnstructuredGoto(BytecodeLoc.TODO));
                Op04StructuredStatement fakeElseTarget = this.getContainer().getTargets().get(1);
                fakeElse.addTarget(fakeElseTarget);
                fakeElseTarget.addSource(fakeElse);
                LinkedList<Op04StructuredStatement> fakeBlockContent = ListFactory.newLinkedList();
                fakeBlockContent.add(fakeElse);
                Op04StructuredStatement fakeElseBlock = new Op04StructuredStatement(new Block(fakeBlockContent, true));
                return new StructuredIf(this.getLoc(), ConditionalUtils.simplify(this.conditionalExpression.getNegated()), innerBlock, fakeElseBlock);
            }
            this.setIfBlock = innerBlock;
            return this;
        }
        if (blockIdentifier == this.knownElseBlock) {
            if (this.setIfBlock == null) {
                throw new ConfusedCFRException("Set else block before setting IF block");
            }
            if (this.knownIfBlock.getBlockType() == BlockType.SIMPLE_IF_TAKEN) {
                this.setIfBlock.removeLastGoto();
            }
            innerBlock = UnstructuredIf.unpackElseIfBlock(innerBlock);
            return new StructuredIf(this.getLoc(), ConditionalUtils.simplify(this.conditionalExpression.getNegated()), this.setIfBlock, innerBlock);
        }
        return null;
    }

    private static Op04StructuredStatement unpackElseIfBlock(Op04StructuredStatement elseBlock) {
        StructuredStatement elseStmt = elseBlock.getStatement();
        if (!(elseStmt instanceof Block)) {
            return elseBlock;
        }
        Block block = (Block)elseStmt;
        Optional<Op04StructuredStatement> maybeStatement = block.getMaybeJustOneStatement();
        if (!maybeStatement.isSet()) {
            return elseBlock;
        }
        Op04StructuredStatement inner = maybeStatement.getValue();
        if (inner.getStatement() instanceof StructuredIf) {
            return inner;
        }
        return elseBlock;
    }

    public StructuredStatement convertEmptyToGoto() {
        if (this.knownIfBlock != null || this.knownElseBlock != null || this.setIfBlock != null) {
            return this;
        }
        Op04StructuredStatement gotoStm = new Op04StructuredStatement(new UnstructuredGoto(BytecodeLoc.TODO));
        Op04StructuredStatement target = this.getContainer().getTargets().get(1);
        gotoStm.addTarget(target);
        target.getSources().remove(this.getContainer());
        target.addSource(gotoStm);
        return new StructuredIf(this.getLoc(), this.conditionalExpression, gotoStm);
    }
}

