/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class StructuredScope {
    private final LinkedList<AtLevel> scope = ListFactory.newLinkedList();

    public void add(StructuredStatement statement) {
        this.scope.addFirst(new AtLevel(statement));
    }

    public void remove(StructuredStatement statement) {
        AtLevel old = this.scope.removeFirst();
        if (statement != old.statement) {
            throw new IllegalStateException();
        }
    }

    public List<Op04StructuredStatement> getPrecedingInblock(int skipN, int back) {
        if (skipN >= this.scope.size()) {
            return null;
        }
        AtLevel level = this.scope.get(skipN);
        StructuredStatement stm = level.statement;
        if (stm instanceof Block) {
            Block block = (Block)stm;
            int end = level.next - 1;
            int start = Math.max(end - back, 0);
            return block.getBlockStatements().subList(start, end);
        }
        return ListFactory.newList();
    }

    public StructuredStatement get(int skipN) {
        if (skipN >= this.scope.size()) {
            return null;
        }
        return this.scope.get((int)skipN).statement;
    }

    public List<StructuredStatement> getAll() {
        List<StructuredStatement> ret = ListFactory.newList();
        for (AtLevel atLevel : this.scope) {
            ret.add(atLevel.statement);
        }
        return ret;
    }

    public void setNextAtThisLevel(StructuredStatement statement, int next) {
        AtLevel atLevel = this.scope.getFirst();
        if (atLevel.statement != statement) {
            throw new IllegalStateException();
        }
        atLevel.next = next;
    }

    public BlockIdentifier getContinueBlock() {
        for (AtLevel atLevel : this.scope) {
            Op04StructuredStatement stm = atLevel.statement.getContainer();
            StructuredStatement stmt = stm.getStatement();
            if (!stmt.supportsBreak()) continue;
            if (!stmt.supportsContinueBreak()) {
                return null;
            }
            return stmt.getBreakableBlockOrNull();
        }
        return null;
    }

    public Set<Op04StructuredStatement> getNextFallThrough(StructuredStatement structuredStatement) {
        Op04StructuredStatement current = structuredStatement.getContainer();
        Set<Op04StructuredStatement> res = SetFactory.newSet();
        int idx = -1;
        for (AtLevel atLevel : this.scope) {
            if (++idx == 0 && atLevel.statement == structuredStatement) continue;
            if (atLevel.statement instanceof Block) {
                if (atLevel.next != -1) {
                    res.addAll(((Block)atLevel.statement).getNextAfter(atLevel.next, false));
                }
                if (!((Block)atLevel.statement).statementIsLast(current)) break;
                current = atLevel.statement.getContainer();
                continue;
            }
            if (!atLevel.statement.fallsNopToNext()) break;
            current = atLevel.statement.getContainer();
        }
        return res;
    }

    public Set<Op04StructuredStatement> getDirectFallThrough() {
        AtLevel atLevel = this.scope.getFirst();
        if (atLevel.statement instanceof Block && atLevel.next != -1) {
            return ((Block)atLevel.statement).getNextAfter(atLevel.next, false);
        }
        return SetFactory.newSet();
    }

    public boolean statementIsLast(StructuredStatement statement) {
        AtLevel atLevel = this.scope.getFirst();
        boolean x = true;
        StructuredStatement s = atLevel.statement;
        if (s instanceof Block) {
            return ((Block)s).statementIsLast(statement.getContainer());
        }
        return statement == s;
    }

    protected static class AtLevel {
        StructuredStatement statement;
        int next;

        private AtLevel(StructuredStatement statement) {
            this.statement = statement;
            this.next = 0;
        }

        public String toString() {
            return this.statement.toString();
        }
    }
}

