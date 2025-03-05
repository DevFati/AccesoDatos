/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.getopt;

import java.util.List;
import org.benf.cfr.reader.util.getopt.OptionDecoderParam;

public interface PermittedOptionProvider {
    public List<String> getFlags();

    public List<? extends ArgumentParam<?, ?>> getArguments();

    public static class Argument<X>
    extends ArgumentParam<X, Void> {
        Argument(String name, OptionDecoderParam<X, Void> fn, String help, boolean hidden) {
            super(name, fn, help, hidden);
        }

        Argument(String name, OptionDecoderParam<X, Void> fn, String help) {
            super(name, fn, help, false);
        }
    }

    public static class ArgumentParam<X, InputType> {
        private final String name;
        private final OptionDecoderParam<X, InputType> fn;
        private final String help;
        private final boolean hidden;

        ArgumentParam(String name, OptionDecoderParam<X, InputType> fn, String help) {
            this(name, fn, help, false);
        }

        ArgumentParam(String name, OptionDecoderParam<X, InputType> fn, String help, boolean hidden) {
            this.name = name;
            this.fn = fn;
            this.help = help;
            this.hidden = hidden;
        }

        public String getName() {
            return this.name;
        }

        OptionDecoderParam<X, InputType> getFn() {
            return this.fn;
        }

        boolean isHidden() {
            return this.hidden;
        }

        String describe() {
            String defaultVal;
            StringBuilder sb = new StringBuilder();
            sb.append("'").append(this.name).append("':\n\n");
            sb.append(this.help).append('\n');
            String range = this.fn.getRangeDescription();
            if (range != null && !range.isEmpty()) {
                sb.append("\nRange : ").append(range).append("\n");
            }
            if ((defaultVal = this.fn.getDefaultValue()) != null && !defaultVal.isEmpty()) {
                sb.append("\nDefault : ").append(defaultVal).append("\n");
            }
            return sb.toString();
        }

        String shortDescribe() {
            StringBuilder sb = new StringBuilder();
            String defaultVal = this.fn.getDefaultValue();
            String range = this.fn.getRangeDescription();
            if (range != null) {
                range = range.split("\n")[0];
            }
            if (range != null && !range.isEmpty()) {
                sb.append(" (").append(range).append(") ");
            }
            if (defaultVal != null && !defaultVal.isEmpty()) {
                sb.append(" default: ").append(defaultVal);
            }
            return sb.toString();
        }
    }
}

