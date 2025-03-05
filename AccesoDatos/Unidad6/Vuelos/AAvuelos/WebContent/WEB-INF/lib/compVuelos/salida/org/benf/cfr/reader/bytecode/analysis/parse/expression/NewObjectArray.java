/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractNewArray;
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
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.output.Dumper;

public class NewObjectArray
extends AbstractNewArray {
    private List<Expression> dimSizes;
    private final JavaTypeInstance allocatedType;
    private final JavaTypeInstance resultType;
    private final int numDims;

    public NewObjectArray(BytecodeLoc loc, List<Expression> dimSizes, JavaTypeInstance resultInstance) {
        super(loc, new InferredJavaType(resultInstance, InferredJavaType.Source.EXPRESSION, true));
        this.dimSizes = dimSizes;
        this.allocatedType = resultInstance.getArrayStrippedType();
        this.resultType = resultInstance;
        this.numDims = resultInstance.getNumArrayDimensions();
        for (Expression size : dimSizes) {
            size.getInferredJavaType().useAsWithoutCasting(RawJavaType.INT);
        }
    }

    private NewObjectArray(BytecodeLoc loc, InferredJavaType inferredJavaType, JavaTypeInstance resultType, int numDims, JavaTypeInstance allocatedType, List<Expression> dimSizes) {
        super(loc, inferredJavaType);
        this.resultType = resultType;
        this.numDims = numDims;
        this.allocatedType = allocatedType;
        this.dimSizes = dimSizes;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.dimSizes, new HasByteCodeLoc[0]);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new NewObjectArray(this.getLoc(), this.getInferredJavaType(), this.resultType, this.numDims, this.allocatedType, cloneHelper.replaceOrClone(this.dimSizes));
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.allocatedType);
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.keyword("new ").dump(this.allocatedType);
        for (Expression dimSize : this.dimSizes) {
            d.separator("[").dump(dimSize).separator("]");
        }
        for (int x = this.dimSizes.size(); x < this.numDims; ++x) {
            d.separator("[]");
        }
        return d;
    }

    @Override
    public int getNumDims() {
        return this.numDims;
    }

    @Override
    public int getNumSizedDims() {
        return this.dimSizes.size();
    }

    @Override
    public Expression getDimSize(int dim) {
        if (dim >= this.dimSizes.size()) {
            throw new ConfusedCFRException("Out of bounds");
        }
        return this.dimSizes.get(dim);
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        LValueRewriter.Util.rewriteArgArray(lValueRewriter, ssaIdentifiers, statementContainer, this.dimSizes);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyForwards(this.dimSizes, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        ExpressionRewriterHelper.applyBackwards(this.dimSizes, expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        for (Expression dimSize : this.dimSizes) {
            dimSize.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        NewObjectArray that = (NewObjectArray)o;
        if (this.numDims != that.numDims) {
            return false;
        }
        if (this.allocatedType != null ? !this.allocatedType.equals(that.allocatedType) : that.allocatedType != null) {
            return false;
        }
        if (this.dimSizes != null ? !this.dimSizes.equals(that.dimSizes) : that.dimSizes != null) {
            return false;
        }
        return !(this.resultType != null ? !this.resultType.equals(that.resultType) : that.resultType != null);
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
        NewObjectArray other = (NewObjectArray)o;
        if (this.numDims != other.numDims) {
            return false;
        }
        if (!constraint.equivalent(this.dimSizes, other.dimSizes)) {
            return false;
        }
        if (!constraint.equivalent(this.allocatedType, other.allocatedType)) {
            return false;
        }
        return constraint.equivalent(this.resultType, other.resultType);
    }
}

