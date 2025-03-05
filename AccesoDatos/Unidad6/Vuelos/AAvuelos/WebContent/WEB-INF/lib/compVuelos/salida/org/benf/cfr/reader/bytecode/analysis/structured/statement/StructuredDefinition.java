/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class StructuredDefinition
extends AbstractStructuredStatement {
    private LValue scopedEntity;

    public StructuredDefinition(LValue scopedEntity) {
        super(BytecodeLoc.NONE);
        this.scopedEntity = scopedEntity;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        this.scopedEntity.collectTypeUsages(collector);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        ClassFile classFile;
        JavaTypeInstance type;
        Class<?> clazz = this.scopedEntity.getClass();
        if (clazz == LocalVariable.class) {
            return LValue.Creation.dump(dumper, this.scopedEntity).endCodeln();
        }
        if (clazz == SentinelLocalClassLValue.class && (type = ((SentinelLocalClassLValue)this.scopedEntity).getLocalClassType().getDeGenerifiedType()) instanceof JavaRefTypeInstance && (classFile = ((JavaRefTypeInstance)type).getClassFile()) != null) {
            return classFile.dumpAsInlineClass(dumper);
        }
        return dumper;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.getLoc();
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
    }

    public LValue getLvalue() {
        return this.scopedEntity;
    }

    @Override
    public List<LValue> findCreatedHere() {
        return ListFactory.newImmutableList(this.scopedEntity);
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!this.equals(o)) {
            return false;
        }
        matchIterator.advance();
        return true;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof StructuredDefinition)) {
            return false;
        }
        StructuredDefinition other = (StructuredDefinition)o;
        return this.scopedEntity.equals(other.scopedEntity);
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
    }
}

