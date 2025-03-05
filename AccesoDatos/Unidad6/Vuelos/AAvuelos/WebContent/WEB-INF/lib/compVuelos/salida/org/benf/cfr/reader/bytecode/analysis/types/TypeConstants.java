/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;

public interface TypeConstants {
    public static final String objectsName = "java.util.Objects";
    public static final String throwableName = "java.lang.Throwable";
    public static final String stringName = "java.lang.String";
    public static final String charSequenceName = "java.lang.CharSequence";
    public static final String stringBuilderName = "java.lang.StringBuilder";
    public static final String stringBufferName = "java.lang.StringBuffer";
    public static final String className = "java.lang.Class";
    public static final String objectName = "java.lang.Object";
    public static final String methodHandleName = "java.lang.invoke.MethodHandle";
    public static final String methodHandlesName = "java.lang.invoke.MethodHandles";
    public static final String methodHandlesLookupName = "java.lang.invoke.MethodHandles$Lookup";
    public static final String methodTypeName = "java.lang.invoke.MethodType";
    public static final String lambdaMetaFactoryName = "java.lang.invoke.LambdaMetafactory";
    public static final String stringConcatFactoryName = "java.lang.invoke.StringConcatFactory";
    public static final JavaRefTypeInstance OBJECT = JavaRefTypeInstance.createTypeConstant("java.lang.Object", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance OBJECTS = JavaRefTypeInstance.createTypeConstant("java.util.Objects", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance ENUM = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.Enum");
    public static final JavaRefTypeInstance ASSERTION_ERROR = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.AssertionError");
    public static final JavaRefTypeInstance CHAR_SEQUENCE = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.CharSequence");
    public static final JavaRefTypeInstance STRING = JavaRefTypeInstance.createTypeConstant("java.lang.String", OBJECT, CHAR_SEQUENCE);
    public static final JavaRefTypeInstance CLASS = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.Class");
    public static final JavaRefTypeInstance ITERABLE = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.Iterable");
    public static final JavaRefTypeInstance CLOSEABLE = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.io.Closeable");
    public static final JavaRefTypeInstance SERIALIZABLE = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.io.Serializable");
    public static final JavaRefTypeInstance THROWABLE = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.Throwable");
    public static final JavaRefTypeInstance AUTO_CLOSEABLE = JavaRefTypeInstance.createTypeConstant("java.lang.AutoCloseable", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance SUPPLIER = JavaRefTypeInstance.createTypeConstant("java.util.function.Supplier", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance SCALA_SIGNATURE = JavaRefTypeInstance.createTypeConstant("scala.reflect.ScalaSignature", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance NOCLASSDEFFOUND_ERROR = JavaRefTypeInstance.createTypeConstant("java.lang.NoClassDefFoundError", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance NOSUCHMETHOD_EXCEPTION = JavaRefTypeInstance.createTypeConstant("java.lang.NoSuchMethodException", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance ILLEGALACCESS_EXCEPTION = JavaRefTypeInstance.createTypeConstant("java.lang.IllegalAccessException", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance ILLEGALARGUMENT_EXCEPTION = JavaRefTypeInstance.createTypeConstant("java.lang.IllegalArgumentException", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance COMPARABLE = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.Comparable");
    public static final JavaRefTypeInstance MATH = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.Math");
    public static final JavaRefTypeInstance OVERRIDE = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.Override");
    public static final JavaRefTypeInstance RECORD = JavaRefTypeInstance.createTypeConstant("java.lang.Record", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance OBJECTMETHODS = JavaRefTypeInstance.createTypeConstant("java.lang.runtime.ObjectMethods", new JavaRefTypeInstance[0]);
    public static final JavaRefTypeInstance METHOD_HANDLE = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.invoke.MethodHandle");
    public static final JavaRefTypeInstance METHOD_HANDLES = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.invoke.MethodHandles");
    public static final JavaRefTypeInstance METHOD_HANDLES$LOOKUP = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.invoke.MethodHandles$Lookup");
    public static final JavaRefTypeInstance METHOD_TYPE = JavaRefTypeInstance.createTypeConstantWithObjectSuper("java.lang.invoke.MethodType");
    public static final String boxingNameBoolean = "java.lang.Boolean";
    public static final String boxingNameByte = "java.lang.Byte";
    public static final String boxingNameShort = "java.lang.Short";
    public static final String boxingNameChar = "java.lang.Character";
    public static final String boxingNameInt = "java.lang.Integer";
    public static final String boxingNameLong = "java.lang.Long";
    public static final String boxingNameFloat = "java.lang.Float";
    public static final String boxingNameDouble = "java.lang.Double";
    public static final String boxingNameNumber = "java.lang.Number";
    public static final JavaRefTypeInstance NUMBER = JavaRefTypeInstance.createTypeConstant("java.lang.Number", OBJECT, SERIALIZABLE);
    public static final JavaRefTypeInstance INTEGER = JavaRefTypeInstance.createTypeConstant("java.lang.Integer", NUMBER, COMPARABLE);
    public static final JavaRefTypeInstance SHORT = JavaRefTypeInstance.createTypeConstant("java.lang.Short", NUMBER, COMPARABLE);
    public static final JavaRefTypeInstance LONG = JavaRefTypeInstance.createTypeConstant("java.lang.Long", NUMBER, COMPARABLE);
    public static final JavaRefTypeInstance DOUBLE = JavaRefTypeInstance.createTypeConstant("java.lang.Double", NUMBER, COMPARABLE);
    public static final JavaRefTypeInstance FLOAT = JavaRefTypeInstance.createTypeConstant("java.lang.Float", NUMBER, COMPARABLE);
    public static final String runtimeExceptionPath = "java/lang/RuntimeException.class";
    public static final String fromMethodDescriptorString = "fromMethodDescriptorString";
}

