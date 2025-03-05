/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.Set;
import java.util.Vector;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class UnstructuredBreak
extends AbstractUnStructuredStatement {
    private final Set<BlockIdentifier> blocksEnding;

    public UnstructuredBreak(BytecodeLoc loc, Set<BlockIdentifier> blocksEnding) {
        super(loc);
        this.blocksEnding = blocksEnding;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** break;").newln();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    @Override
    public StructuredStatement informBlockHeirachy(Vector<BlockIdentifier> blockIdentifiers) {
        int index = Integer.MAX_VALUE;
        BlockIdentifier bestBlock = null;
        for (BlockIdentifier block : this.blocksEnding) {
            int posn = blockIdentifiers.indexOf(block);
            if (posn < 0 || index <= posn) continue;
            index = posn;
            bestBlock = block;
        }
        if (bestBlock == null) {
            return null;
        }
        boolean localBreak = false;
        BlockIdentifier outermostBreakable = BlockIdentifier.getInnermostBreakable(blockIdentifiers);
        if (outermostBreakable == bestBlock) {
            localBreak = true;
        } else {
            bestBlock.addForeignRef();
        }
        return new StructuredBreak(this.getLoc(), bestBlock, localBreak);
    }
}

