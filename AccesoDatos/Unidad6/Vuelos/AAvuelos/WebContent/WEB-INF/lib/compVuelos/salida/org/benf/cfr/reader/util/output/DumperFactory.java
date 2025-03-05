/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.ExceptionDumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.ProgressDumper;
import org.benf.cfr.reader.util.output.SummaryDumper;

public interface DumperFactory {
    public Dumper getNewTopLevelDumper(JavaTypeInstance var1, SummaryDumper var2, TypeUsageInformation var3, IllegalIdentifierDump var4);

    public Dumper wrapLineNoDumper(Dumper var1);

    public ProgressDumper getProgressDumper();

    public SummaryDumper getSummaryDumper();

    public ExceptionDumper getExceptionDumper();

    public DumperFactory getFactoryWithPrefix(String var1, int var2);
}

