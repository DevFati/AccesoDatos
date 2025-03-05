/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.attributes.AnnotationHelpers;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public abstract class AttributeParameterAnnotations
extends Attribute
implements TypeUsageCollectable {
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private static final long OFFSET_OF_NUMBER_OF_PARAMETERS = 6L;
    private static final long OFFSET_OF_ANNOTATION_NAME_TABLE = 7L;
    private final List<List<AnnotationTableEntry>> annotationTableEntryListList;
    private final int length;

    public AttributeParameterAnnotations(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(2L);
        int numParameters = raw.getS1At(6L);
        long offset = 7L;
        this.annotationTableEntryListList = ListFactory.newList();
        for (int x = 0; x < numParameters; ++x) {
            List annotationTableEntryList = ListFactory.newList();
            int numAnnotations = raw.getU2At(offset);
            offset += 2L;
            for (int y = 0; y < numAnnotations; ++y) {
                Pair<Long, AnnotationTableEntry> ape = AnnotationHelpers.getAnnotation(raw, offset, cp);
                offset = ape.getFirst();
                annotationTableEntryList.add(ape.getSecond());
            }
            this.annotationTableEntryListList.add(annotationTableEntryList);
        }
    }

    public List<AnnotationTableEntry> getAnnotationsForParamIdx(int idx) {
        if (idx < 0 || idx >= this.annotationTableEntryListList.size()) {
            return null;
        }
        return this.annotationTableEntryListList.get(idx);
    }

    @Override
    public Dumper dump(Dumper d) {
        return d;
    }

    @Override
    public long getRawByteLength() {
        return 6L + (long)this.length;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (List<AnnotationTableEntry> annotationTableEntryList : this.annotationTableEntryListList) {
            for (AnnotationTableEntry annotationTableEntry : annotationTableEntryList) {
                annotationTableEntry.collectTypeUsages(collector);
            }
        }
    }
}

