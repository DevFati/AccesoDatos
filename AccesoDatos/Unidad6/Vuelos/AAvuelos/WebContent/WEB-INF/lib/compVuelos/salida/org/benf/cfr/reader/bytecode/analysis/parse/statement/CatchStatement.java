/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.output.Dumper;

public class CatchStatement
extends AbstractStatement {
    private final List<ExceptionGroup.Entry> exceptions;
    private BlockIdentifier catchBlockIdent;
    private LValue catching;

    public CatchStatement(BytecodeLoc loc, List<ExceptionGroup.Entry> exceptions, LValue catching) {
        super(loc);
        this.exceptions = exceptions;
        this.catching = catching;
        if (!exceptions.isEmpty()) {
            JavaTypeInstance collapsedCatchType = CatchStatement.determineType(exceptions);
            InferredJavaType catchType = new InferredJavaType(collapsedCatchType, InferredJavaType.Source.EXCEPTION, true);
            this.catching.getInferredJavaType().chain(catchType);
        }
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    private static JavaTypeInstance determineType(List<ExceptionGroup.Entry> exceptions) {
        InferredJavaType ijt = new InferredJavaType();
        ijt.chain(new InferredJavaType(exceptions.get(0).getCatchType(), InferredJavaType.Source.EXCEPTION));
        int len = exceptions.size();
        for (int x = 1; x < len; ++x) {
            ijt.chain(new InferredJavaType(exceptions.get(x).getCatchType(), InferredJavaType.Source.EXCEPTION));
        }
        if (ijt.isClash()) {
            ijt.collapseTypeClash();
        }
        return ijt.getJavaTypeInstance();
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        CatchStatement res = new CatchStatement(this.getLoc(), this.exceptions, cloneHelper.replaceOrClone(this.catching));
        res.setCatchBlockIdent(this.catchBlockIdent);
        return res;
    }

    public void removeCatchBlockFor(final BlockIdentifier tryBlockIdent) {
        List<ExceptionGroup.Entry> toRemove = Functional.filter(this.exceptions, new Predicate<ExceptionGroup.Entry>(){

            @Override
            public boolean test(ExceptionGroup.Entry in) {
                return in.getTryBlockIdentifier().equals(tryBlockIdent);
            }
        });
        this.exceptions.removeAll(toRemove);
    }

    public boolean hasCatchBlockFor(BlockIdentifier tryBlockIdent) {
        for (ExceptionGroup.Entry entry : this.exceptions) {
            if (!entry.getTryBlockIdentifier().equals(tryBlockIdent)) continue;
            return true;
        }
        return false;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.keyword("catch ").separator("( " + this.exceptions + " ").dump(this.catching).separator(" ) ").separator("{").newln();
    }

    public BlockIdentifier getCatchBlockIdent() {
        return this.catchBlockIdent;
    }

    public void setCatchBlockIdent(BlockIdentifier catchBlockIdent) {
        this.catchBlockIdent = catchBlockIdent;
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        this.catching = expressionRewriter.rewriteExpression(this.catching, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.LVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public void collectLValueAssignments(LValueAssignmentCollector<Statement> lValueAssigmentCollector) {
        if (this.catching instanceof LocalVariable) {
            lValueAssigmentCollector.collectLocalVariableAssignment((LocalVariable)this.catching, this.getContainer(), null);
        }
    }

    @Override
    public LValue getCreatedLValue() {
        return this.catching;
    }

    public List<ExceptionGroup.Entry> getExceptions() {
        return this.exceptions;
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        return new UnstructuredCatch(this.exceptions, this.catchBlockIdent, this.catching);
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
        CatchStatement other = (CatchStatement)o;
        if (!constraint.equivalent(this.exceptions, other.exceptions)) {
            return false;
        }
        return constraint.equivalent(this.catching, other.catching);
    }
}

