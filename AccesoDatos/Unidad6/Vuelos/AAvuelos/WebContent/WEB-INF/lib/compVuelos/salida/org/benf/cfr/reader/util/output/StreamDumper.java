/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.QuotingUtils;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.mapping.NullMapping;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.AbstractDumper;
import org.benf.cfr.reader.util.output.BlockCommentState;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.MovableDumperContext;
import org.benf.cfr.reader.util.output.TypeContext;

public abstract class StreamDumper
extends AbstractDumper {
    private final TypeUsageInformation typeUsageInformation;
    protected final Options options;
    protected final IllegalIdentifierDump illegalIdentifierDump;
    private final boolean convertUTF;
    protected final Set<JavaTypeInstance> emitted;

    StreamDumper(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context) {
        super(context);
        this.typeUsageInformation = typeUsageInformation;
        this.options = options;
        this.illegalIdentifierDump = illegalIdentifierDump;
        this.convertUTF = (Boolean)options.getOption(OptionsImpl.HIDE_UTF8);
        this.emitted = SetFactory.newSet();
    }

    StreamDumper(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context, Set<JavaTypeInstance> emitted) {
        super(context);
        this.typeUsageInformation = typeUsageInformation;
        this.options = options;
        this.illegalIdentifierDump = illegalIdentifierDump;
        this.convertUTF = (Boolean)options.getOption(OptionsImpl.HIDE_UTF8);
        this.emitted = emitted;
    }

    @Override
    public TypeUsageInformation getTypeUsageInformation() {
        return this.typeUsageInformation;
    }

    @Override
    public ObfuscationMapping getObfuscationMapping() {
        return NullMapping.INSTANCE;
    }

    protected abstract void write(String var1);

    @Override
    public Dumper label(String s, boolean inline) {
        this.processPendingCR();
        if (inline) {
            this.doIndent();
            this.write(s + ": ");
        } else {
            this.write(s + ":");
            this.newln();
        }
        return this;
    }

    @Override
    public Dumper identifier(String s, Object ref, boolean defines) {
        return this.print(this.illegalIdentifierDump.getLegalIdentifierFor(s));
    }

    @Override
    public Dumper methodName(String s, MethodPrototype p, boolean special, boolean defines) {
        return this.identifier(s, null, defines);
    }

    @Override
    public Dumper packageName(JavaRefTypeInstance t) {
        String s = t.getPackageName();
        if (!s.isEmpty()) {
            this.keyword("package ").print(s).endCodeln().newln();
        }
        return this;
    }

    @Override
    public Dumper print(String s) {
        this.processPendingCR();
        this.doIndent();
        boolean doNewLn = false;
        if (s.endsWith("\n")) {
            s = s.substring(0, s.length() - 1);
            doNewLn = true;
        }
        if (this.convertUTF) {
            s = QuotingUtils.enquoteUTF(s);
        }
        this.write(s);
        this.context.atStart = false;
        if (doNewLn) {
            this.newln();
        }
        ++this.context.outputCount;
        return this;
    }

    @Override
    public Dumper print(char c) {
        return this.print("" + c);
    }

    @Override
    public Dumper keyword(String s) {
        this.print(s);
        return this;
    }

    @Override
    public Dumper operator(String s) {
        this.print(s);
        return this;
    }

    @Override
    public Dumper separator(String s) {
        this.print(s);
        return this;
    }

    @Override
    public Dumper literal(String s, Object o) {
        this.print(s);
        return this;
    }

    @Override
    public Dumper newln() {
        if (this.context.pendingCR) {
            this.write("\n");
            ++this.context.currentLine;
            if (this.context.atStart && this.context.inBlockComment != BlockCommentState.Not) {
                this.doIndent();
            }
        }
        this.context.pendingCR = true;
        this.context.atStart = true;
        ++this.context.outputCount;
        return this;
    }

    @Override
    public Dumper endCodeln() {
        this.write(";");
        this.context.pendingCR = true;
        this.context.atStart = true;
        ++this.context.outputCount;
        return this;
    }

    private void doIndent() {
        if (!this.context.atStart) {
            return;
        }
        for (int x = 0; x < this.context.indent; ++x) {
            this.write("    ");
        }
        this.context.atStart = false;
        if (this.context.inBlockComment != BlockCommentState.Not) {
            this.write(" * ");
        }
    }

    private void processPendingCR() {
        if (this.context.pendingCR) {
            this.write("\n");
            this.context.atStart = true;
            this.context.pendingCR = false;
            ++this.context.currentLine;
        }
    }

    @Override
    public Dumper explicitIndent() {
        this.print("    ");
        return this;
    }

    @Override
    public void indent(int diff) {
        this.context.indent += diff;
    }

    @Override
    public Dumper fieldName(String name, JavaTypeInstance owner, boolean hiddenDeclaration, boolean isStatic, boolean defines) {
        this.identifier(name, null, defines);
        return this;
    }

    @Override
    public Dumper dump(JavaTypeInstance javaTypeInstance, TypeContext typeContext) {
        javaTypeInstance.dumpInto(this, this.typeUsageInformation, typeContext);
        return this;
    }

    @Override
    public Dumper dump(Dumpable d) {
        if (d == null) {
            return this.keyword("null");
        }
        return d.dump(this);
    }

    @Override
    public boolean canEmitClass(JavaTypeInstance type) {
        return this.emitted.add(type);
    }

    @Override
    public int getOutputCount() {
        return this.context.outputCount;
    }

    @Override
    public int getCurrentLine() {
        int res = this.context.currentLine;
        if (this.context.pendingCR) {
            ++res;
        }
        return res;
    }
}

