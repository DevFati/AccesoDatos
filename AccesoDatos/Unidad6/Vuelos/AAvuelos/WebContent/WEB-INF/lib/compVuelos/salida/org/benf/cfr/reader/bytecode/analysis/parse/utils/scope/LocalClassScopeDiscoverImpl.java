/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils.scope;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.AbstractLValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.AbstractTypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.output.Dumper;

public class LocalClassScopeDiscoverImpl
extends AbstractLValueScopeDiscoverer {
    private final Map<JavaTypeInstance, Boolean> localClassTypes = MapFactory.newIdentityMap();
    private final TypeUsageSpotter typeUsageSpotter = new TypeUsageSpotter();
    private final JavaTypeInstance scopeType;

    public LocalClassScopeDiscoverImpl(Options options, Method method, VariableFactory variableFactory) {
        super(options, method.getMethodPrototype(), variableFactory);
        InnerClassInfo innerClassInfo;
        this.scopeType = method.getMethodPrototype().getClassType();
        JavaTypeInstance thisClassType = method.getClassFile().getClassType();
        while (thisClassType != null && null == this.localClassTypes.put(thisClassType, Boolean.FALSE) && (innerClassInfo = thisClassType.getInnerClassHereInfo()).isInnerClass()) {
            thisClassType = innerClassInfo.getOuterClass();
        }
    }

    @Override
    public void processOp04Statement(Op04StructuredStatement statement) {
        statement.getStatement().collectTypeUsages(this.typeUsageSpotter);
        super.processOp04Statement(statement);
    }

    @Override
    public void collectLocalVariableAssignment(LocalVariable localVariable, StatementContainer<StructuredStatement> statementContainer, Expression value) {
        this.collect(localVariable, ReadWrite.WRITE);
    }

    @Override
    public void collect(LValue lValue, ReadWrite rw) {
        Class<?> lValueClass = lValue.getClass();
        if (lValueClass == SentinelLocalClassLValue.class) {
            SentinelLocalClassLValue localClassLValue = (SentinelLocalClassLValue)lValue;
            JavaTypeInstance type = localClassLValue.getLocalClassType();
            if (type.getDeGenerifiedType() == this.scopeType) {
                return;
            }
            this.defineHere(lValue, type, true);
        } else if (lValueClass == FieldVariable.class) {
            lValue.collectLValueUsage(this);
        }
    }

    private void defineHere(LValue lValue, JavaTypeInstance type, boolean immediate) {
        SentinelNV name = new SentinelNV(type);
        SentinelNV keyName = new SentinelNV(type.getDeGenerifiedType());
        AbstractLValueScopeDiscoverer.ScopeDefinition previousDef = (AbstractLValueScopeDiscoverer.ScopeDefinition)this.earliestDefinition.get(keyName);
        if (previousDef != null) {
            if (previousDef.isImmediate() || !immediate) {
                return;
            }
            if (previousDef.getDepth() < this.currentDepth) {
                previousDef.setImmediate();
                return;
            }
            if (!previousDef.isImmediate()) {
                ((Map)this.earliestDefinitionsByLevel.get(this.currentDepth)).remove(keyName);
                this.discoveredCreations.remove(previousDef);
            }
        }
        AbstractLValueScopeDiscoverer.ScopeDefinition scopeDefinition = new AbstractLValueScopeDiscoverer.ScopeDefinition(this.currentDepth, this.currentBlock, (StatementContainer)this.currentBlock.peek(), lValue, type, name, this.currentMark, immediate);
        this.earliestDefinition.put(keyName, scopeDefinition);
        ((Map)this.earliestDefinitionsByLevel.get(this.currentDepth)).put(keyName, true);
        this.discoveredCreations.add(scopeDefinition);
        this.localClassTypes.put(type, Boolean.TRUE);
    }

    @Override
    public boolean descendLambdas() {
        return true;
    }

    class TypeUsageSpotter
    extends AbstractTypeUsageCollector {
        TypeUsageSpotter() {
        }

        @Override
        public void collectRefType(JavaRefTypeInstance type) {
            this.collect(type);
        }

        @Override
        public void collect(JavaTypeInstance type) {
            if (type == null) {
                return;
            }
            Boolean localClass = (Boolean)LocalClassScopeDiscoverImpl.this.localClassTypes.get(type);
            if (localClass == null) {
                localClass = ConstructorInvokationSimple.isAnonymousMethodType(type);
                LocalClassScopeDiscoverImpl.this.localClassTypes.put(type, localClass);
            }
            if (localClass == Boolean.FALSE) {
                return;
            }
            SentinelLocalClassLValue sentinel = new SentinelLocalClassLValue(type);
            LocalClassScopeDiscoverImpl.this.defineHere(sentinel, type, false);
        }

        @Override
        public TypeUsageInformation getTypeUsageInformation() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isStatementRecursive() {
            return false;
        }
    }

    private static class SentinelNV
    implements NamedVariable {
        private final JavaTypeInstance typeInstance;

        private SentinelNV(JavaTypeInstance typeInstance) {
            this.typeInstance = typeInstance;
        }

        @Override
        public void forceName(String name) {
        }

        @Override
        public String getStringName() {
            return this.typeInstance.getRawName();
        }

        @Override
        public boolean isGoodName() {
            return true;
        }

        @Override
        public Dumper dump(Dumper d) {
            return null;
        }

        @Override
        public Dumper dump(Dumper d, boolean defines) {
            return null;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            SentinelNV that = (SentinelNV)o;
            return !(this.typeInstance != null ? !this.typeInstance.equals(that.typeInstance) : that.typeInstance != null);
        }

        public int hashCode() {
            return this.typeInstance != null ? this.typeInstance.hashCode() : 0;
        }
    }
}

