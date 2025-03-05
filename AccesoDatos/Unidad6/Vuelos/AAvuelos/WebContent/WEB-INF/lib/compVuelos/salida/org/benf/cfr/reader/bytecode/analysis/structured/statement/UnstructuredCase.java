/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import java.util.Vector;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.output.Dumper;

public class UnstructuredCase
extends AbstractUnStructuredStatement {
    private final List<Expression> values;
    private final BlockIdentifier blockIdentifier;
    private final InferredJavaType caseType;

    public UnstructuredCase(List<Expression> values, InferredJavaType caseType, BlockIdentifier blockIdentifier) {
        super(BytecodeLoc.NONE);
        this.values = values;
        this.caseType = caseType;
        this.blockIdentifier = blockIdentifier;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (this.values.isEmpty()) {
            dumper.print("** default:").newln();
        } else {
            for (Expression value : this.values) {
                dumper.print("** case ").dump(value).print(":").newln();
            }
        }
        return dumper;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(this.values);
        collector.collect(this.caseType.getJavaTypeInstance());
    }

    StructuredStatement getEmptyStructuredCase() {
        Op04StructuredStatement container = this.getContainer();
        return new StructuredCase(BytecodeLoc.TODO, this.values, this.caseType, new Op04StructuredStatement(container.getIndex().justAfter(), container.getBlockMembership(), Block.getEmptyBlock(false)), this.blockIdentifier);
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier != this.blockIdentifier) {
            throw new ConfusedCFRException("Unstructured case being asked to claim wrong block. [" + blockIdentifier + " != " + this.blockIdentifier + "]");
        }
        return new StructuredCase(BytecodeLoc.TODO, this.values, this.caseType, innerBlock, blockIdentifier);
    }
}

