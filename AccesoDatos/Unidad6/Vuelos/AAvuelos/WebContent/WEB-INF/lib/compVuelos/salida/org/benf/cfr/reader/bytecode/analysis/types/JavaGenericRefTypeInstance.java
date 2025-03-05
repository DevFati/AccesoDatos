/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ComparableUnderEC;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.DefaultEquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.FormalTypeParameter;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaWildcardTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.ObfuscationTypeMap;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.ToStringDumper;
import org.benf.cfr.reader.util.output.TypeContext;

public class JavaGenericRefTypeInstance
implements JavaGenericBaseInstance,
ComparableUnderEC {
    private static final WildcardConstraint WILDCARD_CONSTRAINT = new WildcardConstraint();
    private final JavaRefTypeInstance typeInstance;
    private final List<JavaTypeInstance> genericTypes;
    private final boolean hasUnbound;

    public JavaGenericRefTypeInstance(JavaTypeInstance typeInstance, List<JavaTypeInstance> genericTypes) {
        if (!(typeInstance instanceof JavaRefTypeInstance)) {
            throw new IllegalStateException("Generic sitting on top of non reftype");
        }
        this.typeInstance = (JavaRefTypeInstance)typeInstance;
        this.genericTypes = genericTypes;
        boolean unbound = false;
        for (JavaTypeInstance type : genericTypes) {
            if (!(type instanceof JavaGenericBaseInstance) || !((JavaGenericBaseInstance)type).hasUnbound()) continue;
            unbound = true;
            break;
        }
        this.hasUnbound = unbound;
    }

    @Override
    public void collectInto(TypeUsageCollector typeUsageCollector) {
        typeUsageCollector.collectRefType(this.typeInstance);
        for (JavaTypeInstance genericType : this.genericTypes) {
            typeUsageCollector.collect(genericType);
        }
    }

    @Override
    public JavaAnnotatedTypeInstance getAnnotatedInstance() {
        JavaAnnotatedTypeInstance typeAnnotated = this.typeInstance.getAnnotatedInstance();
        List genericTypeAnnotated = ListFactory.newList();
        for (JavaTypeInstance genericType : this.genericTypes) {
            genericTypeAnnotated.add(genericType.getAnnotatedInstance());
        }
        return new Annotated(typeAnnotated, genericTypeAnnotated);
    }

    @Override
    public boolean hasUnbound() {
        return this.hasUnbound;
    }

    @Override
    public boolean hasForeignUnbound(ConstantPool cp, int depth, boolean noWildcard, Map<String, FormalTypeParameter> externals) {
        if (!this.hasUnbound) {
            return false;
        }
        ++depth;
        for (JavaTypeInstance type : this.genericTypes) {
            if (!(type instanceof JavaGenericBaseInstance) || !((JavaGenericBaseInstance)type).hasForeignUnbound(cp, depth, noWildcard, externals)) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public boolean hasL01Wildcard() {
        for (JavaTypeInstance type : this.genericTypes) {
            if (!(type instanceof JavaWildcardTypeInstance)) continue;
            return true;
        }
        return false;
    }

    @Override
    public JavaTypeInstance getWithoutL01Wildcard() {
        List<JavaTypeInstance> unwildCarded = ListFactory.newList();
        for (JavaTypeInstance type : this.genericTypes) {
            if (type instanceof JavaWildcardTypeInstance) {
                type = ((JavaWildcardTypeInstance)type).getWithoutL01Wildcard();
            }
            unwildCarded.add(type);
        }
        return new JavaGenericRefTypeInstance(this.typeInstance, unwildCarded);
    }

    @Override
    public JavaGenericRefTypeInstance getBoundInstance(GenericTypeBinder genericTypeBinder) {
        if (genericTypeBinder == null) {
            return this;
        }
        List<JavaTypeInstance> res = ListFactory.newList();
        for (JavaTypeInstance genericType : this.genericTypes) {
            res.add(genericTypeBinder.getBindingFor(genericType));
        }
        return new JavaGenericRefTypeInstance(this.typeInstance, res);
    }

    @Override
    public boolean tryFindBinding(JavaTypeInstance other, GenericTypeBinder target) {
        boolean res = false;
        if (other instanceof JavaGenericRefTypeInstance) {
            JavaGenericRefTypeInstance otherJavaGenericRef = (JavaGenericRefTypeInstance)other;
            if (this.genericTypes.size() == otherJavaGenericRef.genericTypes.size()) {
                for (int x = 0; x < this.genericTypes.size(); ++x) {
                    JavaTypeInstance genericType = this.genericTypes.get(x);
                    if (!(genericType instanceof JavaGenericBaseInstance)) continue;
                    JavaGenericBaseInstance genericBaseInstance = (JavaGenericBaseInstance)genericType;
                    res |= genericBaseInstance.tryFindBinding(otherJavaGenericRef.genericTypes.get(x), target);
                }
            }
        }
        return res;
    }

    @Override
    public StackType getStackType() {
        return StackType.REF;
    }

    @Override
    public void dumpInto(Dumper d, TypeUsageInformation typeUsageInformation, TypeContext typeContext) {
        d.dump(this.typeInstance).print('<');
        boolean first = true;
        for (JavaTypeInstance type : this.genericTypes) {
            first = StringUtils.comma(first, d);
            d.dump(type);
        }
        d.print('>');
    }

    public String toString() {
        return new ToStringDumper().dump(this).toString();
    }

    @Override
    public JavaTypeInstance getArrayStrippedType() {
        return this;
    }

    @Override
    public List<JavaTypeInstance> getGenericTypes() {
        return this.genericTypes;
    }

    @Override
    public JavaRefTypeInstance getDeGenerifiedType() {
        return this.typeInstance;
    }

    @Override
    public int getNumArrayDimensions() {
        return 0;
    }

    public int hashCode() {
        return 31 + this.typeInstance.hashCode();
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
        return this.typeInstance.getInnerClassHereInfo();
    }

    public JavaTypeInstance getTypeInstance() {
        return this.typeInstance;
    }

    @Override
    public BindingSuperContainer getBindingSupers() {
        return this.typeInstance.getBindingSupers();
    }

    public boolean equals(Object o) {
        return this.equivalentUnder(o, DefaultEquivalenceConstraint.INSTANCE);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof JavaGenericRefTypeInstance)) {
            return false;
        }
        JavaGenericRefTypeInstance other = (JavaGenericRefTypeInstance)o;
        if (!constraint.equivalent(this.typeInstance, other.typeInstance)) {
            return false;
        }
        return constraint.equivalent(this.genericTypes, other.genericTypes);
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
    public RawJavaType getRawTypeOfSimpleType() {
        return RawJavaType.REF;
    }

    @Override
    public boolean implicitlyCastsTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        if (other == TypeConstants.OBJECT) {
            return true;
        }
        if (this.equivalentUnder(other, WILDCARD_CONSTRAINT)) {
            return true;
        }
        BindingSuperContainer bindingSuperContainer = this.getBindingSupers();
        if (bindingSuperContainer == null) {
            return false;
        }
        JavaTypeInstance degenerifiedOther = other.getDeGenerifiedType();
        JavaRefTypeInstance degenerifiedThis = this.getDeGenerifiedType();
        if (((Object)degenerifiedThis).equals(other)) {
            return true;
        }
        if (!bindingSuperContainer.containsBase(degenerifiedOther)) {
            return false;
        }
        JavaGenericRefTypeInstance boundBase = bindingSuperContainer.getBoundSuperForBase(degenerifiedOther);
        if (other.equals(boundBase)) {
            return true;
        }
        if (degenerifiedOther.equals(other)) {
            return true;
        }
        if (gtb != null) {
            JavaTypeInstance reboundBase = gtb.getBindingFor(boundBase);
            if (other.equals(reboundBase)) {
                return true;
            }
            JavaTypeInstance reboundOther = gtb.getBindingFor(other);
            if (this.equivalentUnder(reboundOther, WILDCARD_CONSTRAINT)) {
                return true;
            }
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
        return this.typeInstance.suggestVarName();
    }

    @Override
    public JavaGenericRefTypeInstance asGenericRefInstance(JavaTypeInstance other) {
        return other == this.getDeGenerifiedType() ? this : null;
    }

    @Override
    public JavaTypeInstance directImplOf(JavaTypeInstance other) {
        return other == this.getDeGenerifiedType() ? this : null;
    }

    @Override
    public JavaTypeInstance deObfuscate(ObfuscationTypeMap obfuscationTypeMap) {
        JavaTypeInstance t = obfuscationTypeMap.get(this.typeInstance);
        List<JavaTypeInstance> gs = Functional.map(this.genericTypes, obfuscationTypeMap.getter());
        return new JavaGenericRefTypeInstance(t, gs);
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    public static class WildcardConstraint
    extends DefaultEquivalenceConstraint {
        @Override
        public boolean equivalent(Object o1, Object o2) {
            if (o2 instanceof JavaGenericPlaceholderTypeInstance && ((JavaGenericPlaceholderTypeInstance)o2).getRawName().equals("?")) {
                return true;
            }
            return super.equivalent(o1, o2);
        }
    }

    private class Annotated
    implements JavaAnnotatedTypeInstance {
        JavaAnnotatedTypeInstance typeAnnotated;
        List<JavaAnnotatedTypeInstance> genericTypeAnnotated;

        private Annotated(JavaAnnotatedTypeInstance typeAnnotated, List<JavaAnnotatedTypeInstance> genericTypeAnnotated) {
            this.typeAnnotated = typeAnnotated;
            this.genericTypeAnnotated = genericTypeAnnotated;
        }

        @Override
        public JavaAnnotatedTypeIterator pathIterator() {
            return new Iterator();
        }

        @Override
        public Dumper dump(Dumper d) {
            this.typeAnnotated.dump(d).print('<');
            boolean first = true;
            for (JavaAnnotatedTypeInstance type : this.genericTypeAnnotated) {
                first = StringUtils.comma(first, d);
                type.dump(d);
            }
            d.print('>');
            return d;
        }

        private class Iterator
        extends JavaAnnotatedTypeIterator.BaseAnnotatedTypeIterator {
            private Iterator() {
            }

            @Override
            public JavaAnnotatedTypeIterator moveArray(DecompilerComments comments) {
                return Annotated.this.typeAnnotated.pathIterator().moveArray(comments);
            }

            @Override
            public JavaAnnotatedTypeIterator moveBound(DecompilerComments comments) {
                return Annotated.this.typeAnnotated.pathIterator().moveBound(comments);
            }

            @Override
            public JavaAnnotatedTypeIterator moveNested(DecompilerComments comments) {
                return Annotated.this.typeAnnotated.pathIterator().moveNested(comments);
            }

            @Override
            public JavaAnnotatedTypeIterator moveParameterized(int index, DecompilerComments comments) {
                return Annotated.this.genericTypeAnnotated.get(index).pathIterator();
            }

            @Override
            public void apply(AnnotationTableEntry entry) {
                Annotated.this.typeAnnotated.pathIterator().apply(entry);
            }
        }
    }
}

