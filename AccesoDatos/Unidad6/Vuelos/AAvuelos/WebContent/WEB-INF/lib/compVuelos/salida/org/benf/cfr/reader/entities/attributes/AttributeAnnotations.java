/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.attributes.AnnotationHelpers;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.output.Dumper;

public abstract class AttributeAnnotations
extends Attribute
implements TypeUsageCollectable {
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private static final long OFFSET_OF_NUMBER_OF_ANNOTATIONS = 6L;
    private static final long OFFSET_OF_ANNOTATION_TABLE = 8L;
    private final List<AnnotationTableEntry> annotationTableEntryList = ListFactory.newList();
    private final int length;

    AttributeAnnotations(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(2L);
        int numAnnotations = raw.getU2At(6L);
        long offset = 8L;
        for (int x = 0; x < numAnnotations; ++x) {
            Pair<Long, AnnotationTableEntry> ape = AnnotationHelpers.getAnnotation(raw, offset, cp);
            offset = ape.getFirst();
            this.annotationTableEntryList.add(ape.getSecond());
        }
    }

    public void hide(final JavaTypeInstance type) {
        List<AnnotationTableEntry> hideThese = Functional.filter(this.annotationTableEntryList, new Predicate<AnnotationTableEntry>(){

            @Override
            public boolean test(AnnotationTableEntry in) {
                return in.getClazz().equals(type);
            }
        });
        for (AnnotationTableEntry hide : hideThese) {
            hide.setHidden();
        }
    }

    @Override
    public Dumper dump(Dumper d) {
        for (AnnotationTableEntry annotationTableEntry : this.annotationTableEntryList) {
            if (annotationTableEntry.isHidden()) continue;
            annotationTableEntry.dump(d);
            d.newln();
        }
        return d;
    }

    public List<AnnotationTableEntry> getEntryList() {
        return this.annotationTableEntryList;
    }

    @Override
    public long getRawByteLength() {
        return 6L + (long)this.length;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (AnnotationTableEntry annotationTableEntry : this.annotationTableEntryList) {
            annotationTableEntry.collectTypeUsages(collector);
        }
    }
}

