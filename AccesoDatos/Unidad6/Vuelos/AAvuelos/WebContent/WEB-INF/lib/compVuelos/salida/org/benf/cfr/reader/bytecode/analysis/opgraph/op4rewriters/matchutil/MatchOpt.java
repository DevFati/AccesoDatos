/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil;

import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.Matcher;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;

public class MatchOpt
implements Matcher<StructuredStatement> {
    private final Matcher<StructuredStatement> matcher;

    public MatchOpt(Matcher<StructuredStatement> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        MatchIterator<StructuredStatement> mi = matchIterator.copy();
        if (this.matcher.match(mi, matchResultCollector)) {
            matchIterator.advanceTo(mi);
            return true;
        }
        return true;
    }
}

