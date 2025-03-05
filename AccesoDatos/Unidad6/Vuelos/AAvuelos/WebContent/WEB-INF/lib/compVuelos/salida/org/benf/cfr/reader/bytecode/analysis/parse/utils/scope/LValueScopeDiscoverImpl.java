/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils.scope;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ReadWrite;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.AbstractLValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.ScopeDiscoverInfoCache;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class LValueScopeDiscoverImpl
extends AbstractLValueScopeDiscoverer {
    private final boolean instanceOfDefines;

    public LValueScopeDiscoverImpl(Options options, MethodPrototype prototype, VariableFactory variableFactory, ClassFileVersion version) {
        super(options, prototype, variableFactory);
        this.instanceOfDefines = options.getOption(OptionsImpl.INSTANCEOF_PATTERN, version);
    }

    @Override
    public void collectLocalVariableAssignment(LocalVariable localVariable, StatementContainer<StructuredStatement> statementContainer, Expression value) {
        localVariable.getInferredJavaType().collapseTypeClash();
        NamedVariable name = localVariable.getName();
        AbstractLValueScopeDiscoverer.ScopeDefinition previousDef = (AbstractLValueScopeDiscoverer.ScopeDefinition)this.earliestDefinition.get(name);
        JavaTypeInstance newType = localVariable.getInferredJavaType().getJavaTypeInstance();
        if (previousDef == null || previousDef.getDepth() == this.currentDepth && previousDef.getExactStatement() != null && previousDef.getExactStatement().getStatement() instanceof StructuredCatch) {
            AbstractLValueScopeDiscoverer.ScopeDefinition scopeDefinition = new AbstractLValueScopeDiscoverer.ScopeDefinition(this.currentDepth, this.currentBlock, statementContainer, localVariable, newType, name, null, true);
            this.earliestDefinition.put(name, scopeDefinition);
            ((Map)this.earliestDefinitionsByLevel.get(this.currentDepth)).put(name, true);
            this.discoveredCreations.add(scopeDefinition);
            return;
        }
        JavaTypeInstance oldType = previousDef.getJavaTypeInstance();
        if (!oldType.equals(newType)) {
            ((Map)this.earliestDefinitionsByLevel.get(previousDef.getDepth())).remove(previousDef.getName());
            if (previousDef.getDepth() == this.currentDepth) {
                this.variableFactory.mutatingRenameUnClash(localVariable);
                name = localVariable.getName();
            }
            InferredJavaType inferredJavaType = localVariable.getInferredJavaType();
            AbstractLValueScopeDiscoverer.ScopeDefinition scopeDefinition = new AbstractLValueScopeDiscoverer.ScopeDefinition(this.currentDepth, this.currentBlock, statementContainer, localVariable, inferredJavaType, name);
            this.earliestDefinition.put(name, scopeDefinition);
            ((Map)this.earliestDefinitionsByLevel.get(this.currentDepth)).put(name, true);
            this.discoveredCreations.add(scopeDefinition);
        }
    }

    public boolean didDetectInstanceOfMatching() {
        if (!this.instanceOfDefines) {
            return false;
        }
        ScopeDiscoverInfoCache sdi = this.getFactCache();
        return sdi.anyFound();
    }

    @Override
    public void collect(LValue lValue, ReadWrite rw) {
        Class<?> lValueClass = lValue.getClass();
        if (lValueClass == LocalVariable.class) {
            LocalVariable localVariable = (LocalVariable)lValue;
            NamedVariable name = localVariable.getName();
            if (name.getStringName().equals("this")) {
                return;
            }
            AbstractLValueScopeDiscoverer.ScopeDefinition previousDef = (AbstractLValueScopeDiscoverer.ScopeDefinition)this.earliestDefinition.get(name);
            if (previousDef != null) {
                return;
            }
            InferredJavaType inferredJavaType = lValue.getInferredJavaType();
            AbstractLValueScopeDiscoverer.ScopeDefinition scopeDefinition = new AbstractLValueScopeDiscoverer.ScopeDefinition(this.currentDepth, this.currentBlock, (StatementContainer)this.currentBlock.peek(), lValue, inferredJavaType, name);
            this.earliestDefinition.put(name, scopeDefinition);
            ((Map)this.earliestDefinitionsByLevel.get(this.currentDepth)).put(name, true);
            this.discoveredCreations.add(scopeDefinition);
        } else if (lValueClass == FieldVariable.class) {
            lValue.collectLValueUsage(this);
        }
    }

    @Override
    public boolean descendLambdas() {
        return false;
    }

    @Override
    public boolean ifCanDefine() {
        return this.instanceOfDefines;
    }
}

