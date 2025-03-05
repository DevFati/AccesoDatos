/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.Main;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.ClassFileSourceWrapper;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.DumperFactory;
import org.benf.cfr.reader.util.output.ExceptionDumper;
import org.benf.cfr.reader.util.output.FileSummaryDumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.MethodErrorCollector;
import org.benf.cfr.reader.util.output.NopSummaryDumper;
import org.benf.cfr.reader.util.output.ProgressDumper;
import org.benf.cfr.reader.util.output.ProgressDumperNop;
import org.benf.cfr.reader.util.output.StdErrExceptionDumper;
import org.benf.cfr.reader.util.output.StringStreamDumper;
import org.benf.cfr.reader.util.output.SummaryDumper;

@Deprecated
public class PluginRunner {
    private final DCCommonState dcCommonState;

    public PluginRunner() {
        this(MapFactory.newMap(), null);
    }

    public PluginRunner(Map<String, String> options) {
        this(options, null);
    }

    public PluginRunner(Map<String, String> options, ClassFileSource classFileSource) {
        this.dcCommonState = PluginRunner.initDCState(options, classFileSource);
    }

    public Options getOptions() {
        return this.dcCommonState.getOptions();
    }

    public List<List<String>> addJarPaths(String[] jarPaths) {
        ArrayList<List<String>> res = new ArrayList<List<String>>();
        for (String jarPath : jarPaths) {
            res.add(this.addJarPath(jarPath));
        }
        return res;
    }

    public List<String> addJarPath(String jarPath) {
        try {
            List<JavaTypeInstance> types = this.dcCommonState.explicitlyLoadJar(jarPath, AnalysisType.JAR).get(0);
            return Functional.map(types, new UnaryFunction<JavaTypeInstance, String>(){

                @Override
                public String invoke(JavaTypeInstance arg) {
                    return arg.getRawName();
                }
            });
        }
        catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    public String getDecompilationFor(String classFilePath) {
        try {
            StringBuilder output = new StringBuilder();
            PluginDumperFactory dumperFactory = new PluginDumperFactory(output, this.dcCommonState.getOptions());
            Main.doClass(this.dcCommonState, classFilePath, false, dumperFactory);
            return output.toString();
        }
        catch (Exception e) {
            return e.toString();
        }
    }

    private static DCCommonState initDCState(Map<String, String> optionsMap, ClassFileSource classFileSource) {
        OptionsImpl options = new OptionsImpl(optionsMap);
        ClassFileSource2 source = classFileSource == null ? new ClassFileSourceImpl(options) : new ClassFileSourceWrapper(classFileSource);
        return new DCCommonState(options, source);
    }

    private class PluginDumperFactory
    implements DumperFactory {
        private final IllegalIdentifierDump illegalIdentifierDump = new IllegalIdentifierDump.Nop();
        private final StringBuilder outBuffer;
        private final Options options;

        public PluginDumperFactory(StringBuilder out, Options options) {
            this.outBuffer = out;
            this.options = options;
        }

        @Override
        public Dumper getNewTopLevelDumper(JavaTypeInstance classType, SummaryDumper summaryDumper, TypeUsageInformation typeUsageInformation, IllegalIdentifierDump illegalIdentifierDump) {
            return new StringStreamDumper(new MethodErrorCollector.SummaryDumperMethodErrorCollector(classType, summaryDumper), this.outBuffer, typeUsageInformation, this.options, this.illegalIdentifierDump);
        }

        @Override
        public Dumper wrapLineNoDumper(Dumper dumper) {
            return dumper;
        }

        @Override
        public SummaryDumper getSummaryDumper() {
            if (!this.options.optionIsSet(OptionsImpl.OUTPUT_DIR)) {
                return new NopSummaryDumper();
            }
            return new FileSummaryDumper((String)this.options.getOption(OptionsImpl.OUTPUT_DIR), this.options, null);
        }

        @Override
        public ProgressDumper getProgressDumper() {
            return ProgressDumperNop.INSTANCE;
        }

        @Override
        public ExceptionDumper getExceptionDumper() {
            return new StdErrExceptionDumper();
        }

        @Override
        public DumperFactory getFactoryWithPrefix(String prefix, int version) {
            return this;
        }
    }
}

