/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.wildcard;

public interface Wildcard<X> {
    public X getMatch();

    public void resetMatch();
}

