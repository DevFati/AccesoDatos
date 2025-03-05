/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.loc;

import java.util.Collection;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocCollector;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactory;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactoryImpl;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.entities.Method;

public abstract class BytecodeLoc {
    public static final BytecodeLoc NONE = BytecodeLocFactory.NONE;
    public static final BytecodeLoc TODO = BytecodeLocFactory.TODO;
    private static BytecodeLocFactoryImpl fact = BytecodeLocFactoryImpl.INSTANCE;

    public static BytecodeLoc combine(HasByteCodeLoc primary, HasByteCodeLoc ... coll) {
        return fact.combine(primary, coll);
    }

    public static BytecodeLoc combine(HasByteCodeLoc primary, Collection<? extends HasByteCodeLoc> coll1, HasByteCodeLoc ... coll2) {
        return fact.combine(primary, coll1, coll2);
    }

    public static BytecodeLoc combineShallow(HasByteCodeLoc ... coll) {
        return fact.combineShallow(coll);
    }

    abstract void addTo(BytecodeLocCollector var1);

    public abstract Collection<Method> getMethods();

    public abstract Collection<Integer> getOffsetsForMethod(Method var1);

    public abstract boolean isEmpty();
}

