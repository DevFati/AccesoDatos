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
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.ObfuscationTypeMap;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

public class JavaGenericPlaceholderTypeInstance
implements JavaGenericBaseInstance {
    private final String className;
    private final ConstantPool cp;
    private final JavaTypeInstance bound;

    public JavaGenericPlaceholderTypeInstance(String className, ConstantPool cp) {
        this.className = className;
        this.cp = cp;
        this.bound = null;
    }

    private JavaGenericPlaceholderTypeInstance(String className, ConstantPool cp, JavaTypeInstance bound) {
        this.className = className;
        this.cp = cp;
        this.bound = bound;
    }

    public JavaGenericPlaceholderTypeInstance withBound(JavaTypeInstance bound) {
        if (bound == null) {
            return this;
        }
        return new JavaGenericPlaceholderTypeInstance(this.className, this.cp, bound);
    }

    @Override
    public JavaTypeInstance getBoundInstance(GenericTypeBinder genericTypeBinder) {
        return genericTypeBinder.getBindingFor(this);
    }

    @Override
    public JavaAnnotatedTypeInstance getAnnotatedInstance() {
        return new Annotated();
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public boolean hasUnbound() {
        return true;
    }

    @Override
    public boolean hasL01Wildcard() {
        return false;
    }

    @Override
    public JavaTypeInstance getWithoutL01Wildcard() {
        return this;
    }

    @Override
    public List<JavaTypeInstance> getGenericTypes() {
        return ListFactory.newImmutableList(this);
    }

    @Override
    public boolean hasForeignUnbound(ConstantPool cp, int depth, boolean noWildcard, Map<String, FormalTypeParameter> externals) {
        if (this.className.equals("?")) {
            return depth == 0 || noWildcard;
        }
        if (!cp.equals(this.cp)) {
            return true;
        }
        if (externals == null) {
            return false;
        }
        return !externals.containsKey(this.className);
    }

    @Override
    public boolean tryFindBinding(JavaTypeInstance other, GenericTypeBinder target) {
        target.suggestBindingFor(this.className, other);
        return true;
    }

    @Override
    public StackType getStackType() {
        return StackType.REF;
    }

    @Override
    public void dumpInto(Dumper d, TypeUsageInformation typeUsageInformation, TypeContext typeContext) {
        d.print(this.toString());
    }

    public String toString() {
        return this.className;
    }

    @Override
    public JavaTypeInstance getArrayStrippedType() {
        return this;
    }

    @Override
    public int getNumArrayDimensions() {
        return 0;
    }

    @Override
    public String getRawName() {
        return this.className;
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
    public JavaTypeInstance directImplOf(JavaTypeInstance other) {
        return this.bound == null ? null : this.bound.directImplOf(other);
    }

    public int hashCode() {
        return 31 + this.className.hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof JavaGenericPlaceholderTypeInstance)) {
            return false;
        }
        JavaGenericPlaceholderTypeInstance other = (JavaGenericPlaceholderTypeInstance)o;
        return other.className.equals(this.className);
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
        return this;
    }

    @Override
    public JavaTypeInstance getDeGenerifiedType() {
        return this.bound == null ? TypeConstants.OBJECT : this.bound.getDeGenerifiedType();
    }

    @Override
    public RawJavaType getRawTypeOfSimpleType() {
        return RawJavaType.REF;
    }

    @Override
    public void collectInto(TypeUsageCollector typeUsageCollector) {
    }

    @Override
    public boolean implicitlyCastsTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        if (other == TypeConstants.OBJECT) {
            return true;
        }
        if (other.equals(this)) {
            return true;
        }
        if (this.bound != null) {
            return this.bound.implicitlyCastsTo(other, gtb);
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
        if (this.className.equals("?")) {
            return "obj";
        }
        return this.className;
    }

    @Override
    public JavaTypeInstance deObfuscate(ObfuscationTypeMap obfuscationTypeMap) {
        return new JavaGenericPlaceholderTypeInstance(this.className, this.cp, obfuscationTypeMap.get(this.bound));
    }

    @Override
    public JavaGenericRefTypeInstance asGenericRefInstance(JavaTypeInstance other) {
        return null;
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    private class Annotated
    implements JavaAnnotatedTypeInstance {
        private final List<AnnotationTableEntry> entries = ListFactory.newList();

        private Annotated() {
        }

        @Override
        public JavaAnnotatedTypeIterator pathIterator() {
            return new Iterator();
        }

        @Override
        public Dumper dump(Dumper d) {
            if (!this.entries.isEmpty()) {
                for (AnnotationTableEntry entry : this.entries) {
                    entry.dump(d);
                    d.print(' ');
                }
            }
            d.print(JavaGenericPlaceholderTypeInstance.this.className);
            return d;
        }

        private class Iterator
        extends JavaAnnotatedTypeIterator.BaseAnnotatedTypeIterator {
            private Iterator() {
            }

            @Override
            public void apply(AnnotationTableEntry entry) {
                Annotated.this.entries.add(entry);
            }
        }
    }
}

