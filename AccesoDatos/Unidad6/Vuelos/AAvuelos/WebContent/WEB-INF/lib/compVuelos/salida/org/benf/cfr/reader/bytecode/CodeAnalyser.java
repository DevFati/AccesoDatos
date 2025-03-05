/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.benf.cfr.reader.bytecode.AnalysisResult;
import org.benf.cfr.reader.bytecode.AnalysisResultFromException;
import org.benf.cfr.reader.bytecode.AnalysisResultSuccessful;
import org.benf.cfr.reader.bytecode.AnonymousClassUsage;
import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.bytecode.RecoveryOption;
import org.benf.cfr.reader.bytecode.RecoveryOptions;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactory;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactoryImpl;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactoryStub;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03Blocks;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op02obf.Op02Obf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.GetClassTestInnerConstructor;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.GetClassTestLambda;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.Op02GetClassRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.Op02RedundantStoreRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.TypeHintRecovery;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.TypeHintRecoveryImpl;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.TypeHintRecoveryNone;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.AnonymousArray;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.BadBoolAssignmentRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.BadNarrowingArgRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.ConditionalRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.ExceptionRewriters;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.FinallyRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.GenericInferer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.InlineDeAssigner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.IterLoopRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.KotlinSwitchHandler;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LValueProp;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LValuePropSimple;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopIdentifier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.NullTypedLValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Op03Rewriters;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.RemoveDeterministicJumps;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.StaticInitReturnRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.SwitchReplacer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.SynchronizedBlocks;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchEnumRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchStringRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.IllegalReturnChecker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.LooseCatchChecker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.VoidVariableChecker;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExplicitTypeCallRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.StringBuilderRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.XorRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.exceptions.ExceptionAggregator;
import org.benf.cfr.reader.entities.exceptions.ExceptionTableEntry;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.UnverifiableJumpException;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;

public class CodeAnalyser {
    private final AttributeCode originalCodeAttribute;
    private final ConstantPool cp;
    private Method method;
    private Op04StructuredStatement analysed;
    private static final Op04StructuredStatement POISON = new Op04StructuredStatement(new StructuredComment("Analysis utterly failed (Recursive inlining?)"));
    private static final RecoveryOptions recover0 = new RecoveryOptions(new RecoveryOption.TrooleanRO(OptionsImpl.RECOVER_TYPECLASHES, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.LIVENESS_CLASH)), new RecoveryOption.TrooleanRO(OptionsImpl.USE_RECOVERED_ITERATOR_TYPE_HINTS, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.ITERATED_TYPE_HINTS)), new RecoveryOption.BooleanRO(OptionsImpl.STATIC_INIT_RETURN, Boolean.FALSE));
    private static final RecoveryOptions recoverExAgg = new RecoveryOptions(new RecoveryOption.TrooleanRO(OptionsImpl.RECOVER_TYPECLASHES, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.LIVENESS_CLASH)), new RecoveryOption.TrooleanRO(OptionsImpl.USE_RECOVERED_ITERATOR_TYPE_HINTS, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.ITERATED_TYPE_HINTS)), new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_EXCEPTIONS), DecompilerComment.AGGRESSIVE_EXCEPTION_AGG));
    private static final RecoveryOptions recover0a = new RecoveryOptions(recover0, new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_COND_PROPAGATE, Troolean.TRUE, DecompilerComment.COND_PROPAGATE), new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_RETURNING_IFS, Troolean.TRUE, DecompilerComment.RETURNING_IFS));
    private static final RecoveryOptions recoverPre1 = new RecoveryOptions(recover0, new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_TOPSORT, Troolean.TRUE, DecompilerComment.AGGRESSIVE_TOPOLOGICAL_SORT), new RecoveryOption.TrooleanRO(OptionsImpl.REDUCE_COND_SCOPE, Troolean.TRUE), new RecoveryOption.TrooleanRO(OptionsImpl.AGGRESSIVE_DUFF, Troolean.TRUE), new RecoveryOption.TrooleanRO(OptionsImpl.FOR_LOOP_CAPTURE, Troolean.TRUE), new RecoveryOption.BooleanRO(OptionsImpl.LENIENT, Boolean.TRUE), new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_COND_PROPAGATE, Troolean.TRUE), new RecoveryOption.TrooleanRO(OptionsImpl.REMOVE_DEAD_CONDITIONALS, Troolean.TRUE), new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_PRUNE_EXCEPTIONS, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_EXCEPTIONS), DecompilerComment.PRUNE_EXCEPTIONS), new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_EXCEPTIONS), DecompilerComment.AGGRESSIVE_EXCEPTION_AGG));
    private static final RecoveryOptions recover1 = new RecoveryOptions(recoverPre1, new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_TOPSORT_NOPULL, Troolean.TRUE));
    private static final RecoveryOptions recover2 = new RecoveryOptions(recover1, new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_TOPSORT_EXTRA, Troolean.TRUE), new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG2, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_EXCEPTIONS)));
    private static final RecoveryOptions recover3 = new RecoveryOptions(recover1, new RecoveryOption.BooleanRO(OptionsImpl.COMMENT_MONITORS, Boolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_MONITORS), DecompilerComment.COMMENT_MONITORS), new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_RETURNING_IFS, Troolean.TRUE, DecompilerComment.RETURNING_IFS));
    private static final RecoveryOptions recover3a = new RecoveryOptions(recover1, new RecoveryOption.IntRO(OptionsImpl.AGGRESSIVE_DO_COPY, 4), new RecoveryOption.TrooleanRO(OptionsImpl.AGGRESSIVE_DO_EXTENSION, Troolean.TRUE), new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_TOPSORT_EXTRA, Troolean.TRUE), new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG2, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_EXCEPTIONS)));
    private static final RecoveryOptions recoverIgnoreExceptions = new RecoveryOptions(recover3, new RecoveryOption.BooleanRO(OptionsImpl.IGNORE_EXCEPTIONS_ALWAYS, true, BytecodeMeta.checkParam(OptionsImpl.IGNORE_EXCEPTIONS), DecompilerComment.DROP_EXCEPTIONS));
    private static final RecoveryOptions recoverMalformed2a = new RecoveryOptions(recover2, new RecoveryOption.TrooleanRO(OptionsImpl.ALLOW_MALFORMED_SWITCH, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.MALFORMED_SWITCH)));
    private static final RecoveryOptions[] recoveryOptionsArr = new RecoveryOptions[]{recover0, recover0a, recoverPre1, recover1, recover2, recoverExAgg, recover3, recover3a, recoverIgnoreExceptions, recoverMalformed2a};

    public CodeAnalyser(AttributeCode attributeCode) {
        this.originalCodeAttribute = attributeCode;
        this.cp = attributeCode.getConstantPool();
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Op04StructuredStatement getAnalysis(DCCommonState dcCommonState) {
        AnalysisResult res;
        if (this.analysed == POISON) {
            throw new ConfusedCFRException("Recursive analysis");
        }
        if (this.analysed != null) {
            return this.analysed;
        }
        this.analysed = POISON;
        Options options = dcCommonState.getOptions();
        List<Op01WithProcessedDataAndByteJumps> instrs = this.getInstrs();
        BytecodeMeta bytecodeMeta = new BytecodeMeta(instrs, this.originalCodeAttribute, options);
        if (options.optionIsSet(OptionsImpl.FORCE_PASS)) {
            int pass = (Integer)options.getOption(OptionsImpl.FORCE_PASS);
            if (pass < 0 || pass >= recoveryOptionsArr.length) {
                throw new IllegalArgumentException("Illegal recovery pass idx");
            }
            RecoveryOptions.Applied applied = recoveryOptionsArr[pass].apply(dcCommonState, options, bytecodeMeta);
            res = this.getAnalysisOrWrapFail(pass, instrs, dcCommonState, applied.options, applied.comments, bytecodeMeta);
        } else {
            res = this.getAnalysisOrWrapFail(0, instrs, dcCommonState, options, null, bytecodeMeta);
            if (res.isFailed() && ((Boolean)options.getOption(OptionsImpl.RECOVER)).booleanValue()) {
                int passIdx = 1;
                for (RecoveryOptions recoveryOptions : recoveryOptionsArr) {
                    RecoveryOptions.Applied applied = recoveryOptions.apply(dcCommonState, options, bytecodeMeta);
                    if (!applied.valid) continue;
                    AnalysisResult nextRes = this.getAnalysisOrWrapFail(passIdx++, instrs, dcCommonState, applied.options, applied.comments, bytecodeMeta);
                    if (res.isFailed() && nextRes.isFailed()) {
                        if (!nextRes.isThrown()) {
                            if (res.isThrown()) {
                                res = nextRes;
                            } else if (res.getComments().contains(DecompilerComment.UNABLE_TO_STRUCTURE) && !nextRes.getComments().contains(DecompilerComment.UNABLE_TO_STRUCTURE)) {
                                res = nextRes;
                            }
                        }
                    } else {
                        res = nextRes;
                    }
                    if (!res.isFailed()) break;
                }
            }
        }
        if (res.getComments() != null) {
            this.method.setComments(res.getComments());
        }
        res.getAnonymousClassUsage().useNotes();
        this.analysed = res.getCode();
        return this.analysed;
    }

    private Op01WithProcessedDataAndByteJumps getSingleInstr(ByteData rawCode, int offset) {
        OffsettingByteData bdCode = rawCode.getOffsettingOffsetData(offset);
        JVMInstr instr = JVMInstr.find(bdCode.getS1At(0L));
        return instr.createOperation(bdCode, this.cp, offset);
    }

    private List<Op01WithProcessedDataAndByteJumps> getInstrs() {
        int length;
        ByteData rawCode = this.originalCodeAttribute.getRawData();
        long codeLength = this.originalCodeAttribute.getCodeLength();
        ArrayList<Op01WithProcessedDataAndByteJumps> instrs = new ArrayList<Op01WithProcessedDataAndByteJumps>();
        OffsettingByteData bdCode = rawCode.getOffsettingOffsetData(0L);
        int offset = 0;
        instrs.add(JVMInstr.NOP.createOperation(null, this.cp, -1));
        do {
            JVMInstr instr = JVMInstr.find(bdCode.getS1At(0L));
            Op01WithProcessedDataAndByteJumps oc = instr.createOperation(bdCode, this.cp, offset);
            length = oc.getInstructionLength();
            instrs.add(oc);
            bdCode.advance(length);
        } while ((long)(offset += length) < codeLength);
        return instrs;
    }

    private AnalysisResult getAnalysisOrWrapFail(int passIdx, List<Op01WithProcessedDataAndByteJumps> instrs, DCCommonState commonState, Options options, List<DecompilerComment> extraComments, BytecodeMeta bytecodeMeta) {
        try {
            AnalysisResult res = this.getAnalysisInner(instrs, commonState, options, bytecodeMeta, passIdx);
            if (extraComments != null) {
                res.getComments().addComments(extraComments);
            }
            return res;
        }
        catch (RuntimeException e) {
            return new AnalysisResultFromException(e);
        }
    }

    private AnalysisResult getAnalysisInner(List<Op01WithProcessedDataAndByteJumps> instrs, DCCommonState dcCommonState, Options options, BytecodeMeta bytecodeMeta, int passIdx) {
        boolean reloop;
        ExceptionAggregator exceptions;
        int x;
        boolean aggressiveSizeReductions;
        boolean willSort = options.getOption(OptionsImpl.FORCE_TOPSORT) == Troolean.TRUE;
        ClassFile classFile = this.method.getClassFile();
        ClassFileVersion classFileVersion = classFile.getClassFileVersion();
        DecompilerComments comments = new DecompilerComments();
        boolean bl = aggressiveSizeReductions = (Integer)options.getOption(OptionsImpl.AGGRESSIVE_SIZE_REDUCTION_THRESHOLD) < instrs.size();
        if (aggressiveSizeReductions) {
            comments.addComment("Opcode count of " + instrs.size() + " triggered aggressive code reduction.  Override with --" + OptionsImpl.AGGRESSIVE_SIZE_REDUCTION_THRESHOLD.getName() + ".");
        }
        TreeMap<Integer, Integer> lutByOffset = new TreeMap<Integer, Integer>();
        HashMap<Integer, Integer> lutByIdx = new HashMap<Integer, Integer>();
        int idx2 = 0;
        int offset2 = -1;
        for (Op01WithProcessedDataAndByteJumps op : instrs) {
            lutByOffset.put(offset2, idx2);
            lutByIdx.put(idx2, offset2);
            offset2 += op.getInstructionLength();
            ++idx2;
        }
        lutByIdx.put(0, -1);
        lutByOffset.put(-1, 0);
        List<Op01WithProcessedDataAndByteJumps> op1list = ListFactory.newList();
        List<Op02WithProcessedDataAndRefs> op2list = ListFactory.newList();
        BytecodeLocFactory locFactory = (Boolean)options.getOption(OptionsImpl.TRACK_BYTECODE_LOC) != false ? BytecodeLocFactoryImpl.INSTANCE : BytecodeLocFactoryStub.INSTANCE;
        for (x = 0; x < instrs.size(); ++x) {
            Op01WithProcessedDataAndByteJumps op1 = instrs.get(x);
            op1list.add(op1);
            Op02WithProcessedDataAndRefs op2 = op1.createOp2(this.cp, x, locFactory, this.method);
            op2list.add(op2);
        }
        int len = op1list.size();
        for (x = 0; x < len; ++x) {
            int[] targetIdxs;
            int offsetOfThisInstruction = (Integer)lutByIdx.get(x);
            try {
                targetIdxs = ((Op01WithProcessedDataAndByteJumps)op1list.get(x)).getAbsoluteIndexJumps(offsetOfThisInstruction, lutByOffset);
            }
            catch (UnverifiableJumpException e) {
                comments.addComment(DecompilerComment.UNVERIFIABLE_BYTECODE_BAD_JUMP);
                this.generateUnverifiable(x, op1list, op2list, lutByIdx, lutByOffset, locFactory);
                try {
                    targetIdxs = op1list.get(x).getAbsoluteIndexJumps(offsetOfThisInstruction, lutByOffset);
                }
                catch (UnverifiableJumpException e2) {
                    throw new ConfusedCFRException("Can't recover from unverifiable jumps at " + offsetOfThisInstruction);
                }
                len = op1list.size();
            }
            Op02WithProcessedDataAndRefs source = (Op02WithProcessedDataAndRefs)op2list.get(x);
            for (int targetIdx : targetIdxs) {
                if (targetIdx >= len) continue;
                Op02WithProcessedDataAndRefs target = op2list.get(targetIdx);
                source.addTarget(target);
                target.addSource(source);
            }
        }
        BlockIdentifierFactory blockIdentifierFactory = new BlockIdentifierFactory();
        List<ExceptionTableEntry> exceptionTableEntries = this.originalCodeAttribute.getExceptionTableEntries();
        if (((Boolean)options.getOption(OptionsImpl.IGNORE_EXCEPTIONS_ALWAYS)).booleanValue()) {
            exceptionTableEntries = ListFactory.newList();
        }
        if ((exceptions = new ExceptionAggregator(exceptionTableEntries, blockIdentifierFactory, lutByOffset, instrs, options, this.cp, comments)).RemovedLoopingExceptions()) {
            comments.addComment(DecompilerComment.LOOPING_EXCEPTIONS);
        }
        if (options.getOption(OptionsImpl.FORCE_PRUNE_EXCEPTIONS) == Troolean.TRUE) {
            exceptions.aggressiveRethrowPruning();
            if (((Boolean)options.getOption(OptionsImpl.ANTI_OBF)).booleanValue()) {
                exceptions.aggressiveImpossiblePruning();
            }
            exceptions.removeSynchronisedHandlers(lutByIdx);
        }
        if (options.getOption(OptionsImpl.REWRITE_LAMBDAS, classFileVersion).booleanValue() && bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.USES_INVOKEDYNAMIC)) {
            Op02GetClassRewriter.removeInvokeGetClass(classFile, op2list, GetClassTestLambda.INSTANCE);
        }
        Op02GetClassRewriter.removeInvokeGetClass(classFile, op2list, GetClassTestInnerConstructor.INSTANCE);
        long codeLength = this.originalCodeAttribute.getCodeLength();
        if (((Boolean)options.getOption(OptionsImpl.CONTROL_FLOW_OBF)).booleanValue()) {
            Op02Obf.removeControlFlowExceptions(this.method, exceptions, op2list, lutByOffset);
            Op02Obf.removeNumericObf(this.method, op2list);
        }
        op2list = Op02WithProcessedDataAndRefs.insertExceptionBlocks(op2list, exceptions, lutByOffset, this.cp, codeLength, options);
        if (aggressiveSizeReductions) {
            Op02RedundantStoreRewriter.rewrite(op2list, this.originalCodeAttribute.getMaxLocals());
        }
        DecompilerComment o2stackComment = Op02WithProcessedDataAndRefs.populateStackInfo(op2list, this.method);
        if (Op02WithProcessedDataAndRefs.processJSR(op2list)) {
            o2stackComment = Op02WithProcessedDataAndRefs.populateStackInfo(op2list, this.method);
        }
        if (o2stackComment != null) {
            comments.addComment(o2stackComment);
        }
        Op02WithProcessedDataAndRefs.unlinkUnreachable(op2list);
        Op02WithProcessedDataAndRefs.discoverStorageLiveness(this.method, comments, op2list, bytecodeMeta);
        VariableFactory variableFactory = new VariableFactory(this.method, bytecodeMeta);
        TypeHintRecovery typeHintRecovery = options.optionIsSet(OptionsImpl.USE_RECOVERED_ITERATOR_TYPE_HINTS) ? new TypeHintRecoveryImpl(bytecodeMeta) : TypeHintRecoveryNone.INSTANCE;
        List<Op03SimpleStatement> op03SimpleParseNodes = Op02WithProcessedDataAndRefs.convertToOp03List(op2list, this.method, variableFactory, blockIdentifierFactory, dcCommonState, comments, typeHintRecovery);
        op03SimpleParseNodes = Cleaner.sortAndRenumber(op03SimpleParseNodes);
        Misc.flattenCompoundStatements(op03SimpleParseNodes);
        Op03Rewriters.rewriteWith(op03SimpleParseNodes, new NullTypedLValueRewriter());
        Op03Rewriters.rewriteWith(op03SimpleParseNodes, new BadBoolAssignmentRewriter());
        GenericInferer.inferGenericObjectInfoFromCalls(op03SimpleParseNodes);
        if (((Boolean)options.getOption(OptionsImpl.RELINK_CONSTANTS)).booleanValue()) {
            Op03Rewriters.relinkInstanceConstants(classFile.getRefClassType(), op03SimpleParseNodes, dcCommonState);
        }
        op03SimpleParseNodes = Cleaner.sortAndRenumber(op03SimpleParseNodes);
        if (aggressiveSizeReductions) {
            op03SimpleParseNodes = LValuePropSimple.condenseSimpleLValues(op03SimpleParseNodes);
        }
        Op03Rewriters.nopIsolatedStackValues(op03SimpleParseNodes);
        Op03SimpleStatement.assignSSAIdentifiers(this.method, op03SimpleParseNodes);
        Op03Rewriters.condenseStaticInstances(op03SimpleParseNodes);
        LValueProp.condenseLValues(op03SimpleParseNodes);
        if (options.getOption(OptionsImpl.REMOVE_DEAD_CONDITIONALS) == Troolean.TRUE) {
            op03SimpleParseNodes = Op03Rewriters.removeDeadConditionals(op03SimpleParseNodes);
        }
        op03SimpleParseNodes = Cleaner.sortAndRenumber(op03SimpleParseNodes);
        op03SimpleParseNodes = KotlinSwitchHandler.extractStringSwitches(op03SimpleParseNodes, bytecodeMeta);
        SwitchReplacer.replaceRawSwitches(this.method, op03SimpleParseNodes, blockIdentifierFactory, options, comments, bytecodeMeta);
        op03SimpleParseNodes = Cleaner.sortAndRenumber(op03SimpleParseNodes);
        Op03Rewriters.removePointlessJumps(op03SimpleParseNodes);
        op03SimpleParseNodes = Op03Rewriters.eliminateCatchTemporaries(op03SimpleParseNodes);
        Op03Rewriters.identifyCatchBlocks(op03SimpleParseNodes, blockIdentifierFactory);
        Op03Rewriters.combineTryCatchBlocks(op03SimpleParseNodes);
        if (((Boolean)options.getOption(OptionsImpl.COMMENT_MONITORS)).booleanValue()) {
            Op03Rewriters.commentMonitors(op03SimpleParseNodes);
        }
        AnonymousClassUsage anonymousClassUsage = new AnonymousClassUsage();
        Op03Rewriters.condenseConstruction(dcCommonState, this.method, op03SimpleParseNodes, anonymousClassUsage);
        op03SimpleParseNodes = Cleaner.sortAndRenumber(op03SimpleParseNodes);
        LValueProp.condenseLValues(op03SimpleParseNodes);
        Op03Rewriters.condenseLValueChain1(op03SimpleParseNodes);
        StaticInitReturnRewriter.rewrite(options, this.method, op03SimpleParseNodes);
        op03SimpleParseNodes = Op03Rewriters.removeRedundantTries(op03SimpleParseNodes);
        FinallyRewriter.identifyFinally(options, this.method, op03SimpleParseNodes, blockIdentifierFactory);
        op03SimpleParseNodes = Cleaner.removeUnreachableCode(op03SimpleParseNodes, !willSort);
        op03SimpleParseNodes = Cleaner.sortAndRenumber(op03SimpleParseNodes);
        Op03Rewriters.extendTryBlocks(dcCommonState, op03SimpleParseNodes);
        Op03Rewriters.combineTryCatchEnds(op03SimpleParseNodes);
        Op03Rewriters.removePointlessExpressionStatements(op03SimpleParseNodes);
        op03SimpleParseNodes = Cleaner.removeUnreachableCode(op03SimpleParseNodes, !willSort);
        Op03Rewriters.replacePrePostChangeAssignments(op03SimpleParseNodes);
        Op03Rewriters.pushPreChangeBack(op03SimpleParseNodes);
        Op03Rewriters.condenseLValueChain2(op03SimpleParseNodes);
        Op03Rewriters.collapseAssignmentsIntoConditionals(op03SimpleParseNodes, options, classFileVersion);
        LValueProp.condenseLValues(op03SimpleParseNodes);
        op03SimpleParseNodes = Cleaner.sortAndRenumber(op03SimpleParseNodes);
        if (options.getOption(OptionsImpl.FORCE_COND_PROPAGATE) == Troolean.TRUE) {
            op03SimpleParseNodes = RemoveDeterministicJumps.apply(this.method, op03SimpleParseNodes);
        }
        if (options.getOption(OptionsImpl.FORCE_TOPSORT) == Troolean.TRUE) {
            if (options.getOption(OptionsImpl.FORCE_RETURNING_IFS) == Troolean.TRUE) {
                Op03Rewriters.replaceReturningIfs(op03SimpleParseNodes, true);
            }
            if (options.getOption(OptionsImpl.FORCE_COND_PROPAGATE) == Troolean.TRUE) {
                Op03Rewriters.propagateToReturn2(op03SimpleParseNodes);
            }
            ExceptionRewriters.handleEmptyTries(op03SimpleParseNodes);
            op03SimpleParseNodes = Cleaner.removeUnreachableCode(op03SimpleParseNodes, false);
            op03SimpleParseNodes = Op03Blocks.topologicalSort(op03SimpleParseNodes, comments, options);
            Op03Rewriters.removePointlessJumps(op03SimpleParseNodes);
            SwitchReplacer.rebuildSwitches(op03SimpleParseNodes, options, comments, bytecodeMeta);
            Op03Rewriters.rejoinBlocks(op03SimpleParseNodes);
            Op03Rewriters.extendTryBlocks(dcCommonState, op03SimpleParseNodes);
            op03SimpleParseNodes = Op03Blocks.combineTryBlocks(op03SimpleParseNodes);
            Op03Rewriters.combineTryCatchEnds(op03SimpleParseNodes);
            Op03Rewriters.rewriteTryBackJumps(op03SimpleParseNodes);
            FinallyRewriter.identifyFinally(options, this.method, op03SimpleParseNodes, blockIdentifierFactory);
            if (options.getOption(OptionsImpl.FORCE_RETURNING_IFS) == Troolean.TRUE) {
                Op03Rewriters.replaceReturningIfs(op03SimpleParseNodes, true);
            }
        }
        if (options.getOption(OptionsImpl.AGGRESSIVE_DUFF) == Troolean.TRUE && bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.SWITCHES)) {
            op03SimpleParseNodes = Cleaner.sortAndRenumber(op03SimpleParseNodes);
            op03SimpleParseNodes = SwitchReplacer.rewriteDuff(op03SimpleParseNodes, variableFactory, comments, options);
        }
        if (options.getOption(OptionsImpl.FORCE_COND_PROPAGATE) == Troolean.TRUE) {
            RemoveDeterministicJumps.propagateToReturn(this.method, op03SimpleParseNodes);
        }
        do {
            Op03Rewriters.rewriteNegativeJumps(op03SimpleParseNodes, true);
            Op03Rewriters.collapseAssignmentsIntoConditionals(op03SimpleParseNodes, options, classFileVersion);
            AnonymousArray.resugarAnonymousArrays(op03SimpleParseNodes);
            reloop = Op03Rewriters.condenseConditionals(op03SimpleParseNodes);
            reloop |= Op03Rewriters.condenseConditionals2(op03SimpleParseNodes);
            if (reloop |= Op03Rewriters.normalizeDupAssigns(op03SimpleParseNodes)) {
                LValueProp.condenseLValues(op03SimpleParseNodes);
            }
            op03SimpleParseNodes = Cleaner.removeUnreachableCode(op03SimpleParseNodes, true);
        } while (reloop);
        AnonymousArray.resugarAnonymousArrays(op03SimpleParseNodes);
        Op03Rewriters.simplifyConditionals(op03SimpleParseNodes, false, this.method);
        op03SimpleParseNodes = Cleaner.sortAndRenumber(op03SimpleParseNodes);
        Op03Rewriters.rewriteNegativeJumps(op03SimpleParseNodes, false);
        Op03Rewriters.optimiseForTypes(op03SimpleParseNodes);
        if (((Boolean)options.getOption(OptionsImpl.ECLIPSE)).booleanValue()) {
            Op03Rewriters.eclipseLoopPass(op03SimpleParseNodes);
        }
        op03SimpleParseNodes = Cleaner.removeUnreachableCode(op03SimpleParseNodes, true);
        LoopIdentifier.identifyLoops1(this.method, op03SimpleParseNodes, blockIdentifierFactory);
        Op03Rewriters.rewriteBadCompares(variableFactory, op03SimpleParseNodes);
        op03SimpleParseNodes = Op03Rewriters.pushThroughGoto(op03SimpleParseNodes);
        if (options.getOption(OptionsImpl.FORCE_RETURNING_IFS) == Troolean.TRUE) {
            Op03Rewriters.replaceReturningIfs(op03SimpleParseNodes, false);
        }
        op03SimpleParseNodes = Cleaner.sortAndRenumber(op03SimpleParseNodes);
        op03SimpleParseNodes = Cleaner.removeUnreachableCode(op03SimpleParseNodes, true);
        Op03Rewriters.rewriteBreakStatements(op03SimpleParseNodes);
        Op03Rewriters.rewriteDoWhileTruePredAsWhile(op03SimpleParseNodes);
        Op03Rewriters.rewriteWhilesAsFors(options, op03SimpleParseNodes);
        Op03Rewriters.removeSynchronizedCatchBlocks(options, op03SimpleParseNodes);
        op03SimpleParseNodes = Op03Rewriters.removeUselessNops(op03SimpleParseNodes);
        Op03Rewriters.removePointlessJumps(op03SimpleParseNodes);
        Op03Rewriters.extractExceptionJumps(op03SimpleParseNodes);
        Op03Rewriters.extractAssertionJumps(op03SimpleParseNodes);
        op03SimpleParseNodes = Cleaner.removeUnreachableCode(op03SimpleParseNodes, true);
        ConditionalRewriter.identifyNonjumpingConditionals(op03SimpleParseNodes, blockIdentifierFactory, options);
        if (options.optionIsSet(OptionsImpl.AGGRESSIVE_DO_COPY)) {
            Op03Rewriters.cloneCodeFromLoop(op03SimpleParseNodes, options, comments);
        }
        if (options.getOption(OptionsImpl.AGGRESSIVE_DO_EXTENSION) == Troolean.TRUE) {
            Op03Rewriters.moveJumpsIntoDo(variableFactory, op03SimpleParseNodes, options, comments);
        }
        LValueProp.condenseLValues(op03SimpleParseNodes);
        if (options.getOption(OptionsImpl.FORCE_COND_PROPAGATE) == Troolean.TRUE) {
            Op03Rewriters.propagateToReturn2(op03SimpleParseNodes);
        }
        op03SimpleParseNodes = Op03Rewriters.removeUselessNops(op03SimpleParseNodes);
        Op03Rewriters.removePointlessJumps(op03SimpleParseNodes);
        Op03Rewriters.rewriteBreakStatements(op03SimpleParseNodes);
        Op03Rewriters.classifyGotos(op03SimpleParseNodes);
        if (((Boolean)options.getOption(OptionsImpl.LABELLED_BLOCKS)).booleanValue()) {
            Op03Rewriters.classifyAnonymousBlockGotos(op03SimpleParseNodes, false);
        }
        ConditionalRewriter.identifyNonjumpingConditionals(op03SimpleParseNodes, blockIdentifierFactory, options);
        InlineDeAssigner.extractAssignments(op03SimpleParseNodes);
        boolean checkLoopTypeClash = false;
        if (options.getOption(OptionsImpl.ARRAY_ITERATOR, classFileVersion).booleanValue()) {
            IterLoopRewriter.rewriteArrayForLoops(op03SimpleParseNodes);
            checkLoopTypeClash = true;
        }
        if (options.getOption(OptionsImpl.COLLECTION_ITERATOR, classFileVersion).booleanValue()) {
            IterLoopRewriter.rewriteIteratorWhileLoops(op03SimpleParseNodes);
            checkLoopTypeClash = true;
        }
        SynchronizedBlocks.findSynchronizedBlocks(op03SimpleParseNodes);
        Op03SimpleStatement.removePointlessSwitchDefaults(op03SimpleParseNodes);
        op03SimpleParseNodes = Op03Rewriters.removeUselessNops(op03SimpleParseNodes);
        Op03Rewriters.rewriteWith(op03SimpleParseNodes, new StringBuilderRewriter(options, classFileVersion));
        Op03Rewriters.rewriteWith(op03SimpleParseNodes, new XorRewriter());
        op03SimpleParseNodes = Cleaner.removeUnreachableCode(op03SimpleParseNodes, true);
        if (((Boolean)options.getOption(OptionsImpl.LABELLED_BLOCKS)).booleanValue()) {
            Op03Rewriters.labelAnonymousBlocks(op03SimpleParseNodes, blockIdentifierFactory);
        }
        Op03Rewriters.simplifyConditionals(op03SimpleParseNodes, true, this.method);
        Op03Rewriters.extractExceptionMiddle(op03SimpleParseNodes);
        Op03Rewriters.removePointlessJumps(op03SimpleParseNodes);
        Op03Rewriters.replaceStackVarsWithLocals(op03SimpleParseNodes);
        Op03Rewriters.narrowAssignmentTypes(this.method, op03SimpleParseNodes);
        if (options.getOption(OptionsImpl.SHOW_INFERRABLE, classFileVersion).booleanValue()) {
            Op03Rewriters.rewriteWith(op03SimpleParseNodes, new ExplicitTypeCallRewriter());
        }
        if (passIdx == 0 && checkLoopTypeClash) {
            if (LoopLivenessClash.detect(op03SimpleParseNodes, bytecodeMeta)) {
                comments.addComment(DecompilerComment.TYPE_CLASHES);
            }
            if (bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.ITERATED_TYPE_HINTS)) {
                comments.addComment(DecompilerComment.ITERATED_TYPE_HINTS);
            }
        }
        if (((Boolean)options.getOption(OptionsImpl.LABELLED_BLOCKS)).booleanValue()) {
            Op03Rewriters.classifyAnonymousBlockGotos(op03SimpleParseNodes, true);
            Op03Rewriters.labelAnonymousBlocks(op03SimpleParseNodes, blockIdentifierFactory);
        }
        Op03Rewriters.rewriteWith(op03SimpleParseNodes, new BadNarrowingArgRewriter());
        Cleaner.reindexInPlace(op03SimpleParseNodes);
        Op03SimpleStatement.noteInterestingLifetimes(op03SimpleParseNodes);
        Op04StructuredStatement block = Op03SimpleStatement.createInitialStructuredBlock(op03SimpleParseNodes);
        Op04StructuredStatement.tidyEmptyCatch(block);
        Op04StructuredStatement.tidyTryCatch(block);
        Op04StructuredStatement.convertUnstructuredIf(block);
        Op04StructuredStatement.inlinePossibles(block);
        Op04StructuredStatement.removeStructuredGotos(block);
        Op04StructuredStatement.removePointlessBlocks(block);
        Op04StructuredStatement.removePointlessReturn(block);
        Op04StructuredStatement.removePointlessControlFlow(block);
        Op04StructuredStatement.removePrimitiveDeconversion(options, this.method, block);
        if (((Boolean)options.getOption(OptionsImpl.LABELLED_BLOCKS)).booleanValue()) {
            Op04StructuredStatement.insertLabelledBlocks(block);
        }
        Op04StructuredStatement.removeUnnecessaryLabelledBreaks(block);
        Op04StructuredStatement.flattenNonReferencedBlocks(block);
        if (!block.isFullyStructured()) {
            comments.addComment(DecompilerComment.UNABLE_TO_STRUCTURE);
        } else {
            Op04StructuredStatement.tidyTypedBooleans(block);
            Op04StructuredStatement.prettifyBadLoops(block);
            new SwitchStringRewriter(options, classFileVersion, bytecodeMeta).rewrite(block);
            new SwitchEnumRewriter(dcCommonState, classFile, blockIdentifierFactory).rewrite(block);
            Op04StructuredStatement.rewriteExplicitTypeUsages(this.method, block, anonymousClassUsage, classFile);
            Op04StructuredStatement.normalizeInstanceOf(block, options, classFileVersion);
            Op04StructuredStatement.discoverVariableScopes(this.method, block, variableFactory, options, classFileVersion, bytecodeMeta);
            if (bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.INSTANCE_OF_MATCHES)) {
                Op04StructuredStatement.tidyInstanceMatches(block);
            }
            if (options.getOption(OptionsImpl.REWRITE_TRY_RESOURCES, classFileVersion).booleanValue()) {
                Op04StructuredStatement.removeEndResource(this.method.getClassFile(), block);
            }
            if (options.getOption(OptionsImpl.SWITCH_EXPRESSION, classFileVersion).booleanValue()) {
                Op04StructuredStatement.switchExpression(this.method, block, comments);
            }
            Op04StructuredStatement.rewriteLambdas(dcCommonState, this.method, block);
            Op04StructuredStatement.discoverLocalClassScopes(this.method, block, variableFactory, options);
            if (((Boolean)options.getOption(OptionsImpl.REMOVE_BOILERPLATE)).booleanValue() && this.method.isConstructor()) {
                Op04StructuredStatement.removeConstructorBoilerplate(block);
            }
            Op04StructuredStatement.removeUnnecessaryVarargArrays(options, this.method, block);
            Op04StructuredStatement.removePrimitiveDeconversion(options, this.method, block);
            Op04StructuredStatement.rewriteBadCastChains(options, this.method, block);
            Op04StructuredStatement.rewriteNarrowingAssignments(options, this.method, block);
            Op04StructuredStatement.tidyVariableNames(this.method, block, bytecodeMeta, comments, this.cp.getClassCache());
            Op04StructuredStatement.tidyObfuscation(options, block);
            Op04StructuredStatement.miscKeyholeTransforms(variableFactory, block);
            Op04StructuredStatement.applyChecker(new LooseCatchChecker(), block, comments);
            Op04StructuredStatement.applyChecker(new VoidVariableChecker(), block, comments);
            Op04StructuredStatement.applyChecker(new IllegalReturnChecker(), block, comments);
            Op04StructuredStatement.flattenNonReferencedBlocks(block);
            Op04StructuredStatement.reduceClashDeclarations(block, bytecodeMeta);
            Op04StructuredStatement.applyTypeAnnotations(this.originalCodeAttribute, block, lutByOffset, comments);
        }
        if (passIdx == 0 && Op04StructuredStatement.checkTypeClashes(block, bytecodeMeta)) {
            comments.addComment(DecompilerComment.TYPE_CLASHES);
        }
        return new AnalysisResultSuccessful(comments, block, anonymousClassUsage);
    }

    private void generateUnverifiable(int x, List<Op01WithProcessedDataAndByteJumps> op1list, List<Op02WithProcessedDataAndRefs> op2list, Map<Integer, Integer> lutByIdx, SortedMap<Integer, Integer> lutByOffset, BytecodeLocFactory locFactory) {
        int[] thisTargets;
        Op01WithProcessedDataAndByteJumps instr = op1list.get(x);
        int thisRaw = instr.getOriginalRawOffset();
        for (int target : thisTargets = instr.getRawTargetOffsets()) {
            if (null != lutByOffset.get(target + thisRaw)) continue;
            this.generateUnverifiableInstr(target + thisRaw, op1list, op2list, lutByIdx, lutByOffset, locFactory);
        }
    }

    private void generateUnverifiableInstr(int offset, List<Op01WithProcessedDataAndByteJumps> op1list, List<Op02WithProcessedDataAndRefs> op2list, Map<Integer, Integer> lutByIdx, SortedMap<Integer, Integer> lutByOffset, BytecodeLocFactory locFactory) {
        int nextOffset;
        ByteData rawData = this.originalCodeAttribute.getRawData();
        int codeLength = this.originalCodeAttribute.getCodeLength();
        do {
            Op01WithProcessedDataAndByteJumps op01 = this.getSingleInstr(rawData, offset);
            int[] targets = op01.getRawTargetOffsets();
            boolean noTargets = false;
            if (targets != null) {
                if (targets.length == 0) {
                    noTargets = true;
                } else {
                    throw new ConfusedCFRException("Can't currently recover from branching unverifiable instructions.");
                }
            }
            int targetIdx = op1list.size();
            op1list.add(op01);
            lutByIdx.put(targetIdx, offset);
            lutByOffset.put(offset, targetIdx);
            Op02WithProcessedDataAndRefs op02 = op01.createOp2(this.cp, targetIdx, locFactory, this.method);
            op2list.add(op02);
            if (noTargets) {
                return;
            }
            nextOffset = offset + op01.getInstructionLength();
            if (!lutByOffset.containsKey(nextOffset)) continue;
            targetIdx = op1list.size();
            int fakeOffset = -op1list.size();
            lutByIdx.put(targetIdx, fakeOffset);
            lutByOffset.put(fakeOffset, targetIdx);
            int[] rawTargets = new int[]{nextOffset - fakeOffset};
            Op01WithProcessedDataAndByteJumps fakeGoto = new Op01WithProcessedDataAndByteJumps(JVMInstr.GOTO, null, rawTargets, fakeOffset);
            op1list.add(fakeGoto);
            op2list.add(fakeGoto.createOp2(this.cp, targetIdx, locFactory, this.method));
            return;
        } while ((offset = nextOffset) < codeLength);
    }

    public void dump(Dumper d) {
        d.newln();
        this.analysed.dump(d);
    }

    public void releaseCode() {
        this.analysed = null;
    }
}

