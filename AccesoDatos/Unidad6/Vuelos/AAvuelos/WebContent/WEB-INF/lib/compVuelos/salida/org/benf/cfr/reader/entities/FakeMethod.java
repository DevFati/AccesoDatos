/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities;

import java.util.EnumSet;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.collections.CollectionUtils;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;

public class FakeMethod
implements TypeUsageCollectable,
Dumpable {
    private final String name;
    private final EnumSet<AccessFlagMethod> accessFlags;
    private final JavaTypeInstance returnType;
    private final Op04StructuredStatement structuredStatement;
    private final DecompilerComments comments;

    public FakeMethod(String name, EnumSet<AccessFlagMethod> accessFlags, JavaTypeInstance returnType, Op04StructuredStatement structuredStatement, DecompilerComments comments) {
        this.name = name;
        this.accessFlags = accessFlags;
        this.returnType = returnType;
        this.structuredStatement = structuredStatement;
        this.comments = comments;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }

    @Override
    public Dumper dump(Dumper d) {
        String prefix;
        if (this.comments != null) {
            this.comments.dump(d);
        }
        if (this.accessFlags != null && !(prefix = CollectionUtils.join(this.accessFlags, " ")).isEmpty()) {
            d.keyword(prefix);
            d.separator(" ");
        }
        d.dump(this.returnType).separator(" ").methodName(this.name, null, false, true).separator("() ");
        this.structuredStatement.dump(d);
        return d;
    }

    public String getName() {
        return this.name;
    }
}

