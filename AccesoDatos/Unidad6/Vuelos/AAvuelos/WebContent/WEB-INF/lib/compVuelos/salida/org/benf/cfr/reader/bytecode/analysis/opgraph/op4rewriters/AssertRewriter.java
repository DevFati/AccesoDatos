/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.AbstractMatchResultIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.CollectMatch;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchOneOf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.Matcher;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.ResetAfterTest;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InfiniteAssertRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.PreconditionAssertRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BoolOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NotOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SwitchExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssert;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionYield;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredSwitch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredThrow;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class AssertRewriter {
    private final ClassFile classFile;
    private StaticVariable assertionStatic = null;
    private final boolean switchExpressions;
    private InferredJavaType boolIjt = new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION);

    public AssertRewriter(ClassFile classFile, Options options) {
        this.classFile = classFile;
        this.switchExpressions = options.getOption(OptionsImpl.SWITCH_EXPRESSION, classFile.getClassFileVersion());
    }

    public void sugarAsserts(Method staticInit) {
        JavaRefTypeInstance nextClass;
        if (!staticInit.hasCodeAttribute()) {
            return;
        }
        List<StructuredStatement> statements = MiscStatementTools.linearise(staticInit.getAnalysis());
        if (statements == null) {
            return;
        }
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(statements);
        WildcardMatch wcm1 = new WildcardMatch();
        JavaTypeInstance topClassType = this.classFile.getClassType();
        InnerClassInfo innerClassInfo = topClassType.getInnerClassHereInfo();
        JavaTypeInstance classType = topClassType;
        while (innerClassInfo != InnerClassInfo.NOT && (nextClass = innerClassInfo.getOuterClass()) != null && !((Object)nextClass).equals(classType)) {
            classType = nextClass;
            innerClassInfo = classType.getInnerClassHereInfo();
        }
        ResetAfterTest m = new ResetAfterTest(wcm1, new CollectMatch("ass1", new StructuredAssignment(BytecodeLoc.NONE, wcm1.getStaticVariable("assertbool", topClassType, new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.TEST)), new NotOperation(BytecodeLoc.NONE, new BooleanExpression(wcm1.getMemberFunction("assertmeth", "desiredAssertionStatus", new Literal(TypedLiteral.getClass(classType))))))));
        AssertVarCollector matchResultCollector = new AssertVarCollector(wcm1);
        while (mi.hasNext()) {
            mi.advance();
            matchResultCollector.clear();
            if (!m.match(mi, (MatchResultCollector)matchResultCollector)) continue;
            if (matchResultCollector.matched()) break;
            mi.rewind1();
        }
        if (!matchResultCollector.matched()) {
            return;
        }
        this.assertionStatic = matchResultCollector.assertStatic;
        this.rewriteMethods();
    }

    private void rewriteMethods() {
        List<Method> methods = this.classFile.getMethods();
        WildcardMatch wcm1 = new WildcardMatch();
        Matcher<StructuredStatement> standardAssertMatcher = this.buildStandardAssertMatcher(wcm1);
        AssertUseCollector collector = new AssertUseCollector(wcm1);
        Matcher<StructuredStatement> switchAssertMatcher = this.buildSwitchAssertMatcher(wcm1);
        SwitchAssertUseCollector swcollector = new SwitchAssertUseCollector();
        for (Method method : methods) {
            Op04StructuredStatement top;
            if (!method.hasCodeAttribute() || (top = method.getAnalysis()) == null) continue;
            this.handlePreConditionedAsserts(top);
            this.handleInfiniteAsserts(top);
            List<StructuredStatement> statements = MiscStatementTools.linearise(top);
            if (statements == null) continue;
            MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(statements);
            while (mi.hasNext()) {
                mi.advance();
                if (!standardAssertMatcher.match(mi, collector)) continue;
                mi.rewind1();
            }
            if (!this.switchExpressions) continue;
            mi = new MatchIterator<StructuredStatement>(statements);
            while (mi.hasNext()) {
                mi.advance();
                if (!switchAssertMatcher.match(mi, swcollector)) continue;
                mi.rewind1();
            }
        }
    }

    private Matcher<StructuredStatement> buildSwitchAssertMatcher(WildcardMatch wcm1) {
        return new ResetAfterTest(wcm1, new CollectMatch("ass1", new MatchSequence(new BeginBlock(null), new StructuredIf(BytecodeLoc.NONE, new NotOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(this.assertionStatic))), null), new BeginBlock(null), new StructuredSwitch(BytecodeLoc.NONE, wcm1.getExpressionWildCard("switchExpression"), null, wcm1.getBlockIdentifier("switchblock")))));
    }

    private Matcher<StructuredStatement> buildStandardAssertMatcher(WildcardMatch wcm1) {
        return new ResetAfterTest(wcm1, new MatchOneOf(new CollectMatch("ass1", new MatchSequence(new StructuredIf(BytecodeLoc.NONE, new BooleanOperation(BytecodeLoc.NONE, new NotOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(this.assertionStatic))), wcm1.getConditionalExpressionWildcard("condition"), BoolOp.AND), null), new BeginBlock(null), new StructuredThrow(BytecodeLoc.NONE, wcm1.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR)), new EndBlock(null))), new CollectMatch("ass1b", new MatchSequence(new StructuredIf(BytecodeLoc.NONE, new NotOperation(BytecodeLoc.NONE, new BooleanOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(this.assertionStatic)), wcm1.getConditionalExpressionWildcard("condition"), BoolOp.OR)), null), new BeginBlock(null), new StructuredThrow(BytecodeLoc.NONE, wcm1.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR)), new EndBlock(null))), new CollectMatch("ass1c", new MatchSequence(new StructuredIf(BytecodeLoc.NONE, new NotOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(this.assertionStatic))), null), new BeginBlock(null), new StructuredIf(BytecodeLoc.NONE, wcm1.getConditionalExpressionWildcard("condition"), null), new BeginBlock(null), new StructuredThrow(BytecodeLoc.NONE, wcm1.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR)), new EndBlock(null), new EndBlock(null))), new CollectMatch("ass2", new MatchSequence(new MatchOneOf(new StructuredIf(BytecodeLoc.NONE, new BooleanOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(this.assertionStatic)), wcm1.getConditionalExpressionWildcard("condition2"), BoolOp.OR), null), new StructuredIf(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(this.assertionStatic)), null)), new BeginBlock(wcm1.getBlockWildcard("condBlock")), new MatchOneOf(new StructuredReturn(BytecodeLoc.NONE, null, null), new StructuredReturn(BytecodeLoc.NONE, wcm1.getExpressionWildCard("retval"), null), new StructuredBreak(BytecodeLoc.NONE, wcm1.getBlockIdentifier("breakblock"), false)), new EndBlock(wcm1.getBlockWildcard("condBlock")), new CollectMatch("ass2throw", new StructuredThrow(BytecodeLoc.NONE, wcm1.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR))))), new CollectMatch("assonly", new MatchSequence(new StructuredIf(BytecodeLoc.NONE, new NotOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(this.assertionStatic))), null), new BeginBlock(null), new StructuredThrow(BytecodeLoc.NONE, wcm1.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR)), new EndBlock(null)))));
    }

    private void handlePreConditionedAsserts(Op04StructuredStatement statements) {
        PreconditionAssertRewriter rewriter = new PreconditionAssertRewriter(this.assertionStatic);
        rewriter.transform(statements);
    }

    private void handleInfiniteAsserts(Op04StructuredStatement statements) {
        InfiniteAssertRewriter rewriter = new InfiniteAssertRewriter(this.assertionStatic);
        rewriter.transform(statements);
    }

    private class AssertUseCollector
    extends AbstractMatchResultIterator {
        private StructuredStatement ass2throw;
        private final WildcardMatch wcm;

        private AssertUseCollector(WildcardMatch wcm) {
            this.wcm = wcm;
        }

        @Override
        public void clear() {
            this.ass2throw = null;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            Expression arg;
            WildcardMatch.ConstructorInvokationSimpleWildcard constructor = this.wcm.getConstructorSimpleWildcard("exception");
            List<Expression> args = constructor.getMatch().getArgs();
            Expression expression = arg = args.size() > 0 ? args.get(0) : null;
            if (arg != null && arg instanceof CastExpression && arg.getInferredJavaType().getJavaTypeInstance() == TypeConstants.OBJECT) {
                arg = ((CastExpression)arg).getChild();
            }
            if (name.equals("ass1") || name.equals("ass1b") || name.equals("ass1c")) {
                StructuredIf ifStatement = (StructuredIf)statement;
                ConditionalExpression condition = this.wcm.getConditionalExpressionWildcard("condition").getMatch();
                if (name.equals("ass1") || name.equals("ass1c")) {
                    condition = new NotOperation(BytecodeLoc.TODO, condition);
                }
                condition = condition.simplify();
                StructuredStatement structuredAssert = ifStatement.convertToAssertion(StructuredAssert.mkStructuredAssert(BytecodeLoc.TODO, condition, arg));
                ifStatement.getContainer().replaceStatement(structuredAssert);
            } else if (name.equals("ass2")) {
                if (this.ass2throw == null) {
                    throw new IllegalStateException();
                }
                StructuredIf ifStatement = (StructuredIf)statement;
                WildcardMatch.ConditionalExpressionWildcard wcard = this.wcm.getConditionalExpressionWildcard("condition2");
                ConditionalExpression conditionalExpression = wcard.getMatch();
                if (conditionalExpression == null) {
                    conditionalExpression = new BooleanExpression(new Literal(TypedLiteral.getBoolean(0)));
                }
                StructuredAssert structuredAssert = StructuredAssert.mkStructuredAssert(BytecodeLoc.TODO, conditionalExpression, arg);
                ifStatement.getContainer().replaceStatement(structuredAssert);
                this.ass2throw.getContainer().replaceStatement(ifStatement.getIfTaken().getStatement());
            } else if (name.equals("ass2throw")) {
                this.ass2throw = statement;
            } else if (name.equals("assonly")) {
                StructuredIf ifStatement = (StructuredIf)statement;
                StructuredStatement structuredAssert = ifStatement.convertToAssertion(StructuredAssert.mkStructuredAssert(BytecodeLoc.TODO, new BooleanExpression(Literal.FALSE), arg));
                ifStatement.getContainer().replaceStatement(structuredAssert);
            }
        }
    }

    static class ControlFlowSwitchExpressionTransformer
    implements StructuredStatementTransformer {
        private Map<Op04StructuredStatement, StructuredExpressionYield> replacements;
        protected boolean failed;
        int totalStatements;
        Expression single;
        int trueFound = 0;
        int falseFound = 0;
        private BlockIdentifier trueBlock;
        private BlockIdentifier falseBlock;

        private ControlFlowSwitchExpressionTransformer(BlockIdentifier trueBlock, BlockIdentifier falseBlock, Map<Op04StructuredStatement, StructuredExpressionYield> replacements) {
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            this.replacements = replacements;
        }

        void additionalHandling(StructuredStatement in) {
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            if (this.failed) {
                return in;
            }
            if (in.isEffectivelyNOP()) {
                return in;
            }
            if (!(in instanceof Block)) {
                StructuredExpressionYield y = this.replacements.get(in.getContainer());
                if (y == null) {
                    ++this.totalStatements;
                } else {
                    this.single = y.getValue();
                }
            }
            if (in instanceof StructuredBreak) {
                BreakClassification bk = this.classifyBreak((StructuredBreak)in, scope);
                switch (bk) {
                    case TRUE_BLOCK: {
                        --this.totalStatements;
                        ++this.trueFound;
                        this.single = Literal.TRUE;
                        this.replacements.put(in.getContainer(), new StructuredExpressionYield(BytecodeLoc.NONE, this.single));
                        return in;
                    }
                    case FALSE_BLOCK: {
                        --this.totalStatements;
                        ++this.falseFound;
                        this.single = Literal.FALSE;
                        this.replacements.put(in.getContainer(), new StructuredExpressionYield(BytecodeLoc.NONE, this.single));
                        return in;
                    }
                    case INNER: {
                        break;
                    }
                    case TOO_FAR: {
                        this.failed = true;
                        return in;
                    }
                }
            }
            if (in instanceof StructuredReturn) {
                this.failed = true;
                return in;
            }
            this.additionalHandling(in);
            in.transformStructuredChildren(this, scope);
            return in;
        }

        BreakClassification classifyBreak(StructuredBreak in, StructuredScope scope) {
            BlockIdentifier breakBlock = in.getBreakBlock();
            if (breakBlock == this.trueBlock) {
                return BreakClassification.TRUE_BLOCK;
            }
            if (breakBlock == this.falseBlock) {
                return BreakClassification.FALSE_BLOCK;
            }
            for (StructuredStatement stm : scope.getAll()) {
                BlockIdentifier block = stm.getBreakableBlockOrNull();
                if (block != breakBlock) continue;
                return BreakClassification.INNER;
            }
            return BreakClassification.TOO_FAR;
        }

        static enum BreakClassification {
            TRUE_BLOCK,
            FALSE_BLOCK,
            TOO_FAR,
            INNER;

        }
    }

    static class AssertionTrackingControlFlowSwitchExpressionTransformer
    extends ControlFlowSwitchExpressionTransformer {
        List<StructuredStatement> throwSS = ListFactory.newList();

        AssertionTrackingControlFlowSwitchExpressionTransformer(BlockIdentifier trueBlock, BlockIdentifier falseBlock, Map<Op04StructuredStatement, StructuredExpressionYield> replacements) {
            super(trueBlock, falseBlock, replacements);
        }

        @Override
        void additionalHandling(StructuredStatement in) {
            if (in instanceof StructuredThrow && ((StructuredThrow)in).getValue().getInferredJavaType().getJavaTypeInstance().equals(TypeConstants.ASSERTION_ERROR)) {
                this.throwSS.add(in);
            }
        }
    }

    private class SwitchAssertUseCollector
    extends AbstractMatchResultIterator {
        private SwitchAssertUseCollector() {
        }

        @Override
        public void clear() {
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            if (!(statement instanceof BeginBlock)) {
                return;
            }
            Block block = ((BeginBlock)statement).getBlock();
            Pair<Boolean, Op04StructuredStatement> content = block.getOneStatementIfPresent();
            if (content.getFirst().booleanValue()) {
                return;
            }
            StructuredStatement ifStm = content.getSecond().getStatement();
            if (!(ifStm instanceof StructuredIf)) {
                return;
            }
            Op04StructuredStatement taken = ((StructuredIf)ifStm).getIfTaken();
            StructuredStatement takenBody = taken.getStatement();
            if (takenBody.getClass() != Block.class) {
                return;
            }
            Block takenBlock = (Block)takenBody;
            List<Op04StructuredStatement> switchAndThrow = takenBlock.getFilteredBlockStatements();
            if (switchAndThrow.isEmpty()) {
                return;
            }
            BlockIdentifier outerBlock = block.getBreakableBlockOrNull();
            StructuredStatement switchS = switchAndThrow.get(0).getStatement();
            if (!(switchS instanceof StructuredSwitch)) {
                return;
            }
            StructuredSwitch struSwi = (StructuredSwitch)switchS;
            BlockIdentifier switchBlock = struSwi.getBlockIdentifier();
            Op04StructuredStatement swBody = struSwi.getBody();
            if (!(swBody.getStatement() instanceof Block)) {
                return;
            }
            Block swBodyBlock = (Block)swBody.getStatement();
            if (switchAndThrow.size() > 2) {
                if ((switchAndThrow = this.tryCombineSwitch(switchAndThrow, outerBlock, switchBlock, swBodyBlock)).size() != 1) {
                    return;
                }
                takenBlock.replaceBlockStatements(switchAndThrow);
            }
            StructuredStatement newAssert = null;
            switch (switchAndThrow.size()) {
                case 1: {
                    newAssert = this.processSwitchEmbeddedThrow(ifStm, outerBlock, swBodyBlock, swBody, struSwi);
                    break;
                }
                case 2: {
                    newAssert = this.processSwitchAndThrow(ifStm, outerBlock, switchBlock, swBodyBlock, struSwi, switchAndThrow.get(1));
                    break;
                }
            }
            if (newAssert != null) {
                content.getSecond().replaceStatement(newAssert);
            }
        }

        private List<Op04StructuredStatement> tryCombineSwitch(List<Op04StructuredStatement> content, BlockIdentifier outer, BlockIdentifier swiBlockIdentifier, Block swBodyBlock) {
            Map replacements = MapFactory.newOrderedMap();
            ControlFlowSwitchExpressionTransformer cfset = new ControlFlowSwitchExpressionTransformer(outer, swiBlockIdentifier, replacements);
            content.get(0).transform(cfset, new StructuredScope());
            if (cfset.failed) {
                return content;
            }
            if (cfset.falseFound != 0) {
                return content;
            }
            List<Op04StructuredStatement> cases = swBodyBlock.getFilteredBlockStatements();
            Op04StructuredStatement lastCase = cases.get(cases.size() - 1);
            StructuredStatement ss = lastCase.getStatement();
            if (!(ss instanceof StructuredCase)) {
                return content;
            }
            Op04StructuredStatement body = ((StructuredCase)ss).getBody();
            StructuredStatement bodySS = body.getStatement();
            Block block = bodySS instanceof Block ? (Block)bodySS : new Block(body);
            block.setIndenting(true);
            block.getBlockStatements().addAll(content.subList(1, content.size()));
            body.replaceStatement(block);
            return ListFactory.newList(content.get(0));
        }

        private Pair<Boolean, Expression> getThrowExpression(StructuredStatement throwS) {
            WildcardMatch wcm2 = new WildcardMatch();
            WildcardMatch.ConstructorInvokationSimpleWildcard constructor = wcm2.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR);
            StructuredThrow test = new StructuredThrow(BytecodeLoc.TODO, constructor);
            if (!((Object)test).equals(throwS)) {
                return Pair.make(false, null);
            }
            Expression exceptArg = null;
            List<Expression> consArg = constructor.getMatch().getArgs();
            if (consArg.size() == 1) {
                exceptArg = consArg.get(0);
            } else if (consArg.size() > 1) {
                return Pair.make(false, null);
            }
            return Pair.make(true, exceptArg);
        }

        private StructuredStatement processSwitchAndThrow(StructuredStatement ifStm, BlockIdentifier outer, BlockIdentifier swiBlockIdentifier, Block swBodyBlock, StructuredSwitch struSwi, Op04StructuredStatement throwStm) {
            Map<Op04StructuredStatement, StructuredExpressionYield> replacements;
            Pair<Boolean, Expression> excepTest = this.getThrowExpression(throwStm.getStatement());
            if (!excepTest.getFirst().booleanValue()) {
                return null;
            }
            Expression exceptArg = excepTest.getSecond();
            List<SwitchExpression.Branch> branches = ListFactory.newList();
            if (!this.getBranches(outer, swiBlockIdentifier, swBodyBlock, branches, replacements = MapFactory.newOrderedMap(), false)) {
                return null;
            }
            SwitchExpression sw = new SwitchExpression(BytecodeLoc.TODO, AssertRewriter.this.boolIjt, struSwi.getSwitchOn(), branches);
            return ((StructuredIf)ifStm).convertToAssertion(StructuredAssert.mkStructuredAssert(BytecodeLoc.TODO, new BooleanExpression(sw), exceptArg));
        }

        private boolean getBranches(BlockIdentifier outer, BlockIdentifier swiBlockIdentifier, Block swBodyBlock, List<SwitchExpression.Branch> branches, Map<Op04StructuredStatement, StructuredExpressionYield> replacements, boolean addYieldTrue) {
            for (Op04StructuredStatement op04StructuredStatement : swBodyBlock.getBlockStatements()) {
                SwitchExpression.Branch branch = this.getBranch(outer, swiBlockIdentifier, replacements, op04StructuredStatement, addYieldTrue);
                if (branch == null) {
                    return false;
                }
                branches.add(branch);
            }
            for (Map.Entry entry : replacements.entrySet()) {
                StructuredBreak sb;
                Op04StructuredStatement first = (Op04StructuredStatement)entry.getKey();
                StructuredStatement statement = first.getStatement();
                if (statement instanceof StructuredBreak && !(sb = (StructuredBreak)statement).isLocalBreak()) {
                    sb.getBreakBlock().releaseForeignRef();
                }
                first.replaceStatement((StructuredStatement)entry.getValue());
            }
            return true;
        }

        private SwitchExpression.Branch getBranch(BlockIdentifier outer, BlockIdentifier swiBlockIdentifier, Map<Op04StructuredStatement, StructuredExpressionYield> replacements, Op04StructuredStatement statement, boolean addYieldTrue) {
            StructuredStatement stm;
            StructuredStatement cstm = statement.getStatement();
            if (!(cstm instanceof StructuredCase)) {
                return null;
            }
            StructuredCase caseStm = (StructuredCase)cstm;
            ControlFlowSwitchExpressionTransformer cfset = new ControlFlowSwitchExpressionTransformer(outer, swiBlockIdentifier, replacements);
            Op04StructuredStatement body = caseStm.getBody();
            body.transform(cfset, new StructuredScope());
            if (cfset.failed) {
                return null;
            }
            if (addYieldTrue && (stm = body.getStatement()) instanceof Block) {
                Block block = (Block)stm;
                Op04StructuredStatement last = block.getLast();
                StructuredStatement lastStm = replacements.get(last);
                if (lastStm == null) {
                    lastStm = last.getStatement();
                }
                if (!(lastStm instanceof StructuredExpressionYield)) {
                    ++cfset.totalStatements;
                    block.getBlockStatements().add(new Op04StructuredStatement(new StructuredExpressionYield(BytecodeLoc.TODO, Literal.TRUE)));
                }
            }
            Expression value = cfset.totalStatements == 0 ? cfset.single : new StructuredStatementExpression(AssertRewriter.this.boolIjt, body.getStatement());
            return new SwitchExpression.Branch(caseStm.getValues(), value);
        }

        private StructuredStatement processSwitchEmbeddedThrow(StructuredStatement ifStm, BlockIdentifier outer, Block swBodyBlock, Op04StructuredStatement switchStm, StructuredSwitch struSwi) {
            List<SwitchExpression.Branch> branches;
            Map<Op04StructuredStatement, StructuredExpressionYield> replacements = MapFactory.newOrderedMap();
            BlockIdentifier swiBlockIdentifier = struSwi.getBlockIdentifier();
            AssertionTrackingControlFlowSwitchExpressionTransformer track = new AssertionTrackingControlFlowSwitchExpressionTransformer(swiBlockIdentifier, outer, replacements);
            switchStm.transform(track, new StructuredScope());
            if (track.failed) {
                return null;
            }
            if (track.throwSS.size() > 1) {
                return null;
            }
            replacements.clear();
            Expression exceptArg = null;
            if (track.throwSS.size() == 1) {
                StructuredStatement throwStm = track.throwSS.get(0);
                Pair<Boolean, Expression> excepTest = this.getThrowExpression(throwStm);
                if (!excepTest.getFirst().booleanValue()) {
                    return null;
                }
                exceptArg = excepTest.getSecond();
                replacements.put(throwStm.getContainer(), new StructuredExpressionYield(BytecodeLoc.TODO, Literal.FALSE));
            }
            if (!this.getBranches(swiBlockIdentifier, swiBlockIdentifier, swBodyBlock, branches = ListFactory.newList(), replacements, true)) {
                return null;
            }
            SwitchExpression sw = new SwitchExpression(BytecodeLoc.TODO, AssertRewriter.this.boolIjt, struSwi.getSwitchOn(), branches);
            return ((StructuredIf)ifStm).convertToAssertion(StructuredAssert.mkStructuredAssert(BytecodeLoc.TODO, new BooleanExpression(sw), exceptArg));
        }
    }

    private class AssertVarCollector
    extends AbstractMatchResultIterator {
        private final WildcardMatch wcm;
        ClassFileField assertField = null;
        StaticVariable assertStatic = null;

        private AssertVarCollector(WildcardMatch wcm) {
            this.wcm = wcm;
        }

        @Override
        public void clear() {
            this.assertField = null;
            this.assertStatic = null;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            ClassFileField field;
            StaticVariable staticVariable = this.wcm.getStaticVariable("assertbool").getMatch();
            try {
                field = AssertRewriter.this.classFile.getFieldByName(staticVariable.getFieldName(), staticVariable.getInferredJavaType().getJavaTypeInstance());
            }
            catch (NoSuchFieldException e) {
                return;
            }
            if (!field.getField().testAccessFlag(AccessFlag.ACC_SYNTHETIC)) {
                return;
            }
            this.assertField = field;
            statement.getContainer().nopOut();
            this.assertField.markHidden();
            this.assertStatic = staticVariable;
        }

        boolean matched() {
            return this.assertField != null;
        }
    }
}

