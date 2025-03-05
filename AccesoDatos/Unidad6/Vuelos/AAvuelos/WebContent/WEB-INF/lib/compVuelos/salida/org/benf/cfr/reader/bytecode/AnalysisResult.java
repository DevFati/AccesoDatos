/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.AnonymousClassUsage;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.util.DecompilerComments;

public interface AnalysisResult {
    public boolean isFailed();

    public boolean isThrown();

    public Op04StructuredStatement getCode();

    public DecompilerComments getComments();

    public AnonymousClassUsage getAnonymousClassUsage();
}

