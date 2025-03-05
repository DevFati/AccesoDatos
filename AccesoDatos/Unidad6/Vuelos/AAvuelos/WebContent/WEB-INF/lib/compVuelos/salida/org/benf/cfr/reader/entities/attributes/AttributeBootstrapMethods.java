/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.bootstrap.BootstrapMethodInfo;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodHandle;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeBootstrapMethods
extends Attribute {
    public static final String ATTRIBUTE_NAME = "BootstrapMethods";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private static final long OFFSET_OF_NUM_METHODS = 6L;
    private final int length;
    private final List<BootstrapMethodInfo> methodInfoList;

    public AttributeBootstrapMethods(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(2L);
        this.methodInfoList = AttributeBootstrapMethods.decodeMethods(raw, cp);
    }

    public BootstrapMethodInfo getBootStrapMethodInfo(int idx) {
        if (idx < 0 || idx >= this.methodInfoList.size()) {
            throw new IllegalArgumentException("Invalid bootstrap index.");
        }
        return this.methodInfoList.get(idx);
    }

    private static List<BootstrapMethodInfo> decodeMethods(ByteData raw, ConstantPool cp) {
        List<BootstrapMethodInfo> res = ListFactory.newList();
        int numMethods = raw.getU2At(6L);
        long offset = 8L;
        for (int x = 0; x < numMethods; ++x) {
            int methodRef = raw.getU2At(offset);
            ConstantPoolEntryMethodHandle methodHandle = cp.getMethodHandleEntry(methodRef);
            int numBootstrapArguments = raw.getU2At(offset += 2L);
            offset += 2L;
            ConstantPoolEntry[] bootstrapArguments = new ConstantPoolEntry[numBootstrapArguments];
            for (int y = 0; y < numBootstrapArguments; ++y) {
                bootstrapArguments[y] = cp.getEntry(raw.getU2At(offset));
                offset += 2L;
            }
            res.add(new BootstrapMethodInfo(methodHandle, bootstrapArguments, cp));
        }
        return res;
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
}

