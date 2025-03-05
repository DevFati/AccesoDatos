/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

public interface InnerClassInfo {
    public static final InnerClassInfo NOT = new InnerClassInfo(){

        @Override
        public boolean isInnerClass() {
            return false;
        }

        @Override
        public void collectTransitiveDegenericParents(Set<JavaTypeInstance> parents) {
        }

        @Override
        public boolean isAnonymousClass() {
            return false;
        }

        @Override
        public boolean isMethodScopedClass() {
            return false;
        }

        @Override
        public void markMethodScoped(boolean isAnonymous) {
        }

        @Override
        public boolean isInnerClassOf(JavaTypeInstance possibleParent) {
            return false;
        }

        @Override
        public boolean isTransitiveInnerClassOf(JavaTypeInstance possibleParent) {
            return false;
        }

        @Override
        public void setHideSyntheticThis() {
            throw new IllegalStateException();
        }

        @Override
        public void hideSyntheticFriendClass() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isSyntheticFriendClass() {
            return false;
        }

        @Override
        public JavaRefTypeInstance getOuterClass() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isHideSyntheticThis() {
            return false;
        }

        @Override
        public boolean getFullInnerPath(StringBuilder sb) {
            return false;
        }
    };

    public void collectTransitiveDegenericParents(Set<JavaTypeInstance> var1);

    public boolean isInnerClass();

    public boolean isInnerClassOf(JavaTypeInstance var1);

    public boolean isTransitiveInnerClassOf(JavaTypeInstance var1);

    public void hideSyntheticFriendClass();

    public boolean isSyntheticFriendClass();

    public void setHideSyntheticThis();

    public boolean isHideSyntheticThis();

    public void markMethodScoped(boolean var1);

    public boolean isAnonymousClass();

    public boolean isMethodScopedClass();

    public JavaRefTypeInstance getOuterClass();

    public boolean getFullInnerPath(StringBuilder var1);
}

