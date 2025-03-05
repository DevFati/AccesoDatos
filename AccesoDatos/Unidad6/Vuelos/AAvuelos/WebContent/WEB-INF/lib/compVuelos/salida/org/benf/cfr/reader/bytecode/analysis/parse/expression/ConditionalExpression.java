/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;

public interface ConditionalExpression
extends Expression {
    public ConditionalExpression getNegated();

    public int getSize(Precedence var1);

    public ConditionalExpression getDemorganApplied(boolean var1);

    public ConditionalExpression getRightDeep();

    public Set<LValue> getLoopLValues();

    public ConditionalExpression optimiseForType();

    public ConditionalExpression simplify();
}

