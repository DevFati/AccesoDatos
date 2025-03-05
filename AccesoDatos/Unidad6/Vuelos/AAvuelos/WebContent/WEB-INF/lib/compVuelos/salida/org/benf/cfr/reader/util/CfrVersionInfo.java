/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util;

public class CfrVersionInfo {
    public static final String VERSION = "0.152";
    public static final boolean SNAPSHOT = "0.152".contains("SNAPSHOT");
    public static final String GIT_COMMIT_ABBREVIATED = "68477be";
    public static final boolean GIT_IS_DIRTY = "false".equals("true");
    public static final String VERSION_INFO = "0.152" + (SNAPSHOT ? " (68477be" + (GIT_IS_DIRTY ? "-dirty" : "") + ")" : "");

    private CfrVersionInfo() {
    }
}

