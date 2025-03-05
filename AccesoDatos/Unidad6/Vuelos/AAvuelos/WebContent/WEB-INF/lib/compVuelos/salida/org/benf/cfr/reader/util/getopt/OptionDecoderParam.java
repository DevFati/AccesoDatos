/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.getopt;

import org.benf.cfr.reader.util.functors.TrinaryFunction;
import org.benf.cfr.reader.util.getopt.Options;

public interface OptionDecoderParam<T, ARG>
extends TrinaryFunction<String, ARG, Options, T> {
    public String getRangeDescription();

    public String getDefaultValue();
}

