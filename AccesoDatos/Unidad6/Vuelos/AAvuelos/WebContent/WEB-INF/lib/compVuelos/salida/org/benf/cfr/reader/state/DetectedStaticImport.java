/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

public class DetectedStaticImport {
    final JavaTypeInstance clazz;
    final String name;

    public JavaTypeInstance getClazz() {
        return this.clazz;
    }

    public String getName() {
        return this.name;
    }

    DetectedStaticImport(JavaTypeInstance clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        DetectedStaticImport that = (DetectedStaticImport)o;
        if (!this.clazz.equals(that.clazz)) {
            return false;
        }
        return this.name.equals(that.name);
    }

    public int hashCode() {
        int result = this.clazz.hashCode();
        result = 31 * result + this.name.hashCode();
        return result;
    }
}

