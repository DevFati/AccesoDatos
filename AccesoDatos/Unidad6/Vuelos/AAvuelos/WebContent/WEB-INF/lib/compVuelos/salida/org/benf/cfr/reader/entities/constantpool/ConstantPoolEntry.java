/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.constantpool;

import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.output.Dumper;

public interface ConstantPoolEntry {
    public long getRawByteLength();

    public void dump(Dumper var1);

    public static enum Type {
        CPT_UTF8,
        CPT_Integer,
        CPT_Float,
        CPT_Long,
        CPT_Double,
        CPT_Class,
        CPT_String,
        CPT_FieldRef,
        CPT_MethodRef,
        CPT_InterfaceMethodRef,
        CPT_NameAndType,
        CPT_MethodHandle,
        CPT_MethodType,
        CPT_DynamicInfo,
        CPT_InvokeDynamic,
        CPT_ModuleInfo,
        CPT_PackageInfo;

        private static final byte VAL_UTF8 = 1;
        private static final byte VAL_Integer = 3;
        private static final byte VAL_Float = 4;
        private static final byte VAL_Long = 5;
        private static final byte VAL_Double = 6;
        private static final byte VAL_Class = 7;
        private static final byte VAL_String = 8;
        private static final byte VAL_FieldRef = 9;
        private static final byte VAL_MethodRef = 10;
        private static final byte VAL_InterfaceMethodRef = 11;
        private static final byte VAL_NameAndType = 12;
        private static final byte VAL_MethodHandle = 15;
        private static final byte VAL_MethodType = 16;
        private static final byte VAL_DynamicInfo = 17;
        private static final byte VAL_InvokeDynamic = 18;
        private static final byte VAL_ModuleInfo = 19;
        private static final byte VAL_PackageInfo = 20;

        public static Type get(byte val) {
            switch (val) {
                case 1: {
                    return CPT_UTF8;
                }
                case 3: {
                    return CPT_Integer;
                }
                case 4: {
                    return CPT_Float;
                }
                case 5: {
                    return CPT_Long;
                }
                case 6: {
                    return CPT_Double;
                }
                case 7: {
                    return CPT_Class;
                }
                case 8: {
                    return CPT_String;
                }
                case 9: {
                    return CPT_FieldRef;
                }
                case 10: {
                    return CPT_MethodRef;
                }
                case 11: {
                    return CPT_InterfaceMethodRef;
                }
                case 12: {
                    return CPT_NameAndType;
                }
                case 15: {
                    return CPT_MethodHandle;
                }
                case 16: {
                    return CPT_MethodType;
                }
                case 17: {
                    return CPT_DynamicInfo;
                }
                case 18: {
                    return CPT_InvokeDynamic;
                }
                case 19: {
                    return CPT_ModuleInfo;
                }
                case 20: {
                    return CPT_PackageInfo;
                }
            }
            throw new ConfusedCFRException("Invalid constant pool entry type : " + val);
        }
    }
}

