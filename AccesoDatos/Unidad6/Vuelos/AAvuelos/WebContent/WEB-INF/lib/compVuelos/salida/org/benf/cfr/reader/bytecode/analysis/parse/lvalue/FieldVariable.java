/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.lvalue;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.AbstractFieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

public class FieldVariable
extends AbstractFieldVariable {
    private Expression object;

    public FieldVariable(Expression object, ConstantPoolEntry field) {
        super(field);
        this.object = object;
    }

    public FieldVariable(Expression object, ClassFileField field, JavaTypeInstance owningClass) {
        super(field, owningClass);
        this.object = object;
    }

    private FieldVariable(FieldVariable other, CloneHelper cloneHelper) {
        super(other);
        this.object = cloneHelper.replaceOrClone(other.object);
    }

    private FieldVariable(FieldVariable other, Expression object) {
        super(other);
        this.object = object;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        super.collectTypeUsages(collector);
        collector.collectFrom(this.object);
    }

    @Override
    public LValue deepClone(CloneHelper cloneHelper) {
        return new FieldVariable(this, cloneHelper);
    }

    public FieldVariable withReplacedObject(Expression object) {
        return new FieldVariable(this, object);
    }

    private boolean isOuterRef() {
        ClassFileField classFileField = this.getClassFileField();
        return classFileField != null && classFileField.isSyntheticOuterRef();
    }

    public Expression getObject() {
        return this.object;
    }

    private boolean objectIsEclipseOuterThis() {
        LValue lValue;
        return this.object instanceof LValueExpression && (lValue = ((LValueExpression)this.object).getLValue()) instanceof FieldVariable && ((FieldVariable)lValue).getClassFileField().getFieldName().endsWith(".this");
    }

    public boolean objectIsThis() {
        LValue lValue;
        if (this.object instanceof LValueExpression && (lValue = ((LValueExpression)this.object).getLValue()) instanceof LocalVariable) {
            return ((LocalVariable)lValue).getName().getStringName().equals("this");
        }
        return false;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        if (!super.canThrow(caught)) {
            return false;
        }
        return !this.objectIsThis();
    }

    private boolean objectIsIllegalThis() {
        LValue lValue;
        if (this.object instanceof LValueExpression && (lValue = ((LValueExpression)this.object).getLValue()) instanceof FieldVariable) {
            FieldVariable fv = (FieldVariable)lValue;
            return fv.getFieldName().equals("this");
        }
        return false;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        if (!(this.isOuterRef() && (this.objectIsThis() || this.objectIsEclipseOuterThis()) || this.objectIsIllegalThis())) {
            this.object.dumpWithOuterPrecedence(d, this.getPrecedence(), Troolean.NEITHER).separator(".");
        }
        return d.fieldName(this.getFieldName(), this.getOwningClassType(), this.isHiddenDeclaration(), false, false);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        this.object.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public LValue replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        this.object = this.object.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public LValue applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        this.object = expressionRewriter.rewriteExpression(this.object, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    public void rewriteLeftNestedSyntheticOuterRefs() {
        if (this.isOuterRef()) {
            FieldVariable lhs;
            LValue lValueLhs;
            while (this.object instanceof LValueExpression && (lValueLhs = ((LValueExpression)this.object).getLValue()) instanceof FieldVariable && (lhs = (FieldVariable)lValueLhs).isOuterRef()) {
                this.object = lhs.object;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FieldVariable)) {
            return false;
        }
        FieldVariable other = (FieldVariable)o;
        if (!super.equals(o)) {
            return false;
        }
        return this.object.equals(other.object);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}

