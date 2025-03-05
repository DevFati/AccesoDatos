/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities;

import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import org.benf.cfr.reader.entities.attributes.AttributeMap;

public enum AccessFlag {
    ACC_PUBLIC("public"),
    ACC_PRIVATE("private"),
    ACC_PROTECTED("protected"),
    ACC_STATIC("static"),
    ACC_FINAL("final"),
    ACC_SUPER("super"),
    ACC_VOLATILE("volatile"),
    ACC_TRANSIENT("transient"),
    ACC_INTERFACE("interface"),
    ACC_ABSTRACT("abstract"),
    ACC_STRICT("strictfp"),
    ACC_SYNTHETIC("/* synthetic */"),
    ACC_ANNOTATION("/* annotation */"),
    ACC_ENUM("/* enum */"),
    ACC_MODULE("/* module */"),
    ACC_FAKE_SEALED("sealed"),
    ACC_FAKE_NON_SEALED("non-sealed");

    public final String name;

    private AccessFlag(String name) {
        this.name = name;
    }

    public static Set<AccessFlag> build(int raw) {
        TreeSet<AccessFlag> res = new TreeSet<AccessFlag>();
        if (0 != (raw & 1)) {
            res.add(ACC_PUBLIC);
        }
        if (0 != (raw & 2)) {
            res.add(ACC_PRIVATE);
        }
        if (0 != (raw & 4)) {
            res.add(ACC_PROTECTED);
        }
        if (0 != (raw & 8)) {
            res.add(ACC_STATIC);
        }
        if (0 != (raw & 0x10)) {
            res.add(ACC_FINAL);
        }
        if (0 != (raw & 0x20)) {
            res.add(ACC_SUPER);
        }
        if (0 != (raw & 0x40)) {
            res.add(ACC_VOLATILE);
        }
        if (0 != (raw & 0x80)) {
            res.add(ACC_TRANSIENT);
        }
        if (0 != (raw & 0x200)) {
            res.add(ACC_INTERFACE);
        }
        if (0 != (raw & 0x400)) {
            res.add(ACC_ABSTRACT);
        }
        if (0 != (raw & 0x1000)) {
            res.add(ACC_SYNTHETIC);
        }
        if (0 != (raw & 0x2000)) {
            res.add(ACC_ANNOTATION);
        }
        if (0 != (raw & 0x4000)) {
            res.add(ACC_ENUM);
        }
        if (0 != (raw & 0x8000)) {
            res.add(ACC_MODULE);
        }
        if (res.isEmpty()) {
            return res;
        }
        return EnumSet.copyOf(res);
    }

    public String toString() {
        return this.name;
    }

    public static void applyAttributes(AttributeMap attributeMap, Set<AccessFlag> accessFlagSet) {
        if (attributeMap.containsKey("Synthetic")) {
            accessFlagSet.add(ACC_SYNTHETIC);
        }
    }
}

