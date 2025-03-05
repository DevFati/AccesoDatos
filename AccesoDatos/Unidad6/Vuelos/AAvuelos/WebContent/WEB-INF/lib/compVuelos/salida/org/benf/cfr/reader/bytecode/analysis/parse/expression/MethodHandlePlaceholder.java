/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationExplicit;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokationExplicit;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokationExplicit;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionVisitor;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.QuotingUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredThrow;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredTry;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.FakeMethod;
import org.benf.cfr.reader.entities.bootstrap.MethodHandleBehaviour;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodHandle;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.output.Dumper;

public class MethodHandlePlaceholder
extends AbstractExpression {
    private ConstantPoolEntryMethodHandle handle;
    private FakeMethod fake;

    public MethodHandlePlaceholder(BytecodeLoc loc, ConstantPoolEntryMethodHandle handle) {
        super(loc, new InferredJavaType(TypeConstants.METHOD_HANDLE, InferredJavaType.Source.FUNCTION, true));
        this.handle = handle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof MethodHandlePlaceholder)) {
            return false;
        }
        return this.handle.equals(((MethodHandlePlaceholder)o).handle);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    @Override
    public Precedence getPrecedence() {
        return this.fake == null ? Precedence.WEAKEST : Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        if (this.fake == null) {
            d.print("/* method handle: ").dump(new Literal(TypedLiteral.getString(this.handle.getLiteralName()))).separator(" */ null");
        } else {
            d.methodName(this.fake.getName(), null, false, false).separator("(").separator(")");
        }
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public <T> T visit(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof MethodHandlePlaceholder)) {
            return false;
        }
        return constraint.equivalent(this.handle, ((MethodHandlePlaceholder)o).handle);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new MethodHandlePlaceholder(this.getLoc(), this.handle);
    }

    public FakeMethod addFakeMethod(ClassFile classFile) {
        this.fake = classFile.addFakeMethod(this.handle, "ldc", new UnaryFunction<String, FakeMethod>(){

            @Override
            public FakeMethod invoke(String name) {
                return MethodHandlePlaceholder.this.generateFake(name);
            }
        });
        return this.fake;
    }

    private FakeMethod generateFake(String name) {
        BlockIdentifier identifier = new BlockIdentifier(-1, BlockType.TRYBLOCK);
        StructuredTry trys = new StructuredTry(new Op04StructuredStatement(Block.getBlockFor(true, new StructuredReturn(BytecodeLoc.TODO, MethodHandlePlaceholder.from(this.handle), TypeConstants.METHOD_HANDLE))), identifier);
        LocalVariable caught = new LocalVariable("except", new InferredJavaType(TypeConstants.THROWABLE, InferredJavaType.Source.EXPRESSION));
        List<JavaRefTypeInstance> catchTypes = ListFactory.newList(TypeConstants.NOSUCHMETHOD_EXCEPTION, TypeConstants.ILLEGALACCESS_EXCEPTION);
        StructuredCatch catche = new StructuredCatch(catchTypes, new Op04StructuredStatement(Block.getBlockFor(true, new StructuredThrow(BytecodeLoc.TODO, new ConstructorInvokationExplicit(this.getLoc(), new InferredJavaType(TypeConstants.ILLEGALARGUMENT_EXCEPTION, InferredJavaType.Source.CONSTRUCTOR), TypeConstants.ILLEGALARGUMENT_EXCEPTION, ListFactory.newList(new LValueExpression(caught)))))), caught, Collections.singleton(identifier));
        trys.getCatchBlocks().add(new Op04StructuredStatement(catche));
        Op04StructuredStatement stm = new Op04StructuredStatement(Block.getBlockFor(true, trys));
        DecompilerComments comments = new DecompilerComments();
        comments.addComment("Works around MethodHandle LDC.");
        return new FakeMethod(name, EnumSet.of(AccessFlagMethod.ACC_STATIC), TypeConstants.METHOD_HANDLE, stm, comments);
    }

    private static Expression from(ConstantPoolEntryMethodHandle cpe) {
        StaticFunctionInvokationExplicit lookup = new StaticFunctionInvokationExplicit(BytecodeLoc.TODO, new InferredJavaType(TypeConstants.METHOD_HANDLES$LOOKUP, InferredJavaType.Source.EXPRESSION), TypeConstants.METHOD_HANDLES, "lookup", Collections.<Expression>emptyList());
        String behaviourName = MethodHandlePlaceholder.lookupFunction(cpe.getReferenceKind());
        ConstantPoolEntryMethodRef ref = cpe.getMethodRef();
        MethodPrototype refProto = ref.getMethodPrototype();
        String descriptor = ref.getNameAndTypeEntry().getDescriptor().getValue();
        return new MemberFunctionInvokationExplicit(BytecodeLoc.TODO, new InferredJavaType(TypeConstants.METHOD_HANDLE, InferredJavaType.Source.EXPRESSION), TypeConstants.METHOD_HANDLES$LOOKUP, lookup, behaviourName, ListFactory.newList(new Literal(TypedLiteral.getClass(refProto.getClassType())), new Literal(TypedLiteral.getString(QuotingUtils.addQuotes(refProto.getName(), false))), MethodHandlePlaceholder.getMethodType(new Literal(TypedLiteral.getString(QuotingUtils.enquoteString(descriptor))))));
    }

    private static String lookupFunction(MethodHandleBehaviour behaviour) {
        switch (behaviour) {
            case GET_FIELD: {
                return "findGetter";
            }
            case GET_STATIC: {
                return "findStaticGetter";
            }
            case PUT_FIELD: {
                return "findSetter";
            }
            case PUT_STATIC: {
                return "findStaticSetter";
            }
            case INVOKE_VIRTUAL: 
            case INVOKE_INTERFACE: {
                return "findVirtual";
            }
            case INVOKE_STATIC: {
                return "findStatic";
            }
            case INVOKE_SPECIAL: 
            case NEW_INVOKE_SPECIAL: {
                return "findSpecial";
            }
        }
        throw new ConfusedCFRException("Unknown method handle behaviour.");
    }

    public static Expression getMethodType(Expression descriptorString) {
        return new StaticFunctionInvokationExplicit(BytecodeLoc.TODO, new InferredJavaType(TypeConstants.METHOD_TYPE, InferredJavaType.Source.EXPRESSION), TypeConstants.METHOD_TYPE, "fromMethodDescriptorString", Arrays.asList(descriptorString, new Literal(TypedLiteral.getNull())));
    }
}

