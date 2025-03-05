/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.Op04Rewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.VariableNameTidier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.ClassCache;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class ScopeHidingVariableRewriter
implements Op04Rewriter {
    private final Method method;
    private final ClassCache classCache;
    private final Set<String> outerNames = SetFactory.newSet();
    private final Set<String> usedNames = SetFactory.newSet();
    private List<LocalVariable> collisions = ListFactory.newList();

    public ScopeHidingVariableRewriter(List<ClassFileField> fieldVariables, Method method, ClassCache classCache) {
        this.method = method;
        this.classCache = classCache;
        MethodPrototype prototype = method.getMethodPrototype();
        for (ClassFileField field : fieldVariables) {
            String fieldName = field.getFieldName();
            this.outerNames.add(fieldName);
            this.usedNames.add(fieldName);
        }
        if (prototype.parametersComputed()) {
            for (LocalVariable localVariable : prototype.getComputedParameters()) {
                this.checkCollision(localVariable);
            }
        }
    }

    private void checkCollision(LocalVariable localVariable) {
        String name = localVariable.getName().getStringName();
        if (this.outerNames.contains(name)) {
            this.collisions.add(localVariable);
        }
        this.usedNames.add(name);
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) {
            return;
        }
        for (StructuredStatement definition : structuredStatements) {
            List<LValue> createdHere = definition.findCreatedHere();
            if (createdHere == null) continue;
            for (LValue lValue : createdHere) {
                if (!(lValue instanceof LocalVariable)) continue;
                this.checkCollision((LocalVariable)lValue);
            }
        }
        if (this.collisions.isEmpty()) {
            return;
        }
        VariableNameTidier variableNameTidier = new VariableNameTidier(this.method, this.classCache);
        variableNameTidier.renameToAvoidHiding(this.usedNames, this.collisions);
    }
}

