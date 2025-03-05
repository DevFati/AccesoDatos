/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.collections.MapFactory;

public class BoundSuperCollector {
    private final ClassFile classFile;
    private final Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> boundSupers;
    private final Map<JavaRefTypeInstance, BindingSuperContainer.Route> boundSuperRoute;

    public BoundSuperCollector(ClassFile classFile) {
        this.classFile = classFile;
        this.boundSupers = MapFactory.newOrderedMap();
        this.boundSuperRoute = MapFactory.newOrderedMap();
    }

    public BindingSuperContainer getBoundSupers() {
        return new BindingSuperContainer(this.classFile, this.boundSupers, this.boundSuperRoute);
    }

    public void collect(JavaGenericRefTypeInstance boundBase, BindingSuperContainer.Route route) {
        JavaRefTypeInstance key = boundBase.getDeGenerifiedType();
        JavaGenericRefTypeInstance prev = this.boundSupers.put(key, boundBase);
        this.boundSuperRoute.put(key, route);
    }

    public void collect(JavaRefTypeInstance boundBase, BindingSuperContainer.Route route) {
        JavaGenericRefTypeInstance prev = this.boundSupers.put(boundBase, null);
        this.boundSuperRoute.put(boundBase, route);
    }
}

