/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import org.benf.cfr.reader.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import org.benf.cfr.reader.util.DecompilerComments;

public interface TypePathPart {
    public JavaAnnotatedTypeIterator apply(JavaAnnotatedTypeIterator var1, DecompilerComments var2);
}

