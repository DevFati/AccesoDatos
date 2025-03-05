/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import java.util.Vector;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredSwitch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredCase;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.output.Dumper;

public class UnstructuredSwitch
extends AbstractUnStructuredStatement {
    private Expression switchOn;
    private final BlockIdentifier blockIdentifier;
    private boolean safeExpression;

    public UnstructuredSwitch(BytecodeLoc loc, Expression switchOn, BlockIdentifier blockIdentifier, boolean safeExpression) {
        super(loc);
        this.switchOn = switchOn;
        this.blockIdentifier = blockIdentifier;
        this.safeExpression = safeExpression;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.print("** switch (").dump(this.switchOn).separator(")").newln();
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.switchOn);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(this.switchOn);
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        Block block;
        List<Op04StructuredStatement> statements;
        Op04StructuredStatement last;
        if (blockIdentifier != this.blockIdentifier) {
            throw new ConfusedCFRException("Unstructured switch being asked to claim wrong block. [" + blockIdentifier + " != " + this.blockIdentifier + "]");
        }
        if (innerBlock.getStatement() instanceof Block && (last = (statements = (block = (Block)innerBlock.getStatement()).getBlockStatements()).get(statements.size() - 1)).getStatement() instanceof UnstructuredCase) {
            UnstructuredCase caseStatement = (UnstructuredCase)last.getStatement();
            last.replaceStatement(caseStatement.getEmptyStructuredCase());
        }
        return new StructuredSwitch(this.getLoc(), this.switchOn, innerBlock, blockIdentifier, this.safeExpression);
    }
}

