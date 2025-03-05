/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class Functional {
    public static <X> List<X> filterOptimistic(List<X> input, Predicate<X> predicate) {
        ArrayList<X> res = null;
        for (int x = 0; x < input.size(); ++x) {
            X item = input.get(x);
            if (!predicate.test(item)) {
                if (res != null) continue;
                res = new ArrayList<X>();
                for (int y = 0; y < x; ++y) {
                    res.add(input.get(y));
                }
                continue;
            }
            if (res == null) continue;
            res.add(item);
        }
        return res == null ? input : res;
    }

    public static <X> List<X> filter(Collection<X> input, Predicate<X> predicate) {
        List result = ListFactory.newList();
        for (X item : input) {
            if (!predicate.test(item)) continue;
            result.add(item);
        }
        return result;
    }

    public static <X> X findOrNull(Collection<X> input, Predicate<X> predicate) {
        List result = ListFactory.newList();
        for (X item : input) {
            if (!predicate.test(item)) continue;
            return item;
        }
        return null;
    }

    public static <X> Set<X> filterSet(Collection<X> input, Predicate<X> predicate) {
        Set result = SetFactory.newSet();
        for (X item : input) {
            if (!predicate.test(item)) continue;
            result.add(item);
        }
        return result;
    }

    public static <X> boolean any(Collection<X> input, Predicate<X> predicate) {
        List result = ListFactory.newList();
        for (X item : input) {
            if (!predicate.test(item)) continue;
            return true;
        }
        return false;
    }

    public static <X> boolean all(Collection<X> input, Predicate<X> predicate) {
        List result = ListFactory.newList();
        for (X item : input) {
            if (predicate.test(item)) continue;
            return false;
        }
        return true;
    }

    public static <X> Pair<List<X>, List<X>> partition(Collection<X> input, Predicate<X> predicate) {
        List lTrue = ListFactory.newList();
        List lFalse = ListFactory.newList();
        for (X item : input) {
            if (predicate.test(item)) {
                lTrue.add(item);
                continue;
            }
            lFalse.add(item);
        }
        return new Pair(lTrue, lFalse);
    }

    public static <X, Y> List<Y> map(Collection<X> input, UnaryFunction<X, Y> function) {
        List result = ListFactory.newList();
        for (X item : input) {
            result.add(function.invoke(item));
        }
        return result;
    }

    public static <X> List<X> uniqAll(List<X> input) {
        Set found = SetFactory.newSet();
        List result = ListFactory.newList();
        for (X in : input) {
            if (!found.add(in)) continue;
            result.add(in);
        }
        return result;
    }

    public static <X> Map<X, Integer> indexedIdentityMapOf(Collection<X> input) {
        Map temp = MapFactory.newIdentityMap();
        int idx = 0;
        for (X x : input) {
            temp.put(x, idx++);
        }
        return temp;
    }

    public static <Y, X> Map<Y, List<X>> groupToMapBy(Collection<X> input, UnaryFunction<X, Y> mapF) {
        Map temp = MapFactory.newMap();
        return Functional.groupToMapBy(input, temp, mapF);
    }

    public static <Y, X> Map<Y, List<X>> groupToMapBy(Collection<X> input, Map<Y, List<X>> tgt, UnaryFunction<X, Y> mapF) {
        for (X x : input) {
            Y key = mapF.invoke(x);
            List<X> lx = tgt.get(key);
            if (lx == null) {
                lx = ListFactory.newList();
                tgt.put(key, lx);
            }
            lx.add(x);
        }
        return tgt;
    }

    public static <Y, X> List<Y> groupBy(List<X> input, Comparator<? super X> comparator, UnaryFunction<List<X>, Y> gf) {
        TreeMap<X, List> temp = new TreeMap<X, List>(comparator);
        for (X x : input) {
            List lx = (List)temp.get(x);
            if (lx == null) {
                lx = ListFactory.newList();
                temp.put(x, lx);
            }
            lx.add(x);
        }
        List res = ListFactory.newList();
        for (List lx : temp.values()) {
            res.add(gf.invoke(lx));
        }
        return res;
    }

    public static class NotNull<X>
    implements Predicate<X> {
        @Override
        public boolean test(X in) {
            return in != null;
        }
    }
}

