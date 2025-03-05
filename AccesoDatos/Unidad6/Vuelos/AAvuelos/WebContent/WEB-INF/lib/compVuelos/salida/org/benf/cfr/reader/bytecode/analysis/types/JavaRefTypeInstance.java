/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.ObfuscationTypeMap;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.annotation.Nullable;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.ToStringDumper;
import org.benf.cfr.reader.util.output.TypeContext;

public class JavaRefTypeInstance
implements JavaTypeInstance {
    private final String className;
    private String shortName;
    private String suggestedVarName;
    private InnerClassInfo innerClassInfo;
    private final DCCommonState dcCommonState;
    private BindingSuperContainer cachedBindingSupers = BindingSuperContainer.POISON;

    private JavaRefTypeInstance(String className, DCCommonState dcCommonState) {
        this.innerClassInfo = InnerClassInfo.NOT;
        this.dcCommonState = dcCommonState;
        if (className.contains("$")) {
            int idx = className.lastIndexOf(36);
            if (idx == className.length() - 1) {
                MiscUtils.handyBreakPoint();
            } else {
                String outer = className.substring(0, idx);
                JavaRefTypeInstance outerClassTmp = dcCommonState.getClassCache().getRefClassFor(outer);
                this.innerClassInfo = new RefTypeInnerClassInfo(outerClassTmp);
            }
        }
        this.className = className;
        this.shortName = JavaRefTypeInstance.getShortName(className, this.innerClassInfo);
    }

    public void setUnexpectedInnerClassOf(JavaRefTypeInstance parent) {
        this.innerClassInfo = new RefTypeInnerClassInfo(parent);
    }

    private JavaRefTypeInstance(String className, JavaRefTypeInstance knownOuter, DCCommonState dcCommonState) {
        this.className = className;
        this.dcCommonState = dcCommonState;
        String innerSub = className.substring(knownOuter.className.length());
        if (innerSub.charAt(0) == '$') {
            innerSub = innerSub.substring(1);
        }
        this.innerClassInfo = new RefTypeInnerClassInfo(knownOuter);
        this.shortName = innerSub;
    }

    @Override
    public JavaAnnotatedTypeInstance getAnnotatedInstance() {
        JavaRefTypeInstance rti = this;
        Annotated prev = null;
        boolean couldLoadOuterClasses = true;
        do {
            InnerClassInfo ici = rti.getInnerClassHereInfo();
            boolean isInner = ici.isInnerClass();
            prev = new Annotated(prev, rti);
            ClassFile classFile = rti.getClassFile();
            if (isInner && (classFile == null || !classFile.testAccessFlag(AccessFlag.ACC_STATIC))) {
                rti = ici.getOuterClass();
                if (!couldLoadOuterClasses || classFile != null) continue;
                couldLoadOuterClasses = false;
                continue;
            }
            rti = null;
        } while (rti != null);
        if (!couldLoadOuterClasses) {
            prev.setComment(DecompilerComment.BAD_ANNOTATION_ON_INNER);
        }
        return prev;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    public void markNotInner() {
        this.innerClassInfo = InnerClassInfo.NOT;
        this.shortName = JavaRefTypeInstance.getShortName(this.className, this.innerClassInfo);
        this.suggestedVarName = null;
    }

    @Override
    public String suggestVarName() {
        char c;
        int x;
        if (this.suggestedVarName != null) {
            return this.suggestedVarName;
        }
        String displayName = this.shortName;
        if (displayName.isEmpty()) {
            return null;
        }
        char[] chars = displayName.toCharArray();
        int len = chars.length;
        for (x = 0; x < len && (c = chars[x]) >= '0' && c <= '9'; ++x) {
        }
        if (x >= len) {
            return null;
        }
        chars[x] = Character.toLowerCase(chars[x]);
        this.suggestedVarName = new String(chars, x, len - x);
        return this.suggestedVarName;
    }

    private JavaRefTypeInstance(String className, String displayableName, JavaRefTypeInstance[] supers) {
        this.innerClassInfo = InnerClassInfo.NOT;
        this.dcCommonState = null;
        this.className = className;
        this.shortName = displayableName;
        Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> tmp = MapFactory.newMap();
        Map<JavaRefTypeInstance, BindingSuperContainer.Route> routes = MapFactory.newMap();
        for (JavaRefTypeInstance supr : supers) {
            tmp.put(supr, null);
            routes.put(supr, BindingSuperContainer.Route.EXTENSION);
        }
        tmp.put(this, null);
        this.cachedBindingSupers = new BindingSuperContainer(null, tmp, routes);
    }

    public static JavaRefTypeInstance create(String rawClassName, DCCommonState dcCommonState) {
        return new JavaRefTypeInstance(rawClassName, dcCommonState);
    }

    public static Pair<JavaRefTypeInstance, JavaRefTypeInstance> createKnownInnerOuter(String inner, String outer, JavaRefTypeInstance outerType, DCCommonState dcCommonState) {
        if (outerType == null) {
            outerType = new JavaRefTypeInstance(outer, dcCommonState);
        }
        JavaRefTypeInstance innerType = !inner.startsWith(outer) ? new JavaRefTypeInstance(inner, dcCommonState) : new JavaRefTypeInstance(inner, outerType, dcCommonState);
        return Pair.make(innerType, outerType);
    }

    static JavaRefTypeInstance createTypeConstant(String rawClassName, String displayableName, JavaRefTypeInstance ... supers) {
        return new JavaRefTypeInstance(rawClassName, displayableName, supers);
    }

    public static JavaRefTypeInstance createTypeConstant(String rawClassName, JavaRefTypeInstance ... supers) {
        return JavaRefTypeInstance.createTypeConstant(rawClassName, JavaRefTypeInstance.getShortName(rawClassName), supers);
    }

    static JavaRefTypeInstance createTypeConstantWithObjectSuper(String rawClassName) {
        return JavaRefTypeInstance.createTypeConstant(rawClassName, JavaRefTypeInstance.getShortName(rawClassName), TypeConstants.OBJECT);
    }

    @Override
    public StackType getStackType() {
        return StackType.REF;
    }

    @Override
    public void dumpInto(Dumper d, TypeUsageInformation typeUsageInformation, TypeContext typeContext) {
        String res = typeUsageInformation.getName(this, typeContext);
        if (res == null) {
            throw new IllegalStateException();
        }
        d.print(res);
    }

    public String getPackageName() {
        return ClassNameUtils.getPackageAndClassNames(this).getFirst();
    }

    public String toString() {
        return new ToStringDumper().dump(this).toString();
    }

    @Override
    public JavaTypeInstance getArrayStrippedType() {
        return this;
    }

    @Override
    public int getNumArrayDimensions() {
        return 0;
    }

    @Override
    public String getRawName() {
        return this.className;
    }

    public String getRawShortName() {
        return this.shortName;
    }

    @Override
    public String getRawName(IllegalIdentifierDump iid) {
        if (iid != null) {
            String replaceShortName = this.getRawShortName(iid);
            if (this.shortName == replaceShortName) {
                return this.className;
            }
            return this.className.substring(0, this.className.length() - this.shortName.length()) + replaceShortName;
        }
        return this.getRawName();
    }

    public String getRawShortName(IllegalIdentifierDump iid) {
        if (iid != null) {
            return iid.getLegalShortName(this.shortName);
        }
        return this.getRawShortName();
    }

    public int hashCode() {
        return 31 + this.className.hashCode();
    }

    @Override
    public InnerClassInfo getInnerClassHereInfo() {
        return this.innerClassInfo;
    }

    public void forceBindingSupers(BindingSuperContainer bindingSuperContainer) {
        this.cachedBindingSupers = bindingSuperContainer;
    }

    @Override
    public BindingSuperContainer getBindingSupers() {
        if (this.cachedBindingSupers != BindingSuperContainer.POISON) {
            return this.cachedBindingSupers;
        }
        try {
            ClassFile classFile = this.getClassFile();
            this.cachedBindingSupers = classFile == null ? null : classFile.getBindingSupers();
        }
        catch (CannotLoadClassException e) {
            this.cachedBindingSupers = null;
        }
        return this.cachedBindingSupers;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof JavaRefTypeInstance)) {
            return false;
        }
        JavaRefTypeInstance other = (JavaRefTypeInstance)o;
        return other.className.equals(this.className);
    }

    @Override
    public boolean isComplexType() {
        return true;
    }

    @Override
    public boolean isUsableType() {
        return true;
    }

    @Override
    public JavaTypeInstance removeAnArrayIndirection() {
        return this;
    }

    @Override
    public JavaTypeInstance getDeGenerifiedType() {
        return this;
    }

    @Override
    public RawJavaType getRawTypeOfSimpleType() {
        return RawJavaType.REF;
    }

    @Override
    public JavaTypeInstance deObfuscate(ObfuscationTypeMap obfuscationTypeMap) {
        return obfuscationTypeMap.get(this);
    }

    @Override
    public boolean implicitlyCastsTo(JavaTypeInstance other, @Nullable GenericTypeBinder gtb) {
        RawJavaType thisAsRaw;
        if (this.equals(other)) {
            return true;
        }
        if (other instanceof RawJavaType && (thisAsRaw = RawJavaType.getUnboxedTypeFor(this)) != null) {
            return thisAsRaw.implicitlyCastsTo(other, gtb);
        }
        if (gtb != null && other instanceof JavaGenericPlaceholderTypeInstance && !((other = gtb.getBindingFor(other)) instanceof JavaGenericPlaceholderTypeInstance)) {
            return this.implicitlyCastsTo(other, gtb);
        }
        if (other instanceof JavaGenericPlaceholderTypeInstance) {
            return false;
        }
        JavaTypeInstance otherRaw = other.getDeGenerifiedType();
        BindingSuperContainer thisBindingSuper = this.getBindingSupers();
        if (thisBindingSuper == null) {
            return otherRaw == TypeConstants.OBJECT;
        }
        return thisBindingSuper.containsBase(otherRaw);
    }

    @Override
    public JavaTypeInstance directImplOf(JavaTypeInstance other) {
        return other == this ? this : null;
    }

    @Override
    public boolean impreciseCanCastTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        if (this == other || this.equals(other)) {
            return true;
        }
        if (other instanceof RawJavaType) {
            RawJavaType thisAsRaw = RawJavaType.getUnboxedTypeFor(this);
            if (thisAsRaw != null) {
                return thisAsRaw.equals(other);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean correctCanCastTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        if (this == other || this.equals(other)) {
            return true;
        }
        if (other instanceof RawJavaType) {
            RawJavaType thisAsRaw = RawJavaType.getUnboxedTypeFor(this);
            if (thisAsRaw != null) {
                return thisAsRaw.equals(other);
            }
            return true;
        }
        BindingSuperContainer bindingSuperContainer = this.getBindingSupers();
        if (bindingSuperContainer == null) {
            return true;
        }
        if (bindingSuperContainer.containsBase(other)) {
            return true;
        }
        bindingSuperContainer = other.getBindingSupers();
        if (bindingSuperContainer == null) {
            return true;
        }
        return bindingSuperContainer.containsBase(this);
    }

    public ClassFile getClassFile() {
        if (this.dcCommonState == null) {
            return null;
        }
        try {
            ClassFile classFile = this.dcCommonState.getClassFile(this);
            return classFile;
        }
        catch (CannotLoadClassException e) {
            return null;
        }
    }

    private static String getShortName(String fullClassName) {
        int idxlast = fullClassName.lastIndexOf(46);
        String partname = idxlast == -1 ? fullClassName : fullClassName.substring(idxlast + 1);
        return partname;
    }

    private static String getShortName(String fullClassName, InnerClassInfo innerClassInfo) {
        if (innerClassInfo.isInnerClass()) {
            fullClassName = fullClassName.replace('$', '.');
        }
        return JavaRefTypeInstance.getShortName(fullClassName);
    }

    @Override
    public void collectInto(TypeUsageCollector typeUsageCollector) {
        typeUsageCollector.collectRefType(this);
    }

    @Override
    public JavaGenericRefTypeInstance asGenericRefInstance(JavaTypeInstance other) {
        return null;
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    private static class RefTypeInnerClassInfo
    implements InnerClassInfo {
        private final JavaRefTypeInstance outerClass;
        private boolean isAnonymous = false;
        private boolean isMethodScoped = false;
        private boolean hideSyntheticThis = false;
        private boolean hideSyntheticFriendClass = false;

        private RefTypeInnerClassInfo(JavaRefTypeInstance outerClass) {
            this.outerClass = outerClass;
        }

        @Override
        public void collectTransitiveDegenericParents(Set<JavaTypeInstance> parents) {
            parents.add(this.outerClass);
            this.outerClass.getInnerClassHereInfo().collectTransitiveDegenericParents(parents);
        }

        @Override
        public boolean getFullInnerPath(StringBuilder sb) {
            if (this.outerClass.getInnerClassHereInfo().getFullInnerPath(sb)) {
                sb.append(this.outerClass.shortName).append('.');
            }
            return true;
        }

        @Override
        public boolean isInnerClass() {
            return true;
        }

        @Override
        public boolean isAnonymousClass() {
            return this.isAnonymous;
        }

        @Override
        public boolean isMethodScopedClass() {
            return this.isMethodScoped;
        }

        @Override
        public void markMethodScoped(boolean isAnonymous) {
            this.isAnonymous = isAnonymous;
            this.isMethodScoped = true;
        }

        @Override
        public boolean isInnerClassOf(JavaTypeInstance possibleParent) {
            if (this.outerClass == null) {
                return false;
            }
            return possibleParent.equals(this.outerClass);
        }

        @Override
        public boolean isTransitiveInnerClassOf(JavaTypeInstance possibleParent) {
            if (this.outerClass == null) {
                return false;
            }
            if (possibleParent.equals(this.outerClass)) {
                return true;
            }
            InnerClassInfo upper = this.outerClass.getInnerClassHereInfo();
            if (!upper.isInnerClass()) {
                return false;
            }
            return upper.isTransitiveInnerClassOf(possibleParent);
        }

        @Override
        public void setHideSyntheticThis() {
            this.hideSyntheticThis = true;
        }

        @Override
        public void hideSyntheticFriendClass() {
            this.hideSyntheticFriendClass = true;
        }

        @Override
        public boolean isSyntheticFriendClass() {
            return this.hideSyntheticFriendClass;
        }

        @Override
        public JavaRefTypeInstance getOuterClass() {
            return this.outerClass;
        }

        @Override
        public boolean isHideSyntheticThis() {
            return this.hideSyntheticThis;
        }
    }

    private static class Annotated
    implements JavaAnnotatedTypeInstance {
        private final List<AnnotationTableEntry> entries = ListFactory.newList();
        private final Annotated inner;
        private final JavaRefTypeInstance outerThis;
        private DecompilerComment nullableComment = null;

        private Annotated(Annotated inner, JavaRefTypeInstance outerThis) {
            this.inner = inner;
            this.outerThis = outerThis;
        }

        public void setComment(DecompilerComment comment) {
            this.nullableComment = comment;
        }

        @Override
        public JavaAnnotatedTypeIterator pathIterator() {
            return new Iterator();
        }

        private void dump(Dumper d, boolean hasDumpedType) {
            if (this.nullableComment != null) {
                d.dump(this.nullableComment);
            }
            if (!this.entries.isEmpty()) {
                if (hasDumpedType) {
                    d.print(' ');
                }
                for (AnnotationTableEntry entry : this.entries) {
                    entry.dump(d);
                    d.print(' ');
                }
            }
            if (!hasDumpedType && this.entries.isEmpty() && this.inner != null) {
                this.inner.dump(d, false);
            } else {
                if (!hasDumpedType) {
                    d.dump(this.outerThis);
                } else {
                    d.print(this.outerThis.getRawShortName());
                }
                if (this.inner != null) {
                    d.print('.');
                    this.inner.dump(d, true);
                }
            }
        }

        Annotated getFirstWithEntries() {
            if (this.inner == null) {
                return null;
            }
            if (this.entries.isEmpty()) {
                return this.inner.getFirstWithEntries();
            }
            return this;
        }

        @Override
        public Dumper dump(Dumper d) {
            boolean hasDumpedType = false;
            Annotated firstEntryType = this.getFirstWithEntries();
            if (firstEntryType != null) {
                JavaRefTypeInstance typ = firstEntryType.outerThis;
                String display = d.getTypeUsageInformation().getName(typ, TypeContext.None);
                String raw = typ.getRawShortName();
                if (!raw.equals(display)) {
                    hasDumpedType = true;
                }
            }
            this.dump(d, hasDumpedType);
            return d;
        }

        private class Iterator
        extends JavaAnnotatedTypeIterator.BaseAnnotatedTypeIterator {
            private Iterator() {
            }

            @Override
            public JavaAnnotatedTypeIterator moveNested(DecompilerComments comments) {
                return Annotated.this.inner.pathIterator();
            }

            @Override
            public void apply(AnnotationTableEntry entry) {
                Annotated.this.entries.add(entry);
            }
        }
    }
}

