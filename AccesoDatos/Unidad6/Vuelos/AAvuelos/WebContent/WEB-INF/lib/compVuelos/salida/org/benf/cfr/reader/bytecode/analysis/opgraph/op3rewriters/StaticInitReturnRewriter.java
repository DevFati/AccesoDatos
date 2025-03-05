/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnNothingStatement;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class StaticInitReturnRewriter {
    public static List<Op03SimpleStatement> rewrite(Options options, Method method, List<Op03SimpleStatement> statementList) {
        if (!method.getName().equals("<clinit>")) {
            return statementList;
        }
        if (!((Boolean)options.getOption(OptionsImpl.STATIC_INIT_RETURN)).booleanValue()) {
            return statementList;
        }
        Op03SimpleStatement last = statementList.get(statementList.size() - 1);
        if (last.getStatement().getClass() != ReturnNothingStatement.class) {
            return statementList;
        }
        int len = statementList.size() - 1;
        for (int x = 0; x < len; ++x) {
            Op03SimpleStatement stm = statementList.get(x);
            if (stm.getStatement().getClass() != ReturnNothingStatement.class) continue;
            stm.replaceStatement(new GotoStatement(BytecodeLoc.TODO));
            stm.addTarget(last);
            last.addSource(stm);
        }
        return statementList;
    }
}

