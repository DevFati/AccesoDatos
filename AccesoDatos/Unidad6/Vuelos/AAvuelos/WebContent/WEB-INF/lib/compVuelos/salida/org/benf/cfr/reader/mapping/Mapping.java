/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.mapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.innerclass.InnerClassAttributeInfo;
import org.benf.cfr.reader.mapping.ClassMapping;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.state.DetectedStaticImport;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.state.TypeUsageInformationImpl;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.output.DelegatingDumper;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

public class Mapping
implements ObfuscationMapping {
    private final Map<JavaTypeInstance, ClassMapping> erasedTypeMap = MapFactory.newMap();
    private final UnaryFunction<JavaTypeInstance, JavaTypeInstance> getter = new UnaryFunction<JavaTypeInstance, JavaTypeInstance>(){

        @Override
        public JavaTypeInstance invoke(JavaTypeInstance arg) {
            return Mapping.this.get(arg);
        }
    };
    private Options options;
    private Map<JavaTypeInstance, List<InnerClassAttributeInfo>> innerInfo;

    Mapping(Options options, List<ClassMapping> classMappings, Map<JavaTypeInstance, List<InnerClassAttributeInfo>> innerInfo) {
        this.options = options;
        this.innerInfo = innerInfo;
        for (ClassMapping cls : classMappings) {
            this.erasedTypeMap.put(cls.getObClass(), cls);
        }
    }

    @Override
    public Dumper wrap(Dumper d) {
        return new ObfuscationWrappingDumper(d);
    }

    @Override
    public boolean providesInnerClassInfo() {
        return true;
    }

    @Override
    public JavaTypeInstance get(JavaTypeInstance type) {
        if (type == null) {
            return null;
        }
        int numDim = type.getNumArrayDimensions();
        JavaTypeInstance strippedType = type.getArrayStrippedType();
        ClassMapping c = this.erasedTypeMap.get(strippedType);
        if (c == null) {
            return type;
        }
        JavaTypeInstance res = c.getRealClass();
        if (numDim > 0) {
            res = new JavaArrayTypeInstance(numDim, res);
        }
        return res;
    }

    @Override
    public List<JavaTypeInstance> get(List<JavaTypeInstance> types) {
        return Functional.map(types, new UnaryFunction<JavaTypeInstance, JavaTypeInstance>(){

            @Override
            public JavaTypeInstance invoke(JavaTypeInstance arg) {
                return Mapping.this.get(arg);
            }
        });
    }

    ClassMapping getClassMapping(JavaTypeInstance type) {
        return this.erasedTypeMap.get(type.getDeGenerifiedType());
    }

    @Override
    public List<InnerClassAttributeInfo> getInnerClassInfo(JavaTypeInstance classType) {
        return this.innerInfo.get(classType);
    }

    @Override
    public UnaryFunction<JavaTypeInstance, JavaTypeInstance> getter() {
        return this.getter;
    }

    private class ObfuscationWrappingDumper
    extends DelegatingDumper {
        private TypeUsageInformation mappingTypeUsage;

        private ObfuscationWrappingDumper(Dumper delegate) {
            super(delegate);
            this.mappingTypeUsage = null;
        }

        private ObfuscationWrappingDumper(Dumper delegate, TypeUsageInformation typeUsageInformation) {
            super(delegate);
            this.mappingTypeUsage = typeUsageInformation;
        }

        @Override
        public TypeUsageInformation getTypeUsageInformation() {
            if (this.mappingTypeUsage == null) {
                TypeUsageInformation dti = this.delegate.getTypeUsageInformation();
                TypeUsageInformationImpl dtr = new TypeUsageInformationImpl(Mapping.this.options, (JavaRefTypeInstance)Mapping.this.get(dti.getAnalysisType()), SetFactory.newOrderedSet(Functional.map(dti.getUsedClassTypes(), new UnaryFunction<JavaRefTypeInstance, JavaRefTypeInstance>(){

                    @Override
                    public JavaRefTypeInstance invoke(JavaRefTypeInstance arg) {
                        return (JavaRefTypeInstance)Mapping.this.get(arg);
                    }
                })), SetFactory.<DetectedStaticImport>newSet());
                this.mappingTypeUsage = new MappingTypeUsage(dtr, dti);
            }
            return this.mappingTypeUsage;
        }

        @Override
        public ObfuscationMapping getObfuscationMapping() {
            return Mapping.this;
        }

        @Override
        public Dumper methodName(String s, MethodPrototype p, boolean special, boolean defines) {
            ClassMapping c;
            JavaTypeInstance classType = p == null ? null : p.getClassType();
            ClassMapping classMapping = c = classType == null ? null : (ClassMapping)Mapping.this.erasedTypeMap.get(classType.getDeGenerifiedType());
            if (c == null || special) {
                this.delegate.methodName(s, p, special, defines);
                return this;
            }
            this.delegate.methodName(c.getMethodName(s, p.getSignatureBoundArgs(), Mapping.this, this.delegate), p, special, defines);
            return this;
        }

        @Override
        public Dumper fieldName(String name, JavaTypeInstance owner, boolean hiddenDeclaration, boolean isStatic, boolean defines) {
            JavaTypeInstance deGenerifiedType = owner.getDeGenerifiedType();
            ClassMapping c = (ClassMapping)Mapping.this.erasedTypeMap.get(deGenerifiedType);
            if (c == null || hiddenDeclaration) {
                this.delegate.fieldName(name, owner, hiddenDeclaration, isStatic, defines);
            } else {
                this.delegate.fieldName(c.getFieldName(name, deGenerifiedType, this, Mapping.this, isStatic), owner, hiddenDeclaration, isStatic, defines);
            }
            return this;
        }

        @Override
        public Dumper packageName(JavaRefTypeInstance t) {
            JavaTypeInstance deGenerifiedType = t.getDeGenerifiedType();
            ClassMapping c = (ClassMapping)Mapping.this.erasedTypeMap.get(deGenerifiedType);
            if (c == null) {
                this.delegate.packageName(t);
            } else {
                this.delegate.packageName(c.getRealClass());
            }
            return this;
        }

        @Override
        public Dumper dump(JavaTypeInstance javaTypeInstance) {
            this.dump(javaTypeInstance, TypeContext.None);
            return this;
        }

        @Override
        public Dumper dump(JavaTypeInstance javaTypeInstance, TypeContext typeContext) {
            javaTypeInstance = javaTypeInstance.deObfuscate(Mapping.this);
            javaTypeInstance.dumpInto(this, this.getTypeUsageInformation(), typeContext);
            return this;
        }

        @Override
        public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
            return new ObfuscationWrappingDumper(this.delegate, innerclassTypeUsageInformation);
        }
    }

    private class MappingTypeUsage
    implements TypeUsageInformation {
        private final TypeUsageInformation delegateRemapped;
        private final TypeUsageInformation delegateOriginal;

        private MappingTypeUsage(TypeUsageInformation delegateRemapped, TypeUsageInformation delegateOriginal) {
            this.delegateRemapped = delegateRemapped;
            this.delegateOriginal = delegateOriginal;
        }

        @Override
        public IllegalIdentifierDump getIid() {
            return this.delegateOriginal.getIid();
        }

        @Override
        public boolean isStaticImport(JavaTypeInstance clazz, String fixedName) {
            return this.delegateOriginal.isStaticImport(clazz, fixedName);
        }

        @Override
        public Set<DetectedStaticImport> getDetectedStaticImports() {
            return this.delegateOriginal.getDetectedStaticImports();
        }

        @Override
        public JavaRefTypeInstance getAnalysisType() {
            return this.delegateRemapped.getAnalysisType();
        }

        @Override
        public Set<JavaRefTypeInstance> getShortenedClassTypes() {
            return this.delegateRemapped.getShortenedClassTypes();
        }

        @Override
        public Set<JavaRefTypeInstance> getUsedClassTypes() {
            return this.delegateOriginal.getUsedClassTypes();
        }

        @Override
        public Set<JavaRefTypeInstance> getUsedInnerClassTypes() {
            return this.delegateOriginal.getUsedClassTypes();
        }

        @Override
        public String getName(JavaTypeInstance type, TypeContext typeContext) {
            return this.delegateRemapped.getName(Mapping.this.get(type), typeContext);
        }

        @Override
        public boolean isNameClash(JavaTypeInstance type, String name, TypeContext typeContext) {
            return this.delegateRemapped.isNameClash(type, name, typeContext);
        }

        @Override
        public boolean hasLocalInstance(JavaRefTypeInstance type) {
            return this.delegateOriginal.hasLocalInstance(type);
        }

        @Override
        public String generateInnerClassShortName(JavaRefTypeInstance clazz) {
            return this.delegateRemapped.generateInnerClassShortName((JavaRefTypeInstance)Mapping.this.get(clazz));
        }

        @Override
        public String generateOverriddenName(JavaRefTypeInstance clazz) {
            return this.delegateRemapped.generateOverriddenName(clazz);
        }
    }
}

