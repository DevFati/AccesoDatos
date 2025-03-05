/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.output.BlockCommentState;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.MovableDumperContext;
import org.benf.cfr.reader.util.output.TypeContext;

abstract class AbstractDumper
implements Dumper {
    protected static final String STANDARD_INDENT = "    ";
    final MovableDumperContext context;

    AbstractDumper(MovableDumperContext context) {
        this.context = context;
    }

    @Override
    public Dumper beginBlockComment(boolean inline) {
        if (this.context.inBlockComment != BlockCommentState.Not) {
            throw new IllegalStateException("Attempt to nest block comments.");
        }
        if (inline) {
            this.print("/* ");
        } else {
            this.print("/*").newln();
        }
        this.context.inBlockComment = inline ? BlockCommentState.InLine : BlockCommentState.In;
        return this;
    }

    @Override
    public Dumper endBlockComment() {
        if (this.context.inBlockComment == BlockCommentState.Not) {
            throw new IllegalStateException("Attempt to end block comment when not in one.");
        }
        BlockCommentState old = this.context.inBlockComment;
        this.context.inBlockComment = BlockCommentState.Not;
        if (old == BlockCommentState.In) {
            if (!this.context.atStart) {
                this.newln();
            }
            this.print(" */").newln();
        } else {
            this.print(" */ ");
        }
        return this;
    }

    @Override
    public Dumper comment(String s) {
        if (this.context.inBlockComment == BlockCommentState.Not) {
            this.print("// " + s);
        } else {
            this.print(s);
        }
        return this.newln();
    }

    @Override
    public void enqueuePendingCarriageReturn() {
        this.context.pendingCR = true;
    }

    @Override
    public Dumper dump(JavaTypeInstance javaTypeInstance) {
        return this.dump(javaTypeInstance, TypeContext.None);
    }

    @Override
    public Dumper removePendingCarriageReturn() {
        this.context.pendingCR = false;
        this.context.atStart = false;
        return this;
    }

    @Override
    public int getCurrentLine() {
        return this.context.currentLine;
    }

    @Override
    public int getIndentLevel() {
        return this.context.indent;
    }

    @Override
    public void informBytecodeLoc(HasByteCodeLoc loc) {
    }
}

