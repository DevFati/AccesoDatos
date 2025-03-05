/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.apiunreleased;

import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.util.AnalysisType;

public interface ClassFileSource2
extends ClassFileSource {
    public JarContent addJarContent(String var1, AnalysisType var2);
}

