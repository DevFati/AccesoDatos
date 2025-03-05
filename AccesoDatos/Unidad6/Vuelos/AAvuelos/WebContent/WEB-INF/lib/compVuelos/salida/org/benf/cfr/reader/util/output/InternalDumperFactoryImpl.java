/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.state.OsInfo;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerCommentSource;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.BytecodeDumpConsumer;
import org.benf.cfr.reader.util.output.BytecodeTrackingDumper;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.DumperFactory;
import org.benf.cfr.reader.util.output.ExceptionDumper;
import org.benf.cfr.reader.util.output.FileDumper;
import org.benf.cfr.reader.util.output.FileSummaryDumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.MovableDumperContext;
import org.benf.cfr.reader.util.output.NopSummaryDumper;
import org.benf.cfr.reader.util.output.ProgressDumper;
import org.benf.cfr.reader.util.output.ProgressDumperNop;
import org.benf.cfr.reader.util.output.ProgressDumperStdErr;
import org.benf.cfr.reader.util.output.StdErrExceptionDumper;
import org.benf.cfr.reader.util.output.StdIODumper;
import org.benf.cfr.reader.util.output.SummaryDumper;

public class InternalDumperFactoryImpl
implements DumperFactory {
    private final boolean checkDupes;
    private final Set<String> seen = SetFactory.newSet();
    private boolean seenCaseDupe = false;
    private final Options options;
    private final ProgressDumper progressDumper;
    private final String prefix;
    private final AtomicInteger truncCount = new AtomicInteger();

    public InternalDumperFactoryImpl(Options options) {
        this.checkDupes = OsInfo.OS().isCaseInsensitive() && (Boolean)options.getOption(OptionsImpl.CASE_INSENSITIVE_FS_RENAME) == false;
        this.options = options;
        this.progressDumper = (Boolean)options.getOption(OptionsImpl.SILENT) == false && (options.optionIsSet(OptionsImpl.OUTPUT_DIR) || options.optionIsSet(OptionsImpl.OUTPUT_PATH)) ? new ProgressDumperStdErr() : ProgressDumperNop.INSTANCE;
        this.prefix = "";
    }

    private InternalDumperFactoryImpl(InternalDumperFactoryImpl other, String prefix) {
        this.checkDupes = other.checkDupes;
        this.seenCaseDupe = other.seenCaseDupe;
        this.options = other.options;
        this.progressDumper = other.progressDumper;
        this.prefix = prefix;
    }

    @Override
    public DumperFactory getFactoryWithPrefix(String prefix, int version) {
        return new InternalDumperFactoryImpl(this, prefix);
    }

    private Pair<String, Boolean> getPathAndClobber() {
        Troolean clobber = (Troolean)((Object)this.options.getOption(OptionsImpl.CLOBBER_FILES));
        if (this.options.optionIsSet(OptionsImpl.OUTPUT_DIR)) {
            return Pair.make(this.options.getOption(OptionsImpl.OUTPUT_DIR), clobber.boolValue(true));
        }
        if (this.options.optionIsSet(OptionsImpl.OUTPUT_PATH)) {
            return Pair.make(this.options.getOption(OptionsImpl.OUTPUT_PATH), clobber.boolValue(false));
        }
        return null;
    }

    @Override
    public Dumper getNewTopLevelDumper(JavaTypeInstance classType, SummaryDumper summaryDumper, TypeUsageInformation typeUsageInformation, IllegalIdentifierDump illegalIdentifierDump) {
        Pair<String, Boolean> targetInfo = this.getPathAndClobber();
        if (targetInfo == null) {
            return new StdIODumper(typeUsageInformation, this.options, illegalIdentifierDump, new MovableDumperContext());
        }
        String encoding = (String)this.options.getOption(OptionsImpl.OUTPUT_ENCODING);
        FileDumper res = new FileDumper(targetInfo.getFirst() + this.prefix, encoding, targetInfo.getSecond(), classType, summaryDumper, typeUsageInformation, this.options, this.truncCount, illegalIdentifierDump);
        if (this.checkDupes && !this.seen.add(res.getFileName().toLowerCase())) {
            this.seenCaseDupe = true;
        }
        return res;
    }

    @Override
    public Dumper wrapLineNoDumper(Dumper dumper) {
        if (((Boolean)this.options.getOption(OptionsImpl.TRACK_BYTECODE_LOC)).booleanValue()) {
            return new BytecodeTrackingDumper(dumper, new BytecodeDumpConsumerImpl(dumper));
        }
        return dumper;
    }

    @Override
    public ExceptionDumper getExceptionDumper() {
        return new StdErrExceptionDumper();
    }

    @Override
    public SummaryDumper getSummaryDumper() {
        Pair<String, Boolean> targetInfo = this.getPathAndClobber();
        if (targetInfo == null) {
            return new NopSummaryDumper();
        }
        return new FileSummaryDumper(targetInfo.getFirst(), this.options, new AdditionalComments());
    }

    @Override
    public ProgressDumper getProgressDumper() {
        return this.progressDumper;
    }

    private class AdditionalComments
    implements DecompilerCommentSource {
        private AdditionalComments() {
        }

        @Override
        public List<DecompilerComment> getComments() {
            if (InternalDumperFactoryImpl.this.seenCaseDupe) {
                List<DecompilerComment> res = ListFactory.newList();
                res.add(DecompilerComment.CASE_CLASH_FS);
                return res;
            }
            return null;
        }
    }

    private static class BytecodeDumpConsumerImpl
    implements BytecodeDumpConsumer {
        private final Dumper dumper;

        BytecodeDumpConsumerImpl(Dumper dumper) {
            this.dumper = dumper;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void accept(Collection<BytecodeDumpConsumer.Item> items) {
            try {
                BufferedOutputStream stream = this.dumper.getAdditionalOutputStream("lineNumberTable");
                OutputStreamWriter sw = new OutputStreamWriter(stream);
                try {
                    sw.write("------------------\n");
                    sw.write("Line number table:\n\n");
                    for (BytecodeDumpConsumer.Item item : items) {
                        sw.write(item.getMethod().getMethodPrototype().toString());
                        sw.write("\n----------\n");
                        for (Map.Entry entry : item.getBytecodeLocs().entrySet()) {
                            sw.write("Line " + entry.getValue() + "\t: " + entry.getKey() + "\n");
                        }
                        sw.write("\n");
                    }
                }
                finally {
                    sw.close();
                }
            }
            catch (IOException e) {
                throw new ConfusedCFRException(e);
            }
        }
    }
}

