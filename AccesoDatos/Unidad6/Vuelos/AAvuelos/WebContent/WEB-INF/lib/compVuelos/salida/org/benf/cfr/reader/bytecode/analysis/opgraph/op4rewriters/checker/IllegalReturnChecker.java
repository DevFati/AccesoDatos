/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.Op04Checker;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;

public class IllegalReturnChecker
implements Op04Checker {
    private boolean found = false;

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        if (this.found) {
            return in;
        }
        if (in instanceof Block) {
            List<Op04StructuredStatement> stms = ((Block)in).getBlockStatements();
            StructuredStatement last = null;
            int len = stms.size();
            for (int x = 0; x < len; ++x) {
                Op04StructuredStatement statement = stms.get(x);
                StructuredStatement stm = statement.getStatement();
                if (stm instanceof StructuredReturn) {
                    if (last == null) {
                        last = stm;
                        continue;
                    }
                    if (last.equals(stm)) {
                        statement.nopOut();
                        continue;
                    }
                    this.found = true;
                    return in;
                }
                if (stm instanceof StructuredComment) continue;
                last = null;
            }
        }
        in.transformStructuredChildren(this, scope);
        return in;
    }

    @Override
    public void commentInto(DecompilerComments comments) {
        if (this.found) {
            comments.addComment(DecompilerComment.NEIGHBOUR_RETURN);
        }
    }
}

