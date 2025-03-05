/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.constantpool;

import org.benf.cfr.reader.bytecode.analysis.parse.utils.QuotingUtils;
import org.benf.cfr.reader.entities.AbstractConstantPoolEntry;
import org.benf.cfr.reader.entities.bootstrap.MethodHandleBehaviour;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryFieldRef;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class ConstantPoolEntryMethodHandle
extends AbstractConstantPoolEntry {
    private static final long OFFSET_OF_REFERENCE_KIND = 1L;
    private static final long OFFSET_OF_REFERENCE_INDEX = 2L;
    private final MethodHandleBehaviour referenceKind;
    private final int referenceIndex;

    public ConstantPoolEntryMethodHandle(ConstantPool cp, ByteData data) {
        super(cp);
        this.referenceKind = MethodHandleBehaviour.decode(data.getS1At(1L));
        this.referenceIndex = data.getU2At(2L);
    }

    @Override
    public long getRawByteLength() {
        return 4L;
    }

    @Override
    public void dump(Dumper d) {
        d.print(this.toString());
    }

    public MethodHandleBehaviour getReferenceKind() {
        return this.referenceKind;
    }

    public ConstantPoolEntryMethodRef getMethodRef() {
        return this.getCp().getMethodRefEntry(this.referenceIndex);
    }

    public ConstantPoolEntryFieldRef getFieldRef() {
        return this.getCp().getFieldRefEntry(this.referenceIndex);
    }

    public boolean isFieldRef() {
        switch (this.referenceKind) {
            case GET_FIELD: 
            case GET_STATIC: 
            case PUT_FIELD: 
            case PUT_STATIC: {
                return true;
            }
        }
        return false;
    }

    public String getLiteralName() {
        if (this.isFieldRef()) {
            return QuotingUtils.enquoteString(this.getFieldRef().getLocalName());
        }
        return this.getMethodRef().getMethodPrototype().toString();
    }

    public String toString() {
        return "MethodHandle value=" + (Object)((Object)this.referenceKind) + "," + this.referenceIndex;
    }
}

