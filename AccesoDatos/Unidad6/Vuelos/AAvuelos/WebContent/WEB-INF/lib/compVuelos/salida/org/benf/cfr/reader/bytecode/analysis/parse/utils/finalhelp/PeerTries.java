/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.finalhelp.CompositeBlockIdentifierKey;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class PeerTries {
    private final Op03SimpleStatement possibleFinallyCatch;
    private final Set<Op03SimpleStatement> seenEver = SetFactory.newOrderedSet();
    private final LinkedList<Op03SimpleStatement> toProcess = ListFactory.newLinkedList();
    private int nextIdx;
    private Set<BlockIdentifier> guessPeerTryBlocks = SetFactory.newOrderedSet();
    private Map<BlockIdentifier, Op03SimpleStatement> guessPeerTryMap = MapFactory.newOrderedMap();
    private Set<Op03SimpleStatement> guessPeerTryStarts = SetFactory.newOrderedSet();
    private final Map<CompositeBlockIdentifierKey, PeerTrySet> triesByLevel = MapFactory.newLazyMap(new TreeMap(), new UnaryFunction<CompositeBlockIdentifierKey, PeerTrySet>(){

        @Override
        public PeerTrySet invoke(CompositeBlockIdentifierKey arg) {
            return new PeerTrySet(PeerTries.this.nextIdx++);
        }
    });

    PeerTries(Op03SimpleStatement possibleFinallyCatch) {
        this.possibleFinallyCatch = possibleFinallyCatch;
        for (Op03SimpleStatement source : possibleFinallyCatch.getSources()) {
            Statement statement = source.getStatement();
            if (!(statement instanceof TryStatement)) continue;
            TryStatement tryStatement = (TryStatement)statement;
            BlockIdentifier blockIdentifier = tryStatement.getBlockIdentifier();
            this.guessPeerTryBlocks.add(blockIdentifier);
            this.guessPeerTryMap.put(blockIdentifier, source);
            this.guessPeerTryStarts.add(source);
        }
    }

    Op03SimpleStatement getOriginalFinally() {
        return this.possibleFinallyCatch;
    }

    Set<BlockIdentifier> getGuessPeerTryBlocks() {
        return this.guessPeerTryBlocks;
    }

    Map<BlockIdentifier, Op03SimpleStatement> getGuessPeerTryMap() {
        return this.guessPeerTryMap;
    }

    Set<Op03SimpleStatement> getGuessPeerTryStarts() {
        return this.guessPeerTryStarts;
    }

    public void add(Op03SimpleStatement tryStatement) {
        if (!(tryStatement.getStatement() instanceof TryStatement)) {
            throw new IllegalStateException();
        }
        if (this.seenEver.contains(tryStatement)) {
            return;
        }
        this.toProcess.add(tryStatement);
        this.triesByLevel.get(new CompositeBlockIdentifierKey(tryStatement)).add(tryStatement);
    }

    public boolean hasNext() {
        return !this.toProcess.isEmpty();
    }

    Op03SimpleStatement removeNext() {
        return this.toProcess.removeFirst();
    }

    List<PeerTrySet> getPeerTryGroups() {
        return ListFactory.newList(this.triesByLevel.values());
    }

    public static final class PeerTrySet {
        private final Set<Op03SimpleStatement> content = SetFactory.newOrderedSet();
        private final int idx;

        private PeerTrySet(int idx) {
            this.idx = idx;
        }

        public void add(Op03SimpleStatement op) {
            this.content.add(op);
        }

        Collection<Op03SimpleStatement> getPeerTries() {
            return this.content;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            PeerTrySet that = (PeerTrySet)o;
            return this.idx == that.idx;
        }

        public int hashCode() {
            return this.idx;
        }
    }
}

