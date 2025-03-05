/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.annotations;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.annotations.ElementValue;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryKind;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationTargetInfo;
import org.benf.cfr.reader.entities.attributes.TypePath;

public class AnnotationTableTypeEntry<T extends TypeAnnotationTargetInfo>
extends AnnotationTableEntry {
    private final TypeAnnotationEntryValue value;
    private final T targetInfo;
    private final TypePath typePath;

    public AnnotationTableTypeEntry(TypeAnnotationEntryValue value, T targetInfo, TypePath typePath, JavaTypeInstance type, Map<String, ElementValue> elementValueMap) {
        super(type, elementValueMap);
        this.value = value;
        this.targetInfo = targetInfo;
        this.typePath = typePath;
    }

    public TypePath getTypePath() {
        return this.typePath;
    }

    public TypeAnnotationEntryValue getValue() {
        return this.value;
    }

    public TypeAnnotationEntryKind getKind() {
        return this.value.getKind();
    }

    public T getTargetInfo() {
        return this.targetInfo;
    }
}

