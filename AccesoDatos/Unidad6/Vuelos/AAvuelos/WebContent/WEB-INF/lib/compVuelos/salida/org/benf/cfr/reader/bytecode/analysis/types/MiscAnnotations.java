/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.attributes.AttributeMap;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeInvisibleAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleAnnotations;
import org.benf.cfr.reader.util.collections.ListFactory;

public class MiscAnnotations {
    public static List<AnnotationTableEntry> BasicAnnotations(AttributeMap attributeMap) {
        AttributeRuntimeVisibleAnnotations a1 = (AttributeRuntimeVisibleAnnotations)attributeMap.getByName("RuntimeVisibleAnnotations");
        AttributeRuntimeInvisibleAnnotations a2 = (AttributeRuntimeInvisibleAnnotations)attributeMap.getByName("RuntimeInvisibleAnnotations");
        List<AnnotationTableEntry> e1 = a1 == null ? null : a1.getEntryList();
        List<AnnotationTableEntry> e2 = a2 == null ? null : a2.getEntryList();
        return ListFactory.combinedOptimistic(e1, e2);
    }
}

