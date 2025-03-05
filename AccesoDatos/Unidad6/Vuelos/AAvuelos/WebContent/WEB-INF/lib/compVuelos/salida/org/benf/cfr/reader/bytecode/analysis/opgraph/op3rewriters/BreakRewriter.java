/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;

public class BreakRewriter {
    public static void rewriteBreakStatements(List<Op03SimpleStatement> statements) {
        Cleaner.reindexInPlace(statements);
        for (Op03SimpleStatement statement : statements) {
            BlockIdentifier outermostContainedIn;
            JumpingStatement jumpingStatement;
            Statement innerStatement = statement.getStatement();
            if (!(innerStatement instanceof JumpingStatement) || !(jumpingStatement = (JumpingStatement)innerStatement).getJumpType().isUnknown()) continue;
            Statement targetInnerStatement = jumpingStatement.getJumpTarget();
            Op03SimpleStatement targetStatement = (Op03SimpleStatement)targetInnerStatement.getContainer();
            if (targetStatement.getThisComparisonBlock() != null) {
                BlockType blockType = targetStatement.getThisComparisonBlock().getBlockType();
                switch (blockType) {
                    default: 
                }
                if (BlockIdentifier.blockIsOneOf(targetStatement.getThisComparisonBlock(), statement.getBlockIdentifiers())) {
                    jumpingStatement.setJumpType(JumpType.CONTINUE);
                    continue;
                }
            }
            if (targetStatement.getBlockStarted() != null && targetStatement.getBlockStarted().getBlockType() == BlockType.UNCONDITIONALDOLOOP && BlockIdentifier.blockIsOneOf(targetStatement.getBlockStarted(), statement.getBlockIdentifiers())) {
                jumpingStatement.setJumpType(JumpType.CONTINUE);
                continue;
            }
            Set<BlockIdentifier> blocksEnded = targetStatement.getBlocksEnded();
            if (blocksEnded.isEmpty() || (outermostContainedIn = BlockIdentifier.getOutermostContainedIn(blocksEnded, statement.getBlockIdentifiers())) == null) continue;
            jumpingStatement.setJumpType(JumpType.BREAK);
        }
    }
}

