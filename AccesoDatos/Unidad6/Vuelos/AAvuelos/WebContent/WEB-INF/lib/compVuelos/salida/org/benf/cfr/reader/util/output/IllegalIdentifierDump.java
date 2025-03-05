/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.IllegalIdentifierReplacement;

public interface IllegalIdentifierDump {
    public String getLegalIdentifierFor(String var1);

    public String getLegalShortName(String var1);

    public static class Factory {
        public static IllegalIdentifierDump get(Options options) {
            if (((Boolean)options.getOption(OptionsImpl.RENAME_ILLEGAL_IDENTS)).booleanValue()) {
                return IllegalIdentifierReplacement.getInstance();
            }
            return Nop.getInstance();
        }

        public static IllegalIdentifierDump getOrNull(Options options) {
            if (((Boolean)options.getOption(OptionsImpl.RENAME_ILLEGAL_IDENTS)).booleanValue()) {
                return IllegalIdentifierReplacement.getInstance();
            }
            return null;
        }
    }

    public static class Nop
    implements IllegalIdentifierDump {
        private static final IllegalIdentifierDump INSTANCE = new Nop();

        public static IllegalIdentifierDump getInstance() {
            return INSTANCE;
        }

        @Override
        public String getLegalIdentifierFor(String identifier) {
            return identifier;
        }

        @Override
        public String getLegalShortName(String shortName) {
            return shortName;
        }
    }
}

