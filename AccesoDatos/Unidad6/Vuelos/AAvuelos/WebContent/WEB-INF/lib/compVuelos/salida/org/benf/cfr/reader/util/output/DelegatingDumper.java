/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.io.BufferedOutputStream;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.TypeContext;

public abstract class DelegatingDumper
implements Dumper {
    protected Dumper delegate;

    public DelegatingDumper(Dumper delegate) {
        this.delegate = delegate;
    }

    @Override
    public TypeUsageInformation getTypeUsageInformation() {
        return this.delegate.getTypeUsageInformation();
    }

    @Override
    public ObfuscationMapping getObfuscationMapping() {
        return this.delegate.getObfuscationMapping();
    }

    @Override
    public Dumper label(String s, boolean inline) {
        this.delegate.label(s, inline);
        return this;
    }

    @Override
    public void enqueuePendingCarriageReturn() {
        this.delegate.enqueuePendingCarriageReturn();
    }

    @Override
    public Dumper removePendingCarriageReturn() {
        this.delegate.removePendingCarriageReturn();
        return this;
    }

    @Override
    public Dumper keyword(String s) {
        this.delegate.keyword(s);
        return this;
    }

    @Override
    public Dumper operator(String s) {
        this.delegate.operator(s);
        return this;
    }

    @Override
    public Dumper separator(String s) {
        this.delegate.separator(s);
        return this;
    }

    @Override
    public Dumper literal(String s, Object o) {
        this.delegate.literal(s, o);
        return this;
    }

    @Override
    public Dumper print(String s) {
        this.delegate.print(s);
        return this;
    }

    @Override
    public Dumper methodName(String s, MethodPrototype p, boolean special, boolean defines) {
        this.delegate.methodName(s, p, special, defines);
        return this;
    }

    @Override
    public Dumper packageName(JavaRefTypeInstance t) {
        this.delegate.packageName(t);
        return this;
    }

    @Override
    public Dumper identifier(String s, Object ref, boolean defines) {
        this.delegate.identifier(s, ref, defines);
        return this;
    }

    @Override
    public Dumper print(char c) {
        this.delegate.print(c);
        return this;
    }

    @Override
    public Dumper newln() {
        this.delegate.newln();
        return this;
    }

    @Override
    public Dumper endCodeln() {
        this.delegate.endCodeln();
        return this;
    }

    @Override
    public void indent(int diff) {
        this.delegate.indent(diff);
    }

    @Override
    public Dumper explicitIndent() {
        this.delegate.explicitIndent();
        return this;
    }

    @Override
    public int getIndentLevel() {
        return this.delegate.getIndentLevel();
    }

    @Override
    public Dumper dump(Dumpable d) {
        if (d == null) {
            return this.keyword("null");
        }
        return d.dump(this);
    }

    @Override
    public Dumper dump(JavaTypeInstance javaTypeInstance) {
        this.delegate.dump(javaTypeInstance);
        return this;
    }

    @Override
    public Dumper dump(JavaTypeInstance javaTypeInstance, TypeContext typeContext) {
        this.delegate.dump(javaTypeInstance, typeContext);
        return this;
    }

    @Override
    public void close() {
        this.delegate.close();
    }

    @Override
    public void addSummaryError(Method method, String s) {
        this.delegate.addSummaryError(method, s);
    }

    @Override
    public boolean canEmitClass(JavaTypeInstance type) {
        return this.delegate.canEmitClass(type);
    }

    @Override
    public Dumper fieldName(String name, JavaTypeInstance owner, boolean hiddenDeclaration, boolean isStatic, boolean defines) {
        this.delegate.fieldName(name, owner, hiddenDeclaration, isStatic, defines);
        return this;
    }

    @Override
    public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
        return this.delegate.withTypeUsageInformation(innerclassTypeUsageInformation);
    }

    @Override
    public Dumper comment(String s) {
        this.delegate.comment(s);
        return this;
    }

    @Override
    public Dumper beginBlockComment(boolean inline) {
        this.delegate.beginBlockComment(inline);
        return this;
    }

    @Override
    public Dumper endBlockComment() {
        this.delegate.endBlockComment();
        return this;
    }

    @Override
    public int getOutputCount() {
        return this.delegate.getOutputCount();
    }

    @Override
    public void informBytecodeLoc(HasByteCodeLoc loc) {
        this.delegate.informBytecodeLoc(loc);
    }

    @Override
    public BufferedOutputStream getAdditionalOutputStream(String description) {
        return this.delegate.getAdditionalOutputStream(description);
    }

    @Override
    public int getCurrentLine() {
        return this.delegate.getCurrentLine();
    }
}

