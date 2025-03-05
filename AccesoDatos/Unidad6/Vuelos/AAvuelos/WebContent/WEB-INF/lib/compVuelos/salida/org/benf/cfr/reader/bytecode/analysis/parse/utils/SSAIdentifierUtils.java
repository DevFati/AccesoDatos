/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import java.util.Collection;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;

public class SSAIdentifierUtils {
    public static boolean isMovableUnder(Collection<LValue> lValues, LValue lValueMove, SSAIdentifiers atTarget, SSAIdentifiers atSource) {
        for (LValue lValue : lValues) {
            if (atTarget.isValidReplacement(lValue, atSource)) continue;
            return false;
        }
        SSAIdent afterSrc = atSource.getSSAIdentOnExit(lValueMove);
        if (afterSrc == null) {
            return false;
        }
        SSAIdent beforeTarget = atTarget.getSSAIdentOnEntry(lValueMove);
        if (beforeTarget == null) {
            return false;
        }
        if (beforeTarget.isSuperSet(afterSrc)) {
            return true;
        }
        SSAIdent afterTarget = atTarget.getSSAIdentOnExit(lValueMove);
        return beforeTarget.equals(afterSrc) && afterTarget.equals(afterSrc);
    }
}

