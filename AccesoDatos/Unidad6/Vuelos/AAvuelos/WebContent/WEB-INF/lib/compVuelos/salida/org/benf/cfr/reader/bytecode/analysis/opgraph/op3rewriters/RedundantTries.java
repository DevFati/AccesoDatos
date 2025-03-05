/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;

public class RedundantTries {
    public static List<Op03SimpleStatement> removeRedundantTries(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> tryStarts = Functional.filter(statements, new TypeFilter<TryStatement>(TryStatement.class));
        boolean effect = false;
        Collections.reverse(tryStarts);
        LinkedList starts = ListFactory.newLinkedList();
        starts.addAll(tryStarts);
        while (!starts.isEmpty()) {
            Op03SimpleStatement trys = (Op03SimpleStatement)starts.removeFirst();
            Statement stm = trys.getStatement();
            if (!(stm instanceof TryStatement)) continue;
            TryStatement tryStatement = (TryStatement)stm;
            BlockIdentifier tryBlock = tryStatement.getBlockIdentifier();
            if (!trys.getTargets().isEmpty() && trys.getTargets().get(0).getBlockIdentifiers().contains(tryBlock)) continue;
            Op03SimpleStatement codeTarget = trys.getTargets().get(0);
            for (Op03SimpleStatement target : trys.getTargets()) {
                target.removeSource(trys);
            }
            trys.getTargets().clear();
            for (Op03SimpleStatement source : trys.getSources()) {
                source.replaceTarget(trys, codeTarget);
                codeTarget.addSource(source);
            }
            trys.getSources().clear();
            effect = true;
        }
        if (effect) {
            statements = Cleaner.removeUnreachableCode(statements, false);
            statements = Cleaner.sortAndRenumber(statements);
        }
        return statements;
    }
}

