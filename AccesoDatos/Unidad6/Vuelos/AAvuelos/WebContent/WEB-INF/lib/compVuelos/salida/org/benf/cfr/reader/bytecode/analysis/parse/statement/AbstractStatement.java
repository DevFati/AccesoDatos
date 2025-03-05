/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactoryImpl;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.CreationCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.output.ToStringDumper;

public abstract class AbstractStatement
implements Statement {
    private BytecodeLoc loc;
    private StatementContainer<Statement> container;

    public AbstractStatement(BytecodeLoc loc) {
        this.loc = loc;
    }

    @Override
    public BytecodeLoc getLoc() {
        return this.loc;
    }

    @Override
    public void addLoc(HasByteCodeLoc loc) {
        if (loc.getLoc().isEmpty()) {
            return;
        }
        this.loc = BytecodeLocFactoryImpl.INSTANCE.combine((HasByteCodeLoc)this, loc);
    }

    @Override
    public void setContainer(StatementContainer<Statement> container) {
        if (container == null) {
            throw new ConfusedCFRException("Trying to setContainer null!");
        }
        this.container = container;
    }

    @Override
    public Statement outerDeepClone(CloneHelper cloneHelper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LValue getCreatedLValue() {
        return null;
    }

    @Override
    public void collectLValueAssignments(LValueAssignmentCollector<Statement> lValueAssigmentCollector) {
    }

    @Override
    public boolean doesBlackListLValueReplacement(LValue lValue, Expression expression) {
        return false;
    }

    @Override
    public void collectObjectCreation(CreationCollector creationCollector) {
    }

    @Override
    public SSAIdentifiers<LValue> collectLocallyMutatedVariables(SSAIdentifierFactory<LValue, ?> ssaIdentifierFactory) {
        return new SSAIdentifiers<LValue>();
    }

    @Override
    public StatementContainer<Statement> getContainer() {
        return this.container;
    }

    @Override
    public Expression getRValue() {
        return null;
    }

    protected Statement getTargetStatement(int idx) {
        return this.container.getTargetStatement(idx);
    }

    @Override
    public boolean isCompound() {
        return false;
    }

    @Override
    public List<Statement> getCompoundParts() {
        throw new ConfusedCFRException("Should not be calling getCompoundParts on this statement");
    }

    public final String toString() {
        ToStringDumper d = new ToStringDumper();
        d.print(this.getClass().getSimpleName()).print(": ").dump(this);
        return ((Object)d).toString();
    }

    @Override
    public boolean fallsToNext() {
        return true;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return true;
    }

    @Override
    public Set<LValue> wantsLifetimeHint() {
        return null;
    }

    @Override
    public void setLifetimeHint(LValue lv, boolean usedInChildren) {
    }
}

