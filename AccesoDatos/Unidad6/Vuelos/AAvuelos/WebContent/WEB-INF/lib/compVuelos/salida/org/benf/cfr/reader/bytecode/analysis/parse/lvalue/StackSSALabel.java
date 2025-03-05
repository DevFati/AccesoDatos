/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.lvalue;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.AbstractLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.stack.StackEntry;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.output.Dumper;

public class StackSSALabel
extends AbstractLValue {
    private final long id;
    private final StackEntry stackEntry;

    public StackSSALabel(long id, StackEntry stackEntry) {
        super(stackEntry.getInferredJavaType());
        this.id = id;
        this.stackEntry = stackEntry;
    }

    protected StackSSALabel(InferredJavaType inferredJavaType) {
        super(inferredJavaType);
        this.id = 0L;
        this.stackEntry = null;
    }

    @Override
    public void markFinal() {
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isFakeIgnored() {
        return false;
    }

    @Override
    public void markVar() {
    }

    @Override
    public boolean isVar() {
        return false;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.HIGHEST;
    }

    @Override
    public Dumper dump(Dumper d, boolean defines) {
        return d.identifier("v" + this.id + this.typeToString(), this, defines);
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        return this.dump(d, false);
    }

    @Override
    public int getNumberOfCreators() {
        return this.stackEntry.getSourceCount();
    }

    @Override
    public LValue deepClone(CloneHelper cloneHelper) {
        return this;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return false;
    }

    public <Statement> void collectLValueAssignments(Expression rhsAssigned, StatementContainer<Statement> statementContainer, LValueAssignmentCollector<Statement> lValueAssigmentCollector) {
        if (this.getNumberOfCreators() == 1) {
            if (rhsAssigned.isSimple() || this.stackEntry.getUsageCount() == 1L) {
                lValueAssigmentCollector.collect(this, statementContainer, rhsAssigned);
            } else if (this.stackEntry.getUsageCount() > 1L) {
                lValueAssigmentCollector.collectMultiUse(this, statementContainer, rhsAssigned);
            }
        }
    }

    @Override
    public SSAIdentifiers<LValue> collectVariableMutation(SSAIdentifierFactory<LValue, ?> ssaIdentifierFactory) {
        return new SSAIdentifiers<LValue>();
    }

    @Override
    public LValue replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        return this;
    }

    @Override
    public LValue applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    public StackEntry getStackEntry() {
        return this.stackEntry;
    }

    public int hashCode() {
        return (int)this.id;
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof StackSSALabel)) {
            return false;
        }
        return this.id == ((StackSSALabel)o).id;
    }
}

