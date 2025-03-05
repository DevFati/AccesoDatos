/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredConditionalLoopStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredWhile;

public class BadLoopPrettifier
implements StructuredStatementTransformer {
    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    private List<Op04StructuredStatement> getIfBlock(Op04StructuredStatement maybeBlock) {
        StructuredStatement bodyStatement = maybeBlock.getStatement();
        if (!(bodyStatement instanceof Block)) {
            return null;
        }
        Block block = (Block)bodyStatement;
        return block.getBlockStatements();
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        if (!(in instanceof AbstractStructuredConditionalLoopStatement)) {
            return in;
        }
        AbstractStructuredConditionalLoopStatement asl = (AbstractStructuredConditionalLoopStatement)in;
        if (!asl.isInfinite()) {
            return in;
        }
        Op04StructuredStatement body = asl.getBody();
        BlockIdentifier blockIdent = asl.getBlock();
        List<Op04StructuredStatement> statements = this.getIfBlock(body);
        if (statements == null || statements.isEmpty()) {
            return in;
        }
        Op04StructuredStatement statement1 = statements.get(0);
        if (!(statement1.getStatement() instanceof StructuredIf)) {
            return in;
        }
        StructuredIf ifStatement = (StructuredIf)statement1.getStatement();
        if (ifStatement.hasElseBlock()) {
            return in;
        }
        List<Op04StructuredStatement> ifStatements = this.getIfBlock(ifStatement.getIfTaken());
        if (ifStatements == null || ifStatements.size() != 1) {
            return in;
        }
        Op04StructuredStatement exitStatement = ifStatements.get(0);
        StructuredStatement structuredExit = exitStatement.getStatement();
        boolean liftTestBody = false;
        if (structuredExit instanceof StructuredBreak) {
            StructuredBreak breakStatement = (StructuredBreak)structuredExit;
            if (!breakStatement.getBreakBlock().equals(blockIdent)) {
                return in;
            }
        } else if (structuredExit instanceof StructuredReturn) {
            Set<Op04StructuredStatement> fallthrough = scope.getNextFallThrough(in);
            if (!fallthrough.isEmpty()) {
                return in;
            }
            liftTestBody = true;
        } else {
            return in;
        }
        statements.remove(0);
        ConditionalExpression newCondition = ifStatement.getConditionalExpression().getNegated().simplify();
        StructuredWhile structuredWhile = new StructuredWhile(newCondition, body, blockIdent);
        if (!liftTestBody) {
            return structuredWhile;
        }
        Block lifted = Block.getBlockFor(false, structuredWhile, structuredExit);
        return lifted;
    }
}

