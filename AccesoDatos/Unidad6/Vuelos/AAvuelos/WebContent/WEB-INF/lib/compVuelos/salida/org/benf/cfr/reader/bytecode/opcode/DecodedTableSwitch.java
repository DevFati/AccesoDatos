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

public class DecodedTableSwitch
implements DecodedSwitch {
    private static final int OFFSET_OF_DEFAULT = 0;
    private static final int OFFSET_OF_LOWBYTE = 4;
    private static final int OFFSET_OF_HIGHBYTE = 8;
    private static final int OFFSET_OF_OFFSETS = 12;
    private final List<DecodedSwitchEntry> jumpTargets;

    public DecodedTableSwitch(byte[] data, int offsetOfOriginalInstruction) {
        int curoffset = offsetOfOriginalInstruction + 1;
        int overflow = curoffset % 4;
        int offset = overflow > 0 ? 4 - overflow : 0;
        BaseByteData bd = new BaseByteData(data);
        int defaultvalue = bd.getS4At(offset + 0);
        int lowvalue = bd.getS4At(offset + 4);
        int highvalue = bd.getS4At(offset + 8);
        int numoffsets = highvalue - lowvalue + 1;
        int defaultTarget = defaultvalue;
        int startValue = lowvalue;
        Map<Integer, List<Integer>> uniqueTargets = MapFactory.newLazyMap(new TreeMap(), new UnaryFunction<Integer, List<Integer>>(){

            @Override
            public List<Integer> invoke(Integer arg) {
                return ListFactory.newList();
            }
        });
        uniqueTargets.get(defaultTarget).add(null);
        for (int x = 0; x < numoffsets; ++x) {
            int target = bd.getS4At(offset + 12 + x * 4);
            if (target == defaultTarget) continue;
            uniqueTargets.get(target).add(startValue + x);
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

