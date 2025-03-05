/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ResourceReleaseDetector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchOneOf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.Matcher;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.ResetAfterTest;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourceTransformerFinally;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TryResourcesTransformerBase;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ComparisonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredFinally;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.entities.ClassFile;

public class TryResourcesTransformerJ7
extends TryResourceTransformerFinally {
    public TryResourcesTransformerJ7(ClassFile classFile) {
        super(classFile);
    }

    @Override
    protected TryResourcesTransformerBase.ResourceMatch findResourceFinally(Op04StructuredStatement finallyBlock) {
        if (finallyBlock == null) {
            return null;
        }
        StructuredFinally finalli = (StructuredFinally)finallyBlock.getStatement();
        Op04StructuredStatement content = finalli.getCatchBlock();
        WildcardMatch wcm = new WildcardMatch();
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(content);
        if (structuredStatements == null) {
            return null;
        }
        WildcardMatch.LValueWildcard throwableLValue = wcm.getLValueWildCard("throwable");
        WildcardMatch.LValueWildcard autoclose = wcm.getLValueWildCard("resource");
        Matcher<StructuredStatement> subMatch = ResourceReleaseDetector.getStructuredStatementMatcher(wcm, throwableLValue, autoclose);
        MatchOneOf m = new MatchOneOf(new ResetAfterTest(wcm, new MatchSequence(new BeginBlock(null), new StructuredIf(BytecodeLoc.NONE, new ComparisonOperation(BytecodeLoc.TODO, new LValueExpression(autoclose), Literal.NULL, CompOp.NE), null), subMatch, new EndBlock(null))), new ResetAfterTest(wcm, subMatch));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);
        TryResourcesTransformerBase.TryResourcesMatchResultCollector collector = new TryResourcesTransformerBase.TryResourcesMatchResultCollector();
        mi.advance();
        boolean res = m.match(mi, (MatchResultCollector)collector);
        if (!res) {
            return null;
        }
        LValue resource = collector.resource;
        LValue throwable = collector.throwable;
        return new TryResourcesTransformerBase.ResourceMatch(null, resource, throwable);
    }
}

