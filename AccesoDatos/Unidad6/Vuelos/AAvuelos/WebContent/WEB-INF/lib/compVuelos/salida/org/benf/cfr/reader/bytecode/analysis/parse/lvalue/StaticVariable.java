/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.lvalue;

import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.AbstractFieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.TypeContext;

public class StaticVariable
extends AbstractFieldVariable {
    private final boolean knownSimple;

    public StaticVariable(ConstantPoolEntry field) {
        super(field);
        this.knownSimple = false;
    }

    public StaticVariable(InferredJavaType type, JavaTypeInstance clazz, String varName) {
        super(type, clazz, varName);
        this.knownSimple = false;
    }

    public StaticVariable(ClassFile classFile, ClassFileField classFileField, boolean local) {
        super(new InferredJavaType(classFileField.getField().getJavaTypeInstance(), InferredJavaType.Source.FIELD, true), classFile.getClassType(), classFileField);
        this.knownSimple = local;
    }

    private StaticVariable(StaticVariable other, boolean knownSimple) {
        super(other);
        this.knownSimple = knownSimple;
    }

    public StaticVariable getSimpleCopy() {
        if (this.knownSimple) {
            return this;
        }
        return new StaticVariable(this, true);
    }

    public StaticVariable getNonSimpleCopy() {
        if (!this.knownSimple) {
            return this;
        }
        return new StaticVariable(this, false);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.HIGHEST;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return false;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        if (this.knownSimple) {
            return d.fieldName(this.getFieldName(), this.getOwningClassType(), false, true, false);
        }
        return d.dump(this.getOwningClassType(), TypeContext.Static).separator(".").fieldName(this.getFieldName(), this.getOwningClassType(), false, true, false);
    }

    @Override
    public LValue deepClone(CloneHelper cloneHelper) {
        return this;
    }

    @Override
    public LValue replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        return this;
    }

    @Override
    public LValue applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StaticVariable)) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}

