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
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredReturn
extends AbstractStructuredStatement
implements BoxingProcessor {
    private Expression value;
    private final JavaTypeInstance fnReturnType;

    public StructuredReturn(BytecodeLoc loc) {
        super(loc);
        this.value = null;
        this.fnReturnType = null;
    }

    public StructuredReturn(BytecodeLoc loc, Expression value, JavaTypeInstance fnReturnType) {
        super(loc);
        this.value = value;
        this.fnReturnType = fnReturnType;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.value);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.fnReturnType);
        collector.collectFrom(this.value);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        if (this.value == null) {
            dumper.keyword("return").print(";");
        } else {
            dumper.keyword("return ").dump(this.value).print(";");
        }
        dumper.newln();
        return dumper;
    }

    public Expression getValue() {
        return this.value;
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
        if (this.value != null) {
            this.value.collectUsedLValues(scopeDiscoverer);
        }
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        if (this.value == null) {
            return false;
        }
        this.value = boxingRewriter.sugarNonParameterBoxing(this.value, this.fnReturnType);
        return false;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        expressionRewriter.handleStatement(this.getContainer());
        if (this.value != null) {
            this.value = expressionRewriter.rewriteExpression(this.value, null, (StatementContainer)this.getContainer(), null);
        }
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != StructuredReturn.class) {
            return false;
        }
        StructuredReturn other = (StructuredReturn)obj;
        if (this.value == null ? other.value != null : !this.value.equals(other.value)) {
            return false;
        }
        return !(this.fnReturnType == null ? other.fnReturnType != null : !this.fnReturnType.equals(other.fnReturnType));
    }

    @Override
    public boolean canFall() {
        return false;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!(o instanceof StructuredReturn)) {
            return false;
        }
        StructuredReturn other = (StructuredReturn)o;
        if (this.value == null ? other.value != null : !this.value.equals(other.value)) {
            return false;
        }
        matchIterator.advance();
        return true;
    }
}

