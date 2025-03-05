/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ExpressionWildcardReplacingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ComparisonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.QuotingUtils;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredThrow;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredTry;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.NonaryFunction;

public class J14ClassObjectRewriter {
    private final ClassFile classFile;
    private final DCCommonState state;

    public J14ClassObjectRewriter(ClassFile classFile, DCCommonState state) {
        this.classFile = classFile;
        this.state = state;
    }

    public void rewrite() {
        Method method = this.classFile.getSingleMethodByNameOrNull("class$");
        JavaTypeInstance classType = this.classFile.getClassType();
        if (!this.methodIsClassLookup(method)) {
            return;
        }
        method.hideSynthetic();
        final WildcardMatch wcm = new WildcardMatch();
        final WildcardMatch.StaticVariableWildcard staticVariable = wcm.getStaticVariable("classVar", classType, new InferredJavaType(TypeConstants.CLASS, InferredJavaType.Source.TEST));
        LValueExpression staticExpression = new LValueExpression(staticVariable);
        TernaryExpression test = new TernaryExpression(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.NONE, staticExpression, Literal.NULL, CompOp.EQ), new AssignmentExpression(BytecodeLoc.NONE, staticVariable, wcm.getStaticFunction("test", classType, (JavaTypeInstance)TypeConstants.CLASS, null, ListFactory.newImmutableList(wcm.getExpressionWildCard("classString")))), staticExpression);
        final Set<Pair> hideThese = SetFactory.newSet();
        ExpressionWildcardReplacingRewriter expressionRewriter = new ExpressionWildcardReplacingRewriter(wcm, test, new NonaryFunction<Expression>(){

            @Override
            public Expression invoke() {
                Expression string = wcm.getExpressionWildCard("classString").getMatch();
                if (!(string instanceof Literal)) {
                    return null;
                }
                TypedLiteral literal = ((Literal)string).getValue();
                if (literal.getType() != TypedLiteral.LiteralType.String) {
                    return null;
                }
                Literal res = new Literal(TypedLiteral.getClass(J14ClassObjectRewriter.this.state.getClassCache().getRefClassFor(QuotingUtils.unquoteString((String)literal.getValue()))));
                StaticVariable found = staticVariable.getMatch();
                hideThese.add(Pair.make(found.getFieldName(), found.getInferredJavaType().getJavaTypeInstance()));
                return res;
            }
        });
        ExpressionRewriterTransformer transformer = new ExpressionRewriterTransformer(expressionRewriter);
        for (ClassFileField field : this.classFile.getFields()) {
            Expression initialValue = field.getInitialValue();
            field.setInitialValue(expressionRewriter.rewriteExpression(initialValue, null, null, ExpressionRewriterFlags.RVALUE));
        }
        for (Method testMethod : this.classFile.getMethods()) {
            if (!testMethod.hasCodeAttribute()) continue;
            testMethod.getAnalysis().transform(transformer, new StructuredScope());
        }
        for (Pair hideThis : hideThese) {
            try {
                ClassFileField fileField = this.classFile.getFieldByName((String)hideThis.getFirst(), (JavaTypeInstance)hideThis.getSecond());
                fileField.markHidden();
            }
            catch (NoSuchFieldException noSuchFieldException) {}
        }
    }

    private boolean methodIsClassLookup(Method method) {
        if (method == null) {
            return false;
        }
        if (!method.getAccessFlags().contains((Object)AccessFlagMethod.ACC_SYNTHETIC)) {
            return false;
        }
        if (!method.hasCodeAttribute()) {
            return false;
        }
        List<StructuredStatement> statements = MiscStatementTools.linearise(method.getAnalysis());
        if (statements == null) {
            return false;
        }
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(statements);
        WildcardMatch wcm1 = new WildcardMatch();
        List<LocalVariable> args = method.getMethodPrototype().getComputedParameters();
        if (args.size() != 1) {
            return false;
        }
        LocalVariable arg = args.get(0);
        if (!TypeConstants.STRING.equals(arg.getInferredJavaType().getJavaTypeInstance())) {
            return false;
        }
        MatchSequence m = new MatchSequence(new BeginBlock(null), new StructuredTry(null, null), new BeginBlock(null), new StructuredReturn(BytecodeLoc.NONE, wcm1.getStaticFunction("forName", (JavaTypeInstance)TypeConstants.CLASS, null, "forName", new LValueExpression(arg)), TypeConstants.CLASS), new EndBlock(null), new StructuredCatch(null, null, null, null), new BeginBlock(null), new StructuredThrow(BytecodeLoc.NONE, wcm1.getMemberFunction("initCause", "initCause", wcm1.getConstructorSimpleWildcard("nocd", TypeConstants.NOCLASSDEFFOUND_ERROR), wcm1.getExpressionWildCard("throwable"))), new EndBlock(null), new EndBlock(null));
        mi.advance();
        return m.match(mi, (MatchResultCollector)null);
    }
}

