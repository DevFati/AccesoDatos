/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.variables;

import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.util.output.Dumper;

public class NamedVariableDefault
implements NamedVariable {
    private String name;
    private boolean isGoodName = false;

    public NamedVariableDefault(String name) {
        this.name = name;
    }

    @Override
    public void forceName(String name) {
        this.name = name;
        this.isGoodName = true;
    }

    @Override
    public String getStringName() {
        return this.name;
    }

    @Override
    public Dumper dump(Dumper d) {
        return this.dump(d, false);
    }

    @Override
    public Dumper dump(Dumper d, boolean defines) {
        return d.identifier(this.name, this, defines);
    }

    @Override
    public boolean isGoodName() {
        return this.isGoodName;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        NamedVariableDefault that = (NamedVariableDefault)o;
        return this.name.equals(that.name);
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public String toString() {
        return this.name;
    }
}

