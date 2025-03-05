/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.opcode;

import java.util.List;
import org.benf.cfr.reader.bytecode.opcode.DecodedSwitchEntry;

public interface DecodedSwitch {
    public List<DecodedSwitchEntry> getJumpTargets();
}

