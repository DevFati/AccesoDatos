/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.collections;

import java.util.Map;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class LazyExceptionRetainingMap<X, Y>
extends LazyMap<X, Y> {
    private final Map<X, RuntimeException> exceptionMap = MapFactory.newMap();

    LazyExceptionRetainingMap(Map<X, Y> inner, UnaryFunction<X, Y> factory) {
        super(inner, factory);
    }

    @Override
    public Y get(Object o) {
        RuntimeException exception = this.exceptionMap.get(o);
        if (exception == null) {
            try {
                return super.get(o);
            }
            catch (RuntimeException e) {
                exception = e;
                this.exceptionMap.put(o, e);
            }
        }
        throw exception;
    }
}

