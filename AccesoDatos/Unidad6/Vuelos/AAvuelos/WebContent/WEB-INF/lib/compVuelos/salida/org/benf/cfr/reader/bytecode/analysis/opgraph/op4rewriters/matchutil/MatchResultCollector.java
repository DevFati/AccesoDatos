/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil;

import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;

public interface MatchResultCollector {
    public void clear();

    public void collectStatement(String var1, StructuredStatement var2);

    public void collectMatches(String var1, WildcardMatch var2);
}

