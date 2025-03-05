/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.ConstructorUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.QuotingUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperRecord;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodHandle;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.Optional;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;

public class RecordRewriter {
    private static Set<AccessFlag> recordFieldFlags = SetFactory.newSet(AccessFlag.ACC_FINAL, AccessFlag.ACC_PRIVATE);
    private static Set<AccessFlagMethod> recordGetterFlags = SetFactory.newSet(AccessFlagMethod.ACC_PUBLIC);

    public static void rewrite(ClassFile classFile, DCCommonState state) {
        if (!TypeConstants.RECORD.equals(classFile.getBaseClassType())) {
            return;
        }
        RecordRewriter.rewriteIfRecord(classFile, state);
    }

    private static boolean rewriteIfRecord(ClassFile classFile, DCCommonState state) {
        List<ClassFileField> instances = Functional.filter(classFile.getFields(), new Predicate<ClassFileField>(){

            @Override
            public boolean test(ClassFileField in) {
                return !in.getField().testAccessFlag(AccessFlag.ACC_STATIC);
            }
        });
        if (!Functional.filter(instances, new Predicate<ClassFileField>(){

            @Override
            public boolean test(ClassFileField in) {
                return !recordFieldFlags.equals(in.getField().getAccessFlags());
            }
        }).isEmpty()) {
            return false;
        }
        List getters = ListFactory.newList();
        for (ClassFileField cff : instances) {
            List<Method> methods = classFile.getMethodsByNameOrNull(cff.getFieldName());
            if (methods == null) {
                return false;
            }
            if ((methods = Functional.filter(methods, new Predicate<Method>(){

                @Override
                public boolean test(Method in) {
                    return in.getMethodPrototype().getArgs().isEmpty();
                }
            })).size() != 1) {
                return false;
            }
            Method method = methods.get(0);
            if (!recordGetterFlags.equals(method.getAccessFlags())) {
                return false;
            }
            getters.add(method);
        }
        List<Method> constructors = classFile.getConstructors();
        Pair<List<Method>, List<Method>> splitCons = Functional.partition(constructors, new IsCanonicalConstructor(instances));
        if (splitCons.getFirst().size() != 1) {
            return false;
        }
        Method canonicalCons = splitCons.getFirst().get(0);
        Method.MethodConstructor constructorFlag = canonicalCons.getConstructorFlag();
        if (!constructorFlag.isConstructor()) {
            return false;
        }
        if (constructorFlag.isEnumConstructor()) {
            return false;
        }
        List<Method> otherCons = splitCons.getSecond();
        for (Method other : otherCons) {
            MethodPrototype chain = ConstructorUtils.getDelegatingPrototype(other);
            if (chain != null) continue;
            return false;
        }
        JavaRefTypeInstance thisType = classFile.getRefClassType();
        if (RecordRewriter.removeImplicitAssignments(canonicalCons, instances, thisType)) {
            canonicalCons.setConstructorFlag(Method.MethodConstructor.RECORD_CANONICAL_CONSTRUCTOR);
        }
        RecordRewriter.hideConstructorIfEmpty(canonicalCons);
        for (int x = 0; x < getters.size(); ++x) {
            RecordRewriter.hideDefaultGetter((Method)getters.get(x), instances.get(x), thisType);
        }
        RecordRewriter.hideDefaultUtilityMethods(classFile, thisType, instances);
        classFile.setDumpHelper(new ClassFileDumperRecord(state));
        return true;
    }

    private static void hideDefaultUtilityMethods(ClassFile classFile, JavaTypeInstance thisType, List<ClassFileField> instances) {
        RecordRewriter.hideEquals(classFile, thisType, instances);
        RecordRewriter.hideToString(classFile, thisType, instances);
        RecordRewriter.hideHashCode(classFile, thisType, instances);
    }

    private static void hideEquals(ClassFile classFile, JavaTypeInstance thisType, List<ClassFileField> fields) {
        Method method = RecordRewriter.getMethod(classFile, Collections.singletonList(TypeConstants.OBJECT), "equals");
        if (method == null) {
            return;
        }
        WildcardMatch wcm = new WildcardMatch();
        StructuredReturn stm = new StructuredReturn(BytecodeLoc.NONE, new CastExpression(BytecodeLoc.NONE, new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.TEST), wcm.getStaticFunction("func", (JavaTypeInstance)TypeConstants.OBJECTMETHODS, (JavaTypeInstance)TypeConstants.OBJECT, "bootstrap", new Literal(TypedLiteral.getString(QuotingUtils.enquoteString("equals"))), wcm.getExpressionWildCard("array"), wcm.getExpressionWildCard("this"), new LValueExpression(method.getMethodPrototype().getComputedParameters().get(0)))), RawJavaType.BOOLEAN);
        RecordRewriter.hideIfMatch(thisType, fields, method, wcm, stm);
    }

    private static void hideToString(ClassFile classFile, JavaTypeInstance thisType, List<ClassFileField> fields) {
        Method method = RecordRewriter.getMethod(classFile, Collections.<JavaTypeInstance>emptyList(), "toString");
        if (method == null) {
            return;
        }
        WildcardMatch wcm = new WildcardMatch();
        StructuredReturn stm = new StructuredReturn(BytecodeLoc.NONE, wcm.getStaticFunction("func", (JavaTypeInstance)TypeConstants.OBJECTMETHODS, (JavaTypeInstance)TypeConstants.OBJECT, "bootstrap", new Literal(TypedLiteral.getString(QuotingUtils.enquoteString("toString"))), wcm.getExpressionWildCard("array"), wcm.getExpressionWildCard("this")), TypeConstants.STRING);
        RecordRewriter.hideIfMatch(thisType, fields, method, wcm, stm);
    }

    private static void hideHashCode(ClassFile classFile, JavaTypeInstance thisType, List<ClassFileField> fields) {
        Method method = RecordRewriter.getMethod(classFile, Collections.<JavaTypeInstance>emptyList(), "hashCode");
        if (method == null) {
            return;
        }
        WildcardMatch wcm = new WildcardMatch();
        StructuredReturn stm = new StructuredReturn(BytecodeLoc.NONE, new CastExpression(BytecodeLoc.NONE, new InferredJavaType(RawJavaType.INT, InferredJavaType.Source.TEST), wcm.getStaticFunction("func", (JavaTypeInstance)TypeConstants.OBJECTMETHODS, (JavaTypeInstance)TypeConstants.OBJECT, "bootstrap", new Literal(TypedLiteral.getString(QuotingUtils.enquoteString("hashCode"))), wcm.getExpressionWildCard("array"), wcm.getExpressionWildCard("this"))), RawJavaType.INT);
        RecordRewriter.hideIfMatch(thisType, fields, method, wcm, stm);
    }

    private static void hideIfMatch(JavaTypeInstance thisType, List<ClassFileField> fields, Method method, WildcardMatch wcm, StructuredStatement stm) {
        StructuredStatement item = RecordRewriter.getSingleCodeLine(method);
        if (!stm.equals(item)) {
            return;
        }
        if (!RecordRewriter.cmpArgsEq(wcm.getExpressionWildCard("array").getMatch(), thisType, fields)) {
            return;
        }
        if (!MiscUtils.isThis(wcm.getExpressionWildCard("this").getMatch(), thisType)) {
            return;
        }
        method.hideDead();
    }

    private static boolean stringArgEq(Expression expression, String name) {
        Literal l = expression.getComputedLiteral(null);
        if (l == null) {
            return false;
        }
        TypedLiteral tl = l.getValue();
        if (tl.getType() != TypedLiteral.LiteralType.String) {
            return false;
        }
        String val = tl.toString();
        return val.equals(QuotingUtils.enquoteString(name));
    }

    private static boolean methodHandleEq(Expression expression, String name) {
        Literal l = expression.getComputedLiteral(null);
        if (l == null) {
            return false;
        }
        TypedLiteral tl = l.getValue();
        if (tl.getType() != TypedLiteral.LiteralType.MethodHandle) {
            return false;
        }
        ConstantPoolEntryMethodHandle handle = tl.getMethodHandle();
        if (!handle.isFieldRef()) {
            return false;
        }
        String fName = handle.getFieldRef().getLocalName();
        return name.equals(fName);
    }

    private static boolean classArgEq(Expression expression, JavaTypeInstance thisType) {
        Literal l = expression.getComputedLiteral(null);
        if (l == null) {
            return false;
        }
        TypedLiteral tl = l.getValue();
        if (tl.getType() != TypedLiteral.LiteralType.Class) {
            return false;
        }
        return thisType.equals(tl.getClassValue());
    }

    private static boolean cmpArgsEq(Expression cmpArgs, JavaTypeInstance thisType, List<ClassFileField> instances) {
        if (!(cmpArgs instanceof NewAnonymousArray)) {
            return false;
        }
        List<Expression> cmpValues = ((NewAnonymousArray)cmpArgs).getValues();
        if (cmpValues.size() != instances.size() + 2) {
            return false;
        }
        if (!RecordRewriter.classArgEq(cmpValues.get(0), thisType)) {
            return false;
        }
        StringBuilder semi = new StringBuilder();
        int idx = 2;
        for (ClassFileField field : instances) {
            String name;
            Expression arg;
            if (idx != 2) {
                semi.append(";");
            }
            if (!RecordRewriter.methodHandleEq(arg = cmpValues.get(idx++), name = field.getFieldName())) {
                return false;
            }
            semi.append(name);
        }
        return RecordRewriter.stringArgEq(cmpValues.get(1), semi.toString());
    }

    private static Method getMethod(ClassFile classFile, final List<JavaTypeInstance> args, String name) {
        List<Method> methods = classFile.getMethodsByNameOrNull(name);
        if (methods == null) {
            return null;
        }
        return (methods = Functional.filter(methods, new Predicate<Method>(){

            @Override
            public boolean test(Method in) {
                if (!in.testAccessFlag(AccessFlagMethod.ACC_PUBLIC)) {
                    return false;
                }
                return in.getMethodPrototype().getArgs().equals(args);
            }
        })).size() == 1 ? methods.get(0) : null;
    }

    private static StructuredStatement getSingleCodeLine(Method method) {
        if (method == null) {
            return null;
        }
        if (method.getCodeAttribute() == null) {
            return null;
        }
        Op04StructuredStatement code = method.getAnalysis();
        StructuredStatement topCode = code.getStatement();
        if (!(topCode instanceof Block)) {
            return null;
        }
        Block block = (Block)topCode;
        Optional<Op04StructuredStatement> content = block.getMaybeJustOneStatement();
        if (!content.isSet()) {
            return null;
        }
        return content.getValue().getStatement();
    }

    private static void hideDefaultGetter(Method method, ClassFileField classFileField, JavaRefTypeInstance thisType) {
        StructuredStatement item = RecordRewriter.getSingleCodeLine(method);
        if (item == null) {
            return;
        }
        WildcardMatch wcm = new WildcardMatch();
        StructuredReturn s = new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("var")), classFileField.getField().getJavaTypeInstance());
        if (!((Object)s).equals(item)) {
            return;
        }
        ClassFileField cff = RecordRewriter.getCFF(wcm.getLValueWildCard("var").getMatch(), thisType);
        if (cff != classFileField) {
            return;
        }
        classFileField.markHidden();
        method.hideDead();
    }

    private static void hideConstructorIfEmpty(Method canonicalCons) {
        if (canonicalCons.getCodeAttribute() == null) {
            return;
        }
        Op04StructuredStatement code = canonicalCons.getAnalysis();
        if (code.getStatement().isEffectivelyNOP()) {
            canonicalCons.hideDead();
        }
    }

    private static boolean removeImplicitAssignments(Method canonicalCons, List<ClassFileField> instances, JavaRefTypeInstance thisType) {
        if (canonicalCons.getCodeAttribute() == null) {
            return false;
        }
        Op04StructuredStatement code = canonicalCons.getAnalysis();
        instances = ListFactory.newList(instances);
        List<LocalVariable> args = canonicalCons.getMethodPrototype().getComputedParameters();
        StructuredStatement topCode = code.getStatement();
        if (!(topCode instanceof Block)) {
            return false;
        }
        Block block = (Block)topCode;
        List<Op04StructuredStatement> statements = block.getBlockStatements();
        List<Op04StructuredStatement> toNop = ListFactory.newList();
        int nopFrom = statements.size();
        for (int x = statements.size() - 1; x >= 0; --x) {
            LocalVariable expected;
            LValue rlv;
            int idx;
            LValue lhs;
            ClassFileField field;
            Op04StructuredStatement stm = statements.get(x);
            StructuredStatement statement = stm.getStatement();
            if (statement.isEffectivelyNOP()) continue;
            if (!(statement instanceof StructuredAssignment) || (field = RecordRewriter.getCFF(lhs = ((StructuredAssignment)statement).getLvalue(), thisType)) == null || (idx = instances.indexOf(field)) == -1) break;
            instances.set(idx, null);
            Expression rhs = ((StructuredAssignment)statement).getRvalue();
            if (!(rhs instanceof LValueExpression) || (rlv = ((LValueExpression)rhs).getLValue()) != (expected = args.get(idx))) break;
            toNop.add(stm);
            nopFrom = x;
        }
        ThisCheck thisCheck = new ThisCheck(thisType);
        ExpressionRewriterTransformer check = new ExpressionRewriterTransformer(thisCheck);
        for (int x = 0; x < nopFrom && !thisCheck.failed; ++x) {
            check.transform(statements.get(x));
        }
        if (thisCheck.failed) {
            return false;
        }
        for (Op04StructuredStatement nop : toNop) {
            nop.nopOut();
        }
        return true;
    }

    private static ClassFileField getCFF(LValue lhs, JavaRefTypeInstance thisType) {
        if (!(lhs instanceof FieldVariable)) {
            return null;
        }
        Expression obj = ((FieldVariable)lhs).getObject();
        if (!MiscUtils.isThis(obj, (JavaTypeInstance)thisType)) {
            return null;
        }
        return ((FieldVariable)lhs).getClassFileField();
    }

    static class IsCanonicalConstructor
    implements Predicate<Method> {
        private final List<ClassFileField> fields;

        IsCanonicalConstructor(List<ClassFileField> fields) {
            this.fields = fields;
        }

        @Override
        public boolean test(Method in) {
            MethodPrototype proto = in.getMethodPrototype();
            if (!proto.parametersComputed()) {
                return false;
            }
            List<JavaTypeInstance> protoArgs = proto.getArgs();
            if (protoArgs.size() != this.fields.size()) {
                return false;
            }
            List<LocalVariable> parameters = proto.getComputedParameters();
            if (parameters.size() != this.fields.size()) {
                return false;
            }
            for (int x = 0; x < this.fields.size(); ++x) {
                JavaTypeInstance paramType;
                JavaTypeInstance fieldType = this.fields.get(x).getField().getJavaTypeInstance();
                if (fieldType.equals(paramType = protoArgs.get(x))) continue;
                return false;
            }
            return true;
        }
    }

    static class ThisCheck
    extends AbstractExpressionRewriter {
        private final JavaTypeInstance thisType;
        private boolean failed;

        ThisCheck(JavaTypeInstance thisType) {
            this.thisType = thisType;
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (this.failed) {
                return lValue;
            }
            if (MiscUtils.isThis(lValue, this.thisType)) {
                this.failed = true;
            }
            return super.rewriteExpression(lValue, ssaIdentifiers, statementContainer, flags);
        }
    }
}

