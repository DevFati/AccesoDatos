/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.output.SummaryDumper;

public interface MethodErrorCollector {
    public void addSummaryError(Method var1, String var2);

    public static class SummaryDumperMethodErrorCollector
    implements MethodErrorCollector {
        private final JavaTypeInstance type;
        private final SummaryDumper summaryDumper;

        public SummaryDumperMethodErrorCollector(JavaTypeInstance type, SummaryDumper summaryDumper) {
            this.type = type;
            this.summaryDumper = summaryDumper;
        }

        @Override
        public void addSummaryError(Method method, String s) {
            this.summaryDumper.notifyError(this.type, method, s);
        }
    }
}

