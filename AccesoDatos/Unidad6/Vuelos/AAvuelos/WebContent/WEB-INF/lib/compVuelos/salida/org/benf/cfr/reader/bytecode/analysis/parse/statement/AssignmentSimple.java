/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import java.util.Collection;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractAssignment;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.CreationCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.output.Dumper;

public class AssignmentSimple
extends AbstractAssignment {
    private LValue lvalue;
    private Expression rvalue;

    public AssignmentSimple(BytecodeLoc loc, LValue lvalue, Expression rvalue) {
        super(loc);
        this.lvalue = lvalue;
        this.rvalue = lvalue.getInferredJavaType().chain(rvalue.getInferredJavaType()).performCastAction(rvalue, lvalue.getInferredJavaType());
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.dump(this.lvalue).operator(" = ").dump(this.rvalue).endCodeln();
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.rvalue);
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        return new AssignmentSimple(this.getLoc(), cloneHelper.replaceOrClone(this.lvalue), cloneHelper.replaceOrClone(this.rvalue));
    }

    @Override
    public void collectLValueAssignments(LValueAssignmentCollector<Statement> lValueAssigmentCollector) {
        this.lvalue.collectLValueAssignments(this.rvalue, this.getContainer(), lValueAssigmentCollector);
    }

    @Override
    public boolean doesBlackListLValueReplacement(LValue lValue, Expression expression) {
        return this.lvalue.doesBlackListLValueReplacement(lValue, expression);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        lValueUsageCollector.collect(this.lvalue, ReadWrite.WRITE);
        this.rvalue.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public void collectObjectCreation(CreationCollector creationCollector) {
        creationCollector.collectCreation(this.lvalue, this.rvalue, this.getContainer());
    }

    @Override
    public SSAIdentifiers<LValue> collectLocallyMutatedVariables(SSAIdentifierFactory<LValue, ?> ssaIdentifierFactory) {
        return this.lvalue.collectVariableMutation(ssaIdentifierFactory);
    }

    @Override
    public LValue getCreatedLValue() {
        return this.lvalue;
    }

    @Override
    public Expression getRValue() {
        return this.rvalue;
    }

    public void setRValue(Expression rvalue) {
        this.rvalue = rvalue;
    }

    @Override
    public boolean isSelfMutatingOperation() {
        MemberFunctionInvokation memberFunctionInvokation;
        Expression object;
        Expression localR = this.rvalue;
        while (localR instanceof CastExpression) {
            localR = ((CastExpression)localR).getChild();
        }
        if (localR instanceof ArithmeticOperation) {
            ArithmeticOperation arithmeticOperation = (ArithmeticOperation)localR;
            return arithmeticOperation.isLiteralFunctionOf(this.lvalue);
        }
        if (localR instanceof MemberFunctionInvokation && (object = (memberFunctionInvokation = (MemberFunctionInvokation)localR).getObject()) instanceof LValueExpression) {
            LValue memberLValue = ((LValueExpression)object).getLValue();
            return memberLValue.equals(this.lvalue);
        }
        return false;
    }

    @Override
    public boolean isSelfMutatingOp1(LValue lValue, ArithOp arithOp) {
        return false;
    }

    @Override
    public Expression getPostMutation() {
        throw new IllegalStateException();
    }

    @Override
    public Expression getPreMutation() {
        throw new IllegalStateException();
    }

    @Override
    public AbstractAssignmentExpression getInliningExpression() {
        return new AssignmentExpression(this.getLoc(), this.getCreatedLValue(), this.getRValue());
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        this.lvalue = this.lvalue.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, this.getContainer());
        LValueUsageCollectorSimple tmp = new LValueUsageCollectorSimple();
        this.lvalue.collectLValueUsage(tmp);
        Collection<LValue> usedLValues = tmp.getUsedLValues();
        if (!usedLValues.isEmpty()) {
            lValueRewriter = lValueRewriter.keepConstant(usedLValues);
        }
        this.rvalue = this.rvalue.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, this.getContainer());
        lValueRewriter.checkPostConditions(this.lvalue, this.rvalue);
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        this.lvalue = expressionRewriter.rewriteExpression(this.lvalue, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.LVALUE);
        this.rvalue = expressionRewriter.rewriteExpression(this.rvalue, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new StructuredAssignment(this.getLoc(), this.lvalue, this.rvalue);
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return this.lvalue.canThrow(caught) || this.rvalue.canThrow(caught);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AssignmentSimple)) {
            return false;
        }
        AssignmentSimple other = (AssignmentSimple)o;
        return this.lvalue.equals(other.lvalue) && this.rvalue.equals(other.rvalue);
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
        AssignmentSimple other = (AssignmentSimple)o;
        if (!constraint.equivalent(this.lvalue, other.lvalue)) {
            return false;
        }
        return constraint.equivalent(this.rvalue, other.rvalue);
    }
}

