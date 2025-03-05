/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.innerclass;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.util.annotation.Nullable;

public class InnerClassAttributeInfo {
    @Nullable
    private final JavaTypeInstance innerClassInfo;
    @Nullable
    private final JavaTypeInstance outerClassInfo;
    @Nullable
    private final String innerName;
    private final Set<AccessFlag> accessFlags;

    public InnerClassAttributeInfo(JavaTypeInstance innerClassInfo, JavaTypeInstance outerClassInfo, String innerName, Set<AccessFlag> accessFlags) {
        this.innerClassInfo = innerClassInfo;
        this.outerClassInfo = outerClassInfo;
        this.innerName = innerName;
        this.accessFlags = accessFlags;
    }

    public JavaTypeInstance getInnerClassInfo() {
        return this.innerClassInfo;
    }

    private JavaTypeInstance getOuterClassInfo() {
        return this.outerClassInfo;
    }

    private String getInnerName() {
        return this.innerName;
    }

    public Set<AccessFlag> getAccessFlags() {
        return this.accessFlags;
    }
}

