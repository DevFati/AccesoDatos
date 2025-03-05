/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesTransformerBase;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredTry;
import org.benf.cfr.reader.entities.ClassFile;

public abstract class TryResourceTransformerFinally
extends TryResourcesTransformerBase {
    public TryResourceTransformerFinally(ClassFile classFile) {
        super(classFile);
    }

    @Override
    protected TryResourcesTransformerBase.ResourceMatch getResourceMatch(StructuredTry structuredTry, StructuredScope scope) {
        Op04StructuredStatement finallyBlock = structuredTry.getFinallyBlock();
        return this.findResourceFinally(finallyBlock);
    }

    protected abstract TryResourcesTransformerBase.ResourceMatch findResourceFinally(Op04StructuredStatement var1);
}

