/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.api;

import java.util.NavigableMap;
import java.util.Set;

public interface SinkReturns {

    public static interface Token {
        public TokenType getTokenType();

        public String getText();

        public Object getRawValue();

        public Set<TokenTypeFlags> getFlags();
    }

    public static enum TokenType {
        WHITESPACE,
        KEYWORD,
        OPERATOR,
        SEPARATOR,
        LITERAL,
        COMMENT,
        IDENTIFIER,
        FIELD,
        METHOD,
        LABEL,
        NEWLINE,
        UNCLASSIFIED,
        EOF(true),
        INDENT(true),
        UNINDENT(true),
        EXPLICIT_INDENT;

        private final boolean control;

        private TokenType() {
            this.control = false;
        }

        private TokenType(boolean control) {
            this.control = control;
        }

        public boolean isControl() {
            return this.control;
        }
    }

    public static enum TokenTypeFlags {
        DEFINES;

    }

    public static interface LineNumberMapping {
        public String methodName();

        public String methodDescriptor();

        public NavigableMap<Integer, Integer> getMappings();

        public NavigableMap<Integer, Integer> getClassFileMappings();
    }

    public static interface DecompiledMultiVer
    extends Decompiled {
        public int getRuntimeFrom();
    }

    public static interface Decompiled {
        public String getPackageName();

        public String getClassName();

        public String getJava();
    }

    public static interface ExceptionMessage {
        public String getPath();

        public String getMessage();

        public Exception getThrownException();
    }
}

