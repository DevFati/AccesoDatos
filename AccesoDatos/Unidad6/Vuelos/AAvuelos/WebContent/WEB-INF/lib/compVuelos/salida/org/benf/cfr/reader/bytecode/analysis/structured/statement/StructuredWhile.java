/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredConditionalLoopStatement;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredWhile
extends AbstractStructuredConditionalLoopStatement {
    public StructuredWhile(ConditionalExpression condition, Op04StructuredStatement body, BlockIdentifier block) {
        super(BytecodeLoc.NONE, condition, block, body);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.getCondition());
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (this.block.hasForeignReferences()) {
            dumper.label(this.block.getName(), true);
        }
        dumper.print("while (");
        if (this.condition == null) {
            dumper.print("true");
        } else {
            dumper.dump(this.condition);
        }
        dumper.print(") ");
        this.getBody().dump(dumper);
        return dumper;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredWhile)) {
            return false;
        }
        StructuredWhile other = (StructuredWhile)o;
        if (this.condition == null ? other.condition != null : !this.condition.equals(other.condition)) {
            return false;
        }
        if (!this.block.equals(other.block)) {
            return false;
        }
        matchIterator.advance();
        return true;
    }
}

