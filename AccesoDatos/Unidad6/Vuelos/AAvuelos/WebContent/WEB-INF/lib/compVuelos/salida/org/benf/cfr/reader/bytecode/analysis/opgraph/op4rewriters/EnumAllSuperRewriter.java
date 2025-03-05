/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.RedundantSuperRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;

public class EnumAllSuperRewriter
extends RedundantSuperRewriter {
    @Override
    protected boolean canBeNopped(SuperFunctionInvokation superInvokation) {
        return true;
    }
}

