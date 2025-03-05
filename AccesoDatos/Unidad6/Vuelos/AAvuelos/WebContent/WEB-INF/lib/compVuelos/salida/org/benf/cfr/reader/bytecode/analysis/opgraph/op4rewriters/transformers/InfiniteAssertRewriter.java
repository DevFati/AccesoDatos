/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BoolOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NotOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredBlockStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredConditionalLoopStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDo;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredThrow;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredWhile;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;

public class InfiniteAssertRewriter
implements StructuredStatementTransformer {
    private final WildcardMatch wcm1 = new WildcardMatch();
    private final Expression match1;
    private final Expression match2;
    private final StructuredStatement thrw;

    public InfiniteAssertRewriter(StaticVariable assertionStatic) {
        this.match1 = new BooleanExpression(new LValueExpression(assertionStatic));
        this.match2 = new BooleanOperation(BytecodeLoc.TODO, new BooleanExpression(new LValueExpression(assertionStatic)), this.wcm1.getConditionalExpressionWildcard("condition"), BoolOp.OR);
        this.thrw = new StructuredThrow(BytecodeLoc.NONE, this.wcm1.getConstructorSimpleWildcard("ignore", TypeConstants.ASSERTION_ERROR));
    }

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        if (!(in instanceof Block)) {
            return in;
        }
        Block b = (Block)in;
        List<Op04StructuredStatement> content = b.getBlockStatements();
        for (int x = 0; x < content.size() - 1; ++x) {
            ConditionalExpression ce;
            AbstractStructuredConditionalLoopStatement sw;
            StructuredStatement stmInner2;
            Op04StructuredStatement next;
            Op04StructuredStatement stm = content.get(x);
            StructuredStatement stmInner = stm.getStatement();
            if (stmInner instanceof StructuredWhile) {
                next = content.get(x + 1);
                stmInner2 = next.getStatement();
                if (!this.checkThrow(stmInner2)) continue;
                sw = (StructuredWhile)stmInner;
                this.wcm1.reset();
                ce = sw.getCondition();
                if (!this.match1.equals(ce) && !this.match2.equals(ce)) continue;
                this.replaceThrow(next, stm, sw.getBlock(), ce);
                continue;
            }
            if (!(stmInner instanceof StructuredDo) || !this.checkThrow(stmInner2 = (next = content.get(x + 1)).getStatement())) continue;
            sw = (StructuredDo)stmInner;
            this.wcm1.reset();
            ce = sw.getCondition();
            if (!this.match2.equals(ce)) continue;
            this.replaceThrow(next, stm, sw.getBlock(), ce);
        }
        return in;
    }

    private void replaceThrow(Op04StructuredStatement thrw, Op04StructuredStatement whil, BlockIdentifier ident, ConditionalExpression cond) {
        StructuredStatement throwInner = thrw.getStatement();
        AbstractStructuredBlockStatement sw = (AbstractStructuredBlockStatement)whil.getStatement();
        Op04StructuredStatement body = sw.getBody();
        whil.replaceStatement(StructuredDo.create(null, body, ident));
        StructuredStatement bodyContent = body.getStatement();
        if (!(bodyContent instanceof Block)) {
            bodyContent = new Block(new Op04StructuredStatement(bodyContent));
            body.replaceStatement(bodyContent);
        }
        Block bodyBlock = (Block)bodyContent;
        bodyBlock.addStatement(new Op04StructuredStatement(new StructuredIf(BytecodeLoc.TODO, new NotOperation(BytecodeLoc.TODO, cond), new Op04StructuredStatement(new Block(new Op04StructuredStatement(throwInner))))));
        thrw.nopOut();
    }

    private boolean checkThrow(StructuredStatement thrw) {
        return this.thrw.equals(thrw);
    }
}

