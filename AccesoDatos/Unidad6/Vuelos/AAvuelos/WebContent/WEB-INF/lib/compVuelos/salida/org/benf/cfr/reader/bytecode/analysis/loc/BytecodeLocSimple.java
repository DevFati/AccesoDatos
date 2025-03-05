/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.loc;

import java.util.Collection;
import java.util.Collections;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocCollector;
import org.benf.cfr.reader.entities.Method;

class BytecodeLocSimple
extends BytecodeLoc {
    private final int offset;
    private Method method;

    BytecodeLocSimple(int offset, Method method) {
        this.offset = offset;
        this.method = method;
    }

    @Override
    void addTo(BytecodeLocCollector collector) {
        collector.add(this.method, this.offset);
    }

    public String toString() {
        return Integer.toString(this.offset);
    }

    @Override
    public Collection<Method> getMethods() {
        return Collections.singleton(this.method);
    }

    @Override
    public Collection<Integer> getOffsetsForMethod(Method method) {
        return Collections.singleton(this.offset);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}

