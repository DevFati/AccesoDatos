/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

public class QuotingUtils {
    public static String enquoteUTF(String s) {
        char[] raw = s.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : raw) {
            if (c < ' ' || c > '~') {
                stringBuilder.append("\\u").append(String.format("%04x", c));
                continue;
            }
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    public static String enquoteString(String s) {
        char[] raw = s.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"");
        block9: for (char c : raw) {
            switch (c) {
                case '\n': {
                    stringBuilder.append("\\n");
                    continue block9;
                }
                case '\r': {
                    stringBuilder.append("\\r");
                    continue block9;
                }
                case '\t': {
                    stringBuilder.append("\\t");
                    continue block9;
                }
                case '\b': {
                    stringBuilder.append("\\b");
                    continue block9;
                }
                case '\f': {
                    stringBuilder.append("\\f");
                    continue block9;
                }
                case '\\': {
                    stringBuilder.append("\\\\");
                    continue block9;
                }
                case '\"': {
                    stringBuilder.append("\\\"");
                    continue block9;
                }
                default: {
                    stringBuilder.append(c);
                }
            }
        }
        stringBuilder.append("\"");
        return stringBuilder.toString();
    }

    public static String unquoteString(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    public static String addQuotes(String s, boolean singleIsChar) {
        if (singleIsChar && s.length() == 1) {
            return "'" + s + "'";
        }
        return '\"' + s + '\"';
    }
}

