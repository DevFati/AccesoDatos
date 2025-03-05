/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.state.ObfuscationTypeMap;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.ToStringDumper;
import org.benf.cfr.reader.util.output.TypeContext;

public class JavaArrayTypeInstance
implements JavaTypeInstance {
    private final int dimensions;
    private final JavaTypeInstance underlyingType;
    private JavaTypeInstance cachedDegenerifiedType;

    public JavaArrayTypeInstance(int dimensions, JavaTypeInstance underlyingType) {
        this.dimensions = dimensions;
        this.underlyingType = underlyingType;
    }

    @Override
    public JavaAnnotatedTypeInstance getAnnotatedInstance() {
        return new Annotated();
    }

    @Override
    public StackType getStackType() {
        return StackType.REF;
    }

    @Override
    public void dumpInto(Dumper d, TypeUsageInformation typeUsageInformation, TypeContext typeContext) {
        this.toCommonString(this.getNumArrayDimensions(), d);
    }

    public String toString() {
        return new ToStringDumper().dump(this).toString();
    }

    private void toCommonString(int numDims, Dumper d) {
        d.dump(this.underlyingType.getArrayStrippedType());
        for (int x = 0; x < numDims; ++x) {
            d.print("[]");
        }
    }

    void toVarargString(Dumper d) {
        this.toCommonString(this.getNumArrayDimensions() - 1, d);
        d.print(" ...");
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public String getRawName() {
        return new ToStringDumper().dump(this).toString();
    }

    @Override
    public String getRawName(IllegalIdentifierDump iid) {
        return this.getRawName();
    }

    @Override
    public InnerClassInfo getInnerClassHereInfo() {
        return InnerClassInfo.NOT;
    }

    @Override
    public BindingSuperContainer getBindingSupers() {
        return null;
    }

    @Override
    public JavaTypeInstance getArrayStrippedType() {
        if (this.underlyingType instanceof JavaArrayTypeInstance) {
            return this.underlyingType.getArrayStrippedType();
        }
        return this.underlyingType;
    }

    @Override
    public int getNumArrayDimensions() {
        return this.dimensions + this.underlyingType.getNumArrayDimensions();
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    public int hashCode() {
        return this.dimensions * 31 + this.underlyingType.hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JavaArrayTypeInstance)) {
            return false;
        }
        JavaArrayTypeInstance other = (JavaArrayTypeInstance)o;
        return other.dimensions == this.dimensions && other.underlyingType.equals(this.underlyingType);
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
        if (this.dimensions == 1) {
            return this.underlyingType;
        }
        return new JavaArrayTypeInstance(this.dimensions - 1, this.underlyingType);
    }

    @Override
    public JavaTypeInstance getDeGenerifiedType() {
        if (this.cachedDegenerifiedType == null) {
            this.cachedDegenerifiedType = new JavaArrayTypeInstance(this.dimensions, this.underlyingType.getDeGenerifiedType());
        }
        return this.cachedDegenerifiedType;
    }

    @Override
    public RawJavaType getRawTypeOfSimpleType() {
        return RawJavaType.REF;
    }

    @Override
    public void collectInto(TypeUsageCollector typeUsageCollector) {
        typeUsageCollector.collect(this.underlyingType);
    }

    @Override
    public boolean implicitlyCastsTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        if (other == TypeConstants.OBJECT) {
            return true;
        }
        if (other instanceof JavaArrayTypeInstance) {
            JavaArrayTypeInstance arrayOther = (JavaArrayTypeInstance)other;
            if (this.getNumArrayDimensions() != arrayOther.getNumArrayDimensions()) {
                return false;
            }
            return this.getArrayStrippedType().implicitlyCastsTo(arrayOther.getArrayStrippedType(), gtb);
        }
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
    public String suggestVarName() {
        return this.underlyingType.suggestVarName() + "Array";
    }

    @Override
    public JavaGenericRefTypeInstance asGenericRefInstance(JavaTypeInstance other) {
        return null;
    }

    @Override
    public JavaTypeInstance deObfuscate(ObfuscationTypeMap obfuscationTypeMap) {
        return new JavaArrayTypeInstance(this.dimensions, obfuscationTypeMap.get(this.underlyingType));
    }

    @Override
    public JavaTypeInstance directImplOf(JavaTypeInstance other) {
        return null;
    }

    private class Annotated
    implements JavaAnnotatedTypeInstance {
        private final List<List<AnnotationTableEntry>> entries = ListFactory.newList();
        private final JavaAnnotatedTypeInstance annotatedUnderlyingType;

        Annotated() {
            for (int x = 0; x < JavaArrayTypeInstance.this.dimensions; ++x) {
                this.entries.add(ListFactory.newList());
            }
            this.annotatedUnderlyingType = JavaArrayTypeInstance.this.underlyingType.getAnnotatedInstance();
        }

        @Override
        public Dumper dump(Dumper d) {
            this.annotatedUnderlyingType.dump(d);
            for (List<AnnotationTableEntry> entry : this.entries) {
                if (!entry.isEmpty()) {
                    d.print(' ');
                    for (AnnotationTableEntry oneEntry : entry) {
                        oneEntry.dump(d);
                        d.print(' ');
                    }
                }
                d.print("[]");
            }
            return d;
        }

        @Override
        public JavaAnnotatedTypeIterator pathIterator() {
            return new Iterator();
        }

        private class Iterator
        extends JavaAnnotatedTypeIterator.BaseAnnotatedTypeIterator {
            private int curIdx;

            private Iterator() {
                this.curIdx = 0;
            }

            private Iterator(int idx) {
                this.curIdx = idx;
            }

            @Override
            public JavaAnnotatedTypeIterator moveArray(DecompilerComments comments) {
                if (this.curIdx + 1 < JavaArrayTypeInstance.this.dimensions) {
                    return new Iterator(this.curIdx + 1);
                }
                return Annotated.this.annotatedUnderlyingType.pathIterator();
            }

            @Override
            public void apply(AnnotationTableEntry entry) {
                ((List)Annotated.this.entries.get(this.curIdx)).add(entry);
            }
        }
    }
}

