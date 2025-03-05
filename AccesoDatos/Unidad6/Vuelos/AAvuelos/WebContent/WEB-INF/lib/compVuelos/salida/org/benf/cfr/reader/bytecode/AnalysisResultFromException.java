/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.AnalysisResult;
import org.benf.cfr.reader.bytecode.AnonymousClassUsage;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredFakeDecompFailure;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;

public class AnalysisResultFromException
implements AnalysisResult {
    private final Op04StructuredStatement code;
    private final DecompilerComments comments;

    public AnalysisResultFromException(Exception e) {
        this.code = new Op04StructuredStatement(new StructuredFakeDecompFailure(e));
        this.comments = new DecompilerComments();
        this.comments.addComment(new DecompilerComment("Exception decompiling", e));
    }

    @Override
    public boolean isFailed() {
        return true;
    }

    @Override
    public boolean isThrown() {
        return true;
    }

    @Override
    public Op04StructuredStatement getCode() {
        return this.code;
    }

    @Override
    public DecompilerComments getComments() {
        return this.comments;
    }

    @Override
    public AnonymousClassUsage getAnonymousClassUsage() {
        return new AnonymousClassUsage();
    }
}

