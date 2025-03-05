/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.loc;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;

public interface HasByteCodeLoc {
    public BytecodeLoc getCombinedLoc();

    public BytecodeLoc getLoc();

    public void addLoc(HasByteCodeLoc var1);
}

