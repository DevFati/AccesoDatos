/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.DeclarationAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MiscAnnotations;
import org.benf.cfr.reader.bytecode.analysis.types.TypeAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.attributes.AttributeMap;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeInvisibleParameterAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleParameterAnnotations;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationTargetInfo;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.output.Dumper;

public class MethodPrototypeAnnotationsHelper {
    private final AttributeMap attributeMap;
    private final TypeAnnotationHelper typeAnnotationHelper;

    public MethodPrototypeAnnotationsHelper(AttributeMap attributes) {
        this.attributeMap = attributes;
        this.typeAnnotationHelper = TypeAnnotationHelper.create(attributes, TypeAnnotationEntryValue.type_generic_method_constructor, TypeAnnotationEntryValue.type_ret_or_new, TypeAnnotationEntryValue.type_receiver, TypeAnnotationEntryValue.type_throws, TypeAnnotationEntryValue.type_formal);
    }

    static void dumpAnnotationTableEntries(List<? extends AnnotationTableEntry> annotationTableEntries, Dumper d) {
        for (AnnotationTableEntry annotationTableEntry : annotationTableEntries) {
            annotationTableEntry.dump(d).print(' ');
        }
    }

    public List<AnnotationTableTypeEntry> getMethodReturnAnnotations() {
        return this.getTypeTargetAnnotations(TypeAnnotationEntryValue.type_ret_or_new);
    }

    public List<AnnotationTableTypeEntry> getTypeTargetAnnotations(final TypeAnnotationEntryValue target) {
        if (this.typeAnnotationHelper == null) {
            return null;
        }
        List<AnnotationTableTypeEntry> res = Functional.filter(this.typeAnnotationHelper.getEntries(), new Predicate<AnnotationTableTypeEntry>(){

            @Override
            public boolean test(AnnotationTableTypeEntry in) {
                return in.getValue() == target;
            }
        });
        if (res.isEmpty()) {
            return null;
        }
        return res;
    }

    public List<AnnotationTableEntry> getMethodAnnotations() {
        return MiscAnnotations.BasicAnnotations(this.attributeMap);
    }

    private List<AnnotationTableEntry> getParameterAnnotations(int idx) {
        AttributeRuntimeVisibleParameterAnnotations a1 = (AttributeRuntimeVisibleParameterAnnotations)this.attributeMap.getByName("RuntimeVisibleParameterAnnotations");
        AttributeRuntimeInvisibleParameterAnnotations a2 = (AttributeRuntimeInvisibleParameterAnnotations)this.attributeMap.getByName("RuntimeInvisibleParameterAnnotations");
        List<AnnotationTableEntry> e1 = a1 == null ? null : a1.getAnnotationsForParamIdx(idx);
        List<AnnotationTableEntry> e2 = a2 == null ? null : a2.getAnnotationsForParamIdx(idx);
        return ListFactory.combinedOptimistic(e1, e2);
    }

    private List<AnnotationTableTypeEntry> getTypeParameterAnnotations(final int paramIdx) {
        List<AnnotationTableTypeEntry> typeEntries = this.getTypeTargetAnnotations(TypeAnnotationEntryValue.type_formal);
        if (typeEntries == null) {
            return null;
        }
        if ((typeEntries = Functional.filter(typeEntries, new Predicate<AnnotationTableTypeEntry>(){

            @Override
            public boolean test(AnnotationTableTypeEntry in) {
                return ((TypeAnnotationTargetInfo.TypeAnnotationFormalParameterTarget)in.getTargetInfo()).getIndex() == paramIdx;
            }
        })).isEmpty()) {
            return null;
        }
        return typeEntries;
    }

    void dumpParamType(JavaTypeInstance arg, int paramIdx, Dumper d) {
        List<AnnotationTableTypeEntry> typeEntries;
        List<AnnotationTableEntry> entries = this.getParameterAnnotations(paramIdx);
        DeclarationAnnotationHelper.DeclarationAnnotationsInfo annotationsInfo = DeclarationAnnotationHelper.getDeclarationInfo(arg, entries, typeEntries = this.getTypeParameterAnnotations(paramIdx));
        boolean usesAdmissibleType = !annotationsInfo.requiresNonAdmissibleType();
        List<AnnotationTableEntry> declAnnotationsToDump = annotationsInfo.getDeclarationAnnotations(usesAdmissibleType);
        List<AnnotationTableTypeEntry> typeAnnotationsToDump = annotationsInfo.getTypeAnnotations(usesAdmissibleType);
        MethodPrototypeAnnotationsHelper.dumpAnnotationTableEntries(declAnnotationsToDump, d);
        if (typeAnnotationsToDump.isEmpty()) {
            d.dump(arg);
        } else {
            JavaAnnotatedTypeInstance jat = arg.getAnnotatedInstance();
            DecompilerComments comments = new DecompilerComments();
            TypeAnnotationHelper.apply(jat, typeAnnotationsToDump, comments);
            d.dump(comments);
            d.dump(jat);
        }
    }
}

