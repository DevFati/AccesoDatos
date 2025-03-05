/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.DetectedStaticImport;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.state.TypeUsageUtils;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

public class TypeUsageInformationImpl
implements TypeUsageInformation {
    private final IllegalIdentifierDump iid;
    private Set<DetectedStaticImport> staticImports;
    private final JavaRefTypeInstance analysisType;
    private final Set<JavaRefTypeInstance> usedRefTypes = SetFactory.newOrderedSet();
    private final Set<JavaRefTypeInstance> shortenedRefTypes = SetFactory.newOrderedSet();
    private final Set<JavaRefTypeInstance> usedLocalInnerTypes = SetFactory.newOrderedSet();
    private final Map<JavaRefTypeInstance, String> displayName = MapFactory.newMap();
    private final Map<String, LinkedList<JavaRefTypeInstance>> shortNames = MapFactory.newLazyMap(new UnaryFunction<String, LinkedList<JavaRefTypeInstance>>(){

        @Override
        public LinkedList<JavaRefTypeInstance> invoke(String arg) {
            return ListFactory.newLinkedList();
        }
    });
    private final Predicate<String> allowShorten;
    private final Map<String, Boolean> clashNames = MapFactory.newLazyMap(new FieldClash());

    public TypeUsageInformationImpl(Options options, JavaRefTypeInstance analysisType, Set<JavaRefTypeInstance> usedRefTypes, Set<DetectedStaticImport> staticImports) {
        this.allowShorten = MiscUtils.mkRegexFilter((String)options.getOption(OptionsImpl.IMPORT_FILTER), true);
        this.analysisType = analysisType;
        this.iid = IllegalIdentifierDump.Factory.getOrNull(options);
        this.staticImports = staticImports;
        this.initialiseFrom(usedRefTypes);
    }

    @Override
    public IllegalIdentifierDump getIid() {
        return this.iid;
    }

    @Override
    public JavaRefTypeInstance getAnalysisType() {
        return this.analysisType;
    }

    @Override
    public String generateInnerClassShortName(JavaRefTypeInstance clazz) {
        return TypeUsageUtils.generateInnerClassShortName(this.iid, clazz, this.analysisType, false);
    }

    @Override
    public String generateOverriddenName(JavaRefTypeInstance clazz) {
        if (clazz.getInnerClassHereInfo().isInnerClass()) {
            return TypeUsageUtils.generateInnerClassShortName(this.iid, clazz, this.analysisType, true);
        }
        return clazz.getRawName(this.iid);
    }

    private void initialiseFrom(Set<JavaRefTypeInstance> usedRefTypes) {
        List<JavaRefTypeInstance> usedRefs = ListFactory.newList(usedRefTypes);
        Collections.sort(usedRefs, new Comparator<JavaRefTypeInstance>(){

            @Override
            public int compare(JavaRefTypeInstance a, JavaRefTypeInstance b) {
                return a.getRawName(TypeUsageInformationImpl.this.iid).compareTo(b.getRawName(TypeUsageInformationImpl.this.iid));
            }
        });
        this.usedRefTypes.addAll(usedRefs);
        Pair<List<JavaRefTypeInstance>, List<JavaRefTypeInstance>> types = Functional.partition(usedRefs, new Predicate<JavaRefTypeInstance>(){

            @Override
            public boolean test(JavaRefTypeInstance in) {
                return in.getInnerClassHereInfo().isTransitiveInnerClassOf(TypeUsageInformationImpl.this.analysisType);
            }
        });
        this.usedLocalInnerTypes.addAll((Collection<JavaRefTypeInstance>)types.getFirst());
        this.addDisplayNames(usedRefTypes);
    }

    private void addDisplayNames(Collection<JavaRefTypeInstance> types) {
        String name;
        if (!this.shortNames.isEmpty()) {
            throw new IllegalStateException();
        }
        for (JavaRefTypeInstance javaRefTypeInstance : types) {
            InnerClassInfo innerClassInfo = javaRefTypeInstance.getInnerClassHereInfo();
            if (innerClassInfo.isInnerClass()) {
                name = this.generateInnerClassShortName(javaRefTypeInstance);
                this.shortNames.get(name).addFirst(javaRefTypeInstance);
                continue;
            }
            if (!this.allowShorten.test(javaRefTypeInstance.getRawName(this.iid))) continue;
            name = javaRefTypeInstance.getRawShortName(this.iid);
            this.shortNames.get(name).addLast(javaRefTypeInstance);
        }
        for (Map.Entry entry : this.shortNames.entrySet()) {
            LinkedList typeList = (LinkedList)entry.getValue();
            name = (String)entry.getKey();
            if (typeList.size() == 1) {
                this.displayName.put((JavaRefTypeInstance)typeList.get(0), name);
                this.shortenedRefTypes.add((JavaRefTypeInstance)typeList.get(0));
                continue;
            }
            class PriClass
            implements Comparable<PriClass> {
                private int priType;
                private boolean innerClass = false;
                private JavaRefTypeInstance type;

                PriClass(JavaRefTypeInstance type) {
                    if (type.equals(TypeUsageInformationImpl.this.analysisType)) {
                        this.priType = 0;
                    } else {
                        InnerClassInfo innerClassInfo = type.getInnerClassHereInfo();
                        if (innerClassInfo.isInnerClass()) {
                            this.innerClass = true;
                            this.priType = innerClassInfo.isTransitiveInnerClassOf(TypeUsageInformationImpl.this.analysisType) ? 1 : 3;
                        } else {
                            String p2;
                            String p1 = type.getPackageName();
                            this.priType = p1.startsWith(p2 = TypeUsageInformationImpl.this.analysisType.getPackageName()) || p2.startsWith(p1) ? 2 : 3;
                        }
                    }
                    this.type = type;
                }

                @Override
                public int compareTo(PriClass priClass) {
                    return this.priType - priClass.priType;
                }
            }
            List<PriClass> priClasses = Functional.map(typeList, new UnaryFunction<JavaRefTypeInstance, PriClass>(){

                @Override
                public PriClass invoke(JavaRefTypeInstance arg) {
                    return new PriClass(arg);
                }
            });
            Collections.sort(priClasses);
            this.displayName.put(priClasses.get(0).type, name);
            this.shortenedRefTypes.add(priClasses.get(0).type);
            priClasses.set(0, null);
            for (int x = 0; x < priClasses.size(); ++x) {
                PriClass priClass = priClasses.get(x);
                if (priClass == null || priClass.priType != 1) continue;
                this.displayName.put(priClass.type, name);
                this.shortenedRefTypes.add(priClass.type);
                priClasses.set(x, null);
            }
            for (PriClass priClass : priClasses) {
                String useName;
                if (priClass == null) continue;
                if (priClass.innerClass) {
                    useName = this.generateInnerClassShortName(priClass.type);
                    this.shortenedRefTypes.add(priClass.type);
                    this.displayName.put(priClass.type, useName);
                    continue;
                }
                useName = priClass.type.getRawName(this.iid);
                this.displayName.put(priClass.type, useName);
            }
        }
    }

    private boolean fieldClash(String name) {
        ClassFile classFile = this.analysisType.getClassFile();
        if (classFile == null) {
            return false;
        }
        return classFile.hasAccessibleField(name, this.analysisType);
    }

    @Override
    public Set<JavaRefTypeInstance> getUsedClassTypes() {
        return this.usedRefTypes;
    }

    @Override
    public Set<JavaRefTypeInstance> getShortenedClassTypes() {
        return this.shortenedRefTypes;
    }

    @Override
    public Set<JavaRefTypeInstance> getUsedInnerClassTypes() {
        return this.usedLocalInnerTypes;
    }

    @Override
    public boolean hasLocalInstance(JavaRefTypeInstance type) {
        return false;
    }

    @Override
    public boolean isStaticImport(JavaTypeInstance clazz, String fixedName) {
        return this.staticImports.contains(new DetectedStaticImport(clazz, fixedName));
    }

    @Override
    public Set<DetectedStaticImport> getDetectedStaticImports() {
        return this.staticImports;
    }

    @Override
    public String getName(JavaTypeInstance type, TypeContext typeContext) {
        String res = this.displayName.get(type);
        if (res == null) {
            return type.getRawName(this.iid);
        }
        if (this.isNameClash(type, res, typeContext)) {
            return type.getRawName(this.iid);
        }
        return res;
    }

    @Override
    public boolean isNameClash(JavaTypeInstance type, String name, TypeContext typeContext) {
        return typeContext == TypeContext.Static && this.clashNames.get(name) != false;
    }

    private class FieldClash
    implements UnaryFunction<String, Boolean> {
        private FieldClash() {
        }

        @Override
        public Boolean invoke(String name) {
            return TypeUsageInformationImpl.this.fieldClash(name);
        }
    }
}

