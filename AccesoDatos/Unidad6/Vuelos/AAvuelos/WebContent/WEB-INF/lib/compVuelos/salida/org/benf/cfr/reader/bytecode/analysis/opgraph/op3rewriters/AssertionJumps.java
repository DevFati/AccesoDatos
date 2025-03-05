/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ThrowStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;

class AssertionJumps {
    AssertionJumps() {
    }

    static void extractAssertionJumps(List<Op03SimpleStatement> in) {
        WildcardMatch wcm = new WildcardMatch();
        ThrowStatement assertionError = new ThrowStatement(BytecodeLoc.TODO, wcm.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR));
        int len = in.size();
        for (int x = 0; x < len; ++x) {
            Op03SimpleStatement next;
            IfStatement ifStatement;
            Op03SimpleStatement ostm = in.get(x);
            Statement stm = ostm.getStatement();
            if (stm.getClass() != IfStatement.class || (ifStatement = (IfStatement)stm).getJumpType() == JumpType.GOTO || (next = in.get(x + 1)).getSources().size() != 1) continue;
            wcm.reset();
            if (!((Object)assertionError).equals(next.getStatement()) || !ostm.getBlockIdentifiers().equals(next.getBlockIdentifiers())) continue;
            GotoStatement reJumpStm = new GotoStatement(BytecodeLoc.TODO);
            reJumpStm.setJumpType(ifStatement.getJumpType());
            Op03SimpleStatement reJump = new Op03SimpleStatement(ostm.getBlockIdentifiers(), reJumpStm, next.getIndex().justAfter());
            in.add(x + 2, reJump);
            Op03SimpleStatement origTarget = ostm.getTargets().get(1);
            ostm.replaceTarget(origTarget, reJump);
            reJump.addSource(ostm);
            origTarget.replaceSource(ostm, reJump);
            reJump.addTarget(origTarget);
            ifStatement.setJumpType(JumpType.GOTO);
            ++len;
        }
    }
}

