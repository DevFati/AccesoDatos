/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.annotations;

import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.entities.annotations.ElementValue;
import org.benf.cfr.reader.entities.annotations.ElementValueArray;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class ElementValueConst
implements ElementValue {
    private final TypedLiteral value;

    public ElementValueConst(TypedLiteral value) {
        this.value = value;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.dump(this.value);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    @Override
    public ElementValue withTypeHint(JavaTypeInstance hint) {
        if (hint == RawJavaType.BOOLEAN) {
            return new ElementValueConst(TypedLiteral.shrinkTo(this.value, RawJavaType.BOOLEAN));
        }
        return this;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ElementValueArray) {
            ElementValueConst other = (ElementValueConst)obj;
            return this.value.equals(other.value);
        }
        return false;
    }
}

