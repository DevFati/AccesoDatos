/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.GraphConversionHelper;
import org.benf.cfr.reader.bytecode.analysis.opgraph.IndexedStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.MutableGraph;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.CompareByIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CaseStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JumpingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.SwitchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.WhileStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.CreationCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.JumpType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentAndAliasCondenser;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimpleRW;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.UniqueSeenQueue;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;

public class Op03SimpleStatement
implements MutableGraph<Op03SimpleStatement>,
Dumpable,
StatementContainer<Statement>,
IndexedStatement {
    private final List<Op03SimpleStatement> sources = ListFactory.newList();
    private final List<Op03SimpleStatement> targets = ListFactory.newList();
    private Op03SimpleStatement linearlyPrevious;
    private Op03SimpleStatement linearlyNext;
    private boolean isNop;
    private InstrIndex index;
    private Statement containedStatement;
    private SSAIdentifiers<LValue> ssaIdentifiers;
    private BlockIdentifier thisComparisonBlock;
    private BlockIdentifier firstStatementInThisBlock;
    private final Set<BlockIdentifier> containedInBlocks = SetFactory.newSet();
    private Set<BlockIdentifier> possibleExitsFor = null;

    public Op03SimpleStatement(Op02WithProcessedDataAndRefs original, Statement statement) {
        this.containedStatement = statement;
        this.isNop = false;
        this.index = original.getIndex();
        this.ssaIdentifiers = new SSAIdentifiers();
        this.containedInBlocks.addAll(original.getContainedInTheseBlocks());
        statement.setContainer(this);
    }

    public Op03SimpleStatement(Set<BlockIdentifier> containedIn, Statement statement, InstrIndex index) {
        this.containedStatement = statement;
        this.isNop = false;
        this.index = index;
        this.ssaIdentifiers = new SSAIdentifiers();
        this.containedInBlocks.addAll(containedIn);
        statement.setContainer(this);
    }

    public Op03SimpleStatement(Set<BlockIdentifier> containedIn, Statement statement, SSAIdentifiers<LValue> ssaIdentifiers, InstrIndex index) {
        this.containedStatement = statement;
        this.isNop = false;
        this.index = index;
        this.ssaIdentifiers = new SSAIdentifiers<LValue>(ssaIdentifiers);
        this.containedInBlocks.addAll(containedIn);
        statement.setContainer(this);
    }

    @Override
    public List<Op03SimpleStatement> getSources() {
        return this.sources;
    }

    @Override
    public List<Op03SimpleStatement> getTargets() {
        return this.targets;
    }

    public void setLinearlyNext(Op03SimpleStatement linearlyNext) {
        this.linearlyNext = linearlyNext;
    }

    public Op03SimpleStatement getLinearlyPrevious() {
        return this.linearlyPrevious;
    }

    public void setLinearlyPrevious(Op03SimpleStatement linearlyPrevious) {
        this.linearlyPrevious = linearlyPrevious;
    }

    public BlockIdentifier getFirstStatementInThisBlock() {
        return this.firstStatementInThisBlock;
    }

    public void setFirstStatementInThisBlock(BlockIdentifier firstStatementInThisBlock) {
        this.firstStatementInThisBlock = firstStatementInThisBlock;
    }

    @Override
    public void addSource(Op03SimpleStatement source) {
        if (source == null) {
            throw new ConfusedCFRException("Null source being added.");
        }
        this.sources.add(source);
    }

    @Override
    public void addTarget(Op03SimpleStatement target) {
        this.targets.add(target);
    }

    @Override
    public Statement getStatement() {
        return this.containedStatement;
    }

    @Override
    public Statement getTargetStatement(int idx) {
        if (this.targets.size() <= idx) {
            throw new ConfusedCFRException("Trying to get invalid target " + idx);
        }
        Op03SimpleStatement target = this.targets.get(idx);
        Statement statement = target.getStatement();
        if (statement == null) {
            throw new ConfusedCFRException("Invalid target statement");
        }
        return statement;
    }

    @Override
    public void replaceStatement(Statement newStatement) {
        newStatement.setContainer(this);
        this.containedStatement = newStatement;
    }

    private void markAgreedNop() {
        this.isNop = true;
    }

    @Override
    public void nopOut() {
        if (this.isNop) {
            return;
        }
        if (this.targets.isEmpty()) {
            for (Op03SimpleStatement source : this.sources) {
                source.removeTarget(this);
            }
            this.sources.clear();
            this.containedStatement = new Nop();
            this.containedStatement.setContainer(this);
            this.markAgreedNop();
            return;
        }
        if (this.targets.size() != 1) {
            throw new ConfusedCFRException("Trying to nopOut a node with multiple targets");
        }
        this.containedStatement = new Nop();
        this.containedStatement.setContainer(this);
        Op03SimpleStatement target = this.targets.get(0);
        for (Op03SimpleStatement source : this.sources) {
            source.replaceTarget(this, target);
        }
        target.replaceSingleSourceWith(this, this.sources);
        this.sources.clear();
        this.targets.clear();
        this.markAgreedNop();
    }

    @Override
    public void nopOutConditional() {
        this.containedStatement = new Nop();
        this.containedStatement.setContainer(this);
        for (int i = 1; i < this.targets.size(); ++i) {
            Op03SimpleStatement dropTarget = this.targets.get(i);
            dropTarget.removeSource(this);
        }
        Op03SimpleStatement target = this.targets.get(0);
        this.targets.clear();
        this.targets.add(target);
        for (Op03SimpleStatement source : this.sources) {
            source.replaceTarget(this, target);
        }
        target.replaceSingleSourceWith(this, this.sources);
        this.sources.clear();
        this.targets.clear();
        this.markAgreedNop();
    }

    public void clear() {
        for (Op03SimpleStatement source : this.sources) {
            if (!source.getTargets().contains(this)) continue;
            source.removeTarget(this);
        }
        this.sources.clear();
        for (Op03SimpleStatement target : this.targets) {
            if (!target.getSources().contains(this)) continue;
            target.removeSource(this);
        }
        this.targets.clear();
        this.nopOut();
    }

    @Override
    public SSAIdentifiers<LValue> getSSAIdentifiers() {
        return this.ssaIdentifiers;
    }

    @Override
    public Set<BlockIdentifier> getBlockIdentifiers() {
        return this.containedInBlocks;
    }

    @Override
    public BlockIdentifier getBlockStarted() {
        return this.firstStatementInThisBlock;
    }

    @Override
    public Set<BlockIdentifier> getBlocksEnded() {
        if (this.linearlyPrevious == null) {
            return SetFactory.newSet();
        }
        Set<BlockIdentifier> in = SetFactory.newSet(this.linearlyPrevious.getBlockIdentifiers());
        in.removeAll(this.getBlockIdentifiers());
        Iterator<BlockIdentifier> iterator = in.iterator();
        while (iterator.hasNext()) {
            BlockIdentifier blockIdentifier = iterator.next();
            if (blockIdentifier.getBlockType().isBreakable()) continue;
            iterator.remove();
        }
        return in;
    }

    public Op03SimpleStatement getLinearlyNext() {
        return this.linearlyNext;
    }

    @Override
    public void copyBlockInformationFrom(StatementContainer<Statement> other) {
        Op03SimpleStatement other3 = (Op03SimpleStatement)other;
        this.containedInBlocks.addAll(other.getBlockIdentifiers());
        if (this.firstStatementInThisBlock == null) {
            this.firstStatementInThisBlock = other3.firstStatementInThisBlock;
        }
    }

    @Override
    public void copyBytecodeInformationFrom(StatementContainer<Statement> other) {
        Op03SimpleStatement other3 = (Op03SimpleStatement)other;
        this.getStatement().addLoc(other3.getStatement());
    }

    public boolean isAgreedNop() {
        return this.isNop;
    }

    void replaceBlockIfIn(BlockIdentifier oldB, BlockIdentifier newB) {
        if (this.containedInBlocks.remove(oldB)) {
            this.containedInBlocks.add(newB);
        }
    }

    public void splice(Op03SimpleStatement newSource) {
        if (newSource.targets.size() != 1) {
            throw new ConfusedCFRException("Can't splice (bad targets)");
        }
        if (this.sources.size() != 1) {
            throw new ConfusedCFRException("Can't splice (bad sources)");
        }
        if (this.targets.size() != 1) {
            throw new ConfusedCFRException("Can't splice (bad new target)");
        }
        Op03SimpleStatement oldSource = this.sources.get(0);
        Op03SimpleStatement oldTarget = this.targets.get(0);
        Op03SimpleStatement newTarget = newSource.targets.get(0);
        oldSource.replaceTarget(this, oldTarget);
        oldTarget.replaceSource(this, oldSource);
        newSource.replaceTarget(newTarget, this);
        newTarget.replaceSource(newSource, this);
        this.sources.set(0, newSource);
        this.targets.set(0, newTarget);
        this.setIndex(newSource.getIndex().justAfter());
    }

    public void replaceTarget(Op03SimpleStatement oldTarget, Op03SimpleStatement newTarget) {
        int index = this.targets.indexOf(oldTarget);
        if (index == -1) {
            throw new ConfusedCFRException("Invalid target");
        }
        this.targets.set(index, newTarget);
    }

    private void replaceSingleSourceWith(Op03SimpleStatement oldSource, List<Op03SimpleStatement> newSources) {
        if (!this.sources.remove(oldSource)) {
            throw new ConfusedCFRException("Invalid source");
        }
        this.sources.addAll(newSources);
    }

    public void replaceSource(Op03SimpleStatement oldSource, Op03SimpleStatement newSource) {
        int index = this.sources.indexOf(oldSource);
        if (index == -1) {
            throw new ConfusedCFRException("Invalid source");
        }
        this.sources.set(index, newSource);
    }

    public void removeSource(Op03SimpleStatement oldSource) {
        if (!this.sources.remove(oldSource)) {
            throw new ConfusedCFRException("Invalid source, tried to remove " + oldSource + "\nfrom " + this + "\nbut was not a source.");
        }
    }

    public void removeTarget(Op03SimpleStatement oldTarget) {
        if (this.containedStatement instanceof GotoStatement) {
            throw new ConfusedCFRException("Removing goto target");
        }
        if (!this.targets.remove(oldTarget)) {
            throw new ConfusedCFRException("Invalid target, tried to remove " + oldTarget + "\nfrom " + this + "\nbut was not a target.");
        }
    }

    public void removeGotoTarget(Op03SimpleStatement oldTarget) {
        if (!this.targets.remove(oldTarget)) {
            throw new ConfusedCFRException("Invalid target, tried to remove " + oldTarget + "\nfrom " + this + "\nbut was not a target.");
        }
    }

    @Override
    public InstrIndex getIndex() {
        return this.index;
    }

    public void setIndex(InstrIndex index) {
        this.index = index;
    }

    public BlockIdentifier getThisComparisonBlock() {
        return this.thisComparisonBlock;
    }

    public void clearThisComparisonBlock() {
        this.thisComparisonBlock = null;
    }

    public void markBlockStatement(BlockIdentifier blockIdentifier, Op03SimpleStatement lastInBlock, Op03SimpleStatement blockEnd, List<Op03SimpleStatement> statements) {
        if (this.thisComparisonBlock != null) {
            throw new ConfusedCFRException("Statement marked as the start of multiple blocks");
        }
        this.thisComparisonBlock = blockIdentifier;
        switch (blockIdentifier.getBlockType()) {
            case WHILELOOP: {
                IfStatement ifStatement = (IfStatement)this.containedStatement;
                ifStatement.replaceWithWhileLoopStart(blockIdentifier);
                Op03SimpleStatement whileEndTarget = this.targets.get(1);
                boolean pullOutJump = this.index.isBackJumpTo(whileEndTarget);
                if (!pullOutJump && statements.indexOf(lastInBlock) != statements.indexOf(blockEnd) - 1) {
                    pullOutJump = true;
                }
                if (!pullOutJump) break;
                Set<BlockIdentifier> backJumpContainedIn = SetFactory.newSet(this.containedInBlocks);
                backJumpContainedIn.remove(blockIdentifier);
                Op03SimpleStatement backJump = new Op03SimpleStatement(backJumpContainedIn, new GotoStatement(BytecodeLoc.NONE), blockEnd.index.justBefore());
                whileEndTarget.replaceSource(this, backJump);
                this.replaceTarget(whileEndTarget, backJump);
                backJump.addSource(this);
                backJump.addTarget(whileEndTarget);
                int insertAfter = statements.indexOf(blockEnd) - 1;
                while (!statements.get((int)insertAfter).containedInBlocks.containsAll(this.containedInBlocks)) {
                    --insertAfter;
                }
                backJump.index = statements.get((int)insertAfter).index.justAfter();
                statements.add(insertAfter + 1, backJump);
                break;
            }
            case UNCONDITIONALDOLOOP: {
                this.containedStatement.getContainer().replaceStatement(new WhileStatement(BytecodeLoc.TODO, null, blockIdentifier));
                break;
            }
            case DOLOOP: {
                IfStatement ifStatement = (IfStatement)this.containedStatement;
                ifStatement.replaceWithWhileLoopEnd(blockIdentifier);
                break;
            }
            case SIMPLE_IF_ELSE: 
            case SIMPLE_IF_TAKEN: {
                throw new ConfusedCFRException("Shouldn't be marking the comparison of an IF");
            }
            default: {
                throw new ConfusedCFRException("Don't know how to start a block like this");
            }
        }
    }

    public void markFirstStatementInBlock(BlockIdentifier blockIdentifier) {
        if (this.firstStatementInThisBlock != null && this.firstStatementInThisBlock != blockIdentifier && blockIdentifier != null) {
            throw new ConfusedCFRException("Statement already marked as first in another block");
        }
        this.firstStatementInThisBlock = blockIdentifier;
    }

    public void markBlock(BlockIdentifier blockIdentifier) {
        this.containedInBlocks.add(blockIdentifier);
    }

    public void collect(LValueAssignmentAndAliasCondenser lValueAssigmentCollector) {
        this.containedStatement.collectLValueAssignments(lValueAssigmentCollector);
    }

    public void condense(LValueRewriter lValueRewriter) {
        this.containedStatement.replaceSingleUsageLValues(lValueRewriter, this.ssaIdentifiers);
    }

    public void rewrite(ExpressionRewriter expressionRewriter) {
        this.containedStatement.rewriteExpressions(expressionRewriter, this.ssaIdentifiers);
    }

    public void findCreation(CreationCollector creationCollector) {
        this.containedStatement.collectObjectCreation(creationCollector);
    }

    public void clearTargets() {
        this.targets.clear();
    }

    private boolean needsLabel() {
        if (this.sources.size() > 1) {
            return true;
        }
        if (this.sources.size() == 0) {
            return false;
        }
        Op03SimpleStatement source = this.sources.get(0);
        return !source.getIndex().directlyPreceeds(this.getIndex());
    }

    @Override
    public String getLabel() {
        return this.getIndex().toString();
    }

    public void dumpInner(Dumper dumper) {
        if (this.needsLabel()) {
            dumper.print(this.getLabel() + ":").newln();
        }
        for (BlockIdentifier blockIdentifier : this.containedInBlocks) {
            dumper.print(blockIdentifier + " ");
        }
        this.getStatement().dump(dumper);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("**********").newln();
        List<Op03SimpleStatement> reachableNodes = ListFactory.newList();
        GraphVisitorCallee graphVisitorCallee = new GraphVisitorCallee(reachableNodes);
        GraphVisitorDFS<Op03SimpleStatement> visitor = new GraphVisitorDFS<Op03SimpleStatement>(this, graphVisitorCallee);
        visitor.process();
        try {
            Collections.sort(reachableNodes, new CompareByIndex());
        }
        catch (ConfusedCFRException e) {
            dumper.print("CONFUSED!" + e);
        }
        for (Op03SimpleStatement op : reachableNodes) {
            op.dumpInner(dumper);
        }
        dumper.print("**********").newln();
        return dumper;
    }

    private Op04StructuredStatement getStructuredStatementPlaceHolder() {
        return new Op04StructuredStatement(this.index, this.containedInBlocks, this.containedStatement.getStructuredStatement());
    }

    public boolean isCompound() {
        return this.containedStatement.isCompound();
    }

    public List<Op03SimpleStatement> splitCompound() {
        List<Op03SimpleStatement> result = ListFactory.newList();
        List<Statement> innerStatements = this.containedStatement.getCompoundParts();
        InstrIndex nextIndex = this.index.justAfter();
        for (Statement statement : innerStatements) {
            result.add(new Op03SimpleStatement(this.containedInBlocks, statement, nextIndex));
            nextIndex = nextIndex.justAfter();
        }
        ((Op03SimpleStatement)result.get((int)0)).firstStatementInThisBlock = this.firstStatementInThisBlock;
        Op03SimpleStatement previous = null;
        for (Op03SimpleStatement statement : result) {
            if (previous != null) {
                statement.addSource(previous);
                previous.addTarget(statement);
            }
            previous = statement;
        }
        Op03SimpleStatement op03SimpleStatement = (Op03SimpleStatement)result.get(0);
        Op03SimpleStatement newEnd = previous;
        for (Op03SimpleStatement source : this.sources) {
            source.replaceTarget(this, op03SimpleStatement);
            op03SimpleStatement.addSource(source);
        }
        for (Op03SimpleStatement target : this.targets) {
            target.replaceSource(this, newEnd);
            newEnd.addTarget(target);
        }
        this.containedStatement = new Nop();
        this.sources.clear();
        this.targets.clear();
        this.markAgreedNop();
        return result;
    }

    private void collectLocallyMutatedVariables(SSAIdentifierFactory<LValue, ?> ssaIdentifierFactory) {
        this.ssaIdentifiers = this.containedStatement.collectLocallyMutatedVariables(ssaIdentifierFactory);
    }

    public void forceSSAIdentifiers(SSAIdentifiers<LValue> newIdentifiers) {
        this.ssaIdentifiers = newIdentifiers;
    }

    public static void noteInterestingLifetimes(List<Op03SimpleStatement> statements) {
        class RemoveState {
            private Set<LValue> write;
            private Set<LValue> read;

            RemoveState() {
            }
        }
        List<Op03SimpleStatement> wantsHint = ListFactory.newList();
        Set wanted = SetFactory.newSet();
        for (Op03SimpleStatement op03SimpleStatement : statements) {
            Set<LValue> hints = op03SimpleStatement.getStatement().wantsLifetimeHint();
            if (hints == null) continue;
            wantsHint.add(op03SimpleStatement);
            wanted.addAll(hints);
        }
        if (wanted.isEmpty()) {
            return;
        }
        Map state = MapFactory.newIdentityMap();
        for (Op03SimpleStatement stm : statements) {
            LValueUsageCollectorSimpleRW rw = new LValueUsageCollectorSimpleRW();
            stm.getStatement().collectLValueUsage(rw);
            Set<LValue> writes = rw.getWritten();
            Set<LValue> reads = rw.getRead();
            writes.retainAll(wanted);
            reads.retainAll(wanted);
            writes.removeAll(reads);
            RemoveState r = new RemoveState();
            r.write = writes;
            r.read = reads;
            state.put(stm, r);
        }
        List<Op03SimpleStatement> list = Functional.filter(statements, new Predicate<Op03SimpleStatement>(){

            @Override
            public boolean test(Op03SimpleStatement in) {
                return in.getTargets().isEmpty();
            }
        });
        UniqueSeenQueue<Op03SimpleStatement> toProcess = new UniqueSeenQueue<Op03SimpleStatement>(list);
        while (!toProcess.isEmpty()) {
            Op03SimpleStatement node = toProcess.removeFirst();
            RemoveState r = (RemoveState)state.get(node);
            Set tmp = SetFactory.newSet();
            for (Op03SimpleStatement target : node.targets) {
                tmp.addAll(((RemoveState)state.get(target)).read);
            }
            tmp.removeAll(r.write);
            boolean changed = r.read.addAll(tmp);
            boolean addOnlyIfUnseen = !changed;
            for (Op03SimpleStatement source : node.sources) {
                toProcess.add(source, addOnlyIfUnseen);
            }
        }
        for (Op03SimpleStatement hint : wantsHint) {
            Set<LValue> lvs = hint.getStatement().wantsLifetimeHint();
            for (LValue lv : lvs) {
                boolean usedInChildren = false;
                for (Op03SimpleStatement target : hint.getTargets()) {
                    if (!((RemoveState)state.get(target)).read.contains(lv)) continue;
                    usedInChildren = true;
                    break;
                }
                hint.getStatement().setLifetimeHint(lv, usedInChildren);
            }
        }
    }

    public static void assignSSAIdentifiers(Method method, List<Op03SimpleStatement> statements) {
        SSAIdentifierFactory ssaIdentifierFactory = new SSAIdentifierFactory(null);
        List<LocalVariable> params = method.getMethodPrototype().getComputedParameters();
        Map initialSSAValues = MapFactory.newMap();
        for (LocalVariable localVariable : params) {
            initialSSAValues.put(localVariable, ssaIdentifierFactory.getIdent(localVariable));
        }
        SSAIdentifiers initialIdents = new SSAIdentifiers(initialSSAValues);
        for (Op03SimpleStatement statement : statements) {
            statement.collectLocallyMutatedVariables(ssaIdentifierFactory);
        }
        Op03SimpleStatement op03SimpleStatement = statements.get(0);
        UniqueSeenQueue<Op03SimpleStatement> toProcess = new UniqueSeenQueue<Op03SimpleStatement>(statements);
        while (!toProcess.isEmpty()) {
            Op03SimpleStatement statement = toProcess.removeFirst();
            SSAIdentifiers<LValue> ssaIdentifiers = statement.ssaIdentifiers;
            boolean changed = false;
            if (statement == op03SimpleStatement && ssaIdentifiers.mergeWith(initialIdents)) {
                changed = true;
            }
            for (Op03SimpleStatement source : statement.getSources()) {
                if (!ssaIdentifiers.mergeWith(source.ssaIdentifiers)) continue;
                changed = true;
            }
            if (!changed) continue;
            toProcess.addAll(statement.getTargets());
        }
    }

    public static Op04StructuredStatement createInitialStructuredBlock(List<Op03SimpleStatement> statements) {
        GraphConversionHelper<Op03SimpleStatement, Op04StructuredStatement> conversionHelper = new GraphConversionHelper<Op03SimpleStatement, Op04StructuredStatement>();
        List<Op04StructuredStatement> containers = ListFactory.newList();
        for (Op03SimpleStatement statement : statements) {
            Op04StructuredStatement unstructuredStatement = statement.getStructuredStatementPlaceHolder();
            containers.add(unstructuredStatement);
            conversionHelper.registerOriginalAndNew(statement, unstructuredStatement);
        }
        conversionHelper.patchUpRelations();
        return Op04StructuredStatement.buildNestedBlocks(containers);
    }

    public JumpType getJumpType() {
        if (this.containedStatement instanceof JumpingStatement) {
            return ((JumpingStatement)this.containedStatement).getJumpType();
        }
        return JumpType.NONE;
    }

    public void addPossibleExitFor(BlockIdentifier ident) {
        if (this.possibleExitsFor == null) {
            this.possibleExitsFor = SetFactory.newOrderedSet();
        }
        this.possibleExitsFor.add(ident);
    }

    public boolean isPossibleExitFor(BlockIdentifier ident) {
        return this.possibleExitsFor != null && this.possibleExitsFor.contains(ident);
    }

    private static void removePointlessSwitchDefault(Op03SimpleStatement swtch) {
        SwitchStatement switchStatement = (SwitchStatement)swtch.getStatement();
        BlockIdentifier switchBlock = switchStatement.getSwitchBlock();
        if (swtch.getTargets().size() <= 1) {
            return;
        }
        for (Op03SimpleStatement tgt : swtch.getTargets()) {
            CaseStatement caseStatement;
            Statement statement = tgt.getStatement();
            if (!(statement instanceof CaseStatement) || (caseStatement = (CaseStatement)statement).getSwitchBlock() != switchBlock || !caseStatement.isDefault()) continue;
            if (tgt.targets.size() != 1) {
                return;
            }
            Op03SimpleStatement afterTgt = tgt.targets.get(0);
            if (!afterTgt.containedInBlocks.contains(switchBlock)) {
                tgt.nopOut();
                return;
            }
            if (afterTgt.getStatement() instanceof Nop) {
                Op03SimpleStatement aat = afterTgt.targets.get(0);
                if (!aat.containedInBlocks.contains(switchBlock)) {
                    tgt.nopOut();
                    afterTgt.getBlockIdentifiers().retainAll(tgt.getBlockIdentifiers());
                    return;
                }
            }
            if (afterTgt.getStatement().getClass() != GotoStatement.class || afterTgt.linearlyPrevious != tgt || afterTgt.getSources().size() != 1) {
                return;
            }
            if (afterTgt.linearlyNext == afterTgt.targets.get(0)) {
                tgt.nopOut();
                afterTgt.nopOut();
                return;
            }
            return;
        }
    }

    public static void removePointlessSwitchDefaults(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> switches = Functional.filter(statements, new TypeFilter<SwitchStatement>(SwitchStatement.class));
        if (switches.isEmpty()) {
            return;
        }
        Cleaner.reLinkInPlace(statements);
        for (Op03SimpleStatement swtch : switches) {
            Op03SimpleStatement.removePointlessSwitchDefault(swtch);
        }
    }

    public String toString() {
        BytecodeLoc loc = this.getStatement().getCombinedLoc();
        Set blockIds = SetFactory.newSet();
        for (BlockIdentifier b : this.containedInBlocks) {
            blockIds.add(b.getIndex());
        }
        return "@" + loc + ", blocks:" + blockIds + " " + this.index + " : " + this.containedStatement;
    }

    public class GraphVisitorCallee
    implements BinaryProcedure<Op03SimpleStatement, GraphVisitor<Op03SimpleStatement>> {
        private final List<Op03SimpleStatement> reachableNodes;

        GraphVisitorCallee(List<Op03SimpleStatement> reachableNodes) {
            this.reachableNodes = reachableNodes;
        }

        @Override
        public void call(Op03SimpleStatement node, GraphVisitor<Op03SimpleStatement> graphVisitor) {
            this.reachableNodes.add(node);
            for (Op03SimpleStatement target : node.targets) {
                graphVisitor.enqueue(target);
            }
        }
    }
}

