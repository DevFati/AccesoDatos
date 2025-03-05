/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.EmptyMatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredThrow;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredTry;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

public abstract class TryResourcesTransformerBase
implements StructuredStatementTransformer {
    private final ClassFile classFile;
    private boolean success = false;

    TryResourcesTransformerBase(ClassFile classFile) {
        this.classFile = classFile;
    }

    public boolean transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
        return this.success;
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        StructuredTry structuredTry;
        ResourceMatch match;
        if (in instanceof StructuredTry && (match = this.getResourceMatch(structuredTry = (StructuredTry)in, scope)) != null && this.rewriteTry(structuredTry, scope, match)) {
            this.success = true;
        }
        in.transformStructuredChildren(this, scope);
        return in;
    }

    protected abstract ResourceMatch getResourceMatch(StructuredTry var1, StructuredScope var2);

    protected boolean rewriteTry(StructuredTry structuredTry, StructuredScope scope, ResourceMatch resourceMatch) {
        LValue resource;
        List<Op04StructuredStatement> preceeding = scope.getPrecedingInblock(1, 2);
        Op04StructuredStatement autoAssign = this.findAutoclosableAssignment(preceeding, resource = resourceMatch.resource);
        if (autoAssign == null) {
            return false;
        }
        StructuredAssignment assign = (StructuredAssignment)autoAssign.getStatement();
        autoAssign.nopOut();
        structuredTry.setFinally(null);
        structuredTry.addResources(Collections.singletonList(new Op04StructuredStatement(assign)));
        if (resourceMatch.resourceMethod != null) {
            resourceMatch.resourceMethod.hideSynthetic();
        }
        if (resourceMatch.reprocessException) {
            return this.rewriteException(structuredTry, preceeding);
        }
        return true;
    }

    private boolean rewriteException(StructuredTry structuredTry, List<Op04StructuredStatement> preceeding) {
        List<Op04StructuredStatement> catchBlocks = structuredTry.getCatchBlocks();
        if (catchBlocks.size() != 1) {
            return false;
        }
        Op04StructuredStatement catchBlock = catchBlocks.get(0);
        Op04StructuredStatement exceptionDeclare = null;
        LValue tempThrowable = null;
        for (int x = preceeding.size() - 1; x >= 0; --x) {
            Op04StructuredStatement stm = preceeding.get(x);
            StructuredStatement structuredStatement = stm.getStatement();
            if (structuredStatement.isScopeBlock()) {
                return false;
            }
            if (!(structuredStatement instanceof StructuredAssignment)) continue;
            StructuredAssignment ass = (StructuredAssignment)structuredStatement;
            LValue lvalue = ass.getLvalue();
            if (!ass.isCreator(lvalue)) {
                return false;
            }
            if (!ass.getRvalue().equals(Literal.NULL)) {
                return false;
            }
            if (!lvalue.getInferredJavaType().getJavaTypeInstance().equals(TypeConstants.THROWABLE)) {
                return false;
            }
            exceptionDeclare = stm;
            tempThrowable = lvalue;
            break;
        }
        if (exceptionDeclare == null) {
            return false;
        }
        List<StructuredStatement> catchContent = ListFactory.newList();
        catchBlock.linearizeStatementsInto(catchContent);
        WildcardMatch wcm = new WildcardMatch();
        WildcardMatch.LValueWildcard exceptionWildCard = wcm.getLValueWildCard("exception");
        MatchSequence matcher = new MatchSequence(new StructuredCatch(null, null, exceptionWildCard, null), new BeginBlock(null), new StructuredAssignment(BytecodeLoc.NONE, tempThrowable, new LValueExpression(exceptionWildCard)), new StructuredThrow(BytecodeLoc.NONE, new LValueExpression(exceptionWildCard)), new EndBlock(null));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(catchContent);
        EmptyMatchResultCollector collector = new EmptyMatchResultCollector();
        mi.advance();
        if (!matcher.match(mi, (MatchResultCollector)collector)) {
            return false;
        }
        LValue caught = wcm.getLValueWildCard("exception").getMatch();
        if (!caught.getInferredJavaType().getJavaTypeInstance().equals(TypeConstants.THROWABLE)) {
            return false;
        }
        exceptionDeclare.nopOut();
        structuredTry.clearCatchBlocks();
        return true;
    }

    private Op04StructuredStatement findAutoclosableAssignment(List<Op04StructuredStatement> preceeding, LValue resource) {
        LValueUsageCheckingRewriter usages = new LValueUsageCheckingRewriter();
        for (int x = preceeding.size() - 1; x >= 0; --x) {
            Op04StructuredStatement stm = preceeding.get(x);
            StructuredStatement structuredStatement = stm.getStatement();
            if (structuredStatement.isScopeBlock()) {
                return null;
            }
            if (!(structuredStatement instanceof StructuredAssignment)) continue;
            StructuredAssignment structuredAssignment = (StructuredAssignment)structuredStatement;
            if (structuredAssignment.isCreator(resource)) {
                LValueUsageCheckingRewriter check = new LValueUsageCheckingRewriter();
                structuredAssignment.rewriteExpressions(check);
                if (SetUtil.hasIntersection(usages.used, check.used)) {
                    return null;
                }
                return stm;
            }
            structuredStatement.rewriteExpressions(usages);
        }
        return null;
    }

    protected ClassFile getClassFile() {
        return this.classFile;
    }

    protected static class TryResourcesMatchResultCollector
    implements MatchResultCollector {
        StaticFunctionInvokation fn;
        LValue resource;
        LValue throwable;

        protected TryResourcesMatchResultCollector() {
        }

        @Override
        public void clear() {
            this.fn = null;
            this.resource = null;
            this.throwable = null;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
        }

        private StaticFunctionInvokation getFn(WildcardMatch wcm, String name) {
            WildcardMatch.StaticFunctionInvokationWildcard staticFunction = wcm.getStaticFunction(name);
            if (staticFunction == null) {
                return null;
            }
            return staticFunction.getMatch();
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            this.fn = this.getFn(wcm, "fn");
            if (this.fn == null) {
                this.fn = this.getFn(wcm, "fn2");
            }
            if (this.fn == null) {
                this.fn = this.getFn(wcm, "fn3");
            }
            this.resource = wcm.getLValueWildCard("resource").getMatch();
            this.throwable = wcm.getLValueWildCard("throwable").getMatch();
        }
    }

    static class ResourceMatch {
        final Method resourceMethod;
        final LValue resource;
        final LValue throwable;
        final boolean reprocessException;
        final List<Op04StructuredStatement> removeThese;

        ResourceMatch(Method resourceMethod, LValue resource, LValue throwable) {
            this(resourceMethod, resource, throwable, true, null);
        }

        ResourceMatch(Method resourceMethod, LValue resource, LValue throwable, boolean reprocessException, List<Op04StructuredStatement> removeThese) {
            this.resourceMethod = resourceMethod;
            this.resource = resource;
            this.throwable = throwable;
            this.reprocessException = reprocessException;
            this.removeThese = removeThese;
        }
    }

    private static class LValueUsageCheckingRewriter
    extends AbstractExpressionRewriter {
        final Set<LValue> used = SetFactory.newSet();

        private LValueUsageCheckingRewriter() {
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            this.used.add(lValue);
            return lValue;
        }
    }
}

