/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.io.BufferedOutputStream;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.MovableDumperContext;
import org.benf.cfr.reader.util.output.StreamDumper;

public class StdIODumper
extends StreamDumper {
    StdIODumper(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context) {
        super(typeUsageInformation, options, illegalIdentifierDump, context);
    }

    private StdIODumper(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context, Set<JavaTypeInstance> emitted) {
        super(typeUsageInformation, options, illegalIdentifierDump, context, emitted);
    }

    @Override
    protected void write(String s) {
        System.out.print(s);
    }

    @Override
    public void addSummaryError(Method method, String s) {
    }

    @Override
    public void close() {
    }

    @Override
    public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
        return new StdIODumper(innerclassTypeUsageInformation, this.options, this.illegalIdentifierDump, this.context, this.emitted);
    }

    @Override
    public BufferedOutputStream getAdditionalOutputStream(String description) {
        return new BufferedOutputStream(System.out);
    }
}

