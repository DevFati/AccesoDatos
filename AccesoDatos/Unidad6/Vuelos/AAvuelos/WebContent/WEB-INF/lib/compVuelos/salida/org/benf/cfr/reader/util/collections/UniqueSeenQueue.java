/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.collections;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class UniqueSeenQueue<T> {
    private final LinkedList<T> ll;
    private final Set<T> llItems = SetFactory.newSet();
    private final Set<T> seen;

    public UniqueSeenQueue(Collection<? extends T> c) {
        this.ll = ListFactory.newLinkedList();
        this.seen = SetFactory.newSet();
        this.ll.addAll(c);
        this.llItems.addAll(c);
        this.seen.addAll(c);
    }

    public boolean isEmpty() {
        return this.ll.isEmpty();
    }

    public T removeFirst() {
        T res = this.ll.removeFirst();
        this.llItems.remove(res);
        return res;
    }

    public boolean add(T c) {
        if (this.llItems.add(c)) {
            this.seen.add(c);
            this.ll.add(c);
            return true;
        }
        return false;
    }

    public boolean addIfUnseen(T c) {
        if (this.seen.add(c)) {
            this.llItems.add(c);
            this.ll.add(c);
            return true;
        }
        return false;
    }

    public boolean add(T c, boolean ifUnseen) {
        if (ifUnseen) {
            return this.addIfUnseen(c);
        }
        return this.add(c);
    }

    public void addAll(Collection<? extends T> ts) {
        for (T t : ts) {
            this.add(t);
        }
    }
}

