/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Collection;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.TypeUsageCollectable;

public interface TypeUsageCollector {
    public void collectRefType(JavaRefTypeInstance var1);

    public void collect(JavaTypeInstance var1);

    public void collect(Collection<? extends JavaTypeInstance> var1);

    public void collectFromT(TypeUsageCollectable var1);

    public void collectFrom(TypeUsageCollectable var1);

    public void collectFrom(Collection<? extends TypeUsageCollectable> var1);

    public TypeUsageInformation getTypeUsageInformation();

    public boolean isStatementRecursive();
}

