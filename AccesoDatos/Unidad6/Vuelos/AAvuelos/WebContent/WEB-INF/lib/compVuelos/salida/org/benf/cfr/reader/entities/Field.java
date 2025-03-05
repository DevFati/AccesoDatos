/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.bytecode.analysis.types.DeclarationAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MiscAnnotations;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.attributes.AttributeConstantValue;
import org.benf.cfr.reader.entities.attributes.AttributeMap;
import org.benf.cfr.reader.entities.attributes.AttributeSignature;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue;
import org.benf.cfr.reader.entities.classfilehelpers.VisibilityHelper;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolUtils;
import org.benf.cfr.reader.entityfactories.AttributeFactory;
import org.benf.cfr.reader.entityfactories.ContiguousEntityFactory;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.KnowsRawSize;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.CollectionUtils;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;

public class Field
implements KnowsRawSize,
TypeUsageCollectable {
    private static final long OFFSET_OF_ACCESS_FLAGS = 0L;
    private static final long OFFSET_OF_NAME_INDEX = 2L;
    private static final long OFFSET_OF_DESCRIPTOR_INDEX = 4L;
    private static final long OFFSET_OF_ATTRIBUTES_COUNT = 6L;
    private static final long OFFSET_OF_ATTRIBUTES = 8L;
    private final ConstantPool cp;
    private final long length;
    private final int descriptorIndex;
    private final Set<AccessFlag> accessFlags;
    private final AttributeMap attributes;
    private final TypedLiteral constantValue;
    private final String fieldName;
    private boolean disambiguate;
    private transient JavaTypeInstance cachedDecodedType;

    public Field(ByteData raw, ConstantPool cp, ClassFileVersion classFileVersion) {
        JavaTypeInstance thisType;
        this.cp = cp;
        this.accessFlags = AccessFlag.build(raw.getU2At(0L));
        int attributes_count = raw.getU2At(6L);
        ArrayList<Attribute> tmpAttributes = new ArrayList<Attribute>();
        tmpAttributes.ensureCapacity(attributes_count);
        long attributesLength = ContiguousEntityFactory.build(raw.getOffsetData(8L), attributes_count, tmpAttributes, AttributeFactory.getBuilder(cp, classFileVersion));
        this.attributes = new AttributeMap(tmpAttributes);
        AccessFlag.applyAttributes(this.attributes, this.accessFlags);
        this.descriptorIndex = raw.getU2At(4L);
        int nameIndex = raw.getU2At(2L);
        this.length = 8L + attributesLength;
        AttributeConstantValue cvAttribute = (AttributeConstantValue)this.attributes.getByName("ConstantValue");
        this.fieldName = cp.getUTF8Entry(nameIndex).getValue();
        this.disambiguate = false;
        TypedLiteral constValue = null;
        if (cvAttribute != null && (constValue = TypedLiteral.getConstantPoolEntry(cp, cvAttribute.getValue())).getType() == TypedLiteral.LiteralType.Integer && (thisType = this.getJavaTypeInstance()) instanceof RawJavaType) {
            constValue = TypedLiteral.shrinkTo(constValue, (RawJavaType)thisType);
        }
        this.constantValue = constValue;
    }

    @Override
    public long getRawByteLength() {
        return this.length;
    }

    private AttributeSignature getSignatureAttribute() {
        return (AttributeSignature)this.attributes.getByName("Signature");
    }

    private JavaTypeInstance generateTypeInstance() {
        JavaTypeInstance desType;
        AttributeSignature sig = this.getSignatureAttribute();
        ConstantPoolEntryUTF8 signature = sig == null ? null : sig.getSignature();
        ConstantPoolEntryUTF8 descriptor = this.cp.getUTF8Entry(this.descriptorIndex);
        JavaTypeInstance sigType = null;
        if (signature != null) {
            try {
                sigType = ConstantPoolUtils.decodeTypeTok(signature.getValue(), this.cp);
                if (sigType != null) {
                    return sigType;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        try {
            desType = ConstantPoolUtils.decodeTypeTok(descriptor.getValue(), this.cp);
        }
        catch (RuntimeException e) {
            if (sigType == null) {
                throw e;
            }
            desType = sigType;
        }
        if (sigType != null) {
            if (sigType.getDeGenerifiedType().equals(desType.getDeGenerifiedType())) {
                return sigType;
            }
            return desType;
        }
        return desType;
    }

    public JavaTypeInstance getJavaTypeInstance() {
        if (this.cachedDecodedType == null) {
            this.cachedDecodedType = this.generateTypeInstance();
        }
        return this.cachedDecodedType;
    }

    void setDisambiguate() {
        this.disambiguate = true;
    }

    public String getFieldName() {
        if (this.disambiguate) {
            return "var_" + ClassNameUtils.getTypeFixPrefix(this.getJavaTypeInstance()) + this.fieldName;
        }
        return this.fieldName;
    }

    public boolean testAccessFlag(AccessFlag accessFlag) {
        return this.accessFlags.contains((Object)accessFlag);
    }

    public Set<AccessFlag> getAccessFlags() {
        return this.accessFlags;
    }

    public TypedLiteral getConstantValue() {
        return this.constantValue;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.getJavaTypeInstance());
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeVisibleAnnotations"));
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeInvisibleAnnotations"));
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeVisibleTypeAnnotations"));
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeInvisibleTypeAnnotations"));
    }

    public void dump(Dumper d, String name, ClassFile owner, boolean asRecordField) {
        JavaTypeInstance type = this.getJavaTypeInstance();
        List<AnnotationTableEntry> declarationAnnotations = MiscAnnotations.BasicAnnotations(this.attributes);
        TypeAnnotationHelper tah = TypeAnnotationHelper.create(this.attributes, TypeAnnotationEntryValue.type_field);
        List<AnnotationTableTypeEntry> fieldTypeAnnotations = tah == null ? null : tah.getEntries();
        DeclarationAnnotationHelper.DeclarationAnnotationsInfo annotationsInfo = DeclarationAnnotationHelper.getDeclarationInfo(type, declarationAnnotations, fieldTypeAnnotations);
        boolean usesAdmissibleType = !annotationsInfo.requiresNonAdmissibleType();
        List<AnnotationTableEntry> declAnnotationsToDump = annotationsInfo.getDeclarationAnnotations(usesAdmissibleType);
        List<AnnotationTableTypeEntry> typeAnnotationsToDump = annotationsInfo.getTypeAnnotations(usesAdmissibleType);
        for (AnnotationTableEntry annotation : declAnnotationsToDump) {
            annotation.dump(d);
            if (asRecordField) {
                d.print(" ");
                continue;
            }
            d.newln();
        }
        if (!asRecordField) {
            String prefix;
            Set<AccessFlag> accessFlagsLocal = this.accessFlags;
            if (((Boolean)this.cp.getDCCommonState().getOptions().getOption(OptionsImpl.ATTRIBUTE_OBF)).booleanValue()) {
                accessFlagsLocal = SetFactory.newSet(accessFlagsLocal);
                accessFlagsLocal.remove((Object)AccessFlag.ACC_ENUM);
                accessFlagsLocal.remove((Object)AccessFlag.ACC_SYNTHETIC);
            }
            if (!(prefix = CollectionUtils.join(accessFlagsLocal, " ")).isEmpty()) {
                d.keyword(prefix).print(' ');
            }
        }
        if (typeAnnotationsToDump.isEmpty()) {
            d.dump(type);
        } else {
            JavaAnnotatedTypeInstance jah = type.getAnnotatedInstance();
            DecompilerComments comments = new DecompilerComments();
            TypeAnnotationHelper.apply(jah, typeAnnotationsToDump, comments);
            d.dump(comments);
            d.dump(jah);
        }
        d.print(' ').fieldName(name, owner.getClassType(), false, false, true);
    }

    public boolean isAccessibleFrom(JavaRefTypeInstance maybeCaller, ClassFile classFile) {
        return VisibilityHelper.isVisibleTo(maybeCaller, classFile, this.testAccessFlag(AccessFlag.ACC_PUBLIC), this.testAccessFlag(AccessFlag.ACC_PRIVATE), this.testAccessFlag(AccessFlag.ACC_PROTECTED));
    }
}

