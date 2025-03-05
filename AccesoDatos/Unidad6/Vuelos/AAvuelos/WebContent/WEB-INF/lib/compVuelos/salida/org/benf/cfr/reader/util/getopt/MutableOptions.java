/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.getopt;

import java.util.Map;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

public class MutableOptions
implements Options {
    private final Options delegate;
    private Map<String, String> overrides = MapFactory.newMap();

    public MutableOptions(Options delegate) {
        this.delegate = delegate;
    }

    public boolean override(PermittedOptionProvider.ArgumentParam<Troolean, Void> argument, Troolean value) {
        Troolean originalValue = this.delegate.getOption(argument);
        if (originalValue == Troolean.NEITHER) {
            this.overrides.put(argument.getName(), value.toString());
            return true;
        }
        return false;
    }

    public boolean override(PermittedOptionProvider.ArgumentParam<Integer, Void> argument, int value) {
        Integer originalValue = this.delegate.getOption(argument);
        if (originalValue != value) {
            this.overrides.put(argument.getName(), Integer.toString(value));
            return true;
        }
        return false;
    }

    public boolean override(PermittedOptionProvider.ArgumentParam<Boolean, Void> argument, boolean value) {
        Boolean originalValue = this.delegate.getOption(argument);
        if (originalValue != value) {
            this.overrides.put(argument.getName(), Boolean.toString(value));
            return true;
        }
        return false;
    }

    @Override
    public boolean optionIsSet(PermittedOptionProvider.ArgumentParam<?, ?> option) {
        if (this.overrides.containsKey(option.getName())) {
            return true;
        }
        return this.delegate.optionIsSet(option);
    }

    @Override
    public <T> T getOption(PermittedOptionProvider.ArgumentParam<T, Void> option) {
        String override = this.overrides.get(option.getName());
        if (override != null) {
            return (T)option.getFn().invoke(override, null, this);
        }
        return this.delegate.getOption(option);
    }

    @Override
    public <T, A> T getOption(PermittedOptionProvider.ArgumentParam<T, A> option, A arg) {
        String override = this.overrides.get(option.getName());
        if (override != null) {
            return (T)option.getFn().invoke(override, arg, this);
        }
        return this.delegate.getOption(option, arg);
    }
}

