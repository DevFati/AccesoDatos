/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.loc;

import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocSet;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocSimple;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.CollectionUtils;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

public class BytecodeLocCollector {
    private final Map<Method, Set<Integer>> data = MapFactory.newIdentityMap();

    private Set<Integer> getForMethod(Method method) {
        Set<Integer> locs = this.data.get(method);
        if (locs == null) {
            locs = SetFactory.newSet();
            this.data.put(method, locs);
        }
        return locs;
    }

    public void add(Method method, int offset) {
        this.getForMethod(method).add(offset);
    }

    public void add(Method method, Set<Integer> offsets) {
        this.getForMethod(method).addAll(offsets);
    }

    public BytecodeLoc getLoc() {
        Set<Integer> s;
        if (this.data.isEmpty()) {
            return BytecodeLoc.NONE;
        }
        if (this.data.values().size() == 1 && (s = CollectionUtils.getSingle(this.data.values())).size() == 1) {
            return new BytecodeLocSimple(SetUtil.getSingle(s), SetUtil.getSingle(this.data.keySet()));
        }
        return new BytecodeLocSet(this.data);
    }
}

