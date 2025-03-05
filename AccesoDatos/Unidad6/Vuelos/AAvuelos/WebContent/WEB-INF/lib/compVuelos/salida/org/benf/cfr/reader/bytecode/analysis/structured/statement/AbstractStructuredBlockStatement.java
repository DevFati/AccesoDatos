/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.state.TypeUsageCollector;

public abstract class AbstractStructuredBlockStatement
extends AbstractStructuredStatement {
    private Op04StructuredStatement body;

    AbstractStructuredBlockStatement(BytecodeLoc loc, Op04StructuredStatement body) {
        super(loc);
        this.body = body;
    }

    public Op04StructuredStatement getBody() {
        return this.body;
    }

    @Override
    public boolean isRecursivelyStructured() {
        return this.body.isFullyStructured();
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
        this.getBody().transform(transformer, scope);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (!collector.isStatementRecursive()) {
            return;
        }
        this.body.collectTypeUsages(collector);
    }
}

