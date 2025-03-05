/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.classfilehelpers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfoUtils;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.FakeMethod;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.attributes.AttributeMap;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeInvisibleAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleAnnotations;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumper;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.DetectedStaticImport;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.CfrVersionInfo;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;

abstract class AbstractClassFileDumper
implements ClassFileDumper {
    private final DCCommonState dcCommonState;

    static String getAccessFlagsString(Set<AccessFlag> accessFlags, AccessFlag[] dumpableAccessFlags) {
        StringBuilder sb = new StringBuilder();
        for (AccessFlag accessFlag : dumpableAccessFlags) {
            if (!accessFlags.contains((Object)accessFlag)) continue;
            sb.append((Object)accessFlag).append(' ');
        }
        return sb.toString();
    }

    AbstractClassFileDumper(DCCommonState dcCommonState) {
        this.dcCommonState = dcCommonState;
    }

    void dumpTopHeader(ClassFile classFile, Dumper d, boolean showPackage) {
        if (this.dcCommonState == null) {
            return;
        }
        Options options = this.dcCommonState.getOptions();
        String header = "Decompiled with CFR";
        if (((Boolean)options.getOption(OptionsImpl.SHOW_CFR_VERSION)).booleanValue()) {
            header = header + " " + CfrVersionInfo.VERSION_INFO;
        }
        header = header + '.';
        d.beginBlockComment(false);
        d.print(header).newln();
        if (((Boolean)options.getOption(OptionsImpl.DECOMPILER_COMMENTS)).booleanValue()) {
            TypeUsageInformation typeUsageInformation = d.getTypeUsageInformation();
            List<JavaTypeInstance> couldNotLoad = ListFactory.newList();
            for (JavaTypeInstance javaTypeInstance : typeUsageInformation.getUsedClassTypes()) {
                if (!(javaTypeInstance instanceof JavaRefTypeInstance)) continue;
                ClassFile loadedClass = null;
                try {
                    loadedClass = this.dcCommonState.getClassFile(javaTypeInstance);
                }
                catch (CannotLoadClassException cannotLoadClassException) {
                    // empty catch block
                }
                if (loadedClass != null) continue;
                couldNotLoad.add(javaTypeInstance);
            }
            if (!couldNotLoad.isEmpty()) {
                d.newln();
                d.print("Could not load the following classes:").newln();
                for (JavaTypeInstance javaTypeInstance : couldNotLoad) {
                    d.print(" ").print(javaTypeInstance.getRawName()).newln();
                }
            }
        }
        d.endBlockComment();
        if (showPackage) {
            d.packageName(classFile.getRefClassType());
        }
    }

    protected void dumpImplements(Dumper d, ClassSignature signature) {
        List<JavaTypeInstance> interfaces = signature.getInterfaces();
        if (!interfaces.isEmpty()) {
            d.keyword("implements ");
            int size = interfaces.size();
            for (int x = 0; x < size; ++x) {
                JavaTypeInstance iface = interfaces.get(x);
                d.dump(iface).separator(x < size - 1 ? "," : "").newln();
            }
        }
    }

    protected void dumpPermitted(ClassFile c, Dumper d) {
        c.dumpPermitted(d);
    }

    void dumpImports(Dumper d, ClassFile classFile) {
        Set<DetectedStaticImport> staticImports;
        List<JavaTypeInstance> classTypes = d.getObfuscationMapping().get(classFile.getAllClassTypes());
        Set<JavaRefTypeInstance> types = d.getTypeUsageInformation().getShortenedClassTypes();
        types.removeAll(classTypes);
        List<JavaRefTypeInstance> inners = Functional.filter(types, new Predicate<JavaRefTypeInstance>(){

            @Override
            public boolean test(JavaRefTypeInstance in) {
                return in.getInnerClassHereInfo().isInnerClass();
            }
        });
        types.removeAll(inners);
        for (JavaRefTypeInstance inner : inners) {
            types.add(InnerClassInfoUtils.getTransitiveOuterClass(inner));
        }
        types.removeAll(Functional.filter(types, new Predicate<JavaRefTypeInstance>(){

            @Override
            public boolean test(JavaRefTypeInstance in) {
                return "".equals(in.getPackageName());
            }
        }));
        Options options = this.dcCommonState.getOptions();
        final IllegalIdentifierDump iid = IllegalIdentifierDump.Factory.getOrNull(options);
        Collection<JavaRefTypeInstance> importTypes = types;
        if (((Boolean)options.getOption(OptionsImpl.HIDE_LANG_IMPORTS)).booleanValue()) {
            importTypes = Functional.filter(importTypes, new Predicate<JavaRefTypeInstance>(){

                @Override
                public boolean test(JavaRefTypeInstance in) {
                    return !"java.lang".equals(in.getPackageName());
                }
            });
        }
        List<String> names = Functional.map(importTypes, new UnaryFunction<JavaRefTypeInstance, String>(){

            @Override
            public String invoke(JavaRefTypeInstance arg) {
                if (arg.getInnerClassHereInfo().isInnerClass()) {
                    String name = arg.getRawName(iid);
                    return name.replace('$', '.');
                }
                return arg.getRawName(iid);
            }
        });
        boolean action = false;
        if (!names.isEmpty()) {
            Collections.sort(names);
            for (String name : names) {
                d.keyword("import ").print(name).endCodeln();
            }
            action = true;
        }
        if (!(staticImports = d.getTypeUsageInformation().getDetectedStaticImports()).isEmpty()) {
            List<String> sis = Functional.map(staticImports, new UnaryFunction<DetectedStaticImport, String>(){

                @Override
                public String invoke(DetectedStaticImport arg) {
                    String name = arg.getClazz().getRawName(iid);
                    return name.replace('$', '.') + '.' + arg.getName();
                }
            });
            Collections.sort(sis);
            for (String si : sis) {
                d.keyword("import").print(' ').keyword("static").print(" " + si).endCodeln();
            }
            action = true;
        }
        if (action) {
            d.newln();
        }
    }

    void dumpMethods(ClassFile classFile, Dumper d, boolean first, boolean asClass) {
        List<FakeMethod> fakes;
        List<Method> methods = classFile.getMethods();
        if (!methods.isEmpty()) {
            for (Method method : methods) {
                if (method.hiddenState() != Method.Visibility.Visible) continue;
                if (!first) {
                    d.newln();
                }
                first = false;
                method.dump(d, asClass);
            }
        }
        if ((fakes = classFile.getMethodFakes()) != null && !fakes.isEmpty()) {
            for (FakeMethod method : fakes) {
                if (!first) {
                    d.newln();
                }
                first = false;
                method.dump(d);
            }
        }
    }

    void dumpComments(ClassFile classFile, Dumper d) {
        DecompilerComments comments = classFile.getNullableDecompilerComments();
        if (comments == null) {
            return;
        }
        comments.dump(d);
    }

    void dumpAnnotations(ClassFile classFile, Dumper d) {
        AttributeMap classFileAttributes = classFile.getAttributes();
        AttributeRuntimeVisibleAnnotations runtimeVisibleAnnotations = (AttributeRuntimeVisibleAnnotations)classFileAttributes.getByName("RuntimeVisibleAnnotations");
        AttributeRuntimeInvisibleAnnotations runtimeInvisibleAnnotations = (AttributeRuntimeInvisibleAnnotations)classFileAttributes.getByName("RuntimeInvisibleAnnotations");
        if (runtimeVisibleAnnotations != null) {
            runtimeVisibleAnnotations.dump(d);
        }
        if (runtimeInvisibleAnnotations != null) {
            runtimeInvisibleAnnotations.dump(d);
        }
    }
}

