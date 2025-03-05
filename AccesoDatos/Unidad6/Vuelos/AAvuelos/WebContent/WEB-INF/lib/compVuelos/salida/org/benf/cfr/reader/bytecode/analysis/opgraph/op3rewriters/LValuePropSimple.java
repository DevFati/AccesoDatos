/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;

public class LValuePropSimple {
    public static List<Op03SimpleStatement> condenseSimpleLValues(List<Op03SimpleStatement> statementList) {
        AssignmentCollector assignmentCollector = new AssignmentCollector();
        UsageCollector usageCollector = new UsageCollector();
        for (Op03SimpleStatement statement : statementList) {
            statement.getStatement().collectLValueAssignments(assignmentCollector);
            statement.getStatement().collectLValueUsage(usageCollector);
        }
        Map<StackSSALabel, StatementContainer<Statement>> created = assignmentCollector.assignments;
        List<StackSSALabel> singleUsages = usageCollector.getSingleUsages();
        Map createdAndUsed = MapFactory.newMap();
        Map creations = MapFactory.newIdentityMap();
        for (StackSSALabel single : singleUsages) {
            StatementContainer<Statement> creation = created.get(single);
            if (creation == null) continue;
            createdAndUsed.put(single, (Op03SimpleStatement)creation);
            creations.put((Op03SimpleStatement)creation, single);
        }
        int nopCount = 0;
        for (int x = statementList.size(); x > 1; --x) {
            Op03SimpleStatement prev = statementList.get(x - 1);
            if (!creations.containsKey(statementList.get(x - 1))) continue;
            Op03SimpleStatement now = statementList.get(x);
            if (prev.getTargets().size() != 1 || prev.getTargets().get(0) != now || now.getSources().size() != 1) continue;
            UsageCollector oneUsagecollector = new UsageCollector();
            now.getStatement().collectLValueUsage(oneUsagecollector);
            if (!prev.getBlockIdentifiers().isEmpty() || !now.getBlockIdentifiers().isEmpty()) continue;
            final StackSSALabel prevCreated = (StackSSALabel)creations.get(prev);
            if (!oneUsagecollector.getSingleUsages().contains(prevCreated)) continue;
            final Expression rhs = assignmentCollector.values.get(prevCreated);
            LValueRewriter<Statement> rewriter = new LValueRewriter<Statement>(){

                @Override
                public Expression getLValueReplacement(LValue lValue, SSAIdentifiers<LValue> ssaIdentifiers, StatementContainer<Statement> statementContainer) {
                    if (lValue.equals(prevCreated)) {
                        return rhs;
                    }
                    return null;
                }

                @Override
                public boolean explicitlyReplaceThisLValue(LValue lValue) {
                    return lValue.equals(prevCreated);
                }

                @Override
                public void checkPostConditions(LValue lValue, Expression rValue) {
                }

                @Override
                public boolean needLR() {
                    return false;
                }

                @Override
                public LValueRewriter<Statement> keepConstant(Collection<LValue> usedLValues) {
                    return this;
                }

                @Override
                public LValueRewriter getWithFixed(Set fixed) {
                    return this;
                }
            };
            Statement nowS = now.getStatement();
            nowS.replaceSingleUsageLValues(rewriter, null);
            prev.replaceStatement(nowS);
            prev.getTargets().clear();
            now.getSources().clear();
            for (Op03SimpleStatement target : now.getTargets()) {
                target.replaceSource(now, prev);
                prev.addTarget(target);
            }
            now.getTargets().clear();
            now.nopOut();
            ++nopCount;
        }
        if (nopCount > 0) {
            statementList = Cleaner.removeUnreachableCode(statementList, false);
        }
        return statementList;
    }

    private static class UsageCollector
    implements LValueUsageCollector {
        Map<StackSSALabel, Boolean> singleUsages = MapFactory.newMap();

        private UsageCollector() {
        }

        @Override
        public void collect(LValue lValue, ReadWrite rw) {
            if (!(lValue instanceof StackSSALabel)) {
                return;
            }
            StackSSALabel stackSSALabel = (StackSSALabel)lValue;
            if (this.singleUsages.containsKey(stackSSALabel)) {
                this.singleUsages.put(stackSSALabel, null);
            } else {
                this.singleUsages.put(stackSSALabel, Boolean.TRUE);
            }
        }

        List<StackSSALabel> getSingleUsages() {
            List<StackSSALabel> res = ListFactory.newList();
            for (Map.Entry<StackSSALabel, Boolean> entry : this.singleUsages.entrySet()) {
                if (entry.getValue() != Boolean.TRUE) continue;
                res.add(entry.getKey());
            }
            return res;
        }
    }

    private static class AssignmentCollector
    implements LValueAssignmentCollector<Statement> {
        Map<StackSSALabel, StatementContainer<Statement>> assignments = MapFactory.newMap();
        Map<StackSSALabel, Expression> values = MapFactory.newMap();

        private AssignmentCollector() {
        }

        @Override
        public void collect(StackSSALabel lValue, StatementContainer<Statement> statementContainer, Expression value) {
            if (this.assignments.containsKey(lValue)) {
                this.assignments.put(lValue, null);
                this.values.remove(lValue);
            } else {
                this.assignments.put(lValue, statementContainer);
                this.values.put(lValue, value);
            }
        }

        @Override
        public void collectMultiUse(StackSSALabel lValue, StatementContainer<Statement> statementContainer, Expression value) {
            this.assignments.put(lValue, null);
        }

        @Override
        public void collectMutatedLValue(LValue lValue, StatementContainer<Statement> statementContainer, Expression value) {
            if (lValue instanceof StackSSALabel) {
                this.assignments.put((StackSSALabel)lValue, null);
            }
        }

        @Override
        public void collectLocalVariableAssignment(LocalVariable localVariable, StatementContainer<Statement> statementContainer, Expression value) {
        }
    }
}

