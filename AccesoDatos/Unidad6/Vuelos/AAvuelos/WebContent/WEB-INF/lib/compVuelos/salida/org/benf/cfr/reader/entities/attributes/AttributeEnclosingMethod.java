/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeEnclosingMethod
extends Attribute {
    public static final String ATTRIBUTE_NAME = "EnclosingMethod";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private final int length;
    private final int classIndex;
    private final int methodIndex;

    public AttributeEnclosingMethod(ByteData raw) {
        this.length = raw.getS4At(2L);
        this.classIndex = raw.getU2At(6L);
        this.methodIndex = raw.getU2At(8L);
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print(ATTRIBUTE_NAME);
    }

    @Override
    public long getRawByteLength() {
        return 6L + (long)this.length;
    }

    public String toString() {
        return ATTRIBUTE_NAME;
    }

    public int getClassIndex() {
        return this.classIndex;
    }

    public int getMethodIndex() {
        return this.methodIndex;
    }
}

