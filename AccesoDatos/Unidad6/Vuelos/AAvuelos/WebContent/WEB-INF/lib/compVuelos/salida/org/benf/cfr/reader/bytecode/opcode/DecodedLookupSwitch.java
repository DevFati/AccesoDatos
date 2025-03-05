/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.opcode;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.benf.cfr.reader.bytecode.opcode.DecodedSwitch;
import org.benf.cfr.reader.bytecode.opcode.DecodedSwitchEntry;
import org.benf.cfr.reader.util.bytestream.BaseByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class DecodedLookupSwitch
implements DecodedSwitch {
    private static final int OFFSET_OF_DEFAULT = 0;
    private static final int OFFSET_OF_NUMPAIRS = 4;
    private static final int OFFSET_OF_PAIRS = 8;
    private final List<DecodedSwitchEntry> jumpTargets;

    public DecodedLookupSwitch(byte[] data, int offsetOfOriginalInstruction) {
        int curoffset = offsetOfOriginalInstruction + 1;
        int overflow = curoffset % 4;
        int offset = overflow > 0 ? 4 - overflow : 0;
        BaseByteData bd = new BaseByteData(data);
        int defaultvalue = bd.getS4At(offset + 0);
        int numpairs = bd.getS4At(offset + 4);
        Map<Integer, List<Integer>> uniqueTargets = MapFactory.newLazyMap(new TreeMap(), new UnaryFunction<Integer, List<Integer>>(){

            @Override
            public List<Integer> invoke(Integer arg) {
                return ListFactory.newList();
            }
        });
        uniqueTargets.get(defaultvalue).add(null);
        for (int x = 0; x < numpairs; ++x) {
            int value = bd.getS4At(offset + 8 + x * 8);
            int target = bd.getS4At(offset + 8 + x * 8 + 4);
            if (target == defaultvalue) continue;
            uniqueTargets.get(target).add(value);
        }
        this.jumpTargets = ListFactory.newList();
        for (Map.Entry<Integer, List<Integer>> entry : uniqueTargets.entrySet()) {
            this.jumpTargets.add(new DecodedSwitchEntry(entry.getValue(), entry.getKey()));
        }
    }

    @Override
    public List<DecodedSwitchEntry> getJumpTargets() {
        return this.jumpTargets;
    }
}

