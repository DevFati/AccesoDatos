/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.innerclass.InnerClassAttributeInfo;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public interface ObfuscationTypeMap {
    public boolean providesInnerClassInfo();

    public JavaTypeInstance get(JavaTypeInstance var1);

    public UnaryFunction<JavaTypeInstance, JavaTypeInstance> getter();

    public List<InnerClassAttributeInfo> getInnerClassInfo(JavaTypeInstance var1);
}

