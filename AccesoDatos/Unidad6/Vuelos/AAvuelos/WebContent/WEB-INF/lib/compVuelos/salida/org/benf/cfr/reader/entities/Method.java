/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.DeclarationAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototypeAnnotationsHelper;
import org.benf.cfr.reader.bytecode.analysis.types.TypeAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.variables.Ident;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamer;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamerFactory;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.annotations.ElementValue;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.attributes.AttributeAnnotationDefault;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.attributes.AttributeExceptions;
import org.benf.cfr.reader.entities.attributes.AttributeLocalVariableTable;
import org.benf.cfr.reader.entities.attributes.AttributeMap;
import org.benf.cfr.reader.entities.attributes.AttributeSignature;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationTargetInfo;
import org.benf.cfr.reader.entities.classfilehelpers.VisibilityHelper;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolUtils;
import org.benf.cfr.reader.entityfactories.AttributeFactory;
import org.benf.cfr.reader.entityfactories.ContiguousEntityFactory;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.LocalClassAwareTypeUsageInformation;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.KnowsRawSize;
import org.benf.cfr.reader.util.MalformedPrototypeException;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.CollectionUtils;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.TypeContext;

public class Method
implements KnowsRawSize,
TypeUsageCollectable {
    private static final long OFFSET_OF_ACCESS_FLAGS = 0L;
    private static final long OFFSET_OF_NAME_INDEX = 2L;
    private static final long OFFSET_OF_DESCRIPTOR_INDEX = 4L;
    private static final long OFFSET_OF_ATTRIBUTES_COUNT = 6L;
    private static final long OFFSET_OF_ATTRIBUTES = 8L;
    private static final AnnotationTableEntry OVERRIDE_ANNOTATION = new AnnotationTableEntry(TypeConstants.OVERRIDE, Collections.<String, ElementValue>emptyMap());
    private final long length;
    private final EnumSet<AccessFlagMethod> accessFlags;
    private final AttributeMap attributes;
    private MethodConstructor isConstructor;
    private final int descriptorIndex;
    private final AttributeCode codeAttribute;
    private final ConstantPool cp;
    private final VariableNamer variableNamer;
    private final MethodPrototype methodPrototype;
    private final ClassFile classFile;
    private Visibility hidden;
    private DecompilerComments comments;
    private final Map<JavaRefTypeInstance, String> localClasses = MapFactory.newOrderedMap();
    private boolean isOverride;
    private transient Set<JavaTypeInstance> thrownTypes = null;

    public Method(ByteData raw, ClassFile classFile, ConstantPool cp, DCCommonState dcCommonState, ClassFileVersion classFileVersion) {
        AttributeCode codeAttribute;
        Options options = dcCommonState.getOptions();
        this.cp = cp;
        this.classFile = classFile;
        this.accessFlags = AccessFlagMethod.build(raw.getU2At(0L));
        this.descriptorIndex = raw.getU2At(4L);
        this.hidden = Visibility.Visible;
        int nameIndex = raw.getU2At(2L);
        String initialName = cp.getUTF8Entry(nameIndex).getValue();
        int numAttributes = raw.getU2At(6L);
        ArrayList<Attribute> tmpAttributes = new ArrayList<Attribute>();
        tmpAttributes.ensureCapacity(numAttributes);
        long attributesLength = ContiguousEntityFactory.build(raw.getOffsetData(8L), numAttributes, tmpAttributes, AttributeFactory.getBuilder(cp, classFileVersion));
        this.attributes = new AttributeMap(tmpAttributes);
        AccessFlagMethod.applyAttributes(this.attributes, this.accessFlags);
        this.length = 8L + attributesLength;
        MethodConstructor methodConstructor = MethodConstructor.NOT;
        if (initialName.equals("<init>")) {
            boolean isEnum = classFile.getAccessFlags().contains((Object)AccessFlag.ACC_ENUM);
            methodConstructor = isEnum ? MethodConstructor.ENUM_CONSTRUCTOR : MethodConstructor.CONSTRUCTOR;
        } else if (initialName.equals("<clinit>")) {
            methodConstructor = MethodConstructor.STATIC_CONSTRUCTOR;
            this.accessFlags.clear();
            this.accessFlags.add(AccessFlagMethod.ACC_STATIC);
        }
        this.isConstructor = methodConstructor;
        if (methodConstructor.isConstructor() && this.accessFlags.contains((Object)AccessFlagMethod.ACC_STRICT)) {
            this.accessFlags.remove((Object)AccessFlagMethod.ACC_STRICT);
            classFile.getAccessFlags().add(AccessFlag.ACC_STRICT);
        }
        if ((codeAttribute = (AttributeCode)this.attributes.getByName("Code")) == null) {
            this.variableNamer = VariableNamerFactory.getNamer(null, cp);
            this.codeAttribute = null;
        } else {
            this.codeAttribute = codeAttribute;
            AttributeLocalVariableTable variableTable = (Boolean)options.getOption(OptionsImpl.USE_NAME_TABLE) != false ? this.codeAttribute.getLocalVariableTable() : null;
            this.variableNamer = VariableNamerFactory.getNamer(variableTable, cp);
            this.codeAttribute.setMethod(this);
        }
        this.methodPrototype = this.generateMethodPrototype(options, initialName, methodConstructor);
        if (this.accessFlags.contains((Object)AccessFlagMethod.ACC_BRIDGE) && !this.accessFlags.contains((Object)AccessFlagMethod.ACC_STATIC) && ((Boolean)options.getOption(OptionsImpl.HIDE_BRIDGE_METHODS)).booleanValue()) {
            this.hidden = Visibility.HiddenBridge;
        }
    }

    void releaseCode() {
        if (this.codeAttribute != null) {
            this.codeAttribute.releaseCode();
        }
        this.attributes.clear();
    }

    public boolean hasDumpableAttributes() {
        return this.attributes.any("RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations", "RuntimeVisibleTypeAnnotations", "RuntimeInvisibleTypeAnnotations", "RuntimeVisibleParameterAnnotations", "RuntimeInvisibleParameterAnnotations");
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.methodPrototype.collectTypeUsages(collector);
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeVisibleAnnotations"));
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeInvisibleAnnotations"));
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeVisibleTypeAnnotations"));
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeInvisibleTypeAnnotations"));
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeVisibleParameterAnnotations"));
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeInvisibleParameterAnnotations"));
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("AnnotationDefault"));
        if (this.codeAttribute != null) {
            this.codeAttribute.collectTypeUsages(collector);
            this.codeAttribute.analyse().collectTypeUsages(collector);
        }
        collector.collect(this.localClasses.keySet());
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("Exceptions"));
    }

    public boolean copyLocalClassesFrom(Method other) {
        for (Map.Entry<JavaRefTypeInstance, String> entry : other.localClasses.entrySet()) {
            this.markUsedLocalClassType(entry.getKey(), entry.getValue());
        }
        return !other.localClasses.isEmpty();
    }

    public Set<AccessFlagMethod> getAccessFlags() {
        return this.accessFlags;
    }

    public void hideSynthetic() {
        this.hidden = Visibility.HiddenSynthetic;
    }

    public void hideDead() {
        this.hidden = Visibility.HiddenDeadCode;
    }

    public Visibility hiddenState() {
        return this.hidden;
    }

    public boolean testAccessFlag(AccessFlagMethod flag) {
        return this.accessFlags.contains((Object)flag);
    }

    public MethodConstructor getConstructorFlag() {
        return this.isConstructor;
    }

    public void setConstructorFlag(MethodConstructor flag) {
        this.isConstructor = flag;
    }

    AttributeSignature getSignatureAttribute() {
        return (AttributeSignature)this.attributes.getByName("Signature");
    }

    public VariableNamer getVariableNamer() {
        return this.variableNamer;
    }

    public ClassFile getClassFile() {
        return this.classFile;
    }

    @Override
    public long getRawByteLength() {
        return this.length;
    }

    public String getName() {
        return this.methodPrototype.getName();
    }

    private MethodPrototype generateMethodPrototype(Options options, String initialName, MethodConstructor constructorFlag) {
        MethodPrototype desproto;
        boolean isEnum;
        AttributeSignature sig = (Boolean)options.getOption(OptionsImpl.USE_SIGNATURES) != false ? this.getSignatureAttribute() : null;
        ConstantPoolEntryUTF8 signature = sig == null ? null : sig.getSignature();
        ConstantPoolEntryUTF8 descriptor = this.cp.getUTF8Entry(this.descriptorIndex);
        boolean isInstance = !this.accessFlags.contains((Object)AccessFlagMethod.ACC_STATIC);
        boolean isVarargs = this.accessFlags.contains((Object)AccessFlagMethod.ACC_VARARGS);
        boolean isSynthetic = this.accessFlags.contains((Object)AccessFlagMethod.ACC_SYNTHETIC);
        DCCommonState state = this.cp.getDCCommonState();
        MethodPrototype sigproto = null;
        boolean bl = isEnum = constructorFlag == MethodConstructor.ENUM_CONSTRUCTOR;
        if (signature == null && isEnum) {
            constructorFlag = MethodConstructor.ECLIPSE_ENUM_CONSTRUCTOR;
        }
        if (signature != null) {
            try {
                sigproto = ConstantPoolUtils.parseJavaMethodPrototype(state, this.classFile, this.classFile.getClassType(), initialName, isInstance, constructorFlag, signature, this.cp, isVarargs, isSynthetic, this.variableNamer, descriptor.getValue());
            }
            catch (MalformedPrototypeException malformedPrototypeException) {
                // empty catch block
            }
        }
        try {
            desproto = ConstantPoolUtils.parseJavaMethodPrototype(state, this.classFile, this.classFile.getClassType(), initialName, isInstance, constructorFlag, descriptor, this.cp, isVarargs, isSynthetic, this.variableNamer, descriptor.getValue());
        }
        catch (MalformedPrototypeException e) {
            if (sigproto == null) {
                throw e;
            }
            desproto = sigproto;
        }
        if (this.classFile.isInnerClass() && sigproto != null && desproto.getArgs().size() != sigproto.getArgs().size()) {
            Method.fixupInnerClassSignature(desproto, sigproto);
        }
        if (sigproto == null) {
            return desproto;
        }
        if (Method.checkSigProto(desproto, sigproto, isEnum, this.classFile.isInnerClass() && constructorFlag.isConstructor())) {
            return sigproto;
        }
        this.addComment(DecompilerComment.BAD_SIGNATURE);
        return desproto;
    }

    private static boolean checkSigProto(MethodPrototype desproto, MethodPrototype sigproto, boolean isEnumConstructor, boolean isInnerConstructor) {
        if (sigproto == null) {
            return false;
        }
        List<JavaTypeInstance> desargs = desproto.getArgs();
        List<JavaTypeInstance> sigargs = sigproto.getArgs();
        int offset = 0;
        if (desargs.size() != sigargs.size()) {
            return isInnerConstructor || isEnumConstructor;
        }
        int len = sigargs.size();
        for (int x = 0; x < len; ++x) {
            JavaTypeInstance desarg = desargs.get(x + offset).getArrayStrippedType();
            JavaTypeInstance sigarg = sigargs.get(x).getArrayStrippedType();
            if (sigarg instanceof JavaGenericPlaceholderTypeInstance || sigarg.getDeGenerifiedType().equals(desarg.getDeGenerifiedType())) continue;
            return false;
        }
        return true;
    }

    private static void fixupInnerClassSignature(MethodPrototype descriptor, MethodPrototype signature) {
        List<JavaTypeInstance> descriptorArgs = descriptor.getArgs();
        List<JavaTypeInstance> signatureArgs = signature.getArgs();
        if (signatureArgs.size() != descriptorArgs.size() - 1) {
            signature.setDescriptorProto(descriptor);
            return;
        }
        for (int x = 0; x < signatureArgs.size(); ++x) {
            if (descriptorArgs.get(x + 1).equals(signatureArgs.get(x).getDeGenerifiedType())) continue;
            return;
        }
        signatureArgs.add(0, descriptorArgs.get(0));
    }

    public MethodPrototype getMethodPrototype() {
        return this.methodPrototype;
    }

    void markOverride() {
        this.isOverride = true;
    }

    public void markUsedLocalClassType(JavaTypeInstance javaTypeInstance, String suggestedName) {
        if (!((javaTypeInstance = javaTypeInstance.getDeGenerifiedType()) instanceof JavaRefTypeInstance)) {
            throw new IllegalStateException("Bad local class Type " + javaTypeInstance.getRawName());
        }
        this.localClasses.put((JavaRefTypeInstance)javaTypeInstance, suggestedName);
    }

    public void markUsedLocalClassType(JavaTypeInstance javaTypeInstance) {
        this.markUsedLocalClassType(javaTypeInstance, null);
    }

    private void dumpMethodAnnotations(Dumper d, List<AnnotationTableEntry> nullableDeclAnnotations) {
        if (this.isOverride) {
            OVERRIDE_ANNOTATION.dump(d).newln();
        }
        if (nullableDeclAnnotations != null) {
            for (AnnotationTableEntry annotation : nullableDeclAnnotations) {
                annotation.dump(d).newln();
            }
        }
    }

    private List<JavaTypeInstance> getDeclaredThrownTypes() {
        List<JavaTypeInstance> attributeTypes = this.getAttributeDeclaredThrownTypes();
        List<JavaTypeInstance> prototypeExceptionTypes = this.methodPrototype.getExceptionTypes();
        int len = attributeTypes.size();
        if (len != prototypeExceptionTypes.size()) {
            return attributeTypes;
        }
        List<JavaTypeInstance> boundProtoExceptionTypes = this.methodPrototype.getSignatureBoundExceptions();
        for (int x = 0; x < len; ++x) {
            JavaTypeInstance signatureType = boundProtoExceptionTypes.get(x);
            JavaTypeInstance attributeType = attributeTypes.get(x);
            if (attributeType.equals(signatureType)) continue;
            return attributeTypes;
        }
        return prototypeExceptionTypes;
    }

    private List<JavaTypeInstance> getAttributeDeclaredThrownTypes() {
        AttributeExceptions exceptionsAttribute = (AttributeExceptions)this.attributes.getByName("Exceptions");
        if (exceptionsAttribute != null) {
            return Functional.map(exceptionsAttribute.getExceptionClassList(), new UnaryFunction<ConstantPoolEntryClass, JavaTypeInstance>(){

                @Override
                public JavaTypeInstance invoke(ConstantPoolEntryClass arg) {
                    return arg.getTypeInstance();
                }
            });
        }
        return Collections.emptyList();
    }

    public Set<JavaTypeInstance> getThrownTypes() {
        if (this.thrownTypes == null) {
            this.thrownTypes = new LinkedHashSet<JavaTypeInstance>(this.getDeclaredThrownTypes());
        }
        return this.thrownTypes;
    }

    private void dumpSignatureText(boolean asClass, Dumper d) {
        String prefix;
        MethodPrototypeAnnotationsHelper annotationsHelper = new MethodPrototypeAnnotationsHelper(this.attributes);
        JavaTypeInstance nullableReturnType = this.getMethodPrototype().getReturnType();
        DeclarationAnnotationHelper.DeclarationAnnotationsInfo annotationsInfo = DeclarationAnnotationHelper.getDeclarationInfo(nullableReturnType, annotationsHelper.getMethodAnnotations(), annotationsHelper.getMethodReturnAnnotations());
        boolean usesAdmissibleType = !annotationsInfo.requiresNonAdmissibleType();
        this.dumpMethodAnnotations(d, annotationsInfo.getDeclarationAnnotations(usesAdmissibleType));
        EnumSet<AccessFlagMethod> localAccessFlags = SetFactory.newSet(this.accessFlags);
        if (!asClass) {
            if (this.codeAttribute != null && !this.accessFlags.contains((Object)AccessFlagMethod.ACC_STATIC) && !this.accessFlags.contains((Object)AccessFlagMethod.ACC_PRIVATE)) {
                d.keyword("default ");
            }
            localAccessFlags.remove((Object)AccessFlagMethod.ACC_ABSTRACT);
        }
        localAccessFlags.remove((Object)AccessFlagMethod.ACC_VARARGS);
        if (((Boolean)this.cp.getDCCommonState().getOptions().getOption(OptionsImpl.ATTRIBUTE_OBF)).booleanValue()) {
            localAccessFlags.remove((Object)AccessFlagMethod.ACC_SYNTHETIC);
            localAccessFlags.remove((Object)AccessFlagMethod.ACC_BRIDGE);
        }
        if (!(prefix = CollectionUtils.join(localAccessFlags, " ")).isEmpty()) {
            d.keyword(prefix);
        }
        if (this.isConstructor == MethodConstructor.STATIC_CONSTRUCTOR) {
            return;
        }
        if (!prefix.isEmpty()) {
            d.print(' ');
        }
        String displayName = this.isConstructor.isConstructor() ? d.getTypeUsageInformation().getName(this.classFile.getClassType(), TypeContext.None) : this.methodPrototype.getFixedName();
        this.getMethodPrototype().dumpDeclarationSignature(d, displayName, this.isConstructor, annotationsHelper, annotationsInfo.getTypeAnnotations(usesAdmissibleType));
        AttributeExceptions exceptionsAttribute = (AttributeExceptions)this.attributes.getByName("Exceptions");
        if (exceptionsAttribute != null) {
            List<AnnotationTableTypeEntry> att = annotationsHelper.getTypeTargetAnnotations(TypeAnnotationEntryValue.type_throws);
            d.print(" throws ");
            boolean first = true;
            int idx = -1;
            for (JavaTypeInstance typeInstance : this.getDeclaredThrownTypes()) {
                ++idx;
                first = StringUtils.comma(first, d);
                if (att != null) {
                    JavaAnnotatedTypeInstance jat = typeInstance.getAnnotatedInstance();
                    final int sidx = idx;
                    List<AnnotationTableTypeEntry> exceptionAnnotations = Functional.filter(att, new Predicate<AnnotationTableTypeEntry>(){

                        @Override
                        public boolean test(AnnotationTableTypeEntry in) {
                            return sidx == ((TypeAnnotationTargetInfo.TypeAnnotationThrowsTarget)in.getTargetInfo()).getIndex();
                        }
                    });
                    DecompilerComments comments = new DecompilerComments();
                    TypeAnnotationHelper.apply(jat, exceptionAnnotations, comments);
                    d.dump(comments);
                    d.dump(jat);
                    continue;
                }
                d.dump(typeInstance);
            }
        }
    }

    public Op04StructuredStatement getAnalysis() {
        if (this.codeAttribute == null) {
            throw new ConfusedCFRException("No code in this method to analyze");
        }
        Op04StructuredStatement analysis = this.codeAttribute.analyse();
        return analysis;
    }

    public boolean isConstructor() {
        return this.isConstructor.isConstructor();
    }

    void analyse() {
        try {
            if (this.codeAttribute != null) {
                this.codeAttribute.analyse();
            }
            if (!this.methodPrototype.parametersComputed()) {
                LazyMap<Integer, Ident> identMap = MapFactory.newLazyMap(new UnaryFunction<Integer, Ident>(){

                    @Override
                    public Ident invoke(Integer arg) {
                        return new Ident(arg, 0);
                    }
                });
                this.methodPrototype.computeParameters(this.getConstructorFlag(), identMap);
            }
        }
        catch (RuntimeException e) {
            System.out.println("While processing method : " + this.getName());
            throw e;
        }
    }

    public boolean hasCodeAttribute() {
        return this.codeAttribute != null;
    }

    public AttributeCode getCodeAttribute() {
        return this.codeAttribute;
    }

    private void dumpComments(Dumper d) {
        if (this.comments != null) {
            this.comments.dump(d);
            for (DecompilerComment decompilerComment : this.comments.getCommentCollection()) {
                String string = decompilerComment.getSummaryMessage();
                if (string == null) continue;
                d.addSummaryError(this, string);
            }
        }
    }

    public void setComments(DecompilerComments comments) {
        if (this.comments == null) {
            this.comments = comments;
        } else {
            this.comments.addComments(comments.getCommentCollection());
        }
    }

    private void addComment(DecompilerComment comment) {
        if (this.comments == null) {
            this.comments = new DecompilerComments();
        }
        this.comments.addComment(comment);
    }

    public boolean isVisibleTo(JavaRefTypeInstance maybeCaller) {
        return VisibilityHelper.isVisibleTo(maybeCaller, this.getClassFile(), this.accessFlags.contains((Object)AccessFlagMethod.ACC_PUBLIC), this.accessFlags.contains((Object)AccessFlagMethod.ACC_PRIVATE), this.accessFlags.contains((Object)AccessFlagMethod.ACC_PROTECTED));
    }

    public void dump(Dumper d, boolean asClass) {
        if (this.codeAttribute != null) {
            this.codeAttribute.analyse();
        }
        this.dumpComments(d);
        this.dumpSignatureText(asClass, d);
        if (this.codeAttribute == null) {
            AttributeAnnotationDefault annotationDefault = (AttributeAnnotationDefault)this.attributes.getByName("AnnotationDefault");
            if (annotationDefault != null) {
                JavaTypeInstance resultType = this.methodPrototype.getReturnType();
                d.keyword(" default ").dump(annotationDefault.getElementValue().withTypeHint(resultType));
            }
            d.endCodeln();
        } else {
            if (!this.localClasses.isEmpty()) {
                TypeUsageInformation tui = d.getTypeUsageInformation();
                Map<JavaRefTypeInstance, String> filteredLocalClasses = MapFactory.newMap();
                for (Map.Entry<JavaRefTypeInstance, String> entry : this.localClasses.entrySet()) {
                    if (entry.getValue() == null && tui.hasLocalInstance(entry.getKey())) continue;
                    filteredLocalClasses.put(entry.getKey(), entry.getValue());
                }
                if (!filteredLocalClasses.isEmpty()) {
                    LocalClassAwareTypeUsageInformation overrides = new LocalClassAwareTypeUsageInformation(filteredLocalClasses, d.getTypeUsageInformation());
                    d = d.withTypeUsageInformation(overrides);
                }
            }
            d.print(' ').dump(this.codeAttribute);
        }
    }

    public String toString() {
        return this.getName() + ": " + this.methodPrototype;
    }

    public static enum Visibility {
        Visible,
        HiddenSynthetic,
        HiddenBridge,
        HiddenDeadCode;

    }

    public static enum MethodConstructor {
        NOT(false, false),
        STATIC_CONSTRUCTOR(false, false),
        CONSTRUCTOR(true, false),
        RECORD_CANONICAL_CONSTRUCTOR(true, false),
        ENUM_CONSTRUCTOR(true, true),
        ECLIPSE_ENUM_CONSTRUCTOR(true, true);

        private final boolean isConstructor;
        private final boolean isEnumConstructor;

        private MethodConstructor(boolean isConstructor, boolean isEnumConstructor) {
            this.isConstructor = isConstructor;
            this.isEnumConstructor = isEnumConstructor;
        }

        public boolean isConstructor() {
            return this.isConstructor;
        }

        public boolean isEnumConstructor() {
            return this.isEnumConstructor;
        }
    }
}

