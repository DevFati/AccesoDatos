/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.output.Dumper;

public interface ObfuscationRewriter {
    public Dumper wrap(Dumper var1);

    public JavaTypeInstance get(JavaTypeInstance var1);

    public List<JavaTypeInstance> get(List<JavaTypeInstance> var1);
}

