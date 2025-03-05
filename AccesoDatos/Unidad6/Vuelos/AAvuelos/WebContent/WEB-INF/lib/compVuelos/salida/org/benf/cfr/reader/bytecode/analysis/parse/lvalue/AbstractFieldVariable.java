/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.lvalue;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.AbstractLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Field;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryFieldRef;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.ConfusedCFRException;

public abstract class AbstractFieldVariable
extends AbstractLValue {
    private final ClassFileField classFileField;
    private final String failureName;
    private final JavaTypeInstance owningClass;

    AbstractFieldVariable(ConstantPoolEntry field) {
        super(AbstractFieldVariable.getFieldType((ConstantPoolEntryFieldRef)field));
        ConstantPoolEntryFieldRef fieldRef = (ConstantPoolEntryFieldRef)field;
        this.classFileField = AbstractFieldVariable.getField(fieldRef);
        this.failureName = fieldRef.getLocalName();
        this.owningClass = fieldRef.getClassEntry().getTypeInstance();
    }

    AbstractFieldVariable(ClassFileField field, JavaTypeInstance owningClass) {
        super(new InferredJavaType(field.getField().getJavaTypeInstance(), InferredJavaType.Source.UNKNOWN));
        this.classFileField = field;
        this.failureName = field.getFieldName();
        this.owningClass = owningClass;
    }

    AbstractFieldVariable(AbstractFieldVariable other) {
        super(other.getInferredJavaType());
        this.classFileField = other.classFileField;
        this.failureName = other.failureName;
        this.owningClass = other.owningClass;
    }

    AbstractFieldVariable(InferredJavaType type, JavaTypeInstance clazz, String varName) {
        super(type);
        this.classFileField = null;
        this.owningClass = clazz;
        this.failureName = varName;
    }

    AbstractFieldVariable(InferredJavaType type, JavaTypeInstance clazz, ClassFileField classFileField) {
        super(type);
        this.classFileField = classFileField;
        this.owningClass = clazz;
        this.failureName = null;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        super.collectTypeUsages(collector);
        if (this.classFileField != null) {
            collector.collect(this.classFileField.getField().getJavaTypeInstance());
        }
        collector.collect(this.owningClass);
    }

    @Override
    public void markFinal() {
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isFakeIgnored() {
        return false;
    }

    @Override
    public void markVar() {
    }

    @Override
    public boolean isVar() {
        return false;
    }

    @Override
    public int getNumberOfCreators() {
        throw new ConfusedCFRException("NYI");
    }

    public JavaTypeInstance getOwningClassType() {
        return this.owningClass;
    }

    public String getFieldName() {
        if (this.classFileField == null) {
            return this.failureName;
        }
        return this.classFileField.getFieldName();
    }

    protected boolean isHiddenDeclaration() {
        if (this.classFileField == null) {
            return false;
        }
        return this.classFileField.shouldNotDisplay();
    }

    public String getRawFieldName() {
        if (this.classFileField == null) {
            return this.failureName;
        }
        return this.classFileField.getRawFieldName();
    }

    public ClassFileField getClassFileField() {
        return this.classFileField;
    }

    @Override
    public SSAIdentifiers<LValue> collectVariableMutation(SSAIdentifierFactory<LValue, ?> ssaIdentifierFactory) {
        return new SSAIdentifiers<LValue>(this, ssaIdentifierFactory);
    }

    public void collectLValueAssignments(Expression assignedTo, StatementContainer statementContainer, LValueAssignmentCollector lValueAssigmentCollector) {
    }

    public static ClassFileField getField(ConstantPoolEntryFieldRef fieldRef) {
        String name = fieldRef.getLocalName();
        JavaRefTypeInstance ref = (JavaRefTypeInstance)fieldRef.getClassEntry().getTypeInstance();
        try {
            ClassFile classFile = ref.getClassFile();
            if (classFile == null) {
                return null;
            }
            ClassFileField field = classFile.getFieldByName(name, fieldRef.getJavaTypeInstance());
            return field;
        }
        catch (NoSuchFieldException noSuchFieldException) {
        }
        catch (CannotLoadClassException cannotLoadClassException) {
            // empty catch block
        }
        return null;
    }

    private static InferredJavaType getFieldType(ConstantPoolEntryFieldRef fieldRef) {
        String name = fieldRef.getLocalName();
        JavaRefTypeInstance ref = (JavaRefTypeInstance)fieldRef.getClassEntry().getTypeInstance();
        try {
            ClassFile classFile = ref.getClassFile();
            if (classFile != null) {
                Field field = classFile.getFieldByName(name, fieldRef.getJavaTypeInstance()).getField();
                return new InferredJavaType(field.getJavaTypeInstance(), InferredJavaType.Source.FIELD, true);
            }
        }
        catch (CannotLoadClassException cannotLoadClassException) {
        }
        catch (NoSuchFieldException noSuchFieldException) {
            // empty catch block
        }
        return new InferredJavaType(fieldRef.getJavaTypeInstance(), InferredJavaType.Source.FIELD, true);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractFieldVariable)) {
            return false;
        }
        AbstractFieldVariable that = (AbstractFieldVariable)o;
        if (!this.getFieldName().equals(that.getFieldName())) {
            return false;
        }
        return !(this.owningClass != null ? !this.owningClass.equals(that.owningClass) : that.owningClass != null);
    }

    public int hashCode() {
        int result = this.getFieldName().hashCode();
        result = 31 * result + (this.owningClass != null ? this.owningClass.hashCode() : 0);
        return result;
    }
}

