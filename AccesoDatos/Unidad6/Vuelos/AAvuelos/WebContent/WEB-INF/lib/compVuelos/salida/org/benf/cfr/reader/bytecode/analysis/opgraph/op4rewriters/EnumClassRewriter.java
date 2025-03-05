/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.EnumAllSuperRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.EnumSuperRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.AbstractMatchResultIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.CollectMatch;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.KleeneStar;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchOneOf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.ResetAfterTest;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractConstructorInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractNewArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewObjectArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.QuotingUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Field;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperEnum;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.Optional;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.IllegalIdentifierReplacement;

public class EnumClassRewriter {
    private final ClassFile classFile;
    private final JavaTypeInstance classType;
    private final DCCommonState state;
    private final InferredJavaType clazzIJT;
    private final Options options;

    public static void rewriteEnumClass(ClassFile classFile, DCCommonState state) {
        ClassFileVersion classFileVersion;
        Options options = state.getOptions();
        if (!options.getOption(OptionsImpl.ENUM_SUGAR, classFileVersion = classFile.getClassFileVersion()).booleanValue()) {
            return;
        }
        JavaTypeInstance classType = classFile.getClassType();
        if (!classFile.getBindingSupers().containsBase(TypeConstants.ENUM)) {
            return;
        }
        EnumClassRewriter c = new EnumClassRewriter(classFile, classType, state);
        if (!c.rewrite()) {
            c.removeAllRemainingSupers();
        }
    }

    private EnumClassRewriter(ClassFile classFile, JavaTypeInstance classType, DCCommonState state) {
        this.classFile = classFile;
        this.classType = classType;
        this.state = state;
        this.options = state.getOptions();
        this.clazzIJT = new InferredJavaType(classType, InferredJavaType.Source.UNKNOWN, true);
    }

    private void removeAllRemainingSupers() {
        List<Method> constructors = this.classFile.getConstructors();
        EnumAllSuperRewriter enumSuperRewriter = new EnumAllSuperRewriter();
        for (Method constructor : constructors) {
            enumSuperRewriter.rewrite(constructor.getAnalysis());
        }
    }

    private boolean rewrite() {
        Method values;
        Method valueOf;
        Method staticInit;
        try {
            staticInit = this.classFile.getMethodByName("<clinit>").get(0);
        }
        catch (NoSuchMethodException e) {
            return false;
        }
        Op04StructuredStatement staticInitCode = staticInit.getAnalysis();
        if (!staticInitCode.isFullyStructured()) {
            return false;
        }
        EnumInitMatchCollector initMatchCollector = this.analyseStaticMethod(staticInitCode);
        if (initMatchCollector == null) {
            return false;
        }
        try {
            valueOf = this.classFile.getMethodByName("valueOf").get(0);
            values = this.classFile.getMethodByName("values").get(0);
        }
        catch (NoSuchMethodException e) {
            return false;
        }
        valueOf.hideSynthetic();
        values.hideSynthetic();
        for (ClassFileField field : initMatchCollector.getMatchedHideTheseFields()) {
            field.markHidden();
        }
        Map entryMap = initMatchCollector.getEntryMap();
        CollectedEnumData matchedArray = initMatchCollector.getMatchedArray();
        for (CollectedEnumData entry : entryMap.values()) {
            entry.getContainer().nopOut();
        }
        matchedArray.getContainer().nopOut();
        Method matchedArrayMethod = matchedArray.getMethodContainer();
        if (matchedArrayMethod != null) {
            matchedArrayMethod.hideSynthetic();
        }
        List<Method> constructors = this.classFile.getConstructors();
        EnumSuperRewriter enumSuperRewriter = new EnumSuperRewriter();
        for (Method method : constructors) {
            enumSuperRewriter.rewrite(method.getAnalysis());
        }
        List<Pair<StaticVariable, AbstractConstructorInvokation>> entries = ListFactory.newList();
        for (Map.Entry entry : entryMap.entrySet()) {
            entries.add(Pair.make(entry.getKey(), ((CollectedEnumData)entry.getValue()).getData()));
        }
        this.classFile.setDumpHelper(new ClassFileDumperEnum(this.state, entries));
        boolean bl = (Boolean)this.options.getOption(OptionsImpl.RENAME_ENUM_MEMBERS);
        if (bl) {
            Set<String> set = SetFactory.newSet(Functional.map(this.classFile.getFields(), new UnaryFunction<ClassFileField, String>(){

                @Override
                public String invoke(ClassFileField arg) {
                    return arg.getFieldName();
                }
            }));
            List<Pair<StaticVariable, AbstractConstructorInvokation>> renames = ListFactory.newList();
            for (Pair<StaticVariable, AbstractConstructorInvokation> entry : entries) {
                Object expectedValue;
                StaticVariable sv = entry.getFirst();
                AbstractConstructorInvokation aci = entry.getSecond();
                String name = sv.getFieldName();
                Expression expectedNameExp = aci.getArgs().get(0);
                String expectedName = name;
                if (expectedNameExp.getInferredJavaType().getJavaTypeInstance() == TypeConstants.STRING && expectedNameExp instanceof Literal && (expectedValue = ((Literal)expectedNameExp).getValue().getValue()) instanceof String) {
                    expectedName = QuotingUtils.unquoteString((String)expectedValue);
                }
                if (name.equals(expectedName) || IllegalIdentifierReplacement.isIllegal(expectedName)) continue;
                renames.add(Pair.make(sv, expectedName));
            }
            for (Pair<StaticVariable, AbstractConstructorInvokation> rename : renames) {
                String newName = (String)((Object)rename.getSecond());
                StaticVariable sv = rename.getFirst();
                if (set.contains(newName)) {
                    this.classFile.addComment("Tried to rename field '" + sv.getFieldName() + "' to '" + newName + "' but it's alread used.");
                    continue;
                }
                set.remove(sv.getFieldName());
                set.add(newName);
                sv.getClassFileField().overrideName(newName);
            }
        }
        return true;
    }

    private EnumInitMatchCollector analyseStaticMethod(Op04StructuredStatement statement) {
        List<StructuredStatement> statements = ListFactory.newList();
        statement.linearizeStatementsInto(statements);
        statements = Functional.filter(statements, new Predicate<StructuredStatement>(){

            @Override
            public boolean test(StructuredStatement in) {
                return !(in instanceof StructuredComment);
            }
        });
        WildcardMatch wcm = new WildcardMatch();
        InferredJavaType clazzIJT = new InferredJavaType(this.classType, InferredJavaType.Source.UNKNOWN, true);
        JavaArrayTypeInstance arrayType = new JavaArrayTypeInstance(1, this.classType);
        InferredJavaType clazzAIJT = new InferredJavaType(arrayType, InferredJavaType.Source.UNKNOWN, true);
        MatchSequence matcher = new MatchSequence(new BeginBlock(null), new KleeneStar(new MatchOneOf(new ResetAfterTest(wcm, new CollectMatch("entry", new StructuredAssignment(BytecodeLoc.NONE, wcm.getStaticVariable("e", this.classType, clazzIJT), wcm.getConstructorSimpleWildcard("c", this.classType)))), new ResetAfterTest(wcm, new CollectMatch("entryderived", new StructuredAssignment(BytecodeLoc.NONE, wcm.getStaticVariable("e2", this.classType, clazzIJT, false), wcm.getConstructorAnonymousWildcard("c2", null)))))), new MatchOneOf(new ResetAfterTest(wcm, new CollectMatch("values", new StructuredAssignment(BytecodeLoc.NONE, wcm.getStaticVariable("v", this.classType, clazzAIJT), wcm.getNewArrayWildCard("v", 0, 1)))), new ResetAfterTest(wcm, new CollectMatch("values15", new StructuredAssignment(BytecodeLoc.NONE, wcm.getStaticVariable("v", this.classType, clazzAIJT), wcm.getStaticFunction("v", this.classType, new JavaArrayTypeInstance(1, this.classType), "$values")))), new ResetAfterTest(wcm, new CollectMatch("noValues", new StructuredAssignment(BytecodeLoc.NONE, wcm.getStaticVariable("v", this.classType, clazzAIJT), new NewObjectArray(BytecodeLoc.NONE, Collections.singletonList(Literal.INT_ZERO), arrayType))))));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(statements);
        EnumInitMatchCollector matchCollector = new EnumInitMatchCollector(wcm);
        mi.advance();
        if (!matcher.match(mi, (MatchResultCollector)matchCollector)) {
            return null;
        }
        if (!matchCollector.isValid()) {
            return null;
        }
        return matchCollector;
    }

    private CollectedEnumData<NewAnonymousArray> getJava15Values(Op04StructuredStatement container, MethodPrototype methodPrototype) {
        Method method = null;
        try {
            method = this.classFile.getMethodByPrototype(methodPrototype);
        }
        catch (NoSuchMethodException e) {
            return null;
        }
        Op04StructuredStatement stm = method.getAnalysis();
        if (!(stm.getStatement() instanceof Block)) {
            return null;
        }
        Block blk = (Block)stm.getStatement();
        Optional<Op04StructuredStatement> inner = blk.getMaybeJustOneStatement();
        if (!inner.isSet()) {
            return null;
        }
        StructuredStatement testArr = inner.getValue().getStatement();
        if (!(testArr instanceof StructuredReturn)) {
            return null;
        }
        Expression rv = ((StructuredReturn)testArr).getValue();
        if (!(rv instanceof NewAnonymousArray)) {
            return null;
        }
        return new CollectedEnumData<NewAnonymousArray>(container, method, (NewAnonymousArray)rv);
    }

    private class EnumInitMatchCollector
    extends AbstractMatchResultIterator {
        private final WildcardMatch wcm;
        private final Map<StaticVariable, CollectedEnumData<? extends AbstractConstructorInvokation>> entryMap = MapFactory.newOrderedMap();
        private CollectedEnumData<NewAnonymousArray> matchedArray;
        private List<ClassFileField> matchedHideTheseFields = ListFactory.newList();

        private EnumInitMatchCollector(WildcardMatch wcm) {
            this.wcm = wcm;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            if (name.equals("entry")) {
                StaticVariable staticVariable = this.wcm.getStaticVariable("e").getMatch();
                ConstructorInvokationSimple constructorInvokation = this.wcm.getConstructorSimpleWildcard("c").getMatch();
                this.entryMap.put(staticVariable, new CollectedEnumData(statement.getContainer(), null, constructorInvokation));
                return;
            }
            if (name.equals("entryderived")) {
                StaticVariable staticVariable = this.wcm.getStaticVariable("e2").getMatch();
                ConstructorInvokationAnonymousInner constructorInvokation = this.wcm.getConstructorAnonymousWildcard("c2").getMatch();
                this.entryMap.put(staticVariable, new CollectedEnumData(statement.getContainer(), null, constructorInvokation));
                return;
            }
            if (name.equals("values")) {
                AbstractNewArray abstractNewArray = this.wcm.getNewArrayWildCard("v").getMatch();
                if (abstractNewArray instanceof NewAnonymousArray) {
                    this.matchedArray = new CollectedEnumData(statement.getContainer(), null, (NewAnonymousArray)abstractNewArray);
                }
            } else if (name.equals("noValues")) {
                this.matchedArray = new CollectedEnumData(statement.getContainer(), null, new NewAnonymousArray(BytecodeLoc.TODO, new InferredJavaType(EnumClassRewriter.this.classType, InferredJavaType.Source.TEST), 1, Collections.<Expression>emptyList(), true));
            } else if (name.equals("values15")) {
                StaticFunctionInvokation sf = this.wcm.getStaticFunction("v").getMatch();
                this.matchedArray = EnumClassRewriter.this.getJava15Values(statement.getContainer(), sf.getMethodPrototype());
            }
        }

        boolean isValid() {
            List<ClassFileField> fields = EnumClassRewriter.this.classFile.getFields();
            for (ClassFileField classFileField : fields) {
                StaticVariable tmp;
                Field field = classFileField.getField();
                JavaTypeInstance fieldType = field.getJavaTypeInstance();
                boolean isStatic = field.testAccessFlag(AccessFlag.ACC_STATIC);
                boolean isEnum = field.testAccessFlag(AccessFlag.ACC_ENUM);
                boolean expected = isStatic && isEnum && fieldType.equals(EnumClassRewriter.this.classType);
                if (expected != this.entryMap.containsKey(tmp = new StaticVariable(EnumClassRewriter.this.clazzIJT, EnumClassRewriter.this.classType, field.getFieldName()))) {
                    return false;
                }
                if (!expected) continue;
                this.matchedHideTheseFields.add(classFileField);
            }
            if (this.matchedArray == null) {
                return false;
            }
            List<Expression> values = ((NewAnonymousArray)((CollectedEnumData)this.matchedArray).getData()).getValues();
            if (values.size() != this.entryMap.size()) {
                return false;
            }
            for (Expression value : values) {
                if (!(value instanceof LValueExpression)) {
                    return false;
                }
                LValueExpression lValueExpression = (LValueExpression)value;
                LValue lvalue = lValueExpression.getLValue();
                if (!(lvalue instanceof StaticVariable)) {
                    return false;
                }
                StaticVariable staticVariable = (StaticVariable)lvalue;
                if (this.entryMap.containsKey(staticVariable)) continue;
                return false;
            }
            LValue lValue = ((StructuredAssignment)((CollectedEnumData)this.matchedArray).getContainer().getStatement()).getLvalue();
            if (!(lValue instanceof StaticVariable)) {
                return false;
            }
            StaticVariable valuesArrayStatic = (StaticVariable)lValue;
            try {
                ClassFileField valuesField = EnumClassRewriter.this.classFile.getFieldByName(valuesArrayStatic.getFieldName(), valuesArrayStatic.getInferredJavaType().getJavaTypeInstance());
                if (!valuesField.getField().testAccessFlag(AccessFlag.ACC_STATIC)) {
                    return false;
                }
                this.matchedHideTheseFields.add(valuesField);
            }
            catch (NoSuchFieldException e) {
                return false;
            }
            return true;
        }

        private List<ClassFileField> getMatchedHideTheseFields() {
            return this.matchedHideTheseFields;
        }

        private Map<StaticVariable, CollectedEnumData<? extends AbstractConstructorInvokation>> getEntryMap() {
            return this.entryMap;
        }

        private CollectedEnumData<NewAnonymousArray> getMatchedArray() {
            return this.matchedArray;
        }
    }

    private static class CollectedEnumData<T> {
        private final Op04StructuredStatement container;
        private final Method methodContainer;
        private final T data;

        private CollectedEnumData(Op04StructuredStatement container, Method methodContainer, T data) {
            this.container = container;
            this.methodContainer = methodContainer;
            this.data = data;
        }

        private Op04StructuredStatement getContainer() {
            return this.container;
        }

        private T getData() {
            return this.data;
        }

        public Method getMethodContainer() {
            return this.methodContainer;
        }
    }
}

