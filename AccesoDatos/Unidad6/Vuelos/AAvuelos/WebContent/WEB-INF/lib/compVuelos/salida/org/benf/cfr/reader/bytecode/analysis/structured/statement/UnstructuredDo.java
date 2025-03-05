/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.LinkedList;
import java.util.Vector;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredContinue;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDo;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredWhile;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Optional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class UnstructuredDo
extends AbstractUnStructuredStatement {
    private BlockIdentifier blockIdentifier;

    public UnstructuredDo(BlockIdentifier blockIdentifier) {
        super(BytecodeLoc.NONE);
        this.blockIdentifier = blockIdentifier;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** do ").newln();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        Op04StructuredStatement last;
        Block block;
        Optional<Op04StructuredStatement> maybeStatement;
        if (blockIdentifier != this.blockIdentifier) {
            throw new RuntimeException("Do statement claiming wrong block");
        }
        UnstructuredWhile lastEndWhile = innerBlock.removeLastEndWhile();
        if (lastEndWhile != null) {
            ConditionalExpression condition = lastEndWhile.getCondition();
            return StructuredDo.create(condition, innerBlock, blockIdentifier);
        }
        StructuredStatement inner = innerBlock.getStatement();
        if (!(inner instanceof Block)) {
            LinkedList<Op04StructuredStatement> blockContent = ListFactory.newLinkedList();
            blockContent.add(new Op04StructuredStatement(inner));
            inner = new Block(blockContent, true);
            innerBlock.replaceStatement(inner);
        }
        if ((maybeStatement = (block = (Block)inner).getMaybeJustOneStatement()).isSet()) {
            Op04StructuredStatement singleStatement = maybeStatement.getValue();
            StructuredStatement stm = singleStatement.getStatement();
            boolean canRemove = true;
            if (stm instanceof StructuredBreak) {
                StructuredBreak brk = (StructuredBreak)stm;
                if (brk.getBreakBlock().equals(blockIdentifier)) {
                    canRemove = false;
                }
            } else if (stm instanceof StructuredContinue) {
                StructuredContinue cnt = (StructuredContinue)stm;
                if (cnt.getContinueTgt().equals(blockIdentifier)) {
                    canRemove = false;
                }
            } else if (stm.canFall()) {
                canRemove = false;
            }
            if (canRemove) {
                return stm;
            }
        }
        if ((last = block.getLast()) != null && last.getStatement().canFall()) {
            block.addStatement(new Op04StructuredStatement(new StructuredBreak(this.getLoc(), blockIdentifier, true)));
        }
        return StructuredDo.create(null, innerBlock, blockIdentifier);
    }
}

