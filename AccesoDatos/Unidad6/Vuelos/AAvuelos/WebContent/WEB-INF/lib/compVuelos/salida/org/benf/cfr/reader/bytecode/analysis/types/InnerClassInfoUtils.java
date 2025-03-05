/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;

public class InnerClassInfoUtils {
    public static JavaRefTypeInstance getTransitiveOuterClass(JavaRefTypeInstance type) {
        while (type.getInnerClassHereInfo().isInnerClass()) {
            type = type.getInnerClassHereInfo().getOuterClass();
        }
        return type;
    }
}

