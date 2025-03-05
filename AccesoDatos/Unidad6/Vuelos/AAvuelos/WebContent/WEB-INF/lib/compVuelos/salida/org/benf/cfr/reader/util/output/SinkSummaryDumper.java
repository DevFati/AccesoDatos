/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.output.SummaryDumper;

public class SinkSummaryDumper
implements SummaryDumper {
    private final OutputSinkFactory.Sink<String> sink;
    private transient JavaTypeInstance lastControllingType = null;
    private transient Method lastMethod = null;

    SinkSummaryDumper(OutputSinkFactory.Sink<String> sink) {
        this.sink = sink;
    }

    @Override
    public void notify(String message) {
        this.sink.write(message + "\n");
    }

    @Override
    public void notifyError(JavaTypeInstance controllingType, Method method, String error) {
        if (this.lastControllingType != controllingType) {
            this.lastControllingType = controllingType;
            this.lastMethod = null;
            this.sink.write("\n\n" + controllingType.getRawName() + "\n----------------------------\n\n");
        }
        if (method != this.lastMethod) {
            this.sink.write(method.getMethodPrototype().toString() + "\n");
            this.lastMethod = method;
        }
        this.sink.write("  " + error + "\n");
    }

    @Override
    public void close() {
    }
}

