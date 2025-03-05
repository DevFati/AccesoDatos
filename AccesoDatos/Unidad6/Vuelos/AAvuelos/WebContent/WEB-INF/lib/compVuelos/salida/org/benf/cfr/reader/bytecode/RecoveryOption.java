/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode;

import java.util.List;
import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.MutableOptions;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

public abstract class RecoveryOption<T> {
    final UnaryFunction<BytecodeMeta, Boolean> canhelp;
    protected final PermittedOptionProvider.Argument<T> arg;
    protected final T value;
    private final DecompilerComment decompilerComment;

    RecoveryOption(PermittedOptionProvider.Argument<T> arg, T value, UnaryFunction<BytecodeMeta, Boolean> canHelp, DecompilerComment comment) {
        this.arg = arg;
        this.value = value;
        this.decompilerComment = comment;
        this.canhelp = canHelp;
    }

    boolean applyComment(boolean applied, List<DecompilerComment> commentList) {
        if (!applied) {
            return false;
        }
        if (this.decompilerComment == null) {
            return true;
        }
        commentList.add(this.decompilerComment);
        return true;
    }

    public abstract boolean apply(MutableOptions var1, List<DecompilerComment> var2, BytecodeMeta var3);

    public static class ConditionalRO<X, T>
    extends RecoveryOption<T> {
        private final RecoveryOption<T> delegate;
        private final PermittedOptionProvider.ArgumentParam<X, ?> test;
        private final X required;

        public ConditionalRO(PermittedOptionProvider.ArgumentParam<X, ?> test, X required, RecoveryOption<T> delegate) {
            super(null, null, null, null);
            this.delegate = delegate;
            this.required = required;
            this.test = test;
        }

        @Override
        public boolean apply(MutableOptions mutableOptions, List<DecompilerComment> commentList, BytecodeMeta bytecodeMeta) {
            if (mutableOptions.getOption(this.test, null).equals(this.required)) {
                return this.delegate.apply(mutableOptions, commentList, bytecodeMeta);
            }
            return false;
        }
    }

    public static class IntRO
    extends RecoveryOption<Integer> {
        IntRO(PermittedOptionProvider.Argument<Integer> arg, int value) {
            super(arg, value, null, null);
        }

        public IntRO(PermittedOptionProvider.Argument<Integer> arg, int value, DecompilerComment comment) {
            super(arg, value, null, comment);
        }

        IntRO(PermittedOptionProvider.Argument<Integer> arg, int value, UnaryFunction<BytecodeMeta, Boolean> canHelp, DecompilerComment comment) {
            super(arg, value, canHelp, comment);
        }

        @Override
        public boolean apply(MutableOptions mutableOptions, List<DecompilerComment> commentList, BytecodeMeta bytecodeMeta) {
            if (this.canhelp != null && !((Boolean)this.canhelp.invoke(bytecodeMeta)).booleanValue()) {
                return false;
            }
            if (mutableOptions.optionIsSet(this.arg)) {
                return false;
            }
            return this.applyComment(mutableOptions.override(this.arg, (Integer)this.value), commentList);
        }
    }

    public static class BooleanRO
    extends RecoveryOption<Boolean> {
        BooleanRO(PermittedOptionProvider.Argument<Boolean> arg, boolean value) {
            super(arg, value, null, null);
        }

        public BooleanRO(PermittedOptionProvider.Argument<Boolean> arg, boolean value, DecompilerComment comment) {
            super(arg, value, null, comment);
        }

        BooleanRO(PermittedOptionProvider.Argument<Boolean> arg, boolean value, UnaryFunction<BytecodeMeta, Boolean> canHelp, DecompilerComment comment) {
            super(arg, value, canHelp, comment);
        }

        @Override
        public boolean apply(MutableOptions mutableOptions, List<DecompilerComment> commentList, BytecodeMeta bytecodeMeta) {
            if (this.canhelp != null && !((Boolean)this.canhelp.invoke(bytecodeMeta)).booleanValue()) {
                return false;
            }
            return this.applyComment(mutableOptions.override(this.arg, (Boolean)this.value), commentList);
        }
    }

    public static class TrooleanRO
    extends RecoveryOption<Troolean> {
        TrooleanRO(PermittedOptionProvider.Argument<Troolean> arg, Troolean value) {
            super(arg, value, null, null);
        }

        TrooleanRO(PermittedOptionProvider.Argument<Troolean> arg, Troolean value, DecompilerComment comment) {
            super(arg, value, null, comment);
        }

        TrooleanRO(PermittedOptionProvider.Argument<Troolean> arg, Troolean value, UnaryFunction<BytecodeMeta, Boolean> canHelp) {
            super(arg, value, canHelp, null);
        }

        TrooleanRO(PermittedOptionProvider.Argument<Troolean> arg, Troolean value, UnaryFunction<BytecodeMeta, Boolean> canHelp, DecompilerComment comment) {
            super(arg, value, canHelp, comment);
        }

        @Override
        public boolean apply(MutableOptions mutableOptions, List<DecompilerComment> commentList, BytecodeMeta bytecodeMeta) {
            if (this.canhelp != null && !((Boolean)this.canhelp.invoke(bytecodeMeta)).booleanValue()) {
                return false;
            }
            return this.applyComment(mutableOptions.override(this.arg, (Troolean)((Object)this.value)), commentList);
        }
    }
}

