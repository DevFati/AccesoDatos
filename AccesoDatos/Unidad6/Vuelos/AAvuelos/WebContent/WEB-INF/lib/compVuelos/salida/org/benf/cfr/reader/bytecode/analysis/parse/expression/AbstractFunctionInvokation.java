/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;

public abstract class AbstractFunctionInvokation
extends AbstractExpression {
    private final ConstantPoolEntryMethodRef function;
    private final MethodPrototype methodPrototype;

    AbstractFunctionInvokation(BytecodeLoc loc, ConstantPoolEntryMethodRef function, InferredJavaType inferredJavaType) {
        super(loc, inferredJavaType);
        this.function = function;
        this.methodPrototype = function.getMethodPrototype();
    }

    public abstract void applyExpressionRewriterToArgs(ExpressionRewriter var1, SSAIdentifiers var2, StatementContainer var3, ExpressionRewriterFlags var4);

    public abstract void setExplicitGenerics(List<JavaTypeInstance> var1);

    public abstract List<JavaTypeInstance> getExplicitGenerics();

    public ConstantPoolEntryMethodRef getFunction() {
        return this.function;
    }

    public MethodPrototype getMethodPrototype() {
        return this.methodPrototype;
    }

    public String getName() {
        return this.methodPrototype.getName();
    }

    String getFixedName() {
        return this.methodPrototype.getFixedName();
    }

    @Override
    public boolean isValidStatement() {
        return true;
    }

    public abstract List<Expression> getArgs();
}

