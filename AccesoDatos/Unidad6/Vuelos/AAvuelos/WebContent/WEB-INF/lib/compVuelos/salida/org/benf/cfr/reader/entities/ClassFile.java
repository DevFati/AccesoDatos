/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.CodeAnalyserWholeClass;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.LiteralRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Triplet;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.BoundSuperCollector;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.FormalTypeParameter;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamerDefault;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.FakeMethod;
import org.benf.cfr.reader.entities.FakeMethods;
import org.benf.cfr.reader.entities.Field;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.attributes.AttributeBootstrapMethods;
import org.benf.cfr.reader.entities.attributes.AttributeEnclosingMethod;
import org.benf.cfr.reader.entities.attributes.AttributeInnerClasses;
import org.benf.cfr.reader.entities.attributes.AttributeMap;
import org.benf.cfr.reader.entities.attributes.AttributePermittedSubclasses;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeSignature;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationTargetInfo;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumper;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperAnnotation;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperInterface;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperModule;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperNormal;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryNameAndType;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolUtils;
import org.benf.cfr.reader.entities.innerclass.InnerClassAttributeInfo;
import org.benf.cfr.reader.entityfactories.AttributeFactory;
import org.benf.cfr.reader.entityfactories.ContiguousEntityFactory;
import org.benf.cfr.reader.relationship.MemberNameResolver;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.InnerClassTypeUsageInformation;
import org.benf.cfr.reader.state.OverloadMethodSetCache;
import org.benf.cfr.reader.state.TypeUsageCollectingDumper;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.functors.UnaryProcedure;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierReplacement;

public class ClassFile
implements Dumpable,
TypeUsageCollectable {
    private static final long OFFSET_OF_MAGIC = 0L;
    private static final long OFFSET_OF_MINOR = 4L;
    private static final long OFFSET_OF_MAJOR = 6L;
    private static final long OFFSET_OF_CONSTANT_POOL_COUNT = 8L;
    private static final long OFFSET_OF_CONSTANT_POOL = 10L;
    private final ConstantPool constantPool;
    private final Set<AccessFlag> accessFlags;
    private final List<ClassFileField> fields;
    private Map<String, Map<JavaTypeInstance, ClassFileField>> fieldsByName;
    private final List<Method> methods;
    private FakeMethods fakeMethods;
    private Map<String, List<Method>> methodsByName;
    private final boolean isInnerClass;
    private final Map<JavaTypeInstance, Pair<InnerClassAttributeInfo, ClassFile>> innerClassesByTypeInfo;
    private final AttributeMap attributes;
    private final ConstantPoolEntryClass thisClass;
    private final ConstantPoolEntryClass rawSuperClass;
    private final List<ConstantPoolEntryClass> rawInterfaces;
    private final ClassSignature classSignature;
    private ClassFileVersion classFileVersion;
    private DecompilerComments decompilerComments;
    private boolean begunAnalysis;
    private boolean hiddenInnerClass;
    private BindingSuperContainer boundSuperClasses;
    private ClassFileDumper dumpHelper;
    private final String usePath;
    private List<ConstructorInvokationAnonymousInner> anonymousUsages = ListFactory.newList();
    private List<ConstructorInvokationSimple> methodUsages = ListFactory.newList();

    public ClassFile(ByteData data, String usePath, final DCCommonState dcCommonState) {
        DecompilerComment renamedClass;
        ClassFileVersion classFileVersion;
        this.usePath = usePath;
        Options options = dcCommonState.getOptions();
        int magic = data.getS4At(0L);
        if (magic != -889275714) {
            throw new ConfusedCFRException("Magic != Cafebabe for class file '" + usePath + "'");
        }
        int minorVer = data.getU2At(4L);
        int majorVer = data.getU2At(6L);
        ClassFileVersion forced = (ClassFileVersion)options.getOption(OptionsImpl.FORCE_CLASSFILEVER);
        this.classFileVersion = classFileVersion = forced != null ? forced : new ClassFileVersion(majorVer, minorVer);
        final ClassFileVersion cfv = classFileVersion;
        int constantPoolCount = data.getU2At(8L);
        this.constantPool = new ConstantPool(this, dcCommonState, data.getOffsetData(10L), constantPoolCount);
        long OFFSET_OF_ACCESS_FLAGS = 10L + this.constantPool.getRawByteLength();
        long OFFSET_OF_THIS_CLASS = OFFSET_OF_ACCESS_FLAGS + 2L;
        long OFFSET_OF_SUPER_CLASS = OFFSET_OF_THIS_CLASS + 2L;
        long OFFSET_OF_INTERFACES_COUNT = OFFSET_OF_SUPER_CLASS + 2L;
        long OFFSET_OF_INTERFACES = OFFSET_OF_INTERFACES_COUNT + 2L;
        int numInterfaces = data.getU2At(OFFSET_OF_INTERFACES_COUNT);
        ArrayList<ConstantPoolEntryClass> tmpInterfaces = new ArrayList<ConstantPoolEntryClass>();
        ContiguousEntityFactory.buildSized(data.getOffsetData(OFFSET_OF_INTERFACES), (short)numInterfaces, 2, tmpInterfaces, new UnaryFunction<ByteData, ConstantPoolEntryClass>(){

            @Override
            public ConstantPoolEntryClass invoke(ByteData arg) {
                int offset = arg.getU2At(0L);
                return (ConstantPoolEntryClass)ClassFile.this.constantPool.getEntry(offset);
            }
        });
        this.thisClass = (ConstantPoolEntryClass)this.constantPool.getEntry(data.getU2At(OFFSET_OF_THIS_CLASS));
        this.rawInterfaces = tmpInterfaces;
        this.accessFlags = AccessFlag.build(data.getU2At(OFFSET_OF_ACCESS_FLAGS));
        long OFFSET_OF_FIELDS_COUNT = OFFSET_OF_INTERFACES + (long)(2 * numInterfaces);
        long OFFSET_OF_FIELDS = OFFSET_OF_FIELDS_COUNT + 2L;
        int numFields = data.getU2At(OFFSET_OF_FIELDS_COUNT);
        List<Field> tmpFields = ListFactory.newList();
        long fieldsLength = ContiguousEntityFactory.build(data.getOffsetData(OFFSET_OF_FIELDS), numFields, tmpFields, new UnaryFunction<ByteData, Field>(){

            @Override
            public Field invoke(ByteData arg) {
                return new Field(arg, ClassFile.this.constantPool, cfv);
            }
        });
        this.fields = ListFactory.newList();
        LiteralRewriter rewriter = new LiteralRewriter(this.getClassType());
        for (Field tmpField : tmpFields) {
            this.fields.add(new ClassFileField(tmpField, rewriter));
        }
        long OFFSET_OF_METHODS_COUNT = OFFSET_OF_FIELDS + fieldsLength;
        long OFFSET_OF_METHODS = OFFSET_OF_METHODS_COUNT + 2L;
        int numMethods = data.getU2At(OFFSET_OF_METHODS_COUNT);
        ArrayList<Method> tmpMethods = new ArrayList<Method>(numMethods);
        long methodsLength = ContiguousEntityFactory.build(data.getOffsetData(OFFSET_OF_METHODS), numMethods, tmpMethods, new UnaryFunction<ByteData, Method>(){

            @Override
            public Method invoke(ByteData arg) {
                return new Method(arg, ClassFile.this, ClassFile.this.constantPool, dcCommonState, cfv);
            }
        });
        if (this.accessFlags.contains((Object)AccessFlag.ACC_STRICT)) {
            for (Method method : tmpMethods) {
                method.getAccessFlags().remove((Object)AccessFlagMethod.ACC_STRICT);
            }
        }
        if (!((Boolean)options.getOption(OptionsImpl.RENAME_ILLEGAL_IDENTS)).booleanValue()) {
            for (Method method : tmpMethods) {
                if (!IllegalIdentifierReplacement.isIllegalMethodName(method.getName())) continue;
                this.addComment(DecompilerComment.ILLEGAL_IDENTIFIERS);
                break;
            }
        }
        long OFFSET_OF_ATTRIBUTES_COUNT = OFFSET_OF_METHODS + methodsLength;
        long OFFSET_OF_ATTRIBUTES = OFFSET_OF_ATTRIBUTES_COUNT + 2L;
        int numAttributes = data.getU2At(OFFSET_OF_ATTRIBUTES_COUNT);
        ArrayList<Attribute> tmpAttributes = new ArrayList<Attribute>();
        tmpAttributes.ensureCapacity(numAttributes);
        ContiguousEntityFactory.build(data.getOffsetData(OFFSET_OF_ATTRIBUTES), numAttributes, tmpAttributes, AttributeFactory.getBuilder(this.constantPool, classFileVersion));
        this.attributes = new AttributeMap(tmpAttributes);
        AccessFlag.applyAttributes(this.attributes, this.accessFlags);
        if (this.attributes.containsKey("PermittedSubclasses")) {
            if (options.getOption(OptionsImpl.SEALED, classFileVersion).booleanValue()) {
                this.accessFlags.add(AccessFlag.ACC_FAKE_SEALED);
            } else {
                this.ensureDecompilerComments().addComment(DecompilerComment.CHECK_SEALED);
            }
        }
        this.isInnerClass = this.testIsInnerClass(dcCommonState);
        int superClassIndex = data.getU2At(OFFSET_OF_SUPER_CLASS);
        this.rawSuperClass = superClassIndex == 0 ? null : (ConstantPoolEntryClass)this.constantPool.getEntry(superClassIndex);
        this.classSignature = this.getSignature(this.constantPool, this.rawSuperClass, this.rawInterfaces);
        this.methods = tmpMethods;
        this.innerClassesByTypeInfo = new LinkedHashMap<JavaTypeInstance, Pair<InnerClassAttributeInfo, ClassFile>>();
        boolean isInterface = this.accessFlags.contains((Object)AccessFlag.ACC_INTERFACE);
        boolean isAnnotation = this.accessFlags.contains((Object)AccessFlag.ACC_ANNOTATION);
        boolean isModule = this.accessFlags.contains((Object)AccessFlag.ACC_MODULE);
        if (isInterface) {
            this.dumpHelper = isAnnotation ? new ClassFileDumperAnnotation(dcCommonState) : new ClassFileDumperInterface(dcCommonState);
        } else {
            if (isModule) {
                if (!this.methods.isEmpty()) {
                    this.addComment("Class file marked as module, but has methods! Treated as a class file");
                    isModule = false;
                }
                if (null == this.attributes.getByName("Module")) {
                    this.addComment("Class file marked as module, but no module attribute!");
                    isModule = false;
                }
            }
            this.dumpHelper = isModule ? new ClassFileDumperModule(dcCommonState) : new ClassFileDumperNormal(dcCommonState);
        }
        if (classFileVersion.before(ClassFileVersion.JAVA_6)) {
            boolean hasSignature = false;
            if (null != this.attributes.getByName("Signature")) {
                hasSignature = true;
            }
            if (!hasSignature) {
                for (Method method : this.methods) {
                    if (null == method.getSignatureAttribute()) continue;
                    hasSignature = true;
                    break;
                }
            }
            if (hasSignature) {
                this.addComment("This class specifies class file version " + classFileVersion + " but uses Java 6 signatures.  Assumed Java 6.");
                classFileVersion = ClassFileVersion.JAVA_6;
            }
        }
        if (classFileVersion.before(ClassFileVersion.JAVA_1_0)) {
            this.addComment(new DecompilerComment("Class file version " + classFileVersion + " predates " + ClassFileVersion.JAVA_1_0 + ", recompilation may lose compatibility!"));
        }
        this.classFileVersion = classFileVersion;
        AttributeInnerClasses attributeInnerClasses = (AttributeInnerClasses)this.attributes.getByName("InnerClasses");
        JavaRefTypeInstance typeInstance = (JavaRefTypeInstance)this.thisClass.getTypeInstance();
        if (typeInstance.getInnerClassHereInfo().isInnerClass()) {
            this.checkInnerClassAssumption(attributeInnerClasses, typeInstance, dcCommonState);
        }
        if (!((Boolean)options.getOption(OptionsImpl.RENAME_DUP_MEMBERS)).booleanValue() && MemberNameResolver.verifySingleClassNames(this)) {
            this.addComment(DecompilerComment.RENAME_MEMBERS);
        }
        if (options.getOption(OptionsImpl.ENUM_SUGAR, classFileVersion).booleanValue()) {
            this.fixConfusingEnumConstructors();
        }
        if (((Boolean)options.getOption(OptionsImpl.ELIDE_SCALA)).booleanValue()) {
            this.elideScala();
        }
        if (this.constantPool.isDynamicConstants()) {
            this.addComment(DecompilerComment.DYNAMIC_CONSTANTS);
        }
        if (dcCommonState.getVersionCollisions().contains(this.getClassType())) {
            this.addComment(DecompilerComment.MULTI_VERSION);
        }
        if ((renamedClass = dcCommonState.renamedTypeComment(this.getClassType().getRawName())) != null) {
            this.addComment(renamedClass);
        }
    }

    private void fixConfusingEnumConstructors() {
        if (this.testAccessFlag(AccessFlag.ACC_ENUM) && TypeConstants.ENUM.equals(this.getBaseClassType())) {
            List<Method> constructors = this.getConstructors();
            for (Method constructor : constructors) {
                MethodPrototype prototype = constructor.getMethodPrototype();
                prototype.unbreakEnumConstructor();
            }
        }
    }

    private void elideScala() {
        try {
            ClassFileField f = this.getFieldByName("serialVersionUID", RawJavaType.LONG);
            f.markHidden();
        }
        catch (Exception f) {
            // empty catch block
        }
        AttributeRuntimeVisibleAnnotations annotations = (AttributeRuntimeVisibleAnnotations)this.attributes.getByName("RuntimeVisibleAnnotations");
        if (annotations != null) {
            annotations.hide(TypeConstants.SCALA_SIGNATURE);
        }
    }

    private void checkInnerClassAssumption(AttributeInnerClasses attributeInnerClasses, JavaRefTypeInstance typeInstance, DCCommonState state) {
        if (attributeInnerClasses != null) {
            for (InnerClassAttributeInfo innerClassAttributeInfo : attributeInnerClasses.getInnerClassAttributeInfoList()) {
                if (!innerClassAttributeInfo.getInnerClassInfo().equals(typeInstance)) continue;
                return;
            }
        }
        if (state.getObfuscationMapping().providesInnerClassInfo()) {
            return;
        }
        typeInstance.markNotInner();
    }

    public String getUsePath() {
        return this.usePath;
    }

    public boolean isInterface() {
        return this.accessFlags.contains((Object)AccessFlag.ACC_INTERFACE);
    }

    public void addComment(DecompilerComment comment) {
        if (this.decompilerComments == null) {
            this.decompilerComments = new DecompilerComments();
        }
        this.decompilerComments.addComment(comment);
    }

    public void addComment(String comment) {
        this.ensureDecompilerComments();
        this.decompilerComments.addComment(comment);
    }

    private void addComment(String comment, Exception e) {
        this.addComment(new DecompilerComment(comment, e));
    }

    public DecompilerComments getNullableDecompilerComments() {
        return this.decompilerComments;
    }

    public DecompilerComments ensureDecompilerComments() {
        if (this.decompilerComments == null) {
            this.decompilerComments = new DecompilerComments();
        }
        return this.decompilerComments;
    }

    public FakeMethod addFakeMethod(Object key, String nameHint, UnaryFunction<String, FakeMethod> methodFactory) {
        if (this.fakeMethods == null) {
            this.fakeMethods = new FakeMethods();
        }
        return this.fakeMethods.add(key, nameHint, methodFactory);
    }

    public List<JavaTypeInstance> getAllClassTypes() {
        List<JavaTypeInstance> res = ListFactory.newList();
        this.getAllClassTypes(res);
        return res;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (this.thisClass != null) {
            collector.collect(this.thisClass.getTypeInstance());
        }
        collector.collectFrom(this.classSignature);
        for (ClassFileField classFileField : this.fields) {
            collector.collectFrom(classFileField.getField());
            collector.collectFrom(classFileField.getInitialValue());
        }
        collector.collectFrom(this.methods);
        if (this.fakeMethods != null) {
            collector.collectFrom(this.fakeMethods);
        }
        for (Map.Entry entry : this.innerClassesByTypeInfo.entrySet()) {
            collector.collect((JavaTypeInstance)entry.getKey());
            ClassFile innerClassFile = (ClassFile)((Pair)entry.getValue()).getSecond();
            innerClassFile.collectTypeUsages(collector);
        }
        collector.collectFrom(this.dumpHelper);
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeVisibleAnnotations"));
        collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("RuntimeInvisibleAnnotations"));
        if (this.accessFlags.contains((Object)AccessFlag.ACC_FAKE_SEALED)) {
            collector.collectFromT((TypeUsageCollectable)this.attributes.getByName("PermittedSubclasses"));
        }
    }

    private void getAllClassTypes(List<JavaTypeInstance> tgt) {
        tgt.add(this.getClassType());
        for (Pair<InnerClassAttributeInfo, ClassFile> pair : this.innerClassesByTypeInfo.values()) {
            pair.getSecond().getAllClassTypes(tgt);
        }
    }

    public void setDumpHelper(ClassFileDumper dumpHelper) {
        this.dumpHelper = dumpHelper;
    }

    public void markHiddenInnerClass() {
        this.hiddenInnerClass = true;
    }

    public ClassFileVersion getClassFileVersion() {
        return this.classFileVersion;
    }

    public boolean isInnerClass() {
        if (this.isInnerClass) {
            return true;
        }
        if (this.thisClass == null) {
            return false;
        }
        return this.thisClass.getTypeInstance().getInnerClassHereInfo().isInnerClass();
    }

    public ConstantPool getConstantPool() {
        return this.constantPool;
    }

    public boolean testAccessFlag(AccessFlag accessFlag) {
        return this.accessFlags.contains((Object)accessFlag);
    }

    public boolean hasFormalTypeParameters() {
        List<FormalTypeParameter> formalTypeParameters = this.classSignature.getFormalTypeParameters();
        return formalTypeParameters != null && !formalTypeParameters.isEmpty();
    }

    private void ensureFieldsByName() {
        if (this.fieldsByName == null) {
            this.calculateFieldsByName();
        }
    }

    public boolean hasLocalField(String name) {
        this.ensureFieldsByName();
        return this.fieldsByName.containsKey(name);
    }

    public boolean hasAccessibleField(String name, JavaRefTypeInstance maybeCaller) {
        this.ensureFieldsByName();
        Map<JavaTypeInstance, ClassFileField> fields = this.fieldsByName.get(name);
        if (fields == null) {
            JavaTypeInstance baseClassType = this.getBaseClassType();
            if (baseClassType == null) {
                return false;
            }
            if ((baseClassType = baseClassType.getDeGenerifiedType()) instanceof JavaRefTypeInstance) {
                ClassFile baseClass = ((JavaRefTypeInstance)baseClassType).getClassFile();
                if (baseClass == null) {
                    return false;
                }
                return baseClass.hasAccessibleField(name, maybeCaller);
            }
            return false;
        }
        for (ClassFileField field : fields.values()) {
            if (!field.getField().isAccessibleFrom(maybeCaller, this)) continue;
            return true;
        }
        return false;
    }

    public ClassFileField getFieldByName(String name, JavaTypeInstance type) throws NoSuchFieldException {
        Map<JavaTypeInstance, ClassFileField> fieldsByType;
        if (this.fieldsByName == null) {
            this.calculateFieldsByName();
        }
        if ((fieldsByType = this.fieldsByName.get(name)) == null || fieldsByType.isEmpty()) {
            throw new NoSuchFieldException(name);
        }
        ClassFileField field = fieldsByType.get(type);
        if (field == null) {
            return fieldsByType.values().iterator().next();
        }
        return field;
    }

    private void calculateFieldsByName() {
        int smallMemberThreshold;
        Options options = this.constantPool.getDCCommonState().getOptions();
        boolean testIllegal = (Boolean)options.getOption(OptionsImpl.RENAME_ILLEGAL_IDENTS) == false;
        boolean illegal = false;
        this.fieldsByName = MapFactory.newMap();
        if (testIllegal) {
            for (ClassFileField field : this.fields) {
                String rawFieldName = field.getRawFieldName();
                if (!IllegalIdentifierReplacement.isIllegal(rawFieldName)) continue;
                illegal = true;
                break;
            }
        }
        boolean renameSmallMembers = (smallMemberThreshold = ((Integer)options.getOption(OptionsImpl.RENAME_SMALL_MEMBERS)).intValue()) > 0;
        for (ClassFileField field : this.fields) {
            String fieldName = field.getFieldName();
            JavaTypeInstance fieldType = field.getField().getJavaTypeInstance();
            Map<JavaTypeInstance, ClassFileField> perNameMap = this.fieldsByName.get(fieldName);
            if (perNameMap == null) {
                perNameMap = MapFactory.newOrderedMap();
                this.fieldsByName.put(fieldName, perNameMap);
            }
            perNameMap.put(fieldType, field);
            if (!renameSmallMembers || fieldName.length() > smallMemberThreshold) continue;
            field.getField().setDisambiguate();
        }
        boolean warnAmbig = false;
        for (Map<JavaTypeInstance, ClassFileField> typeMap : this.fieldsByName.values()) {
            if (typeMap.size() <= 1) continue;
            if (((Boolean)this.constantPool.getDCCommonState().getOptions().getOption(OptionsImpl.RENAME_DUP_MEMBERS)).booleanValue()) {
                for (ClassFileField field : typeMap.values()) {
                    field.getField().setDisambiguate();
                }
                continue;
            }
            warnAmbig = true;
        }
        if (warnAmbig) {
            this.addComment(DecompilerComment.RENAME_MEMBERS);
        }
        if (illegal) {
            this.addComment(DecompilerComment.ILLEGAL_IDENTIFIERS);
        }
    }

    public List<ClassFileField> getFields() {
        return this.fields;
    }

    public List<Method> getMethods() {
        return this.methods;
    }

    private List<Method> getMethodsWithMatchingName(final MethodPrototype prototype) {
        return Functional.filter(this.methods, new Predicate<Method>(){

            @Override
            public boolean test(Method in) {
                return in.getName().equals(prototype.getName());
            }
        });
    }

    private void collectMethods(MethodPrototype prototype, List<Method> tgt, Set<JavaTypeInstance> seen) {
        tgt.addAll(this.getMethodsWithMatchingName(prototype));
        if (this.classSignature == null) {
            return;
        }
        this.collectTypeMethods(prototype, tgt, seen, this.classSignature.getSuperClass());
        for (JavaTypeInstance intf : this.classSignature.getInterfaces()) {
            this.collectTypeMethods(prototype, tgt, seen, intf);
        }
    }

    private void collectTypeMethods(MethodPrototype prototype, List<Method> tgt, Set<JavaTypeInstance> seen, JavaTypeInstance clazz) {
        if (clazz == null) {
            return;
        }
        if (!((clazz = clazz.getDeGenerifiedType()) instanceof JavaRefTypeInstance)) {
            return;
        }
        if (!seen.add(clazz)) {
            return;
        }
        ClassFile classFile = this.constantPool.getDCCommonState().getClassFileOrNull(clazz);
        if (classFile != null) {
            classFile.collectMethods(prototype, tgt, seen);
        }
    }

    public OverloadMethodSet getOverloadMethodSet(MethodPrototype prototype) {
        OverloadMethodSetCache cache = this.constantPool.getDCCommonState().getOverloadMethodSetCache();
        OverloadMethodSet res = cache.get(this, prototype);
        if (res == null) {
            res = this.getOverloadMethodSetInner(prototype);
            cache.set(this, prototype, res);
        }
        return res;
    }

    private OverloadMethodSet getOverloadMethodSetInner(MethodPrototype prototype) {
        final JavaRefTypeInstance thiz = this.getRefClassType();
        final boolean isInstance = prototype.isInstanceMethod();
        final int numArgs = prototype.getArgs().size();
        List<Method> named = ListFactory.newList();
        this.collectMethods(prototype, named, SetFactory.<JavaTypeInstance>newIdentitySet());
        final boolean isVarArgs = prototype.isVarArgs();
        named = Functional.filter(named, new Predicate<Method>(){

            @Override
            public boolean test(Method in) {
                MethodPrototype other = in.getMethodPrototype();
                if (other.isInstanceMethod() != isInstance) {
                    return false;
                }
                if (!in.isVisibleTo(thiz)) {
                    return false;
                }
                boolean otherIsVarargs = other.isVarArgs();
                if (isVarArgs) {
                    if (otherIsVarargs) {
                        return true;
                    }
                    return other.getArgs().size() >= numArgs;
                }
                if (otherIsVarargs) {
                    return other.getArgs().size() <= numArgs;
                }
                return other.getArgs().size() == numArgs;
            }
        });
        List<MethodPrototype> prototypes = Functional.map(named, new UnaryFunction<Method, MethodPrototype>(){

            @Override
            public MethodPrototype invoke(Method arg) {
                return arg.getMethodPrototype();
            }
        });
        List<MethodPrototype> out = ListFactory.newList();
        Set matched = SetFactory.newSet();
        out.add(prototype);
        matched.add(prototype.getComparableString());
        for (MethodPrototype other : prototypes) {
            if (!matched.add(other.getComparableString())) continue;
            out.add(other);
        }
        return new OverloadMethodSet(this, prototype, out);
    }

    public Method getMethodByPrototype(MethodPrototype prototype) throws NoSuchMethodException {
        Method methodMatch = this.getMethodByPrototypeOrNull(prototype);
        if (methodMatch != null) {
            return methodMatch;
        }
        throw new NoSuchMethodException();
    }

    public Method getMethodByPrototypeOrNull(MethodPrototype prototype) {
        List<Method> named = this.getMethodsWithMatchingName(prototype);
        Method methodMatch = null;
        for (Method method : named) {
            MethodPrototype tgt = method.getMethodPrototype();
            if (tgt.equalsMatch(prototype)) {
                return method;
            }
            if (!tgt.equalsGeneric(prototype)) continue;
            methodMatch = method;
        }
        return methodMatch;
    }

    private Method getAccessibleMethodByPrototype(MethodPrototype prototype, GenericTypeBinder binder, JavaRefTypeInstance accessor) throws NoSuchMethodException {
        List<Method> named = this.getMethodsWithMatchingName(prototype);
        Method methodMatch = null;
        for (Method method : named) {
            if (!method.isVisibleTo(accessor)) continue;
            MethodPrototype tgt = method.getMethodPrototype();
            if (tgt.equalsMatch(prototype)) {
                return method;
            }
            if (binder == null || !tgt.equalsGeneric(prototype, binder)) continue;
            methodMatch = method;
        }
        if (methodMatch != null) {
            return methodMatch;
        }
        throw new NoSuchMethodException();
    }

    public Method getSingleMethodByNameOrNull(String name) {
        List<Method> methodList = this.getMethodsByNameOrNull(name);
        if (methodList == null || methodList.size() != 1) {
            return null;
        }
        return methodList.get(0);
    }

    public List<Method> getMethodsByNameOrNull(String name) {
        if (this.methodsByName == null) {
            this.methodsByName = MapFactory.newMap();
            for (Method method : this.methods) {
                List<Method> list = this.methodsByName.get(method.getName());
                if (list == null) {
                    list = ListFactory.newList();
                    this.methodsByName.put(method.getName(), list);
                }
                list.add(method);
            }
        }
        return this.methodsByName.get(name);
    }

    public List<Method> getMethodByName(String name) throws NoSuchMethodException {
        List<Method> methods = this.getMethodsByNameOrNull(name);
        if (methods == null) {
            throw new NoSuchMethodException(name);
        }
        return methods;
    }

    public List<Method> getConstructors() {
        List<Method> res = ListFactory.newList();
        for (Method method : this.methods) {
            if (!method.isConstructor()) continue;
            res.add(method);
        }
        return res;
    }

    public AttributeBootstrapMethods getBootstrapMethods() {
        return (AttributeBootstrapMethods)this.attributes.getByName("BootstrapMethods");
    }

    public ConstantPoolEntryClass getThisClassConstpoolEntry() {
        return this.thisClass;
    }

    private boolean isInferredAnonymousStatic(DCCommonState state, JavaTypeInstance thisType, JavaTypeInstance innerType) {
        if (!innerType.getInnerClassHereInfo().isAnonymousClass()) {
            return false;
        }
        boolean j8orLater = this.classFileVersion.equalOrLater(ClassFileVersion.JAVA_8);
        if (!j8orLater) {
            return false;
        }
        JavaRefTypeInstance containing = thisType.getInnerClassHereInfo().getOuterClass();
        ClassFile containingClassFile = containing.getClassFile();
        if (containingClassFile == null) {
            return false;
        }
        if (containingClassFile.getAccessFlags().contains((Object)AccessFlag.ACC_STATIC)) {
            return true;
        }
        AttributeEnclosingMethod encloser = (AttributeEnclosingMethod)this.attributes.getByName("EnclosingMethod");
        if (encloser == null) {
            return false;
        }
        int classIndex = encloser.getClassIndex();
        if (classIndex == 0) {
            return false;
        }
        ConstantPoolEntryClass encloserClass = this.constantPool.getClassEntry(classIndex);
        JavaTypeInstance encloserType = encloserClass.getTypeInstance();
        if (encloserType != containing) {
            return false;
        }
        int methodIndex = encloser.getMethodIndex();
        if (methodIndex == 0) {
            return false;
        }
        ConstantPoolEntryNameAndType nameAndType = this.constantPool.getNameAndTypeEntry(methodIndex);
        ConstantPoolEntryUTF8 descriptor = nameAndType.getDescriptor();
        String name = nameAndType.getName().getValue();
        VariableNamerDefault fakeNamer = new VariableNamerDefault();
        MethodPrototype basePrototype = ConstantPoolUtils.parseJavaMethodPrototype(state, null, containing, name, false, Method.MethodConstructor.NOT, descriptor, this.constantPool, false, false, fakeNamer, descriptor.getValue());
        try {
            Method m = containingClassFile.getMethodByPrototype(basePrototype);
            if (m.getAccessFlags().contains((Object)AccessFlagMethod.ACC_STATIC)) {
                return true;
            }
        }
        catch (NoSuchMethodException noSuchMethodException) {
            // empty catch block
        }
        return false;
    }

    private boolean testIsInnerClass(DCCommonState dcCommonState) {
        List<InnerClassAttributeInfo> innerClassAttributeInfoList = this.getInnerClassAttributeInfos(dcCommonState);
        if (innerClassAttributeInfoList == null) {
            return false;
        }
        JavaTypeInstance thisType = this.thisClass.getTypeInstance();
        for (InnerClassAttributeInfo innerClassAttributeInfo : innerClassAttributeInfoList) {
            JavaTypeInstance innerType = innerClassAttributeInfo.getInnerClassInfo();
            if (innerType != thisType) continue;
            return true;
        }
        return false;
    }

    public void loadInnerClasses(DCCommonState dcCommonState) {
        List<InnerClassAttributeInfo> innerClassAttributeInfoList = this.getInnerClassAttributeInfos(dcCommonState);
        if (innerClassAttributeInfoList == null) {
            return;
        }
        JavaTypeInstance thisType = this.thisClass.getTypeInstance();
        for (InnerClassAttributeInfo innerClassAttributeInfo : innerClassAttributeInfoList) {
            JavaTypeInstance innerType = innerClassAttributeInfo.getInnerClassInfo();
            if (innerType == null) continue;
            if (innerType == thisType) {
                this.accessFlags.addAll(innerClassAttributeInfo.getAccessFlags());
                if (!this.accessFlags.contains((Object)AccessFlag.ACC_STATIC) && this.isInferredAnonymousStatic(dcCommonState, thisType, innerType)) {
                    this.accessFlags.add(AccessFlag.ACC_STATIC);
                }
                this.sanitiseAccessPermissions();
            }
            if (!innerType.getInnerClassHereInfo().isInnerClassOf(thisType)) continue;
            try {
                ClassFile innerClass = dcCommonState.getClassFile(innerType);
                innerClass.loadInnerClasses(dcCommonState);
                this.innerClassesByTypeInfo.put(innerType, new Pair<InnerClassAttributeInfo, ClassFile>(innerClassAttributeInfo, innerClass));
            }
            catch (CannotLoadClassException cannotLoadClassException) {}
        }
    }

    private List<InnerClassAttributeInfo> getInnerClassAttributeInfos(DCCommonState state) {
        List<InnerClassAttributeInfo> innerClassAttributeInfoList;
        AttributeInnerClasses attributeInnerClasses = (AttributeInnerClasses)this.attributes.getByName("InnerClasses");
        List<InnerClassAttributeInfo> list = innerClassAttributeInfoList = attributeInnerClasses == null ? null : attributeInnerClasses.getInnerClassAttributeInfoList();
        if (innerClassAttributeInfoList != null) {
            return innerClassAttributeInfoList;
        }
        return state.getObfuscationMapping().getInnerClassInfo(this.getClassType());
    }

    private void analyseInnerClassesPass1(DCCommonState state) {
        if (this.innerClassesByTypeInfo == null) {
            return;
        }
        for (Pair<InnerClassAttributeInfo, ClassFile> innerClassInfoClassFilePair : this.innerClassesByTypeInfo.values()) {
            ClassFile classFile = innerClassInfoClassFilePair.getSecond();
            classFile.analyseMid(state);
        }
    }

    private void analysePassOuterFirst(UnaryProcedure<ClassFile> fn) {
        try {
            fn.call(this);
        }
        catch (RuntimeException e) {
            this.addComment("Exception performing whole class analysis ignored.", e);
        }
        if (this.innerClassesByTypeInfo == null) {
            return;
        }
        for (Pair<InnerClassAttributeInfo, ClassFile> innerClassInfoClassFilePair : this.innerClassesByTypeInfo.values()) {
            ClassFile classFile = innerClassInfoClassFilePair.getSecond();
            classFile.analysePassOuterFirst(fn);
        }
    }

    public void analyseTop(final DCCommonState dcCommonState, final TypeUsageCollectingDumper typeUsageCollectingDumper) {
        this.analyseMid(dcCommonState);
        this.analysePassOuterFirst(new UnaryProcedure<ClassFile>(){

            @Override
            public void call(ClassFile arg) {
                CodeAnalyserWholeClass.wholeClassAnalysisPass2(arg, dcCommonState);
            }
        });
        this.dump(typeUsageCollectingDumper);
        this.analysePassOuterFirst(new UnaryProcedure<ClassFile>(){

            @Override
            public void call(ClassFile arg) {
                CodeAnalyserWholeClass.wholeClassAnalysisPass3(arg, dcCommonState, typeUsageCollectingDumper);
            }
        });
    }

    private void analyseSyntheticTags(Method method, Options options) {
        try {
            boolean isResourceRelease;
            Op04StructuredStatement code = method.getAnalysis();
            if (code == null) {
                return;
            }
            if (options.getOption(OptionsImpl.REWRITE_TRY_RESOURCES, this.getClassFileVersion()).booleanValue() && (isResourceRelease = Op04StructuredStatement.isTryWithResourceSynthetic(method, code))) {
                method.getAccessFlags().add(AccessFlagMethod.ACC_FAKE_END_RESOURCE);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void analyseOverrides() {
        try {
            BindingSuperContainer bindingSuperContainer = this.getBindingSupers();
            Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> boundSupers = bindingSuperContainer.getBoundSuperClasses();
            List<Triplet> bindTesters = ListFactory.newList();
            for (Map.Entry<JavaRefTypeInstance, JavaGenericRefTypeInstance> entry : boundSupers.entrySet()) {
                JavaRefTypeInstance superC = entry.getKey();
                if (superC.equals(this.getClassType())) continue;
                ClassFile superClsFile = null;
                try {
                    superClsFile = superC.getClassFile();
                }
                catch (CannotLoadClassException cannotLoadClassException) {
                    // empty catch block
                }
                if (superClsFile == null || superClsFile == this) continue;
                JavaGenericRefTypeInstance boundSuperC = entry.getValue();
                GenericTypeBinder binder = null;
                if (boundSuperC != null) {
                    binder = superClsFile.getGenericTypeBinder(boundSuperC);
                }
                bindTesters.add(Triplet.make(superC, superClsFile, binder));
            }
            for (Method method : this.methods) {
                if (method.isConstructor() || method.testAccessFlag(AccessFlagMethod.ACC_STATIC)) continue;
                MethodPrototype prototype = method.getMethodPrototype();
                Method baseMethod = null;
                for (Triplet bindTester : bindTesters) {
                    ClassFile classFile = (ClassFile)bindTester.getSecond();
                    GenericTypeBinder genericTypeBinder = (GenericTypeBinder)bindTester.getThird();
                    try {
                        baseMethod = classFile.getAccessibleMethodByPrototype(prototype, genericTypeBinder, (JavaRefTypeInstance)this.getClassType().getDeGenerifiedType());
                    }
                    catch (NoSuchMethodException noSuchMethodException) {
                        // empty catch block
                    }
                    if (baseMethod == null) continue;
                    break;
                }
                if (baseMethod == null) continue;
                method.markOverride();
            }
        }
        catch (RuntimeException e) {
            this.addComment("Failed to analyse overrides", e);
        }
    }

    private void analyseMid(DCCommonState state) {
        Options options = state.getOptions();
        if (this.begunAnalysis) {
            return;
        }
        this.begunAnalysis = true;
        if (((Boolean)options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES)).booleanValue()) {
            this.analyseInnerClassesPass1(state);
        }
        Pair<List<Method>, List<Method>> partition = Functional.partition(this.methods, new Predicate<Method>(){

            @Override
            public boolean test(Method x) {
                return x.getAccessFlags().contains((Object)AccessFlagMethod.ACC_SYNTHETIC);
            }
        });
        for (Method method : partition.getFirst()) {
            method.analyse();
            this.analyseSyntheticTags(method, options);
        }
        for (Method method : partition.getSecond()) {
            method.analyse();
        }
        try {
            if (options.getOption(OptionsImpl.OVERRIDES, this.classFileVersion).booleanValue()) {
                this.analyseOverrides();
            }
            CodeAnalyserWholeClass.wholeClassAnalysisPass1(this, state);
        }
        catch (RuntimeException e) {
            this.addComment(DecompilerComment.WHOLE_CLASS_EXCEPTION);
        }
    }

    public void releaseCode() {
        if (this.isInnerClass) {
            return;
        }
        for (Method method : this.methods) {
            method.releaseCode();
        }
    }

    public JavaTypeInstance getClassType() {
        return this.thisClass.getTypeInstance();
    }

    public JavaRefTypeInstance getRefClassType() {
        return (JavaRefTypeInstance)this.thisClass.getTypeInstance();
    }

    public JavaTypeInstance getBaseClassType() {
        return this.classSignature.getSuperClass();
    }

    public ClassSignature getClassSignature() {
        return this.classSignature;
    }

    public Set<AccessFlag> getAccessFlags() {
        return this.accessFlags;
    }

    private void sanitiseAccessPermissions() {
        if (this.accessFlags.contains((Object)AccessFlag.ACC_PRIVATE)) {
            this.accessFlags.remove((Object)AccessFlag.ACC_PROTECTED);
            this.accessFlags.remove((Object)AccessFlag.ACC_PUBLIC);
        } else if (this.accessFlags.contains((Object)AccessFlag.ACC_PROTECTED)) {
            this.accessFlags.remove((Object)AccessFlag.ACC_PUBLIC);
        }
    }

    private ClassSignature getSignature(ConstantPool cp, ConstantPoolEntryClass rawSuperClass, List<ConstantPoolEntryClass> rawInterfaces) {
        block5: {
            AttributeSignature signatureAttribute = (AttributeSignature)this.attributes.getByName("Signature");
            if (signatureAttribute == null) break block5;
            try {
                JavaTypeInstance rawSuperType;
                ClassSignature fromAttr;
                block7: {
                    block6: {
                        fromAttr = ConstantPoolUtils.parseClassSignature(signatureAttribute.getSignature(), cp);
                        if (rawSuperClass == null) break block6;
                        rawSuperType = rawSuperClass.getTypeInstance();
                        JavaTypeInstance fromAttrType = fromAttr.getSuperClass().getDeGenerifiedType();
                        if (!fromAttrType.equals(rawSuperType)) break block7;
                    }
                    return fromAttr;
                }
                this.addComment("Signature claims super is " + fromAttr.getSuperClass().getRawName() + ", not " + rawSuperType.getRawName() + " - discarding signature.");
            }
            catch (Exception fromAttr) {
                // empty catch block
            }
        }
        List<JavaTypeInstance> interfaces = ListFactory.newList();
        for (ConstantPoolEntryClass rawInterface : rawInterfaces) {
            interfaces.add(rawInterface.getTypeInstance());
        }
        return new ClassSignature(null, rawSuperClass == null ? null : rawSuperClass.getTypeInstance(), interfaces);
    }

    public void dumpNamedInnerClasses(Dumper d) {
        if (this.innerClassesByTypeInfo == null || this.innerClassesByTypeInfo.isEmpty()) {
            return;
        }
        for (Pair<InnerClassAttributeInfo, ClassFile> innerClassEntry : this.innerClassesByTypeInfo.values()) {
            InnerClassInfo innerClassInfo = innerClassEntry.getFirst().getInnerClassInfo().getInnerClassHereInfo();
            if (innerClassInfo.isSyntheticFriendClass() || innerClassInfo.isMethodScopedClass()) continue;
            ClassFile classFile = innerClassEntry.getSecond();
            if (classFile.hiddenInnerClass) continue;
            TypeUsageInformation typeUsageInformation = d.getTypeUsageInformation();
            InnerClassTypeUsageInformation innerclassTypeUsageInformation = new InnerClassTypeUsageInformation(typeUsageInformation, (JavaRefTypeInstance)classFile.getClassType());
            d.newln();
            Dumper d2 = d.withTypeUsageInformation(innerclassTypeUsageInformation);
            classFile.dumpHelper.dump(classFile, ClassFileDumper.InnerClassDumpType.INNER_CLASS, d2);
        }
    }

    @Override
    public Dumper dump(Dumper d) {
        return this.dumpHelper.dump(this, ClassFileDumper.InnerClassDumpType.NOT, d);
    }

    public Dumper dumpAsInlineClass(Dumper d) {
        return this.dumpHelper.dump(this, ClassFileDumper.InnerClassDumpType.INLINE_CLASS, d);
    }

    public String getFilePath() {
        return this.thisClass.getFilePath();
    }

    public String toString() {
        return this.thisClass.getTextPath();
    }

    private static void getFormalParametersText(ClassSignature signature, TypeAnnotationHelper ah, UnaryFunction<Integer, Predicate<AnnotationTableTypeEntry>> typeAnnPredicateFact, UnaryFunction<Integer, Predicate<AnnotationTableTypeEntry>> typeBoundAnnPredicateFact, Dumper d) {
        List<FormalTypeParameter> formalTypeParameters = signature.getFormalTypeParameters();
        if (formalTypeParameters == null || formalTypeParameters.isEmpty()) {
            return;
        }
        d.separator("<");
        boolean first = true;
        int len = formalTypeParameters.size();
        for (int idx = 0; idx < len; ++idx) {
            FormalTypeParameter formalTypeParameter = formalTypeParameters.get(idx);
            first = StringUtils.comma(first, d);
            if (ah != null) {
                List<AnnotationTableTypeEntry> typeAnnotations = Functional.filter(ah.getEntries(), typeAnnPredicateFact.invoke(idx));
                List<AnnotationTableTypeEntry> typeBoundAnnotations = Functional.filter(ah.getEntries(), typeBoundAnnPredicateFact.invoke(idx));
                if (!typeAnnotations.isEmpty() || !typeBoundAnnotations.isEmpty()) {
                    formalTypeParameter.dump(d, typeAnnotations, typeBoundAnnotations);
                    continue;
                }
            }
            formalTypeParameter.dump(d);
        }
        d.print(">");
    }

    public void dumpReceiverClassIdentity(List<AnnotationTableTypeEntry> recieverAnnotations, Dumper d) {
        Pair<List<AnnotationTableTypeEntry>, List<AnnotationTableTypeEntry>> split = Functional.partition(recieverAnnotations, new Predicate<AnnotationTableTypeEntry>(){

            @Override
            public boolean test(AnnotationTableTypeEntry in) {
                return in.getTypePath().segments.isEmpty();
            }
        });
        List<AnnotationTableTypeEntry> pre = split.getFirst();
        List<AnnotationTableTypeEntry> type = split.getSecond();
        if (!pre.isEmpty()) {
            pre.get(0).dump(d);
            d.print(" ");
        }
        JavaTypeInstance t = this.classSignature.getThisGeneralTypeClass(this.getClassType(), this.constantPool);
        JavaAnnotatedTypeInstance jat = t.getAnnotatedInstance();
        DecompilerComments comments = new DecompilerComments();
        TypeAnnotationHelper.apply(jat, type, comments);
        d.dump(comments);
        d.dump(jat);
    }

    public void dumpPermitted(Dumper d) {
        if (this.accessFlags.contains((Object)AccessFlag.ACC_FAKE_SEALED)) {
            AttributePermittedSubclasses permitted = (AttributePermittedSubclasses)this.attributes.getByName("PermittedSubclasses");
            if (permitted == null) {
                return;
            }
            List<JavaTypeInstance> permittedClasses = permitted.getPermitted();
            boolean allInner = true;
            JavaTypeInstance classType = this.getClassType();
            for (JavaTypeInstance type : permittedClasses) {
                if (type.getInnerClassHereInfo().isInnerClassOf(classType)) continue;
                allInner = false;
                break;
            }
            if (allInner) {
                return;
            }
            d.print("permits ");
            boolean first = true;
            for (JavaTypeInstance type : permittedClasses) {
                first = StringUtils.comma(first, d);
                d.dump(type);
            }
            d.newln();
        }
    }

    public void dumpClassIdentity(Dumper d) {
        d.dump(this.getThisClassConstpoolEntry().getTypeInstance());
        TypeAnnotationHelper typeAnnotations = TypeAnnotationHelper.create(this.attributes, TypeAnnotationEntryValue.type_generic_class_interface, TypeAnnotationEntryValue.type_type_parameter_class_interface);
        UnaryFunction<Integer, Predicate<AnnotationTableTypeEntry>> typeAnnPredicateFact = new UnaryFunction<Integer, Predicate<AnnotationTableTypeEntry>>(){

            @Override
            public Predicate<AnnotationTableTypeEntry> invoke(final Integer arg) {
                return new Predicate<AnnotationTableTypeEntry>(){

                    @Override
                    public boolean test(AnnotationTableTypeEntry in) {
                        if (in.getValue() != TypeAnnotationEntryValue.type_generic_class_interface) {
                            return false;
                        }
                        return ((TypeAnnotationTargetInfo.TypeAnnotationParameterTarget)in.getTargetInfo()).getIndex() == arg.intValue();
                    }
                };
            }
        };
        UnaryFunction<Integer, Predicate<AnnotationTableTypeEntry>> typeBoundAnnPredicateFact = new UnaryFunction<Integer, Predicate<AnnotationTableTypeEntry>>(){

            @Override
            public Predicate<AnnotationTableTypeEntry> invoke(final Integer arg) {
                return new Predicate<AnnotationTableTypeEntry>(){

                    @Override
                    public boolean test(AnnotationTableTypeEntry in) {
                        if (in.getValue() != TypeAnnotationEntryValue.type_type_parameter_class_interface) {
                            return false;
                        }
                        return ((TypeAnnotationTargetInfo.TypeAnnotationParameterBoundTarget)in.getTargetInfo()).getIndex() == arg.intValue();
                    }
                };
            }
        };
        ClassFile.getFormalParametersText(this.getClassSignature(), typeAnnotations, typeAnnPredicateFact, typeBoundAnnPredicateFact, d);
    }

    public BindingSuperContainer getBindingSupers() {
        if (this.boundSuperClasses == null) {
            this.boundSuperClasses = this.generateBoundSuperClasses();
        }
        return this.boundSuperClasses;
    }

    private BindingSuperContainer generateBoundSuperClasses() {
        GenericTypeBinder genericTypeBinder;
        BoundSuperCollector boundSuperCollector = new BoundSuperCollector(this);
        JavaTypeInstance thisType = this.getClassSignature().getThisGeneralTypeClass(this.getClassType(), this.getConstantPool());
        if (thisType instanceof JavaGenericRefTypeInstance) {
            JavaGenericRefTypeInstance genericThisType = (JavaGenericRefTypeInstance)thisType;
            genericTypeBinder = GenericTypeBinder.buildIdentityBindings(genericThisType);
            boundSuperCollector.collect(genericThisType, BindingSuperContainer.Route.IDENTITY);
        } else {
            genericTypeBinder = null;
            boundSuperCollector.collect((JavaRefTypeInstance)thisType, BindingSuperContainer.Route.IDENTITY);
        }
        JavaTypeInstance base = this.classSignature.getSuperClass();
        if (base == null) {
            return new BindingSuperContainer(this, new HashMap<JavaRefTypeInstance, JavaGenericRefTypeInstance>(), new HashMap<JavaRefTypeInstance, BindingSuperContainer.Route>());
        }
        this.getBoundSuperClasses2(base, genericTypeBinder, boundSuperCollector, BindingSuperContainer.Route.EXTENSION, SetFactory.<JavaTypeInstance>newSet());
        for (JavaTypeInstance interfaceBase : this.classSignature.getInterfaces()) {
            this.getBoundSuperClasses2(interfaceBase, genericTypeBinder, boundSuperCollector, BindingSuperContainer.Route.INTERFACE, SetFactory.<JavaTypeInstance>newSet());
        }
        return boundSuperCollector.getBoundSupers();
    }

    private void getBoundSuperClasses(JavaTypeInstance boundGeneric, BoundSuperCollector boundSuperCollector, BindingSuperContainer.Route route, Set<JavaTypeInstance> seen) {
        GenericTypeBinder genericTypeBinder;
        JavaTypeInstance thisType = this.getClassSignature().getThisGeneralTypeClass(this.getClassType(), this.getConstantPool());
        if (!(thisType instanceof JavaGenericRefTypeInstance)) {
            genericTypeBinder = null;
        } else {
            JavaGenericRefTypeInstance genericThisType = (JavaGenericRefTypeInstance)thisType;
            genericTypeBinder = boundGeneric instanceof JavaGenericRefTypeInstance ? GenericTypeBinder.extractBindings(genericThisType, boundGeneric) : null;
        }
        JavaTypeInstance base = this.classSignature.getSuperClass();
        if (base == null) {
            return;
        }
        this.getBoundSuperClasses2(base, genericTypeBinder, boundSuperCollector, route, SetFactory.newSet(seen));
        for (JavaTypeInstance interfaceBase : this.classSignature.getInterfaces()) {
            this.getBoundSuperClasses2(interfaceBase, genericTypeBinder, boundSuperCollector, BindingSuperContainer.Route.INTERFACE, SetFactory.newSet(seen));
        }
    }

    public GenericTypeBinder getGenericTypeBinder(JavaGenericRefTypeInstance boundGeneric) {
        JavaTypeInstance thisType = this.getClassSignature().getThisGeneralTypeClass(this.getClassType(), this.getConstantPool());
        if (!(thisType instanceof JavaGenericRefTypeInstance)) {
            return null;
        }
        JavaGenericRefTypeInstance genericThisType = (JavaGenericRefTypeInstance)thisType;
        return GenericTypeBinder.extractBindings(genericThisType, boundGeneric);
    }

    private void getBoundSuperClasses2(JavaTypeInstance base, GenericTypeBinder genericTypeBinder, BoundSuperCollector boundSuperCollector, BindingSuperContainer.Route route, Set<JavaTypeInstance> seen) {
        if (seen.contains(base)) {
            return;
        }
        seen.add(base);
        if (base instanceof JavaRefTypeInstance) {
            boundSuperCollector.collect((JavaRefTypeInstance)base, route);
            ClassFile classFile = ((JavaRefTypeInstance)base).getClassFile();
            if (classFile != null) {
                classFile.getBoundSuperClasses(base, boundSuperCollector, route, seen);
            }
            return;
        }
        if (!(base instanceof JavaGenericRefTypeInstance)) {
            throw new IllegalStateException("Base class is not generic");
        }
        JavaGenericRefTypeInstance genericBase = (JavaGenericRefTypeInstance)base;
        JavaGenericRefTypeInstance boundBase = genericBase.getBoundInstance(genericTypeBinder);
        boundSuperCollector.collect(boundBase, route);
        ClassFile classFile = null;
        try {
            classFile = genericBase.getDeGenerifiedType().getClassFile();
        }
        catch (CannotLoadClassException cannotLoadClassException) {
            // empty catch block
        }
        if (classFile == null) {
            return;
        }
        classFile.getBoundSuperClasses(boundBase, boundSuperCollector, route, seen);
    }

    public void noteAnonymousUse(ConstructorInvokationAnonymousInner anoynmousInner) {
        this.anonymousUsages.add(anoynmousInner);
    }

    public void noteMethodUse(ConstructorInvokationSimple constructorCall) {
        this.methodUsages.add(constructorCall);
    }

    public List<ConstructorInvokationAnonymousInner> getAnonymousUsages() {
        return this.anonymousUsages;
    }

    public List<ConstructorInvokationSimple> getMethodUsages() {
        return this.methodUsages;
    }

    public static JavaTypeInstance getAnonymousTypeBase(ClassFile classFile) {
        ClassSignature signature = classFile.getClassSignature();
        if (signature.getInterfaces().isEmpty()) {
            return signature.getSuperClass();
        }
        return signature.getInterfaces().get(0);
    }

    public AttributeMap getAttributes() {
        return this.attributes;
    }

    public List<FakeMethod> getMethodFakes() {
        return this.fakeMethods == null ? null : this.fakeMethods.getMethods();
    }
}

