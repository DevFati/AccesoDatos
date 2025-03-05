/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;

public interface LValueRewriter<T> {
    public Expression getLValueReplacement(LValue var1, SSAIdentifiers<LValue> var2, StatementContainer<T> var3);

    public boolean explicitlyReplaceThisLValue(LValue var1);

    public void checkPostConditions(LValue var1, Expression var2);

    public LValueRewriter getWithFixed(Set<SSAIdent> var1);

    public boolean needLR();

    public LValueRewriter<T> keepConstant(Collection<LValue> var1);

    public static class Util {
        public static void rewriteArgArray(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, List<Expression> args) {
            boolean lr = lValueRewriter.needLR();
            int argsSize = args.size();
            for (int x = 0; x < argsSize; ++x) {
                int y = lr ? x : argsSize - 1 - x;
                args.set(y, args.get(y).replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer));
            }
        }
    }
}

