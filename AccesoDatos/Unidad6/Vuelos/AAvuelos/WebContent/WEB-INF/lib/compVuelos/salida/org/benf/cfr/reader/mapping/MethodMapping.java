/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.mapping;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

public class MethodMapping {
    private final String name;
    private final String rename;
    private final JavaTypeInstance res;
    private final List<JavaTypeInstance> argTypes;

    public MethodMapping(String rename, String name, JavaTypeInstance res, List<JavaTypeInstance> argTypes) {
        this.name = name;
        this.rename = rename;
        this.res = res;
        this.argTypes = argTypes;
    }

    public String getName() {
        return this.name;
    }

    public String getRename() {
        return this.rename;
    }

    public JavaTypeInstance getResultType() {
        return this.res;
    }

    public List<JavaTypeInstance> getArgTypes() {
        return this.argTypes;
    }
}

