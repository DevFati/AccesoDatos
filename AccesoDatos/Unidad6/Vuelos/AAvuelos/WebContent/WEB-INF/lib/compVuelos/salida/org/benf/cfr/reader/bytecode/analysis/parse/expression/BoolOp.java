/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.util.ConfusedCFRException;

public enum BoolOp {
    OR("||", Precedence.LOG_OR),
    AND("&&", Precedence.LOG_AND);

    private final String showAs;
    private final Precedence precedence;

    private BoolOp(String showAs, Precedence precedence) {
        this.showAs = showAs;
        this.precedence = precedence;
    }

    public String getShowAs() {
        return this.showAs;
    }

    public Precedence getPrecedence() {
        return this.precedence;
    }

    public BoolOp getDemorgan() {
        switch (this) {
            case OR: {
                return AND;
            }
            case AND: {
                return OR;
            }
        }
        throw new ConfusedCFRException("Unknown op.");
    }
}

