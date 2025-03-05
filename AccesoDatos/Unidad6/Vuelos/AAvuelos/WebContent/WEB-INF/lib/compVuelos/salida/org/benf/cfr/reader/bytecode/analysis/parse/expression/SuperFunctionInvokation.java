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
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class SuperFunctionInvokation
extends AbstractMemberFunctionInvokation {
    private final boolean isOnInterface;
    private final JavaTypeInstance typeName;

    public SuperFunctionInvokation(BytecodeLoc loc, ConstantPool cp, ConstantPoolEntryMethodRef function, Expression object, List<Expression> args, List<Boolean> nulls, boolean isOnInterface) {
        super(loc, cp, function, object, args, nulls);
        this.isOnInterface = isOnInterface;
        this.typeName = null;
    }

    private SuperFunctionInvokation(BytecodeLoc loc, ConstantPool cp, ConstantPoolEntryMethodRef function, Expression object, List<Expression> args, List<Boolean> nulls, boolean isOnInterface, JavaTypeInstance name) {
        super(loc, cp, function, object, args, nulls);
        this.isOnInterface = isOnInterface;
        this.typeName = name;
    }

    public SuperFunctionInvokation withCustomName(JavaTypeInstance name) {
        return new SuperFunctionInvokation(this.getLoc(), this.getCp(), this.getFunction(), this.getObject(), this.getArgs(), this.getNulls(), this.isOnInterface, name);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new SuperFunctionInvokation(this.getLoc(), this.getCp(), this.getFunction(), cloneHelper.replaceOrClone(this.getObject()), cloneHelper.replaceOrClone(this.getArgs()), this.getNulls(), this.isOnInterface, this.typeName);
    }

    public boolean isEmptyIgnoringSynthetics() {
        MethodPrototype prototype = this.getMethodPrototype();
        int len = prototype.getArgs().size();
        for (int i = 0; i < len; ++i) {
            if (prototype.isHiddenArg(i)) continue;
            return false;
        }
        return true;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (this.isOnInterface) {
            collector.collect(this.getFunction().getClassEntry().getTypeInstance());
        }
        collector.collect(this.typeName);
        super.collectTypeUsages(collector);
    }

    public boolean isInit() {
        return this.getMethodPrototype().getName().equals("<init>");
    }

    @Override
    protected OverloadMethodSet getOverloadMethodSetInner(JavaTypeInstance objectType) {
        return null;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        MethodPrototype methodPrototype = this.getMethodPrototype();
        List<Expression> args = this.getArgs();
        if (methodPrototype.getName().equals("<init>")) {
            d.print("super(");
        } else {
            if (this.isOnInterface) {
                d.dump(this.getFunction().getClassEntry().getTypeInstance()).separator(".");
            }
            if (this.typeName != null) {
                d.dump(this.typeName).separator(".");
            }
            d.print("super").separator(".").methodName(methodPrototype.getFixedName(), methodPrototype, false, false).separator("(");
        }
        boolean first = true;
        for (int x = 0; x < args.size(); ++x) {
            if (methodPrototype.isHiddenArg(x)) continue;
            Expression arg = args.get(x);
            if (!first) {
                d.print(", ");
            }
            first = false;
            methodPrototype.dumpAppropriatelyCastedArgumentString(arg, d);
        }
        d.separator(")");
        return d;
    }

    @Override
    public String getName() {
        return "super";
    }
}

