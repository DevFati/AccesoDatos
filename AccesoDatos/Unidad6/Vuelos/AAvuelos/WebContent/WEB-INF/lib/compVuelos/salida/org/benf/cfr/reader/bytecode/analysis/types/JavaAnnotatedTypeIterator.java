/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import org.benf.cfr.reader.entities.annotations.AnnotationTableEntry;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;

public interface JavaAnnotatedTypeIterator {
    public JavaAnnotatedTypeIterator moveArray(DecompilerComments var1);

    public JavaAnnotatedTypeIterator moveBound(DecompilerComments var1);

    public JavaAnnotatedTypeIterator moveNested(DecompilerComments var1);

    public JavaAnnotatedTypeIterator moveParameterized(int var1, DecompilerComments var2);

    public void apply(AnnotationTableEntry var1);

    public static abstract class BaseAnnotatedTypeIterator
    implements JavaAnnotatedTypeIterator {
        protected void addBadComment(DecompilerComments comments) {
            comments.addComment(DecompilerComment.BAD_ANNOTATION);
        }

        @Override
        public JavaAnnotatedTypeIterator moveParameterized(int index, DecompilerComments comments) {
            this.addBadComment(comments);
            return this;
        }

        @Override
        public JavaAnnotatedTypeIterator moveNested(DecompilerComments comments) {
            this.addBadComment(comments);
            return this;
        }

        @Override
        public JavaAnnotatedTypeIterator moveBound(DecompilerComments comments) {
            this.addBadComment(comments);
            return this;
        }

        @Override
        public JavaAnnotatedTypeIterator moveArray(DecompilerComments comments) {
            this.addBadComment(comments);
            return this;
        }
    }
}

