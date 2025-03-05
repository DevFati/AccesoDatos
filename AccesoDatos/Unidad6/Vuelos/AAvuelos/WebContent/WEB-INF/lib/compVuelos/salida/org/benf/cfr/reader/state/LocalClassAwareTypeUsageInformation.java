/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.state.DetectedStaticImport;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

public class LocalClassAwareTypeUsageInformation
implements TypeUsageInformation {
    private final TypeUsageInformation delegate;
    private final Map<JavaTypeInstance, String> localTypeNames;
    private final Set<String> usedLocalTypeNames;

    public LocalClassAwareTypeUsageInformation(Map<JavaRefTypeInstance, String> localClassTypes, TypeUsageInformation delegate) {
        this.delegate = delegate;
        LazyMap<String, Integer> lastClassByName = MapFactory.newLazyMap(new UnaryFunction<String, Integer>(){

            @Override
            public Integer invoke(String arg) {
                return 0;
            }
        });
        this.localTypeNames = MapFactory.newMap();
        this.usedLocalTypeNames = SetFactory.newSet();
        for (Map.Entry<JavaRefTypeInstance, String> entry : localClassTypes.entrySet()) {
            String usedName;
            JavaRefTypeInstance localType = entry.getKey();
            String suggestedName = entry.getValue();
            if (suggestedName != null) {
                usedName = suggestedName;
            } else {
                String name = delegate.generateInnerClassShortName(localType);
                int len = name.length();
                for (int idx = 0; idx < len; ++idx) {
                    char c = name.charAt(idx);
                    if (c >= '0' && c <= '9') continue;
                    name = name.substring(idx);
                    break;
                }
                int x = (Integer)lastClassByName.get(name);
                lastClassByName.put(name, x + 1);
                usedName = name + (x == 0 ? "" : "_" + x);
            }
            this.localTypeNames.put(localType, usedName);
            this.usedLocalTypeNames.add(usedName);
        }
    }

    @Override
    public IllegalIdentifierDump getIid() {
        return this.delegate.getIid();
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
    public JavaRefTypeInstance getAnalysisType() {
        return this.delegate.getAnalysisType();
    }

    @Override
    public Set<JavaRefTypeInstance> getUsedClassTypes() {
        return this.delegate.getUsedClassTypes();
    }

    @Override
    public Set<JavaRefTypeInstance> getUsedInnerClassTypes() {
        return this.delegate.getUsedInnerClassTypes();
    }

    @Override
    public String getName(JavaTypeInstance type, TypeContext typeContext) {
        String local = this.localTypeNames.get(type);
        if (local != null) {
            return local;
        }
        String res = this.delegate.getName(type, typeContext);
        if (this.usedLocalTypeNames.contains(res)) {
            if (type instanceof JavaRefTypeInstance) {
                return this.delegate.generateOverriddenName((JavaRefTypeInstance)type);
            }
            return type.getRawName();
        }
        return res;
    }

    @Override
    public boolean isNameClash(JavaTypeInstance type, String name, TypeContext typeContext) {
        return this.delegate.isNameClash(type, name, typeContext);
    }

    @Override
    public String generateInnerClassShortName(JavaRefTypeInstance clazz) {
        return this.delegate.generateInnerClassShortName(clazz);
    }

    @Override
    public boolean hasLocalInstance(JavaRefTypeInstance type) {
        return this.localTypeNames.containsKey(type);
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

