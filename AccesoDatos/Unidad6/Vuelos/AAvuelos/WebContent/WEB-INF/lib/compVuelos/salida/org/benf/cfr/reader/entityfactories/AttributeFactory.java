/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entityfactories;

import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.attributes.AttributeAnnotationDefault;
import org.benf.cfr.reader.entities.attributes.AttributeBootstrapMethods;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.attributes.AttributeConstantValue;
import org.benf.cfr.reader.entities.attributes.AttributeDeprecated;
import org.benf.cfr.reader.entities.attributes.AttributeEnclosingMethod;
import org.benf.cfr.reader.entities.attributes.AttributeExceptions;
import org.benf.cfr.reader.entities.attributes.AttributeInnerClasses;
import org.benf.cfr.reader.entities.attributes.AttributeLineNumberTable;
import org.benf.cfr.reader.entities.attributes.AttributeLocalVariableTable;
import org.benf.cfr.reader.entities.attributes.AttributeLocalVariableTypeTable;
import org.benf.cfr.reader.entities.attributes.AttributeModule;
import org.benf.cfr.reader.entities.attributes.AttributeModuleClassMain;
import org.benf.cfr.reader.entities.attributes.AttributeModulePackages;
import org.benf.cfr.reader.entities.attributes.AttributePermittedSubclasses;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeInvisibleAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeInvisibleParameterAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeInvisibleTypeAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleParameterAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleTypeAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeScala;
import org.benf.cfr.reader.entities.attributes.AttributeScalaSig;
import org.benf.cfr.reader.entities.attributes.AttributeSignature;
import org.benf.cfr.reader.entities.attributes.AttributeSourceFile;
import org.benf.cfr.reader.entities.attributes.AttributeStackMapTable;
import org.benf.cfr.reader.entities.attributes.AttributeSynthetic;
import org.benf.cfr.reader.entities.attributes.AttributeUnknown;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class AttributeFactory {
    private static final long OFFSET_OF_ATTRIBUTE_NAME_INDEX = 0L;

    public static Attribute build(ByteData raw, ConstantPool cp, ClassFileVersion classFileVersion) {
        int nameIndex = raw.getU2At(0L);
        ConstantPoolEntryUTF8 name = (ConstantPoolEntryUTF8)cp.getEntry(nameIndex);
        String attributeName = name.getValue();
        if ("Code".equals(attributeName)) {
            return new AttributeCode(raw, cp, classFileVersion);
        }
        try {
            if ("LocalVariableTable".equals(attributeName)) {
                return new AttributeLocalVariableTable(raw);
            }
            if ("Signature".equals(attributeName)) {
                return new AttributeSignature(raw, cp);
            }
            if ("ConstantValue".equals(attributeName)) {
                return new AttributeConstantValue(raw, cp);
            }
            if ("LineNumberTable".equals(attributeName)) {
                return new AttributeLineNumberTable(raw);
            }
            if ("Exceptions".equals(attributeName)) {
                return new AttributeExceptions(raw, cp);
            }
            if ("EnclosingMethod".equals(attributeName)) {
                return new AttributeEnclosingMethod(raw);
            }
            if ("Deprecated".equals(attributeName)) {
                return new AttributeDeprecated(raw);
            }
            if ("RuntimeVisibleAnnotations".equals(attributeName)) {
                return new AttributeRuntimeVisibleAnnotations(raw, cp);
            }
            if ("RuntimeVisibleTypeAnnotations".equals(attributeName)) {
                return new AttributeRuntimeVisibleTypeAnnotations(raw, cp);
            }
            if ("RuntimeInvisibleTypeAnnotations".equals(attributeName)) {
                return new AttributeRuntimeInvisibleTypeAnnotations(raw, cp);
            }
            if ("RuntimeInvisibleAnnotations".equals(attributeName)) {
                return new AttributeRuntimeInvisibleAnnotations(raw, cp);
            }
            if ("RuntimeVisibleParameterAnnotations".equals(attributeName)) {
                return new AttributeRuntimeVisibleParameterAnnotations(raw, cp);
            }
            if ("RuntimeInvisibleParameterAnnotations".equals(attributeName)) {
                return new AttributeRuntimeInvisibleParameterAnnotations(raw, cp);
            }
            if ("SourceFile".equals(attributeName)) {
                return new AttributeSourceFile(raw);
            }
            if ("InnerClasses".equals(attributeName)) {
                return new AttributeInnerClasses(raw, cp);
            }
            if ("BootstrapMethods".equals(attributeName)) {
                return new AttributeBootstrapMethods(raw, cp);
            }
            if ("AnnotationDefault".equals(attributeName)) {
                return new AttributeAnnotationDefault(raw, cp);
            }
            if ("LocalVariableTypeTable".equals(attributeName)) {
                return new AttributeLocalVariableTypeTable(raw);
            }
            if ("StackMapTable".equals(attributeName)) {
                return new AttributeStackMapTable(raw, cp);
            }
            if ("Synthetic".equals(attributeName)) {
                return new AttributeSynthetic(raw);
            }
            if ("ScalaSig".equals(attributeName)) {
                return new AttributeScalaSig(raw);
            }
            if ("Scala".equals(attributeName)) {
                return new AttributeScala(raw);
            }
            if ("Module".equals(attributeName)) {
                return new AttributeModule(raw, cp);
            }
            if ("ModulePackages".equals(attributeName)) {
                return new AttributeModulePackages(raw);
            }
            if ("ModuleClassMain".equals(attributeName)) {
                return new AttributeModuleClassMain(raw);
            }
            if ("PermittedSubclasses".equals(attributeName)) {
                return new AttributePermittedSubclasses(raw, cp);
            }
        }
        catch (Exception e) {
            MiscUtils.handyBreakPoint();
        }
        return new AttributeUnknown(raw, attributeName);
    }

    public static UnaryFunction<ByteData, Attribute> getBuilder(ConstantPool cp, ClassFileVersion classFileVersion) {
        return new AttributeBuilder(cp, classFileVersion);
    }

    private static class AttributeBuilder
    implements UnaryFunction<ByteData, Attribute> {
        private final ConstantPool cp;
        private final ClassFileVersion classFileVersion;

        AttributeBuilder(ConstantPool cp, ClassFileVersion classFileVersion) {
            this.cp = cp;
            this.classFileVersion = classFileVersion;
        }

        @Override
        public Attribute invoke(ByteData arg) {
            return AttributeFactory.build(arg, this.cp, this.classFileVersion);
        }
    }
}

