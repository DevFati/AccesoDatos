/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker;

import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.Op04Checker;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;

public class VoidVariableChecker
implements Op04Checker {
    private boolean found = false;

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        InferredJavaType inferredJavaType;
        if (this.found) {
            return in;
        }
        if (in instanceof StructuredDefinition && (inferredJavaType = ((StructuredDefinition)in).getLvalue().getInferredJavaType()) != null && inferredJavaType.getJavaTypeInstance().getRawTypeOfSimpleType() == RawJavaType.VOID) {
            this.found = true;
            return in;
        }
        in.transformStructuredChildren(this, scope);
        return in;
    }

    @Override
    public void commentInto(DecompilerComments comments) {
        if (this.found) {
            comments.addComment(DecompilerComment.VOID_DECLARATION);
        }
    }
}

