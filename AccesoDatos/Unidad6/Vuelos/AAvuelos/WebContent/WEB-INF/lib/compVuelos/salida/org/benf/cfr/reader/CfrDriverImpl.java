/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.benf.cfr.reader.Driver;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.state.ClassFileSourceChained;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.ClassFileSourceWrapper;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.DumperFactory;
import org.benf.cfr.reader.util.output.InternalDumperFactoryImpl;
import org.benf.cfr.reader.util.output.SinkDumperFactory;

public class CfrDriverImpl
implements CfrDriver {
    private final Options options;
    private final ClassFileSource2 classFileSource;
    private final OutputSinkFactory outputSinkFactory;

    public CfrDriverImpl(ClassFileSource source, OutputSinkFactory outputSinkFactory, Options options, boolean fallbackToDefaultSource) {
        ClassFileSource2 tmpSource;
        if (options == null) {
            options = new OptionsImpl(new HashMap<String, String>());
        }
        if (source == null) {
            tmpSource = new ClassFileSourceImpl(options);
        } else {
            ClassFileSource2 classFileSource2 = tmpSource = source instanceof ClassFileSource2 ? (ClassFileSource2)source : new ClassFileSourceWrapper(source);
            if (fallbackToDefaultSource) {
                tmpSource = new ClassFileSourceChained(Arrays.asList(tmpSource, new ClassFileSourceImpl(options)));
            }
        }
        this.outputSinkFactory = outputSinkFactory;
        this.options = options;
        this.classFileSource = tmpSource;
    }

    @Override
    public void analyse(List<String> toAnalyse) {
        boolean skipInnerClass = toAnalyse.size() > 1 && (Boolean)this.options.getOption(OptionsImpl.SKIP_BATCH_INNER_CLASSES) != false;
        toAnalyse = ListFactory.newList(toAnalyse);
        Collections.sort(toAnalyse);
        for (String path : toAnalyse) {
            this.classFileSource.informAnalysisRelativePathDetail(null, null);
            DCCommonState dcCommonState = new DCCommonState(this.options, this.classFileSource);
            DumperFactory dumperFactory = this.outputSinkFactory != null ? new SinkDumperFactory(this.outputSinkFactory, this.options) : new InternalDumperFactoryImpl(this.options);
            AnalysisType type = (AnalysisType)((Object)this.options.getOption(OptionsImpl.ANALYSE_AS));
            if (type == null || type == AnalysisType.DETECT) {
                type = dcCommonState.detectClsJar(path);
            }
            if (type == AnalysisType.JAR || type == AnalysisType.WAR) {
                Driver.doJar(dcCommonState, path, type, dumperFactory);
                continue;
            }
            if (type != AnalysisType.CLASS) continue;
            Driver.doClass(dcCommonState, path, skipInnerClass, dumperFactory);
        }
    }
}

