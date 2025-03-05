/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.loc;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocSpecific;
import org.benf.cfr.reader.entities.Method;

public interface BytecodeLocFactory {
    public static final BytecodeLoc DISABLED = new BytecodeLocSpecific(BytecodeLocSpecific.Specific.DISABLED);
    public static final BytecodeLoc NONE = new BytecodeLocSpecific(BytecodeLocSpecific.Specific.NONE);
    public static final BytecodeLoc TODO = new BytecodeLocSpecific(BytecodeLocSpecific.Specific.TODO);

    public BytecodeLoc at(int var1, Method var2);
}

