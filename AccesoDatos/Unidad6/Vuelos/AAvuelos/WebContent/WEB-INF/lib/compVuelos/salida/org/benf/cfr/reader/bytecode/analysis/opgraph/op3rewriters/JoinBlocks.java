/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.util.collections.SetFactory;

class JoinBlocks {
    JoinBlocks() {
    }

    static void rejoinBlocks(List<Op03SimpleStatement> statements) {
        Set<Object> lastBlocks = SetFactory.newSet();
        Set haveLeft = SetFactory.newSet();
        Set blackListed = SetFactory.newSet();
        int len = statements.size();
        for (int x = 0; x < len; ++x) {
            Op03SimpleStatement stm = statements.get(x);
            Statement stmInner = stm.getStatement();
            if (stmInner instanceof CatchStatement) {
                CatchStatement catchStatement = (CatchStatement)stmInner;
                for (ExceptionGroup.Entry entry : catchStatement.getExceptions()) {
                    blackListed.add(entry.getTryBlockIdentifier());
                }
            }
            Set<BlockIdentifier> blocks = stm.getBlockIdentifiers();
            blocks.removeAll(blackListed);
            for (BlockIdentifier ident : blocks) {
                Op03SimpleStatement backFill;
                if (!haveLeft.contains(ident)) continue;
                for (int y = x - 1; y >= 0 && (backFill = statements.get(y)).getBlockIdentifiers().add(ident); --y) {
                }
            }
            for (BlockIdentifier wasIn : lastBlocks) {
                if (blocks.contains(wasIn)) continue;
                haveLeft.add(wasIn);
            }
            lastBlocks = blocks;
        }
    }
}

