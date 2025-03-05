/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredContinue;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredFinally;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredTry;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredAnonBreakTarget;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredAnonymousBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredGoto;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredTry;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredWhile;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Optional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class Block
extends AbstractStructuredStatement {
    private LinkedList<Op04StructuredStatement> containedStatements;
    private boolean indenting;
    private BlockIdentifier blockIdentifier;

    public Block(Op04StructuredStatement statement) {
        super(BytecodeLoc.NONE);
        LinkedList<Op04StructuredStatement> stm = new LinkedList<Op04StructuredStatement>();
        stm.add(statement);
        this.containedStatements = stm;
        this.indenting = false;
        this.blockIdentifier = null;
    }

    public Block(LinkedList<Op04StructuredStatement> containedStatements, boolean indenting) {
        this(containedStatements, indenting, null);
    }

    public Block(LinkedList<Op04StructuredStatement> containedStatements, boolean indenting, BlockIdentifier blockIdentifier) {
        super(BytecodeLoc.NONE);
        this.containedStatements = containedStatements;
        this.indenting = indenting;
        this.blockIdentifier = blockIdentifier;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    public void flattenOthersIn() {
        ListIterator<Op04StructuredStatement> iter = this.containedStatements.listIterator();
        while (iter.hasNext()) {
            Block containedBlock;
            Op04StructuredStatement item = (Op04StructuredStatement)iter.next();
            StructuredStatement contained = item.getStatement();
            if (!(contained instanceof Block) || !(containedBlock = (Block)contained).canFoldUp()) continue;
            iter.remove();
            LinkedList<Op04StructuredStatement> children = containedBlock.containedStatements;
            while (!children.isEmpty()) {
                iter.add(children.removeLast());
                iter.previous();
            }
        }
    }

    public void addStatement(Op04StructuredStatement stm) {
        this.containedStatements.add(stm);
    }

    static Block getEmptyBlock(boolean indenting) {
        return new Block(new LinkedList<Op04StructuredStatement>(), indenting);
    }

    public static Block getBlockFor(boolean indenting, StructuredStatement ... statements) {
        LinkedList<Op04StructuredStatement> tmp = ListFactory.newLinkedList();
        for (StructuredStatement statement : statements) {
            tmp.add(new Op04StructuredStatement(statement));
        }
        return new Block(tmp, indenting);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (!collector.isStatementRecursive()) {
            return;
        }
        for (Op04StructuredStatement statement : this.containedStatements) {
            statement.collectTypeUsages(collector);
        }
    }

    public boolean removeLastContinue(BlockIdentifier block) {
        StructuredStatement structuredStatement = this.containedStatements.getLast().getStatement();
        if (structuredStatement instanceof AbstractStructuredContinue) {
            AbstractStructuredContinue structuredContinue = (AbstractStructuredContinue)structuredStatement;
            if (structuredContinue.getContinueTgt() == block) {
                Op04StructuredStatement continueStmt = this.containedStatements.getLast();
                continueStmt.replaceStatementWithNOP("");
                return true;
            }
            return false;
        }
        return false;
    }

    public void removeLastNVReturn() {
        StructuredStatement structuredStatement = this.containedStatements.getLast().getStatement();
        if (structuredStatement instanceof StructuredReturn) {
            Op04StructuredStatement oldReturn = this.containedStatements.getLast();
            StructuredReturn structuredReturn = (StructuredReturn)structuredStatement;
            if (structuredReturn.getValue() == null) {
                oldReturn.replaceStatementWithNOP("");
            }
        }
    }

    public void removeLastGoto() {
        StructuredStatement structuredStatement = this.containedStatements.getLast().getStatement();
        if (structuredStatement instanceof UnstructuredGoto) {
            Op04StructuredStatement oldGoto = this.containedStatements.getLast();
            oldGoto.replaceStatementWithNOP("");
        }
    }

    public Op04StructuredStatement getLast() {
        Iterator<Op04StructuredStatement> iter = this.containedStatements.descendingIterator();
        while (iter.hasNext()) {
            Op04StructuredStatement stm = iter.next();
            if (stm.getStatement() instanceof StructuredComment) continue;
            return stm;
        }
        return null;
    }

    public UnstructuredWhile removeLastEndWhile() {
        StructuredStatement structuredStatement = this.containedStatements.getLast().getStatement();
        if (structuredStatement instanceof UnstructuredWhile) {
            Op04StructuredStatement endWhile = this.containedStatements.getLast();
            endWhile.replaceStatementWithNOP("");
            return (UnstructuredWhile)structuredStatement;
        }
        return null;
    }

    public Pair<Boolean, Op04StructuredStatement> getOneStatementIfPresent() {
        Op04StructuredStatement res = null;
        for (Op04StructuredStatement statement : this.containedStatements) {
            if (statement.getStatement() instanceof StructuredComment) continue;
            if (res == null) {
                res = statement;
                continue;
            }
            return Pair.make(Boolean.FALSE, null);
        }
        return Pair.make(res == null, res);
    }

    public List<Op04StructuredStatement> getFilteredBlockStatements() {
        List<Op04StructuredStatement> res = ListFactory.newList();
        for (Op04StructuredStatement statement : this.containedStatements) {
            if (statement.getStatement() instanceof StructuredComment) continue;
            res.add(statement);
        }
        return res;
    }

    public Optional<Op04StructuredStatement> getMaybeJustOneStatement() {
        Pair<Boolean, Op04StructuredStatement> tmp = this.getOneStatementIfPresent();
        return tmp.getSecond() == null ? Optional.empty() : Optional.of(tmp.getSecond());
    }

    @Override
    public boolean inlineable() {
        for (Op04StructuredStatement in : this.containedStatements) {
            StructuredStatement s = in.getStatement();
            Class<?> c = s.getClass();
            if (c == StructuredReturn.class || c == UnstructuredGoto.class) continue;
            return false;
        }
        return true;
    }

    @Override
    public Op04StructuredStatement getInline() {
        return this.getContainer();
    }

    public void combineInlineable() {
        boolean inline = false;
        for (Op04StructuredStatement in : this.containedStatements) {
            if (!in.getStatement().inlineable()) continue;
            inline = true;
            break;
        }
        if (!inline) {
            return;
        }
        LinkedList newContained = ListFactory.newLinkedList();
        for (Op04StructuredStatement in : this.containedStatements) {
            StructuredStatement s = in.getStatement();
            if (s.inlineable()) {
                Op04StructuredStatement inlinedOp = s.getInline();
                StructuredStatement inlined = inlinedOp.getStatement();
                if (inlined instanceof Block) {
                    List<Op04StructuredStatement> inlinedBlocks = ((Block)inlined).getBlockStatements();
                    newContained.addAll(((Block)inlined).getBlockStatements());
                    this.replaceInlineSource(in, inlinedBlocks.get(0));
                    continue;
                }
                newContained.add(inlinedOp);
                this.replaceInlineSource(in, inlinedOp);
                continue;
            }
            newContained.add(in);
        }
        this.containedStatements = newContained;
    }

    private void replaceInlineSource(Op04StructuredStatement oldS, Op04StructuredStatement newS) {
        for (Op04StructuredStatement src : oldS.getSources()) {
            src.replaceTarget(oldS, newS);
            newS.addSource(src);
        }
        newS.getSources().remove(oldS);
    }

    public void extractLabelledBlocks() {
        Iterator<Op04StructuredStatement> iterator = this.containedStatements.descendingIterator();
        List<Op04StructuredStatement> newEntries = ListFactory.newList();
        while (iterator.hasNext()) {
            Op04StructuredStatement stm = iterator.next();
            StructuredStatement statement = stm.getStatement();
            if (statement.getClass() != UnstructuredAnonBreakTarget.class) continue;
            UnstructuredAnonBreakTarget breakTarget = (UnstructuredAnonBreakTarget)statement;
            BlockIdentifier blockIdentifier = breakTarget.getBlockIdentifier();
            LinkedList<Op04StructuredStatement> inner = ListFactory.newLinkedList();
            iterator.remove();
            while (iterator.hasNext()) {
                inner.addFirst(iterator.next());
                iterator.remove();
            }
            Block nested = new Block(inner, true, blockIdentifier);
            Set<BlockIdentifier> outerIdents = this.getContainer().getBlockIdentifiers();
            Set<BlockIdentifier> innerIdents = SetFactory.newSet(outerIdents);
            innerIdents.add(blockIdentifier);
            InstrIndex newIdx = this.getContainer().getIndex().justAfter();
            Op04StructuredStatement newStm = new Op04StructuredStatement(newIdx, innerIdents, nested);
            newEntries.add(newStm);
            List<Op04StructuredStatement> sources = stm.getSources();
            boolean found = false;
            for (Op04StructuredStatement source : sources) {
                StructuredStatement maybeBreak = source.getStatement();
                if (maybeBreak.getClass() == StructuredIf.class) {
                    StructuredIf structuredIf = (StructuredIf)maybeBreak;
                    source = structuredIf.getIfTaken();
                    maybeBreak = source.getStatement();
                    found = true;
                }
                if (maybeBreak.getClass() != UnstructuredAnonymousBreak.class) continue;
                UnstructuredAnonymousBreak unstructuredBreak = (UnstructuredAnonymousBreak)maybeBreak;
                source.replaceStatement(unstructuredBreak.tryExplicitlyPlaceInBlock(blockIdentifier));
                found = true;
            }
            if (!found) {
                nested.indenting = false;
            }
            stm.replaceStatement(StructuredComment.EMPTY_COMMENT);
        }
        for (Op04StructuredStatement entry : newEntries) {
            this.containedStatements.addFirst(entry);
        }
    }

    public void combineTryCatch() {
        Set<Class> skipThese = SetFactory.newSet(StructuredCatch.class, StructuredFinally.class, StructuredTry.class, UnstructuredTry.class);
        int size = this.containedStatements.size();
        boolean finished = false;
        block0: for (int x = 0; x < size && !finished; ++x) {
            StructuredStatement nextStatement;
            Op04StructuredStatement next;
            Op04StructuredStatement statement = this.containedStatements.get(x);
            StructuredStatement innerStatement = statement.getStatement();
            if (innerStatement instanceof UnstructuredTry) {
                StructuredStatement nextStatement2;
                UnstructuredTry unstructuredTry = (UnstructuredTry)innerStatement;
                if (x < size - 1 && ((nextStatement2 = this.containedStatements.get(x + 1).getStatement()) instanceof StructuredCatch || nextStatement2 instanceof StructuredFinally)) {
                    Op04StructuredStatement replacement = new Op04StructuredStatement(unstructuredTry.getEmptyTry());
                    Op04StructuredStatement.replaceInTargets(statement, replacement);
                    Op04StructuredStatement.replaceInSources(statement, replacement);
                    statement = replacement;
                    this.containedStatements.set(x, statement);
                    innerStatement = statement.getStatement();
                }
            }
            if (!(innerStatement instanceof StructuredTry)) continue;
            StructuredTry structuredTry = (StructuredTry)innerStatement;
            BlockIdentifier tryBlockIdent = structuredTry.getTryBlockIdentifier();
            Op04StructuredStatement op04StructuredStatement = next = ++x < size ? this.containedStatements.get(x) : null;
            if (next != null && !skipThese.contains((nextStatement = next.getStatement()).getClass())) {
                for (int y = x + 1; y < size; ++y) {
                    Set<BlockIdentifier> blocks;
                    StructuredStatement test = this.containedStatements.get(y).getStatement();
                    if (test instanceof StructuredTry || test instanceof UnstructuredTry) continue block0;
                    if (!(test instanceof StructuredCatch) || !(blocks = ((StructuredCatch)test).getPossibleTryBlocks()).contains(tryBlockIdent)) continue;
                    x = y;
                    next = this.containedStatements.get(y);
                    break;
                }
            }
            while (x < size && next != null) {
                ++x;
                nextStatement = next.getStatement();
                if (nextStatement instanceof StructuredComment) {
                    next.nopOut();
                } else if (nextStatement instanceof StructuredCatch) {
                    Set<BlockIdentifier> blocks = ((StructuredCatch)nextStatement).getPossibleTryBlocks();
                    if (!blocks.contains(tryBlockIdent)) {
                        --x;
                        break;
                    }
                    structuredTry.addCatch(next.nopThisAndReplace());
                } else if (next.getStatement() instanceof StructuredFinally) {
                    structuredTry.setFinally(next.nopThisAndReplace());
                } else {
                    --x;
                    break;
                }
                if (x < size) {
                    next = this.containedStatements.get(x);
                    continue;
                }
                next = null;
                finished = true;
            }
            --x;
        }
    }

    @Override
    public boolean isScopeBlock() {
        return true;
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
        int len = this.containedStatements.size();
        for (int x = 0; x < len; ++x) {
            Op04StructuredStatement structuredBlock = this.containedStatements.get(x);
            scope.setNextAtThisLevel(this, x < len - 1 ? x + 1 : -1);
            structuredBlock.transform(transformer, scope);
        }
    }

    @Override
    public void transformStructuredChildrenInReverse(StructuredStatementTransformer transformer, StructuredScope scope) {
        int last;
        for (int x = last = this.containedStatements.size() - 1; x >= 0; --x) {
            Op04StructuredStatement structuredBlock = this.containedStatements.get(x);
            scope.setNextAtThisLevel(this, x < last ? x + 1 : -1);
            structuredBlock.transform(transformer, scope);
        }
    }

    public Set<Op04StructuredStatement> getNextAfter(int x, boolean skipComments) {
        Set<Op04StructuredStatement> res = SetFactory.newSet();
        if (x == -1 || x > this.containedStatements.size()) {
            return res;
        }
        while (x != -1 && x < this.containedStatements.size()) {
            Op04StructuredStatement next = this.containedStatements.get(x);
            if (next.getStatement() instanceof StructuredComment) {
                if (!skipComments) {
                    res.add(this.containedStatements.get(x));
                }
                ++x;
                continue;
            }
            res.add(this.containedStatements.get(x));
            break;
        }
        return res;
    }

    public boolean statementIsLast(Op04StructuredStatement needle) {
        for (int x = this.containedStatements.size() - 1; x >= 0; --x) {
            Op04StructuredStatement statement = this.containedStatements.get(x);
            if (statement == needle) {
                return true;
            }
            if (!(statement.getStatement() instanceof StructuredComment)) break;
        }
        return false;
    }

    @Override
    public BlockIdentifier getBreakableBlockOrNull() {
        return this.blockIdentifier != null && this.blockIdentifier.hasForeignReferences() ? this.blockIdentifier : null;
    }

    @Override
    public boolean isRecursivelyStructured() {
        for (Op04StructuredStatement structuredStatement : this.containedStatements) {
            if (structuredStatement.isFullyStructured()) continue;
            return false;
        }
        return true;
    }

    public List<Op04StructuredStatement> getBlockStatements() {
        return this.containedStatements;
    }

    public void replaceBlockStatements(Collection<Op04StructuredStatement> statements) {
        this.containedStatements.clear();
        this.containedStatements.addAll(statements);
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(new BeginBlock(this));
        for (Op04StructuredStatement structuredBlock : this.containedStatements) {
            structuredBlock.linearizeStatementsInto(out);
        }
        out.add(new EndBlock(this));
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        scopeDiscoverer.enterBlock(this);
        for (Op04StructuredStatement item : this.containedStatements) {
            scopeDiscoverer.mark(item);
            scopeDiscoverer.processOp04Statement(item);
        }
        scopeDiscoverer.leaveBlock(this);
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {
        int idx;
        Op04StructuredStatement declaration = new Op04StructuredStatement(new StructuredDefinition(scopedEntity));
        if (hint != null && (idx = this.containedStatements.indexOf(hint)) != -1) {
            this.containedStatements.add(idx, declaration);
            return;
        }
        this.containedStatements.addFirst(declaration);
    }

    @Override
    public boolean alwaysDefines(LValue scopedEntity) {
        return false;
    }

    private boolean canFoldUp() {
        boolean isIndenting = this.isIndenting();
        if (this.blockIdentifier != null) {
            isIndenting = this.blockIdentifier.hasForeignReferences();
        }
        return !isIndenting;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Dumper dump(Dumper d) {
        boolean isIndenting = this.isIndenting();
        if (this.blockIdentifier != null) {
            if (this.blockIdentifier.hasForeignReferences()) {
                d.label(this.blockIdentifier.getName(), true);
                isIndenting = true;
            } else {
                isIndenting = false;
            }
        }
        if (this.containedStatements.isEmpty()) {
            if (isIndenting) {
                d.separator("{").separator("}");
            }
            d.newln();
            return d;
        }
        try {
            if (isIndenting) {
                d.separator("{").newln();
                d.indent(1);
            }
            for (Op04StructuredStatement structuredBlock : this.containedStatements) {
                structuredBlock.dump(d);
            }
        }
        finally {
            if (isIndenting) {
                d.indent(-1);
                d.separator("}");
                d.enqueuePendingCarriageReturn();
            }
        }
        return d;
    }

    public boolean isIndenting() {
        return this.indenting;
    }

    public void setIndenting(boolean indenting) {
        this.indenting = indenting;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
    }

    @Override
    public boolean isEffectivelyNOP() {
        for (Op04StructuredStatement statement : this.containedStatements) {
            if (statement.getStatement().isEffectivelyNOP()) continue;
            return false;
        }
        return true;
    }
}

