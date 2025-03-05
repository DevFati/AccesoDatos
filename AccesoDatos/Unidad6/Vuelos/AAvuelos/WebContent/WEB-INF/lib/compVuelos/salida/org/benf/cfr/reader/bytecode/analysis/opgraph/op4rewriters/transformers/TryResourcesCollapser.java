/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredTry;
import org.benf.cfr.reader.util.Optional;

public class TryResourcesCollapser
implements StructuredStatementTransformer {
    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    private StructuredTry combineTryResources(StructuredTry stm) {
        Op04StructuredStatement content = stm.getInline();
        StructuredStatement inside = content.getStatement();
        if (inside instanceof Block) {
            Optional<Op04StructuredStatement> maybeJustOneStatement = ((Block)inside).getMaybeJustOneStatement();
            if (!maybeJustOneStatement.isSet()) {
                return stm;
            }
            inside = maybeJustOneStatement.getValue().getStatement();
        }
        if (!(inside instanceof StructuredTry)) {
            return stm;
        }
        StructuredTry inner = (StructuredTry)inside;
        if (!inner.hasResources()) {
            return stm;
        }
        if (inner.getFinallyBlock() != null) {
            return stm;
        }
        if (!inner.getCatchBlocks().isEmpty()) {
            return stm;
        }
        stm.addResources(inner.getResources());
        stm.setTryBlock(inner.getInline());
        return this.combineTryResources(stm);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        if (in instanceof StructuredTry) {
            in = this.combineTryResources((StructuredTry)in);
        }
        in.transformStructuredChildren(this, scope);
        return in;
    }
}

