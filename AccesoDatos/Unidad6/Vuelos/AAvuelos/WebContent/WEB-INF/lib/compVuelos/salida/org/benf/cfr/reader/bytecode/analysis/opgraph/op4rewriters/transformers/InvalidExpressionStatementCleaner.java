/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;

public class InvalidExpressionStatementCleaner
extends AbstractExpressionRewriter
implements StructuredStatementTransformer {
    private VariableFactory variableFactory;

    public InvalidExpressionStatementCleaner(VariableFactory variableNamer) {
        this.variableFactory = variableNamer;
    }

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        Expression exp;
        in.transformStructuredChildren(this, scope);
        if (in instanceof StructuredExpressionStatement && !(exp = ((StructuredExpressionStatement)in).getExpression()).isValidStatement()) {
            return new StructuredAssignment(BytecodeLoc.TODO, this.variableFactory.ignoredVariable(exp.getInferredJavaType()), exp, true);
        }
        return in;
    }
}

