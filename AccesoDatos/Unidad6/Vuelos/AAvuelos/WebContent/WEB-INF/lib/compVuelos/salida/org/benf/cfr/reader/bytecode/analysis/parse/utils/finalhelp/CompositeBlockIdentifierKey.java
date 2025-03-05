/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.functors.Predicate;

public class CompositeBlockIdentifierKey
implements Comparable<CompositeBlockIdentifierKey> {
    private final String key;

    public CompositeBlockIdentifierKey(Op03SimpleStatement statement) {
        this(statement.getBlockIdentifiers());
    }

    public CompositeBlockIdentifierKey(Set<BlockIdentifier> blockIdentifiers) {
        List<BlockIdentifier> b = Functional.filter(blockIdentifiers, new Predicate<BlockIdentifier>(){

            @Override
            public boolean test(BlockIdentifier in) {
                switch (in.getBlockType()) {
                    case TRYBLOCK: 
                    case CATCHBLOCK: {
                        return true;
                    }
                }
                return false;
            }
        });
        Collections.sort(b);
        StringBuilder sb = new StringBuilder();
        for (BlockIdentifier blockIdentifier : b) {
            sb.append(blockIdentifier.getIndex()).append(".");
        }
        this.key = sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        CompositeBlockIdentifierKey that = (CompositeBlockIdentifierKey)o;
        return this.key.equals(that.key);
    }

    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public int compareTo(CompositeBlockIdentifierKey compositeBlockIdentifierKey) {
        if (compositeBlockIdentifierKey == this) {
            return 0;
        }
        if (this.key.length() < compositeBlockIdentifierKey.key.length()) {
            return -1;
        }
        return this.key.compareTo(compositeBlockIdentifierKey.key);
    }

    public String toString() {
        return "CompositeBlockIdentifierKey{key='" + this.key + '\'' + '}';
    }
}

