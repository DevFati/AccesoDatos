/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionVisitor;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class Literal
extends AbstractExpression {
    public static final Literal FALSE = new Literal(TypedLiteral.getBoolean(0));
    public static final Literal TRUE = new Literal(TypedLiteral.getBoolean(1));
    public static final Literal MINUS_ONE = new Literal(TypedLiteral.getInt(-1));
    public static final Literal NULL = new Literal(TypedLiteral.getNull());
    public static final Literal INT_ZERO = new Literal(TypedLiteral.getInt(0));
    public static final Literal INT_ONE = new Literal(TypedLiteral.getInt(1));
    private static final Literal LONG_ONE = new Literal(TypedLiteral.getLong(1L));
    public static final Literal DOUBLE_ZERO = new Literal(TypedLiteral.getDouble(0.0));
    public static final Literal DOUBLE_ONE = new Literal(TypedLiteral.getDouble(1.0));
    public static final Literal DOUBLE_MINUS_ONE = new Literal(TypedLiteral.getDouble(-1.0));
    public static final Literal FLOAT_ZERO = new Literal(TypedLiteral.getFloat(0.0f));
    public static final Literal FLOAT_ONE = new Literal(TypedLiteral.getFloat(1.0f));
    public static final Literal FLOAT_MINUS_ONE = new Literal(TypedLiteral.getFloat(-1.0f));
    protected final TypedLiteral value;

    public Literal(TypedLiteral value) {
        super(BytecodeLoc.NONE, value.getInferredJavaType());
        this.value = value;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.NONE;
    }

    public static Expression getLiteralOrNull(RawJavaType rawCastType, InferredJavaType inferredCastType, int intValue) {
        switch (rawCastType) {
            case BOOLEAN: {
                if (intValue == 0) {
                    return FALSE;
                }
                if (intValue == 1) {
                    return TRUE;
                }
                return null;
            }
            case BYTE: 
            case CHAR: 
            case SHORT: {
                return new CastExpression(BytecodeLoc.NONE, inferredCastType, new Literal(TypedLiteral.getInt(intValue)));
            }
            case INT: {
                return new Literal(TypedLiteral.getInt(intValue));
            }
            case LONG: {
                return new Literal(TypedLiteral.getLong(intValue));
            }
            case FLOAT: {
                return new Literal(TypedLiteral.getFloat(intValue));
            }
            case DOUBLE: {
                return new Literal(TypedLiteral.getDouble(intValue));
            }
        }
        return null;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.HIGHEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        return d.dump(this.value);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.value.collectTypeUsages(collector);
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return this;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public <T> T visit(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        JavaTypeInstance lValueType;
        InnerClassInfo innerClassInfo;
        Object x;
        if (this.value.getType() == TypedLiteral.LiteralType.Class && (x = this.value.getValue()) instanceof JavaTypeInstance && (innerClassInfo = (lValueType = (JavaTypeInstance)x).getInnerClassHereInfo()).isMethodScopedClass() && !innerClassInfo.isAnonymousClass()) {
            lValueUsageCollector.collect(new SentinelLocalClassLValue(lValueType), ReadWrite.READ);
        }
    }

    public Expression appropriatelyCasted(InferredJavaType expected) {
        if (this.value.getType() != TypedLiteral.LiteralType.Integer) {
            return this;
        }
        JavaTypeInstance type = expected.getJavaTypeInstance();
        if (type.getStackType() != StackType.INT) {
            return this;
        }
        if (type == RawJavaType.SHORT || type == RawJavaType.BYTE || type == RawJavaType.CHAR) {
            return new CastExpression(BytecodeLoc.NONE, expected, this);
        }
        return this;
    }

    public TypedLiteral getValue() {
        return this.value;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Literal)) {
            return false;
        }
        Literal other = (Literal)o;
        return this.value.equals(other.value);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof Literal)) {
            return false;
        }
        Literal other = (Literal)o;
        return constraint.equivalent(this.value, other.value);
    }

    @Override
    public Literal getComputedLiteral(Map<LValue, Literal> display) {
        return this;
    }

    public static boolean equalsAnyOne(Expression expression) {
        return expression.equals(INT_ONE) || expression.equals(LONG_ONE);
    }
}

