/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.types.JavaIntersectionTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;

public class FormalTypeParameter
implements Dumpable,
TypeUsageCollectable {
    private String name;
    private JavaTypeInstance classBound;
    private JavaTypeInstance interfaceBound;

    public FormalTypeParameter(String name, JavaTypeInstance classBound, JavaTypeInstance interfaceBound) {
        this.name = name;
        this.classBound = classBound;
        this.interfaceBound = interfaceBound;
    }

    public static Map<String, FormalTypeParameter> getMap(List<FormalTypeParameter> formalTypeParameters) {
        Map<String, FormalTypeParameter> res = MapFactory.newMap();
        if (formalTypeParameters != null) {
            for (FormalTypeParameter p : formalTypeParameters) {
                res.put(p.getName(), p);
            }
        }
        return res;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.classBound);
        collector.collect(this.interfaceBound);
    }

    public void add(FormalTypeParameter other) {
        JavaTypeInstance typ = this.getBound();
        JavaTypeInstance otherTyp = other.getBound();
        typ = typ instanceof JavaIntersectionTypeInstance ? ((JavaIntersectionTypeInstance)typ).withPart(otherTyp) : new JavaIntersectionTypeInstance(ListFactory.newList(typ, otherTyp));
        if (this.classBound != null) {
            this.classBound = typ;
        } else {
            this.interfaceBound = typ;
        }
    }

    public JavaTypeInstance getBound() {
        return this.classBound == null ? this.interfaceBound : this.classBound;
    }

    @Override
    public Dumper dump(Dumper d) {
        JavaTypeInstance dispInterface = this.getBound();
        d.print(this.name);
        if (dispInterface != null && !"java.lang.Object".equals(dispInterface.getRawName())) {
            d.print(" extends ").dump(dispInterface);
        }
        return d;
    }

    public Dumper dump(Dumper d, List<AnnotationTableTypeEntry> typeAnnotations, List<AnnotationTableTypeEntry> typeBoundAnnotations) {
        JavaTypeInstance dispInterface = this.getBound();
        if (!typeAnnotations.isEmpty()) {
            typeAnnotations.get(0).dump(d);
            d.print(' ');
        }
        d.print(this.name);
        if (dispInterface != null) {
            JavaAnnotatedTypeInstance ati = dispInterface.getAnnotatedInstance();
            DecompilerComments comments = new DecompilerComments();
            TypeAnnotationHelper.apply(ati, typeBoundAnnotations, comments);
            d.dump(comments);
            if (!"java.lang.Object".equals(dispInterface.getRawName())) {
                d.print(" extends ").dump(ati);
            }
        }
        return d;
    }

    public String toString() {
        return this.name + " [ " + this.classBound + "|" + this.interfaceBound + "]";
    }
}

