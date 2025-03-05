/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AnonBreakTarget;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;

public class AnonymousBlocks {
    public static void labelAnonymousBlocks(List<Op03SimpleStatement> statements, BlockIdentifierFactory blockIdentifierFactory) {
        List<Op03SimpleStatement> anonBreaks = Functional.filter(statements, new Predicate<Op03SimpleStatement>(){

            @Override
            public boolean test(Op03SimpleStatement in) {
                Statement statement = in.getStatement();
                if (!(statement instanceof JumpingStatement)) {
                    return false;
                }
                JumpType jumpType = ((JumpingStatement)statement).getJumpType();
                return jumpType == JumpType.BREAK_ANONYMOUS;
            }
        });
        if (anonBreaks.isEmpty()) {
            return;
        }
        Set<Op03SimpleStatement> targets = SetFactory.newOrderedSet();
        for (Op03SimpleStatement anonBreak : anonBreaks) {
            JumpingStatement jumpingStatement = (JumpingStatement)anonBreak.getStatement();
            Op03SimpleStatement anonBreakTarget = (Op03SimpleStatement)jumpingStatement.getJumpTarget().getContainer();
            if (anonBreakTarget.getStatement() instanceof AnonBreakTarget) continue;
            targets.add(anonBreakTarget);
        }
        for (Op03SimpleStatement target : targets) {
            BlockIdentifier blockIdentifier = blockIdentifierFactory.getNextBlockIdentifier(BlockType.ANONYMOUS);
            InstrIndex targetIndex = target.getIndex();
            Op03SimpleStatement anonTarget = new Op03SimpleStatement(target.getBlockIdentifiers(), new AnonBreakTarget(blockIdentifier), targetIndex.justBefore());
            List<Op03SimpleStatement> sources = ListFactory.newList(target.getSources());
            for (Op03SimpleStatement source : sources) {
                if (!targetIndex.isBackJumpTo(source)) continue;
                target.removeSource(source);
                source.replaceTarget(target, anonTarget);
                anonTarget.addSource(source);
            }
            target.addSource(anonTarget);
            anonTarget.addTarget(target);
            int pos = statements.indexOf(target);
            statements.add(pos, anonTarget);
        }
    }
}

