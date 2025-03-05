/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AccountingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentAndAliasCondenser;

public class LValueProp {
    public static void condenseLValues(List<Op03SimpleStatement> statements) {
        AccountingRewriter accountingRewriter = new AccountingRewriter();
        for (Op03SimpleStatement op03SimpleStatement : statements) {
            op03SimpleStatement.rewrite(accountingRewriter);
        }
        accountingRewriter.flush();
        LValueAssignmentAndAliasCondenser lValueAssigmentCollector = new LValueAssignmentAndAliasCondenser();
        for (Op03SimpleStatement op03SimpleStatement : statements) {
            op03SimpleStatement.collect(lValueAssigmentCollector);
        }
        LValueAssignmentAndAliasCondenser.MutationRewriterFirstPass mutationRewriterFirstPass = lValueAssigmentCollector.getMutationRewriterFirstPass();
        if (mutationRewriterFirstPass != null) {
            for (Op03SimpleStatement statement : statements) {
                statement.condense(mutationRewriterFirstPass);
            }
            LValueAssignmentAndAliasCondenser.MutationRewriterSecondPass mutationRewriterSecondPass = mutationRewriterFirstPass.getSecondPassRewriter();
            if (mutationRewriterSecondPass != null) {
                for (Op03SimpleStatement statement : statements) {
                    statement.condense(mutationRewriterSecondPass);
                }
            }
            lValueAssigmentCollector = new LValueAssignmentAndAliasCondenser();
            for (Op03SimpleStatement statement : statements) {
                statement.collect(lValueAssigmentCollector);
            }
        }
        LValueAssignmentAndAliasCondenser.AliasRewriter aliasRewriter = lValueAssigmentCollector.getAliasRewriter();
        for (Op03SimpleStatement statement : statements) {
            statement.condense(aliasRewriter);
        }
        aliasRewriter.inferAliases();
        for (Op03SimpleStatement statement : statements) {
            lValueAssigmentCollector.reset();
            statement.condense(lValueAssigmentCollector);
        }
    }
}

