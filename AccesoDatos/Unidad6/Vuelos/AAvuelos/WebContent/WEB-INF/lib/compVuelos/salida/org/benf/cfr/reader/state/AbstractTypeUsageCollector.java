/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Collection;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.TypeUsageCollectable;

public abstract class AbstractTypeUsageCollector
implements TypeUsageCollector {
    @Override
    public void collect(Collection<? extends JavaTypeInstance> types) {
        if (types == null) {
            return;
        }
        for (JavaTypeInstance javaTypeInstance : types) {
            this.collect(javaTypeInstance);
        }
    }

    @Override
    public void collectFrom(TypeUsageCollectable collectable) {
        if (collectable != null) {
            collectable.collectTypeUsages(this);
        }
    }

    @Override
    public void collectFromT(TypeUsageCollectable collectable) {
        this.collectFrom(collectable);
    }

    @Override
    public void collectFrom(Collection<? extends TypeUsageCollectable> collectables) {
        if (collectables != null) {
            for (TypeUsageCollectable typeUsageCollectable : collectables) {
                if (typeUsageCollectable == null) continue;
                typeUsageCollectable.collectTypeUsages(this);
            }
        }
    }
}

