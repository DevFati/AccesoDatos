/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.FormalTypeParameter;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.WildcardType;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.ObfuscationTypeMap;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.ToStringDumper;
import org.benf.cfr.reader.util.output.TypeContext;

public class JavaWildcardTypeInstance
implements JavaGenericBaseInstance {
    private final WildcardType wildcardType;
    private final JavaTypeInstance underlyingType;

    public JavaWildcardTypeInstance(WildcardType wildcardType, JavaTypeInstance underlyingType) {
        this.wildcardType = wildcardType;
        this.underlyingType = underlyingType;
    }

    @Override
    public JavaTypeInstance getBoundInstance(GenericTypeBinder genericTypeBinder) {
        if (this.underlyingType instanceof JavaGenericBaseInstance) {
            return ((JavaGenericBaseInstance)this.underlyingType).getBoundInstance(genericTypeBinder);
        }
        return this.underlyingType;
    }

    @Override
    public JavaAnnotatedTypeInstance getAnnotatedInstance() {
        return new Annotated();
    }

    @Override
    public boolean hasL01Wildcard() {
        return true;
    }

    @Override
    public JavaTypeInstance getWithoutL01Wildcard() {
        return this.underlyingType;
    }

    public JavaTypeInstance getUnderlyingType() {
        return this.underlyingType;
    }

    @Override
    public boolean tryFindBinding(JavaTypeInstance other, GenericTypeBinder target) {
        if (this.underlyingType instanceof JavaGenericBaseInstance) {
            return ((JavaGenericBaseInstance)this.underlyingType).tryFindBinding(other, target);
        }
        return false;
    }

    @Override
    public StackType getStackType() {
        return StackType.REF;
    }

    @Override
    public boolean hasUnbound() {
        if (this.underlyingType instanceof JavaGenericBaseInstance) {
            return ((JavaGenericBaseInstance)this.underlyingType).hasUnbound();
        }
        return false;
    }

    @Override
    public boolean hasForeignUnbound(ConstantPool cp, int depth, boolean noWildcard, Map<String, FormalTypeParameter> externals) {
        if (this.underlyingType instanceof JavaGenericBaseInstance) {
            return ((JavaGenericBaseInstance)this.underlyingType).hasForeignUnbound(cp, depth, noWildcard, externals);
        }
        return false;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public List<JavaTypeInstance> getGenericTypes() {
        if (this.underlyingType instanceof JavaGenericBaseInstance) {
            return ((JavaGenericBaseInstance)this.underlyingType).getGenericTypes();
        }
        return ListFactory.newList();
    }

    @Override
    public void dumpInto(Dumper d, TypeUsageInformation typeUsageInformation, TypeContext typeContext) {
        d.print("? ").print(this.wildcardType.toString()).print(' ');
        d.dump(this.underlyingType);
    }

    public String toString() {
        return new ToStringDumper().dump(this).toString();
    }

    @Override
    public String getRawName() {
        return this.toString();
    }

    @Override
    public String getRawName(IllegalIdentifierDump iid) {
        return this.getRawName();
    }

    @Override
    public void collectInto(TypeUsageCollector typeUsageCollector) {
        this.underlyingType.collectInto(typeUsageCollector);
    }

    @Override
    public InnerClassInfo getInnerClassHereInfo() {
        return this.underlyingType.getInnerClassHereInfo();
    }

    @Override
    public BindingSuperContainer getBindingSupers() {
        return this.underlyingType.getBindingSupers();
    }

    @Override
    public JavaTypeInstance getArrayStrippedType() {
        return this.underlyingType.getArrayStrippedType();
    }

    @Override
    public int getNumArrayDimensions() {
        return this.underlyingType.getNumArrayDimensions();
    }

    public int hashCode() {
        return this.wildcardType.hashCode() * 31 + this.underlyingType.hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JavaWildcardTypeInstance)) {
            return false;
        }
        JavaWildcardTypeInstance other = (JavaWildcardTypeInstance)o;
        return other.wildcardType == this.wildcardType && other.underlyingType.equals(this.underlyingType);
    }

    @Override
    public boolean isComplexType() {
        return true;
    }

    @Override
    public boolean isUsableType() {
        return true;
    }

    @Override
    public JavaTypeInstance removeAnArrayIndirection() {
        return this.underlyingType.removeAnArrayIndirection();
    }

    @Override
    public JavaTypeInstance getDeGenerifiedType() {
        return this.underlyingType;
    }

    @Override
    public RawJavaType getRawTypeOfSimpleType() {
        return this.underlyingType.getRawTypeOfSimpleType();
    }

    @Override
    public boolean implicitlyCastsTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        return false;
    }

    @Override
    public boolean impreciseCanCastTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        return true;
    }

    @Override
    public boolean correctCanCastTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        return this.impreciseCanCastTo(other, gtb);
    }

    @Override
    public JavaTypeInstance directImplOf(JavaTypeInstance other) {
        return this.underlyingType.directImplOf(other);
    }

    @Override
    public String suggestVarName() {
        return null;
    }

    @Override
    public JavaGenericRefTypeInstance asGenericRefInstance(JavaTypeInstance other) {
        return null;
    }

    @Override
    public JavaTypeInstance deObfuscate(ObfuscationTypeMap obfuscationTypeMap) {
        return new JavaWildcardTypeInstance(this.wildcardType, obfuscationTypeMap.get(this.underlyingType));
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    private class Annotated
    implements JavaAnnotatedTypeInstance {
        private final List<AnnotationTableEntry> entries = ListFactory.newList();
        private final JavaAnnotatedTypeInstance underlyingAnnotated;

        private Annotated() {
            this.underlyingAnnotated = JavaWildcardTypeInstance.this.underlyingType.getAnnotatedInstance();
        }

        @Override
        public JavaAnnotatedTypeIterator pathIterator() {
            return new Iterator();
        }

        @Override
        public Dumper dump(Dumper d) {
            for (AnnotationTableEntry entry : this.entries) {
                entry.dump(d);
                d.print(' ');
            }
            d.print("? ").print(JavaWildcardTypeInstance.this.wildcardType.toString()).print(' ');
            this.underlyingAnnotated.dump(d);
            return d;
        }

        private class Iterator
        extends JavaAnnotatedTypeIterator.BaseAnnotatedTypeIterator {
            private Iterator() {
            }

            @Override
            public JavaAnnotatedTypeIterator moveBound(DecompilerComments comments) {
                return Annotated.this.underlyingAnnotated.pathIterator();
            }

            @Override
            public void apply(AnnotationTableEntry entry) {
                Annotated.this.entries.add(entry);
            }
        }
    }
}

