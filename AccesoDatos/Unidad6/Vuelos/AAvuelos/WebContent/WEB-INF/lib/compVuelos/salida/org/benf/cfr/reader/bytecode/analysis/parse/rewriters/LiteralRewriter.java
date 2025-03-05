/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.rewriters;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticMonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.NonaryFunction;

public strictfp class LiteralRewriter
extends AbstractExpressionRewriter {
    public static final LiteralRewriter INSTANCE;
    private final JavaTypeInstance testType;
    private static final InferredJavaType INFERRED_INT;
    private static final StaticVariable I_MAX_VALUE;
    private static final StaticVariable I_MIN_VALUE;
    private static final InferredJavaType INFERRED_SHORT;
    private static final StaticVariable S_MAX_VALUE;
    private static final StaticVariable S_MIN_VALUE;
    private static final InferredJavaType INFERRED_LONG;
    private static final StaticVariable J_MAX_VALUE;
    private static final StaticVariable J_MIN_VALUE;
    private static final InferredJavaType INFERRED_FLOAT;
    private static final StaticVariable F_MAX_VALUE;
    private static final StaticVariable F_MIN_VALUE;
    private static final StaticVariable F_MIN_NORMAL;
    private static final StaticVariable F_NAN;
    private static final StaticVariable F_NEGATIVE_INFINITY;
    private static final StaticVariable F_POSITIVE_INFINITY;
    private static final InferredJavaType INFERRED_DOUBLE;
    private static final StaticVariable D_MAX_VALUE;
    private static final StaticVariable D_MIN_VALUE;
    private static final StaticVariable D_MIN_NORMAL;
    private static final StaticVariable D_NAN;
    private static final StaticVariable D_NEGATIVE_INFINITY;
    private static final StaticVariable D_POSITIVE_INFINITY;
    private static final StaticVariable MATH_PI;
    private static final StaticVariable MATH_E;
    private static final Map<Double, NonaryFunction<Expression>> PI_DOUBLES;
    private static final Map<Float, NonaryFunction<Expression>> PI_FLOATS;

    public LiteralRewriter(JavaTypeInstance testType) {
        this.testType = testType;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if ((expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags)) instanceof Literal) {
            Literal literal = (Literal)expression;
            TypedLiteral typed = literal.getValue();
            TypedLiteral.LiteralType type = typed.getType();
            switch (type) {
                case Integer: {
                    return this.rewriteInteger(literal, typed.getIntValue());
                }
                case Long: {
                    return this.rewriteLong(literal, typed.getLongValue());
                }
                case Double: {
                    return this.rewriteDouble(literal, typed.getDoubleValue());
                }
                case Float: {
                    return this.rewriteFloat(literal, typed.getFloatValue());
                }
            }
        }
        return expression;
    }

    private Expression rewriteInteger(Literal literal, int value) {
        if (!this.testType.equals(TypeConstants.INTEGER)) {
            if (value == Integer.MAX_VALUE) {
                return new LValueExpression(I_MAX_VALUE);
            }
            if (value == Integer.MIN_VALUE) {
                return new LValueExpression(I_MIN_VALUE);
            }
            if (value == Short.MAX_VALUE) {
                return new LValueExpression(S_MAX_VALUE);
            }
            if (value == Short.MIN_VALUE) {
                return new LValueExpression(S_MIN_VALUE);
            }
        }
        return literal;
    }

    private Expression rewriteLong(Literal literal, long value) {
        if (!this.testType.equals(TypeConstants.LONG)) {
            if (value == Long.MAX_VALUE) {
                return new LValueExpression(J_MAX_VALUE);
            }
            if (value == Long.MIN_VALUE) {
                return new LValueExpression(J_MIN_VALUE);
            }
        }
        if (value == Integer.MAX_VALUE) {
            return new LValueExpression(I_MAX_VALUE);
        }
        if (value == Integer.MIN_VALUE) {
            return new LValueExpression(I_MIN_VALUE);
        }
        return literal;
    }

    private Expression rewriteFloat(Literal literal, float value) {
        if (this.testType.equals(TypeConstants.FLOAT)) {
            if (Float.isNaN(value)) {
                return new ArithmeticOperation(BytecodeLoc.NONE, Literal.FLOAT_ZERO, Literal.FLOAT_ZERO, ArithOp.DIVIDE);
            }
            if (Float.compare(Float.NEGATIVE_INFINITY, value) == 0) {
                return new ArithmeticOperation(BytecodeLoc.NONE, Literal.FLOAT_MINUS_ONE, Literal.FLOAT_ZERO, ArithOp.DIVIDE);
            }
            if (Float.compare(Float.POSITIVE_INFINITY, value) == 0) {
                return new ArithmeticOperation(BytecodeLoc.NONE, Literal.FLOAT_ONE, Literal.FLOAT_ZERO, ArithOp.DIVIDE);
            }
        } else {
            if (Float.isNaN(value)) {
                return new LValueExpression(F_NAN);
            }
            if (Float.compare(Float.NEGATIVE_INFINITY, value) == 0) {
                return new LValueExpression(F_NEGATIVE_INFINITY);
            }
            if (Float.compare(Float.POSITIVE_INFINITY, value) == 0) {
                return new LValueExpression(F_POSITIVE_INFINITY);
            }
            if (Float.compare(Float.MAX_VALUE, value) == 0) {
                return new LValueExpression(F_MAX_VALUE);
            }
            if (Float.compare(Float.MIN_VALUE, value) == 0) {
                return new LValueExpression(F_MIN_VALUE);
            }
            if (Float.compare(Float.MIN_NORMAL, value) == 0) {
                return new LValueExpression(F_MIN_NORMAL);
            }
        }
        if (Float.compare((float)Math.E, value) == 0) {
            return new CastExpression(BytecodeLoc.NONE, INFERRED_FLOAT, new LValueExpression(MATH_E));
        }
        Expression piExpr = LiteralRewriter.maybeGetPiExpression(value);
        if (piExpr != null) {
            return piExpr;
        }
        return literal;
    }

    private Expression rewriteDouble(Literal literal, double value) {
        if (this.testType.equals(TypeConstants.DOUBLE)) {
            if (Double.isNaN(value)) {
                return new ArithmeticOperation(BytecodeLoc.NONE, Literal.DOUBLE_ZERO, Literal.DOUBLE_ZERO, ArithOp.DIVIDE);
            }
            if (Double.compare(Double.NEGATIVE_INFINITY, value) == 0) {
                return new ArithmeticOperation(BytecodeLoc.NONE, Literal.DOUBLE_MINUS_ONE, Literal.DOUBLE_ZERO, ArithOp.DIVIDE);
            }
            if (Double.compare(Double.POSITIVE_INFINITY, value) == 0) {
                return new ArithmeticOperation(BytecodeLoc.NONE, Literal.DOUBLE_ONE, Literal.DOUBLE_ZERO, ArithOp.DIVIDE);
            }
        } else {
            if (Double.isNaN(value)) {
                return new LValueExpression(D_NAN);
            }
            if (Double.compare(Double.NEGATIVE_INFINITY, value) == 0) {
                return new LValueExpression(D_NEGATIVE_INFINITY);
            }
            if (Double.compare(Double.POSITIVE_INFINITY, value) == 0) {
                return new LValueExpression(D_POSITIVE_INFINITY);
            }
            if (Double.compare(Double.MAX_VALUE, value) == 0) {
                return new LValueExpression(D_MAX_VALUE);
            }
            if (Double.compare(Double.MIN_VALUE, value) == 0) {
                return new LValueExpression(D_MIN_VALUE);
            }
            if (Double.compare(Double.MIN_NORMAL, value) == 0) {
                return new LValueExpression(D_MIN_NORMAL);
            }
        }
        if (!this.testType.equals(TypeConstants.MATH)) {
            if (Double.compare(Math.E, value) == 0) {
                return new LValueExpression(MATH_E);
            }
            float nearestFloat = (float)value;
            if (Double.compare(nearestFloat, value) == 0 && Float.toString(nearestFloat).length() + 9 < Double.toString(value).length()) {
                return new CastExpression(BytecodeLoc.NONE, INFERRED_DOUBLE, new Literal(TypedLiteral.getFloat(nearestFloat)));
            }
            Expression piExpr = LiteralRewriter.maybeGetPiExpression(value);
            if (piExpr != null) {
                return piExpr;
            }
        }
        return literal;
    }

    private static Expression maybeGetPiExpression(float value) {
        NonaryFunction<Expression> e = PI_FLOATS.get(Float.valueOf(value));
        if (null == e) {
            return null;
        }
        return e.invoke();
    }

    private static Expression maybeGetPiExpression(double value) {
        NonaryFunction<Expression> e = PI_DOUBLES.get(value);
        if (null == e) {
            return null;
        }
        return e.invoke();
    }

    static {
        int ii;
        int i;
        INSTANCE = new LiteralRewriter(TypeConstants.OBJECT);
        INFERRED_INT = new InferredJavaType(RawJavaType.INT, InferredJavaType.Source.LITERAL);
        I_MAX_VALUE = new StaticVariable(INFERRED_INT, (JavaTypeInstance)TypeConstants.INTEGER, "MAX_VALUE");
        I_MIN_VALUE = new StaticVariable(INFERRED_INT, (JavaTypeInstance)TypeConstants.INTEGER, "MIN_VALUE");
        INFERRED_SHORT = new InferredJavaType(RawJavaType.SHORT, InferredJavaType.Source.LITERAL);
        S_MAX_VALUE = new StaticVariable(INFERRED_SHORT, (JavaTypeInstance)TypeConstants.SHORT, "MAX_VALUE");
        S_MIN_VALUE = new StaticVariable(INFERRED_SHORT, (JavaTypeInstance)TypeConstants.SHORT, "MIN_VALUE");
        INFERRED_LONG = new InferredJavaType(RawJavaType.LONG, InferredJavaType.Source.LITERAL);
        J_MAX_VALUE = new StaticVariable(INFERRED_LONG, (JavaTypeInstance)TypeConstants.LONG, "MAX_VALUE");
        J_MIN_VALUE = new StaticVariable(INFERRED_LONG, (JavaTypeInstance)TypeConstants.LONG, "MIN_VALUE");
        INFERRED_FLOAT = new InferredJavaType(RawJavaType.FLOAT, InferredJavaType.Source.LITERAL);
        F_MAX_VALUE = new StaticVariable(INFERRED_FLOAT, (JavaTypeInstance)TypeConstants.FLOAT, "MAX_VALUE");
        F_MIN_VALUE = new StaticVariable(INFERRED_FLOAT, (JavaTypeInstance)TypeConstants.FLOAT, "MIN_VALUE");
        F_MIN_NORMAL = new StaticVariable(INFERRED_FLOAT, (JavaTypeInstance)TypeConstants.FLOAT, "MIN_NORMAL");
        F_NAN = new StaticVariable(INFERRED_FLOAT, (JavaTypeInstance)TypeConstants.FLOAT, "NaN");
        F_NEGATIVE_INFINITY = new StaticVariable(INFERRED_FLOAT, (JavaTypeInstance)TypeConstants.FLOAT, "NEGATIVE_INFINITY");
        F_POSITIVE_INFINITY = new StaticVariable(INFERRED_FLOAT, (JavaTypeInstance)TypeConstants.FLOAT, "POSITIVE_INFINITY");
        INFERRED_DOUBLE = new InferredJavaType(RawJavaType.DOUBLE, InferredJavaType.Source.LITERAL);
        D_MAX_VALUE = new StaticVariable(INFERRED_FLOAT, (JavaTypeInstance)TypeConstants.DOUBLE, "MAX_VALUE");
        D_MIN_VALUE = new StaticVariable(INFERRED_FLOAT, (JavaTypeInstance)TypeConstants.DOUBLE, "MIN_VALUE");
        D_MIN_NORMAL = new StaticVariable(INFERRED_FLOAT, (JavaTypeInstance)TypeConstants.DOUBLE, "MIN_NORMAL");
        D_NAN = new StaticVariable(INFERRED_DOUBLE, (JavaTypeInstance)TypeConstants.DOUBLE, "NaN");
        D_NEGATIVE_INFINITY = new StaticVariable(INFERRED_DOUBLE, (JavaTypeInstance)TypeConstants.DOUBLE, "NEGATIVE_INFINITY");
        D_POSITIVE_INFINITY = new StaticVariable(INFERRED_DOUBLE, (JavaTypeInstance)TypeConstants.DOUBLE, "POSITIVE_INFINITY");
        MATH_PI = new StaticVariable(INFERRED_DOUBLE, (JavaTypeInstance)TypeConstants.MATH, "PI");
        MATH_E = new StaticVariable(INFERRED_DOUBLE, (JavaTypeInstance)TypeConstants.MATH, "E");
        PI_DOUBLES = MapFactory.newMap();
        PI_FLOATS = MapFactory.newMap();
        final LValueExpression pi = new LValueExpression(MATH_PI);
        final ArithmeticMonOperation npi = new ArithmeticMonOperation(BytecodeLoc.NONE, pi, ArithOp.MINUS);
        for (i = -10; i <= 10; ++i) {
            if (i == 0) continue;
            ii = i;
            NonaryFunction<Expression> pifn = new NonaryFunction<Expression>(){

                @Override
                public Expression invoke() {
                    switch (ii) {
                        case 1: {
                            return pi;
                        }
                        case -1: {
                            return npi;
                        }
                    }
                    return new ArithmeticOperation(BytecodeLoc.NONE, pi, new Literal(TypedLiteral.getInt(ii)), ArithOp.MULTIPLY);
                }
            };
            PI_DOUBLES.put(Math.PI * (double)i, pifn);
            pifn = new NonaryFunction<Expression>(){

                @Override
                public Expression invoke() {
                    switch (ii) {
                        case 1: {
                            return new CastExpression(BytecodeLoc.NONE, INFERRED_FLOAT, pi);
                        }
                        case -1: {
                            return new CastExpression(BytecodeLoc.NONE, INFERRED_FLOAT, npi);
                        }
                    }
                    return new CastExpression(BytecodeLoc.NONE, INFERRED_FLOAT, new ArithmeticOperation(BytecodeLoc.NONE, pi, new Literal(TypedLiteral.getInt(ii)), ArithOp.MULTIPLY));
                }
            };
            PI_FLOATS.put(Float.valueOf((float)(Math.PI * (double)i)), pifn);
            if (Math.abs(i) < 2) continue;
            pifn = new NonaryFunction<Expression>(){

                @Override
                public Expression invoke() {
                    return new ArithmeticOperation(BytecodeLoc.NONE, new CastExpression(BytecodeLoc.NONE, INFERRED_FLOAT, pi), new Literal(TypedLiteral.getInt(ii)), ArithOp.MULTIPLY);
                }
            };
            PI_FLOATS.put(Float.valueOf((float)Math.PI * (float)i), pifn);
        }
        for (i = -4; i <= 4; ++i) {
            if (i == 0) continue;
            ii = i;
            final AbstractExpression p = i < 0 ? npi : pi;
            NonaryFunction<Expression> pifn = new NonaryFunction<Expression>(){

                @Override
                public Expression invoke() {
                    return new ArithmeticOperation(BytecodeLoc.NONE, p, new Literal(TypedLiteral.getInt(90 * Math.abs(ii))), ArithOp.DIVIDE);
                }
            };
            PI_DOUBLES.put(Math.PI / (double)(90 * i), pifn);
            pifn = new NonaryFunction<Expression>(){

                @Override
                public Expression invoke() {
                    return new CastExpression(BytecodeLoc.NONE, INFERRED_FLOAT, new ArithmeticOperation(BytecodeLoc.NONE, p, new Literal(TypedLiteral.getInt(90 * Math.abs(ii))), ArithOp.DIVIDE));
                }
            };
            PI_FLOATS.put(Float.valueOf((float)(Math.PI / (double)(90 * i))), pifn);
            pifn = new NonaryFunction<Expression>(){

                @Override
                public Expression invoke() {
                    return new ArithmeticOperation(BytecodeLoc.NONE, new CastExpression(BytecodeLoc.NONE, INFERRED_FLOAT, p), new Literal(TypedLiteral.getInt(90 * Math.abs(ii))), ArithOp.DIVIDE);
                }
            };
            PI_FLOATS.put(Float.valueOf((float)Math.PI / (float)(90 * i)), pifn);
        }
        PI_DOUBLES.put(Math.PI * Math.PI, new NonaryFunction<Expression>(){

            @Override
            public Expression invoke() {
                return new ArithmeticOperation(BytecodeLoc.NONE, pi, pi, ArithOp.MULTIPLY);
            }
        });
        PI_FLOATS.put(Float.valueOf((float)(Math.PI * Math.PI)), new NonaryFunction<Expression>(){

            @Override
            public Expression invoke() {
                return new CastExpression(BytecodeLoc.NONE, INFERRED_FLOAT, new ArithmeticOperation(BytecodeLoc.NONE, pi, pi, ArithOp.MULTIPLY));
            }
        });
    }
}

