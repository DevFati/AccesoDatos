/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types.discovery;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.CastAction;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.BoolPair;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class InferredJavaType {
    private static int global_id = 0;
    private IJTInternal value;
    public static final InferredJavaType IGNORE = new InferredJavaType();

    private static List<JavaTypeInstance> getMostDerivedType(Set<JavaTypeInstance> types) {
        boolean effect;
        List<JavaTypeInstance> poss = ListFactory.newList(types);
        block0: do {
            effect = false;
            for (JavaTypeInstance pos : poss) {
                BindingSuperContainer superContainer = pos.getBindingSupers();
                if (superContainer == null) continue;
                Set<JavaRefTypeInstance> supers = SetFactory.newSet(superContainer.getBoundSuperClasses().keySet());
                supers.remove(pos);
                if (!poss.removeAll(supers)) continue;
                effect = true;
                continue block0;
            }
        } while (effect);
        return poss;
    }

    public InferredJavaType() {
        this.value = new IJTInternal_Impl(RawJavaType.VOID, Source.UNKNOWN, false);
    }

    public InferredJavaType(JavaTypeInstance type, Source source) {
        this.value = new IJTInternal_Impl(type, source, false);
    }

    public InferredJavaType(JavaTypeInstance type, Source source, boolean locked) {
        this.value = new IJTInternal_Impl(type, source, locked);
    }

    private InferredJavaType(IJTInternal_Clash clash) {
        this.value = clash;
    }

    private static InferredJavaType mkClash(List<JavaTypeInstance> types) {
        JavaTypeInstance[] arr = types.toArray(new JavaTypeInstance[types.size()]);
        return InferredJavaType.mkClash(arr);
    }

    public static InferredJavaType combineOrClash(InferredJavaType t1, InferredJavaType t2) {
        if (t1.getJavaTypeInstance().equals(t2.getJavaTypeInstance())) {
            t1.chain(t2);
            return t1;
        }
        return InferredJavaType.mkClash(t1.getJavaTypeInstance(), t2.getJavaTypeInstance());
    }

    public static InferredJavaType mkClash(JavaTypeInstance ... types) {
        List ints = ListFactory.newList();
        for (JavaTypeInstance type : types) {
            ints.add(new IJTInternal_Impl(type, Source.UNKNOWN, false));
        }
        return new InferredJavaType(new IJTInternal_Clash(ints));
    }

    private static Map<JavaTypeInstance, JavaGenericRefTypeInstance> getBoundSuperClasses(JavaTypeInstance clashType) {
        Map<JavaTypeInstance, JavaGenericRefTypeInstance> matches = MapFactory.newMap();
        BindingSuperContainer otherSupers = clashType.getBindingSupers();
        if (otherSupers != null) {
            Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> boundSupers = otherSupers.getBoundSuperClasses();
            matches.putAll(boundSupers);
        }
        return matches;
    }

    public Source getSource() {
        return this.value.getSource();
    }

    private void mergeGenericInfo(JavaGenericRefTypeInstance otherTypeInstance) {
        if (this.value.isLocked()) {
            return;
        }
        JavaGenericRefTypeInstance thisType = (JavaGenericRefTypeInstance)this.value.getJavaTypeInstance();
        if (!thisType.hasUnbound()) {
            return;
        }
        ClassFile degenerifiedThisClassFile = thisType.getDeGenerifiedType().getClassFile();
        if (degenerifiedThisClassFile == null) {
            return;
        }
        JavaGenericRefTypeInstance boundThisType = degenerifiedThisClassFile.getBindingSupers().getBoundAssignable(thisType, otherTypeInstance);
        if (!((Object)boundThisType).equals(thisType)) {
            InferredJavaType.mkDelegate(this.value, new IJTInternal_Impl(boundThisType, Source.GENERICCALL, true));
        }
    }

    public void noteUseAs(JavaTypeInstance type) {
        BindingSuperContainer bindingSuperContainer;
        if (this.value.getClashState() == ClashState.Clash && (bindingSuperContainer = this.getJavaTypeInstance().getBindingSupers()) != null && bindingSuperContainer.containsBase(type.getDeGenerifiedType())) {
            this.value.forceType(type, false);
            this.value.markClashState(ClashState.Resolved);
        }
    }

    public void forceType(JavaTypeInstance type, boolean ignoreLockIfResolveClash) {
        boolean ignoreLock = this.value.isLocked() && this.value.getSource() == Source.RESOLVE_CLASH;
        this.value.forceType(type, ignoreLock);
    }

    public boolean isClash() {
        return this.value.getClashState() == ClashState.Clash;
    }

    public InferredJavaType collapseTypeClash() {
        this.value.collapseTypeClash();
        return this;
    }

    public int getLocalId() {
        return this.value.getLocalId();
    }

    public int getTaggedBytecodeLocation() {
        return this.value.getTaggedBytecodeLocation();
    }

    public void setTaggedBytecodeLocation(int location) {
        this.value.setTaggedBytecodeLocation(location);
    }

    private static boolean checkGenericCompatibility(JavaGenericRefTypeInstance thisType, JavaGenericRefTypeInstance otherType) {
        List<JavaTypeInstance> thisTypes = thisType.getGenericTypes();
        List<JavaTypeInstance> otherTypes = otherType.getGenericTypes();
        if (thisTypes.size() != otherTypes.size()) {
            return true;
        }
        int len = thisTypes.size();
        for (int x = 0; x < len; ++x) {
            JavaTypeInstance other1;
            JavaTypeInstance this1 = thisTypes.get(x);
            if (InferredJavaType.checkBaseCompatibility(this1, other1 = otherTypes.get(x))) continue;
            return false;
        }
        return true;
    }

    private boolean checkBaseCompatibility(JavaTypeInstance otherType) {
        return InferredJavaType.checkBaseCompatibility(this.getJavaTypeInstance(), otherType);
    }

    private static boolean checkBaseCompatibility(JavaTypeInstance thisType, JavaTypeInstance otherType) {
        JavaTypeInstance otherStripped;
        if (thisType instanceof JavaArrayTypeInstance && otherType instanceof JavaArrayTypeInstance && otherType.getNumArrayDimensions() == thisType.getNumArrayDimensions()) {
            thisType = thisType.getArrayStrippedType();
            otherType = otherType.getArrayStrippedType();
        }
        if (thisType instanceof JavaGenericPlaceholderTypeInstance || otherType instanceof JavaGenericPlaceholderTypeInstance) {
            return thisType.equals(otherType);
        }
        JavaTypeInstance thisStripped = thisType.getDeGenerifiedType();
        if (thisStripped.equals(otherStripped = otherType.getDeGenerifiedType())) {
            boolean genericThis = thisType instanceof JavaGenericRefTypeInstance;
            boolean genericThat = otherType instanceof JavaGenericRefTypeInstance;
            if (genericThis && genericThat) {
                return InferredJavaType.checkGenericCompatibility((JavaGenericRefTypeInstance)thisType, (JavaGenericRefTypeInstance)otherType);
            }
            return true;
        }
        BindingSuperContainer otherSupers = otherType.getBindingSupers();
        if (otherSupers == null) {
            if (thisStripped.isRaw() || otherStripped.isRaw()) {
                return thisStripped.implicitlyCastsTo(otherStripped, null) || otherStripped.implicitlyCastsTo(thisStripped, null);
            }
            return true;
        }
        return otherSupers.containsBase(thisStripped);
    }

    private CastAction chainFrom(InferredJavaType other) {
        if (this == other) {
            return CastAction.None;
        }
        JavaTypeInstance thisTypeInstance = this.value.getJavaTypeInstance();
        JavaTypeInstance otherTypeInstance = other.value.getJavaTypeInstance();
        if (thisTypeInstance != RawJavaType.VOID) {
            boolean basecast = false;
            if (thisTypeInstance.isComplexType() && otherTypeInstance.isComplexType()) {
                if (!this.checkBaseCompatibility(other.getJavaTypeInstance())) {
                    this.value = IJTInternal_Clash.mkClash(this.value, other.value);
                    return CastAction.None;
                }
                if (this.value.getClashState() == ClashState.Resolved) {
                    return CastAction.None;
                }
                if (thisTypeInstance.getClass() == otherTypeInstance.getClass()) {
                    basecast = true;
                }
            }
            if (otherTypeInstance instanceof JavaGenericRefTypeInstance && thisTypeInstance instanceof JavaGenericRefTypeInstance) {
                other.mergeGenericInfo((JavaGenericRefTypeInstance)thisTypeInstance);
            }
            if (basecast) {
                return CastAction.None;
            }
            if (otherTypeInstance instanceof JavaGenericPlaceholderTypeInstance ^ thisTypeInstance instanceof JavaGenericPlaceholderTypeInstance) {
                return CastAction.InsertExplicit;
            }
        }
        InferredJavaType.mkDelegate(this.value, other.value);
        if (!other.value.isLocked()) {
            this.value = other.value;
        }
        return CastAction.None;
    }

    private static void mkDelegate(IJTInternal a, IJTInternal b) {
        if (!b.usesFinalId(a.getFinalId())) {
            a.mkDelegate(b);
        }
    }

    public void forceDelegate(InferredJavaType other) {
        InferredJavaType.mkDelegate(this.value, other.value);
    }

    private CastAction chainIntegralTypes(InferredJavaType other) {
        if (this == other) {
            return CastAction.None;
        }
        int pri = this.getRawType().compareTypePriorityTo(other.getRawType());
        if (pri >= 0) {
            IJTInternal otherLocked;
            if (other.value.isLocked()) {
                if (pri > 0) {
                    return CastAction.InsertExplicit;
                }
                return CastAction.None;
            }
            if (pri > 0 && (otherLocked = other.value.getFirstLocked()) != null && otherLocked.getJavaTypeInstance() == other.getJavaTypeInstance()) {
                return CastAction.InsertExplicit;
            }
            InferredJavaType.mkDelegate(other.value, this.value);
        } else {
            if (this.value.isLocked()) {
                return CastAction.InsertExplicit;
            }
            InferredJavaType.mkDelegate(this.value, other.value);
            this.value = other.value;
        }
        return CastAction.None;
    }

    public static void compareAsWithoutCasting(InferredJavaType a, InferredJavaType b, boolean aLit, boolean bLit) {
        if (a == IGNORE) {
            return;
        }
        if (b == IGNORE) {
            return;
        }
        RawJavaType art = a.getRawType();
        RawJavaType brt = b.getRawType();
        if (art.getStackType() != StackType.INT || brt.getStackType() != StackType.INT) {
            return;
        }
        InferredJavaType litType = null;
        InferredJavaType betterType = null;
        BoolPair whichLit = BoolPair.get(a.getSource() == Source.LITERAL, b.getSource() == Source.LITERAL);
        if (whichLit.getCount() != 1) {
            whichLit = BoolPair.get(aLit, bLit);
        }
        if (art == RawJavaType.BOOLEAN && brt.getStackType() == StackType.INT && brt.compareTypePriorityTo(art) > 0) {
            litType = a;
            betterType = b;
        } else if (brt == RawJavaType.BOOLEAN && art.getStackType() == StackType.INT && art.compareTypePriorityTo(brt) > 0) {
            litType = b;
            betterType = a;
        } else {
            switch (whichLit) {
                case FIRST: {
                    litType = a;
                    betterType = b;
                    break;
                }
                case SECOND: {
                    litType = b;
                    betterType = a;
                    break;
                }
                case NEITHER: 
                case BOTH: {
                    return;
                }
            }
        }
        litType.chainFrom(betterType);
    }

    public void useAsWithCast(RawJavaType otherRaw) {
        if (this == IGNORE) {
            return;
        }
        this.value = new IJTInternal_Impl(otherRaw, Source.OPERATION, true);
    }

    public void useInArithOp(InferredJavaType other, RawJavaType otherRaw, boolean forbidBool) {
        if (this == IGNORE) {
            return;
        }
        if (other == IGNORE) {
            return;
        }
        RawJavaType thisRaw = this.getRawType();
        if (thisRaw.getStackType() != otherRaw.getStackType()) {
            return;
        }
        if (thisRaw.getStackType() == StackType.INT) {
            int cmp = thisRaw.compareTypePriorityTo(otherRaw);
            if (cmp < 0) {
                if (thisRaw == RawJavaType.BOOLEAN && forbidBool) {
                    this.value.forceType(otherRaw, false);
                }
            } else if (cmp == 0 && thisRaw == RawJavaType.BOOLEAN && forbidBool) {
                this.value.forceType(RawJavaType.INT, false);
            }
        }
    }

    public static void useInArithOp(InferredJavaType lhs, InferredJavaType rhs, ArithOp op) {
        boolean forbidBool = true;
        if ((op == ArithOp.OR || op == ArithOp.AND || op == ArithOp.XOR) && lhs.getJavaTypeInstance() == RawJavaType.BOOLEAN && rhs.getJavaTypeInstance() == RawJavaType.BOOLEAN) {
            forbidBool = false;
        }
        lhs.useInArithOp(rhs, rhs.getRawType(), forbidBool);
        RawJavaType lhsRawType = lhs.getRawType();
        switch (op) {
            case SHL: 
            case SHR: 
            case SHRU: {
                lhsRawType = RawJavaType.INT;
            }
        }
        rhs.useInArithOp(lhs, lhsRawType, forbidBool);
    }

    public void useAsWithoutCasting(JavaTypeInstance otherTypeInstance) {
        if (this == IGNORE) {
            return;
        }
        if (otherTypeInstance == TypeConstants.OBJECT) {
            return;
        }
        JavaTypeInstance thisTypeInstance = this.getJavaTypeInstance();
        if (thisTypeInstance == RawJavaType.NULL) {
            this.value.markKnownBaseClass(otherTypeInstance);
        }
        if (thisTypeInstance instanceof RawJavaType && otherTypeInstance instanceof RawJavaType) {
            RawJavaType otherRaw = otherTypeInstance.getRawTypeOfSimpleType();
            RawJavaType thisRaw = this.getRawType();
            if (thisRaw.getStackType() != otherRaw.getStackType()) {
                return;
            }
            if (thisRaw.getStackType() == StackType.INT) {
                int cmp = thisRaw.compareTypePriorityTo(otherRaw);
                if (cmp > 0) {
                    this.value.forceType(otherRaw, false);
                } else if (cmp < 0 && thisRaw == RawJavaType.BOOLEAN) {
                    this.value.forceType(otherRaw, false);
                }
            }
        } else if (thisTypeInstance instanceof JavaArrayTypeInstance && otherTypeInstance instanceof JavaArrayTypeInstance) {
            JavaArrayTypeInstance thisArrayTypeInstance = (JavaArrayTypeInstance)thisTypeInstance;
            JavaArrayTypeInstance otherArrayTypeInstance = (JavaArrayTypeInstance)otherTypeInstance;
            if (thisArrayTypeInstance.getNumArrayDimensions() != otherArrayTypeInstance.getNumArrayDimensions()) {
                return;
            }
            JavaTypeInstance thisStripped = thisArrayTypeInstance.getArrayStrippedType().getDeGenerifiedType();
            JavaTypeInstance otherArrayStripped = otherArrayTypeInstance.getArrayStrippedType();
            JavaTypeInstance otherStripped = otherArrayStripped.getDeGenerifiedType();
            if (otherArrayStripped instanceof JavaGenericBaseInstance) {
                return;
            }
            if (thisStripped instanceof JavaRefTypeInstance && otherStripped instanceof JavaRefTypeInstance) {
                JavaRefTypeInstance thisRef = (JavaRefTypeInstance)thisStripped;
                JavaRefTypeInstance otherRef = (JavaRefTypeInstance)otherStripped;
                BindingSuperContainer bindingSuperContainer = thisRef.getBindingSupers();
                if (bindingSuperContainer == null) {
                    if (otherRef == TypeConstants.OBJECT) {
                        this.value.forceType(otherTypeInstance, false);
                    }
                } else if (bindingSuperContainer.containsBase(otherRef)) {
                    this.value.forceType(otherTypeInstance, false);
                }
            }
        } else if (thisTypeInstance instanceof JavaGenericRefTypeInstance && otherTypeInstance instanceof JavaGenericRefTypeInstance) {
            this.improveGenericType((JavaGenericRefTypeInstance)otherTypeInstance);
        }
    }

    private void improveGenericType(JavaGenericRefTypeInstance otherGeneric) {
        JavaGenericRefTypeInstance thisUnbound;
        GenericTypeBinder thisBindings;
        JavaTypeInstance thisTypeInstance = this.getJavaTypeInstance();
        if (!(thisTypeInstance instanceof JavaGenericRefTypeInstance)) {
            throw new IllegalStateException();
        }
        JavaGenericRefTypeInstance thisGeneric = (JavaGenericRefTypeInstance)thisTypeInstance;
        JavaRefTypeInstance other = otherGeneric.getDeGenerifiedType();
        BindingSuperContainer thisBindingContainer = thisTypeInstance.getBindingSupers();
        if (thisBindingContainer == null) {
            return;
        }
        JavaGenericRefTypeInstance otherUnbound = thisBindingContainer.getBoundSuperForBase(other);
        if (otherUnbound == null) {
            return;
        }
        GenericTypeBinder otherBindings = GenericTypeBinder.extractBindings(otherUnbound, otherGeneric);
        GenericTypeBinder improvementBindings = otherBindings.createAssignmentRhsBindings(thisBindings = GenericTypeBinder.extractBindings(thisUnbound = thisBindingContainer.getBoundSuperForBase(thisGeneric.getDeGenerifiedType()), thisGeneric));
        if (improvementBindings == null) {
            return;
        }
        if (thisUnbound == null) {
            return;
        }
        JavaTypeInstance thisRebound = improvementBindings.getBindingFor(thisUnbound);
        if (thisRebound == null || thisRebound.equals(thisGeneric)) {
            return;
        }
        if (!(thisRebound instanceof JavaGenericRefTypeInstance)) {
            return;
        }
        this.value.forceType(thisRebound, true);
    }

    public void deGenerify(JavaTypeInstance other) {
        JavaTypeInstance typeInstanceThis = this.getJavaTypeInstance().getDeGenerifiedType();
        JavaTypeInstance typeInstanceOther = other.getDeGenerifiedType();
        if (!typeInstanceOther.equals(typeInstanceThis) && TypeConstants.OBJECT != typeInstanceThis) {
            this.value.forceType(TypeConstants.OBJECT, true);
            return;
        }
        this.value.forceType(other, true);
    }

    public void applyKnownBaseType() {
        JavaTypeInstance type = this.value.getKnownBaseType();
        if (type != null) {
            this.value.forceType(type, false);
        }
    }

    private static boolean isPrimitiveArray(IJTInternal i) {
        if (!(i.getJavaTypeInstance() instanceof JavaArrayTypeInstance)) {
            return false;
        }
        RawJavaType rawTypeOfSimpleType = i.getJavaTypeInstance().getArrayStrippedType().getRawTypeOfSimpleType();
        return !rawTypeOfSimpleType.isObject();
    }

    public CastAction chain(InferredJavaType other) {
        if (this == IGNORE) {
            return CastAction.None;
        }
        if (other == IGNORE) {
            return CastAction.None;
        }
        if (other.getRawType() == RawJavaType.VOID) {
            return CastAction.None;
        }
        RawJavaType thisRaw = this.value.getRawType();
        RawJavaType otherRaw = other.getRawType();
        if (thisRaw == RawJavaType.VOID) {
            return this.chainFrom(other);
        }
        if (thisRaw.getStackType() != otherRaw.getStackType()) {
            if (MiscUtils.xor(thisRaw.getStackType(), otherRaw.getStackType(), StackType.REF)) {
                this.value = IJTInternal_Clash.mkClash(this.value, other.value);
            }
            return CastAction.InsertExplicit;
        }
        if (thisRaw.getStackType() == StackType.REF && otherRaw != RawJavaType.NULL && thisRaw != RawJavaType.NULL && (InferredJavaType.isPrimitiveArray(this.value) || InferredJavaType.isPrimitiveArray(other.value)) && !other.value.getJavaTypeInstance().equals(this.value.getJavaTypeInstance())) {
            this.value = IJTInternal_Clash.mkClash(this.value, other.value);
            return CastAction.InsertExplicit;
        }
        if (thisRaw == otherRaw && thisRaw.getStackType() != StackType.INT) {
            return this.chainFrom(other);
        }
        if (thisRaw == RawJavaType.NULL && (otherRaw == RawJavaType.NULL || otherRaw == RawJavaType.REF)) {
            return this.chainFrom(other);
        }
        if (thisRaw == RawJavaType.REF && otherRaw == RawJavaType.NULL) {
            return CastAction.None;
        }
        if (thisRaw.getStackType() == StackType.INT) {
            return this.chainIntegralTypes(other);
        }
        throw new ConfusedCFRException("Don't know how to tighten from " + thisRaw + " to " + otherRaw);
    }

    public RawJavaType getRawType() {
        return this.value.getRawType();
    }

    public void shallowSetCanBeVar() {
        this.value.shallowSetCanBeVar();
    }

    public void confirmVarIfPossible() {
        this.value.confirmVarIfPossible();
    }

    public JavaTypeInstance getJavaTypeInstance() {
        return this.value.getJavaTypeInstance();
    }

    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return this.value.getClashState() == ClashState.Clash ? " /* !! */ " : "";
    }

    private static class IJTInternal_Impl
    implements IJTInternal {
        private boolean isDelegate = false;
        private final boolean locked;
        private JavaTypeInstance type;
        private JavaTypeInstance knownBase;
        private int taggedBytecodeLocation = -1;
        private final Source source;
        private final int id;
        private IJTInternal delegate;
        private Troolean canBeVar = Troolean.FALSE;

        private IJTInternal_Impl(JavaTypeInstance type, Source source, boolean locked) {
            this.type = type;
            this.source = source;
            this.id = global_id++;
            this.locked = locked;
        }

        @Override
        public RawJavaType getRawType() {
            if (this.isDelegate) {
                return this.delegate.getRawType();
            }
            return this.type.getRawTypeOfSimpleType();
        }

        @Override
        public int getTaggedBytecodeLocation() {
            if (this.isDelegate) {
                return this.delegate.getTaggedBytecodeLocation();
            }
            return this.taggedBytecodeLocation;
        }

        @Override
        public void setTaggedBytecodeLocation(int location) {
            if (this.isDelegate) {
                this.delegate.setTaggedBytecodeLocation(location);
            } else {
                this.taggedBytecodeLocation = location;
            }
        }

        @Override
        public JavaTypeInstance getJavaTypeInstance() {
            if (this.isDelegate) {
                return this.delegate.getJavaTypeInstance();
            }
            return this.type;
        }

        @Override
        public Source getSource() {
            if (this.isDelegate) {
                return this.delegate.getSource();
            }
            return this.source;
        }

        @Override
        public void collapseTypeClash() {
            if (this.isDelegate) {
                this.delegate.collapseTypeClash();
            }
        }

        @Override
        public int getFinalId() {
            if (this.isDelegate) {
                return this.delegate.getFinalId();
            }
            return this.id;
        }

        @Override
        public boolean usesFinalId(int id) {
            if (this.isDelegate) {
                return this.delegate.usesFinalId(id);
            }
            return this.id == id;
        }

        @Override
        public int getLocalId() {
            return this.id;
        }

        @Override
        public void shallowSetCanBeVar() {
            this.canBeVar = Troolean.NEITHER;
        }

        @Override
        public void confirmVarIfPossible() {
            if (this.canBeVar != Troolean.FALSE) {
                this.canBeVar = Troolean.TRUE;
                this.isDelegate = false;
                return;
            }
            if (this.isDelegate) {
                this.delegate.confirmVarIfPossible();
            }
        }

        @Override
        public ClashState getClashState() {
            return ClashState.None;
        }

        @Override
        public void mkDelegate(IJTInternal newDelegate) {
            if (this.isDelegate) {
                this.delegate.mkDelegate(newDelegate);
            } else {
                this.isDelegate = true;
                this.delegate = newDelegate;
            }
        }

        @Override
        public void markKnownBaseClass(JavaTypeInstance newKnownBase) {
            if (this.isDelegate) {
                this.delegate.markKnownBaseClass(newKnownBase);
                return;
            }
            if (this.knownBase == null) {
                this.knownBase = newKnownBase;
            } else {
                BindingSuperContainer boundSupers = this.knownBase.getBindingSupers();
                if (boundSupers == null || !boundSupers.containsBase(newKnownBase.getDeGenerifiedType())) {
                    this.knownBase = newKnownBase;
                }
            }
        }

        @Override
        public JavaTypeInstance getKnownBaseType() {
            if (this.isDelegate) {
                return this.delegate.getKnownBaseType();
            }
            return this.knownBase;
        }

        @Override
        public void forceType(JavaTypeInstance rawJavaType, boolean ignoreLock) {
            if (!ignoreLock && this.isLocked()) {
                return;
            }
            if (this.isDelegate && this.delegate.isLocked() && !ignoreLock) {
                this.isDelegate = false;
            }
            if (this.isDelegate) {
                this.delegate.forceType(rawJavaType, ignoreLock);
            } else {
                this.type = rawJavaType;
            }
        }

        @Override
        public void markClashState(ClashState newClashState) {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            if (this.isDelegate) {
                return "#" + this.id + " -> " + this.delegate.toString();
            }
            return "#" + this.id + " " + this.type.toString();
        }

        @Override
        public boolean isLocked() {
            return this.locked;
        }

        @Override
        public IJTInternal getFirstLocked() {
            if (this.locked) {
                return this;
            }
            if (this.delegate != null) {
                return this.delegate.getFirstLocked();
            }
            return null;
        }
    }

    private static class IJTInternal_Clash
    implements IJTInternal {
        private boolean resolved = false;
        private List<IJTInternal> clashes;
        private final int id = InferredJavaType.access$008();
        private JavaTypeInstance type = null;

        private IJTInternal_Clash(Collection<IJTInternal> clashes) {
            this.clashes = ListFactory.newList(SetFactory.newOrderedSet(clashes));
        }

        private static Map<JavaTypeInstance, JavaGenericRefTypeInstance> getClashMatches(List<IJTInternal> clashes) {
            List<JavaTypeInstance> clashTypes = ListFactory.newList();
            for (IJTInternal clash : clashes) {
                clashTypes.add(clash.getJavaTypeInstance());
            }
            return IJTInternal_Clash.getMatches(clashTypes);
        }

        private static Map<JavaTypeInstance, JavaGenericRefTypeInstance> getMatches(List<JavaTypeInstance> clashes) {
            Map matches = InferredJavaType.getBoundSuperClasses(clashes.get(0));
            int len = clashes.size();
            for (int x = 1; x < len; ++x) {
                JavaTypeInstance clashType = clashes.get(x);
                BindingSuperContainer otherSupers = clashType.getBindingSupers();
                if (otherSupers == null) {
                    if (clashType.isRaw() && !clashType.isObject()) {
                        matches.clear();
                        return matches;
                    }
                    if (!(clashType instanceof JavaArrayTypeInstance)) continue;
                    matches.keySet().retainAll(ListFactory.newList(clashType, TypeConstants.OBJECT));
                    continue;
                }
                Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> boundSupers = otherSupers.getBoundSuperClasses();
                matches.keySet().retainAll(boundSupers.keySet());
            }
            return matches;
        }

        private static IJTInternal mkClash(IJTInternal delegate1, IJTInternal delegate2) {
            List<IJTInternal> clashes = ListFactory.newList();
            if (delegate1 instanceof IJTInternal_Clash) {
                clashes.addAll(((IJTInternal_Clash)delegate1).clashes);
            } else {
                clashes.add(delegate1);
            }
            if (delegate2 instanceof IJTInternal_Clash) {
                clashes.addAll(((IJTInternal_Clash)delegate2).clashes);
            } else {
                clashes.add(delegate2);
            }
            Map<JavaTypeInstance, JavaGenericRefTypeInstance> matches = IJTInternal_Clash.getClashMatches(clashes);
            if (matches.isEmpty()) {
                IJTInternal_Clash tmp = new IJTInternal_Clash(clashes);
                tmp.collapseTypeClash(false);
                if (tmp.resolved) {
                    return new IJTInternal_Impl(tmp.getJavaTypeInstance(), Source.RESOLVE_CLASH, true);
                }
            }
            if (matches.size() == 1) {
                return new IJTInternal_Impl(matches.keySet().iterator().next(), Source.RESOLVE_CLASH, true);
            }
            return new IJTInternal_Clash(clashes);
        }

        @Override
        public void collapseTypeClash() {
            this.collapseTypeClash(true);
        }

        @Override
        public void shallowSetCanBeVar() {
        }

        @Override
        public void confirmVarIfPossible() {
        }

        private void collapseTypeClash(boolean force) {
            Pair<Boolean, JavaTypeInstance> newlyResolved;
            if (this.resolved) {
                return;
            }
            List<JavaTypeInstance> clashTypes = ListFactory.newList();
            int arraySize = this.clashes.get(0).getJavaTypeInstance().getNumArrayDimensions();
            for (IJTInternal clash : this.clashes) {
                JavaTypeInstance clashType = clash.getJavaTypeInstance();
                if (clashType.getNumArrayDimensions() != arraySize) {
                    arraySize = -1;
                }
                clashTypes.add(clashType);
            }
            if (arraySize == 1) {
                for (int x = 0; x < clashTypes.size(); ++x) {
                    clashTypes.set(x, clashTypes.get(x).removeAnArrayIndirection());
                }
            }
            if ((newlyResolved = IJTInternal_Clash.collapseTypeClash2(clashTypes)) == null) {
                return;
            }
            if (!newlyResolved.getFirst().booleanValue() && !force) {
                return;
            }
            this.resolved = true;
            this.type = newlyResolved.getSecond();
            if (arraySize == 1) {
                this.type = new JavaArrayTypeInstance(1, this.type);
            }
        }

        private static Pair<Boolean, JavaTypeInstance> collapseTypeClash2(List<JavaTypeInstance> clashes) {
            JavaTypeInstance result;
            block10: {
                JavaTypeInstance bindingFor;
                Map<JavaTypeInstance, JavaGenericRefTypeInstance> matches = IJTInternal_Clash.getMatches(clashes);
                if (matches.isEmpty()) {
                    return Pair.make(false, TypeConstants.OBJECT);
                }
                List<JavaTypeInstance> poss = InferredJavaType.getMostDerivedType(matches.keySet());
                JavaTypeInstance oneClash = clashes.get(0);
                Map<JavaRefTypeInstance, BindingSuperContainer.Route> routes = oneClash.getBindingSupers().getBoundSuperRoute();
                if (poss.isEmpty()) {
                    poss = ListFactory.newList(matches.keySet());
                }
                for (JavaTypeInstance pos : poss) {
                    if (BindingSuperContainer.Route.EXTENSION != routes.get(pos)) continue;
                    return Pair.make(true, pos);
                }
                result = poss.get(0);
                JavaGenericRefTypeInstance rhs = matches.get(result);
                if (rhs != null && (bindingFor = GenericTypeBinder.extractBindings(rhs, oneClash).getBindingFor(rhs)) != null && bindingFor instanceof JavaGenericRefTypeInstance) {
                    JavaGenericRefTypeInstance genericBindingFor = (JavaGenericRefTypeInstance)bindingFor;
                    List clashSubs = ListFactory.newList();
                    for (JavaTypeInstance typ : genericBindingFor.getGenericTypes()) {
                        clashSubs.add(ListFactory.newList(typ));
                    }
                    for (int i = 1; i < clashes.size(); ++i) {
                        JavaGenericRefTypeInstance gr2;
                        List<JavaTypeInstance> thisClashSubs;
                        JavaTypeInstance bindingFor2 = GenericTypeBinder.extractBindings(rhs, clashes.get(i)).getBindingFor(rhs);
                        if (bindingFor2 instanceof JavaGenericRefTypeInstance && (thisClashSubs = (gr2 = (JavaGenericRefTypeInstance)bindingFor2).getGenericTypes()).size() == clashSubs.size()) {
                            for (int j = 0; j < clashSubs.size(); ++j) {
                                List clashSubsPosn = (List)clashSubs.get(j);
                                if (((JavaTypeInstance)clashSubsPosn.get(0)).equals(thisClashSubs.get(j))) continue;
                                clashSubsPosn.add(thisClashSubs.get(j));
                            }
                            continue;
                        }
                        break block10;
                    }
                    List<JavaTypeInstance> resolvedSubs = ListFactory.newList();
                    for (int i = 0; i < clashSubs.size(); ++i) {
                        List posSub = (List)clashSubs.get(i);
                        if (posSub.size() == 1) {
                            resolvedSubs.add((JavaTypeInstance)posSub.get(0));
                            continue;
                        }
                        JavaTypeInstance reRes = InferredJavaType.mkClash(posSub).collapseTypeClash().getJavaTypeInstance();
                        resolvedSubs.add(reRes);
                    }
                    result = bindingFor = new JavaGenericRefTypeInstance(genericBindingFor.getTypeInstance(), resolvedSubs);
                }
            }
            return Pair.make(true, result);
        }

        @Override
        public RawJavaType getRawType() {
            if (this.resolved) {
                return this.type.getRawTypeOfSimpleType();
            }
            return this.clashes.get(0).getRawType();
        }

        @Override
        public int getTaggedBytecodeLocation() {
            return -1;
        }

        @Override
        public void setTaggedBytecodeLocation(int location) {
        }

        @Override
        public JavaTypeInstance getJavaTypeInstance() {
            if (this.resolved) {
                return this.type;
            }
            return this.clashes.get(0).getJavaTypeInstance();
        }

        @Override
        public Source getSource() {
            return this.clashes.get(0).getSource();
        }

        @Override
        public int getFinalId() {
            return this.id;
        }

        @Override
        public boolean usesFinalId(int id) {
            if (this.id == id) {
                return true;
            }
            if (this.resolved) {
                return this.clashes.get(0).usesFinalId(id);
            }
            for (IJTInternal internal : this.clashes) {
                if (!internal.usesFinalId(id)) continue;
                return true;
            }
            return false;
        }

        @Override
        public int getLocalId() {
            return this.id;
        }

        @Override
        public ClashState getClashState() {
            if (this.resolved) {
                return ClashState.Resolved;
            }
            return ClashState.Clash;
        }

        @Override
        public void mkDelegate(IJTInternal newDelegate) {
        }

        @Override
        public void forceType(JavaTypeInstance rawJavaType, boolean ignoreLock) {
            this.type = rawJavaType;
            this.resolved = true;
        }

        @Override
        public void markKnownBaseClass(JavaTypeInstance knownBase) {
        }

        @Override
        public JavaTypeInstance getKnownBaseType() {
            return null;
        }

        @Override
        public void markClashState(ClashState newClashState) {
        }

        @Override
        public boolean isLocked() {
            return this.resolved;
        }

        @Override
        public IJTInternal getFirstLocked() {
            return null;
        }

        public String toString() {
            if (this.resolved) {
                return "#" + this.id + " " + this.type.toString();
            }
            StringBuilder sb = new StringBuilder();
            for (IJTInternal clash : this.clashes) {
                sb.append(this.id).append(" -> ").append(clash.toString()).append(", ");
            }
            return sb.toString();
        }
    }

    private static interface IJTInternal {
        public RawJavaType getRawType();

        public JavaTypeInstance getJavaTypeInstance();

        public Source getSource();

        public int getLocalId();

        public int getFinalId();

        public boolean usesFinalId(int var1);

        public ClashState getClashState();

        public void collapseTypeClash();

        public void mkDelegate(IJTInternal var1);

        public void forceType(JavaTypeInstance var1, boolean var2);

        public void markKnownBaseClass(JavaTypeInstance var1);

        public JavaTypeInstance getKnownBaseType();

        public void markClashState(ClashState var1);

        public boolean isLocked();

        public IJTInternal getFirstLocked();

        public int getTaggedBytecodeLocation();

        public void setTaggedBytecodeLocation(int var1);

        public void shallowSetCanBeVar();

        public void confirmVarIfPossible();
    }

    private static enum ClashState {
        None,
        Clash,
        Resolved;

    }

    public static enum Source {
        TEST,
        UNKNOWN,
        LITERAL,
        FIELD,
        FUNCTION,
        PROTOTYPE,
        BOOTSTRAP,
        CONSTRUCTOR,
        OPERATION,
        EXPRESSION,
        INSTRUCTION,
        GENERICCALL,
        EXCEPTION,
        STRING_TRANSFORM,
        IMPROVED_ITERATION,
        TERNARY,
        RESOLVE_CLASH,
        FORCE_TARGET_TYPE,
        TRANSFORM;

    }
}

