/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;

public class StackTypes
extends ArrayList<StackType> {
    public static final StackTypes EMPTY = new StackTypes(new StackType[0]);

    public StackTypes(StackType ... stackTypes) {
        super(Arrays.asList(stackTypes));
    }

    public StackTypes(List<StackType> stackTypes) {
        super(stackTypes);
    }
}

