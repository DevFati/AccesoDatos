/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactoryImpl;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionVisitor;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.ToStringDumper;

public abstract class AbstractExpression
implements Expression {
    private BytecodeLoc loc;
    private final InferredJavaType inferredJavaType;

    public AbstractExpression(BytecodeLoc loc, InferredJavaType inferredJavaType) {
        this.loc = loc;
        this.inferredJavaType = inferredJavaType;
    }

    @Override
    public void addLoc(HasByteCodeLoc loc) {
        if (loc.getLoc().isEmpty()) {
            return;
        }
        this.loc = BytecodeLocFactoryImpl.INSTANCE.combine((HasByteCodeLoc)this, loc);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.inferredJavaType.getJavaTypeInstance());
    }

    @Override
    public BytecodeLoc getLoc() {
        return this.loc;
    }

    @Override
    public boolean canPushDownInto() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Expression pushDown(Expression toPush, Expression parent) {
        throw new ConfusedCFRException("Push down not supported.");
    }

    @Override
    public InferredJavaType getInferredJavaType() {
        return this.inferredJavaType;
    }

    @Override
    public Expression outerDeepClone(CloneHelper cloneHelper) {
        return cloneHelper.replaceOrClone(this);
    }

    public final String toString() {
        return ToStringDumper.toString(this);
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return true;
    }

    public abstract boolean equals(Object var1);

    @Override
    public Literal getComputedLiteral(Map<LValue, Literal> display) {
        return null;
    }

    @Override
    public boolean isValidStatement() {
        return false;
    }

    @Override
    public final Dumper dump(Dumper d) {
        return this.dumpWithOuterPrecedence(d, Precedence.WEAKEST, Troolean.NEITHER);
    }

    @Override
    public abstract Precedence getPrecedence();

    public abstract Dumper dumpInner(Dumper var1);

    @Override
    public <T> T visit(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public final Dumper dumpWithOuterPrecedence(Dumper d, Precedence outerP, Troolean isLhs) {
        Precedence innerP = this.getPrecedence();
        int cmp = innerP.compareTo(outerP);
        boolean requires = false;
        if (cmp > 0) {
            requires = true;
        } else if (cmp == 0) {
            if (innerP == outerP && innerP.isCommutative()) {
                requires = false;
            } else {
                switch (isLhs) {
                    case TRUE: {
                        requires = !innerP.isLtoR();
                        break;
                    }
                    case FALSE: {
                        requires = innerP.isLtoR();
                        break;
                    }
                    case NEITHER: {
                        boolean bl = requires = !innerP.isLtoR();
                    }
                }
            }
        }
        if (requires) {
            d.separator("(");
            this.dumpInner(d);
            d.separator(")");
        } else {
            this.dumpInner(d);
        }
        return d;
    }
}

