/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ComparableUnderEC;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.DefaultEquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;

public class StatementEquivalenceConstraint
extends DefaultEquivalenceConstraint {
    private final SSAIdentifiers<LValue> ident1;
    private final SSAIdentifiers<LValue> ident2;

    public StatementEquivalenceConstraint(Op03SimpleStatement stm1, Op03SimpleStatement stm2) {
        this.ident1 = stm1.getSSAIdentifiers();
        this.ident2 = stm2.getSSAIdentifiers();
    }

    @Override
    public boolean equivalent(ComparableUnderEC o1, ComparableUnderEC o2) {
        if (o1 instanceof LValue && o2 instanceof LValue) {
            SSAIdent i1 = this.ident1.getSSAIdentOnEntry((LValue)((Object)o1));
            SSAIdent i2 = this.ident2.getSSAIdentOnEntry((LValue)((Object)o2));
            if (i1 == null ? i2 != null : !i1.equals(i2)) {
                return false;
            }
        }
        return super.equivalent(o1, o2);
    }
}

