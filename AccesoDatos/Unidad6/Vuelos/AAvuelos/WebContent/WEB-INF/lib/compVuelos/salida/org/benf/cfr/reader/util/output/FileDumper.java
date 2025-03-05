/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicInteger;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.MovableDumperContext;
import org.benf.cfr.reader.util.output.StreamDumper;
import org.benf.cfr.reader.util.output.SummaryDumper;
import org.benf.cfr.reader.util.output.TypeOverridingDumper;

public class FileDumper
extends StreamDumper {
    private final String dir;
    private final String encoding;
    private final boolean clobber;
    private final JavaTypeInstance type;
    private final SummaryDumper summaryDumper;
    private final String path;
    private final BufferedWriter writer;
    private final AtomicInteger truncCount;
    private static final int MAX_FILE_LEN_MINUS_EXT = 249;
    private static final int TRUNC_PREFIX_LEN = 150;

    private String mkFilename(String dir, Pair<String, String> names, SummaryDumper summaryDumper) {
        String packageName = names.getFirst();
        String className = names.getSecond();
        if (className.length() > 249) {
            className = className.substring(0, 150) + "_cfr_" + this.truncCount.getAndIncrement();
            summaryDumper.notify("Class name " + names.getSecond() + " was shortened to " + className + " due to filesystem limitations.");
        }
        return dir + File.separator + packageName.replace(".", File.separator) + (packageName.length() == 0 ? "" : File.separator) + className + ".java";
    }

    FileDumper(String dir, boolean clobber, JavaTypeInstance type, SummaryDumper summaryDumper, TypeUsageInformation typeUsageInformation, Options options, AtomicInteger truncCount, IllegalIdentifierDump illegalIdentifierDump) {
        this(dir, null, clobber, type, summaryDumper, typeUsageInformation, options, truncCount, illegalIdentifierDump);
    }

    FileDumper(String dir, String encoding, boolean clobber, JavaTypeInstance type, SummaryDumper summaryDumper, TypeUsageInformation typeUsageInformation, Options options, AtomicInteger truncCount, IllegalIdentifierDump illegalIdentifierDump) {
        block7: {
            super(typeUsageInformation, options, illegalIdentifierDump, new MovableDumperContext());
            this.truncCount = truncCount;
            this.dir = dir;
            this.encoding = encoding;
            this.clobber = clobber;
            this.type = type;
            this.summaryDumper = summaryDumper;
            String fileName = this.mkFilename(dir, ClassNameUtils.getPackageAndClassNames(type), summaryDumper);
            try {
                File file = new File(fileName);
                File parent = file.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("Couldn't create dir: " + parent);
                }
                if (file.exists() && !clobber) {
                    throw new Dumper.CannotCreate("File already exists, and option '" + OptionsImpl.CLOBBER_FILES.getName() + "' not set");
                }
                this.path = fileName;
                if (encoding != null) {
                    try {
                        this.writer = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(file), encoding));
                        break block7;
                    }
                    catch (UnsupportedEncodingException e) {
                        throw new UnsupportedOperationException("Specified encoding '" + encoding + "' is not supported");
                    }
                }
                this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            }
            catch (FileNotFoundException e) {
                throw new Dumper.CannotCreate(e);
            }
        }
    }

    @Override
    public void close() {
        try {
            this.writer.close();
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void write(String s) {
        try {
            this.writer.write(s);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    String getFileName() {
        return this.path;
    }

    @Override
    public void addSummaryError(Method method, String s) {
        this.summaryDumper.notifyError(this.type, method, s);
    }

    @Override
    public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
        return new TypeOverridingDumper(this, innerclassTypeUsageInformation);
    }

    @Override
    public BufferedOutputStream getAdditionalOutputStream(String description) {
        String fileName = this.mkFilename(this.dir, ClassNameUtils.getPackageAndClassNames(this.type), this.summaryDumper);
        fileName = fileName + "." + description;
        try {
            File file = new File(fileName);
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Couldn't create dir: " + parent);
            }
            if (file.exists() && !this.clobber) {
                throw new Dumper.CannotCreate("File already exists, and option '" + OptionsImpl.CLOBBER_FILES.getName() + "' not set");
            }
            return new BufferedOutputStream(new FileOutputStream(file));
        }
        catch (FileNotFoundException e) {
            throw new Dumper.CannotCreate(e);
        }
    }
}

