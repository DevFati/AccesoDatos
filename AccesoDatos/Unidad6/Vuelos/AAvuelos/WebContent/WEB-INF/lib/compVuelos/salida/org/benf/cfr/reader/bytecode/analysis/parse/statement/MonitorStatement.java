/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractStatement;

public abstract class MonitorStatement
extends AbstractStatement {
    public MonitorStatement(BytecodeLoc loc) {
        super(loc);
    }
}

