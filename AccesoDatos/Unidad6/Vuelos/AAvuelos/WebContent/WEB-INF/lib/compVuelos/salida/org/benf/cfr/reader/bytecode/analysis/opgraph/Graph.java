/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph;

import java.util.List;

public interface Graph<T> {
    public List<T> getSources();

    public List<T> getTargets();
}

