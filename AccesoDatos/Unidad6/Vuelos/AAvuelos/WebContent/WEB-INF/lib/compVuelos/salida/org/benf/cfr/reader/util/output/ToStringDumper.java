/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.io.BufferedOutputStream;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.mapping.NullMapping;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.state.TypeUsageInformationEmpty;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.output.AbstractDumper;
import org.benf.cfr.reader.util.output.BlockCommentState;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.MovableDumperContext;
import org.benf.cfr.reader.util.output.TypeContext;
import org.benf.cfr.reader.util.output.TypeOverridingDumper;

public class ToStringDumper
extends AbstractDumper {
    private final StringBuilder sb = new StringBuilder();
    private final TypeUsageInformation typeUsageInformation = new TypeUsageInformationEmpty();
    private final Set<JavaTypeInstance> emitted = SetFactory.newSet();

    public static String toString(Dumpable d) {
        return new ToStringDumper().dump(d).toString();
    }

    public ToStringDumper() {
        super(new MovableDumperContext());
    }

    @Override
    public Dumper label(String s, boolean inline) {
        this.processPendingCR();
        this.sb.append(s).append(":");
        return this;
    }

    private void processPendingCR() {
        if (this.context.pendingCR) {
            this.sb.append('\n');
            this.context.atStart = true;
            this.context.pendingCR = false;
        }
    }

    @Override
    public Dumper identifier(String s, Object ref, boolean defines) {
        return this.print(s);
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
        this.sb.append(s);
        this.context.atStart = s.endsWith("\n");
        ++this.context.outputCount;
        return this;
    }

    @Override
    public Dumper print(char c) {
        return this.print("" + c);
    }

    @Override
    public Dumper newln() {
        this.sb.append("\n");
        this.context.atStart = true;
        ++this.context.outputCount;
        return this;
    }

    @Override
    public Dumper endCodeln() {
        this.sb.append(";\n");
        this.context.atStart = true;
        ++this.context.outputCount;
        return this;
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

    private void doIndent() {
        if (!this.context.atStart) {
            return;
        }
        for (int x = 0; x < this.context.indent; ++x) {
            this.sb.append("    ");
        }
        this.context.atStart = false;
        if (this.context.inBlockComment != BlockCommentState.Not) {
            this.sb.append(" * ");
        }
    }

    @Override
    public void indent(int diff) {
        this.context.indent += diff;
    }

    @Override
    public Dumper explicitIndent() {
        this.print("    ");
        return this;
    }

    @Override
    public Dumper dump(Dumpable d) {
        if (d == null) {
            this.keyword("null");
            return this;
        }
        d.dump(this);
        return this;
    }

    @Override
    public TypeUsageInformation getTypeUsageInformation() {
        return this.typeUsageInformation;
    }

    @Override
    public ObfuscationMapping getObfuscationMapping() {
        return NullMapping.INSTANCE;
    }

    @Override
    public Dumper dump(JavaTypeInstance javaTypeInstance, TypeContext typeContext) {
        javaTypeInstance.dumpInto(this, this.typeUsageInformation, typeContext);
        return this;
    }

    @Override
    public Dumper fieldName(String name, JavaTypeInstance owner, boolean hiddenDeclaration, boolean isStatic, boolean defines) {
        this.identifier(name, null, defines);
        return this;
    }

    public String toString() {
        return this.sb.toString();
    }

    @Override
    public void addSummaryError(Method method, String s) {
    }

    @Override
    public void close() {
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
    public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
        return new TypeOverridingDumper(this, innerclassTypeUsageInformation);
    }

    @Override
    public BufferedOutputStream getAdditionalOutputStream(String description) {
        throw new IllegalStateException();
    }
}

