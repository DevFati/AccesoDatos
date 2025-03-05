/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class ClassCache {
    private final Map<String, JavaRefTypeInstance> refClassTypeCache = MapFactory.newMap();
    private final Set<String> simpleClassNamesSeen = SetFactory.newSet();
    private final Map<String, String> renamedClasses = MapFactory.newMap();
    private final DCCommonState dcCommonState;

    ClassCache(DCCommonState dcCommonState) {
        this.dcCommonState = dcCommonState;
        this.add(TypeConstants.ASSERTION_ERROR.getRawName(), TypeConstants.ASSERTION_ERROR);
        this.add(TypeConstants.OBJECT.getRawName(), TypeConstants.OBJECT);
        this.add(TypeConstants.STRING.getRawName(), TypeConstants.STRING);
        this.add(TypeConstants.ENUM.getRawName(), TypeConstants.ENUM);
    }

    public JavaRefTypeInstance getRefClassFor(String rawClassName) {
        String originalRawClassName = ClassNameUtils.convertToPath(rawClassName);
        rawClassName = this.dcCommonState.getPossiblyRenamedFileFromClassFileSource(originalRawClassName);
        String name = ClassNameUtils.convertFromPath(rawClassName);
        JavaRefTypeInstance typeInstance = this.refClassTypeCache.get(name);
        String originalName = null;
        if (!rawClassName.equals(originalRawClassName)) {
            originalName = ClassNameUtils.convertFromPath(originalRawClassName);
        }
        if (typeInstance == null) {
            typeInstance = JavaRefTypeInstance.create(name, this.dcCommonState);
            this.add(name, originalName, typeInstance);
        }
        return typeInstance;
    }

    private void add(String name, JavaRefTypeInstance typeInstance) {
        this.add(name, null, typeInstance);
    }

    private void add(String name, String originalName, JavaRefTypeInstance typeInstance) {
        this.refClassTypeCache.put(name, typeInstance);
        this.simpleClassNamesSeen.add(typeInstance.getRawShortName());
        if (originalName != null) {
            this.renamedClasses.put(name, originalName);
        }
    }

    public boolean isClassName(String name) {
        return this.simpleClassNamesSeen.contains(name);
    }

    public Pair<JavaRefTypeInstance, JavaRefTypeInstance> getRefClassForInnerOuterPair(String rawInnerName, String rawOuterName) {
        String innerName = ClassNameUtils.convertFromPath(rawInnerName);
        String outerName = ClassNameUtils.convertFromPath(rawOuterName);
        JavaRefTypeInstance inner = this.refClassTypeCache.get(innerName);
        JavaRefTypeInstance outer = this.refClassTypeCache.get(outerName);
        if (inner != null && outer != null) {
            return Pair.make(inner, outer);
        }
        Pair<JavaRefTypeInstance, JavaRefTypeInstance> pair = JavaRefTypeInstance.createKnownInnerOuter(innerName, outerName, outer, this.dcCommonState);
        if (inner == null) {
            this.add(innerName, pair.getFirst());
            inner = pair.getFirst();
        }
        if (outer == null) {
            this.add(outerName, pair.getSecond());
            outer = pair.getSecond();
        }
        return Pair.make(inner, outer);
    }

    public Collection<JavaRefTypeInstance> getLoadedTypes() {
        return this.refClassTypeCache.values();
    }

    String getOriginalName(String typeName) {
        return this.renamedClasses.get(typeName);
    }
}

