/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaAnnotatedTypeIterator;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.state.ObfuscationTypeMap;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

public enum RawJavaType implements JavaTypeInstance
{
    BOOLEAN("boolean", "bl", StackType.INT, true, "java.lang.Boolean", false, false, 0, 1),
    BYTE("byte", "by", StackType.INT, true, "java.lang.Byte", true, false, -128, 127),
    CHAR("char", "c", StackType.INT, true, "java.lang.Character", false, false, 0, 65535),
    SHORT("short", "s", StackType.INT, true, "java.lang.Short", true, false, Short.MIN_VALUE, Short.MAX_VALUE),
    INT("int", "n", StackType.INT, true, "java.lang.Integer", true, false, Integer.MIN_VALUE, Integer.MAX_VALUE),
    LONG("long", "l", StackType.LONG, true, "java.lang.Long", true, false),
    FLOAT("float", "f", StackType.FLOAT, true, "java.lang.Float", true, false),
    DOUBLE("double", "d", StackType.DOUBLE, true, "java.lang.Double", true, false),
    VOID("void", null, StackType.VOID, false, false),
    REF("reference", null, StackType.REF, false, true),
    RETURNADDRESS("returnaddress", null, StackType.RETURNADDRESS, false, true),
    RETURNADDRESSORREF("returnaddress or ref", null, StackType.RETURNADDRESSORREF, false, true),
    NULL("null", null, StackType.REF, false, true);

    private final String name;
    private final String suggestedVarName;
    private final StackType stackType;
    private final boolean usableType;
    private final String boxedName;
    private final boolean isNumber;
    private final boolean isObject;
    private final int intMin;
    private final int intMax;
    private static final Map<RawJavaType, Set<RawJavaType>> implicitCasts;
    private static final Map<String, RawJavaType> boxingTypes;
    private static final Map<String, RawJavaType> podLookup;

    public static RawJavaType getUnboxedTypeFor(JavaTypeInstance type) {
        String rawName = type.getRawName();
        return boxingTypes.get(rawName);
    }

    public static RawJavaType getPodNamedType(String name) {
        return podLookup.get(name);
    }

    private RawJavaType(String name, String suggestedVarName, StackType stackType, boolean usableType, String boxedName, boolean isNumber, boolean objectType, int intMin, int intMax) {
        this.name = name;
        this.stackType = stackType;
        this.usableType = usableType;
        this.boxedName = boxedName;
        this.suggestedVarName = suggestedVarName;
        this.isNumber = isNumber;
        this.isObject = objectType;
        this.intMin = intMin;
        this.intMax = intMax;
    }

    private RawJavaType(String name, String suggestedVarName, StackType stackType, boolean usableType, String boxedName, boolean isNumber, boolean objectType) {
        this(name, suggestedVarName, stackType, usableType, boxedName, isNumber, objectType, Integer.MAX_VALUE, Integer.MIN_VALUE);
    }

    private RawJavaType(String name, String suggestedVarName, StackType stackType, boolean usableType, boolean objectType) {
        this(name, suggestedVarName, stackType, usableType, null, false, objectType);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public JavaAnnotatedTypeInstance getAnnotatedInstance() {
        return new Annotated();
    }

    @Override
    public StackType getStackType() {
        return this.stackType;
    }

    @Override
    public boolean isComplexType() {
        return false;
    }

    @Override
    public boolean isObject() {
        return this.isObject;
    }

    @Override
    public JavaGenericRefTypeInstance asGenericRefInstance(JavaTypeInstance other) {
        return null;
    }

    @Override
    public JavaTypeInstance directImplOf(JavaTypeInstance other) {
        return other == this ? this : null;
    }

    @Override
    public JavaTypeInstance deObfuscate(ObfuscationTypeMap obfuscationTypeMap) {
        return this;
    }

    public boolean inIntRange(int value) {
        return value >= this.intMin && value <= this.intMax;
    }

    @Override
    public boolean isRaw() {
        return true;
    }

    public int compareTypePriorityTo(RawJavaType other) {
        if (this.stackType != StackType.INT) {
            throw new IllegalArgumentException();
        }
        if (other.stackType != StackType.INT) {
            throw new IllegalArgumentException();
        }
        return this.ordinal() - other.ordinal();
    }

    public int compareAllPriorityTo(RawJavaType other) {
        return this.ordinal() - other.ordinal();
    }

    @Override
    public boolean isUsableType() {
        return this.usableType;
    }

    public boolean isNumber() {
        return this.isNumber;
    }

    @Override
    public RawJavaType getRawTypeOfSimpleType() {
        return this;
    }

    @Override
    public JavaTypeInstance removeAnArrayIndirection() {
        return VOID;
    }

    @Override
    public JavaTypeInstance getArrayStrippedType() {
        return this;
    }

    @Override
    public JavaTypeInstance getDeGenerifiedType() {
        return this;
    }

    @Override
    public int getNumArrayDimensions() {
        return 0;
    }

    @Override
    public String getRawName() {
        return this.name;
    }

    @Override
    public String getRawName(IllegalIdentifierDump iid) {
        return this.getRawName();
    }

    @Override
    public InnerClassInfo getInnerClassHereInfo() {
        return InnerClassInfo.NOT;
    }

    @Override
    public BindingSuperContainer getBindingSupers() {
        return null;
    }

    private boolean implicitlyCastsTo(RawJavaType other) {
        if (other == this) {
            return true;
        }
        Set<RawJavaType> tgt = implicitCasts.get(this);
        if (tgt == null) {
            return false;
        }
        return tgt.contains(other);
    }

    @Override
    public boolean implicitlyCastsTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        if (other instanceof RawJavaType) {
            return this.implicitlyCastsTo((RawJavaType)other);
        }
        if (this == NULL) {
            return true;
        }
        if (this == REF) {
            return true;
        }
        if (other instanceof JavaGenericPlaceholderTypeInstance) {
            return true;
        }
        if (other instanceof JavaRefTypeInstance) {
            if (other == TypeConstants.OBJECT) {
                return true;
            }
            RawJavaType tgt = RawJavaType.getUnboxedTypeFor(other);
            if (tgt == null) {
                if (other.getRawName().equals("java.lang.Number")) {
                    return this.isNumber;
                }
                return false;
            }
            return this.equals(tgt);
        }
        return false;
    }

    @Override
    public boolean impreciseCanCastTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        if (this.boxedName != null && other instanceof JavaRefTypeInstance) {
            RawJavaType tgt = RawJavaType.getUnboxedTypeFor(other);
            if (tgt == null) {
                if (other == TypeConstants.OBJECT) {
                    return true;
                }
                if (other.getRawName().equals("java.lang.Number")) {
                    return this.isNumber;
                }
                return false;
            }
            return this.implicitlyCastsTo(tgt) || tgt.implicitlyCastsTo(this);
        }
        return true;
    }

    @Override
    public boolean correctCanCastTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        return this.impreciseCanCastTo(other, gtb);
    }

    @Override
    public String suggestVarName() {
        return this.suggestedVarName;
    }

    @Override
    public void dumpInto(Dumper d, TypeUsageInformation typeUsageInformation, TypeContext typeContext) {
        if (this == NULL) {
            TypeConstants.OBJECT.dumpInto(d, typeUsageInformation, typeContext);
            return;
        }
        d.print(this.toString());
    }

    @Override
    public void collectInto(TypeUsageCollector typeUsageCollector) {
    }

    public String toString() {
        return this.name;
    }

    static {
        implicitCasts = MapFactory.newMap();
        boxingTypes = MapFactory.newMap();
        podLookup = MapFactory.newMap();
        implicitCasts.put(FLOAT, SetFactory.newSet(DOUBLE));
        implicitCasts.put(LONG, SetFactory.newSet(FLOAT, DOUBLE));
        implicitCasts.put(INT, SetFactory.newSet(LONG, FLOAT, DOUBLE));
        implicitCasts.put(CHAR, SetFactory.newSet(INT, LONG, FLOAT, DOUBLE));
        implicitCasts.put(SHORT, SetFactory.newSet(INT, LONG, FLOAT, DOUBLE));
        implicitCasts.put(BYTE, SetFactory.newSet(SHORT, INT, LONG, FLOAT, DOUBLE));
        for (RawJavaType type : RawJavaType.values()) {
            if (type.boxedName != null) {
                boxingTypes.put(type.boxedName, type);
            }
            if (!type.usableType) continue;
            podLookup.put(type.name, type);
        }
        podLookup.put(RawJavaType.VOID.name, VOID);
    }

    private class Annotated
    implements JavaAnnotatedTypeInstance {
        private final List<AnnotationTableEntry> entries = ListFactory.newList();

        private Annotated() {
        }

        @Override
        public JavaAnnotatedTypeIterator pathIterator() {
            return new Iterator();
        }

        @Override
        public Dumper dump(Dumper d) {
            for (AnnotationTableEntry entry : this.entries) {
                entry.dump(d);
                d.print(' ');
            }
            d.dump(RawJavaType.this);
            return d;
        }

        private class Iterator
        extends JavaAnnotatedTypeIterator.BaseAnnotatedTypeIterator {
            private Iterator() {
            }

            @Override
            public void apply(AnnotationTableEntry entry) {
                Annotated.this.entries.add(entry);
            }
        }
    }
}

