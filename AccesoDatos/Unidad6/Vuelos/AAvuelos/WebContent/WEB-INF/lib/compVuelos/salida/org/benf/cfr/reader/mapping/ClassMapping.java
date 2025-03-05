/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.mapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.mapping.FieldMapping;
import org.benf.cfr.reader.mapping.Mapping;
import org.benf.cfr.reader.mapping.MethodMapping;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.output.Dumper;

public class ClassMapping {
    private final JavaRefTypeInstance realClass;
    private final JavaRefTypeInstance obClass;
    private final Map<String, Map<MethodData, String>> methodMappings = MapFactory.newMap();
    private final Map<String, FieldMapping> fieldMappings = MapFactory.newMap();

    ClassMapping(JavaRefTypeInstance realClass, JavaRefTypeInstance obClass) {
        this.realClass = realClass;
        this.obClass = obClass;
    }

    void addMethodMapping(MethodMapping m) {
        Map<MethodData, String> byName = this.methodMappings.get(m.getName());
        if (byName == null) {
            byName = MapFactory.newOrderedMap();
            this.methodMappings.put(m.getName(), byName);
        }
        MethodData data = new MethodData(m.getArgTypes());
        byName.put(data, m.getRename());
    }

    void addFieldMapping(FieldMapping f) {
        this.fieldMappings.put(f.getName(), f);
    }

    JavaRefTypeInstance getRealClass() {
        return this.realClass;
    }

    JavaRefTypeInstance getObClass() {
        return this.obClass;
    }

    String getMethodName(String displayName, List<JavaTypeInstance> args, final Mapping mapping, Dumper d) {
        Map<MethodData, String> poss = this.methodMappings.get(displayName);
        if (poss == null) {
            return displayName;
        }
        MethodData md = new MethodData(Functional.map(args, new UnaryFunction<JavaTypeInstance, JavaTypeInstance>(){

            @Override
            public JavaTypeInstance invoke(JavaTypeInstance arg) {
                JavaTypeInstance deGenerifiedType = arg.getDeGenerifiedType();
                return mapping.get(deGenerifiedType);
            }
        }));
        String name = poss.get(md);
        if (name != null) {
            return name;
        }
        for (Map.Entry<MethodData, String> entry : poss.entrySet()) {
            if (entry.getKey().args.size() != md.args.size()) continue;
            if (name != null) {
                if (name.equals(entry.getValue())) continue;
                name = null;
                break;
            }
            name = entry.getValue();
        }
        if (name != null) {
            return name;
        }
        d.addSummaryError(null, "Could not resolve method '" + this.getRealClass().getRawName() + " " + displayName + "'");
        return displayName;
    }

    String getFieldName(String name, JavaTypeInstance type, Dumper d, Mapping mapping, boolean isStatic) {
        String res = this.getFieldNameOrNull(name, type, d, mapping);
        if (res == null && isStatic) {
            res = this.getInterfaceFieldNameOrNull(name, type, d, mapping);
        }
        if (res == null) {
            d.addSummaryError(null, "Could not resolve field '" + name + "'");
            return name;
        }
        return res;
    }

    private String getInterfaceFieldNameOrNull(String name, JavaTypeInstance type, Dumper d, Mapping mapping) {
        if (!(type instanceof JavaRefTypeInstance)) {
            return null;
        }
        BindingSuperContainer bindingSupers = type.getBindingSupers();
        for (Map.Entry<JavaRefTypeInstance, BindingSuperContainer.Route> entry : bindingSupers.getBoundSuperRoute().entrySet()) {
            FieldMapping rename;
            ClassMapping cm;
            if (entry.getValue() != BindingSuperContainer.Route.INTERFACE || (cm = mapping.getClassMapping(entry.getKey().getDeGenerifiedType())) == null || (rename = cm.fieldMappings.get(name)) == null) continue;
            return rename.getRename();
        }
        return null;
    }

    private String getFieldNameOrNull(String name, JavaTypeInstance type, Dumper d, Mapping mapping) {
        FieldMapping f;
        if (name.endsWith(".this")) {
            String preName = name.substring(0, name.length() - ".this".length());
            Set<JavaTypeInstance> parents = SetFactory.newOrderedSet();
            type.getInnerClassHereInfo().collectTransitiveDegenericParents(parents);
            for (JavaTypeInstance parent : parents) {
                JavaRefTypeInstance mappedParent;
                if (!((JavaRefTypeInstance)parent).getRawShortName().equals(preName) || (mappedParent = (JavaRefTypeInstance)mapping.get(parent)) == null) continue;
                return mappedParent.getRawShortName() + ".this";
            }
        }
        if ((f = this.fieldMappings.get(name)) == null) {
            JavaTypeInstance baseType;
            String res;
            ClassFile classFile;
            if (type instanceof JavaRefTypeInstance && (classFile = ((JavaRefTypeInstance)type).getClassFile()) != null && (res = this.getClassFieldNameOrNull(name, d, mapping, baseType = classFile.getBaseClassType().getDeGenerifiedType())) != null) {
                return res;
            }
            return null;
        }
        return f.getRename();
    }

    private String getClassFieldNameOrNull(String name, Dumper d, Mapping mapping, JavaTypeInstance baseType) {
        String res;
        ClassMapping parentCM = mapping.getClassMapping(baseType);
        if (parentCM != null && (res = parentCM.getFieldNameOrNull(name, baseType, d, mapping)) != null) {
            return res;
        }
        return null;
    }

    private static class MethodData {
        List<JavaTypeInstance> args;

        MethodData(List<JavaTypeInstance> argTypes) {
            if (!argTypes.isEmpty()) {
                argTypes = Functional.map(argTypes, new UnaryFunction<JavaTypeInstance, JavaTypeInstance>(){

                    @Override
                    public JavaTypeInstance invoke(JavaTypeInstance arg) {
                        return arg.getDeGenerifiedType();
                    }
                });
            }
            this.args = argTypes;
        }

        public int hashCode() {
            int hash = 0;
            for (JavaTypeInstance a : this.args) {
                hash = 31 * hash + a.hashCode();
            }
            return hash;
        }

        public boolean equals(Object obj) {
            if (obj.getClass() != MethodData.class) {
                return false;
            }
            MethodData other = (MethodData)obj;
            if (other.args.size() != this.args.size()) {
                return false;
            }
            return this.args.equals(other.args);
        }

        public String toString() {
            return "" + this.args.size() + " args : " + this.args;
        }
    }
}

