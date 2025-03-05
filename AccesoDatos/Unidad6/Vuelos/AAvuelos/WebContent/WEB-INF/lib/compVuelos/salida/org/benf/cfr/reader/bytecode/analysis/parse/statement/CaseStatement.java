/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredCase;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.output.Dumper;

public class CaseStatement
extends AbstractStatement {
    private List<Expression> values;
    private final BlockIdentifier switchBlock;
    private final BlockIdentifier caseBlock;
    private final InferredJavaType caseType;

    public CaseStatement(BytecodeLoc loc, List<Expression> values, InferredJavaType caseType, BlockIdentifier switchBlock, BlockIdentifier caseBlock) {
        super(loc);
        this.values = values;
        this.caseType = caseType;
        this.switchBlock = switchBlock;
        this.caseBlock = caseBlock;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.values, new HasByteCodeLoc[0]);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (this.values.isEmpty()) {
            dumper.print("default").operator(":").newln();
        } else {
            for (Expression value : this.values) {
                dumper.print("case ").dump(value).operator(":").newln();
            }
        }
        return dumper;
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new CaseStatement(this.getLoc(), cloneHelper.replaceOrClone(this.values), this.caseType, this.switchBlock, this.caseBlock);
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        for (int x = 0; x < this.values.size(); ++x) {
            this.values.set(x, this.values.get(x).replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, this.getContainer()));
        }
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        for (int x = 0; x < this.values.size(); ++x) {
            this.values.set(x, expressionRewriter.rewriteExpression(this.values.get(x), ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE));
        }
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
    }

    public BlockIdentifier getSwitchBlock() {
        return this.switchBlock;
    }

    public boolean isDefault() {
        return this.values.isEmpty();
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredCase(this.values, this.caseType, this.caseBlock);
    }

    public BlockIdentifier getCaseBlock() {
        return this.caseBlock;
    }

    public List<Expression> getValues() {
        return this.values;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return false;
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        CaseStatement other = (CaseStatement)o;
        return constraint.equivalent(this.values, other.values);
    }
}

