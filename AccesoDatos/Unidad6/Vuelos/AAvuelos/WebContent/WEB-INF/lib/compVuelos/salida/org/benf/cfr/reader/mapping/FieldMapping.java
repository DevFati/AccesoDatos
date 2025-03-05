/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.mapping;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

public class FieldMapping {
    private final String name;
    private final String rename;
    private final JavaTypeInstance type;

    FieldMapping(String rename, String name, JavaTypeInstance type) {
        this.name = name;
        this.rename = rename;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public String getRename() {
        return this.rename;
    }

    public JavaTypeInstance getType() {
        return this.type;
    }
}

