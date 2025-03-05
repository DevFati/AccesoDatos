/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.opcode;

import java.util.List;
import org.benf.cfr.reader.util.StringUtils;

public class DecodedSwitchEntry {
    private final List<Integer> value;
    private final int bytecodeTarget;

    public DecodedSwitchEntry(List<Integer> value, int bytecodeTarget) {
        this.bytecodeTarget = bytecodeTarget;
        this.value = value;
    }

    public List<Integer> getValue() {
        return this.value;
    }

    int getBytecodeTarget() {
        return this.bytecodeTarget;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append("case ");
        for (Integer val : this.value) {
            first = StringUtils.comma(first, sb);
            sb.append(val == null ? "default" : val);
        }
        sb.append(" -> ").append(this.bytecodeTarget);
        return sb.toString();
    }

    public boolean hasDefault() {
        return this.value.contains(null);
    }
}

