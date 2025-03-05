/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Graph;

public interface MutableGraph<T>
extends Graph<T> {
    public void addSource(T var1);

    public void addTarget(T var1);
}

