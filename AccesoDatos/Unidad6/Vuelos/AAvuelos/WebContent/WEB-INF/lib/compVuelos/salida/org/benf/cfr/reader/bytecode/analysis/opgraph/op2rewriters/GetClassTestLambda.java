/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.GetClassTest;
import org.benf.cfr.reader.bytecode.analysis.types.DynamicInvokeType;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.bootstrap.BootstrapMethodInfo;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryInvokeDynamic;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;

public class GetClassTestLambda
implements GetClassTest {
    public static GetClassTest INSTANCE = new GetClassTestLambda();

    private GetClassTestLambda() {
    }

    @Override
    public JVMInstr getInstr() {
        return JVMInstr.INVOKEDYNAMIC;
    }

    @Override
    public boolean test(ClassFile classFile, Op02WithProcessedDataAndRefs item) {
        ConstantPoolEntry[] cpEntries = item.getCpEntries();
        ConstantPoolEntryInvokeDynamic invokeDynamic = (ConstantPoolEntryInvokeDynamic)cpEntries[0];
        int idx = invokeDynamic.getBootstrapMethodAttrIndex();
        BootstrapMethodInfo bootstrapMethodInfo = classFile.getBootstrapMethods().getBootStrapMethodInfo(idx);
        ConstantPoolEntryMethodRef methodRef = bootstrapMethodInfo.getConstantPoolEntryMethodRef();
        String methodName = methodRef.getName();
        DynamicInvokeType dynamicInvokeType = DynamicInvokeType.lookup(methodName);
        return dynamicInvokeType != DynamicInvokeType.UNKNOWN;
    }
}

