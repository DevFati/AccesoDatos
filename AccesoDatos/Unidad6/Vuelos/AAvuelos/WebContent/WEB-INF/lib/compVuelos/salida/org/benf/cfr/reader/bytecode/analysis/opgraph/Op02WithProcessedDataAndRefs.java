/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Graph;
import org.benf.cfr.reader.bytecode.analysis.opgraph.GraphConversionHelper;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.TypeHintRecovery;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticMonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArrayIndex;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArrayLength;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ComparisonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.DynamicConstExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.DynamicInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.InstanceOfExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MethodHandlePlaceholder;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewObject;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewObjectArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewPrimitiveArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokationExplicit;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.ArrayVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CatchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CompoundStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ConstructorStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JSRCallStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.JSRRetStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.MonitorEnterStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.MonitorExitStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.RawSwitchStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnNothingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnValueStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ThrowStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.TryStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.QuotingUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.stack.StackDelta;
import org.benf.cfr.reader.bytecode.analysis.stack.StackEntry;
import org.benf.cfr.reader.bytecode.analysis.stack.StackEntryHolder;
import org.benf.cfr.reader.bytecode.analysis.stack.StackSim;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.DynamicInvokeType;
import org.benf.cfr.reader.bytecode.analysis.types.FormalTypeParameter;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaIntersectionTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.analysis.variables.Ident;
import org.benf.cfr.reader.bytecode.analysis.variables.Slot;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamerDefault;
import org.benf.cfr.reader.bytecode.opcode.DecodedLookupSwitch;
import org.benf.cfr.reader.bytecode.opcode.DecodedTableSwitch;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.bootstrap.BootstrapMethodInfo;
import org.benf.cfr.reader.entities.bootstrap.MethodHandleBehaviour;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryDynamicInfo;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryInvokeDynamic;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryLiteral;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodHandle;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodType;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryNameAndType;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolUtils;
import org.benf.cfr.reader.entities.exceptions.ExceptionAggregator;
import org.benf.cfr.reader.entities.exceptions.ExceptionGroup;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.bytestream.BaseByteData;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.collections.StackFactory;
import org.benf.cfr.reader.util.collections.UniqueSeenQueue;
import org.benf.cfr.reader.util.functors.BinaryPredicate;
import org.benf.cfr.reader.util.functors.BinaryProcedure;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.graph.GraphVisitor;
import org.benf.cfr.reader.util.graph.GraphVisitorDFS;
import org.benf.cfr.reader.util.graph.GraphVisitorFIFO;
import org.benf.cfr.reader.util.lambda.LambdaUtils;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.LoggerFactory;
import org.benf.cfr.reader.util.output.ToStringDumper;

public class Op02WithProcessedDataAndRefs
implements Dumpable,
Graph<Op02WithProcessedDataAndRefs> {
    private static final Logger logger = LoggerFactory.create(Op02WithProcessedDataAndRefs.class);
    private InstrIndex index;
    private JVMInstr instr;
    private final int originalRawOffset;
    private final BytecodeLoc loc;
    private final byte[] rawData;
    private List<BlockIdentifier> containedInTheseBlocks = ListFactory.newList();
    private List<ExceptionGroup> exceptionGroups = ListFactory.newList();
    private List<ExceptionGroup.Entry> catchExceptionGroups = ListFactory.newList();
    private final List<Op02WithProcessedDataAndRefs> targets = ListFactory.newList();
    private final List<Op02WithProcessedDataAndRefs> sources = ListFactory.newList();
    private final ConstantPool cp;
    private final ConstantPoolEntry[] cpEntries;
    private long stackDepthBeforeExecution = -1L;
    private long stackDepthAfterExecution;
    private final List<StackEntryHolder> stackConsumed = ListFactory.newList();
    private final List<StackEntryHolder> stackProduced = ListFactory.newList();
    private StackSim unconsumedJoinedStack = null;
    private boolean hasCatchParent = false;
    private SSAIdentifiers<Slot> ssaIdentifiers;
    private Map<Integer, Ident> localVariablesBySlot = MapFactory.newOrderedMap();

    private Op02WithProcessedDataAndRefs(Op02WithProcessedDataAndRefs other) {
        this.instr = other.instr;
        this.rawData = other.rawData;
        this.index = null;
        this.cp = other.cp;
        this.cpEntries = other.cpEntries;
        this.originalRawOffset = other.originalRawOffset;
        this.loc = other.loc;
    }

    public Op02WithProcessedDataAndRefs(JVMInstr instr, byte[] rawData, int index, ConstantPool cp, ConstantPoolEntry[] cpEntries, int originalRawOffset, BytecodeLoc loc) {
        this(instr, rawData, new InstrIndex(index), cp, cpEntries, originalRawOffset, loc);
    }

    public Op02WithProcessedDataAndRefs(JVMInstr instr, byte[] rawData, InstrIndex index, ConstantPool cp, ConstantPoolEntry[] cpEntries, int originalRawOffset, BytecodeLoc loc) {
        this.instr = instr;
        this.rawData = rawData;
        this.index = index;
        this.cp = cp;
        this.cpEntries = cpEntries;
        this.originalRawOffset = originalRawOffset;
        this.loc = loc;
    }

    private void resetStackInfo() {
        this.stackDepthBeforeExecution = -1L;
        this.stackDepthAfterExecution = -1L;
        this.stackConsumed.clear();
        this.stackProduced.clear();
        this.unconsumedJoinedStack = null;
    }

    public InstrIndex getIndex() {
        return this.index;
    }

    public void setIndex(InstrIndex index) {
        this.index = index;
    }

    public void addTarget(Op02WithProcessedDataAndRefs node) {
        this.targets.add(node);
    }

    private void removeTarget(Op02WithProcessedDataAndRefs node) {
        if (!this.targets.remove(node)) {
            throw new ConfusedCFRException("Invalid target, tried to remove " + node + "\nfrom " + this + "\nbut was not a target.");
        }
    }

    public void addSource(Op02WithProcessedDataAndRefs node) {
        this.sources.add(node);
    }

    public JVMInstr getInstr() {
        return this.instr;
    }

    public void replaceTarget(Op02WithProcessedDataAndRefs oldTarget, Op02WithProcessedDataAndRefs newTarget) {
        int index = this.targets.indexOf(oldTarget);
        if (index == -1) {
            throw new ConfusedCFRException("Invalid target");
        }
        this.targets.set(index, newTarget);
    }

    public void replaceSource(Op02WithProcessedDataAndRefs oldSource, Op02WithProcessedDataAndRefs newSource) {
        int index = this.sources.indexOf(oldSource);
        if (index == -1) {
            throw new ConfusedCFRException("Invalid source");
        }
        this.sources.set(index, newSource);
    }

    public void removeSource(Op02WithProcessedDataAndRefs oldSource) {
        if (!this.sources.remove(oldSource)) {
            throw new ConfusedCFRException("Invalid source");
        }
    }

    private int getInstrArgByte(int index) {
        return this.rawData[index];
    }

    private int getInstrArgU1(int index) {
        int res = this.rawData[index];
        if (res < 0) {
            res = 256 + res;
        }
        return res;
    }

    private int getInstrArgShort(int index) {
        BaseByteData tmp = new BaseByteData(this.rawData);
        return tmp.getS2At(index);
    }

    @Override
    public List<Op02WithProcessedDataAndRefs> getTargets() {
        return this.targets;
    }

    @Override
    public List<Op02WithProcessedDataAndRefs> getSources() {
        return this.sources;
    }

    public ConstantPoolEntry[] getCpEntries() {
        return this.cpEntries;
    }

    private void populateStackInfo(StackSim stackSim, Method method, Set<DecompilerComment> comments, LinkedList<Pair<StackSim, Op02WithProcessedDataAndRefs>> next) {
        block8: {
            StackDelta stackDelta;
            block7: {
                stackDelta = this.instr.getStackDelta(this.rawData, this.cpEntries, stackSim, method);
                if (this.stackDepthBeforeExecution == -1L) break block7;
                if (this.instr == JVMInstr.FAKE_CATCH) {
                    return;
                }
                if (stackSim.getDepth() != this.stackDepthBeforeExecution) {
                    throw new ConfusedCFRException("Invalid stack depths @ " + this + " : trying to set " + stackSim.getDepth() + " previously set to " + this.stackDepthBeforeExecution);
                }
                List<StackEntryHolder> alsoConsumed = ListFactory.newList();
                List<StackEntryHolder> alsoProduced = ListFactory.newList();
                StackSim newStackSim = stackSim.getChange(stackDelta, alsoConsumed, alsoProduced, this);
                if (alsoConsumed.size() != this.stackConsumed.size()) {
                    throw new ConfusedCFRException("Unexpected stack sizes on merge");
                }
                for (int i = 0; i < this.stackConsumed.size(); ++i) {
                    this.stackConsumed.get(i).mergeWith(alsoConsumed.get(i), comments);
                }
                if (this.unconsumedJoinedStack == null) break block8;
                long depth = this.unconsumedJoinedStack.getDepth() - (long)alsoProduced.size();
                List<StackEntryHolder> unconsumedEntriesOld = this.unconsumedJoinedStack.getHolders(alsoProduced.size(), depth);
                List<StackEntryHolder> unconsumedEntriesNew = newStackSim.getHolders(alsoProduced.size(), depth);
                for (int i = 0; i < unconsumedEntriesOld.size(); ++i) {
                    unconsumedEntriesOld.get(i).mergeWith(unconsumedEntriesNew.get(i), comments);
                }
                break block8;
            }
            this.stackDepthBeforeExecution = this.instr == JVMInstr.FAKE_CATCH ? 0L : stackSim.getDepth();
            this.stackDepthAfterExecution = this.stackDepthBeforeExecution + stackDelta.getChange();
            StackSim newStackSim = stackSim.getChange(stackDelta, this.stackConsumed, this.stackProduced, this);
            if (this.sources.size() > 1 && newStackSim.getDepth() > (long)this.stackProduced.size()) {
                this.unconsumedJoinedStack = newStackSim;
            }
            for (int i = this.targets.size() - 1; i >= 0; --i) {
                next.addFirst(Pair.make(newStackSim, this.targets.get(i)));
            }
        }
    }

    private ExceptionGroup getSingleExceptionGroup() {
        if (this.exceptionGroups.size() != 1) {
            throw new ConfusedCFRException("Only expecting statement to be tagged with 1 exceptionGroup");
        }
        return this.exceptionGroups.iterator().next();
    }

    @Override
    public Dumper dump(Dumper d) {
        for (BlockIdentifier blockIdentifier : this.containedInTheseBlocks) {
            d.print(" " + blockIdentifier);
        }
        d.print(" " + this.index + " (" + this.originalRawOffset + ") : " + (Object)((Object)this.instr) + "\t Stack:" + this.stackDepthBeforeExecution + "\t");
        d.print("Consumes:[");
        for (StackEntryHolder stackEntryHolder : this.stackConsumed) {
            d.print("" + stackEntryHolder + " ");
        }
        d.print("] Produces:[");
        for (StackEntryHolder stackEntryHolder : this.stackProduced) {
            d.print("" + stackEntryHolder + " ");
        }
        d.print("] sources ");
        for (Op02WithProcessedDataAndRefs source : this.sources) {
            d.print(" " + source.index);
        }
        d.print(" targets ");
        for (Op02WithProcessedDataAndRefs target : this.targets) {
            d.print(" " + target.index);
        }
        d.newln();
        return d;
    }

    private static List<Boolean> getNullsByType(List<Expression> expressions) {
        List<Boolean> res = ListFactory.newList(expressions.size());
        for (Expression e : expressions) {
            res.add(e.getInferredJavaType().getJavaTypeInstance() == RawJavaType.NULL);
        }
        return res;
    }

    private Statement buildInvoke(Method thisCallerMethod) {
        JavaTypeInstance type;
        AbstractMemberFunctionInvokation funcCall;
        FormalTypeParameter fmt;
        MethodPrototype callerProto;
        JavaTypeInstance bestType;
        ClassFile classFile;
        JavaTypeInstance superContainer;
        ConstantPoolEntryMethodRef function = (ConstantPoolEntryMethodRef)this.cpEntries[0];
        StackValue object = this.getStackRValue(this.stackConsumed.size() - 1);
        boolean special = false;
        boolean isSuper = false;
        if (this.instr == JVMInstr.INVOKESPECIAL) {
            special = true;
            JavaTypeInstance objType = object.getInferredJavaType().getJavaTypeInstance();
            JavaTypeInstance callType = function.getClassEntry().getTypeInstance();
            ConstantPoolEntryNameAndType nameAndType = function.getNameAndTypeEntry();
            String funcName = nameAndType.getName().getValue();
            boolean typesMatch = callType.equals(objType);
            if (funcName.equals("<init>")) {
                if (thisCallerMethod.getName().equals("<init>") && !typesMatch && !objType.getRawName().equals("java.lang.Object")) {
                    isSuper = true;
                }
            } else if (!typesMatch) {
                isSuper = true;
            }
        }
        MethodPrototype methodPrototype = function.getMethodPrototype();
        List<Expression> args = this.getNStackRValuesAsExpressions(this.stackConsumed.size() - 1);
        List<Boolean> nulls = Op02WithProcessedDataAndRefs.getNullsByType(args);
        methodPrototype.tightenArgs(object, args);
        boolean superOnInterface = false;
        if (isSuper && (superContainer = function.getClassEntry().getTypeInstance().getDeGenerifiedType()) instanceof JavaRefTypeInstance && (classFile = ((JavaRefTypeInstance)superContainer).getClassFile()) != null && classFile.isInterface()) {
            JavaTypeInstance baseType = thisCallerMethod.getClassFile().getBaseClassType().getDeGenerifiedType();
            boolean bl = superOnInterface = !baseType.equals(superContainer);
        }
        if ((bestType = object.getInferredJavaType().getJavaTypeInstance()) instanceof JavaGenericPlaceholderTypeInstance && (callerProto = thisCallerMethod.getMethodPrototype()).hasFormalTypeParameters() && (fmt = callerProto.getFormalParameterMap().get(bestType.getRawName())) != null) {
            bestType = fmt.getBound();
        }
        AbstractMemberFunctionInvokation abstractMemberFunctionInvokation = funcCall = isSuper ? new SuperFunctionInvokation(this.loc, this.cp, function, (Expression)object, args, nulls, superOnInterface) : new MemberFunctionInvokation(this.loc, this.cp, function, object, bestType, special, args, nulls);
        if (object.getInferredJavaType().getJavaTypeInstance() == RawJavaType.NULL && (type = methodPrototype.getClassType()) != null) {
            object.getInferredJavaType().chain(new InferredJavaType(type, InferredJavaType.Source.FUNCTION));
        }
        if (!isSuper && function.isInitMethod()) {
            return new ConstructorStatement(this.loc, (MemberFunctionInvokation)funcCall);
        }
        if (this.stackProduced.size() == 0) {
            return new ExpressionStatement(funcCall);
        }
        return new AssignmentSimple(this.loc, this.getStackLValue(0), funcCall);
    }

    private Statement buildInvokeDynamic(Method method, DCCommonState dcCommonState, DecompilerComments comments) {
        ConstantPoolEntryInvokeDynamic invokeDynamic = (ConstantPoolEntryInvokeDynamic)this.cpEntries[0];
        ConstantPoolEntryNameAndType nameAndType = invokeDynamic.getNameAndTypeEntry();
        int idx = invokeDynamic.getBootstrapMethodAttrIndex();
        ConstantPoolEntryUTF8 descriptor = nameAndType.getDescriptor();
        ConstantPoolEntryUTF8 name = nameAndType.getName();
        MethodPrototype dynamicPrototype = ConstantPoolUtils.parseJavaMethodPrototype(dcCommonState, null, null, "", false, Method.MethodConstructor.NOT, descriptor, this.cp, false, false, new VariableNamerDefault(), descriptor.getValue());
        return this.buildInvokeDynamic(method.getClassFile(), dcCommonState, name.getValue(), dynamicPrototype, idx, false, comments);
    }

    private Statement buildInvokeDynamic(ClassFile classFile, DCCommonState dcCommonState, String name, MethodPrototype dynamicPrototype, int idx, boolean showBoilerArgs, DecompilerComments comments) {
        boolean hasMarkers;
        AbstractExpression funcCall;
        List<Expression> callargs;
        BootstrapMethodInfo bootstrapMethodInfo = classFile.getBootstrapMethods().getBootStrapMethodInfo(idx);
        ConstantPoolEntryMethodRef methodRef = bootstrapMethodInfo.getConstantPoolEntryMethodRef();
        MethodPrototype prototype = methodRef.getMethodPrototype();
        MethodHandleBehaviour bootstrapBehaviour = bootstrapMethodInfo.getMethodHandleBehaviour();
        String methodName = methodRef.getName();
        DynamicInvokeType dynamicInvokeType = DynamicInvokeType.lookup(methodName);
        List<JavaTypeInstance> markerTypes = ListFactory.newList();
        switch (dynamicInvokeType) {
            case UNKNOWN: 
            case BOOTSTRAP: {
                List<JavaTypeInstance> typeArgs;
                List<Expression> callargs2 = this.buildInvokeBootstrapArgs(prototype, dynamicPrototype, bootstrapBehaviour, bootstrapMethodInfo, methodRef, showBoilerArgs, classFile, dcCommonState, comments);
                List<Expression> dynamicArgs = this.getNStackRValuesAsExpressions(this.stackConsumed.size());
                if (dynamicInvokeType == DynamicInvokeType.UNKNOWN && (typeArgs = dynamicPrototype.getArgs()).size() == dynamicArgs.size()) {
                    dynamicPrototype.tightenArgs(null, dynamicArgs);
                }
                callargs2.add(0, new Literal(TypedLiteral.getString(QuotingUtils.enquoteString(name))));
                callargs2.addAll(dynamicArgs);
                StaticFunctionInvokation funcCall2 = new StaticFunctionInvokation(this.loc, methodRef, callargs2);
                if (this.stackProduced.size() == 0) {
                    return new ExpressionStatement(funcCall2);
                }
                return new AssignmentSimple(this.loc, this.getStackLValue(0), funcCall2);
            }
            case METAFACTORY_1: 
            case METAFACTORY_2: {
                callargs = this.buildInvokeDynamicMetaFactoryArgs(prototype, dynamicPrototype, bootstrapBehaviour, bootstrapMethodInfo, methodRef);
                break;
            }
            case ALTMETAFACTORY_1: 
            case ALTMETAFACTORY_2: {
                callargs = this.buildInvokeDynamicAltMetaFactoryArgs(prototype, dynamicPrototype, bootstrapBehaviour, bootstrapMethodInfo, methodRef, markerTypes);
                break;
            }
            default: {
                throw new IllegalStateException();
            }
        }
        Expression instantiatedType = callargs.get(5);
        JavaTypeInstance callSiteReturnType = dynamicPrototype.getReturnType();
        callSiteReturnType = this.determineDynamicGeneric(callSiteReturnType, dynamicPrototype, instantiatedType, dcCommonState);
        List<Expression> dynamicArgs = this.getNStackRValuesAsExpressions(this.stackConsumed.size());
        dynamicPrototype.tightenArgs(null, dynamicArgs);
        switch (bootstrapBehaviour) {
            case INVOKE_STATIC: {
                funcCall = new StaticFunctionInvokation(this.loc, methodRef, callargs);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Only static invoke dynamic calls supported currently. This is " + (Object)((Object)bootstrapBehaviour));
            }
        }
        JavaTypeInstance lambdaConstructedType = callSiteReturnType;
        boolean bl = hasMarkers = !markerTypes.isEmpty();
        if (hasMarkers && markerTypes.contains(TypeConstants.SERIALIZABLE)) {
            BindingSuperContainer superContainer = callSiteReturnType.getBindingSupers();
            if (superContainer != null && superContainer.containsBase(TypeConstants.SERIALIZABLE)) {
                markerTypes.remove(TypeConstants.SERIALIZABLE);
            }
            boolean bl2 = hasMarkers = !markerTypes.isEmpty();
        }
        if (hasMarkers) {
            markerTypes.add(0, lambdaConstructedType);
            lambdaConstructedType = new JavaIntersectionTypeInstance(markerTypes);
        }
        InferredJavaType castJavaType = new InferredJavaType(lambdaConstructedType, InferredJavaType.Source.OPERATION);
        if (hasMarkers) {
            castJavaType.shallowSetCanBeVar();
        }
        funcCall = new DynamicInvokation(this.loc, castJavaType, funcCall, dynamicArgs);
        if (this.stackProduced.size() == 0) {
            return new ExpressionStatement(funcCall);
        }
        return new AssignmentSimple(this.loc, this.getStackLValue(0), funcCall);
    }

    private JavaTypeInstance determineDynamicGeneric(JavaTypeInstance callsiteReturn, MethodPrototype proto, Expression instantiated, DCCommonState dcCommonState) {
        ClassFile classFile = null;
        try {
            classFile = dcCommonState.getClassFile(proto.getReturnType());
        }
        catch (CannotLoadClassException cannotLoadClassException) {
            // empty catch block
        }
        if (classFile == null) {
            return callsiteReturn;
        }
        List<Method> methods = Functional.filter(classFile.getMethods(), new Predicate<Method>(){

            @Override
            public boolean test(Method in) {
                return !in.hasCodeAttribute();
            }
        });
        if (methods.size() != 1) {
            return callsiteReturn;
        }
        Method method = methods.get(0);
        MethodPrototype genericProto = method.getMethodPrototype();
        MethodPrototype boundProto = LambdaUtils.getLiteralProto(instantiated);
        GenericTypeBinder gtb = genericProto.getTypeBinderForTypes(boundProto.getArgs());
        JavaTypeInstance unboundReturn = genericProto.getReturnType();
        JavaTypeInstance boundReturn = boundProto.getReturnType();
        if (unboundReturn instanceof JavaGenericBaseInstance) {
            GenericTypeBinder gtb2 = GenericTypeBinder.extractBindings((JavaGenericBaseInstance)unboundReturn, boundReturn);
            gtb = gtb.mergeWith(gtb2, true);
        }
        JavaTypeInstance classType = classFile.getClassType();
        BindingSuperContainer b = classFile.getBindingSupers();
        classType = b.getBoundSuperForBase(classType);
        if (classType == null) {
            return callsiteReturn;
        }
        if (!callsiteReturn.getDeGenerifiedType().equals(classType.getDeGenerifiedType())) {
            return callsiteReturn;
        }
        return gtb.getBindingFor(classType);
    }

    private static TypedLiteral getBootstrapArg(ConstantPoolEntry[] bootstrapArguments, int x, ConstantPool cp) {
        ConstantPoolEntry entry = bootstrapArguments[x];
        return TypedLiteral.getConstantPoolEntry(cp, entry);
    }

    private List<Expression> buildInvokeDynamicAltMetaFactoryArgs(MethodPrototype prototype, MethodPrototype dynamicPrototype, MethodHandleBehaviour bootstrapBehaviour, BootstrapMethodInfo bootstrapMethodInfo, ConstantPoolEntryMethodRef methodRef, List<JavaTypeInstance> markerTypes) {
        int FLAG_BRIDGES = 4;
        int FLAG_MARKERS = 2;
        boolean FLAG_SERIALIZABLE = true;
        List<JavaTypeInstance> argTypes = prototype.getArgs();
        ConstantPoolEntry[] bootstrapArguments = bootstrapMethodInfo.getBootstrapArguments();
        if (bootstrapArguments.length < 4) {
            throw new IllegalStateException("Dynamic invoke arg count mismatch ");
        }
        List<Expression> callargs = ListFactory.newList();
        Literal nullExp = new Literal(TypedLiteral.getNull());
        callargs.add(nullExp);
        callargs.add(nullExp);
        callargs.add(nullExp);
        TypedLiteral tlMethodType = Op02WithProcessedDataAndRefs.getBootstrapArg(bootstrapArguments, 0, this.cp);
        TypedLiteral tlImplMethod = Op02WithProcessedDataAndRefs.getBootstrapArg(bootstrapArguments, 1, this.cp);
        TypedLiteral tlInstantiatedMethodType = Op02WithProcessedDataAndRefs.getBootstrapArg(bootstrapArguments, 2, this.cp);
        int iFlags = Op02WithProcessedDataAndRefs.getBootstrapArg(bootstrapArguments, 3, this.cp).getIntValue();
        int nextArgIdx = 4;
        if ((iFlags & 1) != 0) {
            markerTypes.add(TypeConstants.SERIALIZABLE);
        }
        if ((iFlags & 2) != 0) {
            int nMarkers = Op02WithProcessedDataAndRefs.getBootstrapArg(bootstrapArguments, nextArgIdx++, this.cp).getIntValue();
            for (int x = 0; x < nMarkers; ++x) {
                TypedLiteral marker;
                if ((marker = Op02WithProcessedDataAndRefs.getBootstrapArg(bootstrapArguments, nextArgIdx++, this.cp)).getType() != TypedLiteral.LiteralType.Class) continue;
                JavaTypeInstance classType = marker.getClassValue();
                markerTypes.add(classType);
            }
        }
        callargs.add(new Literal(tlMethodType));
        callargs.add(new Literal(tlImplMethod));
        callargs.add(new Literal(tlInstantiatedMethodType));
        return callargs;
    }

    private List<Expression> buildInvokeBootstrapArgs(MethodPrototype prototype, MethodPrototype dynamicPrototype, MethodHandleBehaviour bootstrapBehaviour, BootstrapMethodInfo bootstrapMethodInfo, ConstantPoolEntryMethodRef methodRef, boolean showBoilerArgs, ClassFile classFile, DCCommonState state, DecompilerComments comments) {
        boolean countMismatch;
        int ARG_OFFSET = 3;
        List<JavaTypeInstance> argTypes = prototype.getArgs();
        ConstantPoolEntry[] bootstrapArguments = bootstrapMethodInfo.getBootstrapArguments();
        boolean bl = countMismatch = bootstrapArguments.length + 3 != argTypes.size();
        if (!argTypes.isEmpty()) {
            boolean maybeVarArgs;
            JavaTypeInstance last = argTypes.get(argTypes.size() - 1);
            boolean bl2 = maybeVarArgs = last.getNumArrayDimensions() == 1;
            if (maybeVarArgs) {
                if (countMismatch) {
                    return this.getVarArgs(last, bootstrapArguments);
                }
                TypedLiteral val = Op02WithProcessedDataAndRefs.getBootstrapArg(bootstrapArguments, bootstrapArguments.length - 1, this.cp);
                if (val.getInferredJavaType().getJavaTypeInstance().getNumArrayDimensions() != last.getNumArrayDimensions()) {
                    return this.getVarArgs(last, bootstrapArguments);
                }
            }
        }
        if (countMismatch) {
            comments.addComment(DecompilerComment.DYNAMIC_SIGNATURE_MISMATCH);
        }
        List<Expression> callargs = ListFactory.newList();
        if (showBoilerArgs) {
            Pair<JavaRefTypeInstance, JavaRefTypeInstance> methodHandlesLookup = state.getClassCache().getRefClassForInnerOuterPair("java.lang.invoke.MethodHandles$Lookup", "java.lang.invoke.MethodHandles");
            callargs.add(new StaticFunctionInvokationExplicit(this.loc, new InferredJavaType(methodHandlesLookup.getFirst(), InferredJavaType.Source.LITERAL), methodHandlesLookup.getSecond(), "lookup", Collections.<Expression>emptyList()));
            callargs.add(new Literal(TypedLiteral.getString(QuotingUtils.enquoteString(methodRef.getName()))));
            callargs.add(new LValueExpression(this.loc, new StaticVariable(new InferredJavaType(TypeConstants.CLASS, InferredJavaType.Source.LITERAL), classFile.getClassType(), "class")));
        }
        for (int x = 0; x < bootstrapArguments.length; ++x) {
            JavaTypeInstance expected = argTypes.get(3 + x);
            TypedLiteral typedLiteral = Op02WithProcessedDataAndRefs.getBootstrapArg(bootstrapArguments, x, this.cp);
            AbstractExpression literal = new Literal(typedLiteral);
            if (!expected.equals(typedLiteral.getInferredJavaType().getJavaTypeInstance())) {
                literal = new CastExpression(this.loc, new InferredJavaType(expected, InferredJavaType.Source.BOOTSTRAP), literal);
            }
            callargs.add(literal);
        }
        return callargs;
    }

    private List<Expression> getVarArgs(JavaTypeInstance last, ConstantPoolEntry[] bootstrapArguments) {
        List<Expression> content = ListFactory.newList();
        for (int i = 0; i < bootstrapArguments.length; ++i) {
            TypedLiteral typedLiteral = Op02WithProcessedDataAndRefs.getBootstrapArg(bootstrapArguments, i, this.cp);
            content.add(new Literal(typedLiteral));
        }
        InferredJavaType arrayType = new InferredJavaType(last.getArrayStrippedType(), InferredJavaType.Source.UNKNOWN);
        NewAnonymousArray res = new NewAnonymousArray(this.loc, arrayType, 1, content, false);
        List<Expression> callargs = ListFactory.newList();
        callargs.add(res);
        return callargs;
    }

    private List<Expression> buildInvokeDynamicMetaFactoryArgs(MethodPrototype prototype, MethodPrototype dynamicPrototype, MethodHandleBehaviour bootstrapBehaviour, BootstrapMethodInfo bootstrapMethodInfo, ConstantPoolEntryMethodRef methodRef) {
        int ARG_OFFSET = 3;
        List<JavaTypeInstance> argTypes = prototype.getArgs();
        ConstantPoolEntry[] bootstrapArguments = bootstrapMethodInfo.getBootstrapArguments();
        if (bootstrapArguments.length + 3 != argTypes.size()) {
            throw new IllegalStateException("Dynamic invoke arg count mismatch " + bootstrapArguments.length + "(+3) vs " + argTypes.size());
        }
        List<Expression> callargs = ListFactory.newList();
        Literal nullExp = new Literal(TypedLiteral.getNull());
        callargs.add(nullExp);
        callargs.add(nullExp);
        callargs.add(nullExp);
        for (int x = 0; x < bootstrapArguments.length; ++x) {
            TypedLiteral typedLiteral;
            JavaTypeInstance expected = argTypes.get(3 + x);
            if (!expected.equals((typedLiteral = Op02WithProcessedDataAndRefs.getBootstrapArg(bootstrapArguments, x, this.cp)).getInferredJavaType().getJavaTypeInstance())) {
                throw new IllegalStateException("Dynamic invoke Expected " + expected + ", got " + typedLiteral);
            }
            callargs.add(new Literal(typedLiteral));
        }
        return callargs;
    }

    public Pair<JavaTypeInstance, Integer> getRetrieveType() {
        RawJavaType type;
        switch (this.instr) {
            case ALOAD: 
            case ALOAD_0: 
            case ALOAD_1: 
            case ALOAD_2: 
            case ALOAD_3: 
            case ALOAD_WIDE: {
                type = RawJavaType.REF;
                break;
            }
            case ILOAD: 
            case ILOAD_0: 
            case ILOAD_1: 
            case ILOAD_2: 
            case ILOAD_3: 
            case ILOAD_WIDE: 
            case IINC: 
            case IINC_WIDE: {
                type = RawJavaType.INT;
                break;
            }
            case LLOAD: 
            case LLOAD_0: 
            case LLOAD_1: 
            case LLOAD_2: 
            case LLOAD_3: 
            case LLOAD_WIDE: {
                type = RawJavaType.LONG;
                break;
            }
            case DLOAD: 
            case DLOAD_0: 
            case DLOAD_1: 
            case DLOAD_2: 
            case DLOAD_3: 
            case DLOAD_WIDE: {
                type = RawJavaType.DOUBLE;
                break;
            }
            case FLOAD: 
            case FLOAD_0: 
            case FLOAD_1: 
            case FLOAD_2: 
            case FLOAD_3: 
            case FLOAD_WIDE: {
                type = RawJavaType.FLOAT;
                break;
            }
            case RET: 
            case RET_WIDE: {
                type = RawJavaType.RETURNADDRESS;
                break;
            }
            default: {
                return null;
            }
        }
        Integer idx = this.getRetrieveIdx();
        if (idx == null) {
            return null;
        }
        return Pair.make(type, idx);
    }

    public Integer getRetrieveIdx() {
        switch (this.instr) {
            case ALOAD: 
            case ILOAD: 
            case IINC: 
            case LLOAD: 
            case DLOAD: 
            case FLOAD: {
                return this.getInstrArgU1(0);
            }
            case ALOAD_0: 
            case ILOAD_0: 
            case LLOAD_0: 
            case DLOAD_0: 
            case FLOAD_0: {
                return 0;
            }
            case ALOAD_1: 
            case ILOAD_1: 
            case LLOAD_1: 
            case DLOAD_1: 
            case FLOAD_1: {
                return 1;
            }
            case ALOAD_2: 
            case ILOAD_2: 
            case LLOAD_2: 
            case DLOAD_2: 
            case FLOAD_2: {
                return 2;
            }
            case ALOAD_3: 
            case ILOAD_3: 
            case LLOAD_3: 
            case DLOAD_3: 
            case FLOAD_3: {
                return 3;
            }
            case ALOAD_WIDE: 
            case ILOAD_WIDE: 
            case LLOAD_WIDE: 
            case DLOAD_WIDE: 
            case FLOAD_WIDE: {
                return this.getInstrArgShort(1);
            }
            case RET: {
                return this.getInstrArgByte(0);
            }
            case RET_WIDE: {
                return this.getInstrArgShort(1);
            }
        }
        return null;
    }

    public Pair<JavaTypeInstance, Integer> getStorageType() {
        RawJavaType type;
        switch (this.instr) {
            case ASTORE: 
            case ASTORE_0: 
            case ASTORE_1: 
            case ASTORE_2: 
            case ASTORE_3: 
            case ASTORE_WIDE: {
                type = RawJavaType.REF;
                break;
            }
            case IINC: 
            case IINC_WIDE: 
            case ISTORE: 
            case ISTORE_0: 
            case ISTORE_1: 
            case ISTORE_2: 
            case ISTORE_3: 
            case ISTORE_WIDE: {
                type = RawJavaType.INT;
                break;
            }
            case LSTORE: 
            case LSTORE_0: 
            case LSTORE_1: 
            case LSTORE_2: 
            case LSTORE_3: 
            case LSTORE_WIDE: {
                type = RawJavaType.LONG;
                break;
            }
            case DSTORE: 
            case DSTORE_0: 
            case DSTORE_1: 
            case DSTORE_2: 
            case DSTORE_3: 
            case DSTORE_WIDE: {
                type = RawJavaType.DOUBLE;
                break;
            }
            case FSTORE: 
            case FSTORE_0: 
            case FSTORE_1: 
            case FSTORE_2: 
            case FSTORE_3: 
            case FSTORE_WIDE: {
                type = RawJavaType.FLOAT;
                break;
            }
            default: {
                return null;
            }
        }
        Integer idx = this.getStoreIdx();
        if (idx == null) {
            return null;
        }
        return Pair.make(type, idx);
    }

    public Integer getStoreIdx() {
        switch (this.instr) {
            case IINC: 
            case ASTORE: 
            case ISTORE: 
            case LSTORE: 
            case DSTORE: 
            case FSTORE: {
                return this.getInstrArgU1(0);
            }
            case ASTORE_0: 
            case ISTORE_0: 
            case LSTORE_0: 
            case DSTORE_0: 
            case FSTORE_0: {
                return 0;
            }
            case ASTORE_1: 
            case ISTORE_1: 
            case LSTORE_1: 
            case DSTORE_1: 
            case FSTORE_1: {
                return 1;
            }
            case ASTORE_2: 
            case ISTORE_2: 
            case LSTORE_2: 
            case DSTORE_2: 
            case FSTORE_2: {
                return 2;
            }
            case ASTORE_3: 
            case ISTORE_3: 
            case LSTORE_3: 
            case DSTORE_3: 
            case FSTORE_3: {
                return 3;
            }
            case IINC_WIDE: 
            case ASTORE_WIDE: 
            case ISTORE_WIDE: 
            case LSTORE_WIDE: 
            case DSTORE_WIDE: 
            case FSTORE_WIDE: {
                return this.getInstrArgShort(1);
            }
        }
        return null;
    }

    private Statement mkAssign(VariableFactory variableFactory) {
        Pair<JavaTypeInstance, Integer> storageTypeAndIdx = this.getStorageType();
        int slot = storageTypeAndIdx.getSecond();
        Ident ident = this.localVariablesBySlot.get(slot);
        return new AssignmentSimple(this.loc, variableFactory.localVariable(slot, ident, this.originalRawOffset), this.getStackRValue(0));
    }

    private Statement mkRetrieve(VariableFactory variableFactory) {
        Pair<JavaTypeInstance, Integer> storageTypeAndIdx = this.getRetrieveType();
        int slot = storageTypeAndIdx.getSecond();
        Ident ident = this.localVariablesBySlot.get(slot);
        LValue lValue = variableFactory.localVariable(slot, ident, this.originalRawOffset);
        return new AssignmentSimple(this.loc, this.getStackLValue(0), new LValueExpression(this.loc, lValue));
    }

    private static Expression ensureNonBool(Expression e) {
        InferredJavaType inferredJavaType = e.getInferredJavaType();
        if (inferredJavaType.getRawType() == RawJavaType.BOOLEAN) {
            if (inferredJavaType.getSource() == InferredJavaType.Source.LITERAL) {
                e.getInferredJavaType().useInArithOp(new InferredJavaType(RawJavaType.INT, InferredJavaType.Source.LITERAL), RawJavaType.INT, true);
            } else {
                e = new TernaryExpression(BytecodeLoc.NONE, new BooleanExpression(e), Literal.INT_ONE, Literal.INT_ZERO);
            }
        }
        return e;
    }

    private Statement createStatement(Method method, DecompilerComments comments, VariableFactory variableFactory, BlockIdentifierFactory blockIdentifierFactory, DCCommonState dcCommonState, TypeHintRecovery typeHintRecovery) {
        switch (this.instr) {
            case ALOAD: 
            case ALOAD_0: 
            case ALOAD_1: 
            case ALOAD_2: 
            case ALOAD_3: 
            case ALOAD_WIDE: 
            case ILOAD: 
            case ILOAD_0: 
            case ILOAD_1: 
            case ILOAD_2: 
            case ILOAD_3: 
            case ILOAD_WIDE: 
            case LLOAD: 
            case LLOAD_0: 
            case LLOAD_1: 
            case LLOAD_2: 
            case LLOAD_3: 
            case LLOAD_WIDE: 
            case DLOAD: 
            case DLOAD_0: 
            case DLOAD_1: 
            case DLOAD_2: 
            case DLOAD_3: 
            case DLOAD_WIDE: 
            case FLOAD: 
            case FLOAD_0: 
            case FLOAD_1: 
            case FLOAD_2: 
            case FLOAD_3: 
            case FLOAD_WIDE: {
                return this.mkRetrieve(variableFactory);
            }
            case ACONST_NULL: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getNull()));
            }
            case ICONST_M1: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getInt(-1)));
            }
            case ICONST_0: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getBoolean(0)));
            }
            case ICONST_1: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getBoolean(1)));
            }
            case ICONST_2: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getInt(2)));
            }
            case ICONST_3: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getInt(3)));
            }
            case ICONST_4: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getInt(4)));
            }
            case ICONST_5: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getInt(5)));
            }
            case LCONST_0: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getLong(0L)));
            }
            case LCONST_1: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getLong(1L)));
            }
            case FCONST_0: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getFloat(0.0f)));
            }
            case DCONST_0: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getDouble(0.0)));
            }
            case FCONST_1: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getFloat(1.0f)));
            }
            case DCONST_1: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getDouble(1.0)));
            }
            case FCONST_2: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getFloat(2.0f)));
            }
            case BIPUSH: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getInt(this.rawData[0])));
            }
            case SIPUSH: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getInt(this.getInstrArgShort(0))));
            }
            case ASTORE: 
            case ASTORE_0: 
            case ASTORE_1: 
            case ASTORE_2: 
            case ASTORE_3: 
            case ASTORE_WIDE: 
            case ISTORE: 
            case ISTORE_0: 
            case ISTORE_1: 
            case ISTORE_2: 
            case ISTORE_3: 
            case ISTORE_WIDE: 
            case LSTORE: 
            case LSTORE_0: 
            case LSTORE_1: 
            case LSTORE_2: 
            case LSTORE_3: 
            case LSTORE_WIDE: 
            case DSTORE: 
            case DSTORE_0: 
            case DSTORE_1: 
            case DSTORE_2: 
            case DSTORE_3: 
            case DSTORE_WIDE: 
            case FSTORE: 
            case FSTORE_0: 
            case FSTORE_1: 
            case FSTORE_2: 
            case FSTORE_3: 
            case FSTORE_WIDE: {
                return this.mkAssign(variableFactory);
            }
            case NEW: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new NewObject(this.loc, this.cpEntries[0]));
            }
            case NEWARRAY: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new NewPrimitiveArray(this.loc, (Expression)this.getStackRValue(0), this.rawData[0]));
            }
            case ANEWARRAY: {
                List<Expression> tmp = ListFactory.newList();
                tmp.add(this.getStackRValue(0));
                ConstantPoolEntryClass clazz = (ConstantPoolEntryClass)this.cpEntries[0];
                JavaTypeInstance innerInstance = clazz.getTypeInstance();
                JavaArrayTypeInstance resultInstance = new JavaArrayTypeInstance(1, innerInstance);
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new NewObjectArray(this.loc, tmp, resultInstance));
            }
            case MULTIANEWARRAY: {
                byte numDims = this.rawData[2];
                ConstantPoolEntryClass clazz = (ConstantPoolEntryClass)this.cpEntries[0];
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new NewObjectArray(this.loc, this.getNStackRValuesAsExpressions(numDims), clazz.getTypeInstance()));
            }
            case ARRAYLENGTH: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new ArrayLength(this.loc, this.getStackRValue(0)));
            }
            case AALOAD: 
            case IALOAD: 
            case BALOAD: 
            case CALOAD: 
            case FALOAD: 
            case LALOAD: 
            case DALOAD: 
            case SALOAD: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new ArrayIndex(this.loc, this.getStackRValue(1), this.getStackRValue(0)));
            }
            case AASTORE: 
            case IASTORE: 
            case BASTORE: 
            case CASTORE: 
            case FASTORE: 
            case LASTORE: 
            case DASTORE: 
            case SASTORE: {
                return new AssignmentSimple(this.loc, new ArrayVariable(new ArrayIndex(this.loc, this.getStackRValue(2), this.getStackRValue(1))), this.getStackRValue(0));
            }
            case LCMP: 
            case DCMPG: 
            case DCMPL: 
            case FCMPG: 
            case FCMPL: 
            case LSUB: 
            case LADD: 
            case IADD: 
            case FADD: 
            case DADD: 
            case ISUB: 
            case DSUB: 
            case FSUB: 
            case IREM: 
            case FREM: 
            case LREM: 
            case DREM: 
            case IDIV: 
            case FDIV: 
            case DDIV: 
            case IMUL: 
            case DMUL: 
            case FMUL: 
            case LMUL: 
            case LAND: 
            case LDIV: 
            case LOR: 
            case LXOR: 
            case ISHR: 
            case ISHL: 
            case LSHL: 
            case LSHR: 
            case IUSHR: 
            case LUSHR: {
                StackValue lhs = this.getStackRValue(1);
                StackValue rhs = this.getStackRValue(0);
                ArithmeticOperation op = new ArithmeticOperation(this.loc, lhs, rhs, ArithOp.getOpFor(this.instr));
                return new AssignmentSimple(this.loc, this.getStackLValue(0), op);
            }
            case IOR: 
            case IAND: 
            case IXOR: {
                StackValue lhs = this.getStackRValue(1);
                StackValue rhs = this.getStackRValue(0);
                if (lhs.getInferredJavaType().getJavaTypeInstance() == RawJavaType.BOOLEAN && rhs.getInferredJavaType().getJavaTypeInstance() == RawJavaType.BOOLEAN) {
                    ArithmeticOperation op = new ArithmeticOperation(this.loc, lhs, rhs, ArithOp.getOpFor(this.instr));
                    return new AssignmentSimple(this.loc, this.getStackLValue(0), op);
                }
                ArithOp arithop = ArithOp.getOpFor(this.instr);
                InferredJavaType.useInArithOp(lhs.getInferredJavaType(), rhs.getInferredJavaType(), arithop);
                ArithmeticOperation op = new ArithmeticOperation(this.loc, new InferredJavaType(RawJavaType.INT, InferredJavaType.Source.EXPRESSION, true), lhs, rhs, arithop);
                return new AssignmentSimple(this.loc, this.getStackLValue(0), op);
            }
            case I2B: 
            case I2C: 
            case I2D: 
            case I2F: 
            case I2L: 
            case I2S: 
            case L2D: 
            case L2F: 
            case L2I: 
            case F2D: 
            case F2I: 
            case F2L: 
            case D2F: 
            case D2I: 
            case D2L: {
                LValue lValue = this.getStackLValue(0);
                lValue.getInferredJavaType().useAsWithCast(this.instr.getRawJavaType());
                StackValue rValue = this.getStackRValue(0);
                return new AssignmentSimple(this.loc, lValue, new CastExpression(this.loc, new InferredJavaType(this.instr.getRawJavaType(), InferredJavaType.Source.INSTRUCTION), rValue));
            }
            case INSTANCEOF: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new InstanceOfExpression(this.loc, this.getStackRValue(0), this.cpEntries[0]));
            }
            case CHECKCAST: {
                ConstantPoolEntryClass castTarget = (ConstantPoolEntryClass)this.cpEntries[0];
                JavaTypeInstance tgtJavaType = castTarget.getTypeInstance();
                InferredJavaType srcInferredJavaType = this.getStackRValue(0).getInferredJavaType();
                JavaTypeInstance srcJavaType = srcInferredJavaType.getJavaTypeInstance();
                AbstractExpression rhs = this.getStackRValue(0);
                if (!tgtJavaType.equals(srcJavaType.getDeGenerifiedType())) {
                    JavaTypeInstance implementationOf = srcJavaType.directImplOf(tgtJavaType);
                    if (implementationOf != null) {
                        tgtJavaType = implementationOf;
                    }
                    InferredJavaType castType = new InferredJavaType(tgtJavaType, InferredJavaType.Source.EXPRESSION, true);
                    rhs = new CastExpression(this.loc, castType, this.getStackRValue(0));
                }
                return new AssignmentSimple(this.loc, this.getStackLValue(0), rhs);
            }
            case INVOKESTATIC: {
                ConstantPoolEntryMethodRef function = (ConstantPoolEntryMethodRef)this.cpEntries[0];
                MethodPrototype methodPrototype = function.getMethodPrototype();
                List<Expression> args = this.getNStackRValuesAsExpressions(this.stackConsumed.size());
                methodPrototype.tightenArgs(null, args);
                StaticFunctionInvokation funcCall = new StaticFunctionInvokation(this.loc, function, args);
                if (this.stackProduced.size() == 0) {
                    return new ExpressionStatement(funcCall);
                }
                InferredJavaType type = funcCall.getInferredJavaType();
                type.setTaggedBytecodeLocation(this.originalRawOffset);
                typeHintRecovery.improve(type);
                return new AssignmentSimple(this.loc, this.getStackLValue(0), funcCall);
            }
            case INVOKEDYNAMIC: {
                return this.buildInvokeDynamic(method, dcCommonState, comments);
            }
            case INVOKESPECIAL: 
            case INVOKEVIRTUAL: 
            case INVOKEINTERFACE: {
                return this.buildInvoke(method);
            }
            case RETURN: {
                return new ReturnNothingStatement(this.loc);
            }
            case IF_ACMPEQ: 
            case IF_ACMPNE: 
            case IF_ICMPLT: 
            case IF_ICMPGE: 
            case IF_ICMPGT: 
            case IF_ICMPNE: 
            case IF_ICMPEQ: 
            case IF_ICMPLE: {
                ComparisonOperation conditionalExpression = new ComparisonOperation(this.loc, this.getStackRValue(1), this.getStackRValue(0), CompOp.getOpFor(this.instr));
                return new IfStatement(this.loc, conditionalExpression);
            }
            case IFNONNULL: {
                ComparisonOperation conditionalExpression = new ComparisonOperation(this.loc, this.getStackRValue(0), new Literal(TypedLiteral.getNull()), CompOp.NE);
                return new IfStatement(this.loc, conditionalExpression);
            }
            case IFNULL: {
                ComparisonOperation conditionalExpression = new ComparisonOperation(this.loc, this.getStackRValue(0), new Literal(TypedLiteral.getNull()), CompOp.EQ);
                return new IfStatement(this.loc, conditionalExpression);
            }
            case IFEQ: 
            case IFNE: {
                ComparisonOperation conditionalExpression = new ComparisonOperation(this.loc, this.getStackRValue(0), new Literal(TypedLiteral.getBoolean(0)), CompOp.getOpFor(this.instr));
                return new IfStatement(this.loc, conditionalExpression);
            }
            case IFLE: 
            case IFLT: 
            case IFGT: 
            case IFGE: {
                ComparisonOperation conditionalExpression = new ComparisonOperation(this.loc, this.getStackRValue(0), new Literal(TypedLiteral.getInt(0)), CompOp.getOpFor(this.instr));
                return new IfStatement(this.loc, conditionalExpression);
            }
            case JSR_W: 
            case JSR: {
                return new CompoundStatement(this.loc, new AssignmentSimple(this.loc, this.getStackLValue(0), new Literal(TypedLiteral.getInt(this.originalRawOffset))), new JSRCallStatement(this.loc));
            }
            case RET: {
                int slot = this.getInstrArgU1(0);
                LValueExpression retVal = new LValueExpression(this.loc, variableFactory.localVariable(slot, this.localVariablesBySlot.get(slot), this.originalRawOffset));
                return new JSRRetStatement(this.loc, retVal);
            }
            case GOTO: 
            case GOTO_W: {
                return new GotoStatement(this.loc);
            }
            case ATHROW: {
                return new ThrowStatement(this.loc, this.getStackRValue(0));
            }
            case IRETURN: 
            case ARETURN: 
            case LRETURN: 
            case DRETURN: 
            case FRETURN: {
                StackValue retVal = this.getStackRValue(0);
                JavaTypeInstance tgtType = variableFactory.getReturn();
                retVal.getInferredJavaType().useAsWithoutCasting(tgtType);
                return new ReturnValueStatement(this.loc, retVal, tgtType);
            }
            case GETFIELD: {
                LValueExpression fieldExpression = new LValueExpression(this.loc, new FieldVariable(this.getStackRValue(0), this.cpEntries[0]));
                return new AssignmentSimple(this.loc, this.getStackLValue(0), fieldExpression);
            }
            case GETSTATIC: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new LValueExpression(this.loc, new StaticVariable(this.cpEntries[0])));
            }
            case PUTSTATIC: {
                return new AssignmentSimple(this.loc, new StaticVariable(this.cpEntries[0]), this.getStackRValue(0));
            }
            case PUTFIELD: {
                return new AssignmentSimple(this.loc, new FieldVariable(this.getStackRValue(1), this.cpEntries[0]), this.getStackRValue(0));
            }
            case SWAP: {
                AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(1));
                AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(0));
                return new CompoundStatement(this.loc, s1, s2);
            }
            case DUP: {
                AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(0));
                return new CompoundStatement(this.loc, s1, s2);
            }
            case DUP_X1: {
                AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(1));
                AssignmentSimple s3 = new AssignmentSimple(this.loc, this.getStackLValue(2), this.getStackRValue(0));
                return new CompoundStatement(this.loc, s1, s2, s3);
            }
            case DUP_X2: {
                if (this.stackConsumed.get(1).getStackEntry().getType().getComputationCategory() == 2) {
                    AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                    AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(1));
                    AssignmentSimple s3 = new AssignmentSimple(this.loc, this.getStackLValue(2), this.getStackRValue(0));
                    return new CompoundStatement(this.loc, s1, s2, s3);
                }
                AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(1));
                AssignmentSimple s3 = new AssignmentSimple(this.loc, this.getStackLValue(2), this.getStackRValue(2));
                AssignmentSimple s4 = new AssignmentSimple(this.loc, this.getStackLValue(3), this.getStackRValue(0));
                return new CompoundStatement(this.loc, s1, s2, s3, s4);
            }
            case DUP2: {
                if (this.stackConsumed.get(0).getStackEntry().getType().getComputationCategory() == 2) {
                    AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                    AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(0));
                    return new CompoundStatement(this.loc, s1, s2);
                }
                AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(1));
                AssignmentSimple s3 = new AssignmentSimple(this.loc, this.getStackLValue(2), this.getStackRValue(0));
                AssignmentSimple s4 = new AssignmentSimple(this.loc, this.getStackLValue(3), this.getStackRValue(1));
                return new CompoundStatement(this.loc, s1, s2, s3, s4);
            }
            case DUP2_X1: {
                if (this.stackConsumed.get(0).getStackEntry().getType().getComputationCategory() == 2) {
                    AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                    AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(1));
                    AssignmentSimple s3 = new AssignmentSimple(this.loc, this.getStackLValue(2), this.getStackRValue(0));
                    return new CompoundStatement(this.loc, s1, s2, s3);
                }
                AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(1));
                AssignmentSimple s3 = new AssignmentSimple(this.loc, this.getStackLValue(2), this.getStackRValue(2));
                AssignmentSimple s4 = new AssignmentSimple(this.loc, this.getStackLValue(3), this.getStackRValue(0));
                AssignmentSimple s5 = new AssignmentSimple(this.loc, this.getStackLValue(4), this.getStackRValue(1));
                return new CompoundStatement(this.loc, s1, s2, s3, s4, s5);
            }
            case DUP2_X2: {
                if (this.stackConsumed.get(0).getStackEntry().getType().getComputationCategory() == 2) {
                    if (this.stackConsumed.get(1).getStackEntry().getType().getComputationCategory() == 2) {
                        AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                        AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(1));
                        AssignmentSimple s3 = new AssignmentSimple(this.loc, this.getStackLValue(2), this.getStackRValue(0));
                        return new CompoundStatement(this.loc, s1, s2, s3);
                    }
                    AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                    AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(1));
                    AssignmentSimple s3 = new AssignmentSimple(this.loc, this.getStackLValue(2), this.getStackRValue(2));
                    AssignmentSimple s4 = new AssignmentSimple(this.loc, this.getStackLValue(3), this.getStackRValue(0));
                    return new CompoundStatement(this.loc, s1, s2, s3, s4);
                }
                if (this.stackConsumed.get(2).getStackEntry().getType().getComputationCategory() == 2) {
                    AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                    AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(1));
                    AssignmentSimple s3 = new AssignmentSimple(this.loc, this.getStackLValue(2), this.getStackRValue(2));
                    AssignmentSimple s4 = new AssignmentSimple(this.loc, this.getStackLValue(3), this.getStackRValue(0));
                    AssignmentSimple s5 = new AssignmentSimple(this.loc, this.getStackLValue(4), this.getStackRValue(1));
                    return new CompoundStatement(this.loc, s1, s2, s3, s4, s5);
                }
                AssignmentSimple s1 = new AssignmentSimple(this.loc, this.getStackLValue(0), this.getStackRValue(0));
                AssignmentSimple s2 = new AssignmentSimple(this.loc, this.getStackLValue(1), this.getStackRValue(1));
                AssignmentSimple s3 = new AssignmentSimple(this.loc, this.getStackLValue(2), this.getStackRValue(2));
                AssignmentSimple s4 = new AssignmentSimple(this.loc, this.getStackLValue(3), this.getStackRValue(3));
                AssignmentSimple s5 = new AssignmentSimple(this.loc, this.getStackLValue(4), this.getStackRValue(0));
                AssignmentSimple s6 = new AssignmentSimple(this.loc, this.getStackLValue(5), this.getStackRValue(1));
                return new CompoundStatement(this.loc, s1, s2, s3, s4, s5, s6);
            }
            case LDC: 
            case LDC_W: 
            case LDC2_W: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), this.getLiteralConstantPoolEntry(method, this.cpEntries[0], comments));
            }
            case MONITORENTER: {
                return new MonitorEnterStatement(this.loc, this.getStackRValue(0), blockIdentifierFactory.getNextBlockIdentifier(BlockType.MONITOR));
            }
            case MONITOREXIT: {
                return new MonitorExitStatement(this.loc, this.getStackRValue(0));
            }
            case FAKE_TRY: {
                return new TryStatement(this.loc, this.getSingleExceptionGroup());
            }
            case FAKE_CATCH: {
                return new CatchStatement(this.loc, this.catchExceptionGroups, this.getStackLValue(0));
            }
            case NOP: {
                return new Nop();
            }
            case POP: {
                return new ExpressionStatement(this.getStackRValue(0));
            }
            case POP2: {
                if (this.stackConsumed.get(0).getStackEntry().getType().getComputationCategory() == 2) {
                    return new ExpressionStatement(this.getStackRValue(0));
                }
                ExpressionStatement s1 = new ExpressionStatement(this.getStackRValue(0));
                ExpressionStatement s2 = new ExpressionStatement(this.getStackRValue(1));
                return new CompoundStatement(this.loc, s1, s2);
            }
            case TABLESWITCH: {
                return new RawSwitchStatement(this.loc, Op02WithProcessedDataAndRefs.ensureNonBool(this.getStackRValue(0)), new DecodedTableSwitch(this.rawData, this.originalRawOffset));
            }
            case LOOKUPSWITCH: {
                return new RawSwitchStatement(this.loc, Op02WithProcessedDataAndRefs.ensureNonBool(this.getStackRValue(0)), new DecodedLookupSwitch(this.rawData, this.originalRawOffset));
            }
            case IINC: {
                int variableIndex = this.getInstrArgU1(0);
                int incrAmount = this.getInstrArgByte(1);
                ArithOp op = ArithOp.PLUS;
                if (incrAmount < 0) {
                    incrAmount = -incrAmount;
                    op = ArithOp.MINUS;
                }
                LValue lvalue = variableFactory.localVariable(variableIndex, this.localVariablesBySlot.get(variableIndex), this.originalRawOffset);
                return new AssignmentSimple(this.loc, lvalue, new ArithmeticOperation(this.loc, new LValueExpression(this.loc, lvalue), new Literal(TypedLiteral.getInt(incrAmount)), op));
            }
            case IINC_WIDE: {
                int variableIndex = this.getInstrArgShort(1);
                int incrAmount = this.getInstrArgShort(3);
                ArithOp op = ArithOp.PLUS;
                if (incrAmount < 0) {
                    incrAmount = -incrAmount;
                    op = ArithOp.MINUS;
                }
                LValue lvalue = variableFactory.localVariable(variableIndex, this.localVariablesBySlot.get(variableIndex), this.originalRawOffset);
                return new AssignmentSimple(this.loc, lvalue, new ArithmeticOperation(this.loc, new LValueExpression(this.loc, lvalue), new Literal(TypedLiteral.getInt(incrAmount)), op));
            }
            case DNEG: 
            case FNEG: 
            case LNEG: 
            case INEG: {
                return new AssignmentSimple(this.loc, this.getStackLValue(0), new ArithmeticMonOperation(this.loc, this.getStackRValue(0), ArithOp.MINUS));
            }
        }
        throw new ConfusedCFRException("Not implemented - conversion to statement from " + (Object)((Object)this.instr));
    }

    public Pair<Integer, Integer> getIincInfo() {
        if (this.instr != JVMInstr.IINC) {
            throw new ConfusedCFRException("Should be IINC");
        }
        int variableIndex = this.getInstrArgU1(0);
        int incrAmount = this.getInstrArgByte(1);
        return Pair.make(variableIndex, incrAmount);
    }

    private Expression getLiteralConstantPoolEntry(Method m, ConstantPoolEntry cpe, DecompilerComments comments) {
        if (cpe instanceof ConstantPoolEntryLiteral) {
            return new Literal(TypedLiteral.getConstantPoolEntry(this.cp, cpe));
        }
        if (cpe instanceof ConstantPoolEntryDynamicInfo) {
            return this.getDynamicLiteral(m, (ConstantPoolEntryDynamicInfo)cpe, comments);
        }
        if (cpe instanceof ConstantPoolEntryMethodHandle) {
            return this.getMethodHandleLiteral((ConstantPoolEntryMethodHandle)cpe);
        }
        if (cpe instanceof ConstantPoolEntryMethodType) {
            return this.getMethodTypeLiteral((ConstantPoolEntryMethodType)cpe);
        }
        throw new ConfusedCFRException("Constant pool entry is neither literal, dynamic literal, method handle or method type.");
    }

    private Expression getMethodTypeLiteral(ConstantPoolEntryMethodType cpe) {
        Literal descriptorString = new Literal(TypedLiteral.getConstantPoolEntryUTF8(cpe.getDescriptor()));
        return MethodHandlePlaceholder.getMethodType(descriptorString);
    }

    private Expression getMethodHandleLiteral(ConstantPoolEntryMethodHandle cpe) {
        return new MethodHandlePlaceholder(this.loc, cpe);
    }

    private Expression getDynamicLiteral(Method method, ConstantPoolEntryDynamicInfo cpe, DecompilerComments comments) {
        ClassFile classFile = method.getClassFile();
        ConstantPoolEntryNameAndType nameAndType = cpe.getNameAndTypeEntry();
        int idx = cpe.getBootstrapMethodAttrIndex();
        MethodPrototype dynamicProto = new MethodPrototype(this.cp.getDCCommonState(), classFile, classFile.getClassType(), "???", false, Method.MethodConstructor.NOT, Collections.<FormalTypeParameter>emptyList(), Collections.<JavaTypeInstance>emptyList(), nameAndType.decodeTypeTok(), Collections.<JavaTypeInstance>emptyList(), false, new VariableNamerDefault(), false, "");
        Statement s = this.buildInvokeDynamic(method.getClassFile(), this.cp.getDCCommonState(), nameAndType.getName().getValue(), dynamicProto, idx, true, comments);
        if (!(s instanceof AssignmentSimple)) {
            throw new ConfusedCFRException("Expected a result from a dynamic literal");
        }
        AssignmentSimple as = (AssignmentSimple)s;
        return new DynamicConstExpression(this.loc, as.getRValue());
    }

    private StackValue getStackRValue(int idx) {
        StackEntryHolder stackEntryHolder = this.stackConsumed.get(idx);
        StackEntry stackEntry = stackEntryHolder.getStackEntry();
        stackEntry.incrementUsage();
        return new StackValue(this.loc, stackEntry.getLValue());
    }

    private LValue getStackLValue(int idx) {
        StackEntryHolder stackEntryHolder = this.stackProduced.get(idx);
        StackEntry stackEntry = stackEntryHolder.getStackEntry();
        return stackEntry.getLValue();
    }

    private List<Expression> getNStackRValuesAsExpressions(int count) {
        List<Expression> res = ListFactory.newList();
        for (int i = count - 1; i >= 0; --i) {
            res.add(this.getStackRValue(i));
        }
        return res;
    }

    public String toString() {
        return "" + this.index + " : " + (Object)((Object)this.instr) + " - " + this.ssaIdentifiers;
    }

    public static DecompilerComment populateStackInfo(List<Op02WithProcessedDataAndRefs> op2list, Method method) {
        Set<DecompilerComment> comments = SetFactory.newSet();
        for (Op02WithProcessedDataAndRefs op : op2list) {
            op.resetStackInfo();
        }
        LinkedList<Pair<StackSim, Op02WithProcessedDataAndRefs>> toProcess = ListFactory.newLinkedList();
        toProcess.add(Pair.make(new StackSim(), op2list.get(0)));
        try {
            while (!toProcess.isEmpty()) {
                Pair next = (Pair)toProcess.removeFirst();
                Op02WithProcessedDataAndRefs o2 = (Op02WithProcessedDataAndRefs)next.getSecond();
                StackSim stackSim = (StackSim)next.getFirst();
                o2.populateStackInfo(stackSim, method, comments, toProcess);
            }
        }
        catch (ConfusedCFRException e) {
            ToStringDumper dmp = new ToStringDumper();
            dmp.print("----[known stack info]------------").newln().newln();
            for (Op02WithProcessedDataAndRefs op : op2list) {
                op.dump(dmp);
            }
            System.err.print(((Object)dmp).toString());
            throw e;
        }
        if (comments.isEmpty()) {
            return null;
        }
        return (DecompilerComment)SetUtil.getSingle(comments);
    }

    public static void unlinkUnreachable(List<Op02WithProcessedDataAndRefs> op2list) {
        final Set reached = SetFactory.newSet();
        GraphVisitorDFS<Op02WithProcessedDataAndRefs> reachableVisitor = new GraphVisitorDFS<Op02WithProcessedDataAndRefs>(op2list.get(0), new BinaryProcedure<Op02WithProcessedDataAndRefs, GraphVisitor<Op02WithProcessedDataAndRefs>>(){

            @Override
            public void call(Op02WithProcessedDataAndRefs arg1, GraphVisitor<Op02WithProcessedDataAndRefs> arg2) {
                reached.add(arg1);
                for (Op02WithProcessedDataAndRefs target : arg1.getTargets()) {
                    arg2.enqueue(target);
                }
            }
        });
        reachableVisitor.process();
        for (Op02WithProcessedDataAndRefs op : op2list) {
            if (reached.contains(op)) continue;
            for (Op02WithProcessedDataAndRefs target : op.targets) {
                target.removeSource(op);
            }
            op.instr = JVMInstr.NOP;
            op.targets.clear();
        }
    }

    public void nop() {
        this.instr = JVMInstr.NOP;
    }

    public void replaceInstr(JVMInstr newInstr) {
        this.instr = newInstr;
    }

    private void collectLocallyMutatedVariables(SSAIdentifierFactory<Slot, StackType> ssaIdentifierFactory) {
        Pair<JavaTypeInstance, Integer> storage = this.getStorageType();
        if (storage != null) {
            this.ssaIdentifiers = new SSAIdentifiers<Slot>(new Slot(storage.getFirst(), storage.getSecond()), ssaIdentifierFactory);
            return;
        }
        this.ssaIdentifiers = new SSAIdentifiers();
    }

    private static void assignSSAIdentifiers(SSAIdentifierFactory<Slot, StackType> ssaIdentifierFactory, Method method, DecompilerComments comments, List<Op02WithProcessedDataAndRefs> statements, BytecodeMeta bytecodeMeta) {
        NavigableMap<Integer, JavaTypeInstance> missing = Op02WithProcessedDataAndRefs.assignIdentsAndGetMissingMap(ssaIdentifierFactory, method, statements, bytecodeMeta, true);
        if (missing.isEmpty()) {
            return;
        }
        if (!method.getConstructorFlag().isConstructor()) {
            throw new IllegalStateException("Invisible function parameters on a non-constructor (or reads of uninitialised local variables).");
        }
        JavaTypeInstance classType = method.getClassFile().getClassType();
        if (classType.getInnerClassHereInfo().isMethodScopedClass()) {
            missing = Op02WithProcessedDataAndRefs.assignIdentsAndGetMissingMap(ssaIdentifierFactory, method, statements, bytecodeMeta, false);
            method.getMethodPrototype().setMethodScopedSyntheticConstructorParameters(missing);
            Op02WithProcessedDataAndRefs.assignIdentsAndGetMissingMap(ssaIdentifierFactory, method, statements, bytecodeMeta, true);
        } else {
            method.getMethodPrototype().setNonMethodScopedSyntheticConstructorParameters(method.getConstructorFlag(), comments, missing);
        }
        Op02WithProcessedDataAndRefs.assignSSAIdentifiersInner(ssaIdentifierFactory, method, statements, bytecodeMeta, true);
    }

    private static NavigableMap<Integer, JavaTypeInstance> assignIdentsAndGetMissingMap(SSAIdentifierFactory<Slot, StackType> ssaIdentifierFactory, Method method, List<Op02WithProcessedDataAndRefs> statements, BytecodeMeta bytecodeMeta, boolean useProtoArgs) {
        Op02WithProcessedDataAndRefs.assignSSAIdentifiersInner(ssaIdentifierFactory, method, statements, bytecodeMeta, useProtoArgs);
        TreeMap<Integer, JavaTypeInstance> missing = MapFactory.newTreeMap();
        for (Op02WithProcessedDataAndRefs op02 : statements) {
            SSAIdent ident;
            Pair<JavaTypeInstance, Integer> load = op02.getRetrieveType();
            if (load == null || (ident = op02.ssaIdentifiers.getSSAIdentOnExit(new Slot(load.getFirst(), load.getSecond()))) != null) continue;
            missing.put(load.getSecond(), load.getFirst());
        }
        return missing;
    }

    private static void assignSSAIdentifiersInner(SSAIdentifierFactory<Slot, StackType> ssaIdentifierFactory, Method method, List<Op02WithProcessedDataAndRefs> statements, BytecodeMeta bytecodeMeta, boolean useProtoArgs) {
        Map idents = useProtoArgs ? method.getMethodPrototype().collectInitialSlotUsage(ssaIdentifierFactory) : MapFactory.newMap();
        for (Op02WithProcessedDataAndRefs statement : statements) {
            statement.collectLocallyMutatedVariables(ssaIdentifierFactory);
        }
        statements.get((int)0).ssaIdentifiers = new SSAIdentifiers(idents);
        final Set<Integer> livenessClashes = bytecodeMeta.getLivenessClashes();
        BinaryPredicate<Slot, Slot> testSlot = new BinaryPredicate<Slot, Slot>(){

            @Override
            public boolean test(Slot a, Slot b) {
                StackType t2;
                StackType t1 = a.getJavaTypeInstance().getStackType();
                if (t1 == (t2 = b.getJavaTypeInstance().getStackType())) {
                    if (t1.isClosed()) {
                        return true;
                    }
                    if (livenessClashes.isEmpty()) {
                        return true;
                    }
                    return !livenessClashes.contains(a.getIdx());
                }
                return false;
            }
        };
        BinaryPredicate<Slot, Slot> always = new BinaryPredicate<Slot, Slot>(){

            @Override
            public boolean test(Slot a, Slot b) {
                return false;
            }
        };
        UniqueSeenQueue<Op02WithProcessedDataAndRefs> toProcess = new UniqueSeenQueue<Op02WithProcessedDataAndRefs>(statements);
        while (!toProcess.isEmpty()) {
            Op02WithProcessedDataAndRefs statement = toProcess.removeFirst();
            SSAIdentifiers<Slot> ssaIdentifiers = statement.ssaIdentifiers;
            boolean changed = false;
            BinaryPredicate<Slot, Slot> test = testSlot;
            if (statement.hasCatchParent) {
                test = always;
            }
            for (Op02WithProcessedDataAndRefs source : statement.getSources()) {
                if (!ssaIdentifiers.mergeWith(source.ssaIdentifiers, test)) continue;
                changed = true;
            }
            if (!changed) continue;
            toProcess.addAll(statement.getTargets());
        }
    }

    private static void removeUnusedSSAIdentifiers(SSAIdentifierFactory<Slot, StackType> ssaIdentifierFactory, Method method, List<Op02WithProcessedDataAndRefs> op2list) {
        final List endPoints = ListFactory.newList();
        GraphVisitorDFS<Op02WithProcessedDataAndRefs> gv = new GraphVisitorDFS<Op02WithProcessedDataAndRefs>(op2list.get(0), new BinaryProcedure<Op02WithProcessedDataAndRefs, GraphVisitor<Op02WithProcessedDataAndRefs>>(){

            @Override
            public void call(Op02WithProcessedDataAndRefs arg1, GraphVisitor<Op02WithProcessedDataAndRefs> arg2) {
                if (arg1.getTargets().isEmpty()) {
                    endPoints.add(arg1);
                } else {
                    arg2.enqueue(arg1.getTargets());
                }
            }
        });
        gv.process();
        UniqueSeenQueue toProcess = new UniqueSeenQueue(endPoints);
        SSAIdentifiers<Slot> initial = new SSAIdentifiers<Slot>(op2list.get((int)0).ssaIdentifiers);
        List<Op02WithProcessedDataAndRefs> storeWithoutRead = ListFactory.newList();
        while (!toProcess.isEmpty()) {
            Op02WithProcessedDataAndRefs node = (Op02WithProcessedDataAndRefs)toProcess.removeFirst();
            Pair<JavaTypeInstance, Integer> retrieved = node.getRetrieveType();
            Pair<JavaTypeInstance, Integer> stored = node.getStorageType();
            SSAIdentifiers<Slot> ssaIdents = node.ssaIdentifiers;
            Map<Slot, SSAIdent> idents = ssaIdents.getKnownIdentifiersOnExit();
            Iterator<Map.Entry<Slot, SSAIdent>> iterator = idents.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Slot, SSAIdent> entry = iterator.next();
                Slot slot = entry.getKey();
                SSAIdent thisIdent = entry.getValue();
                boolean used = false;
                if (retrieved != null && retrieved.getSecond().intValue() == slot.getIdx()) {
                    used = true;
                }
                if (!used) {
                    for (Op02WithProcessedDataAndRefs target : node.targets) {
                        if (target.ssaIdentifiers.getSSAIdentOnEntry(slot) == null) continue;
                        used = true;
                        break;
                    }
                }
                if (!used && stored != null) {
                    for (Op02WithProcessedDataAndRefs source : node.sources) {
                        SSAIdent sourceIdent = source.ssaIdentifiers.getSSAIdentOnExit(slot);
                        if (sourceIdent == null || !thisIdent.isSuperSet(sourceIdent)) continue;
                        used = true;
                        break;
                    }
                }
                if (!used) {
                    for (Op02WithProcessedDataAndRefs source : node.sources) {
                        toProcess.add(source);
                    }
                    if (stored != null && stored.getSecond().intValue() == slot.getIdx()) {
                        storeWithoutRead.add(node);
                    }
                    iterator.remove();
                    ssaIdents.removeEntryIdent(slot);
                    continue;
                }
                for (Op02WithProcessedDataAndRefs source : node.sources) {
                    toProcess.addIfUnseen(source);
                }
            }
        }
        for (Op02WithProcessedDataAndRefs store : storeWithoutRead) {
            Pair<JavaTypeInstance, Integer> storage = store.getStorageType();
            Slot slot = new Slot(storage.getFirst(), storage.getSecond());
            SSAIdent ident = ssaIdentifierFactory.getIdent(slot);
            store.ssaIdentifiers.setKnownIdentifierOnExit(slot, ident);
        }
        op2list.get((int)0).ssaIdentifiers.mergeWith(initial);
    }

    public static void discoverStorageLiveness(Method method, DecompilerComments comments, List<Op02WithProcessedDataAndRefs> op2list, BytecodeMeta bytecodeMeta) {
        Slot slot;
        SSAIdentifierFactory<Slot, StackType> ssaIdentifierFactory = new SSAIdentifierFactory<Slot, StackType>(new UnaryFunction<Slot, StackType>(){

            @Override
            public StackType invoke(Slot arg) {
                return arg.getJavaTypeInstance().getStackType();
            }
        });
        Op02WithProcessedDataAndRefs.assignSSAIdentifiers(ssaIdentifierFactory, method, comments, op2list, bytecodeMeta);
        Op02WithProcessedDataAndRefs.removeUnusedSSAIdentifiers(ssaIdentifierFactory, method, op2list);
        Map<Slot, Map<SSAIdent, Set<SSAIdent>>> identChain = MapFactory.newLinkedLazyMap(new UnaryFunction<Slot, Map<SSAIdent, Set<SSAIdent>>>(){

            @Override
            public Map<SSAIdent, Set<SSAIdent>> invoke(Slot arg) {
                return MapFactory.newLinkedLazyMap(new UnaryFunction<SSAIdent, Set<SSAIdent>>(){

                    @Override
                    public Set<SSAIdent> invoke(SSAIdent arg) {
                        return SetFactory.newOrderedSet();
                    }
                });
            }
        });
        LazyMap<Slot, Set<SSAIdent>> poisoned = MapFactory.newLazyMap(new UnaryFunction<Slot, Set<SSAIdent>>(){

            @Override
            public Set<SSAIdent> invoke(Slot arg) {
                return SetFactory.newSet();
            }
        });
        final Set<Integer> livenessClashes = bytecodeMeta.getLivenessClashes();
        for (Op02WithProcessedDataAndRefs op : op2list) {
            Set<Slot> set;
            SSAIdentifiers<Slot> identifiers = op.ssaIdentifiers;
            if (op.hasCatchParent && !(set = identifiers.getFixedHere()).isEmpty()) {
                for (Slot slot2 : set) {
                    SSAIdent finalIdent = identifiers.getSSAIdentOnExit(slot2);
                    ((Set)poisoned.get(slot2)).add(finalIdent);
                }
            }
            Map<Slot, SSAIdent> map = identifiers.getKnownIdentifiersOnExit();
            for (Map.Entry entry : map.entrySet()) {
                Slot thisSlot = (Slot)entry.getKey();
                SSAIdent thisIdents = (SSAIdent)entry.getValue();
                Map<SSAIdent, Set<SSAIdent>> map2 = identChain.get(thisSlot);
                Set<SSAIdent> thisNextSet = map2.get(thisIdents);
                for (Op02WithProcessedDataAndRefs tgt : op.getTargets()) {
                    SSAIdent nextIdents = tgt.ssaIdentifiers.getSSAIdentOnExit(thisSlot);
                    if (nextIdents == null || !nextIdents.isSuperSet(thisIdents)) continue;
                    thisNextSet.add(nextIdents);
                }
            }
        }
        final Map<Pair<Slot, SSAIdent>, Ident> combinedMap = MapFactory.newOrderedMap();
        IdentFactory identFactory = new IdentFactory();
        for (Map.Entry entry : poisoned.entrySet()) {
            slot = (Slot)entry.getKey();
            Map<SSAIdent, Set<SSAIdent>> map = identChain.get(slot);
            for (SSAIdent key : (Set)entry.getValue()) {
                map.get(key).clear();
            }
        }
        for (Map.Entry<Object, Object> entry : identChain.entrySet()) {
            slot = (Slot)entry.getKey();
            final Map map = (Map)entry.getValue();
            final Map<SSAIdent, Set<SSAIdent>> upMap = Op02WithProcessedDataAndRefs.createReverseMap(map);
            Set<SSAIdent> keys = SetFactory.newOrderedSet();
            keys.addAll(map.keySet());
            keys.addAll(upMap.keySet());
            for (SSAIdent key : keys) {
                final Pair<Slot, SSAIdent> slotkey = Pair.make(slot, key);
                if (combinedMap.containsKey(slotkey)) continue;
                final Ident thisIdent = identFactory.getNextIdent(slot.getIdx());
                GraphVisitorDFS<SSAIdent> gv = new GraphVisitorDFS<SSAIdent>(key, new BinaryProcedure<SSAIdent, GraphVisitor<SSAIdent>>(){

                    @Override
                    public void call(SSAIdent arg1, GraphVisitor<SSAIdent> arg2) {
                        Pair<Slot, SSAIdent> innerslotkey = Pair.make(slot, arg1);
                        if (livenessClashes.contains(slot.getIdx()) && !innerslotkey.equals(slotkey)) {
                            StackType s1 = innerslotkey.getFirst().getJavaTypeInstance().getStackType();
                            StackType s2 = ((Slot)slotkey.getFirst()).getJavaTypeInstance().getStackType();
                            if (innerslotkey.getSecond().getComparisonType() instanceof StackType) {
                                s1 = (StackType)((Object)innerslotkey.getSecond().getComparisonType());
                            }
                            if (((SSAIdent)slotkey.getSecond()).getComparisonType() instanceof StackType) {
                                s2 = (StackType)((Object)innerslotkey.getSecond().getComparisonType());
                            }
                            if (s1 != s2 || !s1.isClosed() || s1 == StackType.INT) {
                                return;
                            }
                        }
                        if (combinedMap.containsKey(innerslotkey)) {
                            return;
                        }
                        combinedMap.put(innerslotkey, thisIdent);
                        arg2.enqueue((Collection)map.get(arg1));
                        arg2.enqueue((Collection)upMap.get(arg1));
                    }
                });
                gv.process();
            }
        }
        for (Op02WithProcessedDataAndRefs op02WithProcessedDataAndRefs : op2list) {
            op02WithProcessedDataAndRefs.mapSSASlots(combinedMap);
        }
        method.getMethodPrototype().computeParameters(method.getConstructorFlag(), op2list.get((int)0).localVariablesBySlot);
    }

    private void mapSSASlots(Map<Pair<Slot, SSAIdent>, Ident> identmap) {
        Map<Slot, SSAIdent> knownIdents = this.ssaIdentifiers.getKnownIdentifiersOnExit();
        for (Map.Entry<Slot, SSAIdent> entry : knownIdents.entrySet()) {
            Ident ident = identmap.get(Pair.make(entry.getKey(), entry.getValue()));
            if (ident == null) {
                throw new IllegalStateException("Null ident");
            }
            this.localVariablesBySlot.put(entry.getKey().getIdx(), ident);
        }
    }

    public ConstantPool getCp() {
        return this.cp;
    }

    public int getOriginalRawOffset() {
        return this.originalRawOffset;
    }

    public BytecodeLoc getBytecodeLoc() {
        return this.loc;
    }

    private static Map<SSAIdent, Set<SSAIdent>> createReverseMap(Map<SSAIdent, Set<SSAIdent>> downMap) {
        Map<SSAIdent, Set<SSAIdent>> res = MapFactory.newLinkedLazyMap(new UnaryFunction<SSAIdent, Set<SSAIdent>>(){

            @Override
            public Set<SSAIdent> invoke(SSAIdent arg) {
                return SetFactory.newOrderedSet();
            }
        });
        for (Map.Entry<SSAIdent, Set<SSAIdent>> entry : downMap.entrySet()) {
            SSAIdent revValue = entry.getKey();
            Set<SSAIdent> revKeys = entry.getValue();
            for (SSAIdent revKey : revKeys) {
                res.get(revKey).add(revValue);
            }
        }
        return res;
    }

    public static List<Op03SimpleStatement> convertToOp03List(List<Op02WithProcessedDataAndRefs> op2list, final Method method, final VariableFactory variableFactory, final BlockIdentifierFactory blockIdentifierFactory, final DCCommonState dcCommonState, final DecompilerComments comments, final TypeHintRecovery typeHintRecovery) {
        final List<Op03SimpleStatement> op03SimpleParseNodesTmp = ListFactory.newList();
        final GraphConversionHelper conversionHelper = new GraphConversionHelper();
        GraphVisitorFIFO<Op02WithProcessedDataAndRefs> o2Converter = new GraphVisitorFIFO<Op02WithProcessedDataAndRefs>(op2list.get(0), new BinaryProcedure<Op02WithProcessedDataAndRefs, GraphVisitor<Op02WithProcessedDataAndRefs>>(){

            @Override
            public void call(Op02WithProcessedDataAndRefs arg1, GraphVisitor<Op02WithProcessedDataAndRefs> arg2) {
                Op03SimpleStatement res = new Op03SimpleStatement(arg1, arg1.createStatement(method, comments, variableFactory, blockIdentifierFactory, dcCommonState, typeHintRecovery));
                conversionHelper.registerOriginalAndNew(arg1, res);
                op03SimpleParseNodesTmp.add(res);
                for (Op02WithProcessedDataAndRefs target : arg1.getTargets()) {
                    arg2.enqueue(target);
                }
            }
        });
        o2Converter.process();
        conversionHelper.patchUpRelations();
        return op03SimpleParseNodesTmp;
    }

    private static Op02WithProcessedDataAndRefs adjustOrdering(Map<InstrIndex, List<ExceptionTempStatement>> insertions, Op02WithProcessedDataAndRefs infrontOf, ExceptionGroup exceptionGroup, Op02WithProcessedDataAndRefs newNode) {
        Op02WithProcessedDataAndRefs afterThis;
        InstrIndex idxInfrontOf = infrontOf.getIndex();
        List<ExceptionTempStatement> collides = insertions.get(idxInfrontOf);
        ExceptionTempStatement exceptionTempStatement = new ExceptionTempStatement(exceptionGroup, newNode);
        if (collides.isEmpty()) {
            collides.add(exceptionTempStatement);
            return infrontOf;
        }
        logger.finer("Adding " + newNode + " ident " + exceptionGroup.getTryBlockIdentifier());
        logger.finer("Already have " + collides);
        int insertionPos = Collections.binarySearch(collides, exceptionTempStatement);
        insertionPos = insertionPos >= 0 ? ++insertionPos : -(insertionPos + 1);
        if (insertionPos == 0) {
            collides.add(0, exceptionTempStatement);
            throw new ConfusedCFRException("EEk.");
        }
        logger.finer("Insertion position = " + insertionPos);
        if (insertionPos == collides.size()) {
            collides.add(exceptionTempStatement);
            afterThis = infrontOf;
        } else {
            afterThis = collides.get(insertionPos).getOp();
            collides.add(insertionPos, exceptionTempStatement);
        }
        for (ExceptionTempStatement ets : collides) {
            ets.getOp().setIndex(infrontOf.getIndex().justBefore());
        }
        return afterThis;
    }

    private static void tidyMultipleInsertionIdentifiers(Collection<List<ExceptionTempStatement>> etsList) {
        for (List<ExceptionTempStatement> ets : etsList) {
            if (ets.size() <= 1) continue;
            for (int idx = 0; idx < ets.size(); ++idx) {
                ExceptionTempStatement et = ets.get(idx);
                if (!et.isTry()) continue;
                BlockIdentifier tryGroup = et.triggeringGroup.getTryBlockIdentifier();
                logger.finer("Removing try group identifier " + tryGroup + " idx " + idx);
                for (int idx2 = 0; idx2 < idx; ++idx2) {
                    logger.finest("" + ets.get(idx2).getOp());
                    logger.finest("" + ets.get((int)idx2).getOp().containedInTheseBlocks + " -->");
                    ets.get((int)idx2).getOp().containedInTheseBlocks.remove(tryGroup);
                    logger.finest("" + ets.get((int)idx2).getOp().containedInTheseBlocks);
                }
            }
        }
    }

    private static int getLastIndex(Map<Integer, Integer> lutByOffset, int op2count, long codeLength, int offset) {
        Integer iinclusiveLastIndex = lutByOffset.get(offset);
        if (iinclusiveLastIndex == null) {
            if ((long)offset == codeLength) {
                iinclusiveLastIndex = op2count - 1;
            } else {
                throw new ConfusedCFRException("Last index of " + offset + " is not a valid entry into the code block");
            }
        }
        return iinclusiveLastIndex;
    }

    public static List<Op02WithProcessedDataAndRefs> insertExceptionBlocks(List<Op02WithProcessedDataAndRefs> op2list, ExceptionAggregator exceptions, Map<Integer, Integer> lutByOffset, ConstantPool cp, long codeLength, Options options) {
        int originalIndex;
        BlockIdentifier tryBlockIdentifier;
        int originalInstrCount = op2list.size();
        if (exceptions.getExceptionsGroups().isEmpty()) {
            return op2list;
        }
        LazyMap<InstrIndex, List<ExceptionTempStatement>> insertions = MapFactory.newLazyMap(new UnaryFunction<InstrIndex, List<ExceptionTempStatement>>(){

            @Override
            public List<ExceptionTempStatement> invoke(InstrIndex ignore) {
                return ListFactory.newList();
            }
        });
        for (ExceptionGroup exceptionGroup : exceptions.getExceptionsGroups()) {
            tryBlockIdentifier = exceptionGroup.getTryBlockIdentifier();
            originalIndex = lutByOffset.get(exceptionGroup.getBytecodeIndexFrom());
            int exclusiveLastIndex = Op02WithProcessedDataAndRefs.getLastIndex(lutByOffset, originalInstrCount, codeLength, exceptionGroup.getBytecodeIndexTo());
            for (int x = originalIndex; x < exclusiveLastIndex; ++x) {
                op2list.get((int)x).containedInTheseBlocks.add(tryBlockIdentifier);
            }
        }
        for (ExceptionGroup exceptionGroup : exceptions.getExceptionsGroups()) {
            List<ExceptionGroup.Entry> rawes = exceptionGroup.getEntries();
            originalIndex = lutByOffset.get(exceptionGroup.getBytecodeIndexFrom());
            Op02WithProcessedDataAndRefs startInstruction = op2list.get(originalIndex);
            int inclusiveLastIndex = Op02WithProcessedDataAndRefs.getLastIndex(lutByOffset, originalInstrCount, codeLength, exceptionGroup.getBytecodeIndexTo());
            Op02WithProcessedDataAndRefs lastTryInstruction = op2list.get(inclusiveLastIndex);
            List<Pair> handlerTargets = ListFactory.newList();
            for (ExceptionGroup.Entry exceptionEntry : rawes) {
                int handler = exceptionEntry.getBytecodeIndexHandler();
                int handlerIndex = lutByOffset.get(handler);
                if (handlerIndex <= originalIndex && !((Boolean)options.getOption(OptionsImpl.LENIENT)).booleanValue()) {
                    throw new ConfusedCFRException("Back jump on a try block " + exceptionEntry);
                }
                Op02WithProcessedDataAndRefs handerTarget = op2list.get(handlerIndex);
                handlerTargets.add(Pair.make(handerTarget, exceptionEntry));
            }
            Op02WithProcessedDataAndRefs tryOp = new Op02WithProcessedDataAndRefs(JVMInstr.FAKE_TRY, null, startInstruction.getIndex().justBefore(), cp, null, -1, BytecodeLoc.NONE);
            startInstruction = Op02WithProcessedDataAndRefs.adjustOrdering(insertions, startInstruction, exceptionGroup, tryOp);
            tryOp.containedInTheseBlocks.addAll(startInstruction.containedInTheseBlocks);
            tryOp.containedInTheseBlocks.remove(exceptionGroup.getTryBlockIdentifier());
            tryOp.exceptionGroups.add(exceptionGroup);
            List removeThese = ListFactory.newList();
            for (Op02WithProcessedDataAndRefs source : startInstruction.getSources()) {
                if (startInstruction.getIndex().isBackJumpFrom(source.getIndex()) && !lastTryInstruction.getIndex().isBackJumpFrom(source.getIndex())) continue;
                source.replaceTarget(startInstruction, tryOp);
                removeThese.add(source);
                tryOp.addSource(source);
            }
            Iterator<Object> handler = removeThese.iterator();
            while (handler.hasNext()) {
                Op02WithProcessedDataAndRefs remove = handler.next();
                startInstruction.removeSource(remove);
            }
            for (Pair catchTargets : handlerTargets) {
                Op02WithProcessedDataAndRefs tryTarget = (Op02WithProcessedDataAndRefs)catchTargets.getFirst();
                List<Op02WithProcessedDataAndRefs> tryTargetSources = tryTarget.getSources();
                Op02WithProcessedDataAndRefs preCatchOp = null;
                boolean addFakeCatch = false;
                if (tryTargetSources.isEmpty()) {
                    addFakeCatch = true;
                } else {
                    for (Op02WithProcessedDataAndRefs source : tryTargetSources) {
                        if (source.getInstr() == JVMInstr.FAKE_CATCH) {
                            preCatchOp = source;
                            continue;
                        }
                        if (((Boolean)options.getOption(OptionsImpl.LENIENT)).booleanValue()) continue;
                        throw new ConfusedCFRException("non catch before exception catch block");
                    }
                    if (preCatchOp == null) {
                        addFakeCatch = true;
                    }
                }
                if (addFakeCatch) {
                    ExceptionGroup.Entry entry = (ExceptionGroup.Entry)catchTargets.getSecond();
                    byte[] data = null;
                    if (entry.isJustThrowable()) {
                        data = new byte[]{};
                    }
                    preCatchOp = new Op02WithProcessedDataAndRefs(JVMInstr.FAKE_CATCH, data, tryTarget.getIndex().justBefore(), cp, null, -1, BytecodeLoc.NONE);
                    tryTarget = Op02WithProcessedDataAndRefs.adjustOrdering(insertions, tryTarget, exceptionGroup, preCatchOp);
                    preCatchOp.containedInTheseBlocks.addAll(tryTarget.getContainedInTheseBlocks());
                    preCatchOp.addTarget(tryTarget);
                    if (JVMInstr.isAStore(tryTarget.getInstr())) {
                        tryTarget.hasCatchParent = true;
                    }
                    tryTarget.addSource(preCatchOp);
                    op2list.add(preCatchOp);
                }
                if (preCatchOp == null) {
                    throw new IllegalStateException("Bad precatch op state.");
                }
                preCatchOp.addSource(tryOp);
                tryOp.addTarget(preCatchOp);
                preCatchOp.catchExceptionGroups.add((ExceptionGroup.Entry)catchTargets.getSecond());
            }
            tryOp.targets.add(0, startInstruction);
            startInstruction.addSource(tryOp);
            op2list.add(tryOp);
        }
        for (ExceptionGroup exceptionGroup : exceptions.getExceptionsGroups()) {
            tryBlockIdentifier = exceptionGroup.getTryBlockIdentifier();
            int beforeLastIndex = Op02WithProcessedDataAndRefs.getLastIndex(lutByOffset, originalInstrCount, codeLength, exceptionGroup.getBytecodeIndexTo()) - 1;
            Op02WithProcessedDataAndRefs lastStatement = op2list.get(beforeLastIndex);
            Set<BlockIdentifier> blocks = SetFactory.newSet(lastStatement.containedInTheseBlocks);
            int x = beforeLastIndex + 1;
            if (lastStatement.targets.size() != 1 || op2list.get(x) != lastStatement.targets.get(0)) continue;
            Op02WithProcessedDataAndRefs next = op2list.get(x);
            boolean bOk = true;
            if (next.sources.size() > 1) {
                for (Op02WithProcessedDataAndRefs source : next.sources) {
                    Set<BlockIdentifier> blocks2 = SetFactory.newSet(source.containedInTheseBlocks);
                    if (blocks.equals(blocks2)) continue;
                    bOk = false;
                }
            }
            Set<BlockIdentifier> blocksWithoutTry = SetFactory.newSet(blocks);
            blocksWithoutTry.remove(tryBlockIdentifier);
            if (!bOk) continue;
            switch (next.instr) {
                case RETURN: 
                case GOTO: 
                case GOTO_W: 
                case IRETURN: 
                case ARETURN: 
                case LRETURN: 
                case DRETURN: 
                case FRETURN: {
                    Set<BlockIdentifier> blocks2 = SetFactory.newSet(next.containedInTheseBlocks);
                    if (!blocksWithoutTry.equals(blocks2)) break;
                    next.containedInTheseBlocks.add(tryBlockIdentifier);
                }
            }
        }
        Op02WithProcessedDataAndRefs.tidyMultipleInsertionIdentifiers(insertions.values());
        return op2list;
    }

    public List<BlockIdentifier> getContainedInTheseBlocks() {
        return this.containedInTheseBlocks;
    }

    private static boolean isJSR(Op02WithProcessedDataAndRefs op) {
        JVMInstr instr = op.instr;
        return instr == JVMInstr.JSR || instr == JVMInstr.JSR_W;
    }

    private static boolean isRET(Op02WithProcessedDataAndRefs op) {
        JVMInstr instr = op.instr;
        return instr == JVMInstr.RET || instr == JVMInstr.RET_WIDE;
    }

    public static boolean processJSR(List<Op02WithProcessedDataAndRefs> ops) {
        List<Op02WithProcessedDataAndRefs> jsrInstrs = Op02WithProcessedDataAndRefs.justJSRs(ops);
        if (jsrInstrs.isEmpty()) {
            return false;
        }
        Op02WithProcessedDataAndRefs.processJSRs(jsrInstrs, ops);
        return true;
    }

    private static List<Op02WithProcessedDataAndRefs> justJSRs(List<Op02WithProcessedDataAndRefs> ops) {
        return Functional.filter(ops, new Predicate<Op02WithProcessedDataAndRefs>(){

            @Override
            public boolean test(Op02WithProcessedDataAndRefs in) {
                return Op02WithProcessedDataAndRefs.isJSR(in);
            }
        });
    }

    private static Op02WithProcessedDataAndRefs followNopGoto(Op02WithProcessedDataAndRefs op) {
        Set seen = SetFactory.newIdentitySet();
        do {
            if (op.getTargets().size() != 1) {
                return op;
            }
            JVMInstr instr = op.getInstr();
            if (instr != JVMInstr.NOP && instr != JVMInstr.GOTO && instr != JVMInstr.GOTO_W) {
                return op;
            }
            op = op.getTargets().get(0);
        } while (seen.add(op));
        return op;
    }

    private static void processJSRs(List<Op02WithProcessedDataAndRefs> jsrs, List<Op02WithProcessedDataAndRefs> ops) {
        Map<Op02WithProcessedDataAndRefs, Integer> idxOf = Functional.indexedIdentityMapOf(ops);
        Map<Op02WithProcessedDataAndRefs, List<Op02WithProcessedDataAndRefs>> targets = Op02WithProcessedDataAndRefs.getJsrsWithCommonTarget(jsrs);
        block0: for (Map.Entry<Op02WithProcessedDataAndRefs, List<Op02WithProcessedDataAndRefs>> entry : targets.entrySet()) {
            int idx;
            List<Op02WithProcessedDataAndRefs> onetarget = entry.getValue();
            if (onetarget.size() < 2 || (idx = idxOf.get(onetarget.get(0)).intValue()) >= ops.size()) continue;
            Op02WithProcessedDataAndRefs eventual = Op02WithProcessedDataAndRefs.followNopGoto(ops.get(idx + 1));
            for (int x = 1; x < onetarget.size(); ++x) {
                Op02WithProcessedDataAndRefs e2;
                idx = idxOf.get(onetarget.get(x));
                if (idx >= ops.size() || (e2 = Op02WithProcessedDataAndRefs.followNopGoto(ops.get(idx + 1))) != eventual) continue block0;
            }
            Op02WithProcessedDataAndRefs saveJsr = onetarget.get(onetarget.size() - 1);
            for (int x = 0; x < onetarget.size() - 1; ++x) {
                Op02WithProcessedDataAndRefs j = onetarget.get(x);
                j.targets.get(0).removeSource(j);
                j.targets.clear();
                j.targets.add(saveJsr);
                saveJsr.addSource(j);
                jsrs.remove(j);
                j.instr = JVMInstr.GOTO;
            }
        }
        boolean result = false;
        for (Op02WithProcessedDataAndRefs jsr : jsrs) {
            result |= Op02WithProcessedDataAndRefs.SimulateJSR(jsr, ops);
        }
        if (result) {
            jsrs = Op02WithProcessedDataAndRefs.justJSRs(jsrs);
        }
        targets = Op02WithProcessedDataAndRefs.getJsrsWithCommonTarget(jsrs);
        Set inlineCandidates = SetFactory.newSet();
        for (Op02WithProcessedDataAndRefs target : targets.keySet()) {
            GraphVisitorDFS<Op02WithProcessedDataAndRefs> gv = new GraphVisitorDFS<Op02WithProcessedDataAndRefs>(target.getTargets(), new BinaryProcedure<Op02WithProcessedDataAndRefs, GraphVisitor<Op02WithProcessedDataAndRefs>>(){

                @Override
                public void call(Op02WithProcessedDataAndRefs arg1, GraphVisitor<Op02WithProcessedDataAndRefs> arg2) {
                    if (Op02WithProcessedDataAndRefs.isRET(arg1)) {
                        return;
                    }
                    if (arg1 == Op02WithProcessedDataAndRefs.this) {
                        arg2.abort();
                        return;
                    }
                    arg2.enqueue(arg1.getTargets());
                }
            });
            gv.process();
            if (gv.wasAborted()) continue;
            Set<Op02WithProcessedDataAndRefs> nodes = SetFactory.newSet(gv.getVisitedNodes());
            nodes.add(target);
            if (SetUtil.hasIntersection(inlineCandidates, nodes)) continue;
            inlineCandidates.addAll(nodes);
            Op02WithProcessedDataAndRefs.inlineJSR(target, nodes, ops);
        }
        jsrs = Op02WithProcessedDataAndRefs.justJSRs(ops);
        for (Op02WithProcessedDataAndRefs jsr : jsrs) {
            Op02WithProcessedDataAndRefs target;
            List<Op02WithProcessedDataAndRefs> sources;
            if (!Op02WithProcessedDataAndRefs.isJSR(jsr) || (sources = targets.get(target = jsr.targets.get(0))) == null || sources.size() > 1) continue;
            final List<Op02WithProcessedDataAndRefs> rets = ListFactory.newList();
            GraphVisitorDFS<Op02WithProcessedDataAndRefs> gv = new GraphVisitorDFS<Op02WithProcessedDataAndRefs>(target.getTargets(), new BinaryProcedure<Op02WithProcessedDataAndRefs, GraphVisitor<Op02WithProcessedDataAndRefs>>(){

                @Override
                public void call(Op02WithProcessedDataAndRefs arg1, GraphVisitor<Op02WithProcessedDataAndRefs> arg2) {
                    if (Op02WithProcessedDataAndRefs.isRET(arg1)) {
                        rets.add(arg1);
                        return;
                    }
                    if (arg1 == target) {
                        return;
                    }
                    arg2.enqueue(arg1.getTargets());
                }
            });
            gv.process();
            int idx = ops.indexOf(jsr) + 1;
            if (idx >= ops.size()) continue;
            Op02WithProcessedDataAndRefs afterJsr = ops.get(idx);
            for (Op02WithProcessedDataAndRefs ret : rets) {
                ret.instr = JVMInstr.GOTO;
                ret.targets.clear();
                ret.addTarget(afterJsr);
                afterJsr.addSource(ret);
            }
            Op02WithProcessedDataAndRefs.inlineReplaceJSR(jsr, ops);
        }
        for (Op02WithProcessedDataAndRefs jsr : jsrs) {
            if (!Op02WithProcessedDataAndRefs.isJSR(jsr)) continue;
            Op02WithProcessedDataAndRefs.inlineReplaceJSR(jsr, ops);
        }
    }

    private static Map<Op02WithProcessedDataAndRefs, List<Op02WithProcessedDataAndRefs>> getJsrsWithCommonTarget(List<Op02WithProcessedDataAndRefs> jsrs) {
        return Functional.groupToMapBy(jsrs, new UnaryFunction<Op02WithProcessedDataAndRefs, Op02WithProcessedDataAndRefs>(){

            @Override
            public Op02WithProcessedDataAndRefs invoke(Op02WithProcessedDataAndRefs arg) {
                return arg.getTargets().get(0);
            }
        });
    }

    private static boolean SimulateJSR(Op02WithProcessedDataAndRefs start, List<Op02WithProcessedDataAndRefs> ops) {
        Op02WithProcessedDataAndRefs afterThis;
        List processed;
        Stack stackJumpLocs;
        Op02WithProcessedDataAndRefs currInstr;
        block32: {
            currInstr = start;
            stackJumpLocs = StackFactory.newStack();
            Map stackJumpLocLocals = MapFactory.newMap();
            processed = ListFactory.newList();
            afterThis = null;
            do {
                switch (currInstr.getInstr()) {
                    case JSR_W: 
                    case JSR: {
                        stackJumpLocs.push(currInstr);
                        break;
                    }
                    case GOTO: 
                    case GOTO_W: 
                    case NOP: {
                        break;
                    }
                    case ASTORE_0: {
                        if (stackJumpLocs.empty()) {
                            return false;
                        }
                        stackJumpLocLocals.put(0, stackJumpLocs.pop());
                        break;
                    }
                    case ASTORE_1: {
                        if (stackJumpLocs.empty()) {
                            return false;
                        }
                        stackJumpLocLocals.put(1, stackJumpLocs.pop());
                        break;
                    }
                    case ASTORE_2: {
                        if (stackJumpLocs.empty()) {
                            return false;
                        }
                        stackJumpLocLocals.put(2, stackJumpLocs.pop());
                        break;
                    }
                    case ASTORE_3: {
                        if (stackJumpLocs.empty()) {
                            return false;
                        }
                        stackJumpLocLocals.put(2, stackJumpLocs.pop());
                        break;
                    }
                    case ASTORE: {
                        if (stackJumpLocs.empty()) {
                            return false;
                        }
                        stackJumpLocLocals.put(currInstr.getInstrArgU1(0), stackJumpLocs.pop());
                        break;
                    }
                    case ASTORE_WIDE: {
                        if (stackJumpLocs.empty()) {
                            return false;
                        }
                        stackJumpLocLocals.put(currInstr.getInstrArgShort(1), stackJumpLocs.pop());
                        break;
                    }
                    case POP: {
                        if (stackJumpLocs.empty()) {
                            return false;
                        }
                        stackJumpLocs.pop();
                        break;
                    }
                    case POP2: {
                        if (stackJumpLocs.size() < 2) {
                            return false;
                        }
                        stackJumpLocs.pop();
                        stackJumpLocs.pop();
                        break;
                    }
                    case DUP: {
                        if (stackJumpLocs.empty()) {
                            return false;
                        }
                        Op02WithProcessedDataAndRefs tmp = (Op02WithProcessedDataAndRefs)stackJumpLocs.pop();
                        stackJumpLocs.push(tmp);
                        stackJumpLocs.push(tmp);
                        break;
                    }
                    case SWAP: {
                        if (stackJumpLocs.size() < 2) {
                            return false;
                        }
                        Op02WithProcessedDataAndRefs tmp1 = (Op02WithProcessedDataAndRefs)stackJumpLocs.pop();
                        Op02WithProcessedDataAndRefs tmp2 = (Op02WithProcessedDataAndRefs)stackJumpLocs.pop();
                        stackJumpLocs.push(tmp1);
                        stackJumpLocs.push(tmp2);
                        break;
                    }
                    case RET: 
                    case RET_WIDE: {
                        int idx = currInstr.getInstr() == JVMInstr.RET ? currInstr.getInstrArgU1(0) : currInstr.getInstrArgShort(1);
                        afterThis = (Op02WithProcessedDataAndRefs)stackJumpLocLocals.get(idx);
                        if (afterThis != null) break;
                        return false;
                    }
                    default: {
                        return false;
                    }
                }
                processed.add(currInstr);
                if (afterThis != null) break block32;
                if (currInstr.targets.size() != 1) {
                    return false;
                }
                currInstr = currInstr.targets.get(0);
            } while (currInstr.sources.size() == 1);
            return false;
        }
        if (afterThis == start) {
            Op02WithProcessedDataAndRefs[] remaining = stackJumpLocs.toArray(new Op02WithProcessedDataAndRefs[stackJumpLocs.size()]);
            int remainIdx = 0;
            List<Op02WithProcessedDataAndRefs> canGoto = ListFactory.newList();
            int len = processed.size();
            for (int x = 1; x < len; ++x) {
                Op02WithProcessedDataAndRefs node = (Op02WithProcessedDataAndRefs)processed.get(x);
                if (Op02WithProcessedDataAndRefs.isJSR(node) && remainIdx < remaining.length && node == remaining[remainIdx]) {
                    ++remainIdx;
                    continue;
                }
                canGoto.add(node);
            }
            if (remainIdx != remaining.length) {
                return false;
            }
            for (Op02WithProcessedDataAndRefs remove : canGoto) {
                remove.instr = JVMInstr.GOTO;
            }
            int idxStart = ops.indexOf(start);
            Op02WithProcessedDataAndRefs afterStart = ops.get(idxStart + 1);
            start.instr = JVMInstr.GOTO;
            currInstr.instr = JVMInstr.GOTO;
            currInstr.addTarget(afterStart);
            afterStart.addSource(currInstr);
            return true;
        }
        return false;
    }

    private static void inlineReplaceJSR(Op02WithProcessedDataAndRefs jsrCall, List<Op02WithProcessedDataAndRefs> ops) {
        Op02WithProcessedDataAndRefs jsrTarget = jsrCall.getTargets().get(0);
        Op02WithProcessedDataAndRefs newGoto = new Op02WithProcessedDataAndRefs(JVMInstr.GOTO, null, jsrCall.getIndex().justAfter(), jsrCall.cp, null, -1, BytecodeLoc.NONE);
        jsrTarget.removeSource(jsrCall);
        jsrCall.removeTarget(jsrTarget);
        newGoto.addTarget(jsrTarget);
        newGoto.addSource(jsrCall);
        jsrCall.addTarget(newGoto);
        jsrTarget.addSource(newGoto);
        jsrCall.instr = JVMInstr.ACONST_NULL;
        int jsrIdx = ops.indexOf(jsrCall);
        ops.add(jsrIdx + 1, newGoto);
    }

    private static void inlineJSR(Op02WithProcessedDataAndRefs start, Set<Op02WithProcessedDataAndRefs> nodes, List<Op02WithProcessedDataAndRefs> ops) {
        List<Op02WithProcessedDataAndRefs> instrs = ListFactory.newList(nodes);
        Collections.sort(instrs, new Comparator<Op02WithProcessedDataAndRefs>(){

            @Override
            public int compare(Op02WithProcessedDataAndRefs o1, Op02WithProcessedDataAndRefs o2) {
                return o1.getIndex().compareTo(o2.getIndex());
            }
        });
        ops.removeAll(instrs);
        List<Op02WithProcessedDataAndRefs> sources = ListFactory.newList(start.getSources());
        Op02WithProcessedDataAndRefs newStart = new Op02WithProcessedDataAndRefs(JVMInstr.ACONST_NULL, null, start.getIndex().justBefore(), start.cp, null, -1, BytecodeLoc.NONE);
        instrs.add(0, newStart);
        start.getSources().clear();
        start.addSource(newStart);
        newStart.addTarget(start);
        for (Op02WithProcessedDataAndRefs source : sources) {
            source.removeTarget(start);
            List<Op02WithProcessedDataAndRefs> instrCopy = Op02WithProcessedDataAndRefs.copyBlock(instrs, source.getIndex());
            int idx = ops.indexOf(source) + 1;
            if (idx < ops.size()) {
                Op02WithProcessedDataAndRefs retTgt = ops.get(idx);
                for (Op02WithProcessedDataAndRefs op : instrCopy) {
                    if (!Op02WithProcessedDataAndRefs.isRET(op)) continue;
                    op.instr = JVMInstr.GOTO;
                    op.addTarget(retTgt);
                    retTgt.addSource(op);
                }
            }
            source.instr = JVMInstr.NOP;
            int sourceIdx = ops.indexOf(source);
            ops.addAll(sourceIdx + 1, instrCopy);
            Op02WithProcessedDataAndRefs blockStart = instrCopy.get(0);
            blockStart.addSource(source);
            source.addTarget(blockStart);
        }
    }

    private static List<Op02WithProcessedDataAndRefs> copyBlock(List<Op02WithProcessedDataAndRefs> orig, InstrIndex afterThis) {
        List<Op02WithProcessedDataAndRefs> output = ListFactory.newList(orig.size());
        Map<Op02WithProcessedDataAndRefs, Op02WithProcessedDataAndRefs> fromTo = MapFactory.newMap();
        for (Op02WithProcessedDataAndRefs in : orig) {
            Op02WithProcessedDataAndRefs copy = new Op02WithProcessedDataAndRefs(in);
            copy.index = afterThis = afterThis.justAfter();
            fromTo.put(in, copy);
            output.add(copy);
        }
        int len = orig.size();
        for (int x = 0; x < len; ++x) {
            Op02WithProcessedDataAndRefs in = orig.get(x);
            Op02WithProcessedDataAndRefs copy = output.get(x);
            copy.exceptionGroups = ListFactory.newList(in.exceptionGroups);
            copy.containedInTheseBlocks = ListFactory.newList(in.containedInTheseBlocks);
            copy.catchExceptionGroups = ListFactory.newList(in.catchExceptionGroups);
            Op02WithProcessedDataAndRefs.tieUpRelations(copy.getSources(), in.getSources(), fromTo);
            Op02WithProcessedDataAndRefs.tieUpRelations(copy.getTargets(), in.getTargets(), fromTo);
        }
        return output;
    }

    private static void tieUpRelations(List<Op02WithProcessedDataAndRefs> out, List<Op02WithProcessedDataAndRefs> in, Map<Op02WithProcessedDataAndRefs, Op02WithProcessedDataAndRefs> map) {
        out.clear();
        for (Op02WithProcessedDataAndRefs i : in) {
            Op02WithProcessedDataAndRefs mapped = map.get(i);
            if (mapped == null) {
                throw new ConfusedCFRException("Missing node tying up JSR block");
            }
            out.add(mapped);
        }
    }

    public static void replace(Op02WithProcessedDataAndRefs oldOp, Op02WithProcessedDataAndRefs newOp) {
        for (Op02WithProcessedDataAndRefs source : oldOp.sources) {
            source.replaceTarget(oldOp, newOp);
        }
        for (Op02WithProcessedDataAndRefs target : oldOp.targets) {
            target.replaceSource(oldOp, newOp);
        }
        newOp.targets.addAll(oldOp.targets);
        newOp.sources.addAll(oldOp.sources);
        oldOp.sources.clear();
        oldOp.targets.clear();
    }

    private static class ExceptionTempStatement
    implements Comparable<ExceptionTempStatement> {
        private final ExceptionGroup triggeringGroup;
        private final Op02WithProcessedDataAndRefs op;
        private final boolean isTry;

        private ExceptionTempStatement(ExceptionGroup triggeringGroup, Op02WithProcessedDataAndRefs op) {
            this.triggeringGroup = triggeringGroup;
            this.op = op;
            this.isTry = op.instr == JVMInstr.FAKE_TRY;
        }

        public Op02WithProcessedDataAndRefs getOp() {
            return this.op;
        }

        public boolean isTry() {
            return this.isTry;
        }

        @Override
        public int compareTo(ExceptionTempStatement other) {
            if (other == this) {
                return 0;
            }
            int startCompare = this.triggeringGroup.getBytecodeIndexFrom() - other.triggeringGroup.getBytecodeIndexFrom();
            if (startCompare != 0) {
                return startCompare;
            }
            int endCompare = this.triggeringGroup.getBytecodeIndexTo() - other.triggeringGroup.getBytecodeIndexTo();
            return 0 - endCompare;
        }

        public String toString() {
            return this.op.toString();
        }
    }

    private static class IdentFactory {
        int nextIdx = 0;

        private IdentFactory() {
        }

        Ident getNextIdent(int slot) {
            return new Ident(slot, this.nextIdx++);
        }
    }
}

