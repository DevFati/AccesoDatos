/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util;

public enum Troolean {
    NEITHER,
    TRUE,
    FALSE;


    public static Troolean get(Boolean a) {
        if (a == null) {
            return NEITHER;
        }
        return a != false ? TRUE : FALSE;
    }

    public boolean boolValue(boolean ifNeither) {
        switch (this) {
            case TRUE: {
                return true;
            }
            case FALSE: {
                return false;
            }
        }
        return ifNeither;
    }
}

