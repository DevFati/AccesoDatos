/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.wildcard;

import org.benf.cfr.reader.util.Optional;

public class OptionalMatch<T> {
    final Optional<T> expected;
    T matched;

    OptionalMatch(Optional<T> expected) {
        this.expected = expected;
        this.reset();
    }

    public boolean match(T other) {
        if (this.matched != null) {
            return this.matched.equals(other);
        }
        this.matched = other;
        return true;
    }

    public void reset() {
        this.matched = this.expected.isSet() ? this.expected.getValue() : null;
    }

    public T getMatch() {
        return this.matched;
    }
}

