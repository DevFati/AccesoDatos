/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.mapping.MappingFactory;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.relationship.MemberNameResolver;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollectingDumper;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.CfrVersionInfo;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.BinaryFunction;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.DumperFactory;
import org.benf.cfr.reader.util.output.ExceptionDumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.NopSummaryDumper;
import org.benf.cfr.reader.util.output.ProgressDumper;
import org.benf.cfr.reader.util.output.SummaryDumper;
import org.benf.cfr.reader.util.output.ToStringDumper;

class Driver {
    Driver() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void doClass(DCCommonState dcCommonState, String path, boolean skipInnerClass, DumperFactory dumperFactory) {
        Options options = dcCommonState.getOptions();
        ObfuscationMapping mapping = MappingFactory.get(options, dcCommonState);
        dcCommonState = new DCCommonState(dcCommonState, mapping);
        IllegalIdentifierDump illegalIdentifierDump = IllegalIdentifierDump.Factory.get(options);
        Dumper d = new ToStringDumper();
        ExceptionDumper ed = dumperFactory.getExceptionDumper();
        try {
            String methname;
            NopSummaryDumper summaryDumper = new NopSummaryDumper();
            ClassFile c = dcCommonState.getClassFileMaybePath(path);
            if (skipInnerClass && c.isInnerClass()) {
                return;
            }
            dcCommonState.configureWith(c);
            dumperFactory.getProgressDumper().analysingType(c.getClassType());
            try {
                c = dcCommonState.getClassFile(c.getClassType());
            }
            catch (CannotLoadClassException cannotLoadClassException) {
                // empty catch block
            }
            if (((Boolean)options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES)).booleanValue()) {
                c.loadInnerClasses(dcCommonState);
            }
            if (((Boolean)options.getOption(OptionsImpl.RENAME_DUP_MEMBERS)).booleanValue()) {
                MemberNameResolver.resolveNames(dcCommonState, ListFactory.newList(dcCommonState.getClassCache().getLoadedTypes()));
            }
            TypeUsageCollectingDumper collectingDumper = new TypeUsageCollectingDumper(options, c);
            c.analyseTop(dcCommonState, collectingDumper);
            TypeUsageInformation typeUsageInformation = collectingDumper.getRealTypeUsageInformation();
            d = dumperFactory.getNewTopLevelDumper(c.getClassType(), summaryDumper, typeUsageInformation, illegalIdentifierDump);
            d = dcCommonState.getObfuscationMapping().wrap(d);
            if (((Boolean)options.getOption(OptionsImpl.TRACK_BYTECODE_LOC)).booleanValue()) {
                d = dumperFactory.wrapLineNoDumper(d);
            }
            if ((methname = (String)options.getOption(OptionsImpl.METHODNAME)) == null) {
                c.dump(d);
            } else {
                try {
                    for (Method method : c.getMethodByName(methname)) {
                        method.dump(d, true);
                    }
                }
                catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException("No such method '" + methname + "'.");
                }
            }
            d.print("");
        }
        catch (Exception e) {
            ed.noteException(path, null, e);
        }
        finally {
            if (d != null) {
                d.close();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void doJar(DCCommonState dcCommonState, String path, AnalysisType analysisType, DumperFactory dumperFactory) {
        Options options = dcCommonState.getOptions();
        IllegalIdentifierDump illegalIdentifierDump = IllegalIdentifierDump.Factory.get(options);
        ObfuscationMapping mapping = MappingFactory.get(options, dcCommonState);
        dcCommonState = new DCCommonState(dcCommonState, mapping);
        SummaryDumper summaryDumper = null;
        try {
            ProgressDumper progressDumper = dumperFactory.getProgressDumper();
            summaryDumper = dumperFactory.getSummaryDumper();
            summaryDumper.notify("Summary for " + path);
            summaryDumper.notify("Decompiled with CFR " + CfrVersionInfo.VERSION_INFO);
            progressDumper.analysingPath(path);
            TreeMap<Integer, List<JavaTypeInstance>> clstypes = dcCommonState.explicitlyLoadJar(path, analysisType);
            Set<JavaTypeInstance> versionCollisions = Driver.getVersionCollisions(clstypes);
            dcCommonState.setCollisions(versionCollisions);
            List versionsSeen = ListFactory.newList();
            Driver.addMissingOuters(clstypes);
            for (Map.Entry entry : clstypes.entrySet()) {
                int forVersion = (Integer)entry.getKey();
                versionsSeen.add(forVersion);
                List<Integer> localVersionsSeen = ListFactory.newList(versionsSeen);
                List types = (List)entry.getValue();
                Driver.doJarVersionTypes(forVersion, localVersionsSeen, dcCommonState, dumperFactory, illegalIdentifierDump, summaryDumper, progressDumper, types);
            }
        }
        catch (Exception e) {
            dumperFactory.getExceptionDumper().noteException(path, "Exception analysing jar", e);
            if (summaryDumper != null) {
                summaryDumper.notify("Exception analysing jar " + e);
            }
        }
        finally {
            if (summaryDumper != null) {
                summaryDumper.close();
            }
        }
    }

    private static void addMissingOuters(Map<Integer, List<JavaTypeInstance>> clstypes) {
        for (Map.Entry<Integer, List<JavaTypeInstance>> entry : clstypes.entrySet()) {
            int version = entry.getKey();
            if (version == 0) continue;
            Set distinct = SetFactory.newOrderedSet((Collection)entry.getValue());
            Set toAdd = SetFactory.newOrderedSet();
            for (JavaTypeInstance typ : entry.getValue()) {
                InnerClassInfo ici = typ.getInnerClassHereInfo();
                while (ici != null && ici.isInnerClass()) {
                    typ = ici.getOuterClass();
                    if (distinct.add(typ)) {
                        toAdd.add(typ);
                    }
                    ici = typ.getInnerClassHereInfo();
                }
            }
            entry.getValue().addAll(toAdd);
        }
    }

    private static Set<JavaTypeInstance> getVersionCollisions(Map<Integer, List<JavaTypeInstance>> clstypes) {
        if (clstypes.size() <= 1) {
            return Collections.emptySet();
        }
        Set<JavaTypeInstance> collisions = SetFactory.newOrderedSet();
        Set seen = SetFactory.newSet();
        for (List<JavaTypeInstance> types : clstypes.values()) {
            for (JavaTypeInstance type : types) {
                if (seen.add(type)) continue;
                collisions.add(type);
            }
        }
        return collisions;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void doJarVersionTypes(int forVersion, final List<Integer> versionsSeen, DCCommonState dcCommonState, DumperFactory dumperFactory, IllegalIdentifierDump illegalIdentifierDump, SummaryDumper summaryDumper, ProgressDumper progressDumper, List<JavaTypeInstance> types) {
        Options options = dcCommonState.getOptions();
        boolean lomem = (Boolean)options.getOption(OptionsImpl.LOMEM);
        final Predicate<String> matcher = MiscUtils.mkRegexFilter((String)options.getOption(OptionsImpl.JAR_FILTER), true);
        boolean silent = (Boolean)options.getOption(OptionsImpl.SILENT);
        if (forVersion > 0) {
            dumperFactory = dumperFactory.getFactoryWithPrefix("/META-INF/versions/" + forVersion + "/", forVersion);
            Collections.reverse(versionsSeen);
            dcCommonState = new DCCommonState(dcCommonState, new BinaryFunction<String, DCCommonState, ClassFile>(){

                @Override
                public ClassFile invoke(String arg, DCCommonState arg2) {
                    CannotLoadClassException lastException = null;
                    Iterator iterator = versionsSeen.iterator();
                    while (iterator.hasNext()) {
                        int version = (Integer)iterator.next();
                        try {
                            if (version == 0) {
                                return arg2.loadClassFileAtPath(arg);
                            }
                            return arg2.loadClassFileAtPath("META-INF/versions/" + version + "/" + arg);
                        }
                        catch (CannotLoadClassException e) {
                            lastException = e;
                        }
                    }
                    throw new CannotLoadClassException(arg, lastException);
                }
            });
        }
        types = Functional.filter(types, new Predicate<JavaTypeInstance>(){

            @Override
            public boolean test(JavaTypeInstance in) {
                return matcher.test(in.getRawName());
            }
        });
        if (((Boolean)options.getOption(OptionsImpl.RENAME_DUP_MEMBERS)).booleanValue() || ((Boolean)options.getOption(OptionsImpl.RENAME_ENUM_MEMBERS)).booleanValue()) {
            MemberNameResolver.resolveNames(dcCommonState, types);
        }
        for (JavaTypeInstance type : types) {
            Dumper d = new ToStringDumper();
            try {
                ClassFile c = dcCommonState.getClassFile(type);
                if (c.isInnerClass()) {
                    d = null;
                    continue;
                }
                if (!silent) {
                    type = dcCommonState.getObfuscationMapping().get(type);
                    progressDumper.analysingType(type);
                }
                if (((Boolean)options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES)).booleanValue()) {
                    c.loadInnerClasses(dcCommonState);
                }
                TypeUsageCollectingDumper collectingDumper = new TypeUsageCollectingDumper(options, c);
                c.analyseTop(dcCommonState, collectingDumper);
                JavaTypeInstance classType = c.getClassType();
                classType = dcCommonState.getObfuscationMapping().get(classType);
                TypeUsageInformation typeUsageInformation = collectingDumper.getRealTypeUsageInformation();
                d = dumperFactory.getNewTopLevelDumper(classType, summaryDumper, typeUsageInformation, illegalIdentifierDump);
                d = dcCommonState.getObfuscationMapping().wrap(d);
                c.dump(d);
                d.newln();
                d.newln();
                if (!lomem) continue;
                c.releaseCode();
            }
            catch (Dumper.CannotCreate e) {
                throw e;
            }
            catch (RuntimeException e) {
                d.print(e.toString()).newln().newln().newln();
            }
            finally {
                if (d == null) continue;
                d.close();
            }
        }
    }
}

