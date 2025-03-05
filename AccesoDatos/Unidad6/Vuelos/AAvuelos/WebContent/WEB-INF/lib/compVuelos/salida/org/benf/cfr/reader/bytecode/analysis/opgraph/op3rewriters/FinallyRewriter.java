/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.FinallyStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp.FinalAnalyzer;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class FinallyRewriter {
    public static void identifyFinally(Options options, Method method, List<Op03SimpleStatement> in, BlockIdentifierFactory blockIdentifierFactory) {
        List<Op03SimpleStatement> tryStarts;
        boolean continueLoop;
        if (!((Boolean)options.getOption(OptionsImpl.DECODE_FINALLY)).booleanValue()) {
            return;
        }
        final Set<Op03SimpleStatement> analysedTries = SetFactory.newSet();
        do {
            tryStarts = Functional.filter(in, new Predicate<Op03SimpleStatement>(){

                @Override
                public boolean test(Op03SimpleStatement in) {
                    return in.getStatement() instanceof TryStatement && !analysedTries.contains(in);
                }
            });
            for (Op03SimpleStatement tryS : tryStarts) {
                FinalAnalyzer.identifyFinally(method, tryS, in, blockIdentifierFactory, analysedTries);
            }
        } while (continueLoop = !tryStarts.isEmpty());
    }

    static Set<BlockIdentifier> getBlocksAffectedByFinally(List<Op03SimpleStatement> statements) {
        Set<BlockIdentifier> res = SetFactory.newSet();
        for (Op03SimpleStatement stm : statements) {
            if (!(stm.getStatement() instanceof TryStatement)) continue;
            TryStatement tryStatement = (TryStatement)stm.getStatement();
            Set newBlocks = SetFactory.newSet();
            boolean found = false;
            newBlocks.add(tryStatement.getBlockIdentifier());
            for (Op03SimpleStatement tgt : stm.getTargets()) {
                Statement inr = tgt.getStatement();
                if (inr instanceof CatchStatement) {
                    newBlocks.add(((CatchStatement)inr).getCatchBlockIdent());
                }
                if (!(tgt.getStatement() instanceof FinallyStatement)) continue;
                found = true;
            }
            if (!found) continue;
            res.addAll(newBlocks);
        }
        return res;
    }
}

