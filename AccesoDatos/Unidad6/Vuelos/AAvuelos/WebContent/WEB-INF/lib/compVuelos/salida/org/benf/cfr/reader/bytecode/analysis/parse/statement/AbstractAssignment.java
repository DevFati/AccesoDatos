/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractStatement;

public abstract class AbstractAssignment
extends AbstractStatement {
    public AbstractAssignment(BytecodeLoc loc) {
        super(loc);
    }

    public abstract boolean isSelfMutatingOperation();

    public abstract boolean isSelfMutatingOp1(LValue var1, ArithOp var2);

    public abstract Expression getPostMutation();

    public abstract Expression getPreMutation();

    public abstract AbstractAssignmentExpression getInliningExpression();
}

