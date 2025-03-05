/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.collections;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.benf.cfr.reader.util.collections.LazyExceptionRetainingMap;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class MapFactory {
    public static <X, Y> Map<X, Y> newMap() {
        return new HashMap();
    }

    public static <X, Y> Map<X, Y> newOrderedMap() {
        return new LinkedHashMap();
    }

    public static <X, Y> Map<X, Y> newIdentityMap() {
        return new IdentityHashMap();
    }

    public static <X, Y> Map<X, Y> newIdentityLazyMap(UnaryFunction<X, Y> factory) {
        return new LazyMap<X, Y>(MapFactory.<X, Y>newIdentityMap(), factory);
    }

    public static <X, Y> TreeMap<X, Y> newTreeMap() {
        return new TreeMap();
    }

    public static <X, Y> LazyMap<X, Y> newLazyMap(UnaryFunction<X, Y> factory) {
        return new LazyMap<X, Y>(MapFactory.<X, Y>newMap(), factory);
    }

    public static <X, Y> Map<X, Y> newLinkedLazyMap(UnaryFunction<X, Y> factory) {
        return new LazyMap<X, Y>(MapFactory.<X, Y>newOrderedMap(), factory);
    }

    public static <X, Y> Map<X, Y> newLazyMap(Map<X, Y> base, UnaryFunction<X, Y> factory) {
        return new LazyMap<X, Y>(base, factory);
    }

    public static <X, Y> Map<X, Y> newExceptionRetainingLazyMap(UnaryFunction<X, Y> factory) {
        return new LazyExceptionRetainingMap<X, Y>(MapFactory.<X, Y>newMap(), factory);
    }
}

