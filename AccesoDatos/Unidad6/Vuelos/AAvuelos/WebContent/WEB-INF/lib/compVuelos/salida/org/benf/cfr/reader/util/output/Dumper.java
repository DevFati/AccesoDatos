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
import org.benf.cfr.reader.util.output.MethodErrorCollector;
import org.benf.cfr.reader.util.output.TypeContext;

public interface Dumper
extends MethodErrorCollector {
    public TypeUsageInformation getTypeUsageInformation();

    public ObfuscationMapping getObfuscationMapping();

    public Dumper label(String var1, boolean var2);

    public void enqueuePendingCarriageReturn();

    public Dumper removePendingCarriageReturn();

    public Dumper keyword(String var1);

    public Dumper operator(String var1);

    public Dumper separator(String var1);

    public Dumper literal(String var1, Object var2);

    public Dumper print(String var1);

    public Dumper methodName(String var1, MethodPrototype var2, boolean var3, boolean var4);

    public Dumper packageName(JavaRefTypeInstance var1);

    public Dumper identifier(String var1, Object var2, boolean var3);

    public Dumper print(char var1);

    public Dumper newln();

    public Dumper endCodeln();

    public Dumper explicitIndent();

    public void indent(int var1);

    public int getIndentLevel();

    public void close();

    @Override
    public void addSummaryError(Method var1, String var2);

    public boolean canEmitClass(JavaTypeInstance var1);

    public Dumper fieldName(String var1, JavaTypeInstance var2, boolean var3, boolean var4, boolean var5);

    public Dumper withTypeUsageInformation(TypeUsageInformation var1);

    public Dumper comment(String var1);

    public Dumper beginBlockComment(boolean var1);

    public Dumper endBlockComment();

    public int getOutputCount();

    public Dumper dump(JavaTypeInstance var1, TypeContext var2);

    public Dumper dump(JavaTypeInstance var1);

    public Dumper dump(Dumpable var1);

    public int getCurrentLine();

    public void informBytecodeLoc(HasByteCodeLoc var1);

    public BufferedOutputStream getAdditionalOutputStream(String var1);

    public static class CannotCreate
    extends RuntimeException {
        CannotCreate(String s) {
            super(s);
        }

        CannotCreate(Throwable throwable) {
            super(throwable);
        }

        @Override
        public String toString() {
            return "Cannot create dumper " + super.toString();
        }
    }
}

