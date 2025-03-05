/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

public enum DynamicInvokeType {
    UNKNOWN("?"),
    BOOTSTRAP("bootstrap"),
    METAFACTORY_1("metaFactory"),
    METAFACTORY_2("metafactory"),
    ALTMETAFACTORY_1("altMetaFactory"),
    ALTMETAFACTORY_2("altMetafactory");

    private final String constName;

    private DynamicInvokeType(String constName) {
        this.constName = constName;
    }

    public String getConstName() {
        return this.constName;
    }

    public static DynamicInvokeType lookup(String name) {
        if (name.equals(DynamicInvokeType.METAFACTORY_1.constName)) {
            return METAFACTORY_1;
        }
        if (name.equals(DynamicInvokeType.METAFACTORY_2.constName)) {
            return METAFACTORY_2;
        }
        if (name.equals(DynamicInvokeType.ALTMETAFACTORY_1.constName)) {
            return ALTMETAFACTORY_1;
        }
        if (name.equals(DynamicInvokeType.ALTMETAFACTORY_2.constName)) {
            return ALTMETAFACTORY_2;
        }
        if (name.equals(DynamicInvokeType.BOOTSTRAP.constName)) {
            return BOOTSTRAP;
        }
        return UNKNOWN;
    }
}

