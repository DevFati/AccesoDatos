/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.GetClassTest;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;

public class GetClassTestInnerConstructor
implements GetClassTest {
    public static GetClassTest INSTANCE = new GetClassTestInnerConstructor();

    private GetClassTestInnerConstructor() {
    }

    @Override
    public JVMInstr getInstr() {
        return JVMInstr.INVOKESPECIAL;
    }

    @Override
    public boolean test(ClassFile classFile, Op02WithProcessedDataAndRefs item) {
        ConstantPoolEntryMethodRef function = (ConstantPoolEntryMethodRef)item.getCpEntries()[0];
        if (!function.getName().equals("<init>")) {
            return false;
        }
        JavaTypeInstance initType = function.getClassEntry().getTypeInstance();
        InnerClassInfo innerClassInfo = initType.getInnerClassHereInfo();
        return innerClassInfo.isInnerClass();
    }
}

