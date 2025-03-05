/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.FormalTypeParameter;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaWildcardTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototypeAnnotationsHelper;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.analysis.variables.Ident;
import org.benf.cfr.reader.bytecode.analysis.variables.Slot;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamer;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.annotation.Nullable;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;

public class MethodPrototype
implements TypeUsageCollectable {
    private MethodPrototype descriptorProto;
    private final String originalDescriptor;
    private final List<FormalTypeParameter> formalTypeParameters;
    private final List<JavaTypeInstance> args;
    private final List<JavaTypeInstance> exceptionTypes;
    private final Set<Integer> hidden = SetFactory.newSet();
    private boolean innerOuterThis = false;
    private JavaTypeInstance result;
    private final VariableNamer variableNamer;
    private final boolean instanceMethod;
    private final boolean varargs;
    private final String name;
    @Nullable
    private String fixedName;
    private final ClassFile classFile;
    private final List<Slot> syntheticArgs = ListFactory.newList();
    private final List<Slot> syntheticCaptureArgs = ListFactory.newList();
    private List<ParameterLValue> parameterLValues = null;

    public MethodPrototype(DCCommonState state, ClassFile classFile, JavaTypeInstance classType, String name, boolean instanceMethod, Method.MethodConstructor constructorFlag, List<FormalTypeParameter> formalTypeParameters, List<JavaTypeInstance> args, JavaTypeInstance result, List<JavaTypeInstance> exceptionTypes, boolean varargs, VariableNamer variableNamer, boolean synthetic, String originalDescriptor) {
        this.formalTypeParameters = formalTypeParameters;
        this.instanceMethod = instanceMethod;
        this.originalDescriptor = originalDescriptor;
        if (!(constructorFlag != Method.MethodConstructor.ENUM_CONSTRUCTOR && constructorFlag != Method.MethodConstructor.ECLIPSE_ENUM_CONSTRUCTOR || synthetic)) {
            List args2 = ListFactory.newList();
            if (constructorFlag != Method.MethodConstructor.ECLIPSE_ENUM_CONSTRUCTOR) {
                args2.add(TypeConstants.STRING);
                args2.add(RawJavaType.INT);
            }
            args2.addAll(args);
            if (state == null || state.getOptions().getOption(OptionsImpl.ENUM_SUGAR, classFile.getClassFileVersion()).booleanValue()) {
                this.hide(0);
                this.hide(1);
            }
            args = args2;
        }
        this.args = args;
        JavaTypeInstance resultType = "<init>".equals(name) ? (classFile == null ? classType : null) : result;
        this.result = resultType;
        this.exceptionTypes = exceptionTypes;
        this.varargs = varargs;
        this.variableNamer = variableNamer;
        this.name = name;
        this.fixedName = null;
        this.classFile = classFile;
    }

    public OverloadMethodSet getOverloadMethodSet() {
        if (this.classFile == null) {
            return null;
        }
        return this.classFile.getOverloadMethodSet(this);
    }

    public void unbreakEnumConstructor() {
        this.args.remove(0);
        this.args.remove(0);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(this.result);
        collector.collect(this.args);
        collector.collectFrom(this.formalTypeParameters);
    }

    public void hide(int idx) {
        this.hidden.add(idx);
    }

    public Map<String, FormalTypeParameter> getFormalParameterMap() {
        return FormalTypeParameter.getMap(this.formalTypeParameters);
    }

    public void setDescriptorProto(MethodPrototype descriptorProto) {
        this.descriptorProto = descriptorProto;
    }

    public void setInnerOuterThis() {
        this.innerOuterThis = true;
    }

    public boolean isHiddenArg(int x) {
        return this.hidden.contains(x);
    }

    public boolean isInnerOuterThis() {
        return this.innerOuterThis;
    }

    public void dumpDeclarationSignature(Dumper d, String methName, Method.MethodConstructor isConstructor, MethodPrototypeAnnotationsHelper annotationsHelper, List<AnnotationTableTypeEntry> returnTypeAnnotations) {
        if (this.formalTypeParameters != null) {
            d.operator("<");
            boolean first = true;
            for (FormalTypeParameter formalTypeParameter : this.formalTypeParameters) {
                first = StringUtils.comma(first, d);
                d.dump(formalTypeParameter);
            }
            d.operator("> ");
        }
        if (isConstructor.isConstructor()) {
            MethodPrototypeAnnotationsHelper.dumpAnnotationTableEntries(returnTypeAnnotations, d);
        } else {
            if (!returnTypeAnnotations.isEmpty()) {
                JavaAnnotatedTypeInstance jah = this.result.getAnnotatedInstance();
                DecompilerComments comments = new DecompilerComments();
                TypeAnnotationHelper.apply(jah, returnTypeAnnotations, comments);
                d.dump(comments);
                jah.dump(d);
            } else {
                d.dump(this.result);
            }
            d.print(' ');
        }
        d.methodName(methName, this, isConstructor.isConstructor(), true);
        if (isConstructor == Method.MethodConstructor.RECORD_CANONICAL_CONSTRUCTOR && null == annotationsHelper.getTypeTargetAnnotations(TypeAnnotationEntryValue.type_formal)) {
            return;
        }
        d.separator("(");
        List<LocalVariable> parameterLValues = this.getComputedParameters();
        int argssize = this.args.size();
        boolean first = true;
        List<AnnotationTableTypeEntry> receiverAnnotations = null;
        if (this.isInstanceMethod() || isConstructor.isConstructor()) {
            receiverAnnotations = annotationsHelper.getTypeTargetAnnotations(TypeAnnotationEntryValue.type_receiver);
        }
        if (receiverAnnotations != null) {
            if (isConstructor.isConstructor()) {
                if (this.classFile.isInnerClass()) {
                    MethodPrototypeAnnotationsHelper.dumpAnnotationTableEntries(receiverAnnotations, d);
                    d.dump(this.args.get(0));
                    d.print(' ').dump(this.args.get(0)).print(".this");
                    first = StringUtils.comma(first, d);
                }
            } else {
                this.classFile.dumpReceiverClassIdentity(receiverAnnotations, d);
                d.print(' ').print("this");
                first = StringUtils.comma(first, d);
            }
        }
        int offset = 0;
        for (int i = 0; i < argssize && offset + i < parameterLValues.size(); ++i) {
            JavaTypeInstance arg = this.args.get(i);
            if (this.getParameterLValues().get((int)(i + offset)).hidden != HiddenReason.NotHidden) {
                ++offset;
                --i;
                continue;
            }
            if (this.hidden.contains(offset + i)) continue;
            first = StringUtils.comma(first, d);
            int paramIdx = i + offset;
            LocalVariable param = parameterLValues.get(paramIdx);
            if (param.isFinal()) {
                d.print("final ");
            }
            if (this.varargs && i == argssize - 1) {
                if (!(arg instanceof JavaArrayTypeInstance)) {
                    d.print(" /* corrupt varargs signature?! */ ");
                    annotationsHelper.dumpParamType(arg, paramIdx, d);
                    d.dump(arg);
                } else {
                    ((JavaArrayTypeInstance)arg).toVarargString(d);
                }
            } else {
                annotationsHelper.dumpParamType(arg, paramIdx, d);
            }
            d.print(" ");
            param.getName().dump(d, true);
        }
        d.separator(")");
    }

    public boolean parametersComputed() {
        return this.parameterLValues != null;
    }

    public List<ParameterLValue> getParameterLValues() {
        if (this.parameterLValues == null) {
            throw new IllegalStateException("Parameters not created");
        }
        return this.parameterLValues;
    }

    public List<LocalVariable> getComputedParameters() {
        return Functional.map(this.getParameterLValues(), new UnaryFunction<ParameterLValue, LocalVariable>(){

            @Override
            public LocalVariable invoke(ParameterLValue arg) {
                return arg.localVariable;
            }
        });
    }

    public void setNonMethodScopedSyntheticConstructorParameters(Method.MethodConstructor constructorFlag, DecompilerComments comments, Map<Integer, JavaTypeInstance> synthetics) {
        this.syntheticArgs.clear();
        this.syntheticCaptureArgs.clear();
        int offset = 0;
        switch (constructorFlag) {
            case ENUM_CONSTRUCTOR: {
                offset = 3;
                break;
            }
            case ECLIPSE_ENUM_CONSTRUCTOR: {
                offset = 1;
                break;
            }
            default: {
                if (!this.isInstanceMethod()) break;
                offset = 1;
            }
        }
        List<Slot> tmp = ListFactory.newList();
        for (Map.Entry<Integer, JavaTypeInstance> entry : synthetics.entrySet()) {
            tmp.add(new Slot(entry.getValue(), entry.getKey()));
        }
        if (!tmp.isEmpty()) {
            Slot test = tmp.get(0);
            if (offset != test.getIdx()) {
                List replacements = ListFactory.newList();
                for (Slot synthetic : tmp) {
                    JavaTypeInstance type = synthetic.getJavaTypeInstance();
                    Slot replacement = new Slot(type, offset);
                    offset += type.getStackType().getComputationCategory();
                    replacements.add(replacement);
                }
                this.syntheticArgs.addAll(replacements);
                comments.addComment(DecompilerComment.PARAMETER_CORRUPTION);
            } else {
                this.syntheticArgs.addAll(tmp);
            }
        }
    }

    public Map<Slot, SSAIdent> collectInitialSlotUsage(SSAIdentifierFactory<Slot, ?> ssaIdentifierFactory) {
        Map<Slot, SSAIdent> res = MapFactory.newOrderedMap();
        int offset = 0;
        if (this.instanceMethod) {
            Slot tgt = new Slot(this.classFile.getClassType(), 0);
            res.put(tgt, ssaIdentifierFactory.getIdent(tgt));
            offset = 1;
        }
        if (!this.syntheticArgs.isEmpty()) {
            for (Slot synthetic : this.syntheticArgs) {
                res.put(synthetic, ssaIdentifierFactory.getIdent(synthetic));
                offset += synthetic.getJavaTypeInstance().getStackType().getComputationCategory();
            }
        }
        for (JavaTypeInstance arg : this.args) {
            Slot tgt = new Slot(arg, offset);
            res.put(tgt, ssaIdentifierFactory.getIdent(tgt));
            offset += arg.getStackType().getComputationCategory();
        }
        if (!this.syntheticCaptureArgs.isEmpty()) {
            for (Slot synthetic : this.syntheticCaptureArgs) {
                res.put(synthetic, ssaIdentifierFactory.getIdent(synthetic));
                offset += synthetic.getJavaTypeInstance().getStackType().getComputationCategory();
            }
        }
        return res;
    }

    public List<LocalVariable> computeParameters(Method.MethodConstructor constructorFlag, Map<Integer, Ident> slotToIdentMap) {
        JavaTypeInstance typeInstance;
        if (this.parameterLValues != null) {
            return this.getComputedParameters();
        }
        this.parameterLValues = ListFactory.newList();
        int offset = 0;
        if (this.instanceMethod) {
            this.variableNamer.forceName(slotToIdentMap.get(0), 0L, "this");
            offset = 1;
        }
        if (constructorFlag == Method.MethodConstructor.ENUM_CONSTRUCTOR) {
            MiscUtils.handyBreakPoint();
        } else {
            for (Slot synthetic : this.syntheticArgs) {
                typeInstance = synthetic.getJavaTypeInstance();
                this.parameterLValues.add(new ParameterLValue(new LocalVariable(offset, slotToIdentMap.get(synthetic.getIdx()), this.variableNamer, 0, false, new InferredJavaType(typeInstance, InferredJavaType.Source.FIELD, true)), HiddenReason.HiddenOuterReference));
                offset += typeInstance.getStackType().getComputationCategory();
            }
        }
        for (JavaTypeInstance arg : this.args) {
            Ident ident = slotToIdentMap.get(offset);
            this.parameterLValues.add(new ParameterLValue(new LocalVariable(offset, ident, this.variableNamer, 0, false, new InferredJavaType(arg, InferredJavaType.Source.FIELD, true)), HiddenReason.NotHidden));
            offset += arg.getStackType().getComputationCategory();
        }
        for (Slot synthetic : this.syntheticCaptureArgs) {
            typeInstance = synthetic.getJavaTypeInstance();
            this.parameterLValues.add(new ParameterLValue(new LocalVariable(offset, slotToIdentMap.get(synthetic.getIdx()), this.variableNamer, 0, false, new InferredJavaType(typeInstance, InferredJavaType.Source.FIELD, true)), HiddenReason.HiddenCapture));
            offset += typeInstance.getStackType().getComputationCategory();
        }
        return this.getComputedParameters();
    }

    public JavaTypeInstance getReturnType() {
        return this.result;
    }

    public String getName() {
        return this.name;
    }

    public String getFixedName() {
        return this.fixedName != null ? this.fixedName : this.name;
    }

    public boolean hasNameBeenFixed() {
        return this.fixedName != null;
    }

    public void setFixedName(String name) {
        this.fixedName = name;
    }

    public boolean hasFormalTypeParameters() {
        return this.formalTypeParameters != null && !this.formalTypeParameters.isEmpty();
    }

    public List<JavaTypeInstance> getExplicitGenericUsage(GenericTypeBinder binder) {
        List<JavaTypeInstance> types = ListFactory.newList();
        for (FormalTypeParameter parameter : this.formalTypeParameters) {
            JavaTypeInstance type = binder.getBindingFor(parameter);
            if (type == null) {
                return null;
            }
            types.add(type);
        }
        return types;
    }

    public JavaTypeInstance getClassType() {
        if (this.classFile == null) {
            return null;
        }
        return this.classFile.getClassType();
    }

    public JavaTypeInstance getReturnType(JavaTypeInstance thisTypeInstance, List<Expression> invokingArgs) {
        if (this.classFile == null) {
            return this.result;
        }
        if (this.result == null) {
            if ("<init>".equals(this.getName())) {
                this.result = this.classFile.getClassSignature().getThisGeneralTypeClass(this.classFile.getClassType(), this.classFile.getConstantPool());
            } else {
                throw new IllegalStateException();
            }
        }
        if (this.hasFormalTypeParameters() || this.classFile.hasFormalTypeParameters()) {
            JavaGenericRefTypeInstance genericRefTypeInstance = thisTypeInstance.asGenericRefInstance(this.getClassType());
            JavaTypeInstance boundResult = this.getResultBoundAccordingly(this.result, genericRefTypeInstance, invokingArgs);
            return boundResult;
        }
        return this.result;
    }

    public List<JavaTypeInstance> getArgs() {
        return this.args;
    }

    public List<JavaTypeInstance> getSignatureBoundArgs() {
        return this.getSignatureBoundTypes(this.args);
    }

    public List<JavaTypeInstance> getExceptionTypes() {
        return this.exceptionTypes;
    }

    public List<JavaTypeInstance> getSignatureBoundExceptions() {
        return this.getSignatureBoundTypes(this.exceptionTypes);
    }

    private List<JavaTypeInstance> getSignatureBoundTypes(List<JavaTypeInstance> types) {
        if (this.classFile == null || types.isEmpty()) {
            return types;
        }
        ClassSignature sig = this.classFile.getClassSignature();
        List<FormalTypeParameter> ftp = sig.getFormalTypeParameters();
        if (ftp == null && this.formalTypeParameters == null) {
            return types;
        }
        final GenericTypeBinder gtb = GenericTypeBinder.create(ftp, this.formalTypeParameters);
        return Functional.map(types, new UnaryFunction<JavaTypeInstance, JavaTypeInstance>(){

            @Override
            public JavaTypeInstance invoke(JavaTypeInstance arg) {
                JavaTypeInstance res = arg;
                while ((res = gtb.getBindingFor(arg = res)) instanceof JavaGenericPlaceholderTypeInstance && res != arg) {
                }
                return res;
            }
        });
    }

    public int getVisibleArgCount() {
        return this.args.size() - this.hidden.size();
    }

    public boolean isInstanceMethod() {
        return this.instanceMethod;
    }

    public Expression getAppropriatelyCastedArgument(Expression expression, int argidx) {
        RawJavaType providedRawJavaType;
        JavaTypeInstance type = this.args.get(argidx);
        if (type.isComplexType()) {
            return expression;
        }
        RawJavaType expectedRawJavaType = type.getRawTypeOfSimpleType();
        if (expectedRawJavaType.compareAllPriorityTo(providedRawJavaType = expression.getInferredJavaType().getRawType()) == 0) {
            return expression;
        }
        return new CastExpression(BytecodeLoc.NONE, new InferredJavaType(expectedRawJavaType, InferredJavaType.Source.EXPRESSION, true), expression);
    }

    public void dumpAppropriatelyCastedArgumentString(Expression expression, Dumper d) {
        expression.dump(d);
    }

    public void tightenArgs(Expression object, List<Expression> expressions) {
        JavaTypeInstance objecTypeInstance;
        if (expressions.size() != this.args.size()) {
            throw new ConfusedCFRException("expr arg size mismatch");
        }
        JavaTypeInstance classType = null;
        JavaTypeInstance javaTypeInstance = objecTypeInstance = object == null ? null : object.getInferredJavaType().getJavaTypeInstance();
        if (object != null && this.classFile != null && !"<init>".equals(this.name)) {
            classType = this.classFile.getClassType();
            if (objecTypeInstance != null && objecTypeInstance.getBindingSupers() != null && objecTypeInstance.getBindingSupers().containsBase(classType)) {
                classType = objecTypeInstance.getBindingSupers().getBoundSuperForBase(classType);
            }
            object.getInferredJavaType().collapseTypeClash().noteUseAs(classType);
        }
        int length = this.args.size();
        for (int x = 0; x < length; ++x) {
            Expression expression = expressions.get(x);
            JavaTypeInstance type = this.args.get(x);
            expression.getInferredJavaType().useAsWithoutCasting(type);
        }
        GenericTypeBinder genericTypeBinder = null;
        if (classType instanceof JavaGenericBaseInstance) {
            genericTypeBinder = GenericTypeBinder.extractBindings((JavaGenericBaseInstance)classType, objecTypeInstance);
        } else if (object != null && objecTypeInstance instanceof JavaGenericBaseInstance) {
            JavaGenericRefTypeInstance boundInstance;
            JavaTypeInstance objectType = objecTypeInstance;
            List<JavaTypeInstance> invokingTypes = ListFactory.newList();
            for (Expression invokingArg : expressions) {
                invokingTypes.add(invokingArg.getInferredJavaType().getJavaTypeInstance());
            }
            JavaGenericRefTypeInstance javaGenericRefTypeInstance = boundInstance = objectType instanceof JavaGenericRefTypeInstance ? (JavaGenericRefTypeInstance)objectType : null;
            if (this.classFile != null) {
                genericTypeBinder = GenericTypeBinder.bind(this.formalTypeParameters, this.classFile.getClassSignature(), this.args, boundInstance, invokingTypes);
            }
        }
        for (int x = 0; x < length; ++x) {
            Expression expression = expressions.get(x);
            JavaTypeInstance type = this.args.get(x);
            JavaTypeInstance exprType = expression.getInferredJavaType().getJavaTypeInstance();
            if (MethodPrototype.isGenericArg(exprType) || exprType == RawJavaType.NULL) continue;
            if (genericTypeBinder != null) {
                type = genericTypeBinder.getBindingFor(type);
            }
            if (MethodPrototype.isGenericArg(type) && (!(type instanceof JavaGenericRefTypeInstance) || ((JavaGenericRefTypeInstance)type).hasUnbound())) continue;
            expressions.set(x, new CastExpression(BytecodeLoc.NONE, new InferredJavaType(type, InferredJavaType.Source.PROTOTYPE, true), expression));
        }
    }

    private static boolean isGenericArg(JavaTypeInstance arg) {
        return (arg = arg.getArrayStrippedType()) instanceof JavaGenericBaseInstance;
    }

    public String getComparableString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName()).append('(');
        for (JavaTypeInstance arg : this.args) {
            sb.append(arg.getRawName()).append(" ");
        }
        sb.append(')');
        return sb.toString();
    }

    public String toString() {
        return this.getComparableString();
    }

    public boolean equalsGeneric(MethodPrototype other) {
        GenericTypeBinder genericTypeBinder = GenericTypeBinder.createEmpty();
        return this.equalsGeneric(other, genericTypeBinder);
    }

    public boolean equalsGeneric(MethodPrototype other, GenericTypeBinder genericTypeBinder) {
        JavaTypeInstance deGenerifiedResOther;
        JavaTypeInstance deGenerifiedRes;
        List<JavaTypeInstance> otherArgs = other.args;
        if (otherArgs.size() != this.args.size()) {
            return false;
        }
        JavaTypeInstance otherRes = other.getReturnType();
        JavaTypeInstance res = this.getReturnType();
        if (res != null && otherRes != null && !(deGenerifiedRes = res.getDeGenerifiedType()).equals(deGenerifiedResOther = otherRes.getDeGenerifiedType())) {
            if (res instanceof JavaGenericBaseInstance) {
                if (!((JavaGenericBaseInstance)res).tryFindBinding(otherRes, genericTypeBinder)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        for (int x = 0; x < this.args.size(); ++x) {
            JavaTypeInstance deGenerifiedRhs;
            JavaTypeInstance lhs = this.args.get(x);
            JavaTypeInstance rhs = otherArgs.get(x);
            JavaTypeInstance deGenerifiedLhs = lhs.getDeGenerifiedType();
            if (deGenerifiedLhs.equals(deGenerifiedRhs = rhs.getDeGenerifiedType())) continue;
            if (lhs instanceof JavaGenericBaseInstance) {
                if (((JavaGenericBaseInstance)lhs).tryFindBinding(rhs, genericTypeBinder)) continue;
                return false;
            }
            return false;
        }
        return true;
    }

    public GenericTypeBinder getTypeBinderForTypes(List<JavaTypeInstance> invokingArgTypes) {
        if (this.classFile == null) {
            return null;
        }
        if (invokingArgTypes.size() != this.args.size()) {
            return null;
        }
        GenericTypeBinder genericTypeBinder = GenericTypeBinder.bind(this.formalTypeParameters, this.classFile.getClassSignature(), this.args, null, invokingArgTypes);
        return genericTypeBinder;
    }

    public GenericTypeBinder getTypeBinderFor(List<Expression> invokingArgs) {
        List<JavaTypeInstance> invokingTypes = ListFactory.newList();
        for (Expression invokingArg : invokingArgs) {
            invokingTypes.add(invokingArg.getInferredJavaType().getJavaTypeInstance());
        }
        return this.getTypeBinderForTypes(invokingTypes);
    }

    private JavaTypeInstance getResultBoundAccordingly(JavaTypeInstance result, JavaGenericRefTypeInstance boundInstance, List<Expression> invokingArgs) {
        if (result instanceof JavaArrayTypeInstance) {
            JavaArrayTypeInstance arrayTypeInstance = (JavaArrayTypeInstance)result;
            JavaTypeInstance stripped = result.getArrayStrippedType();
            JavaTypeInstance tmp = this.getResultBoundAccordinglyInner(stripped, boundInstance, invokingArgs);
            if (tmp == stripped) {
                return result;
            }
            return new JavaArrayTypeInstance(arrayTypeInstance.getNumArrayDimensions(), tmp);
        }
        return this.getResultBoundAccordinglyInner(result, boundInstance, invokingArgs);
    }

    private JavaTypeInstance getResultBoundAccordinglyInner(JavaTypeInstance result, JavaGenericRefTypeInstance boundInstance, List<Expression> invokingArgs) {
        if (!(result instanceof JavaGenericBaseInstance)) {
            return result;
        }
        List<JavaTypeInstance> invokingTypes = ListFactory.newList();
        for (Expression invokingArg : invokingArgs) {
            invokingTypes.add(invokingArg.getInferredJavaType().getJavaTypeInstance());
        }
        GenericTypeBinder genericTypeBinder = GenericTypeBinder.bind(this.formalTypeParameters, this.classFile.getClassSignature(), this.args, boundInstance, invokingTypes);
        if (genericTypeBinder == null) {
            return result;
        }
        JavaGenericBaseInstance genericResult = (JavaGenericBaseInstance)result;
        JavaTypeInstance boundResultInstance = genericResult.getBoundInstance(genericTypeBinder);
        if (boundResultInstance instanceof JavaWildcardTypeInstance) {
            boundResultInstance = ((JavaWildcardTypeInstance)boundResultInstance).getUnderlyingType();
        }
        return boundResultInstance;
    }

    public boolean isVarArgs() {
        return this.varargs;
    }

    public boolean equalsMatch(MethodPrototype other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!this.name.equals(other.name)) {
            return false;
        }
        List<JavaTypeInstance> otherArgs = other.getArgs();
        if (!this.args.equals(otherArgs)) {
            return false;
        }
        if (this.result != null && other.result != null && !this.result.equals(other.result)) {
            BindingSuperContainer otherBindingSupers = other.result.getBindingSupers();
            if (otherBindingSupers == null) {
                return false;
            }
            return otherBindingSupers.containsBase(this.result);
        }
        return true;
    }

    public void setMethodScopedSyntheticConstructorParameters(NavigableMap<Integer, JavaTypeInstance> missing) {
        List<Slot> missingList = ListFactory.newList();
        int expected = 0;
        for (Map.Entry missingItem : missing.entrySet()) {
            Integer thisOffset = (Integer)missingItem.getKey();
            JavaTypeInstance type = (JavaTypeInstance)missingItem.getValue();
            while (thisOffset > expected) {
                missingList.add(new Slot(expected == 0 ? RawJavaType.REF : RawJavaType.NULL, expected++));
            }
            missingList.add(new Slot(type, thisOffset));
            expected = thisOffset + type.getStackType().getComputationCategory();
        }
        if (missingList.size() < 2) {
            return;
        }
        if (this.descriptorProto != null) {
            List<JavaTypeInstance> descriptorArgs = this.descriptorProto.args;
            for (int x = 0; x < descriptorArgs.size() - this.args.size(); ++x) {
                int y;
                if (!MethodPrototype.satisfies(descriptorArgs, x, this.args)) continue;
                int s = this.args.size() + x;
                this.args.clear();
                this.args.addAll(descriptorArgs);
                for (y = 0; y < x; ++y) {
                    this.hide(y);
                }
                for (y = s; y < this.args.size(); ++y) {
                    this.hide(y);
                }
            }
        }
        if (((Slot)missingList.get(0)).getJavaTypeInstance() != RawJavaType.REF) {
            return;
        }
        Slot removed = (Slot)missingList.remove(0);
        boolean all0 = MethodPrototype.satisfiesSlots(missingList, 0, this.args);
        boolean all1 = MethodPrototype.satisfiesSlots(missingList, 1, this.args);
        if (all1) {
            this.syntheticArgs.add(missingList.remove(0));
        } else if (!all0) {
            this.syntheticArgs.add(missingList.remove(0));
        }
        for (int x = this.args.size(); x < missingList.size(); ++x) {
            this.syntheticCaptureArgs.add(missingList.get(x));
        }
    }

    private static boolean satisfies(List<JavaTypeInstance> haystack, int start, List<JavaTypeInstance> args) {
        if (haystack.size() - start < args.size()) {
            return false;
        }
        for (int x = 0; x < args.size(); ++x) {
            JavaTypeInstance here = haystack.get(x + start);
            JavaTypeInstance expected = args.get(x);
            if (expected.equals(here)) continue;
            return false;
        }
        return true;
    }

    private static boolean satisfiesSlots(List<Slot> haystack, int start, List<JavaTypeInstance> args) {
        List<Slot> originalHaystack = haystack;
        if (haystack.size() - start < args.size()) {
            return false;
        }
        for (int x = 0; x < args.size(); ++x) {
            StackType st2;
            Slot here = haystack.get(x + start);
            JavaTypeInstance expected = args.get(x);
            StackType st1 = here.getJavaTypeInstance().getStackType();
            if (st1 == (st2 = expected.getStackType())) continue;
            if (here.getJavaTypeInstance() == RawJavaType.NULL) {
                switch (st2.getComputationCategory()) {
                    case 1: {
                        haystack = ListFactory.newList(haystack);
                        haystack.set(x + start, new Slot(expected, here.getIdx()));
                        break;
                    }
                    case 2: {
                        Slot here2;
                        if (haystack.size() > x + start + 1 && (here2 = haystack.get(x + start + 1)).getJavaTypeInstance() == RawJavaType.NULL) {
                            haystack = ListFactory.newList(haystack);
                            haystack.remove(x + start + 1);
                            break;
                        }
                        return false;
                    }
                }
                continue;
            }
            return false;
        }
        if (haystack != originalHaystack) {
            originalHaystack.clear();
            originalHaystack.addAll(haystack);
        }
        return true;
    }

    public String getOriginalDescriptor() {
        return this.originalDescriptor;
    }

    public static enum HiddenReason {
        NotHidden,
        HiddenOuterReference,
        HiddenCapture;

    }

    public static class ParameterLValue {
        public LocalVariable localVariable;
        public HiddenReason hidden;

        ParameterLValue(LocalVariable localVariable, HiddenReason hidden) {
            this.localVariable = localVariable;
            this.hidden = hidden;
        }

        public String toString() {
            return "" + this.localVariable + " [" + (Object)((Object)this.hidden) + "]";
        }

        public boolean isHidden() {
            return this.hidden != HiddenReason.NotHidden;
        }
    }
}

