/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.util.output.Dumper;

public class LiteralHex
extends Literal {
    public LiteralHex(TypedLiteral value) {
        super(value);
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        return this.value.dumpWithHint(d, TypedLiteral.FormatHint.Hex);
    }
}

