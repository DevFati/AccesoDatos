/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.api;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.CfrDriverImpl;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public interface CfrDriver {
    public void analyse(List<String> var1);

    public static class Builder {
        ClassFileSource source = null;
        Options builtOptions = null;
        OutputSinkFactory output = null;
        boolean fallbackToDefaultSource = false;

        public Builder withClassFileSource(ClassFileSource source) {
            this.source = source;
            return this;
        }

        public Builder withOverrideClassFileSource(ClassFileSource source) {
            this.source = source;
            this.fallbackToDefaultSource = true;
            return this;
        }

        public Builder withOutputSink(OutputSinkFactory output) {
            this.output = output;
            return this;
        }

        public Builder withOptions(Map<String, String> options) {
            this.builtOptions = OptionsImpl.getFactory().create(options);
            return this;
        }

        public Builder withBuiltOptions(Options options) {
            this.builtOptions = options;
            return this;
        }

        public CfrDriver build() {
            return new CfrDriverImpl(this.source, this.output, this.builtOptions, this.fallbackToDefaultSource);
        }
    }
}

