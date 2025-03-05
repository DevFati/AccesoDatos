/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeSignature
extends Attribute {
    public static final String ATTRIBUTE_NAME = "Signature";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private final int length;
    private final ConstantPoolEntryUTF8 signature;

    public AttributeSignature(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(2L);
        this.signature = cp.getUTF8Entry(raw.getU2At(6L));
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print("Signature : " + this.signature);
    }

    @Override
    public long getRawByteLength() {
        return 6L + (long)this.length;
    }

    public ConstantPoolEntryUTF8 getSignature() {
        return this.signature;
    }
}

