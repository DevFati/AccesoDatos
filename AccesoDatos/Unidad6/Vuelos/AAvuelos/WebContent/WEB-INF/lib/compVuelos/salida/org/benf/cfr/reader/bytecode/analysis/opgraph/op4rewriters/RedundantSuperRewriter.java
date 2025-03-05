/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.Op04Rewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.AbstractMatchResultIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.CollectMatch;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.functors.Predicate;

public class RedundantSuperRewriter
implements Op04Rewriter {
    protected List<Expression> getSuperArgs(WildcardMatch wcm) {
        return null;
    }

    protected Set<LValue> getDeclarationsToNop(WildcardMatch wcm) {
        return null;
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) {
            return;
        }
        WildcardMatch wcm1 = new WildcardMatch();
        CollectMatch m = new CollectMatch("ass1", new StructuredExpressionStatement(BytecodeLoc.NONE, wcm1.getSuperFunction("s1", this.getSuperArgs(wcm1)), false));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);
        SuperResultCollector collector = new SuperResultCollector(wcm1, structuredStatements);
        while (mi.hasNext()) {
            mi.advance();
            if (!m.match(mi, (MatchResultCollector)collector)) continue;
            return;
        }
    }

    protected boolean canBeNopped(SuperFunctionInvokation superInvokation) {
        return superInvokation.isEmptyIgnoringSynthetics();
    }

    private class SuperResultCollector
    extends AbstractMatchResultIterator {
        private final WildcardMatch wcm;
        private final List<StructuredStatement> structuredStatements;

        private SuperResultCollector(WildcardMatch wcm, List<StructuredStatement> structuredStatements) {
            this.wcm = wcm;
            this.structuredStatements = structuredStatements;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            SuperFunctionInvokation superInvokation = this.wcm.getSuperFunction("s1").getMatch();
            if (RedundantSuperRewriter.this.canBeNopped(superInvokation)) {
                statement.getContainer().nopOut();
                Set<LValue> declarationsToNop = RedundantSuperRewriter.this.getDeclarationsToNop(this.wcm);
                if (declarationsToNop != null) {
                    List<StructuredStatement> decls = Functional.filter(this.structuredStatements, new Predicate<StructuredStatement>(){

                        @Override
                        public boolean test(StructuredStatement in) {
                            return in instanceof StructuredDefinition;
                        }
                    });
                    for (StructuredStatement decl : decls) {
                        StructuredDefinition defn = (StructuredDefinition)decl;
                        if (!declarationsToNop.contains(defn.getLvalue())) continue;
                        defn.getContainer().nopOut();
                    }
                }
            }
        }
    }
}

