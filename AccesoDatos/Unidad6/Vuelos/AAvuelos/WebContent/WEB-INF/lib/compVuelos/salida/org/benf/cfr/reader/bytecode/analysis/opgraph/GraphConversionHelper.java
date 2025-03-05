/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Graph;
import org.benf.cfr.reader.bytecode.analysis.opgraph.MutableGraph;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.collections.MapFactory;

class GraphConversionHelper<X extends Graph<X>, Y extends MutableGraph<Y>> {
    private final Map<X, Y> correspondance = MapFactory.newMap();

    GraphConversionHelper() {
    }

    private Y findEntry(X key, X orig, String dbg) {
        MutableGraph value = (MutableGraph)this.correspondance.get(key);
        if (value == null) {
            throw new ConfusedCFRException("Missing key when tying up graph " + key + ", was " + dbg + " of " + orig);
        }
        return (Y)value;
    }

    void patchUpRelations() {
        for (Map.Entry<X, Y> entry : this.correspondance.entrySet()) {
            Graph orig = (Graph)entry.getKey();
            MutableGraph newnode = (MutableGraph)entry.getValue();
            for (Graph source : orig.getSources()) {
                newnode.addSource(this.findEntry(source, orig, "source"));
            }
            for (Graph target : orig.getTargets()) {
                newnode.addTarget(this.findEntry(target, orig, "target"));
            }
        }
    }

    void registerOriginalAndNew(X original, Y newnode) {
        this.correspondance.put(original, newnode);
    }
}

