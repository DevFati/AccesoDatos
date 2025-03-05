/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Collections;
import java.util.Set;
import org.benf.cfr.reader.util.collections.SetFactory;

public class OsInfo {
    public static OS OS() {
        String osname = System.getProperty("os.name", "").toLowerCase();
        if (osname.contains("windows")) {
            return OS.WINDOWS;
        }
        if (osname.contains("mac")) {
            return OS.OSX;
        }
        return OS.OTHER;
    }

    public static enum OS {
        WINDOWS(true, SetFactory.newSet("con", "aux", "prn", "nul", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "conin$", "conout$")),
        OSX(true, Collections.<String>emptySet()),
        OTHER(false, Collections.<String>emptySet());

        private boolean caseInsensitive;
        private Set<String> illegalNames;

        private OS(boolean caseInsensitive, Set<String> illegalNames) {
            this.caseInsensitive = caseInsensitive;
            this.illegalNames = illegalNames;
        }

        public boolean isCaseInsensitive() {
            return this.caseInsensitive;
        }

        public Set<String> getIllegalNames() {
            return this.illegalNames;
        }
    }
}

