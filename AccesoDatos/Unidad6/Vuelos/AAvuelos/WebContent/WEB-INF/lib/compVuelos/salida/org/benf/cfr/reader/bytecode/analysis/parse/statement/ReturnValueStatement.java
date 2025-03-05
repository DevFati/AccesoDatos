/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.output.Dumper;

public class ReturnValueStatement
extends ReturnStatement {
    private Expression rvalue;
    private final JavaTypeInstance fnReturnType;

    public ReturnValueStatement(BytecodeLoc loc, Expression rvalue, JavaTypeInstance fnReturnType) {
        super(loc);
        this.rvalue = rvalue;
        if (fnReturnType instanceof JavaGenericPlaceholderTypeInstance) {
            this.rvalue = new CastExpression(BytecodeLoc.NONE, new InferredJavaType(fnReturnType, InferredJavaType.Source.FUNCTION, true), this.rvalue);
        }
        this.fnReturnType = fnReturnType;
    }

    @Override
    public ReturnStatement deepClone(CloneHelper cloneHelper) {
        return new ReturnValueStatement(this.getLoc(), cloneHelper.replaceOrClone(this.rvalue), this.fnReturnType);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.rvalue);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.keyword("return ").dump(this.rvalue).endCodeln();
    }

    public Expression getReturnValue() {
        return this.rvalue;
    }

    public JavaTypeInstance getFnReturnType() {
        return this.fnReturnType;
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        this.rvalue = this.rvalue.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, this.getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        this.rvalue = expressionRewriter.rewriteExpression(this.rvalue, ssaIdentifiers, this.getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        this.rvalue.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        Expression rvalueUse = this.rvalue;
        if (this.fnReturnType instanceof RawJavaType && !this.rvalue.getInferredJavaType().getJavaTypeInstance().implicitlyCastsTo(this.fnReturnType, null)) {
            InferredJavaType inferredJavaType = new InferredJavaType(this.fnReturnType, InferredJavaType.Source.FUNCTION, true);
            rvalueUse = new CastExpression(BytecodeLoc.NONE, inferredJavaType, this.rvalue, true);
        }
        return new StructuredReturn(this.getLoc(), rvalueUse, this.fnReturnType);
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReturnValueStatement)) {
            return false;
        }
        ReturnValueStatement other = (ReturnValueStatement)o;
        return this.rvalue.equals(other.rvalue);
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
        ReturnValueStatement other = (ReturnValueStatement)o;
        return constraint.equivalent(this.rvalue, other.rvalue);
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return this.rvalue.canThrow(caught);
    }
}

