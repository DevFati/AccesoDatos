/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractNewArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class NewAnonymousArray
extends AbstractNewArray
implements BoxingProcessor {
    private JavaTypeInstance allocatedType;
    private int numDims;
    private List<Expression> values = ListFactory.newList();
    private boolean isCompletelyAnonymous;

    public NewAnonymousArray(BytecodeLoc loc, InferredJavaType type, int numDims, List<Expression> values, boolean isCompletelyAnonymous) {
        super(loc, type);
        this.numDims = numDims;
        this.allocatedType = type.getJavaTypeInstance().getArrayStrippedType();
        if (this.allocatedType instanceof RawJavaType) {
            for (Expression value : values) {
                value.getInferredJavaType().useAsWithoutCasting(this.allocatedType);
            }
        }
        for (Expression value : values) {
            if (numDims > 1 && value instanceof NewAnonymousArray) {
                NewAnonymousArray newAnonymousArrayInner = (NewAnonymousArray)value;
                newAnonymousArrayInner.isCompletelyAnonymous = true;
            }
            this.values.add(value);
        }
        this.isCompletelyAnonymous = isCompletelyAnonymous;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.values, new HasByteCodeLoc[0]);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.allocatedType);
        collector.collectFrom(this.values);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        for (int i = 0; i < this.values.size(); ++i) {
            this.values.set(i, boxingRewriter.sugarNonParameterBoxing(this.values.get(i), this.allocatedType));
        }
        return false;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new NewAnonymousArray(this.getLoc(), this.getInferredJavaType(), this.numDims, cloneHelper.replaceOrClone(this.values), this.isCompletelyAnonymous);
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        if (!this.isCompletelyAnonymous) {
            d.keyword("new ").dump(this.allocatedType);
            for (int x = 0; x < this.numDims; ++x) {
                d.print("[]");
            }
        }
        d.separator("{");
        boolean first = true;
        for (Expression value : this.values) {
            first = StringUtils.comma(first, d);
            d.dump(value);
        }
        d.separator("}");
        return d;
    }

    public List<Expression> getValues() {
        return this.values;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        LValueRewriter.Util.rewriteArgArray(lValueRewriter, ssaIdentifiers, statementContainer, this.values);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(this.values, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyBackwards(this.values, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        for (Expression value : this.values) {
            value.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public int getNumDims() {
        return this.numDims;
    }

    @Override
    public int getNumSizedDims() {
        return 0;
    }

    @Override
    public Expression getDimSize(int dim) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        NewAnonymousArray that = (NewAnonymousArray)o;
        if (this.isCompletelyAnonymous != that.isCompletelyAnonymous) {
            return false;
        }
        if (this.numDims != that.numDims) {
            return false;
        }
        if (this.allocatedType != null ? !this.allocatedType.equals(that.allocatedType) : that.allocatedType != null) {
            return false;
        }
        return !(this.values != null ? !this.values.equals(that.values) : that.values != null);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        NewAnonymousArray other = (NewAnonymousArray)o;
        if (this.isCompletelyAnonymous != other.isCompletelyAnonymous) {
            return false;
        }
        if (this.numDims != other.numDims) {
            return false;
        }
        if (!constraint.equivalent(this.allocatedType, other.allocatedType)) {
            return false;
        }
        return constraint.equivalent(this.values, other.values);
    }
}

