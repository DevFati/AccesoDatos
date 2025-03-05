/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.annotations;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.annotations.ElementValue;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class ElementValueAnnotation
implements ElementValue {
    private final AnnotationTableEntry annotationTableEntry;

    public ElementValueAnnotation(AnnotationTableEntry annotationTableEntry) {
        this.annotationTableEntry = annotationTableEntry;
    }

    @Override
    public Dumper dump(Dumper d) {
        return this.annotationTableEntry.dump(d);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.annotationTableEntry.collectTypeUsages(collector);
    }

    @Override
    public ElementValue withTypeHint(JavaTypeInstance hint) {
        return this;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ElementValueAnnotation) {
            ElementValueAnnotation other = (ElementValueAnnotation)obj;
            return this.annotationTableEntry.equals(other.annotationTableEntry);
        }
        return false;
    }
}

