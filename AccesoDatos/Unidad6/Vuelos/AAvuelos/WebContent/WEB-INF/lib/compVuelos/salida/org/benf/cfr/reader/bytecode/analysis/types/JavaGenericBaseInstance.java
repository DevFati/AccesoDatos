/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.types.FormalTypeParameter;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;

public interface JavaGenericBaseInstance
extends JavaTypeInstance {
    public JavaTypeInstance getBoundInstance(GenericTypeBinder var1);

    public boolean tryFindBinding(JavaTypeInstance var1, GenericTypeBinder var2);

    public boolean hasUnbound();

    public boolean hasL01Wildcard();

    public JavaTypeInstance getWithoutL01Wildcard();

    public boolean hasForeignUnbound(ConstantPool var1, int var2, boolean var3, Map<String, FormalTypeParameter> var4);

    public List<JavaTypeInstance> getGenericTypes();
}

