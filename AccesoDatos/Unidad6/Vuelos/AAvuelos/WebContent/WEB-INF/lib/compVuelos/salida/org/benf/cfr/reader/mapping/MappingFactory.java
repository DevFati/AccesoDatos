/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.mapping;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.innerclass.InnerClassAttributeInfo;
import org.benf.cfr.reader.mapping.ClassMapping;
import org.benf.cfr.reader.mapping.FieldMapping;
import org.benf.cfr.reader.mapping.Mapping;
import org.benf.cfr.reader.mapping.MethodMapping;
import org.benf.cfr.reader.mapping.NullMapping;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.state.ClassCache;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class MappingFactory {
    private final ClassCache classCache;
    private final Options options;
    private static final Pattern fieldPattern = Pattern.compile("^\\s*(\\d+:\\d+:)?([^ ]+)\\s+(.*) -> (.*)$");
    private static final Pattern methodPattern = Pattern.compile("^\\s*(\\d+:\\d+:)?([^ ]+)\\s+([^(]+)[(](.*)[)] -> (.*)$");
    private static final Pattern classPattern = Pattern.compile("^(.+) -> (.+):$");

    private MappingFactory(Options options, ClassCache classCache) {
        this.options = options;
        this.classCache = classCache;
    }

    public static ObfuscationMapping get(Options options, DCCommonState state) {
        String path = (String)options.getOption(OptionsImpl.OBFUSCATION_PATH);
        if (path == null) {
            return NullMapping.INSTANCE;
        }
        return new MappingFactory(options, state.getClassCache()).createFromPath(path);
    }

    private Mapping createFromPath(String path) {
        List<ClassMapping> classMappings = ListFactory.newList();
        try {
            String line;
            FileInputStream is = new FileInputStream(path);
            BufferedReader isr = new BufferedReader(new InputStreamReader(is));
            ClassMapping currentClassMapping = null;
            while ((line = isr.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                if (line.endsWith(":")) {
                    currentClassMapping = this.parseClassMapping(line);
                    classMappings.add(currentClassMapping);
                    continue;
                }
                if (currentClassMapping == null) {
                    throw new ConfusedCFRException("No class mapping in place - illegal mapping file?");
                }
                if (line.contains(") ")) {
                    currentClassMapping.addMethodMapping(this.parseMethodMapping(line));
                    continue;
                }
                currentClassMapping.addFieldMapping(this.parseFieldMapping(line));
            }
        }
        catch (FileNotFoundException e) {
            throw new ConfusedCFRException(e);
        }
        catch (IOException e) {
            throw new ConfusedCFRException(e);
        }
        Map<JavaRefTypeInstance, JavaRefTypeInstance> parents = MapFactory.newMap();
        Map<JavaTypeInstance, List<InnerClassAttributeInfo>> innerInfo = this.inferInnerClasses(classMappings, parents);
        for (Map.Entry<JavaRefTypeInstance, JavaRefTypeInstance> pc : parents.entrySet()) {
            JavaRefTypeInstance child = pc.getKey();
            JavaRefTypeInstance parent = pc.getValue();
            child.setUnexpectedInnerClassOf(parent);
        }
        return new Mapping(this.options, classMappings, innerInfo);
    }

    private Map<JavaTypeInstance, List<InnerClassAttributeInfo>> inferInnerClasses(List<ClassMapping> classMappings, Map<JavaRefTypeInstance, JavaRefTypeInstance> parents) {
        Map byRealName = MapFactory.newMap();
        for (ClassMapping classMapping : classMappings) {
            String real = classMapping.getRealClass().getRawName();
            byRealName.put(real, classMapping);
        }
        LazyMap<JavaTypeInstance, List<JavaTypeInstance>> children = MapFactory.newLazyMap(new UnaryFunction<JavaTypeInstance, List<JavaTypeInstance>>(){

            @Override
            public List<JavaTypeInstance> invoke(JavaTypeInstance arg) {
                return ListFactory.newList();
            }
        });
        for (ClassMapping classMapping : classMappings) {
            String prefix;
            ClassMapping parent;
            String real = classMapping.getRealClass().getRawName();
            int idx = real.lastIndexOf(36);
            if (idx == -1 || (parent = (ClassMapping)byRealName.get(prefix = real.substring(0, idx))) == null) continue;
            JavaRefTypeInstance parentClass = parent.getObClass();
            JavaRefTypeInstance childClass = classMapping.getObClass();
            parents.put(childClass, parentClass);
            ((List)children.get(parentClass)).add(childClass);
        }
        Map<JavaTypeInstance, List<InnerClassAttributeInfo>> map = MapFactory.newMap();
        Map<JavaTypeInstance, List<InnerClassAttributeInfo>> lazyRes = MapFactory.newLazyMap(map, new UnaryFunction<JavaTypeInstance, List<InnerClassAttributeInfo>>(){

            @Override
            public List<InnerClassAttributeInfo> invoke(JavaTypeInstance arg) {
                return ListFactory.newList();
            }
        });
        for (Map.Entry entry : children.entrySet()) {
            JavaTypeInstance parent = (JavaTypeInstance)entry.getKey();
            List<InnerClassAttributeInfo> parentIac = lazyRes.get(parent);
            for (JavaTypeInstance child : (List)entry.getValue()) {
                InnerClassAttributeInfo iac = new InnerClassAttributeInfo(child, parent, null, Collections.<AccessFlag>emptySet());
                parentIac.add(iac);
                lazyRes.get(child).add(iac);
            }
        }
        return map;
    }

    private FieldMapping parseFieldMapping(String line) {
        Matcher m = fieldPattern.matcher(line);
        if (!m.matches()) {
            throw new ConfusedCFRException("Can't match field: " + line);
        }
        String type = m.group(2);
        String name = m.group(3);
        String rename = m.group(4);
        return new FieldMapping(name, rename, this.getJavaStringTypeInstance(type));
    }

    private MethodMapping parseMethodMapping(String line) {
        List<JavaTypeInstance> argTypes;
        Matcher m = methodPattern.matcher(line);
        if (!m.matches()) {
            throw new ConfusedCFRException("Can't match method: " + line);
        }
        String type = m.group(2);
        String name = m.group(3);
        String args = m.group(4);
        String rename = m.group(5);
        if (args.isEmpty()) {
            argTypes = Collections.emptyList();
        } else {
            argTypes = ListFactory.newList();
            for (String arg : args.split(",")) {
                if ((arg = arg.trim()).isEmpty()) continue;
                argTypes.add(this.getJavaStringTypeInstance(arg));
            }
        }
        JavaTypeInstance result = this.getJavaStringTypeInstance(type);
        return new MethodMapping(name, rename, result, argTypes);
    }

    private JavaTypeInstance getJavaStringTypeInstance(String type) {
        int numarray = 0;
        while (type.endsWith("[]")) {
            type = type.substring(0, type.length() - 2);
            ++numarray;
        }
        JavaTypeInstance result = RawJavaType.getPodNamedType(type);
        if (result == null) {
            result = this.classCache.getRefClassFor(type);
        }
        if (numarray > 0) {
            result = new JavaArrayTypeInstance(numarray, result);
        }
        return result;
    }

    private ClassMapping parseClassMapping(String line) {
        Matcher m = classPattern.matcher(line);
        if (!m.matches()) {
            throw new ConfusedCFRException("Can't match class: " + line);
        }
        return new ClassMapping((JavaRefTypeInstance)this.getJavaStringTypeInstance(m.group(1)), (JavaRefTypeInstance)this.getJavaStringTypeInstance(m.group(2)));
    }
}

