/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.annotations.ElementValue;
import org.benf.cfr.reader.entities.annotations.ElementValueAnnotation;
import org.benf.cfr.reader.entities.annotations.ElementValueArray;
import org.benf.cfr.reader.entities.annotations.ElementValueClass;
import org.benf.cfr.reader.entities.annotations.ElementValueConst;
import org.benf.cfr.reader.entities.annotations.ElementValueEnum;
import org.benf.cfr.reader.entities.attributes.BadAttributeException;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryKind;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationTargetInfo;
import org.benf.cfr.reader.entities.attributes.TypePath;
import org.benf.cfr.reader.entities.attributes.TypePathPart;
import org.benf.cfr.reader.entities.attributes.TypePathPartArray;
import org.benf.cfr.reader.entities.attributes.TypePathPartBound;
import org.benf.cfr.reader.entities.attributes.TypePathPartNested;
import org.benf.cfr.reader.entities.attributes.TypePathPartParameterized;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolUtils;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;

class AnnotationHelpers {
    AnnotationHelpers() {
    }

    static Pair<Long, AnnotationTableEntry> getAnnotation(ByteData raw, long offset, ConstantPool cp) {
        ConstantPoolEntryUTF8 typeName = cp.getUTF8Entry(raw.getU2At(offset));
        int numElementPairs = raw.getU2At(offset += 2L);
        offset += 2L;
        Map<String, ElementValue> elementValueMap = MapFactory.newOrderedMap();
        for (int x = 0; x < numElementPairs; ++x) {
            offset = AnnotationHelpers.getElementValuePair(raw, offset, cp, elementValueMap);
        }
        return new Pair<Long, AnnotationTableEntry>(offset, new AnnotationTableEntry(ConstantPoolUtils.decodeTypeTok(typeName.getValue(), cp), elementValueMap));
    }

    private static long getElementValuePair(ByteData raw, long offset, ConstantPool cp, Map<String, ElementValue> res) {
        ConstantPoolEntryUTF8 elementName = cp.getUTF8Entry(raw.getU2At(offset));
        Pair<Long, ElementValue> elementValueP = AnnotationHelpers.getElementValue(raw, offset += 2L, cp);
        offset = elementValueP.getFirst();
        res.put(elementName.getValue(), elementValueP.getSecond());
        return offset;
    }

    static Pair<Long, ElementValue> getElementValue(ByteData raw, long offset, ConstantPool cp) {
        char c = (char)raw.getU1At(offset);
        ++offset;
        switch (c) {
            case 'B': 
            case 'C': 
            case 'D': 
            case 'F': 
            case 'I': 
            case 'J': 
            case 'S': 
            case 'Z': {
                RawJavaType rawJavaType = ConstantPoolUtils.decodeRawJavaType(c);
                ConstantPoolEntry constantPoolEntry = cp.getEntry(raw.getU2At(offset));
                TypedLiteral typedLiteral = TypedLiteral.getConstantPoolEntry(cp, constantPoolEntry);
                ElementValue value = new ElementValueConst(typedLiteral);
                value = value.withTypeHint(rawJavaType);
                return new Pair<Long, ElementValue>(offset + 2L, value);
            }
            case 's': {
                ConstantPoolEntry constantPoolEntry = cp.getEntry(raw.getU2At(offset));
                TypedLiteral typedLiteral = TypedLiteral.getConstantPoolEntryUTF8((ConstantPoolEntryUTF8)constantPoolEntry);
                return new Pair<Long, ElementValue>(offset + 2L, new ElementValueConst(typedLiteral));
            }
            case 'e': {
                ConstantPoolEntryUTF8 enumClassName = cp.getUTF8Entry(raw.getU2At(offset));
                ConstantPoolEntryUTF8 enumEntryName = cp.getUTF8Entry(raw.getU2At(offset + 2L));
                return new Pair<Long, ElementValue>(offset + 4L, new ElementValueEnum(ConstantPoolUtils.decodeTypeTok(enumClassName.getValue(), cp), enumEntryName.getValue()));
            }
            case 'c': {
                ConstantPoolEntryUTF8 className = cp.getUTF8Entry(raw.getU2At(offset));
                String typeName = className.getValue();
                if (typeName.equals("V")) {
                    return new Pair<Long, ElementValue>(offset + 2L, new ElementValueClass(RawJavaType.VOID));
                }
                return new Pair<Long, ElementValue>(offset + 2L, new ElementValueClass(ConstantPoolUtils.decodeTypeTok(typeName, cp)));
            }
            case '@': {
                Pair<Long, AnnotationTableEntry> ape = AnnotationHelpers.getAnnotation(raw, offset, cp);
                return new Pair<Long, ElementValue>(ape.getFirst(), new ElementValueAnnotation(ape.getSecond()));
            }
            case '[': {
                int numArrayEntries = raw.getU2At(offset);
                offset += 2L;
                List<ElementValue> res = ListFactory.newList();
                for (int x = 0; x < numArrayEntries; ++x) {
                    Pair<Long, ElementValue> ape = AnnotationHelpers.getElementValue(raw, offset, cp);
                    offset = ape.getFirst();
                    res.add(ape.getSecond());
                }
                return new Pair<Long, ElementValue>(offset, new ElementValueArray(res));
            }
        }
        throw new ConfusedCFRException("Illegal attribute tag [" + c + "]");
    }

    static Pair<Long, AnnotationTableTypeEntry> getTypeAnnotation(ByteData raw, long offset, ConstantPool cp) {
        short targetType = raw.getU1At(offset++);
        TypeAnnotationEntryValue typeAnnotationEntryValue = TypeAnnotationEntryValue.get(targetType);
        Pair<Long, TypeAnnotationTargetInfo> targetInfoPair = AnnotationHelpers.readTypeAnnotationTargetInfo(typeAnnotationEntryValue.getKind(), raw, offset);
        offset = targetInfoPair.getFirst();
        TypeAnnotationTargetInfo targetInfo = targetInfoPair.getSecond();
        int type_path_length = raw.getU1At(offset++);
        List<TypePathPart> pathData = ListFactory.newList();
        block6: for (int x = 0; x < type_path_length; ++x) {
            short type_path_kind = raw.getU1At(offset++);
            short type_argument_index = raw.getU1At(offset++);
            switch (type_path_kind) {
                case 0: {
                    pathData.add(TypePathPartArray.INSTANCE);
                    continue block6;
                }
                case 1: {
                    pathData.add(TypePathPartNested.INSTANCE);
                    continue block6;
                }
                case 2: {
                    pathData.add(TypePathPartBound.INSTANCE);
                    continue block6;
                }
                case 3: {
                    pathData.add(new TypePathPartParameterized(type_argument_index));
                }
            }
        }
        TypePath path = new TypePath(pathData);
        int type_index = raw.getU2At(offset);
        ConstantPoolEntryUTF8 type_entry = cp.getUTF8Entry(type_index);
        JavaTypeInstance type = ConstantPoolUtils.decodeTypeTok(type_entry.getValue(), cp);
        int numElementPairs = raw.getU2At(offset += 2L);
        offset += 2L;
        Map<String, ElementValue> elementValueMap = MapFactory.newOrderedMap();
        for (int x = 0; x < numElementPairs; ++x) {
            offset = AnnotationHelpers.getElementValuePair(raw, offset, cp, elementValueMap);
        }
        AnnotationTableTypeEntry<TypeAnnotationTargetInfo> res = new AnnotationTableTypeEntry<TypeAnnotationTargetInfo>(typeAnnotationEntryValue, targetInfo, path, type, elementValueMap);
        return new Pair<Long, AnnotationTableTypeEntry>(offset, res);
    }

    private static Pair<Long, TypeAnnotationTargetInfo> readTypeAnnotationTargetInfo(TypeAnnotationEntryKind kind, ByteData raw, long offset) {
        switch (kind) {
            case type_parameter_target: {
                return TypeAnnotationTargetInfo.TypeAnnotationParameterTarget.Read(raw, offset);
            }
            case supertype_target: {
                return TypeAnnotationTargetInfo.TypeAnnotationSupertypeTarget.Read(raw, offset);
            }
            case type_parameter_bound_target: {
                return TypeAnnotationTargetInfo.TypeAnnotationParameterBoundTarget.Read(raw, offset);
            }
            case empty_target: {
                return TypeAnnotationTargetInfo.TypeAnnotationEmptyTarget.Read(raw, offset);
            }
            case method_formal_parameter_target: {
                return TypeAnnotationTargetInfo.TypeAnnotationFormalParameterTarget.Read(raw, offset);
            }
            case throws_target: {
                return TypeAnnotationTargetInfo.TypeAnnotationThrowsTarget.Read(raw, offset);
            }
            case localvar_target: {
                return TypeAnnotationTargetInfo.TypeAnnotationLocalVarTarget.Read(raw, offset);
            }
            case catch_target: {
                return TypeAnnotationTargetInfo.TypeAnnotationCatchTarget.Read(raw, offset);
            }
            case offset_target: {
                return TypeAnnotationTargetInfo.TypeAnnotationOffsetTarget.Read(raw, offset);
            }
            case type_argument_target: {
                return TypeAnnotationTargetInfo.TypeAnnotationTypeArgumentTarget.Read(raw, offset);
            }
        }
        throw new BadAttributeException();
    }
}

