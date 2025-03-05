/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BoolOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ComparisonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.InstanceOfExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.InstanceOfExpressionDefining;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NotOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.collections.ListFactory;

public class InstanceOfAssignRewriter {
    private static final InferredJavaType ijtBool = new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION);
    private final WildcardMatch wcm = new WildcardMatch();
    private final LValue scopedEntity;
    private final WildcardMatch.LValueWildcard objWildcard;
    private final WildcardMatch.LValueWildcard tmpWildcard;
    private final List<ConditionTest> tests;

    public static boolean hasInstanceOf(ConditionalExpression conditionalExpression) {
        InstanceOfSearch search = new InstanceOfSearch();
        search.rewriteExpression(conditionalExpression, null, null, null);
        return search.found;
    }

    public InstanceOfAssignRewriter(LValue scopedEntity) {
        JavaTypeInstance target = scopedEntity.getInferredJavaType().getJavaTypeInstance();
        this.scopedEntity = scopedEntity;
        InferredJavaType ijtTarget = new InferredJavaType(target, InferredJavaType.Source.EXPRESSION);
        this.objWildcard = this.wcm.getLValueWildCard("obj");
        this.tmpWildcard = this.wcm.getLValueWildCard("tmp");
        LValueExpression obj = new LValueExpression(this.objWildcard);
        CastExpression castObj = new CastExpression(BytecodeLoc.TODO, ijtTarget, obj);
        this.tests = ListFactory.newList();
        BooleanOperation cPos1 = new BooleanOperation(BytecodeLoc.NONE, new BooleanExpression(new InstanceOfExpression(BytecodeLoc.NONE, ijtBool, obj, target)), new ComparisonOperation(BytecodeLoc.NONE, new AssignmentExpression(BytecodeLoc.NONE, scopedEntity, castObj), castObj, CompOp.EQ), BoolOp.AND);
        NotOperation cPos2 = new NotOperation(BytecodeLoc.NONE, cPos1.getDemorganApplied(true));
        this.tests.add(new ConditionTest(cPos1, true, MatchType.SIMPLE));
        this.tests.add(new ConditionTest(cPos2, true, MatchType.SIMPLE));
        this.tests.add(new ConditionTest(cPos1.getNegated(), false, MatchType.SIMPLE));
        this.tests.add(new ConditionTest(cPos2.getNegated(), false, MatchType.SIMPLE));
        CastExpression castTmp = new CastExpression(BytecodeLoc.NONE, ijtTarget, new LValueExpression(this.tmpWildcard));
        BooleanOperation dPos1 = new BooleanOperation(BytecodeLoc.NONE, new BooleanExpression(new InstanceOfExpression(BytecodeLoc.NONE, ijtBool, new AssignmentExpression(BytecodeLoc.NONE, this.tmpWildcard, obj), target)), new ComparisonOperation(BytecodeLoc.NONE, new AssignmentExpression(BytecodeLoc.NONE, scopedEntity, castTmp), castTmp, CompOp.EQ), BoolOp.AND);
        NotOperation dPos2 = new NotOperation(BytecodeLoc.NONE, cPos1.getDemorganApplied(true));
        this.tests.add(new ConditionTest(dPos1, true, MatchType.ASSIGN_SIMPLE));
        this.tests.add(new ConditionTest(dPos2, true, MatchType.ASSIGN_SIMPLE));
        this.tests.add(new ConditionTest(dPos1.getNegated(), false, MatchType.ASSIGN_SIMPLE));
        this.tests.add(new ConditionTest(dPos2.getNegated(), false, MatchType.ASSIGN_SIMPLE));
    }

    private ConditionTest getMatchingTest(ConditionalExpression ce) {
        for (ConditionTest ct : this.tests) {
            this.wcm.reset();
            if (!ct.expression.equals(ce)) continue;
            return ct;
        }
        return null;
    }

    public boolean isMatchFor(ConditionalExpression ce) {
        RewriteFinder rewriteFinder = new RewriteFinder();
        rewriteFinder.rewriteExpression(ce, null, null, null);
        return rewriteFinder.found;
    }

    public ConditionalExpression rewriteDefining(ConditionalExpression ce) {
        return new Rewriter().rewriteExpression(ce, null, null, null);
    }

    private ConditionalExpression rewriteInner(ConditionalExpression ce) {
        ConditionTest ct = this.getMatchingTest(ce);
        if (ct == null) {
            return ce;
        }
        if (ct.matchType == MatchType.SIMPLE) {
            LValue obj = this.objWildcard.getMatch();
            ce = new BooleanExpression(new InstanceOfExpressionDefining(BytecodeLoc.TODO, new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION), new LValueExpression(obj), this.scopedEntity.getInferredJavaType().getJavaTypeInstance(), this.scopedEntity));
        } else {
            LValue obj = this.objWildcard.getMatch();
            LValue tmp = this.tmpWildcard.getMatch();
            ce = new BooleanExpression(new InstanceOfExpressionDefining(BytecodeLoc.TODO, new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION), new AssignmentExpression(BytecodeLoc.TODO, tmp, new LValueExpression(BytecodeLoc.TODO, obj)), this.scopedEntity.getInferredJavaType().getJavaTypeInstance(), this.scopedEntity));
        }
        if (!ct.isPositive) {
            ce = ce.getNegated();
        }
        return ce;
    }

    private class Rewriter
    extends AbstractExpressionRewriter {
        private Rewriter() {
        }

        @Override
        public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            expression = InstanceOfAssignRewriter.this.rewriteInner(expression);
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }

    private class RewriteFinder
    extends AbstractExpressionRewriter {
        private boolean found = false;

        private RewriteFinder() {
        }

        @Override
        public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (this.found) {
                return expression;
            }
            if (InstanceOfAssignRewriter.this.getMatchingTest(expression) != null) {
                this.found = true;
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (this.found) {
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }

    private static class ConditionTest {
        final ConditionalExpression expression;
        final boolean isPositive;
        final MatchType matchType;

        ConditionTest(ConditionalExpression ct, boolean isPositive, MatchType matchType) {
            this.expression = ct;
            this.isPositive = isPositive;
            this.matchType = matchType;
        }
    }

    private static enum MatchType {
        SIMPLE,
        ASSIGN_SIMPLE;

    }

    private static class InstanceOfSearch
    extends AbstractExpressionRewriter {
        private boolean found = false;

        private InstanceOfSearch() {
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (this.found) {
                return expression;
            }
            if (expression instanceof InstanceOfExpression) {
                this.found = true;
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        @Override
        public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (this.found) {
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }
}

