/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.AssertRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.EnumClassRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.FakeMethodRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.IllegalGenericRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.J14ClassObjectRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LocalInlinedStringConstantRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.NonStaticLifter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.RecordRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.RetroLambdaRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ScopeHidingVariableRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SealedClassChecker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.StaticLifter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.UnreachableStaticRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.DeadMethodRemover;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.ConstructorUtils;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.AbstractFieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.AbstractLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.FormalTypeParameter;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.classfilehelpers.ConstantLinks;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.ClassCache;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollectingDumper;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class CodeAnalyserWholeClass {
    public static void wholeClassAnalysisPass1(ClassFile classFile, DCCommonState state) {
        Options options = state.getOptions();
        EnumClassRewriter.rewriteEnumClass(classFile, state);
        if (((Boolean)options.getOption(OptionsImpl.REMOVE_BAD_GENERICS)).booleanValue()) {
            CodeAnalyserWholeClass.removeIllegalGenerics(classFile, options);
        }
        if (((Boolean)options.getOption(OptionsImpl.SUGAR_ASSERTS)).booleanValue()) {
            CodeAnalyserWholeClass.resugarAsserts(classFile, options);
        }
        CodeAnalyserWholeClass.tidyAnonymousConstructors(classFile);
        if (((Boolean)options.getOption(OptionsImpl.LIFT_CONSTRUCTOR_INIT)).booleanValue()) {
            CodeAnalyserWholeClass.liftStaticInitialisers(classFile);
            CodeAnalyserWholeClass.liftNonStaticInitialisers(classFile);
        }
        if (options.getOption(OptionsImpl.JAVA_4_CLASS_OBJECTS, classFile.getClassFileVersion()).booleanValue()) {
            CodeAnalyserWholeClass.resugarJava14classObjects(classFile, state);
        }
        if (((Boolean)options.getOption(OptionsImpl.REMOVE_BOILERPLATE)).booleanValue()) {
            CodeAnalyserWholeClass.removeBoilerplateMethods(classFile);
        }
        if (((Boolean)options.getOption(OptionsImpl.REMOVE_INNER_CLASS_SYNTHETICS)).booleanValue()) {
            if (classFile.isInnerClass()) {
                CodeAnalyserWholeClass.removeInnerClassOuterThis(classFile);
            }
            CodeAnalyserWholeClass.removeInnerClassSyntheticConstructorFriends(classFile);
        }
        if (options.getOption(OptionsImpl.RECORD_TYPES, classFile.getClassFileVersion()).booleanValue()) {
            CodeAnalyserWholeClass.resugarRecords(classFile, state);
        }
        if (((Boolean)options.getOption(OptionsImpl.SUGAR_RETRO_LAMBDA)).booleanValue()) {
            CodeAnalyserWholeClass.resugarRetroLambda(classFile, state);
        }
        if (options.getOption(OptionsImpl.SEALED, classFile.getClassFileVersion()).booleanValue()) {
            CodeAnalyserWholeClass.checkNonSealed(classFile, state);
        }
    }

    private static void resugarRecords(ClassFile classFile, DCCommonState state) {
        RecordRewriter.rewrite(classFile, state);
    }

    private static void resugarRetroLambda(ClassFile classFile, DCCommonState state) {
        RetroLambdaRewriter.rewrite(classFile, state);
    }

    private static void checkNonSealed(ClassFile classFile, DCCommonState state) {
        SealedClassChecker.rewrite(classFile, state);
    }

    private static void removeRedundantSupers(ClassFile classFile) {
        for (Method method : classFile.getConstructors()) {
            if (!method.hasCodeAttribute()) continue;
            Op04StructuredStatement code = method.getAnalysis();
            Op04StructuredStatement.removeConstructorBoilerplate(code);
        }
    }

    private static void replaceNestedSyntheticOuterRefs(ClassFile classFile) {
        for (Method method : classFile.getMethods()) {
            if (!method.hasCodeAttribute()) continue;
            Op04StructuredStatement code = method.getAnalysis();
            Op04StructuredStatement.replaceNestedSyntheticOuterRefs(code);
        }
    }

    private static void inlineAccessors(DCCommonState state, ClassFile classFile) {
        for (Method method : classFile.getMethods()) {
            if (!method.hasCodeAttribute()) continue;
            Op04StructuredStatement code = method.getAnalysis();
            Op04StructuredStatement.inlineSyntheticAccessors(state, method, code);
        }
    }

    private static void renameAnonymousScopeHidingVariables(ClassFile classFile, ClassCache classCache) {
        List<ClassFileField> fields = Functional.filter(classFile.getFields(), new Predicate<ClassFileField>(){

            @Override
            public boolean test(ClassFileField in) {
                return in.isSyntheticOuterRef();
            }
        });
        if (fields.isEmpty()) {
            return;
        }
        for (Method method : classFile.getMethods()) {
            if (!method.hasCodeAttribute()) continue;
            ScopeHidingVariableRewriter rewriter = new ScopeHidingVariableRewriter(fields, method, classCache);
            rewriter.rewrite(method.getAnalysis());
        }
    }

    private static void fixInnerClassConstructorSyntheticOuterArgs(ClassFile classFile) {
        if (classFile.isInnerClass()) {
            Set<MethodPrototype> processed = SetFactory.newSet();
            for (Method method : classFile.getConstructors()) {
                Op04StructuredStatement.fixInnerClassConstructorSyntheticOuterArgs(classFile, method, method.getAnalysis(), processed);
            }
        }
    }

    private static void tidyAnonymousConstructors(ClassFile classFile) {
        for (Method method : classFile.getMethods()) {
            if (!method.hasCodeAttribute()) continue;
            Op04StructuredStatement code = method.getAnalysis();
            Op04StructuredStatement.tidyAnonymousConstructors(code);
        }
    }

    private static void removeInnerClassSyntheticConstructorFriends(ClassFile classFile) {
        for (Method method : classFile.getConstructors()) {
            InnerClassInfo innerClassInfo;
            MethodPrototype prototype;
            List<JavaTypeInstance> argsThis;
            MethodPrototype chainPrototype;
            Set<AccessFlagMethod> flags = method.getAccessFlags();
            if (!flags.contains((Object)AccessFlagMethod.ACC_SYNTHETIC) || flags.contains((Object)AccessFlagMethod.ACC_PUBLIC) || (chainPrototype = ConstructorUtils.getDelegatingPrototype(method)) == null || (argsThis = (prototype = method.getMethodPrototype()).getArgs()).isEmpty()) continue;
            List<JavaTypeInstance> argsThat = chainPrototype.getArgs();
            if (argsThis.size() != argsThat.size() + 1) continue;
            JavaTypeInstance last = argsThis.get(argsThis.size() - 1);
            UnaryFunction<JavaTypeInstance, JavaTypeInstance> degenerifier = new UnaryFunction<JavaTypeInstance, JavaTypeInstance>(){

                @Override
                public JavaTypeInstance invoke(JavaTypeInstance arg) {
                    return arg.getDeGenerifiedType();
                }
            };
            argsThis = Functional.map(argsThis, degenerifier);
            argsThat = Functional.map(argsThat, degenerifier);
            argsThis.remove(argsThis.size() - 1);
            if (!argsThis.equals(argsThat) || !(innerClassInfo = last.getInnerClassHereInfo()).isInnerClass()) continue;
            if (classFile.getClassType() != last) {
                innerClassInfo.hideSyntheticFriendClass();
            }
            prototype.hide(argsThis.size());
            method.hideSynthetic();
        }
    }

    private static void removeInnerClassOuterThis(ClassFile classFile) {
        if (classFile.testAccessFlag(AccessFlag.ACC_STATIC)) {
            return;
        }
        AbstractLValue foundOuterThis = null;
        ClassFileField classFileField = null;
        for (Method method : classFile.getConstructors()) {
            if (ConstructorUtils.isDelegating(method)) continue;
            FieldVariable outerThis = Op04StructuredStatement.findInnerClassOuterThis(method, method.getAnalysis());
            if (outerThis == null) {
                return;
            }
            if (foundOuterThis == null) {
                foundOuterThis = outerThis;
                classFileField = ((AbstractFieldVariable)foundOuterThis).getClassFileField();
                continue;
            }
            if (classFileField == outerThis.getClassFileField()) continue;
            return;
        }
        if (foundOuterThis == null) {
            return;
        }
        JavaTypeInstance fieldType = foundOuterThis.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance classType = classFile.getClassType();
        if (!classType.getInnerClassHereInfo().isTransitiveInnerClassOf(fieldType)) {
            classFile.getAccessFlags().add(AccessFlag.ACC_STATIC);
            return;
        }
        classFileField.markHidden();
        classFileField.markSyntheticOuterRef();
        for (Method method : classFile.getConstructors()) {
            if (ConstructorUtils.isDelegating(method)) {
                MethodPrototype prototype = method.getMethodPrototype();
                prototype.setInnerOuterThis();
                prototype.hide(0);
            }
            Op04StructuredStatement.removeInnerClassOuterThis(method, method.getAnalysis());
        }
        String originalName = ((AbstractFieldVariable)foundOuterThis).getFieldName();
        if (!(fieldType instanceof JavaRefTypeInstance)) {
            return;
        }
        JavaRefTypeInstance fieldRefType = (JavaRefTypeInstance)fieldType.getDeGenerifiedType();
        String name = fieldRefType.getRawShortName();
        String explicitName = name + ".this";
        if (fieldRefType.getInnerClassHereInfo().isMethodScopedClass()) {
            explicitName = "this";
        }
        classFileField.overrideName(explicitName);
        classFileField.markSyntheticOuterRef();
        try {
            ClassFileField localClassFileField = classFile.getFieldByName(originalName, fieldType);
            localClassFileField.overrideName(explicitName);
            localClassFileField.markSyntheticOuterRef();
        }
        catch (NoSuchFieldException noSuchFieldException) {
            // empty catch block
        }
        classFile.getClassType().getInnerClassHereInfo().setHideSyntheticThis();
    }

    private static Method getStaticConstructor(ClassFile classFile) {
        Method staticInit;
        try {
            staticInit = classFile.getMethodByName("<clinit>").get(0);
        }
        catch (NoSuchMethodException e) {
            return null;
        }
        return staticInit;
    }

    private static void liftStaticInitialisers(ClassFile classFile) {
        Method staticInit = CodeAnalyserWholeClass.getStaticConstructor(classFile);
        if (staticInit == null) {
            return;
        }
        new StaticLifter(classFile).liftStatics(staticInit);
    }

    private static void liftNonStaticInitialisers(ClassFile classFile) {
        new NonStaticLifter(classFile).liftNonStatics();
    }

    private static void removeDeadMethods(ClassFile classFile) {
        Method staticInit = CodeAnalyserWholeClass.getStaticConstructor(classFile);
        if (staticInit != null) {
            DeadMethodRemover.removeDeadMethod(classFile, staticInit);
        }
        CodeAnalyserWholeClass.tryRemoveConstructor(classFile);
    }

    private static void removeBoilerplateMethods(ClassFile classFile) {
        String[] removeThese;
        for (String methName : removeThese = new String[]{"$deserializeLambda$"}) {
            List<Method> methods = classFile.getMethodsByNameOrNull(methName);
            if (methods == null) continue;
            for (Method method : methods) {
                method.hideSynthetic();
            }
        }
    }

    private static void relinkConstantStrings(ClassFile classFile, DCCommonState state) {
        Map<String, Expression> rewrites = ConstantLinks.getLocalStringConstants(classFile, state);
        if (rewrites == null || rewrites.isEmpty()) {
            return;
        }
        LocalInlinedStringConstantRewriter rewriter = new LocalInlinedStringConstantRewriter(rewrites);
        for (Method m : classFile.getMethods()) {
            Op04StructuredStatement code;
            if (!m.hasCodeAttribute() || !(code = m.getAnalysis()).isFullyStructured()) continue;
            rewriter.rewrite(code);
        }
    }

    private static void tryRemoveConstructor(ClassFile classFile) {
        List<Method> constructors = Functional.filter(classFile.getConstructors(), new Predicate<Method>(){

            @Override
            public boolean test(Method in) {
                return in.hiddenState() == Method.Visibility.Visible;
            }
        });
        if (constructors.size() != 1) {
            return;
        }
        Method constructor = constructors.get(0);
        MethodPrototype methodPrototype = constructor.getMethodPrototype();
        if (methodPrototype.getVisibleArgCount() > 0) {
            return;
        }
        if (constructor.testAccessFlag(AccessFlagMethod.ACC_FINAL)) {
            return;
        }
        if (!constructor.getConstructorFlag().isEnumConstructor() && !constructor.testAccessFlag(AccessFlagMethod.ACC_PUBLIC)) {
            return;
        }
        if (!MiscStatementTools.isDeadCode(constructor.getAnalysis())) {
            return;
        }
        if (constructor.hasDumpableAttributes()) {
            return;
        }
        constructor.hideDead();
    }

    private static void removeIllegalGenerics(ClassFile classFile, Options state) {
        ConstantPool cp = classFile.getConstantPool();
        JavaRefTypeInstance classType = classFile.getRefClassType();
        Map<String, FormalTypeParameter> params = FormalTypeParameter.getMap(classFile.getClassSignature().getFormalTypeParameters());
        for (Method m : classFile.getMethods()) {
            List<StructuredStatement> statements;
            Op04StructuredStatement code;
            if (!m.hasCodeAttribute() || !(code = m.getAnalysis()).isFullyStructured() || (statements = MiscStatementTools.linearise(code)) == null) continue;
            boolean bStatic = m.testAccessFlag(AccessFlagMethod.ACC_STATIC);
            Map<String, FormalTypeParameter> formalParams = MapFactory.newMap();
            if (!bStatic) {
                formalParams.putAll(params);
            }
            formalParams.putAll(m.getMethodPrototype().getFormalParameterMap());
            IllegalGenericRewriter r = new IllegalGenericRewriter(cp, formalParams);
            for (StructuredStatement statement : statements) {
                statement.rewriteExpressions(r);
            }
            Op04StructuredStatement.removePrimitiveDeconversion(state, m, code);
        }
    }

    private static void resugarAsserts(ClassFile classFile, Options options) {
        Method staticInit = CodeAnalyserWholeClass.getStaticConstructor(classFile);
        if (staticInit != null) {
            new AssertRewriter(classFile, options).sugarAsserts(staticInit);
        }
    }

    private static void resugarJava14classObjects(ClassFile classFile, DCCommonState state) {
        new J14ClassObjectRewriter(classFile, state).rewrite();
    }

    public static void wholeClassAnalysisPass3(ClassFile classFile, DCCommonState state, TypeUsageCollectingDumper typeUsage) {
        Options options = state.getOptions();
        if (((Boolean)options.getOption(OptionsImpl.REMOVE_BOILERPLATE)).booleanValue()) {
            CodeAnalyserWholeClass.removeRedundantSupers(classFile);
        }
        if (((Boolean)options.getOption(OptionsImpl.REMOVE_DEAD_METHODS)).booleanValue()) {
            CodeAnalyserWholeClass.removeDeadMethods(classFile);
        }
        CodeAnalyserWholeClass.rewriteUnreachableStatics(classFile, typeUsage);
        CodeAnalyserWholeClass.detectFakeMethods(classFile, typeUsage);
    }

    private static void detectFakeMethods(ClassFile classFile, TypeUsageCollectingDumper typeUsage) {
        FakeMethodRewriter.rewrite(classFile, typeUsage);
    }

    private static void rewriteUnreachableStatics(ClassFile classFile, TypeUsageCollectingDumper typeUsage) {
        UnreachableStaticRewriter.rewrite(classFile, typeUsage);
    }

    public static void wholeClassAnalysisPass2(ClassFile classFile, DCCommonState state) {
        Options options = state.getOptions();
        if (((Boolean)options.getOption(OptionsImpl.REMOVE_INNER_CLASS_SYNTHETICS)).booleanValue()) {
            if (classFile.isInnerClass()) {
                CodeAnalyserWholeClass.fixInnerClassConstructorSyntheticOuterArgs(classFile);
            }
            CodeAnalyserWholeClass.replaceNestedSyntheticOuterRefs(classFile);
            CodeAnalyserWholeClass.inlineAccessors(state, classFile);
            CodeAnalyserWholeClass.renameAnonymousScopeHidingVariables(classFile, state.getClassCache());
        }
        if (((Boolean)options.getOption(OptionsImpl.RELINK_CONSTANT_STRINGS)).booleanValue()) {
            CodeAnalyserWholeClass.relinkConstantStrings(classFile, state);
        }
    }
}

