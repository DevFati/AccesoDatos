/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.DetectedStaticImport;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.state.TypeUsageUtils;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

public class InnerClassTypeUsageInformation
implements TypeUsageInformation {
    private final IllegalIdentifierDump iid;
    private final TypeUsageInformation delegate;
    private final JavaRefTypeInstance analysisInnerClass;
    private final Map<JavaRefTypeInstance, String> localTypeNames = MapFactory.newMap();
    private final Set<String> usedLocalTypeNames = SetFactory.newSet();
    private final Set<JavaRefTypeInstance> usedInnerClassTypes = SetFactory.newSet();

    public InnerClassTypeUsageInformation(TypeUsageInformation delegate, JavaRefTypeInstance analysisInnerClass) {
        this.delegate = delegate;
        this.analysisInnerClass = analysisInnerClass;
        this.iid = delegate.getIid();
        this.initializeFrom();
    }

    private boolean clashesWithField(String name) {
        JavaRefTypeInstance type = this.analysisInnerClass;
        if (type == null) {
            return false;
        }
        ClassFile classFile = type.getClassFile();
        if (classFile == null) {
            return false;
        }
        return classFile.hasLocalField(name);
    }

    @Override
    public IllegalIdentifierDump getIid() {
        return this.iid;
    }

    @Override
    public JavaRefTypeInstance getAnalysisType() {
        return this.delegate.getAnalysisType();
    }

    private void initializeFrom() {
        Set<JavaRefTypeInstance> outerInners = this.delegate.getUsedInnerClassTypes();
        for (JavaRefTypeInstance outerInner : outerInners) {
            if (!outerInner.getInnerClassHereInfo().isTransitiveInnerClassOf(this.analysisInnerClass)) continue;
            this.usedInnerClassTypes.add(outerInner);
            String name = TypeUsageUtils.generateInnerClassShortName(this.iid, outerInner, this.analysisInnerClass, false);
            if (this.usedLocalTypeNames.contains(name)) continue;
            this.localTypeNames.put(outerInner, name);
            this.usedLocalTypeNames.add(name);
        }
    }

    @Override
    public Set<JavaRefTypeInstance> getUsedClassTypes() {
        return this.delegate.getUsedClassTypes();
    }

    @Override
    public Set<JavaRefTypeInstance> getUsedInnerClassTypes() {
        return this.usedInnerClassTypes;
    }

    @Override
    public boolean hasLocalInstance(JavaRefTypeInstance type) {
        return this.localTypeNames.get(type) != null;
    }

    @Override
    public String getName(JavaTypeInstance type, TypeContext typeContext) {
        String local = this.localTypeNames.get(type);
        if (local != null) {
            return local;
        }
        String res = this.delegate.getName(type, typeContext);
        if (this.usedLocalTypeNames.contains(res)) {
            return type.getRawName(this.iid);
        }
        if (this.isNameClash(type, res, typeContext)) {
            return type.getRawName(this.iid);
        }
        return res;
    }

    @Override
    public boolean isNameClash(JavaTypeInstance type, String name, TypeContext typeContext) {
        return typeContext == TypeContext.Static && (this.clashesWithField(name) || this.delegate.isNameClash(type, name, typeContext));
    }

    @Override
    public String generateInnerClassShortName(JavaRefTypeInstance clazz) {
        return this.delegate.generateInnerClassShortName(clazz);
    }

    @Override
    public boolean isStaticImport(JavaTypeInstance clazz, String fixedName) {
        return false;
    }

    @Override
    public Set<DetectedStaticImport> getDetectedStaticImports() {
        return Collections.emptySet();
    }

    @Override
    public String generateOverriddenName(JavaRefTypeInstance clazz) {
        return this.delegate.generateOverriddenName(clazz);
    }

    @Override
    public Set<JavaRefTypeInstance> getShortenedClassTypes() {
        return this.delegate.getShortenedClassTypes();
    }
}

