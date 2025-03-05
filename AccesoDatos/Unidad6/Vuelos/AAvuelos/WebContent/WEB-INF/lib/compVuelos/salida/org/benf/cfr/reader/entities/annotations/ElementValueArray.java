/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.annotations;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.annotations.ElementValue;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.output.Dumper;

public class ElementValueArray
implements ElementValue {
    private final List<ElementValue> content;

    public ElementValueArray(List<ElementValue> content) {
        this.content = content;
    }

    @Override
    public Dumper dump(Dumper d) {
        d.print('{');
        boolean first = true;
        for (ElementValue value : this.content) {
            first = StringUtils.comma(first, d);
            value.dump(d);
        }
        d.print('}');
        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (ElementValue e : this.content) {
            e.collectTypeUsages(collector);
        }
    }

    @Override
    public ElementValue withTypeHint(JavaTypeInstance hint) {
        return this;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ElementValueArray) {
            ElementValueArray other = (ElementValueArray)obj;
            return this.content.equals(other.content);
        }
        return false;
    }
}

