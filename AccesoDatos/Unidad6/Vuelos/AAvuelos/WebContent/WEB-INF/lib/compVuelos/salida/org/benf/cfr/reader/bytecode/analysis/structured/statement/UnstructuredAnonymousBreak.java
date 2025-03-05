/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.Vector;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class UnstructuredAnonymousBreak
extends AbstractUnStructuredStatement {
    private final BlockIdentifier blockEnding;

    public UnstructuredAnonymousBreak(BytecodeLoc loc, BlockIdentifier blockEnding) {
        super(loc);
        this.blockEnding = blockEnding;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** break ").print(this.blockEnding.getName()).newln();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    @Override
    public StructuredStatement informBlockHeirachy(Vector<BlockIdentifier> blockIdentifiers) {
        return null;
    }

    StructuredStatement tryExplicitlyPlaceInBlock(BlockIdentifier block) {
        if (block != this.blockEnding) {
            return this;
        }
        block.addForeignRef();
        return new StructuredBreak(this.getLoc(), block, false);
    }
}

