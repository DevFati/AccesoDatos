/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.getopt;

import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

public class BadParametersException
extends IllegalArgumentException {
    private final PermittedOptionProvider.ArgumentParam<?, ?> option;

    BadParametersException(String s, PermittedOptionProvider.ArgumentParam<?, ?> option) {
        super(s);
        this.option = option;
    }

    @Override
    public String toString() {
        String sb = "While processing argument '" + this.option.getName() + "':\n" + super.getMessage() + "\nValid argument range: " + this.option.getFn().getRangeDescription() + "\n";
        return sb;
    }
}

