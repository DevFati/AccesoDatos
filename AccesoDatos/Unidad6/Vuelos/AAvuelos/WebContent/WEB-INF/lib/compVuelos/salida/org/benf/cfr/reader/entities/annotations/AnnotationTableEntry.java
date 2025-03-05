/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.annotations;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.annotations.ElementValue;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.output.Dumper;

public class AnnotationTableEntry
implements TypeUsageCollectable {
    private final JavaTypeInstance clazz;
    private final Map<String, ElementValue> elementValueMap;
    private boolean hidden;

    public AnnotationTableEntry(JavaTypeInstance clazz, Map<String, ElementValue> elementValueMap) {
        this.clazz = clazz;
        this.elementValueMap = elementValueMap;
    }

    public void setHidden() {
        this.hidden = true;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public JavaTypeInstance getClazz() {
        return this.clazz;
    }

    public Dumper dump(Dumper d) {
        d.print('@').dump(this.clazz);
        if (this.elementValueMap != null && !this.elementValueMap.isEmpty()) {
            d.print('(');
            boolean first = true;
            for (Map.Entry<String, ElementValue> elementValueEntry : this.elementValueMap.entrySet()) {
                first = StringUtils.comma(first, d);
                d.print(elementValueEntry.getKey()).print('=');
                elementValueEntry.getValue().dump(d);
            }
            d.print(')');
        }
        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.clazz);
        if (this.elementValueMap != null) {
            for (ElementValue elementValue : this.elementValueMap.values()) {
                elementValue.collectTypeUsages(collector);
            }
        }
    }

    public boolean isAnnotationEqual(AnnotationTableEntry other) {
        return this.clazz.equals(other.getClazz()) && this.elementValueMap.equals(other.elementValueMap);
    }
}

