/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeExceptions
extends Attribute {
    public static final String ATTRIBUTE_NAME = "Exceptions";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_NUMBER_OF_EXCEPTIONS = 6L;
    private static final long OFFSET_OF_EXCEPTION_TABLE = 8L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private final List<ConstantPoolEntryClass> exceptionClassList = ListFactory.newList();
    private final int length;

    public AttributeExceptions(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(2L);
        int numExceptions = raw.getU2At(6L);
        long offset = 8L;
        int x = 0;
        while (x < numExceptions) {
            this.exceptionClassList.add(cp.getClassEntry(raw.getU2At(offset)));
            ++x;
            offset += 2L;
        }
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (ConstantPoolEntryClass exceptionClass : this.exceptionClassList) {
            collector.collect(exceptionClass.getTypeInstance());
        }
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d;
    }

    public List<ConstantPoolEntryClass> getExceptionClassList() {
        return this.exceptionClassList;
    }

    @Override
    public long getRawByteLength() {
        return 6L + (long)this.length;
    }
}

