/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.annotations;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.output.Dumpable;

public interface ElementValue
extends Dumpable,
TypeUsageCollectable {
    public ElementValue withTypeHint(JavaTypeInstance var1);
}

