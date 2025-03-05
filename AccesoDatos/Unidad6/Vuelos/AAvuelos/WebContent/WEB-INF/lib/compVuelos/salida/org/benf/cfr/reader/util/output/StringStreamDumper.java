/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.io.BufferedOutputStream;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.MethodErrorCollector;
import org.benf.cfr.reader.util.output.MovableDumperContext;
import org.benf.cfr.reader.util.output.StreamDumper;
import org.benf.cfr.reader.util.output.TypeOverridingDumper;

public class StringStreamDumper
extends StreamDumper {
    private final MethodErrorCollector methodErrorCollector;
    private final StringBuilder stringBuilder;

    public StringStreamDumper(MethodErrorCollector methodErrorCollector, StringBuilder sb, TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump) {
        this(methodErrorCollector, sb, typeUsageInformation, options, illegalIdentifierDump, new MovableDumperContext());
    }

    public StringStreamDumper(MethodErrorCollector methodErrorCollector, StringBuilder sb, TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context) {
        super(typeUsageInformation, options, illegalIdentifierDump, context);
        this.methodErrorCollector = methodErrorCollector;
        this.stringBuilder = sb;
    }

    @Override
    protected void write(String s) {
        this.stringBuilder.append(s);
    }

    @Override
    public void close() {
    }

    @Override
    public void addSummaryError(Method method, String s) {
        this.methodErrorCollector.addSummaryError(method, s);
    }

    @Override
    public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
        return new TypeOverridingDumper(this, innerclassTypeUsageInformation);
    }

    @Override
    public BufferedOutputStream getAdditionalOutputStream(String description) {
        throw new IllegalStateException();
    }
}

