/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.VarArgsRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractConstructorInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.FunctionProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.output.Dumper;

public class ConstructorInvokationSimple
extends AbstractConstructorInvokation
implements FunctionProcessor {
    private final MemberFunctionInvokation constructorInvokation;
    private InferredJavaType constructionType;

    public ConstructorInvokationSimple(BytecodeLoc loc, MemberFunctionInvokation constructorInvokation, InferredJavaType inferredJavaType, InferredJavaType constructionType, List<Expression> args) {
        super(loc, inferredJavaType, constructorInvokation.getFunction(), args);
        this.constructorInvokation = constructorInvokation;
        this.constructionType = constructionType;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine((HasByteCodeLoc)this, this.getArgs(), this.constructorInvokation);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new ConstructorInvokationSimple(this.getLoc(), this.constructorInvokation, this.getInferredJavaType(), this.constructionType, cloneHelper.replaceOrClone(this.getArgs()));
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    private JavaTypeInstance getFinalDisplayTypeInstance() {
        JavaTypeInstance res = this.constructionType.getJavaTypeInstance();
        if (!(res instanceof JavaGenericBaseInstance)) {
            return res;
        }
        if (!((JavaGenericBaseInstance)res).hasL01Wildcard()) {
            return res;
        }
        res = ((JavaGenericBaseInstance)res).getWithoutL01Wildcard();
        return res;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        ClassFileField classFileField;
        LValue lValue;
        Expression a1;
        JavaTypeInstance clazz = this.getFinalDisplayTypeInstance();
        List<Expression> args = this.getArgs();
        MethodPrototype prototype = this.constructorInvokation.getMethodPrototype();
        if (!(!prototype.isInnerOuterThis() || !prototype.isHiddenArg(0) || args.size() <= 0 || (a1 = args.get(0)).toString().equals("this") || a1 instanceof LValueExpression && (lValue = ((LValueExpression)a1).getLValue()) instanceof FieldVariable && (classFileField = ((FieldVariable)lValue).getClassFileField()).isSyntheticOuterRef())) {
            d.dump(a1).print('.');
        }
        d.keyword("new ").dump(clazz).separator("(");
        boolean first = true;
        for (int i = 0; i < args.size(); ++i) {
            if (prototype.isHiddenArg(i)) continue;
            Expression arg = args.get(i);
            if (!first) {
                d.print(", ");
            }
            first = false;
            d.dump(arg);
        }
        d.separator(")");
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof ConstructorInvokationSimple)) {
            return false;
        }
        return super.equals(o);
    }

    public static boolean isAnonymousMethodType(JavaTypeInstance lValueType) {
        InnerClassInfo innerClassInfo = lValueType.getInnerClassHereInfo();
        return innerClassInfo.isMethodScopedClass() && !innerClassInfo.isAnonymousClass();
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        JavaTypeInstance lValueType = this.constructorInvokation.getClassTypeInstance();
        if (ConstructorInvokationSimple.isAnonymousMethodType(lValueType)) {
            lValueUsageCollector.collect(new SentinelLocalClassLValue(lValueType), ReadWrite.READ);
        }
        super.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (!(o instanceof ConstructorInvokationSimple)) {
            return false;
        }
        return super.equivalentUnder(o, constraint);
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return caught.checkAgainst(this.constructorInvokation);
    }

    @Override
    public void rewriteVarArgs(VarArgsRewriter varArgsRewriter) {
        MethodPrototype methodPrototype = this.getMethodPrototype();
        if (!methodPrototype.isVarArgs()) {
            return;
        }
        OverloadMethodSet overloadMethodSet = this.getOverloadMethodSet();
        if (overloadMethodSet == null) {
            return;
        }
        GenericTypeBinder gtb = methodPrototype.getTypeBinderFor(this.getArgs());
        varArgsRewriter.rewriteVarArgsArg(overloadMethodSet, methodPrototype, this.getArgs(), gtb);
    }

    public MethodPrototype getConstructorPrototype() {
        return this.getMethodPrototype();
    }
}

