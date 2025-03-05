/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.GetClassTest;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;

public class Op02GetClassRewriter {
    private static final Op02GetClassRewriter INSTANCE = new Op02GetClassRewriter();

    private Op02WithProcessedDataAndRefs getSinglePrev(Op02WithProcessedDataAndRefs item) {
        if (item.getSources().size() != 1) {
            return null;
        }
        Op02WithProcessedDataAndRefs prev = item.getSources().get(0);
        if (prev.getTargets().size() != 1) {
            return null;
        }
        return prev;
    }

    private void tryRemove(ClassFile classFile, Op02WithProcessedDataAndRefs item, GetClassTest classTest) {
        Op02WithProcessedDataAndRefs pop = this.getSinglePrev(item);
        if (pop == null) {
            return;
        }
        if (pop.getInstr() != JVMInstr.POP) {
            return;
        }
        Op02WithProcessedDataAndRefs getClass = this.getSinglePrev(pop);
        if (getClass == null) {
            return;
        }
        if (!this.isGetClass(getClass) && !this.isRequireNonNull(getClass)) {
            return;
        }
        Op02WithProcessedDataAndRefs dup = this.getSinglePrev(getClass);
        if (dup == null) {
            return;
        }
        if (dup.getInstr() != JVMInstr.DUP) {
            return;
        }
        if (!classTest.test(classFile, item)) {
            return;
        }
        dup.nop();
        getClass.nop();
        pop.nop();
    }

    private boolean isGetClass(Op02WithProcessedDataAndRefs item) {
        ConstantPoolEntry[] cpEntries = item.getCpEntries();
        if (cpEntries == null || cpEntries.length == 0) {
            return false;
        }
        ConstantPoolEntry entry = cpEntries[0];
        if (!(entry instanceof ConstantPoolEntryMethodRef)) {
            return false;
        }
        ConstantPoolEntryMethodRef function = (ConstantPoolEntryMethodRef)entry;
        MethodPrototype methodPrototype = function.getMethodPrototype();
        if (!methodPrototype.getName().equals("getClass")) {
            return false;
        }
        if (methodPrototype.getArgs().size() != 0) {
            return false;
        }
        return methodPrototype.getReturnType().getDeGenerifiedType().getRawName().equals("java.lang.Class");
    }

    private boolean isRequireNonNull(Op02WithProcessedDataAndRefs item) {
        ConstantPoolEntry[] cpEntries = item.getCpEntries();
        if (cpEntries == null || cpEntries.length == 0) {
            return false;
        }
        ConstantPoolEntry entry = cpEntries[0];
        if (!(entry instanceof ConstantPoolEntryMethodRef)) {
            return false;
        }
        ConstantPoolEntryMethodRef function = (ConstantPoolEntryMethodRef)entry;
        ConstantPoolEntryClass classEntry = function.getClassEntry();
        String className = classEntry.getTypeInstance().getRawName();
        if (!className.equals("java.util.Objects")) {
            return false;
        }
        MethodPrototype methodPrototype = function.getMethodPrototype();
        if (!methodPrototype.getName().equals("requireNonNull")) {
            return false;
        }
        if (methodPrototype.getArgs().size() != 1) {
            return false;
        }
        return methodPrototype.getReturnType().getDeGenerifiedType().equals(TypeConstants.OBJECT);
    }

    public static void removeInvokeGetClass(ClassFile classFile, List<Op02WithProcessedDataAndRefs> op02list, GetClassTest classTest) {
        JVMInstr testInstr = classTest.getInstr();
        for (Op02WithProcessedDataAndRefs item : op02list) {
            if (item.getInstr() != testInstr) continue;
            INSTANCE.tryRemove(classFile, item, classTest);
        }
    }
}

