/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.DeepCloneable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ComparableUnderEC;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.CreationCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.output.Dumpable;

public interface Statement
extends Dumpable,
ComparableUnderEC,
DeepCloneable<Statement>,
HasByteCodeLoc {
    public void setContainer(StatementContainer<Statement> var1);

    public void collectLValueAssignments(LValueAssignmentCollector<Statement> var1);

    public void collectLValueUsage(LValueUsageCollector var1);

    public boolean doesBlackListLValueReplacement(LValue var1, Expression var2);

    public void replaceSingleUsageLValues(LValueRewriter var1, SSAIdentifiers var2);

    public void rewriteExpressions(ExpressionRewriter var1, SSAIdentifiers var2);

    public void collectObjectCreation(CreationCollector var1);

    public SSAIdentifiers<LValue> collectLocallyMutatedVariables(SSAIdentifierFactory<LValue, ?> var1);

    public boolean isCompound();

    public LValue getCreatedLValue();

    public Expression getRValue();

    public StatementContainer<Statement> getContainer();

    public List<Statement> getCompoundParts();

    public StructuredStatement getStructuredStatement();

    @Override
    public boolean equivalentUnder(Object var1, EquivalenceConstraint var2);

    public boolean fallsToNext();

    public boolean canThrow(ExceptionCheck var1);

    public Set<LValue> wantsLifetimeHint();

    public void setLifetimeHint(LValue var1, boolean var2);
}

