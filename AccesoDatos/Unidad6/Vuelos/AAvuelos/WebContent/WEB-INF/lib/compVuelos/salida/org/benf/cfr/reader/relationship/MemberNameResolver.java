/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.relationship;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.collections.StackFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class MemberNameResolver {
    private final DCCommonState dcCommonState;
    private final transient UnaryFunction<ClassFile, Set<ClassFile>> mapFactory = new UnaryFunction<ClassFile, Set<ClassFile>>(){

        @Override
        public Set<ClassFile> invoke(ClassFile arg) {
            return SetFactory.newOrderedSet();
        }
    };
    private final Map<ClassFile, Set<ClassFile>> childToParent = MapFactory.newLazyMap(this.mapFactory);
    private final Map<ClassFile, Set<ClassFile>> parentToChild = MapFactory.newLazyMap(this.mapFactory);
    private final Map<ClassFile, MemberInfo> infoMap = MapFactory.newIdentityMap();

    public static void resolveNames(DCCommonState dcCommonState, Collection<? extends JavaTypeInstance> types) {
        MemberNameResolver self = new MemberNameResolver(dcCommonState);
        self.initialise(types);
        self.resolve();
    }

    public static boolean verifySingleClassNames(ClassFile oneClassFile) {
        MemberInfo memberInfo = new MemberInfo(oneClassFile);
        for (Method method : oneClassFile.getMethods()) {
            if (method.hiddenState() != Method.Visibility.Visible || method.testAccessFlag(AccessFlagMethod.ACC_BRIDGE) || method.testAccessFlag(AccessFlagMethod.ACC_SYNTHETIC)) continue;
            memberInfo.add(method);
        }
        return memberInfo.hasClashes();
    }

    private MemberNameResolver(DCCommonState dcCommonState) {
        this.dcCommonState = dcCommonState;
    }

    private ClassFile classFileOrNull(JavaTypeInstance type) {
        try {
            return this.dcCommonState.getClassFile(type);
        }
        catch (CannotLoadClassException e) {
            return null;
        }
    }

    private void initialise(Collection<? extends JavaTypeInstance> types) {
        List<ClassFile> classFiles = ListFactory.newList();
        for (JavaTypeInstance javaTypeInstance : types) {
            try {
                classFiles.add(this.dcCommonState.getClassFile(javaTypeInstance));
            }
            catch (CannotLoadClassException cannotLoadClassException) {}
        }
        for (ClassFile classFile : classFiles) {
            JavaTypeInstance superClass;
            ClassSignature signature = classFile.getClassSignature();
            if (signature == null || (superClass = signature.getSuperClass()) == null) continue;
            ClassFile base = this.classFileOrNull(superClass);
            if (base != null) {
                this.childToParent.get(classFile).add(base);
                this.parentToChild.get(base).add(classFile);
            }
            for (JavaTypeInstance interfac : signature.getInterfaces()) {
                ClassFile iface = this.classFileOrNull(interfac);
                if (iface == null) continue;
                this.childToParent.get(classFile).add(iface);
                this.parentToChild.get(iface).add(classFile);
            }
        }
        for (ClassFile classFile : classFiles) {
            MemberInfo memberInfo = new MemberInfo(classFile);
            for (Method method : classFile.getMethods()) {
                memberInfo.add(method);
            }
            this.infoMap.put(classFile, memberInfo);
        }
    }

    private void resolve() {
        List<ClassFile> roots = SetUtil.differenceAtakeBtoList(this.parentToChild.keySet(), this.childToParent.keySet());
        for (ClassFile root : roots) {
            this.checkBadNames(root);
        }
        this.insertParentClashes();
        for (ClassFile root : roots) {
            this.rePushBadNames(root);
        }
        this.patchBadNames();
    }

    private void patchBadNames() {
        Collection<MemberInfo> memberInfos = this.infoMap.values();
        for (MemberInfo memberInfo : memberInfos) {
            if (!memberInfo.hasClashes()) continue;
            Set<MethodKey> clashes = memberInfo.getClashes();
            for (MethodKey clashKey : clashes) {
                Map<JavaTypeInstance, Collection<Method>> clashMap = memberInfo.getClashedMethodsFor(clashKey);
                for (Map.Entry<JavaTypeInstance, Collection<Method>> clashByType : clashMap.entrySet()) {
                    String resolvedName = null;
                    for (Method method : clashByType.getValue()) {
                        MethodPrototype methodPrototype = method.getMethodPrototype();
                        if (methodPrototype.hasNameBeenFixed()) {
                            if (resolvedName != null) continue;
                            resolvedName = methodPrototype.getFixedName();
                            continue;
                        }
                        if (resolvedName == null) {
                            resolvedName = ClassNameUtils.getTypeFixPrefix(clashByType.getKey()) + methodPrototype.getName();
                        }
                        methodPrototype.setFixedName(resolvedName);
                    }
                }
            }
        }
    }

    private void insertParentClashes() {
        for (MemberInfo memberInfo : this.infoMap.values()) {
            if (!memberInfo.hasClashes()) continue;
            Set<MethodKey> clashes = memberInfo.getClashes();
            for (MethodKey clash : clashes) {
                for (Collection<Method> methodList : memberInfo.getClashedMethodsFor(clash).values()) {
                    for (Method method : methodList) {
                        this.infoMap.get(method.getClassFile()).addClash(clash);
                    }
                }
            }
        }
    }

    private void rePushBadNames(ClassFile c) {
        Stack<ClassFile> parents = StackFactory.newStack();
        Set<MethodKey> clashes = SetFactory.newSet();
        this.rePushBadNames(c, clashes, parents);
    }

    private void rePushBadNames(ClassFile c, Set<MethodKey> clashes, Stack<ClassFile> parents) {
        MemberInfo memberInfo = this.infoMap.get(c);
        if (memberInfo != null) {
            memberInfo.addClashes(clashes);
            if (!memberInfo.getClashes().isEmpty()) {
                clashes = SetFactory.newSet(clashes);
                clashes.addAll(memberInfo.getClashes());
            }
        }
        parents.push(c);
        for (ClassFile child : this.parentToChild.get(c)) {
            this.rePushBadNames(child, clashes, parents);
        }
        parents.pop();
    }

    private void checkBadNames(ClassFile c) {
        Stack<ClassFile> parents = StackFactory.newStack();
        MemberInfo base = new MemberInfo(null);
        this.checkBadNames(c, base, parents);
    }

    private void checkBadNames(ClassFile c, MemberInfo inherited, Stack<ClassFile> parents) {
        MemberInfo memberInfo = this.infoMap.get(c);
        if (memberInfo == null) {
            memberInfo = inherited;
        } else {
            memberInfo.inheritFrom(inherited);
        }
        parents.push(c);
        for (ClassFile child : this.parentToChild.get(c)) {
            this.checkBadNames(child, memberInfo, parents);
        }
        parents.pop();
    }

    private static class MethodKey {
        private final String name;
        private final List<JavaTypeInstance> args;

        private MethodKey(String name, List<JavaTypeInstance> args) {
            this.name = name;
            this.args = args;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            MethodKey methodKey = (MethodKey)o;
            if (!this.args.equals(methodKey.args)) {
                return false;
            }
            return this.name.equals(methodKey.name);
        }

        public int hashCode() {
            int result = this.name.hashCode();
            result = 31 * result + this.args.hashCode();
            return result;
        }

        public String toString() {
            return "MethodKey{name='" + this.name + '\'' + ", args=" + this.args + '}';
        }
    }

    private static class MemberInfo {
        private final ClassFile classFile;
        private final Map<MethodKey, Map<JavaTypeInstance, Collection<Method>>> knownMethods = MapFactory.newLazyMap(new UnaryFunction<MethodKey, Map<JavaTypeInstance, Collection<Method>>>(){

            @Override
            public Map<JavaTypeInstance, Collection<Method>> invoke(MethodKey arg) {
                return MapFactory.newLazyMap(new UnaryFunction<JavaTypeInstance, Collection<Method>>(){

                    @Override
                    public Collection<Method> invoke(JavaTypeInstance arg) {
                        return SetFactory.newOrderedSet();
                    }
                });
            }
        });
        private final Set<MethodKey> clashes = SetFactory.newSet();

        private MemberInfo(ClassFile classFile) {
            this.classFile = classFile;
        }

        public void add(Method method) {
            if (method.isConstructor()) {
                return;
            }
            MethodPrototype prototype = method.getMethodPrototype();
            String name = prototype.getName();
            List<JavaTypeInstance> args = Functional.map(prototype.getArgs(), new UnaryFunction<JavaTypeInstance, JavaTypeInstance>(){

                @Override
                public JavaTypeInstance invoke(JavaTypeInstance arg) {
                    return arg.getDeGenerifiedType();
                }
            });
            MethodKey methodKey = new MethodKey(name, args);
            JavaTypeInstance type = prototype.getReturnType();
            if (type instanceof JavaGenericBaseInstance) {
                return;
            }
            this.add(methodKey, prototype.getReturnType(), method, false);
        }

        private void add(MethodKey key1, JavaTypeInstance key2, Method method, boolean fromParent) {
            JavaTypeInstance existing;
            BindingSuperContainer supers;
            Map<JavaTypeInstance, Collection<Method>> methods = this.knownMethods.get(key1);
            if (method.hiddenState() != Method.Visibility.Visible) {
                return;
            }
            if (fromParent && !methods.containsKey(key2) && !methods.isEmpty() && methods.keySet().size() == 1 && (supers = (existing = methods.keySet().iterator().next()).getBindingSupers()) != null && supers.containsBase(key2)) {
                key2 = existing;
            }
            methods.get(key2).add(method);
            if (methods.size() > 1) {
                this.clashes.add(key1);
            }
        }

        boolean hasClashes() {
            return !this.clashes.isEmpty();
        }

        Set<MethodKey> getClashes() {
            return this.clashes;
        }

        void addClashes(Set<MethodKey> newClashes) {
            this.clashes.addAll(newClashes);
        }

        void addClash(MethodKey clash) {
            this.clashes.add(clash);
        }

        Map<JavaTypeInstance, Collection<Method>> getClashedMethodsFor(MethodKey key) {
            return this.knownMethods.get(key);
        }

        void inheritFrom(MemberInfo base) {
            for (Map.Entry<MethodKey, Map<JavaTypeInstance, Collection<Method>>> entry : base.knownMethods.entrySet()) {
                MethodKey key = entry.getKey();
                for (Map.Entry<JavaTypeInstance, Collection<Method>> entry2 : entry.getValue().entrySet()) {
                    JavaTypeInstance returnType = entry2.getKey();
                    Collection<Method> methods = entry2.getValue();
                    for (Method method : methods) {
                        if (!method.isVisibleTo(this.classFile.getRefClassType())) continue;
                        this.add(key, returnType, method, true);
                    }
                }
            }
        }

        public String toString() {
            return "" + this.classFile;
        }
    }
}

