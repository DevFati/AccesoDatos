/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.ConfusedCFRException;

public enum ArithOp {
    LCMP("LCMP", true, false, Precedence.WEAKEST),
    DCMPL("DCMPL", true, false, Precedence.WEAKEST),
    DCMPG("DCMPG", true, false, Precedence.WEAKEST),
    FCMPL("FCMPL", true, false, Precedence.WEAKEST),
    FCMPG("FCMPG", true, false, Precedence.WEAKEST),
    PLUS("+", false, false, Precedence.ADD_SUB),
    MINUS("-", false, false, Precedence.ADD_SUB),
    MULTIPLY("*", false, false, Precedence.MUL_DIV_MOD),
    DIVIDE("/", false, false, Precedence.MUL_DIV_MOD),
    REM("%", false, false, Precedence.MUL_DIV_MOD),
    OR("|", false, true, Precedence.BIT_OR),
    AND("&", false, true, Precedence.BIT_AND),
    SHR(">>", false, false, Precedence.BITWISE_SHIFT),
    SHL("<<", false, false, Precedence.BITWISE_SHIFT),
    SHRU(">>>", false, false, Precedence.BITWISE_SHIFT),
    XOR("^", false, true, Precedence.BIT_XOR),
    NEG("~", false, false, Precedence.UNARY_OTHER);

    private final String showAs;
    private final boolean temporary;
    private final boolean boolSafe;
    private final Precedence precedence;

    private ArithOp(String showAs, boolean temporary, boolean boolSafe, Precedence precedence) {
        this.showAs = showAs;
        this.temporary = temporary;
        this.boolSafe = boolSafe;
        this.precedence = precedence;
    }

    public String getShowAs() {
        return this.showAs;
    }

    public boolean isTemporary() {
        return this.temporary;
    }

    public Precedence getPrecedence() {
        return this.precedence;
    }

    public static ArithOp getOpFor(JVMInstr instr) {
        switch (instr) {
            case LCMP: {
                return LCMP;
            }
            case DCMPG: {
                return DCMPG;
            }
            case DCMPL: {
                return DCMPL;
            }
            case FCMPG: {
                return FCMPG;
            }
            case FCMPL: {
                return FCMPL;
            }
            case ISUB: 
            case LSUB: 
            case FSUB: 
            case DSUB: {
                return MINUS;
            }
            case IMUL: 
            case LMUL: 
            case FMUL: 
            case DMUL: {
                return MULTIPLY;
            }
            case IADD: 
            case LADD: 
            case FADD: 
            case DADD: {
                return PLUS;
            }
            case LDIV: 
            case IDIV: 
            case FDIV: 
            case DDIV: {
                return DIVIDE;
            }
            case LOR: 
            case IOR: {
                return OR;
            }
            case LAND: 
            case IAND: {
                return AND;
            }
            case IREM: 
            case LREM: 
            case FREM: 
            case DREM: {
                return REM;
            }
            case ISHR: 
            case LSHR: {
                return SHR;
            }
            case IUSHR: 
            case LUSHR: {
                return SHRU;
            }
            case ISHL: 
            case LSHL: {
                return SHL;
            }
            case IXOR: 
            case LXOR: {
                return XOR;
            }
        }
        throw new ConfusedCFRException("Don't know arith op for " + (Object)((Object)instr));
    }

    public boolean canThrow(InferredJavaType inferredJavaType, ExceptionCheck caught, Set<? extends JavaTypeInstance> instances) {
        StackType stackType = inferredJavaType.getRawType().getStackType();
        switch (stackType) {
            case DOUBLE: 
            case FLOAT: 
            case INT: 
            case LONG: {
                if (this == DIVIDE) break;
                return false;
            }
        }
        return caught.checkAgainst(instances);
    }

    public boolean isBoolSafe() {
        return this.boolSafe;
    }
}

