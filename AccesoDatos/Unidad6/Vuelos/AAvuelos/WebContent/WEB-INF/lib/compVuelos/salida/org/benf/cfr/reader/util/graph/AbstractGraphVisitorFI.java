/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.graph;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.graph.GraphVisitor;

public abstract class AbstractGraphVisitorFI<T>
implements GraphVisitor<T> {
    private final LinkedList<T> toVisit = ListFactory.newLinkedList();
    private final Set<T> visited = SetFactory.newSet();
    private final BinaryProcedure<T, GraphVisitor<T>> callee;
    private boolean aborted = false;

    AbstractGraphVisitorFI(T first, BinaryProcedure<T, GraphVisitor<T>> callee) {
        this.add(first);
        this.callee = callee;
    }

    private void add(T next) {
        if (next == null) {
            return;
        }
        if (!this.visited.contains(next)) {
            this.toVisit.add(next);
            this.visited.add(next);
        }
    }

    @Override
    public void abort() {
        this.toVisit.clear();
        this.aborted = true;
    }

    @Override
    public boolean wasAborted() {
        return this.aborted;
    }

    @Override
    public Collection<T> getVisitedNodes() {
        return this.visited;
    }

    @Override
    public void enqueue(T next) {
        this.add(next);
    }

    @Override
    public void enqueue(Collection<? extends T> next) {
        for (T t : next) {
            this.enqueue(t);
        }
    }

    @Override
    public void process() {
        do {
            T next = this.toVisit.removeFirst();
            this.callee.call(next, this);
        } while (!this.toVisit.isEmpty());
    }
}

