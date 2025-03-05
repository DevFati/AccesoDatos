/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.state.DetectedStaticImport;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

public interface TypeUsageInformation {
    public JavaRefTypeInstance getAnalysisType();

    public Set<JavaRefTypeInstance> getShortenedClassTypes();

    public Set<JavaRefTypeInstance> getUsedClassTypes();

    public Set<JavaRefTypeInstance> getUsedInnerClassTypes();

    public boolean hasLocalInstance(JavaRefTypeInstance var1);

    public String getName(JavaTypeInstance var1, TypeContext var2);

    public boolean isNameClash(JavaTypeInstance var1, String var2, TypeContext var3);

    public String generateInnerClassShortName(JavaRefTypeInstance var1);

    public String generateOverriddenName(JavaRefTypeInstance var1);

    public IllegalIdentifierDump getIid();

    public boolean isStaticImport(JavaTypeInstance var1, String var2);

    public Set<DetectedStaticImport> getDetectedStaticImports();
}

