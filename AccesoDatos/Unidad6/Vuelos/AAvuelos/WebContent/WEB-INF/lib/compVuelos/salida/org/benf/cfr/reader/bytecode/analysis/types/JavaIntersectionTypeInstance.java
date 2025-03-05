/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.state.ObfuscationTypeMap;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

public class JavaIntersectionTypeInstance
implements JavaTypeInstance {
    private final List<JavaTypeInstance> parts;
    private final int id;
    private static final AtomicInteger sid = new AtomicInteger();

    public JavaIntersectionTypeInstance(List<JavaTypeInstance> parts) {
        this.parts = parts;
        this.id = sid.getAndIncrement();
    }

    JavaIntersectionTypeInstance withPart(JavaTypeInstance part) {
        List<JavaTypeInstance> newParts = ListFactory.newList(this.parts);
        newParts.add(part);
        return new JavaIntersectionTypeInstance(newParts);
    }

    @Override
    public JavaAnnotatedTypeInstance getAnnotatedInstance() {
        return null;
    }

    @Override
    public StackType getStackType() {
        return StackType.REF;
    }

    @Override
    public boolean isComplexType() {
        return false;
    }

    @Override
    public boolean isUsableType() {
        return false;
    }

    @Override
    public RawJavaType getRawTypeOfSimpleType() {
        return RawJavaType.REF;
    }

    @Override
    public JavaTypeInstance removeAnArrayIndirection() {
        return this;
    }

    @Override
    public JavaTypeInstance getArrayStrippedType() {
        return this;
    }

    @Override
    public JavaTypeInstance getDeGenerifiedType() {
        return this;
    }

    @Override
    public int getNumArrayDimensions() {
        return 0;
    }

    @Override
    public String getRawName() {
        return "<intersection#" + this.id + ">";
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
    public boolean implicitlyCastsTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        for (JavaTypeInstance t : this.parts) {
            if (!t.implicitlyCastsTo(other, gtb)) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean impreciseCanCastTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        for (JavaTypeInstance t : this.parts) {
            if (!t.impreciseCanCastTo(other, gtb)) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean correctCanCastTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        for (JavaTypeInstance t : this.parts) {
            if (!t.correctCanCastTo(other, gtb)) continue;
            return true;
        }
        return false;
    }

    @Override
    public String suggestVarName() {
        return "intersect";
    }

    @Override
    public void dumpInto(Dumper d, TypeUsageInformation typeUsageInformation, TypeContext typeContext) {
        boolean first = true;
        for (JavaTypeInstance t : this.parts) {
            if (!first) {
                d.print(" & ");
            }
            first = false;
            d.dump(t);
        }
    }

    @Override
    public void collectInto(TypeUsageCollector typeUsageCollector) {
        for (JavaTypeInstance t : this.parts) {
            t.collectInto(typeUsageCollector);
        }
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    @Override
    public JavaGenericRefTypeInstance asGenericRefInstance(JavaTypeInstance other) {
        JavaTypeInstance degenerifiedOther = other.getDeGenerifiedType();
        for (JavaTypeInstance part : this.parts) {
            if (!part.getDeGenerifiedType().equals(degenerifiedOther)) continue;
            return part.asGenericRefInstance(other);
        }
        return null;
    }

    @Override
    public JavaTypeInstance deObfuscate(ObfuscationTypeMap obfuscationTypeMap) {
        return new JavaIntersectionTypeInstance(Functional.map(this.parts, obfuscationTypeMap.getter()));
    }

    @Override
    public JavaTypeInstance directImplOf(JavaTypeInstance other) {
        for (JavaTypeInstance part : this.parts) {
            JavaTypeInstance res = part.directImplOf(other);
            if (res == null) continue;
            return res;
        }
        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (JavaTypeInstance t : this.parts) {
            if (!first) {
                sb.append(" & ");
            }
            first = false;
            sb.append(t);
        }
        return sb.toString();
    }
}

