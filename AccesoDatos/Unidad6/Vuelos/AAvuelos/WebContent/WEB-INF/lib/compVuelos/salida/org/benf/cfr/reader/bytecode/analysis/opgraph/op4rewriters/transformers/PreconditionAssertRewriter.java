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
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.util.collections.ListFactory;

public class PreconditionAssertRewriter
implements StructuredStatementTransformer {
    private Expression test;

    public PreconditionAssertRewriter(StaticVariable assertionStatic) {
        this.test = new NotOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(assertionStatic)));
    }

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        if (in instanceof StructuredIf) {
            in = this.transformAssertIf((StructuredIf)in);
        }
        return in;
    }

    private StructuredStatement transformAssertIf(StructuredIf in) {
        List<ConditionalExpression> cnf;
        if (in.hasElseBlock()) {
            return in;
        }
        ConditionalExpression expression = in.getConditionalExpression();
        if (expression instanceof NotOperation) {
            expression = expression.getDemorganApplied(false);
        }
        if ((cnf = this.getFlattenedCNF(expression)).size() < 2) {
            return in;
        }
        for (int x = 0; x < cnf.size(); ++x) {
            if (!this.test.equals(cnf.get(x))) continue;
            if (x == 0) {
                return in;
            }
            ConditionalExpression c1 = BooleanOperation.makeRightDeep(cnf.subList(0, x), BoolOp.AND);
            ConditionalExpression c2 = BooleanOperation.makeRightDeep(cnf.subList(x, cnf.size()), BoolOp.AND);
            return new StructuredIf(BytecodeLoc.TODO, c1, new Op04StructuredStatement(new StructuredIf(BytecodeLoc.TODO, c2, in.getIfTaken())));
        }
        return in;
    }

    private List<ConditionalExpression> getFlattenedCNF(ConditionalExpression ce) {
        List<ConditionalExpression> accum = ListFactory.newList();
        this.getFlattenedCNF(ce, accum);
        return accum;
    }

    private void getFlattenedCNF(ConditionalExpression ce, List<ConditionalExpression> accum) {
        BooleanOperation bo;
        if (ce instanceof BooleanOperation && (bo = (BooleanOperation)ce).getOp() == BoolOp.AND) {
            this.getFlattenedCNF(bo.getLhs(), accum);
            this.getFlattenedCNF(bo.getRhs(), accum);
            return;
        }
        accum.add(ce);
    }
}

