/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import org.benf.cfr.reader.bytecode.analysis.types.StackTypes;

public enum StackType {
    INT("int", 1, true),
    FLOAT("float", 1, true),
    REF("reference", 1, false),
    RETURNADDRESS("returnaddress", 1, false),
    RETURNADDRESSORREF("returnaddress or ref", 1, false),
    LONG("long", 2, true),
    DOUBLE("double", 2, true),
    VOID("void", 0, false);

    private final int computationCategory;
    private final StackTypes asList;
    private final boolean closed;
    private final String name;

    private StackType(String name, int computationCategory, boolean closed) {
        this.name = name;
        this.computationCategory = computationCategory;
        this.asList = new StackTypes(this);
        this.closed = closed;
    }

    public int getComputationCategory() {
        return this.computationCategory;
    }

    public StackTypes asList() {
        return this.asList;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public String toString() {
        return this.name;
    }
}

