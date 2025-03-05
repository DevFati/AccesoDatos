/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Logger;
import org.benf.cfr.reader.bytecode.AnonymousClassUsage;
import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.MutableGraph;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.AnonymousClassConstructorRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.BadCastChainRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ClashDeclarationReducer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.InnerClassConstructorRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.InstanceofMatchTidyingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LValueReplacingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.NarrowingAssignmentRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PointlessStructuredExpressions;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.RedundantSuperRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ResourceReleaseDetector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SyntheticAccessorRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SyntheticOuterRefRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.VarArgsRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.Op04Checker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.BadLoopPrettifier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.CanRemovePointlessBlock;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ControlFlowCleaningTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.HexLiteralTidier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InstanceOfTreeTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InvalidBooleanCastCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InvalidExpressionStatementCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.LValueTypeClashCheck;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.LambdaCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.NakedNullCaster;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ObjectTypeUsageRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TernaryCastCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesCollapser;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesTransformerJ12;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesTransformerJ7;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesTransformerJ9;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TypeAnnotationTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TypedBooleanTidier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.UnusedAnonymousBlockFlattener;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.VariableNameTidier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ConstantFoldingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.LiteralRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Triplet;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.AbstractLValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverImpl;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LocalClassScopeDiscoverImpl;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredAnonymousBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredGoto;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredWhile;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeInvisibleTypeAnnotations;
import org.benf.cfr.reader.entities.attributes.AttributeRuntimeVisibleTypeAnnotations;
import org.benf.cfr.reader.state.ClassCache;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.collections.StackFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.LoggerFactory;

public class Op04StructuredStatement
implements MutableGraph<Op04StructuredStatement>,
Dumpable,
StatementContainer<StructuredStatement>,
TypeUsageCollectable {
    private static final Logger logger = LoggerFactory.create(Op04StructuredStatement.class);
    private InstrIndex instrIndex;
    private List<Op04StructuredStatement> sources = ListFactory.newList();
    private List<Op04StructuredStatement> targets = ListFactory.newList();
    private StructuredStatement structuredStatement;
    private Set<BlockIdentifier> blockMembership;
    private static final Set<BlockIdentifier> EMPTY_BLOCKSET = SetFactory.newSet();

    private static Set<BlockIdentifier> blockSet(Collection<BlockIdentifier> in) {
        if (in == null || in.isEmpty()) {
            return EMPTY_BLOCKSET;
        }
        return SetFactory.newSet(in);
    }

    public Op04StructuredStatement(StructuredStatement justStatement) {
        this.structuredStatement = justStatement;
        this.instrIndex = new InstrIndex(-1000);
        this.blockMembership = EMPTY_BLOCKSET;
        justStatement.setContainer(this);
    }

    public Op04StructuredStatement(InstrIndex instrIndex, Collection<BlockIdentifier> blockMembership, StructuredStatement structuredStatement) {
        this.instrIndex = instrIndex;
        this.structuredStatement = structuredStatement;
        this.blockMembership = Op04StructuredStatement.blockSet(blockMembership);
        structuredStatement.setContainer(this);
    }

    public static void rewriteExplicitTypeUsages(Method method, Op04StructuredStatement block, AnonymousClassUsage anonymousClassUsage, ClassFile classFile) {
        new ObjectTypeUsageRewriter(anonymousClassUsage, classFile).transform(block);
    }

    public static void flattenNonReferencedBlocks(Op04StructuredStatement block) {
        block.transform(new UnusedAnonymousBlockFlattener(), new StructuredScope());
    }

    public static void switchExpression(Method method, Op04StructuredStatement root, DecompilerComments comments) {
        SwitchExpressionRewriter switchExpressionRewriter = new SwitchExpressionRewriter(comments, method);
        switchExpressionRewriter.transform(root);
    }

    public static void reduceClashDeclarations(Op04StructuredStatement root, BytecodeMeta bytecodeMeta) {
        if (bytecodeMeta.getLivenessClashes().isEmpty()) {
            return;
        }
        root.transform(new ClashDeclarationReducer(bytecodeMeta.getLivenessClashes()), new StructuredScope());
    }

    public static void normalizeInstanceOf(Op04StructuredStatement root, Options options, ClassFileVersion classFileVersion) {
        if (options.getOption(OptionsImpl.INSTANCEOF_PATTERN, classFileVersion).booleanValue()) {
            new InstanceOfTreeTransformer().transform(root);
        }
    }

    public Op04StructuredStatement nopThisAndReplace() {
        Op04StructuredStatement replacement = new Op04StructuredStatement(this.instrIndex, this.blockMembership, this.structuredStatement);
        this.replaceStatementWithNOP("");
        Op04StructuredStatement.replaceInSources(this, replacement);
        Op04StructuredStatement.replaceInTargets(this, replacement);
        return replacement;
    }

    @Override
    public void nopOut() {
        this.replaceStatementWithNOP("");
    }

    @Override
    public StructuredStatement getStatement() {
        return this.structuredStatement;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        if (!collector.isStatementRecursive()) {
            return;
        }
        this.structuredStatement.collectTypeUsages(collector);
    }

    @Override
    public StructuredStatement getTargetStatement(int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLabel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstrIndex getIndex() {
        return this.instrIndex;
    }

    @Override
    public void replaceStatement(StructuredStatement newTarget) {
        this.structuredStatement = newTarget;
        newTarget.setContainer(this);
    }

    @Override
    public void nopOutConditional() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SSAIdentifiers<LValue> getSSAIdentifiers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<BlockIdentifier> getBlockIdentifiers() {
        return this.blockMembership;
    }

    @Override
    public BlockIdentifier getBlockStarted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<BlockIdentifier> getBlocksEnded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyBlockInformationFrom(StatementContainer<StructuredStatement> other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyBytecodeInformationFrom(StatementContainer<StructuredStatement> other) {
        throw new UnsupportedOperationException();
    }

    private boolean hasUnstructuredSource() {
        for (Op04StructuredStatement source : this.sources) {
            if (source.structuredStatement.isProperlyStructured()) continue;
            return true;
        }
        return false;
    }

    public Collection<BlockIdentifier> getBlockMembership() {
        return this.blockMembership;
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.informBytecodeLoc(this.structuredStatement);
        if (this.hasUnstructuredSource()) {
            dumper.label(this.instrIndex.toString(), false).comment(this.sources.size() + " sources").newln();
        }
        dumper.dump(this.structuredStatement);
        return dumper;
    }

    @Override
    public List<Op04StructuredStatement> getSources() {
        return this.sources;
    }

    @Override
    public List<Op04StructuredStatement> getTargets() {
        return this.targets;
    }

    @Override
    public void addSource(Op04StructuredStatement source) {
        this.sources.add(source);
    }

    @Override
    public void addTarget(Op04StructuredStatement target) {
        this.targets.add(target);
    }

    public String getTargetLabel(int idx) {
        return this.targets.get((int)idx).instrIndex.toString();
    }

    public boolean isEmptyInitialiser() {
        List<StructuredStatement> stms = ListFactory.newList();
        this.linearizeStatementsInto(stms);
        for (StructuredStatement stm : stms) {
            Expression expression;
            if (stm instanceof BeginBlock || stm instanceof EndBlock || stm instanceof StructuredComment || stm instanceof StructuredExpressionStatement && (expression = ((StructuredExpressionStatement)stm).getExpression()) instanceof SuperFunctionInvokation && ((SuperFunctionInvokation)expression).isInit()) continue;
            return false;
        }
        return true;
    }

    private void replaceAsSource(Op04StructuredStatement old) {
        Op04StructuredStatement.replaceInSources(old, this);
        this.addTarget(old);
        old.addSource(this);
    }

    public void replaceTarget(Op04StructuredStatement from, Op04StructuredStatement to) {
        int index = this.targets.indexOf(from);
        if (index == -1) {
            throw new ConfusedCFRException("Invalid target.  Trying to replace " + from + " -> " + to);
        }
        this.targets.set(index, to);
    }

    public void replaceSource(Op04StructuredStatement from, Op04StructuredStatement to) {
        int index = this.sources.indexOf(from);
        if (index == -1) {
            throw new ConfusedCFRException("Invalid source");
        }
        this.sources.set(index, to);
    }

    public void setSources(List<Op04StructuredStatement> sources) {
        this.sources = sources;
    }

    public void setTargets(List<Op04StructuredStatement> targets) {
        this.targets = targets;
    }

    public static void replaceInSources(Op04StructuredStatement original, Op04StructuredStatement replacement) {
        for (Op04StructuredStatement source : original.getSources()) {
            source.replaceTarget(original, replacement);
        }
        replacement.setSources(original.getSources());
        original.setSources(ListFactory.<Op04StructuredStatement>newList());
    }

    public static void replaceInTargets(Op04StructuredStatement original, Op04StructuredStatement replacement) {
        for (Op04StructuredStatement target : original.getTargets()) {
            target.replaceSource(original, replacement);
        }
        replacement.setTargets(original.getTargets());
        original.setTargets(ListFactory.<Op04StructuredStatement>newList());
    }

    public void linearizeStatementsInto(List<StructuredStatement> out) {
        this.structuredStatement.linearizeInto(out);
    }

    public void removeLastContinue(BlockIdentifier block) {
        if (!(this.structuredStatement instanceof Block)) {
            throw new ConfusedCFRException("Trying to remove last continue, but statement isn't block");
        }
        boolean removed = ((Block)this.structuredStatement).removeLastContinue(block);
        logger.info("Removing last continue for " + block + " succeeded? " + removed);
    }

    public void removeLastGoto() {
        if (!(this.structuredStatement instanceof Block)) {
            throw new ConfusedCFRException("Trying to remove last goto, but statement isn't a block!");
        }
        ((Block)this.structuredStatement).removeLastGoto();
    }

    public UnstructuredWhile removeLastEndWhile() {
        if (this.structuredStatement instanceof Block) {
            return ((Block)this.structuredStatement).removeLastEndWhile();
        }
        return null;
    }

    public void informBlockMembership(Vector<BlockIdentifier> currentlyIn) {
        StructuredStatement replacement = this.structuredStatement.informBlockHeirachy(currentlyIn);
        if (replacement == null) {
            return;
        }
        this.structuredStatement = replacement;
        replacement.setContainer(this);
    }

    public String toString() {
        return this.structuredStatement.toString();
    }

    public void replaceStatementWithNOP(String comment) {
        this.structuredStatement = new StructuredComment(comment);
        this.structuredStatement.setContainer(this);
    }

    private boolean claimBlock(Op04StructuredStatement innerBlock, BlockIdentifier thisBlock, Vector<BlockIdentifier> currentlyIn) {
        int idx = this.targets.indexOf(innerBlock);
        if (idx == -1) {
            return false;
        }
        StructuredStatement replacement = this.structuredStatement.claimBlock(innerBlock, thisBlock, currentlyIn);
        if (replacement == null) {
            return false;
        }
        this.structuredStatement = replacement;
        replacement.setContainer(this);
        return true;
    }

    private static Set<BlockIdentifier> getEndingBlocks(Stack<BlockIdentifier> wasIn, Set<BlockIdentifier> nowIn) {
        Set<BlockIdentifier> wasCopy = SetFactory.newSet(wasIn);
        wasCopy.removeAll(nowIn);
        return wasCopy;
    }

    private static BlockIdentifier getStartingBlocks(Stack<BlockIdentifier> wasIn, Set<BlockIdentifier> nowIn) {
        if (nowIn.size() <= wasIn.size()) {
            return null;
        }
        Set<BlockIdentifier> nowCopy = SetFactory.newSet(nowIn);
        nowCopy.removeAll(wasIn);
        if (nowCopy.size() != 1) {
            throw new ConfusedCFRException("Started " + nowCopy.size() + " blocks at once");
        }
        return nowCopy.iterator().next();
    }

    private static void processEndingBlocks(Set<BlockIdentifier> endOfTheseBlocks, Stack<BlockIdentifier> blocksCurrentlyIn, Stack<StackedBlock> stackedBlocks, MutableProcessingBlockState mutableProcessingBlockState) {
        logger.fine("statement is last statement in these blocks " + endOfTheseBlocks);
        while (!endOfTheseBlocks.isEmpty()) {
            if (mutableProcessingBlockState.currentBlockIdentifier == null) {
                throw new ConfusedCFRException("Trying to end block, but not in any!");
            }
            if (!endOfTheseBlocks.remove(mutableProcessingBlockState.currentBlockIdentifier)) {
                throw new ConfusedCFRException("Tried to end blocks " + endOfTheseBlocks + ", but top level block is " + mutableProcessingBlockState.currentBlockIdentifier);
            }
            BlockIdentifier popBlockIdentifier = blocksCurrentlyIn.pop();
            if (popBlockIdentifier != mutableProcessingBlockState.currentBlockIdentifier) {
                throw new ConfusedCFRException("Tried to end blocks " + endOfTheseBlocks + ", but top level block is " + mutableProcessingBlockState.currentBlockIdentifier);
            }
            LinkedList<Op04StructuredStatement> blockJustEnded = mutableProcessingBlockState.currentBlock;
            StackedBlock popBlock = stackedBlocks.pop();
            mutableProcessingBlockState.currentBlock = popBlock.statements;
            Op04StructuredStatement finishedBlock = new Op04StructuredStatement(new Block(blockJustEnded, true));
            finishedBlock.replaceAsSource(blockJustEnded.getFirst());
            Op04StructuredStatement blockStartContainer = popBlock.outerStart;
            if (!blockStartContainer.claimBlock(finishedBlock, mutableProcessingBlockState.currentBlockIdentifier, blocksCurrentlyIn)) {
                mutableProcessingBlockState.currentBlock.add(finishedBlock);
            }
            mutableProcessingBlockState.currentBlockIdentifier = popBlock.blockIdentifier;
        }
    }

    public boolean isFullyStructured() {
        return this.structuredStatement.isRecursivelyStructured();
    }

    static Op04StructuredStatement buildNestedBlocks(List<Op04StructuredStatement> containers) {
        Stack<BlockIdentifier> blocksCurrentlyIn = StackFactory.newStack();
        LinkedList<Op04StructuredStatement> outerBlock = ListFactory.newLinkedList();
        Stack<StackedBlock> stackedBlocks = StackFactory.newStack();
        MutableProcessingBlockState mutableProcessingBlockState = new MutableProcessingBlockState();
        mutableProcessingBlockState.currentBlock = outerBlock;
        for (Op04StructuredStatement container : containers) {
            BlockIdentifier startsThisBlock;
            Set<BlockIdentifier> endOfTheseBlocks = Op04StructuredStatement.getEndingBlocks(blocksCurrentlyIn, container.blockMembership);
            if (!endOfTheseBlocks.isEmpty()) {
                Op04StructuredStatement.processEndingBlocks(endOfTheseBlocks, blocksCurrentlyIn, stackedBlocks, mutableProcessingBlockState);
            }
            if ((startsThisBlock = Op04StructuredStatement.getStartingBlocks(blocksCurrentlyIn, container.blockMembership)) != null) {
                logger.fine("Starting block " + startsThisBlock);
                BlockType blockType = startsThisBlock.getBlockType();
                Op04StructuredStatement blockClaimer = mutableProcessingBlockState.currentBlock.getLast();
                stackedBlocks.push(new StackedBlock(mutableProcessingBlockState.currentBlockIdentifier, mutableProcessingBlockState.currentBlock, blockClaimer));
                mutableProcessingBlockState.currentBlock = ListFactory.newLinkedList();
                mutableProcessingBlockState.currentBlockIdentifier = startsThisBlock;
                blocksCurrentlyIn.push(mutableProcessingBlockState.currentBlockIdentifier);
            }
            container.informBlockMembership(blocksCurrentlyIn);
            mutableProcessingBlockState.currentBlock.add(container);
        }
        if (!stackedBlocks.isEmpty()) {
            Op04StructuredStatement.processEndingBlocks(SetFactory.newSet(blocksCurrentlyIn), blocksCurrentlyIn, stackedBlocks, mutableProcessingBlockState);
        }
        Block result = new Block(outerBlock, true);
        return new Op04StructuredStatement(result);
    }

    private static StructuredStatement transformStructuredGotoWithScope(StructuredScope scope, StructuredStatement stm, Stack<Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>>> breaktargets) {
        Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>> breakTarget;
        Op04StructuredStatement target;
        Set<Op04StructuredStatement> nextFallThrough = scope.getNextFallThrough(stm);
        List<Op04StructuredStatement> targets = stm.getContainer().getTargets();
        Op04StructuredStatement op04StructuredStatement = target = targets.isEmpty() ? null : targets.get(0);
        if (nextFallThrough.contains(target)) {
            if (scope.statementIsLast(stm)) {
                return StructuredComment.EMPTY_COMMENT;
            }
            if (scope.getDirectFallThrough().contains(target)) {
                return StructuredComment.EMPTY_COMMENT;
            }
            return stm;
        }
        if (!breaktargets.isEmpty() && (breakTarget = breaktargets.peek()).getThird().contains(target)) {
            return new StructuredBreak(BytecodeLoc.TODO, breakTarget.getSecond(), true);
        }
        return stm;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void transform(StructuredStatementTransformer transformer, StructuredScope scope) {
        StructuredStatement scopeBlock;
        StructuredStatement old = this.structuredStatement;
        StructuredStatement structuredStatement = scopeBlock = this.structuredStatement.isScopeBlock() ? this.structuredStatement : null;
        if (scopeBlock != null) {
            scope.add(scopeBlock);
        }
        try {
            this.structuredStatement = transformer.transform(this.structuredStatement, scope);
            if (this.structuredStatement != old && this.structuredStatement != null) {
                this.structuredStatement.setContainer(this);
            }
        }
        finally {
            if (scopeBlock != null) {
                scope.remove(scopeBlock);
            }
        }
    }

    public static void insertLabelledBlocks(Op04StructuredStatement root) {
        root.transform(new LabelledBlockExtractor(), new StructuredScope());
    }

    public static void tidyEmptyCatch(Op04StructuredStatement root) {
        root.transform(new EmptyCatchTidier(), new StructuredScope());
    }

    public static void tidyTryCatch(Op04StructuredStatement root) {
        root.transform(new TryCatchTidier(), new StructuredScope());
    }

    public static void inlinePossibles(Op04StructuredStatement root) {
        root.transform(new Inliner(), new StructuredScope());
    }

    public static void convertUnstructuredIf(Op04StructuredStatement root) {
        root.transform(new UnstructuredIfConverter(), new StructuredScope());
    }

    public static void tidyVariableNames(Method method, Op04StructuredStatement root, BytecodeMeta bytecodeMeta, DecompilerComments comments, ClassCache classCache) {
        VariableNameTidier variableNameTidier = new VariableNameTidier(method, VariableNameTidier.NameDiscoverer.getUsedLambdaNames(bytecodeMeta, root), classCache);
        variableNameTidier.transform(root);
        if (variableNameTidier.isClassRenamed()) {
            comments.addComment(DecompilerComment.CLASS_RENAMED);
        }
    }

    public static void applyTypeAnnotations(AttributeCode code, Op04StructuredStatement root, SortedMap<Integer, Integer> instrsByOffset, DecompilerComments comments) {
        AttributeRuntimeVisibleTypeAnnotations vis = code.getRuntimeVisibleTypeAnnotations();
        AttributeRuntimeInvisibleTypeAnnotations invis = code.getRuntimeInvisibleTypeAnnotations();
        if (vis == null && invis == null) {
            return;
        }
        TypeAnnotationTransformer transformer = new TypeAnnotationTransformer(vis, invis, instrsByOffset, comments);
        transformer.transform(root);
    }

    public static void removePointlessReturn(Op04StructuredStatement root) {
        StructuredStatement statement = root.getStatement();
        if (statement instanceof Block) {
            Block block = (Block)statement;
            block.removeLastNVReturn();
        }
    }

    public static void removeEndResource(ClassFile classFile, Op04StructuredStatement root) {
        boolean s1 = new TryResourcesTransformerJ9(classFile).transform(root);
        boolean s2 = new TryResourcesTransformerJ7(classFile).transform(root);
        boolean s3 = new TryResourcesTransformerJ12(classFile).transform(root);
        if (s1 || s2 || s3) {
            new TryResourcesCollapser().transform(root);
        }
    }

    public static void removePointlessControlFlow(Op04StructuredStatement root) {
        new ControlFlowCleaningTransformer().transform(root);
    }

    public static void tidyTypedBooleans(Op04StructuredStatement root) {
        new TypedBooleanTidier().transform(root);
    }

    public static void miscKeyholeTransforms(VariableFactory variableFactory, Op04StructuredStatement root) {
        new NakedNullCaster().transform(root);
        new LambdaCleaner().transform(root);
        new TernaryCastCleaner().transform(root);
        new InvalidBooleanCastCleaner().transform(root);
        new HexLiteralTidier().transform(root);
        new ExpressionRewriterTransformer(LiteralRewriter.INSTANCE).transform(root);
        new InvalidExpressionStatementCleaner(variableFactory).transform(root);
    }

    public static void tidyObfuscation(Options options, Op04StructuredStatement root) {
        if (((Boolean)options.getOption(OptionsImpl.CONST_OBF)).booleanValue()) {
            new ExpressionRewriterTransformer(ConstantFoldingRewriter.INSTANCE).transform(root);
        }
    }

    public static void prettifyBadLoops(Op04StructuredStatement root) {
        new BadLoopPrettifier().transform(root);
    }

    public static void removeStructuredGotos(Op04StructuredStatement root) {
        root.transform(new StructuredGotoRemover(), new StructuredScope());
    }

    public static void removeUnnecessaryLabelledBreaks(Op04StructuredStatement root) {
        root.transform(new NamedBreakRemover(), new StructuredScope());
    }

    public static void removePointlessBlocks(Op04StructuredStatement root) {
        root.transform(new PointlessBlockRemover(), new StructuredScope());
    }

    public static void discoverVariableScopes(Method method, Op04StructuredStatement root, VariableFactory variableFactory, Options options, ClassFileVersion classFileVersion, BytecodeMeta bytecodeMeta) {
        LValueScopeDiscoverImpl scopeDiscoverer = new LValueScopeDiscoverImpl(options, method.getMethodPrototype(), variableFactory, classFileVersion);
        scopeDiscoverer.processOp04Statement(root);
        scopeDiscoverer.markDiscoveredCreations();
        if (scopeDiscoverer.didDetectInstanceOfMatching()) {
            bytecodeMeta.set(BytecodeMeta.CodeInfoFlag.INSTANCE_OF_MATCHES);
        }
    }

    public static void discoverLocalClassScopes(Method method, Op04StructuredStatement root, VariableFactory variableFactory, Options options) {
        LocalClassScopeDiscoverImpl scopeDiscoverer = new LocalClassScopeDiscoverImpl(options, method, variableFactory);
        ((AbstractLValueScopeDiscoverer)scopeDiscoverer).processOp04Statement(root);
        scopeDiscoverer.markDiscoveredCreations();
    }

    public static void tidyInstanceMatches(Op04StructuredStatement block) {
        InstanceofMatchTidyingRewriter.rewrite(block);
    }

    public static boolean checkTypeClashes(Op04StructuredStatement block, BytecodeMeta bytecodeMeta) {
        LValueTypeClashCheck clashCheck = new LValueTypeClashCheck();
        clashCheck.processOp04Statement(block);
        Set<Integer> clashes = clashCheck.getClashes();
        if (!clashes.isEmpty()) {
            bytecodeMeta.informLivenessClashes(clashes);
            return true;
        }
        return false;
    }

    public static FieldVariable findInnerClassOuterThis(Method method, Op04StructuredStatement root) {
        MethodPrototype prototype = method.getMethodPrototype();
        List<LocalVariable> vars = prototype.getComputedParameters();
        if (vars.isEmpty()) {
            return null;
        }
        LocalVariable outerThis = vars.get(0);
        InnerClassConstructorRewriter innerClassConstructorRewriter = new InnerClassConstructorRewriter(method.getClassFile(), outerThis);
        innerClassConstructorRewriter.rewrite(root);
        FieldVariable matchedLValue = innerClassConstructorRewriter.getMatchedField();
        return matchedLValue;
    }

    public static void removeInnerClassOuterThis(Method method, Op04StructuredStatement root) {
        MethodPrototype prototype = method.getMethodPrototype();
        List<LocalVariable> vars = prototype.getComputedParameters();
        if (vars.isEmpty()) {
            return;
        }
        LocalVariable outerThis = vars.get(0);
        InnerClassConstructorRewriter innerClassConstructorRewriter = new InnerClassConstructorRewriter(method.getClassFile(), outerThis);
        innerClassConstructorRewriter.rewrite(root);
        FieldVariable matchedLValue = innerClassConstructorRewriter.getMatchedField();
        if (matchedLValue == null) {
            return;
        }
        Map<LValue, LValue> replacements = MapFactory.newMap();
        replacements.put(outerThis, matchedLValue);
        innerClassConstructorRewriter.getAssignmentStatement().getContainer().nopOut();
        prototype.setInnerOuterThis();
        prototype.hide(0);
        Op04StructuredStatement.applyLValueReplacer(replacements, root);
    }

    private static void removeMethodScopedSyntheticConstructorOuterArgs(Method method, Op04StructuredStatement root, Set<MethodPrototype> processed) {
        class CaptureExpression {
            private int idx;
            private Set<Expression> captures = new HashSet<Expression>();

            CaptureExpression(int idx) {
                this.idx = idx;
            }
        }
        MethodPrototype prototype = method.getMethodPrototype();
        if (!processed.add(prototype)) {
            return;
        }
        List<MethodPrototype.ParameterLValue> vars = prototype.getParameterLValues();
        if (vars.isEmpty()) {
            return;
        }
        List<ConstructorInvokationSimple> usages = method.getClassFile().getMethodUsages();
        if (usages.isEmpty()) {
            return;
        }
        IdentityHashMap<MethodPrototype, MethodPrototype> protos = new IdentityHashMap<MethodPrototype, MethodPrototype>();
        Map captured = MapFactory.newIdentityMap();
        for (ConstructorInvokationSimple constructorInvokationSimple : usages) {
            List<Expression> args = constructorInvokationSimple.getArgs();
            MethodPrototype proto = constructorInvokationSimple.getConstructorPrototype();
            protos.put(proto, proto);
            for (int x = 0; x < vars.size(); ++x) {
                MethodPrototype.ParameterLValue var = vars.get(x);
                if (!var.isHidden() && !proto.isHiddenArg(x)) continue;
                CaptureExpression capture = (CaptureExpression)captured.get(var);
                if (capture == null) {
                    capture = new CaptureExpression(x);
                    captured.put(var, capture);
                }
                capture.captures.add(args.get(x));
            }
        }
        MethodPrototype callProto = null;
        switch (protos.size()) {
            case 0: {
                return;
            }
            case 1: {
                callProto = (MethodPrototype)SetUtil.getSingle(protos.keySet());
                break;
            }
            default: {
                for (MethodPrototype proto : protos.keySet()) {
                    if (!proto.equalsMatch(prototype)) continue;
                    if (callProto == null) {
                        callProto = proto;
                        continue;
                    }
                    return;
                }
            }
        }
        if (callProto == null) {
            return;
        }
        ClassFile classFile = method.getClassFile();
        for (int x = 0; x < vars.size(); ++x) {
            LValue lValueArg;
            String overrideName;
            Expression expr;
            MethodPrototype.ParameterLValue parameterLValue = vars.get(x);
            CaptureExpression captureExpression = (CaptureExpression)captured.get(parameterLValue);
            if (captureExpression == null || captureExpression.captures.size() != 1 || !((expr = (Expression)captureExpression.captures.iterator().next()) instanceof LValueExpression) || (overrideName = Op04StructuredStatement.getInnerClassOuterArgName(method, lValueArg = ((LValueExpression)expr).getLValue())) == null) continue;
            if (parameterLValue.hidden == MethodPrototype.HiddenReason.HiddenOuterReference) {
                if (prototype.isInnerOuterThis()) {
                    if (!prototype.isHiddenArg(captureExpression.idx)) continue;
                    callProto.hide(captureExpression.idx);
                    continue;
                }
                Op04StructuredStatement.hideField(root, callProto, classFile, captureExpression.idx, parameterLValue.localVariable, lValueArg, overrideName);
                continue;
            }
            if (parameterLValue.hidden != MethodPrototype.HiddenReason.HiddenCapture && !callProto.isHiddenArg(x)) continue;
            Op04StructuredStatement.hideField(root, callProto, classFile, captureExpression.idx, parameterLValue.localVariable, lValueArg, overrideName);
        }
    }

    private static void removeAnonymousSyntheticConstructorOuterArgs(Method method, Op04StructuredStatement root, boolean isInstance) {
        ConstructorInvokationAnonymousInner usage;
        MethodPrototype prototype = method.getMethodPrototype();
        List<LocalVariable> vars = prototype.getComputedParameters();
        if (vars.isEmpty()) {
            return;
        }
        Map<LValue, LValue> replacements = MapFactory.newMap();
        List<ConstructorInvokationAnonymousInner> usages = method.getClassFile().getAnonymousUsages();
        ConstructorInvokationAnonymousInner constructorInvokationAnonymousInner = usage = usages.size() == 1 ? usages.get(0) : null;
        if (usage == null) {
            return;
        }
        List<Expression> actualArgs = usage.getArgs();
        if (actualArgs.size() != vars.size()) {
            return;
        }
        int start = isInstance ? 1 : 0;
        ClassFile classFile = method.getClassFile();
        int len = vars.size();
        for (int x = start; x < len; ++x) {
            LValue lValueArg;
            String overrideName;
            LocalVariable protoVar = vars.get(x);
            Expression arg = actualArgs.get(x);
            if (!((arg = CastExpression.removeImplicit(arg)) instanceof LValueExpression) || (overrideName = Op04StructuredStatement.getInnerClassOuterArgName(method, lValueArg = ((LValueExpression)arg).getLValue())) == null) continue;
            Op04StructuredStatement.hideField(root, prototype, classFile, x, protoVar, lValueArg, overrideName);
        }
        Op04StructuredStatement.applyLValueReplacer(replacements, root);
    }

    private static String getInnerClassOuterArgName(Method method, LValue lValueArg) {
        String overrideName = null;
        if (lValueArg instanceof LocalVariable) {
            LocalVariable localVariable = (LocalVariable)lValueArg;
            overrideName = localVariable.getName().getStringName();
        } else if (lValueArg instanceof FieldVariable) {
            FieldVariable fv = (FieldVariable)lValueArg;
            JavaTypeInstance thisClass = method.getClassFile().getClassType();
            JavaTypeInstance fieldClass = fv.getOwningClassType();
            boolean isInner = thisClass.getInnerClassHereInfo().isTransitiveInnerClassOf(fieldClass);
            if (isInner) {
                overrideName = fv.getFieldName();
            }
        }
        return overrideName;
    }

    private static void hideField(Op04StructuredStatement root, MethodPrototype prototype, ClassFile classFile, int x, LocalVariable protoVar, LValue lValueArg, String overrideName) {
        InnerClassConstructorRewriter innerClassConstructorRewriter = new InnerClassConstructorRewriter(classFile, protoVar);
        innerClassConstructorRewriter.rewrite(root);
        FieldVariable matchedField = innerClassConstructorRewriter.getMatchedField();
        if (matchedField == null) {
            return;
        }
        innerClassConstructorRewriter.getAssignmentStatement().getContainer().nopOut();
        ClassFileField classFileField = matchedField.getClassFileField();
        classFileField.overrideName(overrideName);
        classFileField.markSyntheticOuterRef();
        classFileField.markHidden();
        prototype.hide(x);
        lValueArg.markFinal();
    }

    private static void applyLValueReplacer(Map<LValue, LValue> replacements, Op04StructuredStatement root) {
        if (!replacements.isEmpty()) {
            LValueReplacingRewriter lValueReplacingRewriter = new LValueReplacingRewriter(replacements);
            MiscStatementTools.applyExpressionRewriter(root, lValueReplacingRewriter);
        }
    }

    public static void fixInnerClassConstructorSyntheticOuterArgs(ClassFile classFile, Method method, Op04StructuredStatement root, Set<MethodPrototype> processed) {
        if (classFile.isInnerClass()) {
            boolean instance = !classFile.testAccessFlag(AccessFlag.ACC_STATIC);
            Op04StructuredStatement.removeAnonymousSyntheticConstructorOuterArgs(method, root, instance);
            Op04StructuredStatement.removeMethodScopedSyntheticConstructorOuterArgs(method, root, processed);
        }
    }

    public static void tidyAnonymousConstructors(Op04StructuredStatement root) {
        root.transform(new ExpressionRewriterTransformer(new AnonymousClassConstructorRewriter()), new StructuredScope());
    }

    public static void inlineSyntheticAccessors(DCCommonState state, Method method, Op04StructuredStatement root) {
        JavaTypeInstance classType = method.getClassFile().getClassType();
        new SyntheticAccessorRewriter(state, classType).rewrite(root);
    }

    public static void removeConstructorBoilerplate(Op04StructuredStatement root) {
        new RedundantSuperRewriter().rewrite(root);
    }

    public static void rewriteLambdas(DCCommonState state, Method method, Op04StructuredStatement root) {
        Options options = state.getOptions();
        if (!options.getOption(OptionsImpl.REWRITE_LAMBDAS, method.getClassFile().getClassFileVersion()).booleanValue()) {
            return;
        }
        new LambdaRewriter(state, method).rewrite(root);
    }

    public static void removeUnnecessaryVarargArrays(Options options, Method method, Op04StructuredStatement root) {
        new VarArgsRewriter().rewrite(root);
    }

    public static void removePrimitiveDeconversion(Options options, Method method, Op04StructuredStatement root) {
        if (!((Boolean)options.getOption(OptionsImpl.SUGAR_BOXING)).booleanValue()) {
            return;
        }
        root.transform(new ExpressionRewriterTransformer(new PrimitiveBoxingRewriter()), new StructuredScope());
    }

    public static void rewriteBadCastChains(Options options, Method method, Op04StructuredStatement root) {
        root.transform(new ExpressionRewriterTransformer(new BadCastChainRewriter()), new StructuredScope());
    }

    public static void rewriteNarrowingAssignments(Options options, Method method, Op04StructuredStatement root) {
        new NarrowingAssignmentRewriter().rewrite(root);
    }

    public static void replaceNestedSyntheticOuterRefs(Op04StructuredStatement root) {
        List<StructuredStatement> statements = MiscStatementTools.linearise(root);
        if (statements == null) {
            return;
        }
        SyntheticOuterRefRewriter syntheticOuterRefRewriter = new SyntheticOuterRefRewriter();
        for (StructuredStatement statement : statements) {
            statement.rewriteExpressions(syntheticOuterRefRewriter);
            PointlessStructuredExpressions.removePointlessExpression(statement);
        }
    }

    public static void applyChecker(Op04Checker checker, Op04StructuredStatement root, DecompilerComments comments) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(checker, structuredScope);
        checker.commentInto(comments);
    }

    public static boolean isTryWithResourceSynthetic(Method m, Op04StructuredStatement root) {
        return ResourceReleaseDetector.isResourceRelease(m, root);
    }

    private static class PointlessBlockRemover
    implements StructuredStatementTransformer {
        private PointlessBlockRemover() {
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            if (in instanceof CanRemovePointlessBlock) {
                ((CanRemovePointlessBlock)((Object)in)).removePointlessBlocks(scope);
            }
            return in;
        }
    }

    private static class NamedBreakRemover
    extends ScopeDescendingTransformer {
        private NamedBreakRemover() {
        }

        @Override
        protected StructuredStatement doTransform(StructuredStatement statement, Stack<Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>>> targets, StructuredScope scope) {
            if (statement instanceof StructuredBreak) {
                statement = ((StructuredBreak)statement).maybeTightenToLocal(targets);
            }
            return statement;
        }
    }

    private static class StructuredGotoRemover
    extends ScopeDescendingTransformer {
        private StructuredGotoRemover() {
        }

        @Override
        protected StructuredStatement doTransform(StructuredStatement statement, Stack<Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>>> targets, StructuredScope scope) {
            if (statement instanceof UnstructuredGoto || statement instanceof UnstructuredAnonymousBreak) {
                statement = Op04StructuredStatement.transformStructuredGotoWithScope(scope, statement, targets);
            }
            return statement;
        }
    }

    private static abstract class ScopeDescendingTransformer
    implements StructuredStatementTransformer {
        private final Stack<Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>>> targets = new Stack();

        private ScopeDescendingTransformer() {
        }

        protected abstract StructuredStatement doTransform(StructuredStatement var1, Stack<Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>>> var2, StructuredScope var3);

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            BlockIdentifier breakableBlock = in.getBreakableBlockOrNull();
            if (breakableBlock != null) {
                Set<Op04StructuredStatement> next = scope.getNextFallThrough(in);
                this.targets.push(Triplet.make(in, breakableBlock, next));
            }
            StructuredStatement out = in;
            try {
                out.transformStructuredChildrenInReverse(this, scope);
                out = this.doTransform(out, this.targets, scope);
                if (out instanceof StructuredBreak) {
                    out = ((StructuredBreak)out).maybeTightenToLocal(this.targets);
                }
            }
            finally {
                if (breakableBlock != null) {
                    this.targets.pop();
                }
            }
            return out;
        }
    }

    public static class UnstructuredIfConverter
    implements StructuredStatementTransformer {
        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            if (in instanceof UnstructuredIf) {
                in = ((UnstructuredIf)in).convertEmptyToGoto();
            }
            return in;
        }
    }

    private static class Inliner
    implements StructuredStatementTransformer {
        private Inliner() {
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            if (in instanceof Block) {
                Block block = (Block)in;
                block.combineInlineable();
            }
            return in;
        }
    }

    private static class TryCatchTidier
    implements StructuredStatementTransformer {
        private TryCatchTidier() {
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            if (in instanceof Block) {
                Block block = (Block)in;
                block.combineTryCatch();
            }
            in.transformStructuredChildren(this, scope);
            return in;
        }
    }

    private static class EmptyCatchTidier
    implements StructuredStatementTransformer {
        private EmptyCatchTidier() {
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            if (in instanceof UnstructuredCatch) {
                return ((UnstructuredCatch)in).getCatchForEmpty();
            }
            in.transformStructuredChildren(this, scope);
            return in;
        }
    }

    private static class LabelledBlockExtractor
    implements StructuredStatementTransformer {
        private LabelledBlockExtractor() {
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            if (in instanceof Block) {
                Block block = (Block)in;
                block.extractLabelledBlocks();
            }
            in.transformStructuredChildren(this, scope);
            return in;
        }
    }

    private static class MutableProcessingBlockState {
        BlockIdentifier currentBlockIdentifier = null;
        LinkedList<Op04StructuredStatement> currentBlock = ListFactory.newLinkedList();

        private MutableProcessingBlockState() {
        }
    }

    private static class StackedBlock {
        BlockIdentifier blockIdentifier;
        LinkedList<Op04StructuredStatement> statements;
        Op04StructuredStatement outerStart;

        private StackedBlock(BlockIdentifier blockIdentifier, LinkedList<Op04StructuredStatement> statements, Op04StructuredStatement outerStart) {
            this.blockIdentifier = blockIdentifier;
            this.statements = statements;
            this.outerStart = outerStart;
        }
    }
}

