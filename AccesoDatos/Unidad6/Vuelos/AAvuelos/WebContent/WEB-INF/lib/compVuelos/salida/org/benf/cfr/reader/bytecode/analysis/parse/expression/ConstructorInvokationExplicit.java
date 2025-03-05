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

public class ConstructorInvokationExplicit
extends AbstractFunctionInvokationExplicit {
    ConstructorInvokationExplicit(BytecodeLoc loc, InferredJavaType res, JavaTypeInstance clazz, List<Expression> args) {
        super(loc, res, clazz, null, args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof ConstructorInvokationExplicit)) {
            return false;
        }
        ConstructorInvokationExplicit other = (ConstructorInvokationExplicit)o;
        return this.getClazz().equals(other.getClazz()) && this.getArgs().equals(other.getArgs());
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.getArgs(), new HasByteCodeLoc[0]);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.WEAKEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.keyword("new ").dump(this.getClazz()).separator("(");
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
        if (!(o instanceof ConstructorInvokationExplicit)) {
            return false;
        }
        ConstructorInvokationExplicit other = (ConstructorInvokationExplicit)o;
        if (!constraint.equivalent(this.getClazz(), other.getClazz())) {
            return false;
        }
        return constraint.equivalent(this.getArgs(), other.getArgs());
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new ConstructorInvokationExplicit(this.getLoc(), this.getInferredJavaType(), this.getClazz(), cloneHelper.replaceOrClone(this.getArgs()));
    }
}

