/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

public class MemberFunctionInvokation
extends AbstractMemberFunctionInvokation {
    private final boolean special;
    private final boolean isInitMethod;

    public MemberFunctionInvokation(BytecodeLoc loc, ConstantPool cp, ConstantPoolEntryMethodRef function, Expression object, JavaTypeInstance bestType, boolean special, List<Expression> args, List<Boolean> nulls) {
        super(loc, cp, function, object, bestType, args, nulls);
        this.isInitMethod = function.isInitMethod();
        this.special = special;
    }

    private MemberFunctionInvokation(BytecodeLoc loc, ConstantPool cp, ConstantPoolEntryMethodRef function, Expression object, boolean special, List<Expression> args, List<Boolean> nulls) {
        super(loc, cp, function, object, args, nulls);
        this.isInitMethod = function.isInitMethod();
        this.special = special;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new MemberFunctionInvokation(this.getLoc(), this.getCp(), this.getFunction(), cloneHelper.replaceOrClone(this.getObject()), this.special, cloneHelper.replaceOrClone(this.getArgs()), this.getNulls());
    }

    public MemberFunctionInvokation withReplacedObject(Expression object) {
        return new MemberFunctionInvokation(this.getLoc(), this.getCp(), this.getFunction(), object, this.special, this.getArgs(), this.getNulls());
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        this.getObject().dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.NEITHER);
        MethodPrototype methodPrototype = this.getMethodPrototype();
        if (!this.isInitMethod) {
            d.separator(".").methodName(this.getFixedName(), methodPrototype, false, false);
        }
        d.separator("(");
        List<Expression> args = this.getArgs();
        boolean first = true;
        for (int x = 0; x < args.size(); ++x) {
            if (methodPrototype.isHiddenArg(x)) continue;
            Expression arg = args.get(x);
            first = StringUtils.comma(first, d);
            methodPrototype.dumpAppropriatelyCastedArgumentString(arg, d);
        }
        d.separator(")");
        return d;
    }

    public boolean isInitMethod() {
        return this.isInitMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof MemberFunctionInvokation)) {
            return false;
        }
        return this.getName().equals(((MemberFunctionInvokation)o).getName());
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (!super.equivalentUnder(o, constraint)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof MemberFunctionInvokation)) {
            return false;
        }
        MemberFunctionInvokation other = (MemberFunctionInvokation)o;
        return constraint.equivalent(this.getName(), other.getName());
    }
}

