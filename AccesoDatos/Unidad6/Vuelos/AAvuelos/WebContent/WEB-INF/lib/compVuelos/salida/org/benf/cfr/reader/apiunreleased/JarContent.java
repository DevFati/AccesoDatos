/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.apiunreleased;

import java.util.Collection;
import java.util.Map;
import org.benf.cfr.reader.util.AnalysisType;

public interface JarContent {
    public Collection<String> getClassFiles();

    public Map<String, String> getManifestEntries();

    public AnalysisType getAnalysisType();
}

