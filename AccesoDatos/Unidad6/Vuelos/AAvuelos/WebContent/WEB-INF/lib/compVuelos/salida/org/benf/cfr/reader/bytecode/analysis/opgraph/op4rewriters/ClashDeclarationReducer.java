/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ExpressionReplacingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.collections.ListFactory;

public class ClashDeclarationReducer
extends AbstractExpressionRewriter
implements StructuredStatementTransformer {
    private final Set<Integer> clashes;

    public ClashDeclarationReducer(Set<Integer> clashes) {
        this.clashes = clashes;
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        if (in instanceof Block) {
            this.transformBlock((Block)in);
        }
        in.transformStructuredChildren(this, scope);
        return in;
    }

    private void transformBlock(Block in) {
        List<Op04StructuredStatement> statements = in.getBlockStatements();
        for (int x = statements.size() - 1; x > 0; --x) {
            int slot;
            StructuredAssignment sa;
            LValue lv;
            Op04StructuredStatement stm = statements.get(x);
            StructuredStatement s = stm.getStatement();
            if (!(s instanceof StructuredAssignment) || !((lv = (sa = (StructuredAssignment)s).getLvalue()) instanceof LocalVariable) || !this.clashes.contains(slot = ((LocalVariable)lv).getIdx())) continue;
            List<LValue> replaceThese = ListFactory.newList();
            List<Op04StructuredStatement> inThese = ListFactory.newList();
            inThese.add(stm);
            x = 1 + this.goBack(x - 1, statements, lv.getInferredJavaType().getJavaTypeInstance(), slot, replaceThese, inThese);
            if (replaceThese.isEmpty()) continue;
            this.doReplace(lv, replaceThese, inThese);
        }
    }

    private void doReplace(LValue lv, List<LValue> replaceThese, List<Op04StructuredStatement> inThese) {
        for (int x = 0; x < inThese.size() - 1; ++x) {
            LValue replaceThis = replaceThese.get(x);
            Op04StructuredStatement inThis = inThese.get(x);
            ExpressionReplacingRewriter err = new ExpressionReplacingRewriter(new LValueExpression(replaceThis), new LValueExpression(lv));
            StructuredAssignment statement = (StructuredAssignment)inThis.getStatement();
            statement.rewriteExpressions(err);
            inThis.replaceStatement(new StructuredAssignment(BytecodeLoc.TODO, lv, statement.getRvalue()));
        }
        Op04StructuredStatement last = inThese.get(inThese.size() - 1);
        StructuredAssignment structuredAssignment = (StructuredAssignment)last.getStatement();
        last.replaceStatement(new StructuredAssignment(BytecodeLoc.TODO, lv, structuredAssignment.getRvalue(), true));
    }

    private int goBack(int idx, List<Op04StructuredStatement> statements, JavaTypeInstance type, int slot, List<LValue> replaceThese, List<Op04StructuredStatement> inThese) {
        for (int x = idx; x >= 0; --x) {
            Op04StructuredStatement stm = statements.get(x);
            StructuredStatement s = stm.getStatement();
            if (s instanceof StructuredComment) continue;
            if (!(s instanceof StructuredAssignment)) {
                return x;
            }
            StructuredAssignment sa = (StructuredAssignment)s;
            LValue lv = sa.getLvalue();
            if (!(lv instanceof LocalVariable)) {
                return x;
            }
            if (((LocalVariable)lv).getIdx() != slot) {
                return x;
            }
            if (!type.equals(lv.getInferredJavaType().getJavaTypeInstance())) {
                return x;
            }
            replaceThese.add(lv);
            inThese.add(stm);
        }
        return idx;
    }
}

