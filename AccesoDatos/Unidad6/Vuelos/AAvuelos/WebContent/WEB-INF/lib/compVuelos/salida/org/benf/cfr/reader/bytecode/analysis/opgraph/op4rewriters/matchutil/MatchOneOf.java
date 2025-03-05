/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil;

import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.Matcher;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;

public class MatchOneOf
implements Matcher<StructuredStatement> {
    private final Matcher<StructuredStatement>[] matchers;

    public MatchOneOf(Matcher<StructuredStatement> ... matchers) {
        this.matchers = matchers;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        for (Matcher<StructuredStatement> matcher : this.matchers) {
            MatchIterator<StructuredStatement> mi = matchIterator.copy();
            if (!matcher.match(mi, matchResultCollector)) continue;
            matchIterator.advanceTo(mi);
            return true;
        }
        return false;
    }
}

