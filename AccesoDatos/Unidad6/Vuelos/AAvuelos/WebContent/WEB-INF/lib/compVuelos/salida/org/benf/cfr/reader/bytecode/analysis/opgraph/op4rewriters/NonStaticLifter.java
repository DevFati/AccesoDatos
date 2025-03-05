/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.ConstructorUtils;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Field;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;

public class NonStaticLifter {
    private final ClassFile classFile;

    public NonStaticLifter(ClassFile classFile) {
        this.classFile = classFile;
    }

    public void liftNonStatics() {
        Pair<List<ClassFileField>, List<ClassFileField>> fields = Functional.partition(this.classFile.getFields(), new Predicate<ClassFileField>(){

            @Override
            public boolean test(ClassFileField in) {
                if (in.getField().testAccessFlag(AccessFlag.ACC_STATIC)) {
                    return false;
                }
                return !in.getField().testAccessFlag(AccessFlag.ACC_SYNTHETIC);
            }
        });
        LinkedList classFileFields = new LinkedList(fields.getFirst());
        Map other = MapFactory.newMap();
        for (ClassFileField otherField : fields.getSecond()) {
            other.put(otherField.getFieldName(), otherField);
        }
        if (classFileFields.isEmpty()) {
            return;
        }
        Map<String, Pair<Integer, ClassFileField>> fieldMap = MapFactory.newMap();
        int len = classFileFields.size();
        for (int x = 0; x < len; ++x) {
            ClassFileField classFileField = (ClassFileField)classFileFields.get(x);
            fieldMap.put(classFileField.getField().getFieldName(), Pair.make(x, classFileField));
        }
        List<Method> constructors = Functional.filter(this.classFile.getConstructors(), new Predicate<Method>(){

            @Override
            public boolean test(Method in) {
                return !ConstructorUtils.isDelegating(in);
            }
        });
        List<List> constructorCodeList = ListFactory.newList();
        int minSize = Integer.MAX_VALUE;
        for (Method constructor : constructors) {
            Expression expression;
            List<Op04StructuredStatement> blockStatements = MiscStatementTools.getBlockStatements(constructor.getAnalysis());
            if (blockStatements == null) {
                return;
            }
            if ((blockStatements = Functional.filter(blockStatements, new Predicate<Op04StructuredStatement>(){

                @Override
                public boolean test(Op04StructuredStatement in) {
                    StructuredStatement stm = in.getStatement();
                    if (stm instanceof StructuredComment) {
                        return false;
                    }
                    return !(stm instanceof StructuredDefinition);
                }
            })).isEmpty()) {
                return;
            }
            StructuredStatement superTest = blockStatements.get(0).getStatement();
            if (superTest instanceof StructuredExpressionStatement && (expression = ((StructuredExpressionStatement)superTest).getExpression()) instanceof SuperFunctionInvokation) {
                blockStatements.remove(0);
            }
            constructorCodeList.add(blockStatements);
            if (blockStatements.size() >= minSize) continue;
            minSize = blockStatements.size();
        }
        if (constructorCodeList.isEmpty()) {
            return;
        }
        int numConstructors = constructorCodeList.size();
        List constructorCode = constructorCodeList.get(0);
        if (constructorCode.isEmpty()) {
            return;
        }
        Set<Expression> usedFvs = SetFactory.newSet();
        int maxFieldIdx = -1;
        for (int x = 0; x < minSize; ++x) {
            StructuredStatement s1 = ((Op04StructuredStatement)constructorCode.get(x)).getStatement();
            for (int y = 1; y < numConstructors; ++y) {
                StructuredStatement sOther = ((Op04StructuredStatement)constructorCodeList.get(y).get(x)).getStatement();
                if (s1.equals(sOther)) continue;
                return;
            }
            if (!(s1 instanceof StructuredAssignment)) {
                return;
            }
            StructuredAssignment structuredAssignment = (StructuredAssignment)s1;
            LValue lValue = structuredAssignment.getLvalue();
            if (!(lValue instanceof FieldVariable)) {
                return;
            }
            FieldVariable fieldVariable = (FieldVariable)lValue;
            if (!this.fromThisClass(fieldVariable)) {
                return;
            }
            Expression rValue = structuredAssignment.getRvalue();
            if (!this.tryLift(fieldVariable, rValue, fieldMap, usedFvs)) {
                ClassFileField f = (ClassFileField)other.get(fieldVariable.getFieldName());
                if (f == null) {
                    return;
                }
                Field field = f.getField();
                if (field.testAccessFlag(AccessFlag.ACC_SYNTHETIC) && !field.testAccessFlag(AccessFlag.ACC_STATIC)) continue;
                return;
            }
            Pair<Integer, ClassFileField> fieldPair = fieldMap.get(fieldVariable.getFieldName());
            ClassFileField f = fieldPair.getSecond();
            Field field = f.getField();
            boolean rLit = rValue instanceof Literal;
            if (field.testAccessFlag(AccessFlag.ACC_FINAL) && field.getConstantValue() == null && rLit && (field.getJavaTypeInstance().isRaw() || field.getJavaTypeInstance() == TypeConstants.STRING)) continue;
            if (!rLit) {
                int fieldIdx = fieldPair.getFirst();
                if (fieldIdx < maxFieldIdx) {
                    maxFieldIdx = Integer.MAX_VALUE;
                    continue;
                }
                maxFieldIdx = fieldIdx;
            }
            f.setInitialValue(rValue);
            for (List constructorCodeLst1 : constructorCodeList) {
                ((Op04StructuredStatement)constructorCodeLst1.get(x)).nopOut();
            }
            usedFvs.add(fieldVariable.getObject());
        }
    }

    private boolean fromThisClass(FieldVariable fv) {
        return fv.getOwningClassType().equals(this.classFile.getClassType());
    }

    private boolean tryLift(FieldVariable lValue, Expression rValue, Map<String, Pair<Integer, ClassFileField>> fieldMap, Set<Expression> usedFvs) {
        Pair<Integer, ClassFileField> thisField = fieldMap.get(lValue.getFieldName());
        if (thisField == null) {
            return false;
        }
        return this.hasLegitArgs(rValue, usedFvs);
    }

    private boolean hasLegitArgs(Expression rValue, Set<Expression> usedFvs) {
        LValueUsageCollectorSimple usageCollector = new LValueUsageCollectorSimple();
        rValue.collectUsedLValues(usageCollector);
        for (LValue usedLValue : usageCollector.getUsedLValues()) {
            LocalVariable variable;
            if (usedLValue instanceof StaticVariable) continue;
            if (usedLValue instanceof FieldVariable) {
                if (usedFvs.contains(((FieldVariable)usedLValue).getObject())) continue;
                return false;
            }
            if (usedLValue instanceof LocalVariable && (variable = (LocalVariable)usedLValue).getInferredJavaType().getJavaTypeInstance() == this.classFile.getClassType() && variable.getName().getStringName().equals("this")) continue;
            return false;
        }
        return true;
    }
}

