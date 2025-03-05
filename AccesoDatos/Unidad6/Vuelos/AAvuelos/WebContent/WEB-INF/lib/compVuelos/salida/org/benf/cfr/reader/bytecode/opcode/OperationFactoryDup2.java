/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.opcode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.stack.StackDelta;
import org.benf.cfr.reader.bytecode.analysis.stack.StackDeltaImpl;
import org.benf.cfr.reader.bytecode.analysis.stack.StackSim;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.bytecode.opcode.OperationFactoryDupBase;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.util.bytestream.ByteData;

public class OperationFactoryDup2
extends OperationFactoryDupBase {
    @Override
    public StackDelta getStackDelta(JVMInstr instr, byte[] data, ConstantPoolEntry[] cpEntries, StackSim stackSim, Method method) {
        if (OperationFactoryDup2.getCat(stackSim, 0) == 1) {
            OperationFactoryDup2.checkCat(stackSim, 1, 1);
            return new StackDeltaImpl(OperationFactoryDup2.getStackTypes(stackSim, 0, 1), OperationFactoryDup2.getStackTypes(stackSim, 0, 1, 0, 1));
        }
        return new StackDeltaImpl(OperationFactoryDup2.getStackTypes(stackSim, 0), OperationFactoryDup2.getStackTypes(stackSim, 0, 0));
    }

    @Override
    public Op01WithProcessedDataAndByteJumps createOperation(JVMInstr instr, ByteData bd, ConstantPool cp, int offset) {
        return new Op01WithProcessedDataAndByteJumps(instr, null, null, offset);
    }
}

