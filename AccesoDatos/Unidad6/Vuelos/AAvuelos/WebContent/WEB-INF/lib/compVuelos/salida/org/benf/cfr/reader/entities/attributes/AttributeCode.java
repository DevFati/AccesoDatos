/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.ArrayList;
import java.util.List;
import org.benf.cfr.reader.bytecode.CodeAnalyser;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.attributes.AttributeLineNumberTable;
import org.benf.cfr.reader.entities.attributes.AttributeLocalVariableTable;
import org.benf.cfr.reader.entities.attributes.AttributeMap;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeInvisibleTypeAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleTypeAnnotations;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.exceptions.ExceptionTableEntry;
import org.benf.cfr.reader.entityfactories.AttributeFactory;
import org.benf.cfr.reader.entityfactories.ContiguousEntityFactory;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeCode
extends Attribute {
    public static final String ATTRIBUTE_NAME = "Code";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_MAX_STACK = 6L;
    private final int length;
    private final int maxStack;
    private final int maxLocals;
    private final int codeLength;
    private final List<ExceptionTableEntry> exceptionTableEntries;
    private final AttributeMap attributes;
    private final ConstantPool cp;
    private final ByteData rawData;
    private final CodeAnalyser codeAnalyser;

    public AttributeCode(ByteData raw, ConstantPool cp, ClassFileVersion classFileVersion) {
        int codeLength;
        int maxLocals;
        int maxStack;
        this.cp = cp;
        this.length = raw.getS4At(2L);
        long OFFSET_OF_MAX_LOCALS = 8L;
        long OFFSET_OF_CODE_LENGTH = 10L;
        long OFFSET_OF_CODE = 14L;
        if (classFileVersion.before(ClassFileVersion.JAVA_1_0)) {
            OFFSET_OF_MAX_LOCALS = 7L;
            OFFSET_OF_CODE_LENGTH = 8L;
            OFFSET_OF_CODE = 10L;
            maxStack = raw.getU1At(6L);
            maxLocals = raw.getU1At(OFFSET_OF_MAX_LOCALS);
            codeLength = raw.getU2At(OFFSET_OF_CODE_LENGTH);
        } else {
            maxStack = raw.getU2At(6L);
            maxLocals = raw.getU2At(OFFSET_OF_MAX_LOCALS);
            codeLength = raw.getS4At(OFFSET_OF_CODE_LENGTH);
        }
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
        this.codeLength = codeLength;
        long OFFSET_OF_EXCEPTION_TABLE_LENGTH = OFFSET_OF_CODE + (long)codeLength;
        long OFFSET_OF_EXCEPTION_TABLE = OFFSET_OF_EXCEPTION_TABLE_LENGTH + 2L;
        ArrayList<ExceptionTableEntry> etis = new ArrayList<ExceptionTableEntry>();
        int numExceptions = raw.getU2At(OFFSET_OF_EXCEPTION_TABLE_LENGTH);
        etis.ensureCapacity(numExceptions);
        long numBytesExceptionInfo = ContiguousEntityFactory.buildSized(raw.getOffsetData(OFFSET_OF_EXCEPTION_TABLE), numExceptions, 8, etis, ExceptionTableEntry.getBuilder());
        this.exceptionTableEntries = etis;
        long OFFSET_OF_ATTRIBUTES_COUNT = OFFSET_OF_EXCEPTION_TABLE + numBytesExceptionInfo;
        long OFFSET_OF_ATTRIBUTES = OFFSET_OF_ATTRIBUTES_COUNT + 2L;
        int numAttributes = raw.getU2At(OFFSET_OF_ATTRIBUTES_COUNT);
        ArrayList<Attribute> tmpAttributes = new ArrayList<Attribute>();
        tmpAttributes.ensureCapacity(numAttributes);
        ContiguousEntityFactory.build(raw.getOffsetData(OFFSET_OF_ATTRIBUTES), numAttributes, tmpAttributes, AttributeFactory.getBuilder(cp, classFileVersion));
        this.attributes = new AttributeMap(tmpAttributes);
        this.rawData = raw.getOffsetData(OFFSET_OF_CODE);
        this.codeAnalyser = new CodeAnalyser(this);
    }

    public void setMethod(Method method) {
        this.codeAnalyser.setMethod(method);
    }

    public Op04StructuredStatement analyse() {
        return this.codeAnalyser.getAnalysis(this.getConstantPool().getDCCommonState());
    }

    public ConstantPool getConstantPool() {
        return this.cp;
    }

    public AttributeLocalVariableTable getLocalVariableTable() {
        return (AttributeLocalVariableTable)this.attributes.getByName("LocalVariableTable");
    }

    public AttributeLineNumberTable getLineNumberTable() {
        return (AttributeLineNumberTable)this.attributes.getByName("LineNumberTable");
    }

    public AttributeRuntimeVisibleTypeAnnotations getRuntimeVisibleTypeAnnotations() {
        return (AttributeRuntimeVisibleTypeAnnotations)this.attributes.getByName("RuntimeVisibleTypeAnnotations");
    }

    public AttributeRuntimeInvisibleTypeAnnotations getRuntimeInvisibleTypeAnnotations() {
        return (AttributeRuntimeInvisibleTypeAnnotations)this.attributes.getByName("RuntimeInvisibleTypeAnnotations");
    }

    public ByteData getRawData() {
        return this.rawData;
    }

    public List<ExceptionTableEntry> getExceptionTableEntries() {
        return this.exceptionTableEntries;
    }

    public int getMaxLocals() {
        return this.maxLocals;
    }

    public int getCodeLength() {
        return this.codeLength;
    }

    @Override
    public Dumper dump(Dumper d) {
        return this.codeAnalyser.getAnalysis(this.getConstantPool().getDCCommonState()).dump(d);
    }

    @Override
    public long getRawByteLength() {
        return 6L + (long)this.length;
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.attributes.collectTypeUsages(collector);
    }

    public void releaseCode() {
        this.codeAnalyser.releaseCode();
    }

    public AttributeMap getAttributes() {
        return this.attributes;
    }
}

