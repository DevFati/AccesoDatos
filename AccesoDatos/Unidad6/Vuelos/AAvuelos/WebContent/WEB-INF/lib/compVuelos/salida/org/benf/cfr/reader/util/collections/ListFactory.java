/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.util.collections.SetFactory;

public class ListFactory {
    public static <X> List<X> newList() {
        return new ArrayList();
    }

    public static <X> List<X> newImmutableList(X ... original) {
        return Arrays.asList(original);
    }

    public static <X> List<X> newList(X ... original) {
        List<X> res = ListFactory.newList();
        Collections.addAll(res, original);
        return res;
    }

    public static <X> List<X> newList(Collection<X> original) {
        return new ArrayList<X>(original);
    }

    public static <X> List<X> newList(int size) {
        return new ArrayList(size);
    }

    public static <X> LinkedList<X> newLinkedList() {
        return new LinkedList();
    }

    public static <X> List<X> uniqueList(Collection<X> list) {
        List<X> res = ListFactory.newList();
        Set tmp = SetFactory.newSet();
        for (X x : list) {
            if (!tmp.add(x)) continue;
            res.add(x);
        }
        return res;
    }

    public static <X> List<X> combinedOptimistic(List<X> a, List<X> b) {
        if (a == null || a.isEmpty()) {
            return b;
        }
        if (b == null || b.isEmpty()) {
            return a;
        }
        List<X> res = ListFactory.newList();
        res.addAll(a);
        res.addAll(b);
        return res;
    }

    public static <X> List<X> orEmptyList(List<X> nullableList) {
        return nullableList == null ? Collections.emptyList() : nullableList;
    }
}

