/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public interface ClassFileSource {
    public void informAnalysisRelativePathDetail(String var1, String var2);

    public Collection<String> addJar(String var1);

    public String getPossiblyRenamedPath(String var1);

    public Pair<byte[], String> getClassFileContent(String var1) throws IOException;

    public static class Factory {
        static ClassFileSource createInternalClassFileSource(Map<String, String> options) {
            Options parsedOptions = OptionsImpl.getFactory().create(options);
            return new ClassFileSourceImpl(parsedOptions);
        }
    }
}

