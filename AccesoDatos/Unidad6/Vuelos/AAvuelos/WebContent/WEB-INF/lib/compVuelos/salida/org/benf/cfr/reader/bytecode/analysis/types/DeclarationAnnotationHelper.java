/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.attributes.TypePathPart;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.ListFactory;

public class DeclarationAnnotationHelper {
    private static final DecompilerComments EMPTY_DECOMPILER_COMMENTS = new DecompilerComments();

    private DeclarationAnnotationHelper() {
    }

    private static Set<JavaTypeInstance> getDeclAndTypeUseAnnotationTypes(List<AnnotationTableEntry> declAnnotations, List<AnnotationTableTypeEntry> typeAnnotations) {
        HashSet<JavaTypeInstance> declTypeAnnotations = new HashSet<JavaTypeInstance>();
        for (AnnotationTableEntry declAnn : declAnnotations) {
            declTypeAnnotations.add(declAnn.getClazz());
        }
        ArrayList<JavaTypeInstance> typeAnnotationClasses = new ArrayList<JavaTypeInstance>();
        for (AnnotationTableTypeEntry typeAnn : typeAnnotations) {
            typeAnnotationClasses.add(typeAnn.getClazz());
        }
        declTypeAnnotations.retainAll(typeAnnotationClasses);
        return declTypeAnnotations;
    }

    private static Integer getCommonInnerClassAnnotationIndex(List<AnnotationTableTypeEntry> typeAnnotations) {
        Integer commonIndex = null;
        for (AnnotationTableTypeEntry annotation : typeAnnotations) {
            NestedCountingIterator annotationIterator = new NestedCountingIterator();
            for (TypePathPart typePathPart : annotation.getTypePath().segments) {
                typePathPart.apply(annotationIterator, EMPTY_DECOMPILER_COMMENTS);
                if (!annotationIterator.wasOtherTypeUsed) continue;
                break;
            }
            if (commonIndex == null) {
                commonIndex = annotationIterator.nestingLevel;
                continue;
            }
            if (commonIndex == annotationIterator.nestingLevel) continue;
            return null;
        }
        return commonIndex;
    }

    private static boolean canTypeAnnotationBeMovedToDecl(JavaTypeInstance annotatedType, AnnotationTableTypeEntry typeAnnotation, Integer commonInnerAnnotationIndex) {
        List<TypePathPart> typePathParts = typeAnnotation.getTypePath().segments;
        if (annotatedType.getInnerClassHereInfo().isInnerClass()) {
            NestedCountingIterator annotationIterator = new NestedCountingIterator();
            for (TypePathPart typePathPart : typePathParts) {
                typePathPart.apply(annotationIterator, EMPTY_DECOMPILER_COMMENTS);
                if (!annotationIterator.wasOtherTypeUsed) continue;
                return false;
            }
            int nestingLevel = annotationIterator.nestingLevel;
            return nestingLevel == 0 || commonInnerAnnotationIndex != null && nestingLevel == commonInnerAnnotationIndex;
        }
        if (annotatedType.getNumArrayDimensions() > 0) {
            ArrayCountingIterator annotationIterator = new ArrayCountingIterator();
            for (TypePathPart typePathPart : typePathParts) {
                typePathPart.apply(annotationIterator, EMPTY_DECOMPILER_COMMENTS);
                if (!annotationIterator.wasOtherTypeUsed) continue;
                return false;
            }
            return annotationIterator.componentLevel == annotatedType.getNumArrayDimensions();
        }
        return typePathParts.isEmpty();
    }

    private static boolean areAnnotationsEqual(List<AnnotationTableEntry> declAnnotations, List<AnnotationTableTypeEntry> typeAnnotations) {
        if (declAnnotations.size() != typeAnnotations.size()) {
            return false;
        }
        for (int i = 0; i < declAnnotations.size(); ++i) {
            if (declAnnotations.get(i).isAnnotationEqual(typeAnnotations.get(i))) continue;
            return false;
        }
        return true;
    }

    public static DeclarationAnnotationsInfo getDeclarationInfo(JavaTypeInstance nullableAnnotatedType, List<AnnotationTableEntry> declarationAnnotations, List<AnnotationTableTypeEntry> typeAnnotations) {
        boolean contradictingMoves;
        if (declarationAnnotations == null || declarationAnnotations.isEmpty() || typeAnnotations == null || typeAnnotations.isEmpty()) {
            return DeclarationAnnotationsInfo.possibleAdmissible(ListFactory.orEmptyList(declarationAnnotations), ListFactory.orEmptyList(typeAnnotations));
        }
        Set<JavaTypeInstance> declTypeAnnotations = DeclarationAnnotationHelper.getDeclAndTypeUseAnnotationTypes(declarationAnnotations, typeAnnotations);
        if (declTypeAnnotations.isEmpty()) {
            return DeclarationAnnotationsInfo.possibleAdmissible(declarationAnnotations, typeAnnotations);
        }
        Integer commonInnerAnnotationIndex = DeclarationAnnotationHelper.getCommonInnerClassAnnotationIndex(typeAnnotations);
        ArrayList<AnnotationTableTypeEntry> typeDeclTypeAnnotations = new ArrayList<AnnotationTableTypeEntry>();
        boolean requiresMoveToTypeAnn = false;
        int firstMovedTypeAnnIndex = -1;
        for (AnnotationTableTypeEntry typeAnn : typeAnnotations) {
            if (declTypeAnnotations.contains(typeAnn.getClazz())) {
                if (firstMovedTypeAnnIndex != -1 && nullableAnnotatedType != null && !DeclarationAnnotationHelper.canTypeAnnotationBeMovedToDecl(nullableAnnotatedType, typeAnn, commonInnerAnnotationIndex)) {
                    firstMovedTypeAnnIndex = typeDeclTypeAnnotations.size();
                }
                if (firstMovedTypeAnnIndex != -1) {
                    requiresMoveToTypeAnn = true;
                }
                typeDeclTypeAnnotations.add(typeAnn);
                continue;
            }
            if (firstMovedTypeAnnIndex != -1) continue;
            firstMovedTypeAnnIndex = typeDeclTypeAnnotations.size();
        }
        int lastNonMovableDeclAnnIndex = -1;
        ArrayList<AnnotationTableEntry> declDeclTypeAnnotations = new ArrayList<AnnotationTableEntry>();
        for (AnnotationTableEntry declAnn : declarationAnnotations) {
            if (declTypeAnnotations.contains(declAnn.getClazz())) {
                declDeclTypeAnnotations.add(declAnn);
                continue;
            }
            lastNonMovableDeclAnnIndex = declDeclTypeAnnotations.size() - 1;
        }
        boolean bl = contradictingMoves = requiresMoveToTypeAnn && lastNonMovableDeclAnnIndex >= firstMovedTypeAnnIndex;
        if (contradictingMoves || typeDeclTypeAnnotations.size() != declDeclTypeAnnotations.size()) {
            return DeclarationAnnotationsInfo.requiringNonAdmissible(declarationAnnotations, typeAnnotations);
        }
        if (!DeclarationAnnotationHelper.areAnnotationsEqual(declDeclTypeAnnotations, typeDeclTypeAnnotations)) {
            return DeclarationAnnotationsInfo.requiringNonAdmissible(declarationAnnotations, typeAnnotations);
        }
        if (requiresMoveToTypeAnn) {
            ArrayList<AnnotationTableEntry> declAnnotationsAdmissible = new ArrayList<AnnotationTableEntry>(declarationAnnotations);
            declAnnotationsAdmissible.removeAll(declDeclTypeAnnotations.subList(firstMovedTypeAnnIndex, declDeclTypeAnnotations.size()));
            ArrayList<AnnotationTableTypeEntry> typeAnnotationsAdmissible = new ArrayList<AnnotationTableTypeEntry>(typeAnnotations);
            typeAnnotationsAdmissible.removeAll(typeDeclTypeAnnotations.subList(0, firstMovedTypeAnnIndex));
            return DeclarationAnnotationsInfo.possibleAdmissible(declAnnotationsAdmissible, declarationAnnotations, typeAnnotationsAdmissible, typeAnnotations);
        }
        ArrayList<AnnotationTableTypeEntry> typeAnnotationsAdmissible = new ArrayList<AnnotationTableTypeEntry>(typeAnnotations);
        typeAnnotationsAdmissible.removeAll(typeDeclTypeAnnotations);
        return DeclarationAnnotationsInfo.possibleAdmissible(declarationAnnotations, declarationAnnotations, typeAnnotationsAdmissible, typeAnnotations);
    }

    private static class ArrayCountingIterator
    extends SinglePartTypeIterator {
        private int componentLevel = 0;

        private ArrayCountingIterator() {
        }

        @Override
        public JavaAnnotatedTypeIterator moveArray(DecompilerComments comments) {
            ++this.componentLevel;
            return this;
        }
    }

    private static class NestedCountingIterator
    extends SinglePartTypeIterator {
        private int nestingLevel = 0;

        private NestedCountingIterator() {
        }

        @Override
        public JavaAnnotatedTypeIterator moveNested(DecompilerComments comments) {
            ++this.nestingLevel;
            return this;
        }
    }

    private static class SinglePartTypeIterator
    implements JavaAnnotatedTypeIterator {
        protected boolean wasOtherTypeUsed = false;

        private SinglePartTypeIterator() {
        }

        @Override
        public JavaAnnotatedTypeIterator moveArray(DecompilerComments comments) {
            this.wasOtherTypeUsed = true;
            return this;
        }

        @Override
        public JavaAnnotatedTypeIterator moveBound(DecompilerComments comments) {
            this.wasOtherTypeUsed = true;
            return this;
        }

        @Override
        public JavaAnnotatedTypeIterator moveNested(DecompilerComments comments) {
            this.wasOtherTypeUsed = true;
            return this;
        }

        @Override
        public JavaAnnotatedTypeIterator moveParameterized(int index, DecompilerComments comments) {
            this.wasOtherTypeUsed = true;
            return this;
        }

        @Override
        public void apply(AnnotationTableEntry entry) {
        }
    }

    public static class DeclarationAnnotationsInfo {
        private final List<AnnotationTableEntry> declAnnotationsAdmissible;
        private final List<AnnotationTableEntry> declAnnotationsNonAdmissible;
        private final List<AnnotationTableTypeEntry> typeAnnotationsAdmissible;
        private final List<AnnotationTableTypeEntry> typeAnnotationsNonAdmissible;
        private final boolean requiresNonAdmissibleType;

        private DeclarationAnnotationsInfo(List<AnnotationTableEntry> declAnnotationsAdmissible, List<AnnotationTableEntry> declAnnotationsNonAdmissible, List<AnnotationTableTypeEntry> typeAnnotationsAdmissible, List<AnnotationTableTypeEntry> typeAnnotationsNonAdmissible, boolean requiresNonAdmissibleType) {
            this.declAnnotationsAdmissible = declAnnotationsAdmissible;
            this.declAnnotationsNonAdmissible = declAnnotationsNonAdmissible;
            this.typeAnnotationsAdmissible = typeAnnotationsAdmissible;
            this.typeAnnotationsNonAdmissible = typeAnnotationsNonAdmissible;
            this.requiresNonAdmissibleType = requiresNonAdmissibleType;
        }

        private static DeclarationAnnotationsInfo possibleAdmissible(List<AnnotationTableEntry> declAnnotations, List<AnnotationTableTypeEntry> typeAnnotations) {
            return new DeclarationAnnotationsInfo(declAnnotations, declAnnotations, typeAnnotations, typeAnnotations, false);
        }

        private static DeclarationAnnotationsInfo possibleAdmissible(List<AnnotationTableEntry> declAnnotationsAdmissible, List<AnnotationTableEntry> declAnnotationsNonAdmissible, List<AnnotationTableTypeEntry> typeAnnotationsAdmissible, List<AnnotationTableTypeEntry> typeAnnotationsNonAdmissible) {
            return new DeclarationAnnotationsInfo(declAnnotationsAdmissible, declAnnotationsNonAdmissible, typeAnnotationsAdmissible, typeAnnotationsNonAdmissible, false);
        }

        private static DeclarationAnnotationsInfo requiringNonAdmissible(List<AnnotationTableEntry> declAnnotations, List<AnnotationTableTypeEntry> typeAnnotations) {
            return new DeclarationAnnotationsInfo(null, declAnnotations, null, typeAnnotations, true);
        }

        public boolean requiresNonAdmissibleType() {
            return this.requiresNonAdmissibleType;
        }

        private void checkCanProvideAnnotations(boolean usesAdmissibleType) {
            if (usesAdmissibleType && this.requiresNonAdmissibleType()) {
                throw new IllegalArgumentException("Can only provide annotations if non-admissible type is used");
            }
        }

        public List<AnnotationTableEntry> getDeclarationAnnotations(boolean usesAdmissibleType) {
            this.checkCanProvideAnnotations(usesAdmissibleType);
            return usesAdmissibleType ? this.declAnnotationsAdmissible : this.declAnnotationsNonAdmissible;
        }

        public List<AnnotationTableTypeEntry> getTypeAnnotations(boolean usesAdmissibleType) {
            this.checkCanProvideAnnotations(usesAdmissibleType);
            return usesAdmissibleType ? this.typeAnnotationsAdmissible : this.typeAnnotationsNonAdmissible;
        }
    }
}

