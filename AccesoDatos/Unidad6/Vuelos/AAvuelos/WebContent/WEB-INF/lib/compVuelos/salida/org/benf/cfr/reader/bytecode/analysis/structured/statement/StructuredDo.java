/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredConditionalLoopStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredWhile;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredDo
extends AbstractStructuredConditionalLoopStatement {
    private StructuredDo(ConditionalExpression condition, Op04StructuredStatement body, BlockIdentifier block) {
        super(BytecodeLoc.NONE, condition, block, body);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (this.block.hasForeignReferences()) {
            dumper.label(this.block.getName(), true);
        }
        dumper.print("do ");
        this.getBody().dump(dumper);
        dumper.removePendingCarriageReturn();
        dumper.print(" while (");
        if (this.condition == null) {
            dumper.print("true");
        } else {
            dumper.dump(this.condition);
        }
        return dumper.print(");").newln();
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredDo)) {
            return false;
        }
        StructuredDo other = (StructuredDo)o;
        if (this.condition == null ? other.condition != null : !this.condition.equals(other.condition)) {
            return false;
        }
        if (!this.block.equals(other.block)) {
            return false;
        }
        matchIterator.advance();
        return true;
    }

    public static AbstractStructuredConditionalLoopStatement create(ConditionalExpression condition, Op04StructuredStatement body, BlockIdentifier block) {
        if (condition == null) {
            return new StructuredWhile(null, body, block);
        }
        return new StructuredDo(condition, body, block);
    }
}

