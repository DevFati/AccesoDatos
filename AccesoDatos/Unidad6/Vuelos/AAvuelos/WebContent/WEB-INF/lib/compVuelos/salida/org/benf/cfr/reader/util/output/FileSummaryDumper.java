/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerCommentSource;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.SummaryDumper;

public class FileSummaryDumper
implements SummaryDumper {
    private final BufferedWriter writer;
    private final DecompilerCommentSource additionalComments;
    private final Options options;
    private transient JavaTypeInstance lastControllingType = null;
    private transient Method lastMethod = null;

    public FileSummaryDumper(String dir, Options options, DecompilerCommentSource additional) {
        this.additionalComments = additional;
        this.options = options;
        String fileName = dir + File.separator + "summary.txt";
        try {
            File file = new File(fileName);
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Couldn't create dir: " + parent);
            }
            this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        }
        catch (FileNotFoundException e) {
            throw new Dumper.CannotCreate(e);
        }
    }

    @Override
    public void notify(String message) {
        try {
            this.writer.write(message + "\n");
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void notifyError(JavaTypeInstance controllingType, Method method, String error) {
        try {
            if (this.lastControllingType != controllingType) {
                this.lastControllingType = controllingType;
                this.lastMethod = null;
                this.writer.write("\n\n" + controllingType.getRawName() + "\n----------------------------\n\n");
            }
            if (method != this.lastMethod) {
                if (method != null) {
                    this.writer.write(method.getMethodPrototype().toString() + "\n");
                }
                this.lastMethod = method;
            }
            this.writer.write("  " + error + "\n");
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void notifyAdditionalAtEnd() {
        try {
            List<DecompilerComment> comments;
            List<DecompilerComment> list = comments = this.additionalComments != null ? this.additionalComments.getComments() : null;
            if (comments != null && !comments.isEmpty()) {
                this.writer.write("\n");
                for (DecompilerComment comment : comments) {
                    this.writer.write(comment.toString() + "\n");
                    if (((Boolean)this.options.getOption(OptionsImpl.SILENT)).booleanValue()) continue;
                    System.err.println(comment.toString());
                }
            }
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        try {
            this.notifyAdditionalAtEnd();
            this.writer.close();
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

