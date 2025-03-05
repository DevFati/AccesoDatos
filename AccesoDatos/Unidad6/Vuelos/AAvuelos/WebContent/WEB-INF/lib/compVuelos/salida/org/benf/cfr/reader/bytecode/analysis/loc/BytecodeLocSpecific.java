/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.loc;

import java.util.Collection;
import java.util.Collections;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocCollector;
import org.benf.cfr.reader.entities.Method;

class BytecodeLocSpecific
extends BytecodeLoc {
    private final Specific type;

    BytecodeLocSpecific(Specific type) {
        this.type = type;
    }

    @Override
    void addTo(BytecodeLocCollector collector) {
    }

    public String toString() {
        return this.type.name();
    }

    @Override
    public Collection<Method> getMethods() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Integer> getOffsetsForMethod(Method method) {
        return Collections.emptyList();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    static enum Specific {
        DISABLED,
        TODO,
        NONE;

    }
}

