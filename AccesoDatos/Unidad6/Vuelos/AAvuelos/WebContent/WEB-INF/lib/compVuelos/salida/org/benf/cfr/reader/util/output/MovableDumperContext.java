/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import org.benf.cfr.reader.util.output.BlockCommentState;

class MovableDumperContext {
    BlockCommentState inBlockComment = BlockCommentState.Not;
    boolean atStart = true;
    boolean pendingCR = false;
    int indent;
    int outputCount = 0;
    int currentLine = 1;

    MovableDumperContext() {
    }
}

