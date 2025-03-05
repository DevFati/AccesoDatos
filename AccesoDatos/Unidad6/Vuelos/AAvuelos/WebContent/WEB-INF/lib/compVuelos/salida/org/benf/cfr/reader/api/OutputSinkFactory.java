/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.api;

import java.util.Collection;
import java.util.List;
import org.benf.cfr.reader.api.SinkReturns;

public interface OutputSinkFactory {
    public List<SinkClass> getSupportedSinks(SinkType var1, Collection<SinkClass> var2);

    public <T> Sink<T> getSink(SinkType var1, SinkClass var2);

    public static interface Sink<T> {
        public void write(T var1);
    }

    public static enum SinkType {
        JAVA,
        SUMMARY,
        PROGRESS,
        EXCEPTION,
        LINENUMBER;

    }

    public static enum SinkClass {
        STRING(String.class),
        DECOMPILED(SinkReturns.Decompiled.class),
        DECOMPILED_MULTIVER(SinkReturns.DecompiledMultiVer.class),
        EXCEPTION_MESSAGE(SinkReturns.ExceptionMessage.class),
        TOKEN_STREAM(SinkReturns.Token.class),
        LINE_NUMBER_MAPPING(SinkReturns.LineNumberMapping.class);

        public final Class<?> sinkClass;

        private SinkClass(Class<?> sinkClass) {
            this.sinkClass = sinkClass;
        }
    }
}

