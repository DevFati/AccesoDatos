/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.exceptions;

import java.util.Collections;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.collections.SetFactory;

public class BasicExceptions {
    public static final Set<? extends JavaTypeInstance> instances = Collections.unmodifiableSet(SetFactory.newSet(JavaRefTypeInstance.createTypeConstant("java.lang.AbstractMethodError", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.ArithmeticException", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.ArrayIndexOutOfBoundsException", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.ArrayStoreException", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.ClassCastException", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.IllegalAccessError", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.IllegalMonitorStateException", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.IncompatibleClassChangeError", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.InstantiationError", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.NegativeArraySizeException", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.NullPointerException", new JavaRefTypeInstance[0]), JavaRefTypeInstance.createTypeConstant("java.lang.UnsatisfiedLinkError", new JavaRefTypeInstance[0])));
}

