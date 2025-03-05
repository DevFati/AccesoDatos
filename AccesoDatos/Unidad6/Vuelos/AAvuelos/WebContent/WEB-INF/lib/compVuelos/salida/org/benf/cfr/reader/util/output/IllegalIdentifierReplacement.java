/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.variables.Keywords;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;

public class IllegalIdentifierReplacement
implements IllegalIdentifierDump {
    private final Map<String, Integer> identifiers = MapFactory.newMap();
    private final Map<String, String> classes = MapFactory.newMap();
    private static final Map<String, Boolean> known = MapFactory.newIdentityMap();
    private int next = 0;
    private static final IllegalIdentifierReplacement instance = new IllegalIdentifierReplacement();

    private IllegalIdentifierReplacement() {
    }

    private String renamedIdent(Integer key) {
        return "cfr_renamed_" + key;
    }

    private static boolean isIllegalIdentifier(String identifier) {
        if (Keywords.isAKeyword(identifier)) {
            return true;
        }
        if (identifier.length() == 0) {
            return false;
        }
        char[] chars = identifier.toCharArray();
        if (!Character.isJavaIdentifierStart(chars[0])) {
            return true;
        }
        for (int x = 1; x < chars.length; ++x) {
            char c = chars[x];
            if (Character.isJavaIdentifierPart(c) && !Character.isIdentifierIgnorable(c)) continue;
            return true;
        }
        return false;
    }

    public static boolean isIllegal(String identifier) {
        if (!IllegalIdentifierReplacement.isIllegalIdentifier(identifier)) {
            return false;
        }
        if (identifier.endsWith(".this")) {
            return false;
        }
        return !known.containsKey(identifier);
    }

    public static boolean isIllegalMethodName(String name) {
        if (name.equals("<init>")) {
            return false;
        }
        if (name.equals("<clinit>")) {
            return false;
        }
        return IllegalIdentifierReplacement.isIllegal(name);
    }

    @Override
    public String getLegalIdentifierFor(String identifier) {
        Integer idx = this.identifiers.get(identifier);
        if (idx != null) {
            if (idx == -1) {
                return identifier;
            }
            return this.renamedIdent(idx);
        }
        if (IllegalIdentifierReplacement.isIllegal(identifier)) {
            idx = this.next++;
            this.identifiers.put(identifier, idx);
            return this.renamedIdent(idx);
        }
        this.identifiers.put(identifier, -1);
        return identifier;
    }

    @Override
    public String getLegalShortName(String shortName) {
        String key = this.classes.get(shortName);
        if (key != null) {
            if (key.isEmpty()) {
                return shortName;
            }
            return key;
        }
        if (IllegalIdentifierReplacement.isIllegal(shortName)) {
            String testPrefix = "_" + shortName;
            String replace = IllegalIdentifierReplacement.isIllegal(testPrefix) ? "CfrRenamed" + this.classes.size() : testPrefix;
            this.classes.put(shortName, replace);
            return replace;
        }
        this.classes.put(shortName, "");
        return shortName;
    }

    public static IllegalIdentifierReplacement getInstance() {
        return instance;
    }

    static {
        known.put("this", true);
        known.put("new", true);
    }
}

