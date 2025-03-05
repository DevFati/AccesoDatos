/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import java.util.Vector;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredFor;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.output.Dumper;

public class UnstructuredFor
extends AbstractUnStructuredStatement {
    private ConditionalExpression condition;
    private BlockIdentifier blockIdentifier;
    private AssignmentSimple initial;
    private List<AbstractAssignmentExpression> assignments;

    public UnstructuredFor(BytecodeLoc loc, ConditionalExpression condition, BlockIdentifier blockIdentifier, AssignmentSimple initial, List<AbstractAssignmentExpression> assignments) {
        super(loc);
        this.condition = condition;
        this.blockIdentifier = blockIdentifier;
        this.initial = initial;
        this.assignments = assignments;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.assignments, this.condition, this.initial);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(this.condition);
        collector.collectFrom(this.assignments);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("** for (").dump(this.initial).print("; ").dump(this.condition).print("; ");
        boolean first = true;
        for (AbstractAssignmentExpression assignment : this.assignments) {
            first = StringUtils.comma(first, dumper);
            dumper.dump(assignment);
        }
        return dumper.separator(")").newln();
    }

    @Override
    public StructuredStatement claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier blockIdentifier, Vector<BlockIdentifier> blocksCurrentlyIn) {
        if (blockIdentifier != this.blockIdentifier) {
            throw new RuntimeException("For statement claiming wrong block");
        }
        innerBlock.removeLastContinue(blockIdentifier);
        return new StructuredFor(this.getLoc(), this.condition, this.initial, this.assignments, innerBlock, blockIdentifier);
    }
}

