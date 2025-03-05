/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.getopt;

import java.util.Map;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

public interface GetOptSinkFactory<T>
extends PermittedOptionProvider {
    public T create(Map<String, String> var1);
}

