/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;

public class BindingSuperContainer {
    static BindingSuperContainer POISON = new BindingSuperContainer(null, null, null);
    private final ClassFile thisClass;
    private final Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> boundSuperClasses;
    private final Map<JavaRefTypeInstance, Route> boundSuperRoute;

    public BindingSuperContainer(ClassFile thisClass, Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> boundSuperClasses, Map<JavaRefTypeInstance, Route> boundSuperRoute) {
        this.thisClass = thisClass;
        this.boundSuperClasses = boundSuperClasses;
        this.boundSuperRoute = boundSuperRoute;
    }

    public static BindingSuperContainer unknownThrowable(JavaRefTypeInstance refType) {
        Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> supers = MapFactory.newMap();
        supers.put(TypeConstants.THROWABLE, null);
        supers.put(refType, null);
        Map<JavaRefTypeInstance, Route> routes = MapFactory.newMap();
        routes.put(TypeConstants.THROWABLE, Route.EXTENSION);
        routes.put(refType, Route.IDENTITY);
        return new BindingSuperContainer(null, supers, routes);
    }

    public JavaGenericRefTypeInstance getBoundAssignable(JavaGenericRefTypeInstance assignable, JavaGenericRefTypeInstance superType) {
        JavaRefTypeInstance baseKey = superType.getDeGenerifiedType();
        JavaGenericRefTypeInstance reboundBase = this.boundSuperClasses.get(baseKey);
        if (reboundBase == null) {
            return assignable;
        }
        GenericTypeBinder genericTypeBinder = GenericTypeBinder.extractBindings(reboundBase, superType);
        JavaGenericRefTypeInstance boundAssignable = assignable.getBoundInstance(genericTypeBinder);
        return boundAssignable;
    }

    public boolean containsBase(JavaTypeInstance possBase) {
        if (!(possBase instanceof JavaRefTypeInstance)) {
            return false;
        }
        return this.boundSuperClasses.containsKey(possBase);
    }

    public Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> getBoundSuperClasses() {
        return this.boundSuperClasses;
    }

    public JavaTypeInstance getMostLikelyAnonymousType(JavaTypeInstance original) {
        JavaTypeInstance generic;
        List<JavaRefTypeInstance> orderedTypes = ListFactory.newList(this.boundSuperClasses.keySet());
        if (orderedTypes.isEmpty() || orderedTypes.size() == 1) {
            return original;
        }
        JavaRefTypeInstance candidate = orderedTypes.get(1);
        if (candidate.equals(TypeConstants.OBJECT)) {
            if (orderedTypes.size() >= 3) {
                candidate = orderedTypes.get(2);
            } else {
                return original;
            }
        }
        if ((generic = (JavaTypeInstance)this.boundSuperClasses.get(candidate)) == null) {
            return candidate;
        }
        return generic;
    }

    public JavaGenericRefTypeInstance getBoundSuperForBase(JavaTypeInstance possBase) {
        if (!(possBase instanceof JavaRefTypeInstance)) {
            return null;
        }
        return this.boundSuperClasses.get(possBase);
    }

    public Map<JavaRefTypeInstance, Route> getBoundSuperRoute() {
        return this.boundSuperRoute;
    }

    public static enum Route {
        IDENTITY,
        EXTENSION,
        INTERFACE;

    }
}

