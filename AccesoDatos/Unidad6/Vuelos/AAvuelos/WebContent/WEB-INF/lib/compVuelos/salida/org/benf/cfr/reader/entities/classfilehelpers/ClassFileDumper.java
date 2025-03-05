/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.classfilehelpers;

import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.output.Dumper;

public interface ClassFileDumper
extends TypeUsageCollectable {
    public Dumper dump(ClassFile var1, InnerClassDumpType var2, Dumper var3);

    @Override
    public void collectTypeUsages(TypeUsageCollector var1);

    public static enum InnerClassDumpType {
        NOT(false),
        INNER_CLASS(true),
        INLINE_CLASS(true);

        final boolean isInnerClass;

        private InnerClassDumpType(boolean isInnerClass) {
            this.isInnerClass = isInnerClass;
        }

        public boolean isInnerClass() {
            return this.isInnerClass;
        }
    }
}

