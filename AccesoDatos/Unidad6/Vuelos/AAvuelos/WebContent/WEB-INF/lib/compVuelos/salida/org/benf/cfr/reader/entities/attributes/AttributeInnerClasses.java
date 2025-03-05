/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.entities.innerclass.InnerClassAttributeInfo;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeInnerClasses
extends Attribute {
    public static final String ATTRIBUTE_NAME = "InnerClasses";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private static final long OFFSET_OF_NUMBER_OF_CLASSES = 6L;
    private static final long OFFSET_OF_CLASS_ARRAY = 8L;
    private final int length;
    private final List<InnerClassAttributeInfo> innerClassAttributeInfoList = ListFactory.newList();

    private static JavaTypeInstance getOptClass(int idx, ConstantPool cp) {
        if (idx == 0) {
            return null;
        }
        return cp.getClassEntry(idx).getTypeInstance();
    }

    private static String getOptName(int idx, ConstantPool cp) {
        if (idx == 0) {
            return null;
        }
        return cp.getUTF8Entry(idx).getValue();
    }

    private static Pair<JavaTypeInstance, JavaTypeInstance> getInnerOuter(int idxinner, int idxouter, ConstantPool cp) {
        if (idxinner == 0 || idxouter == 0) {
            return Pair.make(AttributeInnerClasses.getOptClass(idxinner, cp), AttributeInnerClasses.getOptClass(idxouter, cp));
        }
        ConstantPoolEntryClass cpecInner = cp.getClassEntry(idxinner);
        ConstantPoolEntryClass cpecOuter = cp.getClassEntry(idxouter);
        JavaTypeInstance innerType = cpecInner.getTypeInstanceKnownOuter(cpecOuter);
        JavaTypeInstance outerType = cpecOuter.getTypeInstanceKnownInner(cpecInner);
        return Pair.make(innerType, outerType);
    }

    public AttributeInnerClasses(ByteData raw, ConstantPool cp) {
        Boolean forbidMethodScopedClasses = (Boolean)cp.getDCCommonState().getOptions().getOption(OptionsImpl.FORBID_METHOD_SCOPED_CLASSES);
        Boolean forbidAnonymousClasses = (Boolean)cp.getDCCommonState().getOptions().getOption(OptionsImpl.FORBID_ANONYMOUS_CLASSES);
        this.length = raw.getS4At(2L);
        int numberInnerClasses = raw.getU2At(6L);
        long offset = 8L;
        for (int x = 0; x < numberInnerClasses; ++x) {
            int innerClassInfoIdx = raw.getU2At(offset);
            int outerClassInfoIdx = raw.getU2At(offset += 2L);
            int innerNameIdx = raw.getU2At(offset += 2L);
            int innerAccessFlags = raw.getU2At(offset += 2L);
            offset += 2L;
            Pair<JavaTypeInstance, JavaTypeInstance> innerOuter = AttributeInnerClasses.getInnerOuter(innerClassInfoIdx, outerClassInfoIdx, cp);
            JavaTypeInstance innerClassType = innerOuter.getFirst();
            JavaTypeInstance outerClassType = innerOuter.getSecond();
            if (outerClassType == null) {
                if (forbidMethodScopedClasses.booleanValue()) {
                    outerClassType = innerClassType.getInnerClassHereInfo().getOuterClass();
                } else {
                    boolean isAnonymous = forbidAnonymousClasses == false && innerNameIdx == 0;
                    innerClassType.getInnerClassHereInfo().markMethodScoped(isAnonymous);
                }
            }
            this.innerClassAttributeInfoList.add(new InnerClassAttributeInfo(innerClassType, outerClassType, AttributeInnerClasses.getOptName(innerNameIdx, cp), AccessFlag.build(innerAccessFlags)));
        }
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

    public List<InnerClassAttributeInfo> getInnerClassAttributeInfoList() {
        return this.innerClassAttributeInfoList;
    }

    public String toString() {
        return ATTRIBUTE_NAME;
    }
}

