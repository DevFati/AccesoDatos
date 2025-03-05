/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.loc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocCollector;
import org.benf.cfr.reader.entities.Method;

public class BytecodeLocSet
extends BytecodeLoc {
    private final Map<Method, Set<Integer>> locs;

    BytecodeLocSet(Map<Method, Set<Integer>> locs) {
        this.locs = locs;
    }

    @Override
    void addTo(BytecodeLocCollector collector) {
        for (Map.Entry<Method, Set<Integer>> entry : this.locs.entrySet()) {
            collector.add(entry.getKey(), entry.getValue());
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Method, Set<Integer>> entry : this.locs.entrySet()) {
            sb.append(entry.getKey().getName()).append("[");
            for (Integer i : entry.getValue()) {
                sb.append(i).append(",");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public Collection<Method> getMethods() {
        return this.locs.keySet();
    }

    @Override
    public Collection<Integer> getOffsetsForMethod(Method method) {
        return this.locs.get(method);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}

