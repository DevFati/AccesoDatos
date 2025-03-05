/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredAssignment
extends AbstractStructuredStatement
implements BoxingProcessor {
    private LValue lvalue;
    private Expression rvalue;
    private boolean creator;

    public StructuredAssignment(BytecodeLoc loc, LValue lvalue, Expression rvalue) {
        super(loc);
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.creator = false;
    }

    public StructuredAssignment(BytecodeLoc loc, LValue lvalue, Expression rvalue, boolean creator) {
        super(loc);
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.creator = creator;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.rvalue);
    }

    public boolean isCreator(LValue lvalue) {
        return this.creator && this.lvalue.equals(lvalue);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.lvalue.collectTypeUsages(collector);
        collector.collectFrom(this.rvalue);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (this.creator) {
            if (this.lvalue.isFinal()) {
                dumper.print("final ");
            }
            LValue.Creation.dump(dumper, this.lvalue);
        } else {
            dumper.dump(this.lvalue);
        }
        dumper.operator(" = ").dump(this.rvalue).endCodeln();
        return dumper;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        this.rvalue.collectUsedLValues(scopeDiscoverer);
        this.lvalue.collectLValueAssignments(this.rvalue, this.getContainer(), scopeDiscoverer);
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {
        if (scopedEntity instanceof LocalVariable) {
            LocalVariable localVariable = (LocalVariable)scopedEntity;
            if (!localVariable.equals(this.lvalue)) {
                throw new IllegalArgumentException("Being asked to mark creator for wrong variable");
            }
            this.creator = true;
            InferredJavaType inferredJavaType = localVariable.getInferredJavaType();
            if (inferredJavaType.isClash()) {
                inferredJavaType.collapseTypeClash();
            }
        }
    }

    @Override
    public List<LValue> findCreatedHere() {
        if (this.creator) {
            return ListFactory.newImmutableList(this.lvalue);
        }
        return null;
    }

    public LValue getLvalue() {
        return this.lvalue;
    }

    public Expression getRvalue() {
        return this.rvalue;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!this.equals(o)) {
            return false;
        }
        matchIterator.advance();
        return true;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof StructuredAssignment)) {
            return false;
        }
        StructuredAssignment other = (StructuredAssignment)o;
        if (!this.lvalue.equals(other.lvalue)) {
            return false;
        }
        return this.rvalue.equals(other.rvalue);
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        expressionRewriter.handleStatement(this.getContainer());
        this.lvalue = expressionRewriter.rewriteExpression(this.lvalue, null, (StatementContainer)this.getContainer(), null);
        this.rvalue = expressionRewriter.rewriteExpression(this.rvalue, null, (StatementContainer)this.getContainer(), null);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        this.rvalue = boxingRewriter.sugarNonParameterBoxing(this.rvalue, this.lvalue.getInferredJavaType().getJavaTypeInstance());
        return true;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.lvalue = this.lvalue.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }
}

