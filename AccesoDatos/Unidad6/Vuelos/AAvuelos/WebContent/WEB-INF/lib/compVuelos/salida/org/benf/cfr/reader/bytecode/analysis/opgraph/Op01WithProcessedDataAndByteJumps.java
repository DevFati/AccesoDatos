/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactory;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.util.UnverifiableJumpException;

public class Op01WithProcessedDataAndByteJumps {
    private final JVMInstr instruction;
    private final byte[] data;
    private final int[] rawTargetOffsets;
    private final ConstantPoolEntry[] constantPoolEntries;
    private final int originalRawOffset;

    public Op01WithProcessedDataAndByteJumps(JVMInstr instruction, byte[] data, int[] rawTargetOffsets, int originalRawOffset) {
        this.instruction = instruction;
        this.data = data;
        this.rawTargetOffsets = rawTargetOffsets;
        this.constantPoolEntries = null;
        this.originalRawOffset = originalRawOffset;
    }

    public Op01WithProcessedDataAndByteJumps(JVMInstr instruction, byte[] data, int[] rawTargetOffsets, int originalRawOffset, ConstantPoolEntry[] constantPoolEntries) {
        this.instruction = instruction;
        this.data = data;
        this.rawTargetOffsets = rawTargetOffsets;
        this.originalRawOffset = originalRawOffset;
        this.constantPoolEntries = constantPoolEntries;
    }

    public JVMInstr getJVMInstr() {
        return this.instruction;
    }

    public int[] getRawTargetOffsets() {
        return this.rawTargetOffsets;
    }

    public int getOriginalRawOffset() {
        return this.originalRawOffset;
    }

    public byte[] getData() {
        return this.data;
    }

    public Op02WithProcessedDataAndRefs createOp2(ConstantPool cp, int index, BytecodeLocFactory locFactory, Method method) {
        return new Op02WithProcessedDataAndRefs(this.instruction, this.data, index, cp, this.constantPoolEntries, this.originalRawOffset, locFactory.at(this.originalRawOffset, method));
    }

    public int[] getAbsoluteIndexJumps(int thisOpByteIndex, Map<Integer, Integer> lutByOffset) {
        int thisOpInstructionIndex = lutByOffset.get(thisOpByteIndex);
        if (this.rawTargetOffsets == null) {
            return new int[]{thisOpInstructionIndex + 1};
        }
        int[] targetIndexes = new int[this.rawTargetOffsets.length];
        for (int x = 0; x < this.rawTargetOffsets.length; ++x) {
            int targetRawAddress = thisOpByteIndex + this.rawTargetOffsets[x];
            Integer targetIndex = lutByOffset.get(targetRawAddress);
            if (targetIndex == null) {
                throw new UnverifiableJumpException();
            }
            targetIndexes[x] = targetIndex;
        }
        return targetIndexes;
    }

    public int getInstructionLength() {
        return this.data == null ? 1 : this.data.length + 1;
    }

    public String toString() {
        return "op1 : " + (Object)((Object)this.instruction) + ", length " + this.getInstructionLength();
    }

    public Integer getAStoreIdx() {
        switch (this.instruction) {
            case ASTORE: {
                return this.data[0];
            }
            case ASTORE_WIDE: {
                throw new UnsupportedOperationException();
            }
            case ASTORE_0: {
                return 0;
            }
            case ASTORE_1: {
                return 1;
            }
            case ASTORE_2: {
                return 2;
            }
            case ASTORE_3: {
                return 3;
            }
        }
        return null;
    }

    public Integer getALoadIdx() {
        switch (this.instruction) {
            case ALOAD: {
                return this.data[0];
            }
            case ALOAD_WIDE: {
                throw new UnsupportedOperationException();
            }
            case ALOAD_0: {
                return 0;
            }
            case ALOAD_1: {
                return 1;
            }
            case ALOAD_2: {
                return 2;
            }
            case ALOAD_3: {
                return 3;
            }
        }
        return null;
    }
}

