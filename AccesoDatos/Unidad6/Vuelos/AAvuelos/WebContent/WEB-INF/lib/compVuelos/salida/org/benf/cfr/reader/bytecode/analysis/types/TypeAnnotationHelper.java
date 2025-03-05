/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.attributes.AttributeMap;
import org.benf.cfr.reader.entities.attributes.AttributeTypeAnnotations;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue;
import org.benf.cfr.reader.entities.attributes.TypePathPart;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.ListFactory;

public class TypeAnnotationHelper {
    private final List<AnnotationTableTypeEntry> entries;

    private TypeAnnotationHelper(List<AnnotationTableTypeEntry> entries) {
        this.entries = entries;
    }

    public static TypeAnnotationHelper create(AttributeMap map, TypeAnnotationEntryValue ... tkeys) {
        String[] keys = new String[]{"RuntimeVisibleTypeAnnotations", "RuntimeInvisibleTypeAnnotations"};
        List<AnnotationTableTypeEntry> res = ListFactory.newList();
        for (String key : keys) {
            List<AnnotationTableTypeEntry> tmp;
            AttributeTypeAnnotations ann = (AttributeTypeAnnotations)map.getByName(key);
            if (ann == null || (tmp = ann.getAnnotationsFor(tkeys)) == null) continue;
            res.addAll(tmp);
        }
        if (!res.isEmpty()) {
            return new TypeAnnotationHelper(res);
        }
        return null;
    }

    public static void apply(JavaAnnotatedTypeInstance annotatedTypeInstance, List<? extends AnnotationTableTypeEntry> typeEntries, DecompilerComments comments) {
        if (typeEntries != null) {
            for (AnnotationTableTypeEntry annotationTableTypeEntry : typeEntries) {
                TypeAnnotationHelper.apply(annotatedTypeInstance, annotationTableTypeEntry, comments);
            }
        }
    }

    private static void apply(JavaAnnotatedTypeInstance annotatedTypeInstance, AnnotationTableTypeEntry typeEntry, DecompilerComments comments) {
        JavaAnnotatedTypeIterator iterator = annotatedTypeInstance.pathIterator();
        List<TypePathPart> segments = typeEntry.getTypePath().segments;
        for (TypePathPart part : segments) {
            iterator = part.apply(iterator, comments);
        }
        iterator.apply(typeEntry);
    }

    public List<AnnotationTableTypeEntry> getEntries() {
        return this.entries;
    }
}

