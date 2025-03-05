/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.attributes.AnnotationHelpers;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.output.Dumper;

public abstract class AttributeTypeAnnotations
extends Attribute {
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private static final long OFFSET_OF_NUMBER_OF_ANNOTATIONS = 6L;
    private static final long OFFSET_OF_ANNOTATION_TABLE = 8L;
    private Map<TypeAnnotationEntryValue, List<AnnotationTableTypeEntry>> annotationTableEntryData = MapFactory.newMap();
    private final int length;

    AttributeTypeAnnotations(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(2L);
        int numAnnotations = raw.getU2At(6L);
        long offset = 8L;
        Map<TypeAnnotationEntryValue, List<AnnotationTableTypeEntry>> entryData = MapFactory.newLazyMap(this.annotationTableEntryData, new UnaryFunction<TypeAnnotationEntryValue, List<AnnotationTableTypeEntry>>(){

            @Override
            public List<AnnotationTableTypeEntry> invoke(TypeAnnotationEntryValue arg) {
                return ListFactory.newList();
            }
        });
        for (int x = 0; x < numAnnotations; ++x) {
            Pair<Long, AnnotationTableTypeEntry> ape = AnnotationHelpers.getTypeAnnotation(raw, offset, cp);
            offset = ape.getFirst();
            AnnotationTableTypeEntry entry = ape.getSecond();
            entryData.get((Object)entry.getValue()).add(entry);
        }
    }

    @Override
    public Dumper dump(Dumper d) {
        for (List<AnnotationTableTypeEntry> annotationTableEntryList : this.annotationTableEntryData.values()) {
            for (AnnotationTableTypeEntry annotationTableEntry : annotationTableEntryList) {
                annotationTableEntry.dump(d);
                d.newln();
            }
        }
        return d;
    }

    @Override
    public long getRawByteLength() {
        return 6L + (long)this.length;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (List<AnnotationTableTypeEntry> annotationTableEntryList : this.annotationTableEntryData.values()) {
            for (AnnotationTableTypeEntry annotationTableEntry : annotationTableEntryList) {
                annotationTableEntry.collectTypeUsages(collector);
            }
        }
    }

    public List<AnnotationTableTypeEntry> getAnnotationsFor(TypeAnnotationEntryValue ... types) {
        List<AnnotationTableTypeEntry> res = null;
        boolean orig = true;
        for (TypeAnnotationEntryValue type : types) {
            List<AnnotationTableTypeEntry> items = this.annotationTableEntryData.get((Object)type);
            if (items == null) continue;
            if (orig) {
                res = items;
                orig = false;
                continue;
            }
            res = ListFactory.newList(res);
            res.addAll(items);
        }
        return res;
    }
}

