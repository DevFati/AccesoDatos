/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util;

import java.util.Collection;
import java.util.Set;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;

public class DecompilerComments
implements Dumpable {
    private Set<DecompilerComment> comments = SetFactory.newOrderedSet();

    public void addComment(String comment) {
        DecompilerComment decompilerComment = new DecompilerComment(comment);
        this.comments.add(decompilerComment);
    }

    public void addComment(DecompilerComment comment) {
        this.comments.add(comment);
    }

    public void addComments(Collection<DecompilerComment> comments) {
        this.comments.addAll(comments);
    }

    @Override
    public Dumper dump(Dumper d) {
        if (this.comments.isEmpty()) {
            return d;
        }
        d.beginBlockComment(false);
        for (DecompilerComment comment : this.comments) {
            d.dump(comment);
        }
        d.endBlockComment();
        return d;
    }

    public boolean contains(DecompilerComment comment) {
        return this.comments.contains(comment);
    }

    public Collection<DecompilerComment> getCommentCollection() {
        return this.comments;
    }
}

