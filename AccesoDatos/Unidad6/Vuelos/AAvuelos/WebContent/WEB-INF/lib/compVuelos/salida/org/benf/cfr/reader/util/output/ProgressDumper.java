/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

public interface ProgressDumper {
    public void analysingType(JavaTypeInstance var1);

    public void analysingPath(String var1);
}

