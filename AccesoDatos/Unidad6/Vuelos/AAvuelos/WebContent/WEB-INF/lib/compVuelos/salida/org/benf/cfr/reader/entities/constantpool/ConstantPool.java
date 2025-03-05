/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.constantpool;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.benf.cfr.reader.entities.AbstractConstantPoolEntry;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryDouble;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryDynamicInfo;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryFieldRef;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryFloat;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryInteger;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryInvokeDynamic;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryLong;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodHandle;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodType;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryModuleInfo;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryNameAndType;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryPackageInfo;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryString;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.state.ClassCache;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.output.LoggerFactory;

public class ConstantPool {
    private static final Logger logger = LoggerFactory.create(ConstantPool.class);
    private final long length;
    private final List<ConstantPoolEntry> entries;
    private final Options options;
    private final DCCommonState dcCommonState;
    private final ClassCache classCache;
    private final ClassFile classFile;
    private String comparisonKey;
    private boolean isLoaded;
    private final int idx = sidx.getAndIncrement();
    private static final AtomicInteger sidx = new AtomicInteger();
    private final boolean dynamicConstants;

    public ConstantPool(ClassFile classFile, DCCommonState dcCommonState, ByteData raw, int count) {
        this.classFile = classFile;
        this.options = dcCommonState.getOptions();
        RawTmp tmp = this.processRaw(raw, --count);
        this.entries = tmp.entries;
        this.length = tmp.rawLength;
        this.dynamicConstants = tmp.dynamicConstants;
        this.dcCommonState = dcCommonState;
        this.classCache = dcCommonState.getClassCache();
        this.isLoaded = true;
    }

    public DCCommonState getDCCommonState() {
        return this.dcCommonState;
    }

    public boolean isLoaded() {
        return this.isLoaded;
    }

    public boolean isDynamicConstants() {
        return this.dynamicConstants;
    }

    private RawTmp processRaw(ByteData raw, int count) {
        List<ConstantPoolEntry> tgt = ListFactory.newList(count);
        OffsettingByteData data = raw.getOffsettingOffsetData(0L);
        boolean dynamicConstant = false;
        logger.info("Processing " + count + " constpool entries.");
        for (int x = 0; x < count; ++x) {
            AbstractConstantPoolEntry cpe;
            ConstantPoolEntry.Type type = ConstantPoolEntry.Type.get(data.getS1At(0L));
            switch (type) {
                case CPT_NameAndType: {
                    cpe = new ConstantPoolEntryNameAndType(this, data);
                    break;
                }
                case CPT_String: {
                    cpe = new ConstantPoolEntryString(this, data);
                    break;
                }
                case CPT_FieldRef: {
                    cpe = new ConstantPoolEntryFieldRef(this, data);
                    break;
                }
                case CPT_MethodRef: {
                    cpe = new ConstantPoolEntryMethodRef(this, data, false);
                    break;
                }
                case CPT_InterfaceMethodRef: {
                    cpe = new ConstantPoolEntryMethodRef(this, data, true);
                    break;
                }
                case CPT_Class: {
                    cpe = new ConstantPoolEntryClass(this, data);
                    break;
                }
                case CPT_Double: {
                    cpe = new ConstantPoolEntryDouble(this, data);
                    break;
                }
                case CPT_Float: {
                    cpe = new ConstantPoolEntryFloat(this, data);
                    break;
                }
                case CPT_Long: {
                    cpe = new ConstantPoolEntryLong(this, data);
                    break;
                }
                case CPT_Integer: {
                    cpe = new ConstantPoolEntryInteger(this, data);
                    break;
                }
                case CPT_UTF8: {
                    cpe = new ConstantPoolEntryUTF8(this, data, this.options);
                    break;
                }
                case CPT_MethodHandle: {
                    cpe = new ConstantPoolEntryMethodHandle(this, data);
                    break;
                }
                case CPT_MethodType: {
                    cpe = new ConstantPoolEntryMethodType(this, data);
                    break;
                }
                case CPT_DynamicInfo: {
                    cpe = new ConstantPoolEntryDynamicInfo(this, data);
                    dynamicConstant = true;
                    break;
                }
                case CPT_InvokeDynamic: {
                    cpe = new ConstantPoolEntryInvokeDynamic(this, data);
                    break;
                }
                case CPT_ModuleInfo: {
                    cpe = new ConstantPoolEntryModuleInfo(this, data);
                    break;
                }
                case CPT_PackageInfo: {
                    cpe = new ConstantPoolEntryPackageInfo(this, data);
                    break;
                }
                default: {
                    throw new ConfusedCFRException("Invalid constant pool entry : " + (Object)((Object)type));
                }
            }
            logger.info("" + (x + 1) + " : " + cpe);
            tgt.add(cpe);
            switch (type) {
                case CPT_Double: 
                case CPT_Long: {
                    tgt.add(null);
                    ++x;
                }
            }
            long size = cpe.getRawByteLength();
            data.advance(size);
        }
        return new RawTmp(tgt, data.getOffset(), dynamicConstant);
    }

    public long getRawByteLength() {
        return this.length;
    }

    public ConstantPoolEntry getEntry(int index) {
        if (index == 0) {
            throw new ConfusedCFRException("Attempt to fetch element 0 from constant pool");
        }
        if (index > this.entries.size()) {
            throw new IndexOutOfBoundsException("Constant pool has " + this.entries.size() + " entries - attempted to access entry #" + (index - 1));
        }
        return this.entries.get(index - 1);
    }

    public ConstantPoolEntryUTF8 getUTF8Entry(int index) {
        return (ConstantPoolEntryUTF8)this.getEntry(index);
    }

    public ConstantPoolEntryNameAndType getNameAndTypeEntry(int index) {
        return (ConstantPoolEntryNameAndType)this.getEntry(index);
    }

    public ConstantPoolEntryMethodHandle getMethodHandleEntry(int index) {
        return (ConstantPoolEntryMethodHandle)this.getEntry(index);
    }

    ConstantPoolEntryMethodRef getMethodRefEntry(int index) {
        ConstantPoolEntry entry = this.getEntry(index);
        return (ConstantPoolEntryMethodRef)entry;
    }

    ConstantPoolEntryFieldRef getFieldRefEntry(int index) {
        ConstantPoolEntry entry = this.getEntry(index);
        return (ConstantPoolEntryFieldRef)entry;
    }

    public ConstantPoolEntryClass getClassEntry(int index) {
        return (ConstantPoolEntryClass)this.getEntry(index);
    }

    public ConstantPoolEntryModuleInfo getModuleEntry(int index) {
        return (ConstantPoolEntryModuleInfo)this.getEntry(index);
    }

    public ConstantPoolEntryPackageInfo getPackageEntry(int index) {
        return (ConstantPoolEntryPackageInfo)this.getEntry(index);
    }

    public ClassCache getClassCache() {
        return this.classCache;
    }

    public boolean equals(Object o) {
        this.getComparisonKey();
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ConstantPool that = (ConstantPool)o;
        return this.comparisonKey.equals(that.comparisonKey);
    }

    public String toString() {
        return this.getComparisonKey() + "[" + this.idx + "]";
    }

    public int hashCode() {
        return this.getComparisonKey().hashCode();
    }

    private String getComparisonKey() {
        if (this.comparisonKey == null) {
            this.comparisonKey = this.classFile.getFilePath();
        }
        return this.comparisonKey;
    }

    private static class RawTmp {
        final List<ConstantPoolEntry> entries;
        final long rawLength;
        final boolean dynamicConstants;

        RawTmp(List<ConstantPoolEntry> entries, long rawLength, boolean dynamicConstants) {
            this.entries = entries;
            this.rawLength = rawLength;
            this.dynamicConstants = dynamicConstants;
        }
    }
}

