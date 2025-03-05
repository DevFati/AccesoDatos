/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractConstructorInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.collections.ListFactory;

public class AnonymousClassUsage {
    private final List<Pair<ClassFile, ConstructorInvokationAnonymousInner>> noted = ListFactory.newList();
    private final List<Pair<ClassFile, ConstructorInvokationSimple>> localNoted = ListFactory.newList();

    public void note(ClassFile classFile, ConstructorInvokationAnonymousInner constructorInvokationAnonymousInner) {
        this.noted.add(Pair.make(classFile, constructorInvokationAnonymousInner));
    }

    public void noteMethodClass(ClassFile classFile, ConstructorInvokationSimple constructorInvokation) {
        this.localNoted.add(Pair.make(classFile, constructorInvokation));
    }

    public boolean isEmpty() {
        return this.noted.isEmpty() && this.localNoted.isEmpty();
    }

    void useNotes() {
        for (Pair<ClassFile, ConstructorInvokationAnonymousInner> pair : this.noted) {
            pair.getFirst().noteAnonymousUse(pair.getSecond());
        }
        for (Pair<ClassFile, AbstractConstructorInvokation> pair : this.localNoted) {
            pair.getFirst().noteMethodUse((ConstructorInvokationSimple)pair.getSecond());
        }
    }
}

