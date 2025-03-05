/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.literal;

import org.benf.cfr.reader.bytecode.analysis.parse.utils.QuotingUtils;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryDouble;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryFloat;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryInteger;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryLong;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodHandle;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodType;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryString;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.ToStringDumper;

public class TypedLiteral
implements TypeUsageCollectable,
Dumpable {
    private final InferredJavaType inferredJavaType;
    private final LiteralType type;
    private final Object value;

    protected TypedLiteral(LiteralType type, InferredJavaType inferredJavaType, Object value) {
        this.type = type;
        this.value = value;
        this.inferredJavaType = inferredJavaType;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (this.type == LiteralType.Class) {
            collector.collect((JavaTypeInstance)this.value);
        }
    }

    private static String integerName(Object o, FormatHint formatHint) {
        if (!(o instanceof Integer)) {
            return o.toString();
        }
        int i = (Integer)o;
        if ((long)i > 1048575L || formatHint == FormatHint.Hex) {
            String hex = Integer.toHexString(i).toUpperCase();
            if (formatHint == FormatHint.Hex || TypedLiteral.hexTest(hex)) {
                return "0x" + hex;
            }
        }
        return o.toString();
    }

    public boolean getBoolValue() {
        if (this.type != LiteralType.Integer) {
            throw new IllegalStateException("Expecting integral literal");
        }
        Integer i = (Integer)this.value;
        return i != 0;
    }

    public long getLongValue() {
        if (this.type != LiteralType.Long) {
            throw new IllegalStateException("Expecting long literal");
        }
        return (Long)this.value;
    }

    public int getIntValue() {
        if (this.type != LiteralType.Integer) {
            throw new IllegalStateException("Expecting integral literal");
        }
        return (Integer)this.value;
    }

    public float getFloatValue() {
        if (this.type != LiteralType.Float) {
            throw new IllegalStateException("Expecting float literal");
        }
        return ((Float)this.value).floatValue();
    }

    public double getDoubleValue() {
        if (this.type != LiteralType.Double) {
            throw new IllegalStateException("Expecting double literal");
        }
        return (Double)this.value;
    }

    public Boolean getMaybeBoolValue() {
        if (this.type != LiteralType.Integer) {
            return null;
        }
        Integer i = (Integer)this.value;
        return i == 0 ? Boolean.FALSE : Boolean.TRUE;
    }

    public ConstantPoolEntryMethodHandle getMethodHandle() {
        if (this.type != LiteralType.MethodHandle) {
            throw new IllegalStateException("Expecting MethodHandle literal");
        }
        return (ConstantPoolEntryMethodHandle)this.value;
    }

    public JavaTypeInstance getClassValue() {
        if (this.type != LiteralType.Class) {
            throw new IllegalStateException("Expecting Class literal");
        }
        return (JavaTypeInstance)this.value;
    }

    private static String charName(Object o) {
        if (!(o instanceof Integer)) {
            throw new ConfusedCFRException("Expecting char-as-int");
        }
        int i = (Integer)o;
        char c = (char)i;
        switch (c) {
            case '\"': {
                return "'\\\"'";
            }
            case '\r': {
                return "'\\r'";
            }
            case '\n': {
                return "'\\n'";
            }
            case '\t': {
                return "'\\t'";
            }
            case '\b': {
                return "'\\b'";
            }
            case '\f': {
                return "'\\f'";
            }
            case '\\': {
                return "'\\\\'";
            }
            case '\'': {
                return "'\\''";
            }
        }
        if (i < 32 || i >= 254) {
            return "'\\u" + String.format("%04x", i) + "'";
        }
        return "'" + c + "'";
    }

    private static String boolName(Object o) {
        if (!(o instanceof Integer)) {
            throw new ConfusedCFRException("Expecting boolean-as-int");
        }
        int i = (Integer)o;
        switch (i) {
            case 0: {
                return "false";
            }
            case 1: {
                return "true";
            }
        }
        return i + " != 0";
    }

    private static boolean hexTest(String hex) {
        int diff = 0;
        byte[] bytes = hex.getBytes();
        byte[] count = new byte[16];
        for (byte b : bytes) {
            if (b >= 48 && b <= 57) {
                int n = b - 48;
                count[n] = (byte)(count[n] + 1);
                if (count[n] != 1) continue;
                ++diff;
                continue;
            }
            if (b >= 65 && b <= 70) {
                int n = b - 65 + 10;
                count[n] = (byte)(count[n] + 1);
                if (count[n] != 1) continue;
                ++diff;
                continue;
            }
            return false;
        }
        return diff <= 3;
    }

    private static String longName(Object o, FormatHint formatHint) {
        if (!(o instanceof Long)) {
            return o.toString();
        }
        long l = (Long)o;
        String longString = null;
        if (l > 1048575L || formatHint == FormatHint.Hex) {
            String hex = Long.toHexString(l).toUpperCase();
            if (formatHint == FormatHint.Hex || TypedLiteral.hexTest(hex)) {
                longString = "0x" + hex;
            }
        }
        if (longString == null) {
            longString = o.toString();
        }
        return longString + "L";
    }

    private static String methodHandleName(Object o) {
        ConstantPoolEntryMethodHandle methodHandle = (ConstantPoolEntryMethodHandle)o;
        return methodHandle.getLiteralName();
    }

    private static String methodTypeName(Object o) {
        ConstantPoolEntryMethodType methodType = (ConstantPoolEntryMethodType)o;
        return methodType.getDescriptor().getValue();
    }

    @Override
    public Dumper dump(Dumper d) {
        return this.dumpWithHint(d, FormatHint.None);
    }

    public Dumper dumpWithHint(Dumper d, FormatHint hint) {
        switch (this.type) {
            case String: {
                return d.literal((String)this.value, this.value);
            }
            case NullObject: {
                return d.keyword("null");
            }
            case Integer: {
                switch (this.inferredJavaType.getRawType()) {
                    case CHAR: {
                        return d.literal(TypedLiteral.charName(this.value), this.value);
                    }
                    case BOOLEAN: {
                        return d.literal(TypedLiteral.boolName(this.value), this.value);
                    }
                }
                return d.literal(TypedLiteral.integerName(this.value, hint), this.value);
            }
            case Long: {
                return d.literal(TypedLiteral.longName(this.value, hint), this.value);
            }
            case MethodType: {
                return d.print(TypedLiteral.methodTypeName(this.value));
            }
            case MethodHandle: {
                return d.print(TypedLiteral.methodHandleName(this.value));
            }
            case Class: {
                return d.dump((JavaTypeInstance)this.value).print(".class");
            }
            case Double: {
                return d.literal(this.value.toString(), this.value);
            }
            case Float: {
                return d.literal(this.value.toString() + "f", this.value);
            }
        }
        return d.print(this.value.toString());
    }

    public String toString() {
        return ToStringDumper.toString(this);
    }

    public static TypedLiteral getLong(long v) {
        return new TypedLiteral(LiteralType.Long, new InferredJavaType(RawJavaType.LONG, InferredJavaType.Source.LITERAL), v);
    }

    public static TypedLiteral getInt(int v, InferredJavaType type) {
        return new TypedLiteral(LiteralType.Integer, type, v);
    }

    public static TypedLiteral getInt(int v, RawJavaType type) {
        return new TypedLiteral(LiteralType.Integer, new InferredJavaType(type, InferredJavaType.Source.LITERAL), v);
    }

    public static TypedLiteral getInt(int v) {
        return TypedLiteral.getInt(v, new InferredJavaType(RawJavaType.INT, InferredJavaType.Source.LITERAL));
    }

    public static TypedLiteral getShort(int v) {
        return TypedLiteral.getInt(v, new InferredJavaType(RawJavaType.SHORT, InferredJavaType.Source.LITERAL));
    }

    public static TypedLiteral getChar(int v) {
        return TypedLiteral.getInt(v, new InferredJavaType(RawJavaType.CHAR, InferredJavaType.Source.LITERAL));
    }

    public static TypedLiteral getBoolean(int v) {
        return TypedLiteral.getInt(v, new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.LITERAL));
    }

    public static TypedLiteral getDouble(double v) {
        return new TypedLiteral(LiteralType.Double, new InferredJavaType(RawJavaType.DOUBLE, InferredJavaType.Source.LITERAL), v);
    }

    public static TypedLiteral getFloat(float v) {
        return new TypedLiteral(LiteralType.Float, new InferredJavaType(RawJavaType.FLOAT, InferredJavaType.Source.LITERAL), Float.valueOf(v));
    }

    public static TypedLiteral getClass(JavaTypeInstance v) {
        JavaGenericRefTypeInstance tgt = new JavaGenericRefTypeInstance(TypeConstants.CLASS, ListFactory.newImmutableList(v));
        return new TypedLiteral(LiteralType.Class, new InferredJavaType(tgt, InferredJavaType.Source.LITERAL), v);
    }

    public static TypedLiteral getString(String v) {
        return new TypedLiteral(LiteralType.String, new InferredJavaType(TypeConstants.STRING, InferredJavaType.Source.LITERAL), v);
    }

    public static TypedLiteral getNull() {
        return new TypedLiteral(LiteralType.NullObject, new InferredJavaType(RawJavaType.NULL, InferredJavaType.Source.LITERAL), null);
    }

    private static TypedLiteral getMethodHandle(ConstantPoolEntryMethodHandle methodHandle, ConstantPool cp) {
        JavaRefTypeInstance typeInstance = cp.getClassCache().getRefClassFor("java.lang.invoke.MethodHandle");
        return new TypedLiteral(LiteralType.MethodHandle, new InferredJavaType(typeInstance, InferredJavaType.Source.LITERAL), methodHandle);
    }

    private static TypedLiteral getMethodType(ConstantPoolEntryMethodType methodType, ConstantPool cp) {
        JavaRefTypeInstance typeInstance = cp.getClassCache().getRefClassFor("java.lang.invoke.MethodType");
        return new TypedLiteral(LiteralType.MethodType, new InferredJavaType(typeInstance, InferredJavaType.Source.LITERAL), methodType);
    }

    public static TypedLiteral getConstantPoolEntryUTF8(ConstantPoolEntryUTF8 cpe) {
        return TypedLiteral.getString(QuotingUtils.enquoteString(cpe.getValue()));
    }

    public static TypedLiteral getConstantPoolEntry(ConstantPool cp, ConstantPoolEntry cpe) {
        if (cpe instanceof ConstantPoolEntryDouble) {
            return TypedLiteral.getDouble(((ConstantPoolEntryDouble)cpe).getValue());
        }
        if (cpe instanceof ConstantPoolEntryFloat) {
            return TypedLiteral.getFloat(((ConstantPoolEntryFloat)cpe).getValue());
        }
        if (cpe instanceof ConstantPoolEntryLong) {
            return TypedLiteral.getLong(((ConstantPoolEntryLong)cpe).getValue());
        }
        if (cpe instanceof ConstantPoolEntryInteger) {
            return TypedLiteral.getInt(((ConstantPoolEntryInteger)cpe).getValue());
        }
        if (cpe instanceof ConstantPoolEntryString) {
            return TypedLiteral.getString(((ConstantPoolEntryString)cpe).getValue());
        }
        if (cpe instanceof ConstantPoolEntryClass) {
            return TypedLiteral.getClass(((ConstantPoolEntryClass)cpe).getTypeInstance());
        }
        if (cpe instanceof ConstantPoolEntryMethodHandle) {
            return TypedLiteral.getMethodHandle((ConstantPoolEntryMethodHandle)cpe, cp);
        }
        if (cpe instanceof ConstantPoolEntryMethodType) {
            return TypedLiteral.getMethodType((ConstantPoolEntryMethodType)cpe, cp);
        }
        throw new ConfusedCFRException("Can't turn ConstantPoolEntry into Literal - got " + cpe);
    }

    public static TypedLiteral shrinkTo(TypedLiteral original, RawJavaType tgt) {
        if (original.getType() != LiteralType.Integer) {
            return original;
        }
        if (tgt.getStackType() != StackType.INT) {
            return original;
        }
        Integer i = (Integer)original.value;
        if (i == null) {
            return original;
        }
        switch (tgt) {
            case BOOLEAN: {
                return TypedLiteral.getBoolean(i);
            }
            case CHAR: {
                return TypedLiteral.getChar(i);
            }
        }
        return original;
    }

    public LiteralType getType() {
        return this.type;
    }

    public Object getValue() {
        return this.value;
    }

    public InferredJavaType getInferredJavaType() {
        return this.inferredJavaType;
    }

    public boolean checkIntegerUsage(RawJavaType rawType) {
        if (this.type != LiteralType.Integer) {
            return false;
        }
        int x = this.getIntValue();
        return rawType.inIntRange(x);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TypedLiteral)) {
            return false;
        }
        TypedLiteral other = (TypedLiteral)o;
        return this.type == other.type && (this.value == null ? other.value == null : this.value.equals(other.value));
    }

    public static enum FormatHint {
        None,
        Hex;

    }

    public static enum LiteralType {
        Integer,
        Long,
        Double,
        Float,
        String,
        NullObject,
        Class,
        MethodHandle,
        MethodType;

    }
}

