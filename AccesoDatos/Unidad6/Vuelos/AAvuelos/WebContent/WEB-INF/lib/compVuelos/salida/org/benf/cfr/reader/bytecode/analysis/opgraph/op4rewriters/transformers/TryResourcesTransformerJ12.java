/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ResourceReleaseDetector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.EmptyMatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchOneOf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.ResetAfterTest;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesTransformerBase;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredTry;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.functors.Predicate;

public class TryResourcesTransformerJ12
extends TryResourcesTransformerBase {
    public TryResourcesTransformerJ12(ClassFile classFile) {
        super(classFile);
    }

    @Override
    protected boolean rewriteTry(StructuredTry structuredTry, StructuredScope scope, TryResourcesTransformerBase.ResourceMatch resourceMatch) {
        if (!super.rewriteTry(structuredTry, scope, resourceMatch)) {
            return false;
        }
        structuredTry.getCatchBlocks().clear();
        for (Op04StructuredStatement remove : resourceMatch.removeThese) {
            remove.nopOut();
        }
        return true;
    }

    @Override
    protected TryResourcesTransformerBase.ResourceMatch getResourceMatch(StructuredTry structuredTry, StructuredScope scope) {
        if (structuredTry.getFinallyBlock() == null) {
            return this.getComplexResourceMatch(structuredTry, scope);
        }
        return this.getSimpleResourceMatch(structuredTry, scope);
    }

    private TryResourcesTransformerBase.ResourceMatch getSimpleResourceMatch(StructuredTry structuredTry, StructuredScope scope) {
        Op04StructuredStatement finallyBlock = structuredTry.getFinallyBlock();
        WildcardMatch wcm = new WildcardMatch();
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(finallyBlock);
        if (structuredStatements == null) {
            return null;
        }
        WildcardMatch.LValueWildcard throwableLValue = wcm.getLValueWildCard("throwable");
        WildcardMatch.LValueWildcard autoclose = wcm.getLValueWildCard("resource");
        ResetAfterTest m = new ResetAfterTest(wcm, new MatchSequence(new BeginBlock(null), ResourceReleaseDetector.getSimpleStructuredStatementMatcher(wcm, throwableLValue, autoclose), new EndBlock(null)));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);
        TryResourcesTransformerBase.TryResourcesMatchResultCollector collector = new TryResourcesTransformerBase.TryResourcesMatchResultCollector();
        mi.advance();
        mi.advance();
        boolean res = m.match(mi, (MatchResultCollector)collector);
        if (!res) {
            return null;
        }
        return new TryResourcesTransformerBase.ResourceMatch(null, collector.resource, collector.throwable, false, Collections.<Op04StructuredStatement>emptyList());
    }

    private TryResourcesTransformerBase.ResourceMatch getComplexResourceMatch(StructuredTry structuredTry, StructuredScope scope) {
        if (structuredTry.getCatchBlocks().size() != 1) {
            return null;
        }
        Op04StructuredStatement catchBlock = structuredTry.getCatchBlocks().get(0);
        StructuredStatement catchStm = catchBlock.getStatement();
        if (!(catchStm instanceof StructuredCatch)) {
            return null;
        }
        StructuredCatch catchStatement = (StructuredCatch)catchStm;
        if (catchStatement.getCatchTypes().size() != 1) {
            return null;
        }
        JavaTypeInstance caughtType = catchStatement.getCatchTypes().get(0);
        if (!TypeConstants.THROWABLE.equals(caughtType)) {
            return null;
        }
        WildcardMatch wcm = new WildcardMatch();
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(catchBlock);
        if (structuredStatements == null) {
            return null;
        }
        WildcardMatch.LValueWildcard throwableLValue = wcm.getLValueWildCard("throwable");
        WildcardMatch.LValueWildcard autoclose = wcm.getLValueWildCard("resource");
        ResetAfterTest m = new ResetAfterTest(wcm, new MatchSequence(new BeginBlock(null), ResourceReleaseDetector.getNonTestingStructuredStatementMatcher(wcm, throwableLValue, autoclose), new EndBlock(null)));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);
        TryResourcesTransformerBase.TryResourcesMatchResultCollector collector = new TryResourcesTransformerBase.TryResourcesMatchResultCollector();
        mi.advance();
        mi.advance();
        boolean res = m.match(mi, (MatchResultCollector)collector);
        if (!res) {
            return null;
        }
        List<Op04StructuredStatement> toRemove = this.getCloseStatementAfter(structuredTry, scope, wcm, collector);
        if (toRemove == null && (toRemove = this.getCloseStatementEndTry(structuredTry, scope, wcm, collector)) == null) {
            return null;
        }
        return new TryResourcesTransformerBase.ResourceMatch(null, collector.resource, collector.throwable, false, toRemove);
    }

    private List<Op04StructuredStatement> getCloseStatementEndTry(StructuredTry structuredTry, StructuredScope scope, WildcardMatch wcm, TryResourcesTransformerBase.TryResourcesMatchResultCollector collector) {
        Op04StructuredStatement tryb = structuredTry.getTryBlock();
        StructuredStatement tryStm = tryb.getStatement();
        if (!(tryStm instanceof Block)) {
            return null;
        }
        Block block = (Block)tryStm;
        Op04StructuredStatement lastInBlock = block.getLast();
        if (this.getMatchingCloseStatement(wcm, collector, lastInBlock.getStatement())) {
            return Collections.singletonList(lastInBlock);
        }
        return null;
    }

    private List<Op04StructuredStatement> getCloseStatementAfter(StructuredTry structuredTry, StructuredScope scope, WildcardMatch wcm, TryResourcesTransformerBase.TryResourcesMatchResultCollector collector) {
        Set<Op04StructuredStatement> next = scope.getNextFallThrough(structuredTry);
        List<Op04StructuredStatement> toRemove = Functional.filter(next, new Predicate<Op04StructuredStatement>(){

            @Override
            public boolean test(Op04StructuredStatement in) {
                return !(in.getStatement() instanceof StructuredComment);
            }
        });
        if (toRemove.size() != 1) {
            return null;
        }
        StructuredStatement statement = toRemove.get(0).getStatement();
        if (this.getMatchingCloseStatement(wcm, collector, statement)) {
            return toRemove;
        }
        return null;
    }

    private boolean getMatchingCloseStatement(WildcardMatch wcm, TryResourcesTransformerBase.TryResourcesMatchResultCollector collector, StructuredStatement statement) {
        MatchOneOf checkClose = ResourceReleaseDetector.getCloseExpressionMatch(wcm, new LValueExpression(collector.resource));
        MatchIterator<StructuredStatement> closeStm = new MatchIterator<StructuredStatement>(Collections.singletonList(statement));
        closeStm.advance();
        return checkClose.match(closeStm, (MatchResultCollector)new EmptyMatchResultCollector());
    }
}

