/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.variables;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.analysis.variables.Ident;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamer;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.MapFactory;

public class VariableFactory {
    private final VariableNamer variableNamer;
    private final Map<Integer, InferredJavaType> typedArgs;
    private final Set<Integer> clashes;
    private final Method method;
    private int ignored;
    private final Map<LValue, LValue> cache = MapFactory.newMap();

    public VariableFactory(Method method, BytecodeMeta bytecodeMeta) {
        this.variableNamer = method.getVariableNamer();
        this.clashes = bytecodeMeta.getLivenessClashes();
        MethodPrototype methodPrototype = method.getMethodPrototype();
        List<JavaTypeInstance> args = methodPrototype.getArgs();
        this.typedArgs = MapFactory.newMap();
        int offset = 0;
        if (methodPrototype.isInstanceMethod()) {
            JavaTypeInstance thisType = method.getClassFile().getClassType();
            this.typedArgs.put(offset++, new InferredJavaType(thisType, InferredJavaType.Source.UNKNOWN, true));
        }
        for (JavaTypeInstance arg : args) {
            this.typedArgs.put(offset, new InferredJavaType(arg, InferredJavaType.Source.UNKNOWN, true));
            offset += arg.getStackType().getComputationCategory();
        }
        if (methodPrototype.parametersComputed()) {
            for (LocalVariable localVariable : methodPrototype.getComputedParameters()) {
                this.cache.put(localVariable, localVariable);
            }
        }
        this.method = method;
    }

    public JavaTypeInstance getReturn() {
        return this.method.getMethodPrototype().getReturnType();
    }

    public LValue ignoredVariable(InferredJavaType type) {
        LocalVariable res = new LocalVariable("cfr_ignored_" + this.ignored++, type);
        res.markIgnored();
        return res;
    }

    public LValue tempVariable(InferredJavaType type) {
        return new LocalVariable("cfr_temp_" + this.ignored++, type);
    }

    public LValue localVariable(int stackPosition, Ident ident, int origCodeRawOffset) {
        LocalVariable tmp;
        LValue val;
        InferredJavaType varType;
        if (ident == null) {
            ident = new Ident(stackPosition, -1);
        }
        InferredJavaType inferredJavaType = varType = ident.getIdx() == 0 ? this.typedArgs.get(stackPosition) : null;
        if (varType == null) {
            varType = new InferredJavaType(RawJavaType.VOID, InferredJavaType.Source.UNKNOWN);
        }
        if ((val = this.cache.get(tmp = new LocalVariable(stackPosition, ident, this.variableNamer, origCodeRawOffset, this.clashes.contains(stackPosition) && this.typedArgs.get(stackPosition) == null, varType))) == null) {
            this.cache.put(tmp, tmp);
            val = tmp;
        }
        return val;
    }

    public void mutatingRenameUnClash(LocalVariable toRename) {
        this.variableNamer.mutatingRenameUnClash(toRename.getName());
    }
}

