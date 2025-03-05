/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.io.BufferedOutputStream;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.mapping.NullMapping;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.output.AbstractDumper;
import org.benf.cfr.reader.util.output.BlockCommentState;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.MethodErrorCollector;
import org.benf.cfr.reader.util.output.MovableDumperContext;
import org.benf.cfr.reader.util.output.TypeContext;

public class TokenStreamDumper
extends AbstractDumper {
    private final RecycleToken tok = new RecycleToken();
    private final Token cr = new Token(SinkReturns.TokenType.NEWLINE, "\n", (Object)null);
    private final OutputSinkFactory.Sink<SinkReturns.Token> sink;
    private final int version;
    private final JavaTypeInstance classType;
    private final MethodErrorCollector methodErrorCollector;
    private final TypeUsageInformation typeUsageInformation;
    private final Options options;
    private final IllegalIdentifierDump illegalIdentifierDump;
    private final Map<Object, Object> refMap = MapFactory.newLazyMap(new IdentityHashMap(), new UnaryFunction<Object, Object>(){

        @Override
        public Object invoke(Object arg) {
            return new Object();
        }
    });
    private final Set<JavaTypeInstance> emitted = SetFactory.newSet();

    TokenStreamDumper(OutputSinkFactory.Sink<SinkReturns.Token> sink, int version, JavaTypeInstance classType, MethodErrorCollector methodErrorCollector, TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context) {
        super(context);
        this.sink = sink;
        this.version = version;
        this.classType = classType;
        this.methodErrorCollector = methodErrorCollector;
        this.typeUsageInformation = typeUsageInformation;
        this.options = options;
        this.illegalIdentifierDump = illegalIdentifierDump;
    }

    @Override
    public TypeUsageInformation getTypeUsageInformation() {
        return this.typeUsageInformation;
    }

    @Override
    public ObfuscationMapping getObfuscationMapping() {
        return NullMapping.INSTANCE;
    }

    private void sink(SinkReturns.TokenType type, String text) {
        this.flushPendingCR();
        this.sink.write(this.tok.set(this.adjustComment(type), text));
    }

    private SinkReturns.TokenType adjustComment(SinkReturns.TokenType type) {
        return this.context.inBlockComment == BlockCommentState.Not ? type : SinkReturns.TokenType.COMMENT;
    }

    private void sink(Token token) {
        this.flushPendingCR();
        this.sink.write(token);
    }

    private void flushPendingCR() {
        if (this.context.pendingCR) {
            this.context.pendingCR = false;
            ++this.context.currentLine;
            this.sink.write(this.cr);
        }
    }

    @Override
    public Dumper label(String s, boolean inline) {
        this.sink(new Token(SinkReturns.TokenType.LABEL, s, SinkReturns.TokenTypeFlags.DEFINES));
        return this;
    }

    @Override
    public void enqueuePendingCarriageReturn() {
        this.context.pendingCR = true;
    }

    @Override
    public Dumper removePendingCarriageReturn() {
        this.context.pendingCR = false;
        this.context.atStart = false;
        return this;
    }

    @Override
    public Dumper comment(String s) {
        this.sink(SinkReturns.TokenType.COMMENT, s);
        return this;
    }

    @Override
    public Dumper beginBlockComment(boolean inline) {
        if (this.context.inBlockComment != BlockCommentState.Not) {
            throw new IllegalStateException("Attempt to nest block comments.");
        }
        this.context.inBlockComment = inline ? BlockCommentState.InLine : BlockCommentState.In;
        this.print("/* ");
        if (inline) {
            this.newln();
        }
        return this;
    }

    @Override
    public Dumper endBlockComment() {
        if (this.context.inBlockComment == BlockCommentState.Not) {
            throw new IllegalStateException("Attempt to end block comment when not in one.");
        }
        if (this.context.inBlockComment == BlockCommentState.In) {
            if (!this.context.atStart) {
                this.newln();
            }
            this.print(" */").newln();
        } else {
            this.print(" */ ");
        }
        this.context.inBlockComment = BlockCommentState.Not;
        return this;
    }

    @Override
    public Dumper keyword(String s) {
        this.sink(SinkReturns.TokenType.KEYWORD, s);
        return this;
    }

    @Override
    public Dumper operator(String s) {
        this.sink(SinkReturns.TokenType.OPERATOR, s);
        return this;
    }

    @Override
    public Dumper separator(String s) {
        this.sink(SinkReturns.TokenType.SEPARATOR, s);
        return this;
    }

    @Override
    public Dumper literal(String s, Object o) {
        this.sink(new Token(SinkReturns.TokenType.LITERAL, s, o));
        return this;
    }

    @Override
    public Dumper print(String s) {
        this.sink(SinkReturns.TokenType.UNCLASSIFIED, s);
        return this;
    }

    @Override
    public Dumper methodName(String s, MethodPrototype p, boolean special, boolean defines) {
        if (defines) {
            this.sink(new Token(SinkReturns.TokenType.METHOD, s, this.refMap.get(p), SinkReturns.TokenTypeFlags.DEFINES));
        } else {
            this.sink(new Token(SinkReturns.TokenType.METHOD, s, this.refMap.get(p)));
        }
        return this;
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
    public Dumper identifier(String s, Object ref, boolean defines) {
        if (defines) {
            this.sink(new Token(SinkReturns.TokenType.IDENTIFIER, s, this.refMap.get(ref), SinkReturns.TokenTypeFlags.DEFINES));
        } else {
            this.sink(new Token(SinkReturns.TokenType.IDENTIFIER, s, this.refMap.get(ref)));
        }
        return this;
    }

    @Override
    public Dumper print(char c) {
        this.print("" + c);
        return this;
    }

    @Override
    public Dumper newln() {
        if (this.context.pendingCR) {
            this.sink(this.cr);
        }
        this.context.pendingCR = true;
        this.context.atStart = true;
        ++this.context.outputCount;
        return this;
    }

    @Override
    public Dumper endCodeln() {
        this.sink(SinkReturns.TokenType.UNCLASSIFIED, ";");
        this.context.pendingCR = true;
        this.context.atStart = true;
        ++this.context.outputCount;
        return this;
    }

    @Override
    public Dumper explicitIndent() {
        this.sink(SinkReturns.TokenType.EXPLICIT_INDENT, "");
        return this;
    }

    @Override
    public void indent(int diff) {
        this.sink(diff > 0 ? SinkReturns.TokenType.INDENT : SinkReturns.TokenType.UNINDENT, "");
    }

    @Override
    public Dumper dump(JavaTypeInstance javaTypeInstance, TypeContext typeContext) {
        javaTypeInstance.dumpInto(this, this.typeUsageInformation, typeContext);
        return this;
    }

    @Override
    public Dumper dump(Dumpable d) {
        if (d == null) {
            this.keyword("null");
        } else {
            d.dump(this);
        }
        return this;
    }

    @Override
    public void close() {
        this.sink(SinkReturns.TokenType.EOF, "");
    }

    @Override
    public void addSummaryError(Method method, String s) {
        this.methodErrorCollector.addSummaryError(method, s);
    }

    @Override
    public boolean canEmitClass(JavaTypeInstance type) {
        return this.emitted.add(type);
    }

    @Override
    public Dumper fieldName(String name, JavaTypeInstance owner, boolean hiddenDeclaration, boolean isStatic, boolean defines) {
        if (defines) {
            this.sink(new Token(SinkReturns.TokenType.FIELD, name, SinkReturns.TokenTypeFlags.DEFINES));
        } else {
            this.sink(SinkReturns.TokenType.FIELD, name);
        }
        return this;
    }

    @Override
    public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
        return new TokenStreamDumper(this.sink, this.version, this.classType, this.methodErrorCollector, innerclassTypeUsageInformation, this.options, this.illegalIdentifierDump, this.context);
    }

    @Override
    public int getOutputCount() {
        return this.context.outputCount;
    }

    @Override
    public int getCurrentLine() {
        return this.context.currentLine;
    }

    @Override
    public BufferedOutputStream getAdditionalOutputStream(String description) {
        throw new IllegalStateException();
    }

    private static class Token
    implements SinkReturns.Token {
        private final SinkReturns.TokenType type;
        private final String value;
        private final Object raw;
        private final Set<SinkReturns.TokenTypeFlags> flags;

        Token(SinkReturns.TokenType type, String value, Object raw) {
            this(type, value, raw, Collections.emptySet());
        }

        Token(SinkReturns.TokenType type, String value, Object raw, SinkReturns.TokenTypeFlags flag) {
            this(type, value, raw, Collections.singleton(flag));
        }

        Token(SinkReturns.TokenType type, String value, SinkReturns.TokenTypeFlags flag) {
            this(type, value, null, Collections.singleton(flag));
        }

        Token(SinkReturns.TokenType type, String value, SinkReturns.TokenTypeFlags ... flags) {
            this(type, value, null, SetFactory.newSet(flags));
        }

        private Token(SinkReturns.TokenType type, String value, Object raw, Set<SinkReturns.TokenTypeFlags> flags) {
            this.type = type;
            this.value = value;
            this.raw = raw;
            this.flags = flags;
        }

        @Override
        public SinkReturns.TokenType getTokenType() {
            return this.type;
        }

        @Override
        public String getText() {
            return this.value;
        }

        @Override
        public Object getRawValue() {
            return this.raw;
        }

        @Override
        public Set<SinkReturns.TokenTypeFlags> getFlags() {
            return this.flags;
        }
    }

    private static class RecycleToken
    implements SinkReturns.Token {
        private SinkReturns.TokenType type;
        private String text;

        private RecycleToken() {
        }

        @Override
        public SinkReturns.TokenType getTokenType() {
            return this.type;
        }

        @Override
        public String getText() {
            return this.text;
        }

        @Override
        public Object getRawValue() {
            return null;
        }

        @Override
        public Set<SinkReturns.TokenTypeFlags> getFlags() {
            return Collections.emptySet();
        }

        SinkReturns.Token set(SinkReturns.TokenType type, String text) {
            this.text = text;
            this.type = type;
            return this;
        }
    }
}

