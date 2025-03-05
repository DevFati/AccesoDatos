/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils.scope;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.ScopeDiscoverInfoCache;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredSwitch;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;

public abstract class AbstractLValueScopeDiscoverer
implements LValueScopeDiscoverer {
    final Map<NamedVariable, ScopeDefinition> earliestDefinition = MapFactory.newOrderedMap();
    final Map<Integer, Map<NamedVariable, Boolean>> earliestDefinitionsByLevel = MapFactory.newLazyMap(new UnaryFunction<Integer, Map<NamedVariable, Boolean>>(){

        @Override
        public Map<NamedVariable, Boolean> invoke(Integer arg) {
            return MapFactory.newIdentityMap();
        }
    });
    int currentDepth = 0;
    Stack<StatementContainer<StructuredStatement>> currentBlock = new Stack();
    final List<ScopeDefinition> discoveredCreations = ListFactory.newList();
    final VariableFactory variableFactory;
    StatementContainer<StructuredStatement> currentMark = null;
    Options options;
    private final MethodPrototype prototype;
    private final ScopeDiscoverInfoCache factCache = new ScopeDiscoverInfoCache();

    AbstractLValueScopeDiscoverer(Options options, MethodPrototype prototype, VariableFactory variableFactory) {
        this.options = options;
        this.prototype = prototype;
        List<LocalVariable> parameters = prototype.getComputedParameters();
        this.variableFactory = variableFactory;
        for (LocalVariable parameter : parameters) {
            InferredJavaType inferredJavaType = parameter.getInferredJavaType();
            ScopeDefinition prototypeScope = new ScopeDefinition(0, null, null, parameter, inferredJavaType, parameter.getName());
            this.earliestDefinition.put(parameter.getName(), prototypeScope);
        }
    }

    ScopeDiscoverInfoCache getFactCache() {
        return this.factCache;
    }

    @Override
    public void enterBlock(StructuredStatement structuredStatement) {
        Op04StructuredStatement container = structuredStatement.getContainer();
        if (container == null) {
            return;
        }
        this.currentBlock.push(container);
        ++this.currentDepth;
    }

    @Override
    public boolean ifCanDefine() {
        return false;
    }

    @Override
    public void processOp04Statement(Op04StructuredStatement statement) {
        statement.getStatement().traceLocalVariableScope(this);
    }

    @Override
    public void mark(StatementContainer<StructuredStatement> mark) {
        this.currentMark = mark;
    }

    @Override
    public void leaveBlock(StructuredStatement structuredStatement) {
        Op04StructuredStatement container = structuredStatement.getContainer();
        if (container == null) {
            return;
        }
        for (NamedVariable definedHere : this.earliestDefinitionsByLevel.get(this.currentDepth).keySet()) {
            this.earliestDefinition.remove(definedHere);
        }
        this.earliestDefinitionsByLevel.remove(this.currentDepth);
        StatementContainer<StructuredStatement> oldContainer = this.currentBlock.pop();
        if (container != oldContainer) {
            throw new IllegalStateException();
        }
        --this.currentDepth;
    }

    @Override
    public void collect(StackSSALabel lValue, StatementContainer<StructuredStatement> statementContainer, Expression value) {
    }

    @Override
    public void collectMultiUse(StackSSALabel lValue, StatementContainer<StructuredStatement> statementContainer, Expression value) {
    }

    @Override
    public void collectMutatedLValue(LValue lValue, StatementContainer<StructuredStatement> statementContainer, Expression value) {
    }

    public void markDiscoveredCreations() {
        Map<ScopeKey, List<ScopeDefinition>> definitionsByType = Functional.groupToMapBy(this.discoveredCreations, MapFactory.newOrderedMap(), new UnaryFunction<ScopeDefinition, ScopeKey>(){

            @Override
            public ScopeKey invoke(ScopeDefinition arg) {
                return arg.getScopeKey();
            }
        });
        for (Map.Entry<ScopeKey, List<ScopeDefinition>> entry : definitionsByType.entrySet()) {
            StatementContainer<StructuredStatement> hint;
            ScopeKey scopeKey = entry.getKey();
            List<ScopeDefinition> definitions = entry.getValue();
            List<StatementContainer<StructuredStatement>> commonScope = null;
            ScopeDefinition bestDefn = null;
            LValue scopedEntity = scopeKey.getlValue();
            for (int x = definitions.size() - 1; x >= 0; --x) {
                ScopeDefinition definition = definitions.get(x);
                StructuredStatement statement = definition.getStatementContainer().getStatement();
                if (statement.alwaysDefines(scopedEntity)) {
                    statement.markCreator(scopedEntity, null);
                    continue;
                }
                List<StatementContainer<StructuredStatement>> scopeList = definition.getNestedScope();
                if (scopeList.isEmpty()) {
                    scopeList = null;
                }
                if (scopeList == null) {
                    commonScope = null;
                    bestDefn = definition;
                    break;
                }
                if (commonScope == null) {
                    commonScope = scopeList;
                    bestDefn = definition;
                    continue;
                }
                bestDefn = (commonScope = AbstractLValueScopeDiscoverer.getCommonPrefix(commonScope, scopeList)).size() == scopeList.size() ? definition : null;
            }
            if (bestDefn != definitions.get(0)) {
                bestDefn = null;
            }
            StatementContainer<StructuredStatement> creationContainer = null;
            if (scopedEntity instanceof SentinelLocalClassLValue) {
                List<StatementContainer<StructuredStatement>> scope = null;
                if (bestDefn != null) {
                    scope = bestDefn.getNestedScope();
                } else if (commonScope != null) {
                    scope = commonScope;
                }
                if (scope != null) {
                    for (int i = scope.size() - 1; i >= 0; --i) {
                        StatementContainer<StructuredStatement> thisItem = scope.get(i);
                        if (!(thisItem.getStatement() instanceof Block)) continue;
                        Block block = (Block)thisItem.getStatement();
                        block.setIndenting(true);
                        creationContainer = thisItem;
                        break;
                    }
                }
            } else if (bestDefn != null) {
                creationContainer = bestDefn.getStatementContainer();
            } else if (commonScope != null && !commonScope.isEmpty()) {
                StatementContainer<StructuredStatement> testSwitch;
                if (commonScope.size() > 2 && (testSwitch = commonScope.get(commonScope.size() - 2)).getStatement() instanceof StructuredSwitch) {
                    if (this.defineInsideSwitchContent(scopedEntity, definitions, commonScope)) continue;
                    commonScope = commonScope.subList(0, commonScope.size() - 2);
                }
                creationContainer = commonScope.get(commonScope.size() - 1);
            }
            StatementContainer<StructuredStatement> statementContainer = hint = bestDefn == null ? null : bestDefn.localHint;
            if (creationContainer == null) continue;
            if (hint == null && commonScope != null && commonScope.size() == 1 && "<init>".equals(this.prototype.getName())) {
                hint = this.getNonInit(creationContainer);
            }
            creationContainer.getStatement().markCreator(scopedEntity, hint);
        }
    }

    private StatementContainer<StructuredStatement> getNonInit(StatementContainer<StructuredStatement> creationContainer) {
        int x;
        StructuredStatement stm = creationContainer.getStatement();
        if (!(stm instanceof Block)) {
            return null;
        }
        List<Op04StructuredStatement> content = ((Block)stm).getBlockStatements();
        int len = content.size() - 1;
        for (x = 0; x < len; ++x) {
            Expression e;
            StructuredStatement item = content.get(x).getStatement();
            if (item instanceof StructuredComment) continue;
            if (item instanceof StructuredExpressionStatement && ((e = ((StructuredExpressionStatement)item).getExpression()) instanceof MemberFunctionInvokation && ((MemberFunctionInvokation)e).isInitMethod() || e instanceof SuperFunctionInvokation)) break;
            return null;
        }
        return content.get(x + 1);
    }

    private boolean defineInsideSwitchContent(LValue scopedEntity, List<ScopeDefinition> definitions, List<StatementContainer<StructuredStatement>> commonScope) {
        int commonScopeSize = commonScope.size();
        Set usedPoints = SetFactory.newIdentitySet();
        List<ScopeDefinition> foundPoints = ListFactory.newList();
        for (ScopeDefinition def : definitions) {
            if (def.nestedScope.size() <= commonScopeSize) {
                return false;
            }
            StatementContainer innerDef = (StatementContainer)def.nestedScope.get(commonScopeSize);
            if (!((StructuredStatement)innerDef.getStatement()).canDefine(scopedEntity, this.factCache)) {
                return false;
            }
            if (!usedPoints.add(innerDef)) continue;
            foundPoints.add(def);
        }
        for (ScopeDefinition def : foundPoints) {
            StatementContainer stm = (StatementContainer)def.nestedScope.get(commonScopeSize);
            if (def.nestedScope.size() == commonScopeSize + 1 && def.exactStatement != null) {
                stm = def.exactStatement;
            }
            ((StructuredStatement)stm.getStatement()).markCreator(scopedEntity, stm);
        }
        return true;
    }

    private static <T> List<T> getCommonPrefix(List<T> a, List<T> b) {
        List<T> lb;
        List<T> la;
        if (a.size() < b.size()) {
            la = a;
            lb = b;
        } else {
            la = b;
            lb = a;
        }
        int maxRes = Math.min(la.size(), lb.size());
        int sameLen = 0;
        int x = 0;
        while (x < maxRes && la.get(x).equals(lb.get(x))) {
            ++x;
            ++sameLen;
        }
        if (sameLen == la.size()) {
            return la;
        }
        return la.subList(0, sameLen);
    }

    private JavaTypeInstance getUnclashedType(InferredJavaType inferredJavaType) {
        if (inferredJavaType.isClash()) {
            inferredJavaType.collapseTypeClash();
        }
        return inferredJavaType.getJavaTypeInstance();
    }

    class ScopeDefinition {
        private final int depth;
        private boolean immediate;
        private final List<StatementContainer<StructuredStatement>> nestedScope;
        private final StatementContainer<StructuredStatement> exactStatement;
        private final StatementContainer<StructuredStatement> localHint;
        private final LValue lValue;
        private final JavaTypeInstance lValueType;
        private final NamedVariable name;
        private final ScopeKey scopeKey;

        ScopeDefinition(int depth, Stack<StatementContainer<StructuredStatement>> nestedScope, StatementContainer<StructuredStatement> exactStatement, LValue lValue, InferredJavaType inferredJavaType, NamedVariable name) {
            this(depth, nestedScope, exactStatement, lValue, this$0.getUnclashedType(inferredJavaType), name, null, true);
        }

        StatementContainer<StructuredStatement> getExactStatement() {
            return this.exactStatement;
        }

        ScopeDefinition(int depth, Stack<StatementContainer<StructuredStatement>> nestedScope, StatementContainer<StructuredStatement> exactStatement, LValue lValue, JavaTypeInstance type, NamedVariable name, StatementContainer<StructuredStatement> hint, boolean immediate) {
            this.depth = depth;
            this.immediate = immediate;
            Pair<List<StatementContainer<StructuredStatement>>, StatementContainer<StructuredStatement>> adjustedScope = this.getBestScopeFor(lValue, nestedScope, exactStatement);
            this.nestedScope = adjustedScope.getFirst();
            this.exactStatement = adjustedScope.getSecond();
            this.lValue = lValue;
            this.lValueType = type;
            this.name = name;
            this.localHint = hint;
            this.scopeKey = new ScopeKey(lValue, type);
        }

        private Pair<List<StatementContainer<StructuredStatement>>, StatementContainer<StructuredStatement>> getBestScopeFor(LValue lValue, Collection<StatementContainer<StructuredStatement>> nestedScope, StatementContainer<StructuredStatement> exactStatement) {
            StatementContainer<StructuredStatement> scopeTest;
            if (nestedScope == null) {
                return Pair.make(null, exactStatement);
            }
            List<StatementContainer<StructuredStatement>> scope = ListFactory.newList(nestedScope);
            if (exactStatement != null && exactStatement.getStatement().alwaysDefines(lValue)) {
                return Pair.make(scope, exactStatement);
            }
            if (scope.isEmpty()) {
                return Pair.make(scope, exactStatement);
            }
            for (int x = scope.size() - 1; x >= 0 && !(scopeTest = scope.get(x)).getStatement().canDefine(lValue, AbstractLValueScopeDiscoverer.this.factCache); --x) {
                scope.remove(x);
            }
            if (scope.size() == nestedScope.size()) {
                return Pair.make(scope, exactStatement);
            }
            if (scope.isEmpty()) {
                return Pair.make(null, exactStatement);
            }
            exactStatement = scope.get(scope.size() - 1);
            return Pair.make(scope, exactStatement);
        }

        public JavaTypeInstance getJavaTypeInstance() {
            return this.lValueType;
        }

        public StatementContainer<StructuredStatement> getStatementContainer() {
            return this.exactStatement;
        }

        public LValue getlValue() {
            return this.lValue;
        }

        int getDepth() {
            return this.depth;
        }

        public NamedVariable getName() {
            return this.name;
        }

        ScopeKey getScopeKey() {
            return this.scopeKey;
        }

        List<StatementContainer<StructuredStatement>> getNestedScope() {
            return this.nestedScope;
        }

        public String toString() {
            return this.name + " : " + this.lValueType.getRawName();
        }

        boolean isImmediate() {
            return this.immediate;
        }

        void setImmediate() {
            this.immediate = true;
        }
    }

    private static class ScopeKey {
        private final LValue lValue;
        private final JavaTypeInstance type;

        private ScopeKey(LValue lValue, JavaTypeInstance type) {
            this.lValue = lValue;
            this.type = type;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            ScopeKey scopeKey = (ScopeKey)o;
            if (!this.lValue.equals(scopeKey.lValue)) {
                return false;
            }
            return this.type.equals(scopeKey.type);
        }

        private LValue getlValue() {
            return this.lValue;
        }

        public int hashCode() {
            int result = this.lValue.hashCode();
            result = 31 * result + this.type.hashCode();
            return result;
        }
    }
}

