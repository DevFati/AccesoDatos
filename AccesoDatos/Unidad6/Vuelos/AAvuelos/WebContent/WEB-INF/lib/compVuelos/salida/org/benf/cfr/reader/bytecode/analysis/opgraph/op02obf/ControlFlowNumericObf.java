/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op02obf;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.Method;

public class ControlFlowNumericObf {
    public static ControlFlowNumericObf Instance = new ControlFlowNumericObf();

    public void process(Method method, List<Op02WithProcessedDataAndRefs> op2list) {
        for (int idx = 0; idx < op2list.size(); ++idx) {
            Op02WithProcessedDataAndRefs op = op2list.get(idx);
            JVMInstr instr = op.getInstr();
            if (instr != JVMInstr.IINC) continue;
            this.processOne(op, op2list, idx);
        }
    }

    private void processOne(Op02WithProcessedDataAndRefs op, List<Op02WithProcessedDataAndRefs> op2list, int idx) {
        Op02WithProcessedDataAndRefs next = op.getTargets().get(0);
        if (next != op2list.get(idx + 1)) {
            return;
        }
        if (next.getSources().size() != 1) {
            return;
        }
        if (next.getInstr() != JVMInstr.IINC) {
            return;
        }
        Pair<Integer, Integer> iinc1 = op.getIincInfo();
        Pair<Integer, Integer> iinc2 = next.getIincInfo();
        if (!iinc1.getFirst().equals(iinc2.getFirst())) {
            return;
        }
        int diff = iinc1.getSecond() + iinc2.getSecond();
        if ((byte)diff == diff) {
            Op02WithProcessedDataAndRefs replace = new Op02WithProcessedDataAndRefs(JVMInstr.IINC, new byte[]{(byte)iinc1.getFirst().intValue(), (byte)diff}, op.getIndex(), op.getCp(), null, op.getOriginalRawOffset(), op.getBytecodeLoc());
            op2list.set(idx, replace);
            Op02WithProcessedDataAndRefs.replace(op, replace);
            next.nop();
        }
    }
}

