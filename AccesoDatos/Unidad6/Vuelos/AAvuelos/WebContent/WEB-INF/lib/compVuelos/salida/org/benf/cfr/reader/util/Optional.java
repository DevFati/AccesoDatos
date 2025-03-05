/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util;

import org.benf.cfr.reader.util.functors.UnaryProcedure;

public class Optional<T> {
    private final T value;
    private final boolean set;
    private static final Optional Empty = new Optional();

    private Optional(T val) {
        this.value = val;
        this.set = true;
    }

    private Optional() {
        this.set = false;
        this.value = null;
    }

    public boolean isSet() {
        return this.set;
    }

    public T getValue() {
        return this.value;
    }

    public void then(UnaryProcedure<T> func) {
        func.call(this.value);
    }

    public static <T> Optional<T> of(T value) {
        return new Optional<T>(value);
    }

    public static <T> Optional<T> empty() {
        return Empty;
    }
}

