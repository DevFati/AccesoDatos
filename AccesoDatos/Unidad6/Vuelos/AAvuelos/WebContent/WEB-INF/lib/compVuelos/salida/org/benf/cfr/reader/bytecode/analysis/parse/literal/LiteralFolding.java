/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.literal;

import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;

public class LiteralFolding {
    public static Literal foldArithmetic(RawJavaType returnType, Literal l, Literal r, ArithOp op) {
        if (!returnType.isNumber()) {
            return null;
        }
        if ((l = LiteralFolding.foldCast(l, returnType)) == null) {
            return null;
        }
        if ((r = LiteralFolding.foldCast(r, returnType)) == null) {
            return null;
        }
        TypedLiteral tl = LiteralFolding.computeLiteral(returnType, l.getValue(), r.getValue(), op);
        if (tl == null) {
            return null;
        }
        return new Literal(tl);
    }

    private static TypedLiteral computeLiteral(RawJavaType type, TypedLiteral l, TypedLiteral r, ArithOp op) {
        switch (type) {
            case BYTE: {
                Integer val = LiteralFolding.computeLiteral(l.getIntValue(), r.getIntValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getInt((int)((byte)val.intValue()), type);
            }
            case SHORT: {
                Integer val = LiteralFolding.computeLiteral(l.getIntValue(), r.getIntValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getInt((int)((short)val.intValue()), type);
            }
            case INT: {
                Integer val = LiteralFolding.computeLiteral(l.getIntValue(), r.getIntValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getInt((int)val, type);
            }
            case LONG: {
                Long val = LiteralFolding.computeLiteral(l.getLongValue(), r.getLongValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getLong(val);
            }
            case FLOAT: {
                Float val = LiteralFolding.computeLiteral(l.getFloatValue(), r.getFloatValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getFloat(val.floatValue());
            }
            case DOUBLE: {
                Double val = LiteralFolding.computeLiteral(l.getDoubleValue(), r.getDoubleValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getDouble(val);
            }
        }
        return null;
    }

    private static Double computeLiteral(double l, double r, ArithOp op) {
        switch (op) {
            case PLUS: {
                return l + r;
            }
            case MINUS: {
                return l - r;
            }
            case MULTIPLY: {
                return l * r;
            }
            case DIVIDE: {
                return l / r;
            }
            case REM: {
                return l % r;
            }
        }
        return null;
    }

    private static Float computeLiteral(float l, float r, ArithOp op) {
        switch (op) {
            case PLUS: {
                return Float.valueOf(l + r);
            }
            case MINUS: {
                return Float.valueOf(l - r);
            }
            case MULTIPLY: {
                return Float.valueOf(l * r);
            }
            case DIVIDE: {
                return Float.valueOf(l / r);
            }
            case REM: {
                return Float.valueOf(l % r);
            }
        }
        return null;
    }

    private static Long computeLiteral(long l, long r, ArithOp op) {
        switch (op) {
            case PLUS: {
                return l + r;
            }
            case MINUS: {
                return l - r;
            }
            case MULTIPLY: {
                return l * r;
            }
            case DIVIDE: {
                if (r == 0L) {
                    return null;
                }
                return l / r;
            }
            case REM: {
                if (r == 0L) {
                    return null;
                }
                return l % r;
            }
            case SHR: {
                return l >> (int)r;
            }
            case SHL: {
                return l << (int)r;
            }
            case SHRU: {
                return l >>> (int)r;
            }
            case XOR: {
                return l ^ r;
            }
        }
        return null;
    }

    private static Integer computeLiteral(int l, int r, ArithOp op) {
        switch (op) {
            case PLUS: {
                return l + r;
            }
            case MINUS: {
                return l - r;
            }
            case MULTIPLY: {
                return l * r;
            }
            case DIVIDE: {
                if (r == 0) {
                    return null;
                }
                return l / r;
            }
            case REM: {
                if (r == 0) {
                    return null;
                }
                return l % r;
            }
            case OR: {
                return l | r;
            }
            case AND: {
                return l & r;
            }
            case SHR: {
                return l >> r;
            }
            case SHL: {
                return l << r;
            }
            case SHRU: {
                return l >>> r;
            }
            case XOR: {
                return l ^ r;
            }
        }
        return null;
    }

    public static Literal foldArithmetic(RawJavaType returnType, Literal l, ArithOp op) {
        if (!returnType.isNumber()) {
            return null;
        }
        if ((l = LiteralFolding.foldCast(l, returnType)) == null) {
            return null;
        }
        TypedLiteral tl = LiteralFolding.computeLiteral(returnType, l.getValue(), op);
        if (tl == null) {
            return null;
        }
        return new Literal(tl);
    }

    private static TypedLiteral computeLiteral(RawJavaType type, TypedLiteral l, ArithOp op) {
        switch (type) {
            case BYTE: {
                Integer val = LiteralFolding.computeLiteral(l.getIntValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getInt((int)((byte)val.intValue()), type);
            }
            case SHORT: {
                Integer val = LiteralFolding.computeLiteral(l.getIntValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getInt((int)((short)val.intValue()), type);
            }
            case INT: {
                Integer val = LiteralFolding.computeLiteral(l.getIntValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getInt((int)val, type);
            }
            case LONG: {
                Long val = LiteralFolding.computeLiteral(l.getLongValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getLong(val);
            }
            case FLOAT: {
                Float val = LiteralFolding.computeLiteral(l.getFloatValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getFloat(val.floatValue());
            }
            case DOUBLE: {
                Double val = LiteralFolding.computeLiteral(l.getDoubleValue(), op);
                if (val == null) {
                    return null;
                }
                return TypedLiteral.getDouble(val);
            }
        }
        return null;
    }

    private static Double computeLiteral(double l, ArithOp op) {
        if (op == ArithOp.MINUS) {
            return -l;
        }
        return null;
    }

    private static Float computeLiteral(float l, ArithOp op) {
        if (op == ArithOp.MINUS) {
            return Float.valueOf(-l);
        }
        return null;
    }

    private static Long computeLiteral(long l, ArithOp op) {
        switch (op) {
            case MINUS: {
                return -l;
            }
            case NEG: {
                return l ^ 0xFFFFFFFFFFFFFFFFL;
            }
        }
        return null;
    }

    private static Integer computeLiteral(int l, ArithOp op) {
        switch (op) {
            case MINUS: {
                return -l;
            }
            case NEG: {
                return ~l;
            }
        }
        return null;
    }

    public static Literal foldCast(Literal val, RawJavaType returnType) {
        if (val == null) {
            return null;
        }
        RawJavaType fromType = LiteralFolding.getRawType(val);
        if (fromType == null) {
            return null;
        }
        if (!fromType.isNumber() && fromType != RawJavaType.BOOLEAN) {
            return null;
        }
        if (!returnType.isNumber()) {
            return null;
        }
        TypedLiteral tl = LiteralFolding.getCast(val.getValue(), fromType, returnType);
        if (tl == null) {
            return null;
        }
        return new Literal(tl);
    }

    private static TypedLiteral getCast(TypedLiteral val, RawJavaType fromType, RawJavaType returnType) {
        if (fromType == returnType) {
            return val;
        }
        switch (returnType) {
            case BYTE: {
                switch (fromType) {
                    case SHORT: 
                    case INT: 
                    case BOOLEAN: {
                        return TypedLiteral.getInt((int)((byte)val.getIntValue()), returnType);
                    }
                    case LONG: {
                        return TypedLiteral.getInt((byte)val.getLongValue());
                    }
                    case FLOAT: {
                        return TypedLiteral.getInt((byte)val.getFloatValue());
                    }
                    case DOUBLE: {
                        return TypedLiteral.getInt((byte)val.getDoubleValue());
                    }
                }
                break;
            }
            case SHORT: {
                switch (fromType) {
                    case BYTE: 
                    case INT: 
                    case BOOLEAN: {
                        return TypedLiteral.getInt((int)((short)val.getIntValue()), returnType);
                    }
                    case LONG: {
                        return TypedLiteral.getInt((short)val.getLongValue());
                    }
                    case FLOAT: {
                        return TypedLiteral.getInt((short)val.getFloatValue());
                    }
                    case DOUBLE: {
                        return TypedLiteral.getInt((short)val.getDoubleValue());
                    }
                }
                break;
            }
            case INT: {
                switch (fromType) {
                    case BYTE: 
                    case SHORT: 
                    case BOOLEAN: {
                        return TypedLiteral.getInt(val.getIntValue(), returnType);
                    }
                    case LONG: {
                        return TypedLiteral.getInt((int)val.getLongValue());
                    }
                    case FLOAT: {
                        return TypedLiteral.getInt((int)val.getFloatValue());
                    }
                    case DOUBLE: {
                        return TypedLiteral.getInt((int)val.getDoubleValue());
                    }
                }
                break;
            }
            case LONG: {
                switch (fromType) {
                    case BYTE: 
                    case SHORT: 
                    case INT: 
                    case BOOLEAN: {
                        return TypedLiteral.getLong(val.getIntValue());
                    }
                    case FLOAT: {
                        return TypedLiteral.getLong((long)val.getFloatValue());
                    }
                    case DOUBLE: {
                        return TypedLiteral.getLong((long)val.getDoubleValue());
                    }
                }
                break;
            }
            case FLOAT: {
                switch (fromType) {
                    case BYTE: 
                    case SHORT: 
                    case INT: 
                    case BOOLEAN: {
                        return TypedLiteral.getFloat(val.getIntValue());
                    }
                    case LONG: {
                        return TypedLiteral.getFloat(val.getLongValue());
                    }
                    case DOUBLE: {
                        return TypedLiteral.getFloat((float)val.getDoubleValue());
                    }
                }
                break;
            }
            case DOUBLE: {
                switch (fromType) {
                    case BYTE: 
                    case SHORT: 
                    case INT: 
                    case BOOLEAN: {
                        return TypedLiteral.getDouble(val.getIntValue());
                    }
                    case LONG: {
                        return TypedLiteral.getDouble(val.getLongValue());
                    }
                    case FLOAT: {
                        return TypedLiteral.getDouble(val.getFloatValue());
                    }
                }
            }
        }
        return null;
    }

    private static RawJavaType getRawType(Literal l) {
        JavaTypeInstance typ = l.getInferredJavaType().getJavaTypeInstance();
        if (typ instanceof RawJavaType) {
            return (RawJavaType)typ;
        }
        return null;
    }
}

