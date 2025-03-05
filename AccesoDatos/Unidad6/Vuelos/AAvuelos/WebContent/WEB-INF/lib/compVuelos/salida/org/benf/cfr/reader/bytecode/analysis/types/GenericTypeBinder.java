/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.FormalTypeParameter;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaWildcardTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class GenericTypeBinder {
    private final Map<String, JavaTypeInstance> nameToBoundType;

    private GenericTypeBinder(Map<String, JavaTypeInstance> nameToBoundType) {
        this.nameToBoundType = nameToBoundType;
    }

    public static GenericTypeBinder createEmpty() {
        return new GenericTypeBinder(MapFactory.<String, JavaTypeInstance>newMap());
    }

    public static GenericTypeBinder create(List<FormalTypeParameter> ... ftps) {
        Map<String, JavaTypeInstance> bounds = MapFactory.newMap();
        for (List<FormalTypeParameter> ftp : ftps) {
            if (ftp == null) continue;
            for (FormalTypeParameter f : ftp) {
                bounds.put(f.getName(), f.getBound());
            }
        }
        return new GenericTypeBinder(bounds);
    }

    static GenericTypeBinder bind(List<FormalTypeParameter> methodFormalTypeParameters, ClassSignature classSignature, List<JavaTypeInstance> args, JavaGenericRefTypeInstance boundInstance, List<JavaTypeInstance> boundArgs) {
        int x;
        Map<String, JavaTypeInstance> nameToBoundType = MapFactory.newMap();
        if (boundInstance != null) {
            List<FormalTypeParameter> unboundParameters = classSignature.getFormalTypeParameters();
            List<JavaTypeInstance> boundParameters = boundInstance.getGenericTypes();
            if (unboundParameters == null || boundParameters.size() != unboundParameters.size()) {
                return null;
            }
            for (x = 0; x < boundParameters.size(); ++x) {
                nameToBoundType.put(unboundParameters.get(x).getName(), boundParameters.get(x));
            }
        }
        List<FormalTypeParameter> classFormalTypeParamters = classSignature.getFormalTypeParameters();
        GenericTypeBinder res = new GenericTypeBinder(nameToBoundType);
        if (methodFormalTypeParameters != null && !methodFormalTypeParameters.isEmpty() || classFormalTypeParamters != null && !classFormalTypeParamters.isEmpty()) {
            if (args.size() != boundArgs.size()) {
                throw new IllegalArgumentException();
            }
            for (x = 0; x < args.size(); ++x) {
                JavaGenericRefTypeInstance boundAppropriate;
                BindingSuperContainer bindingSupers;
                JavaTypeInstance unbound = args.get(x);
                JavaTypeInstance bound = boundArgs.get(x);
                if (bound.getDeGenerifiedType() != unbound.getDeGenerifiedType() && unbound instanceof JavaGenericRefTypeInstance && ((JavaGenericRefTypeInstance)unbound).hasUnbound() && (bindingSupers = bound.getBindingSupers()) != null && (boundAppropriate = bindingSupers.getBoundSuperForBase(unbound.getDeGenerifiedType())) != null && !boundAppropriate.hasUnbound()) {
                    bound = boundAppropriate;
                }
                if (unbound instanceof JavaArrayTypeInstance && bound instanceof JavaArrayTypeInstance && unbound.getNumArrayDimensions() == bound.getNumArrayDimensions()) {
                    unbound = unbound.getArrayStrippedType();
                    bound = bound.getArrayStrippedType();
                }
                if (!(unbound instanceof JavaGenericBaseInstance)) continue;
                JavaGenericBaseInstance unboundGeneric = (JavaGenericBaseInstance)unbound;
                unboundGeneric.tryFindBinding(bound, res);
            }
        }
        return res;
    }

    public static GenericTypeBinder buildIdentityBindings(JavaGenericRefTypeInstance unbound) {
        List<JavaTypeInstance> typeParameters = unbound.getGenericTypes();
        Map<String, JavaTypeInstance> unboundNames = MapFactory.newMap();
        int len = typeParameters.size();
        for (int x = 0; x < len; ++x) {
            JavaTypeInstance unboundParam = typeParameters.get(x);
            if (!(unboundParam instanceof JavaGenericPlaceholderTypeInstance)) {
                throw new ConfusedCFRException("Unbound parameter expected to be placeholder!");
            }
            unboundNames.put(unboundParam.getRawName(), unboundParam);
        }
        return new GenericTypeBinder(unboundNames);
    }

    public static GenericTypeBinder extractBaseBindings(JavaGenericBaseInstance unbound, JavaTypeInstance maybeBound) {
        if (!(unbound instanceof JavaGenericRefTypeInstance)) {
            return GenericTypeBinder.extractBindings(unbound, maybeBound);
        }
        if (!(maybeBound instanceof JavaGenericRefTypeInstance)) {
            return GenericTypeBinder.extractBindings(unbound, maybeBound);
        }
        JavaGenericRefTypeInstance unboundGeneric = (JavaGenericRefTypeInstance)unbound;
        JavaGenericRefTypeInstance maybeBoundGeneric = (JavaGenericRefTypeInstance)maybeBound;
        BindingSuperContainer maybeBindingContainer = maybeBound.getBindingSupers();
        JavaGenericRefTypeInstance boundAssignable = maybeBindingContainer.getBoundAssignable(maybeBoundGeneric, unboundGeneric);
        GenericTypeBinder binder = GenericTypeBinder.extractBindings(unboundGeneric, boundAssignable);
        return binder;
    }

    public static GenericTypeBinder extractBindings(JavaGenericBaseInstance unbound, JavaTypeInstance maybeBound) {
        Map<String, JavaTypeInstance> boundNames = MapFactory.newMap();
        GenericTypeBinder.doBind(boundNames, unbound, maybeBound);
        return new GenericTypeBinder(boundNames);
    }

    private static void doBind(Map<String, JavaTypeInstance> boundNames, JavaGenericBaseInstance unbound, JavaTypeInstance maybeBound) {
        if (unbound.getClass() == JavaGenericPlaceholderTypeInstance.class) {
            JavaGenericPlaceholderTypeInstance placeholder = (JavaGenericPlaceholderTypeInstance)unbound;
            boundNames.put(placeholder.getRawName(), maybeBound);
            return;
        }
        List<JavaTypeInstance> typeParameters = unbound.getGenericTypes();
        if (!(maybeBound instanceof JavaGenericBaseInstance)) {
            return;
        }
        JavaGenericBaseInstance bound = (JavaGenericBaseInstance)maybeBound;
        List<JavaTypeInstance> boundTypeParameters = bound.getGenericTypes();
        if (typeParameters.size() != boundTypeParameters.size()) {
            return;
        }
        int len = typeParameters.size();
        for (int x = 0; x < len; ++x) {
            JavaTypeInstance unboundParam = typeParameters.get(x);
            JavaTypeInstance boundParam = boundTypeParameters.get(x);
            if (!(unboundParam instanceof JavaGenericBaseInstance)) continue;
            GenericTypeBinder.doBind(boundNames, (JavaGenericBaseInstance)unboundParam, boundParam);
        }
    }

    public void removeBinding(JavaGenericPlaceholderTypeInstance type) {
        String name = type.getRawName();
        this.nameToBoundType.remove(name);
    }

    JavaTypeInstance getBindingFor(FormalTypeParameter formalTypeParameter) {
        return this.nameToBoundType.get(formalTypeParameter.getName());
    }

    public JavaTypeInstance getBindingFor(JavaTypeInstance maybeUnbound) {
        if (maybeUnbound instanceof JavaGenericPlaceholderTypeInstance) {
            JavaGenericPlaceholderTypeInstance placeholder = (JavaGenericPlaceholderTypeInstance)maybeUnbound;
            String name = placeholder.getRawName();
            JavaTypeInstance bound = this.nameToBoundType.get(name);
            if (bound != null) {
                return bound;
            }
        } else {
            JavaTypeInstance bindingFor;
            JavaArrayTypeInstance ja;
            JavaTypeInstance jaStripped;
            if (maybeUnbound instanceof JavaGenericBaseInstance) {
                return ((JavaGenericBaseInstance)maybeUnbound).getBoundInstance(this);
            }
            if (maybeUnbound instanceof JavaArrayTypeInstance && !(jaStripped = (ja = (JavaArrayTypeInstance)maybeUnbound).getArrayStrippedType()).equals(bindingFor = this.getBindingFor(jaStripped))) {
                return new JavaArrayTypeInstance(ja.getNumArrayDimensions(), bindingFor);
            }
        }
        return maybeUnbound;
    }

    private static boolean isBetterBinding(JavaTypeInstance isBetter, JavaTypeInstance than) {
        if (than == null) {
            return true;
        }
        return !(isBetter instanceof JavaGenericPlaceholderTypeInstance);
    }

    public void suggestOnlyNullBinding(JavaGenericPlaceholderTypeInstance type) {
        String name = type.getRawName();
        if (this.nameToBoundType.containsKey(name)) {
            return;
        }
        this.nameToBoundType.put(name, TypeConstants.OBJECT);
    }

    public void suggestBindingFor(String name, JavaTypeInstance binding) {
        JavaTypeInstance alreadyBound = this.nameToBoundType.get(name);
        if (GenericTypeBinder.isBetterBinding(binding, alreadyBound)) {
            this.nameToBoundType.put(name, binding);
        }
    }

    public GenericTypeBinder mergeWith(GenericTypeBinder other, boolean mergeToCommonClass) {
        Set<String> keys = SetFactory.newSet(this.nameToBoundType.keySet());
        keys.addAll(other.nameToBoundType.keySet());
        Map<String, JavaTypeInstance> res = MapFactory.newMap();
        for (String key : keys) {
            JavaTypeInstance t1 = this.nameToBoundType.get(key);
            JavaTypeInstance t2 = other.nameToBoundType.get(key);
            if (t1 == null) {
                res.put(key, t2);
                continue;
            }
            if (t2 == null) {
                res.put(key, t1);
                continue;
            }
            if (mergeToCommonClass) {
                if (t1.implicitlyCastsTo(t2, other)) {
                    res.put(key, t2);
                    continue;
                }
                if (t2.implicitlyCastsTo(t1, other)) {
                    res.put(key, t1);
                    continue;
                }
                InferredJavaType clash = InferredJavaType.mkClash(t1, t2);
                clash.collapseTypeClash();
                res.put(key, clash.getJavaTypeInstance());
                continue;
            }
            return null;
        }
        return new GenericTypeBinder(res);
    }

    public GenericTypeBinder createAssignmentRhsBindings(GenericTypeBinder rhsBinder) {
        if (!this.nameToBoundType.keySet().equals(rhsBinder.nameToBoundType.keySet())) {
            return null;
        }
        Map<String, JavaTypeInstance> resultMap = MapFactory.newMap();
        for (Map.Entry<String, JavaTypeInstance> entry : this.nameToBoundType.entrySet()) {
            BindingSuperContainer rhsBoundSupers;
            JavaTypeInstance rhsStripped;
            String key = entry.getKey();
            JavaTypeInstance lhstype = entry.getValue();
            JavaTypeInstance rhstype = rhsBinder.nameToBoundType.get(key);
            JavaTypeInstance lhsStripped = lhstype.getDeGenerifiedType();
            if (!(lhsStripped.equals(rhsStripped = rhstype.getDeGenerifiedType()) || rhstype instanceof JavaGenericPlaceholderTypeInstance || (rhsBoundSupers = rhstype.getBindingSupers()) != null && rhsBoundSupers.containsBase(lhstype.getDeGenerifiedType()))) {
                return null;
            }
            JavaTypeInstance bestGuess = lhstype instanceof JavaWildcardTypeInstance ? rhstype : lhstype;
            if (bestGuess instanceof JavaGenericPlaceholderTypeInstance) {
                return null;
            }
            resultMap.put(key, bestGuess);
        }
        return new GenericTypeBinder(resultMap);
    }
}

