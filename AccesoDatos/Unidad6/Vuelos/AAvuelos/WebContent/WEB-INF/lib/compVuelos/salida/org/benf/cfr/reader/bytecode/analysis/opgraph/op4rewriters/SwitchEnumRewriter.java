/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.Op04Rewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.AbstractMatchResultIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.CollectMatch;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.KleeneStar;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchOneOf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.ResetAfterTest;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArrayIndex;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArrayLength;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ComparisonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewPrimitiveArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.ArrayVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredSwitch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredTry;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Field;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class SwitchEnumRewriter
implements Op04Rewriter {
    private final DCCommonState dcCommonState;
    private final ClassFile classFile;
    private final ClassFileVersion classFileVersion;
    private final BlockIdentifierFactory blockIdentifierFactory;
    private static final JavaTypeInstance expectedLUTType = new JavaArrayTypeInstance(1, RawJavaType.INT);

    public SwitchEnumRewriter(DCCommonState dcCommonState, ClassFile classFile, BlockIdentifierFactory blockIdentifierFactory) {
        this.dcCommonState = dcCommonState;
        this.classFile = classFile;
        this.classFileVersion = classFile.getClassFileVersion();
        this.blockIdentifierFactory = blockIdentifierFactory;
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        List<StructuredStatement> expressionStatements;
        Options options = this.dcCommonState.getOptions();
        if (!options.getOption(OptionsImpl.ENUM_SWITCH, this.classFileVersion).booleanValue()) {
            return;
        }
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) {
            return;
        }
        List<StructuredStatement> switchStatements = Functional.filter(structuredStatements, new Predicate<StructuredStatement>(){

            @Override
            public boolean test(StructuredStatement in) {
                return in.getClass() == StructuredSwitch.class;
            }
        });
        WildcardMatch wcm = new WildcardMatch();
        if (!switchStatements.isEmpty()) {
            MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(switchStatements);
            ResetAfterTest m = new ResetAfterTest(wcm, new CollectMatch("switch", new StructuredSwitch(BytecodeLoc.NONE, new ArrayIndex(BytecodeLoc.NONE, wcm.getExpressionWildCard("lut"), wcm.getMemberFunction("fncall", "ordinal", wcm.getExpressionWildCard("object"))), null, wcm.getBlockIdentifier("block"))));
            SwitchEnumMatchResultCollector matchResultCollector = new SwitchEnumMatchResultCollector();
            while (mi.hasNext()) {
                mi.advance();
                matchResultCollector.clear();
                if (!m.match(mi, (MatchResultCollector)matchResultCollector)) continue;
                this.tryRewrite(matchResultCollector, false);
                mi.rewind1();
            }
        }
        if (!(expressionStatements = Functional.filter(structuredStatements, new Predicate<StructuredStatement>(){

            @Override
            public boolean test(StructuredStatement in) {
                return in.getClass() == StructuredExpressionStatement.class;
            }
        })).isEmpty()) {
            ResetAfterTest mInline = new ResetAfterTest(wcm, new CollectMatch("bodylessswitch", new StructuredExpressionStatement(BytecodeLoc.NONE, new ArrayIndex(BytecodeLoc.NONE, wcm.getExpressionWildCard("lut"), wcm.getMemberFunction("fncall", "ordinal", wcm.getExpressionWildCard("object"))), true)));
            MatchIterator<StructuredStatement> mi2 = new MatchIterator<StructuredStatement>(expressionStatements);
            SwitchEnumMatchResultCollector matchResultCollector2 = new SwitchEnumMatchResultCollector();
            while (mi2.hasNext()) {
                mi2.advance();
                matchResultCollector2.clear();
                if (!mInline.match(mi2, (MatchResultCollector)matchResultCollector2)) continue;
                this.tryRewrite(matchResultCollector2, true);
                mi2.rewind1();
            }
        }
    }

    private void tryRewrite(SwitchEnumMatchResultCollector mrc, boolean expression) {
        Expression lookup = mrc.getLookupTable();
        if (lookup instanceof LValueExpression) {
            this.tryRewriteJavac(mrc, ((LValueExpression)lookup).getLValue(), expression);
        }
        if (lookup instanceof StaticFunctionInvokation) {
            this.tryRewriteEclipse(mrc, (StaticFunctionInvokation)lookup, expression);
        }
    }

    private void tryRewriteEclipse(SwitchEnumMatchResultCollector mrc, StaticFunctionInvokation lookupFn, boolean expression) {
        Expression enumObject = mrc.getEnumObject();
        Literal lv = enumObject.getComputedLiteral(MapFactory.<LValue, Literal>newMap());
        boolean isNull = Literal.NULL.equals(lv);
        if (lookupFn.getClazz() != this.classFile.getClassType()) {
            return;
        }
        if (!lookupFn.getArgs().isEmpty()) {
            return;
        }
        Method meth = null;
        try {
            List<Method> methods = this.classFile.getMethodByName(lookupFn.getName());
            if (methods.size() == 1 && !(meth = methods.get(0)).getMethodPrototype().getArgs().isEmpty()) {
                meth = null;
            }
        }
        catch (NoSuchMethodException methods) {
            // empty catch block
        }
        if (meth == null) {
            return;
        }
        if (!meth.testAccessFlag(AccessFlagMethod.ACC_SYNTHETIC) || !meth.testAccessFlag(AccessFlagMethod.ACC_STATIC)) {
            return;
        }
        MethodPrototype methodPrototype = meth.getMethodPrototype();
        if (!methodPrototype.getReturnType().equals(expectedLUTType)) {
            return;
        }
        List<StructuredStatement> structuredStatements = this.getLookupMethodStatements(meth);
        if (structuredStatements == null) {
            return;
        }
        WildcardMatch wcm1 = new WildcardMatch();
        JavaTypeInstance enumType = isNull ? null : enumObject.getInferredJavaType().getJavaTypeInstance();
        ResetAfterTest test = new ResetAfterTest(wcm1, new MatchSequence(new MatchOneOf(new MatchSequence(new StructuredAssignment(BytecodeLoc.NONE, wcm1.getLValueWildCard("res"), new LValueExpression(wcm1.getLValueWildCard("static"))), new StructuredIf(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.TODO, new LValueExpression(wcm1.getLValueWildCard("res")), Literal.NULL, CompOp.NE), null), new BeginBlock(null), new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm1.getLValueWildCard("res")), null), new EndBlock(null)), new MatchSequence(new StructuredIf(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.TODO, new LValueExpression(wcm1.getLValueWildCard("static")), Literal.NULL, CompOp.NE), null), new BeginBlock(null), new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm1.getLValueWildCard("static")), null), new EndBlock(null))), new StructuredAssignment(BytecodeLoc.NONE, wcm1.getLValueWildCard("lookup"), new NewPrimitiveArray(BytecodeLoc.TODO, (Expression)new ArrayLength(BytecodeLoc.NONE, wcm1.getStaticFunction("func", enumType, null, "values")), RawJavaType.INT))));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);
        boolean matched = false;
        EclipseVarResultCollector assignment = new EclipseVarResultCollector();
        while (mi.hasNext()) {
            mi.advance();
            assignment.clear();
            if (!test.match(mi, (MatchResultCollector)assignment)) continue;
            matched = true;
            break;
        }
        if (!matched) {
            return;
        }
        LValue fieldLv = assignment.field;
        if (!(fieldLv instanceof StaticVariable)) {
            return;
        }
        StaticVariable sv = (StaticVariable)fieldLv;
        ClassFileField fieldvar = sv.getClassFileField();
        Field field = fieldvar.getField();
        if (!field.testAccessFlag(AccessFlag.ACC_SYNTHETIC) || !field.testAccessFlag(AccessFlag.ACC_STATIC)) {
            return;
        }
        if (enumType == null) {
            enumType = assignment.arrayLen.getClazz();
            enumObject = new CastExpression(BytecodeLoc.NONE, new InferredJavaType(enumType, InferredJavaType.Source.TRANSFORM), enumObject);
        }
        WildcardMatch wcm2 = new WildcardMatch();
        ResetAfterTest m = new ResetAfterTest(wcm1, new MatchSequence(new StructuredAssignment(BytecodeLoc.NONE, assignment.lookup, new NewPrimitiveArray(BytecodeLoc.TODO, (Expression)new ArrayLength(BytecodeLoc.NONE, wcm1.getStaticFunction("func", enumType, null, "values")), RawJavaType.INT)), this.getEnumSugarKleeneStar(assignment.lookup, enumObject, wcm2)));
        SwitchForeignEnumMatchResultCollector matchResultCollector = new SwitchForeignEnumMatchResultCollector(wcm2);
        matched = false;
        mi.rewind();
        while (mi.hasNext()) {
            mi.advance();
            matchResultCollector.clear();
            if (!m.match(mi, (MatchResultCollector)matchResultCollector)) continue;
            matched = true;
            break;
        }
        if (!matched) {
            return;
        }
        if (this.replaceIndexedSwitch(mrc, expression, enumObject, matchResultCollector)) {
            return;
        }
        fieldvar.markHidden();
        meth.hideSynthetic();
    }

    private void tryRewriteJavac(SwitchEnumMatchResultCollector mrc, LValue lookupTable, boolean expression) {
        Method lutStaticInit;
        Field lut;
        ClassFile enumLutClass;
        Expression enumObject = mrc.getEnumObject();
        if (!(lookupTable instanceof StaticVariable)) {
            return;
        }
        StaticVariable staticLookupTable = (StaticVariable)lookupTable;
        JavaTypeInstance classInfo = staticLookupTable.getOwningClassType();
        String varName = staticLookupTable.getFieldName();
        try {
            enumLutClass = this.dcCommonState.getClassFile(classInfo);
        }
        catch (CannotLoadClassException e) {
            return;
        }
        try {
            lut = enumLutClass.getFieldByName(varName, staticLookupTable.getInferredJavaType().getJavaTypeInstance()).getField();
        }
        catch (NoSuchFieldException e) {
            return;
        }
        JavaTypeInstance fieldType = lut.getJavaTypeInstance();
        if (!fieldType.equals(expectedLUTType)) {
            return;
        }
        try {
            lutStaticInit = enumLutClass.getMethodByName("<clinit>").get(0);
        }
        catch (NoSuchMethodException e) {
            return;
        }
        List<StructuredStatement> structuredStatements = this.getLookupMethodStatements(lutStaticInit);
        if (structuredStatements == null) {
            return;
        }
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);
        WildcardMatch wcm1 = new WildcardMatch();
        WildcardMatch wcm2 = new WildcardMatch();
        ResetAfterTest m = new ResetAfterTest(wcm1, new MatchSequence(new StructuredAssignment(BytecodeLoc.NONE, lookupTable, new NewPrimitiveArray(BytecodeLoc.TODO, (Expression)new ArrayLength(BytecodeLoc.NONE, wcm1.getStaticFunction("func", enumObject.getInferredJavaType().getJavaTypeInstance(), null, "values")), RawJavaType.INT)), this.getEnumSugarKleeneStar(lookupTable, enumObject, wcm2)));
        SwitchForeignEnumMatchResultCollector matchResultCollector = new SwitchForeignEnumMatchResultCollector(wcm2);
        boolean matched = false;
        while (mi.hasNext()) {
            mi.advance();
            matchResultCollector.clear();
            if (!m.match(mi, (MatchResultCollector)matchResultCollector)) continue;
            matched = true;
            break;
        }
        if (!matched) {
            return;
        }
        if (this.replaceIndexedSwitch(mrc, expression, enumObject, matchResultCollector)) {
            return;
        }
        enumLutClass.markHiddenInnerClass();
    }

    private boolean replaceIndexedSwitch(SwitchEnumMatchResultCollector mrc, boolean expression, Expression enumObject, SwitchForeignEnumMatchResultCollector matchResultCollector) {
        StructuredSwitch newSwitch;
        AbstractStructuredStatement structuredStatement;
        Map<Integer, StaticVariable> reverseLut = matchResultCollector.getLUT();
        if (!expression) {
            StructuredSwitch structuredSwitch = mrc.getStructuredSwitch();
            structuredStatement = structuredSwitch;
            Op04StructuredStatement switchBlock = structuredSwitch.getBody();
            StructuredStatement switchBlockStatement = switchBlock.getStatement();
            if (!(switchBlockStatement instanceof Block)) {
                throw new IllegalStateException("Inside switch should be a block");
            }
            Block block = (Block)switchBlockStatement;
            List<Op04StructuredStatement> caseStatements = block.getBlockStatements();
            LinkedList<Op04StructuredStatement> newBlockContent = ListFactory.newLinkedList();
            InferredJavaType inferredJavaType = enumObject.getInferredJavaType();
            for (Op04StructuredStatement caseOuter : caseStatements) {
                StructuredStatement caseInner = caseOuter.getStatement();
                if (!(caseInner instanceof StructuredCase)) {
                    return true;
                }
                StructuredCase caseStmt = (StructuredCase)caseInner;
                List<Expression> values = caseStmt.getValues();
                List<Expression> newValues = ListFactory.newList();
                for (Expression value : values) {
                    Integer iVal = this.getIntegerFromLiteralExpression(value);
                    if (iVal == null) {
                        return true;
                    }
                    StaticVariable enumVal = reverseLut.get(iVal);
                    if (enumVal == null) {
                        return true;
                    }
                    newValues.add(new LValueExpression(enumVal));
                }
                StructuredCase replacement = new StructuredCase(BytecodeLoc.TODO, newValues, inferredJavaType, caseStmt.getBody(), caseStmt.getBlockIdentifier(), true);
                newBlockContent.add(new Op04StructuredStatement(replacement));
            }
            Block replacementBlock = new Block(newBlockContent, block.isIndenting());
            newSwitch = new StructuredSwitch(BytecodeLoc.TODO, enumObject, new Op04StructuredStatement(replacementBlock), structuredSwitch.getBlockIdentifier());
        } else {
            structuredStatement = mrc.getStructuredExpressionStatement();
            LinkedList<Op04StructuredStatement> tmp = new LinkedList<Op04StructuredStatement>();
            tmp.add(new Op04StructuredStatement(new StructuredComment("Empty switch")));
            newSwitch = new StructuredSwitch(BytecodeLoc.TODO, enumObject, new Op04StructuredStatement(new Block(tmp, true)), this.blockIdentifierFactory.getNextBlockIdentifier(BlockType.SWITCH));
        }
        structuredStatement.getContainer().replaceStatement(newSwitch);
        return false;
    }

    private KleeneStar getEnumSugarKleeneStar(LValue lookupTable, Expression enumObject, WildcardMatch wcm) {
        return new KleeneStar(new ResetAfterTest(wcm, new MatchSequence(new StructuredTry(null, null), new BeginBlock(null), new StructuredAssignment(BytecodeLoc.NONE, new ArrayVariable(new ArrayIndex(BytecodeLoc.NONE, new LValueExpression(lookupTable), wcm.getMemberFunction("ordinal", "ordinal", new LValueExpression(wcm.getStaticVariable("enumval", enumObject.getInferredJavaType().getJavaTypeInstance(), enumObject.getInferredJavaType()))))), wcm.getExpressionWildCard("literal")), new EndBlock(null), new StructuredCatch(null, null, null, null), new BeginBlock(null), new EndBlock(null))));
    }

    private List<StructuredStatement> getLookupMethodStatements(Method lutStaticInit) {
        Op04StructuredStatement lutStaticInitCode = lutStaticInit.getAnalysis();
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(lutStaticInitCode);
        if (structuredStatements == null) {
            return null;
        }
        structuredStatements = Functional.filter(structuredStatements, new Predicate<StructuredStatement>(){

            @Override
            public boolean test(StructuredStatement in) {
                return !(in instanceof StructuredComment);
            }
        });
        return structuredStatements;
    }

    private Integer getIntegerFromLiteralExpression(Expression exp) {
        if (!(exp instanceof Literal)) {
            return null;
        }
        Literal literal = (Literal)exp;
        TypedLiteral typedLiteral = literal.getValue();
        if (typedLiteral.getType() != TypedLiteral.LiteralType.Integer) {
            return null;
        }
        return (Integer)typedLiteral.getValue();
    }

    private class SwitchForeignEnumMatchResultCollector
    extends AbstractMatchResultIterator {
        private final WildcardMatch wcmCase;
        private final Map<Integer, StaticVariable> lutValues = MapFactory.newMap();

        private SwitchForeignEnumMatchResultCollector(WildcardMatch wcmCase) {
            this.wcmCase = wcmCase;
        }

        Map<Integer, StaticVariable> getLUT() {
            return this.lutValues;
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            if (wcm == this.wcmCase) {
                StaticVariable staticVariable = wcm.getStaticVariable("enumval").getMatch();
                Expression exp = wcm.getExpressionWildCard("literal").getMatch();
                Integer literalInt = SwitchEnumRewriter.this.getIntegerFromLiteralExpression(exp);
                if (literalInt == null) {
                    return;
                }
                this.lutValues.put(literalInt, staticVariable);
            }
        }
    }

    private static class SwitchEnumMatchResultCollector
    extends AbstractMatchResultIterator {
        private Expression lookupTable;
        private Expression enumObject;
        private StructuredSwitch structuredSwitch;
        private StructuredExpressionStatement structuredExpressionStatement;

        private SwitchEnumMatchResultCollector() {
        }

        @Override
        public void clear() {
            this.lookupTable = null;
            this.enumObject = null;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            if (name.equals("switch")) {
                this.structuredSwitch = (StructuredSwitch)statement;
            } else if (name.equals("bodylessswitch")) {
                this.structuredExpressionStatement = (StructuredExpressionStatement)statement;
            }
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            this.lookupTable = wcm.getExpressionWildCard("lut").getMatch();
            this.enumObject = wcm.getExpressionWildCard("object").getMatch();
        }

        Expression getLookupTable() {
            return this.lookupTable;
        }

        Expression getEnumObject() {
            return this.enumObject;
        }

        StructuredSwitch getStructuredSwitch() {
            return this.structuredSwitch;
        }

        StructuredExpressionStatement getStructuredExpressionStatement() {
            return this.structuredExpressionStatement;
        }
    }

    private static class EclipseVarResultCollector
    implements MatchResultCollector {
        LValue lookup;
        LValue field;
        StaticFunctionInvokation arrayLen;

        private EclipseVarResultCollector() {
        }

        @Override
        public void clear() {
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            this.lookup = wcm.getLValueWildCard("lookup").getMatch();
            this.field = wcm.getLValueWildCard("static").getMatch();
            this.arrayLen = wcm.getStaticFunction("func").getMatch();
        }
    }
}

