/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractFunctionInvokationExplicit;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.output.Dumper;

public class StaticFunctionInvokationExplicit
extends AbstractFunctionInvokationExplicit {
    public StaticFunctionInvokationExplicit(BytecodeLoc loc, InferredJavaType res, JavaTypeInstance clazz, String method, List<Expression> args) {
        super(loc, res, clazz, method, args);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.getArgs(), new HasByteCodeLoc[0]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof StaticFunctionInvokationExplicit)) {
            return false;
        }
        StaticFunctionInvokationExplicit other = (StaticFunctionInvokationExplicit)o;
        return this.getClazz().equals(other.getClazz()) && this.getMethod().equals(other.getMethod()) && this.getArgs().equals(other.getArgs());
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.dump(this.getClazz()).separator(".").print(this.getMethod()).separator("(");
        boolean first = true;
        for (Expression arg : this.getArgs()) {
            first = StringUtils.comma(first, d);
            d.dump(arg);
        }
        d.separator(")");
        return d;
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof StaticFunctionInvokationExplicit)) {
            return false;
        }
        StaticFunctionInvokationExplicit other = (StaticFunctionInvokationExplicit)o;
        if (!constraint.equivalent(this.getMethod(), other.getMethod())) {
            return false;
        }
        if (!constraint.equivalent(this.getClazz(), other.getClazz())) {
            return false;
        }
        return constraint.equivalent(this.getArgs(), other.getArgs());
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new StaticFunctionInvokationExplicit(this.getLoc(), this.getInferredJavaType(), this.getClazz(), this.getMethod(), cloneHelper.replaceOrClone(this.getArgs()));
    }
}

