/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ForIterStatement;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.SetFactory;

public class LoopLivenessClash {
    public static boolean detect(List<Op03SimpleStatement> statements, BytecodeMeta bytecodeMeta) {
        List<Op03SimpleStatement> iters = Functional.filter(statements, new TypeFilter<ForIterStatement>(ForIterStatement.class));
        if (iters.isEmpty()) {
            return false;
        }
        boolean found = false;
        for (Op03SimpleStatement iter : iters) {
            if (!LoopLivenessClash.detect(iter, bytecodeMeta)) continue;
            found = true;
        }
        return found;
    }

    private static JavaTypeInstance getIterableIterType(JavaTypeInstance type) {
        GenericTypeBinder typeBinder;
        if (!(type instanceof JavaGenericRefTypeInstance)) {
            return null;
        }
        JavaGenericRefTypeInstance generic = (JavaGenericRefTypeInstance)type;
        BindingSuperContainer bindingSuperContainer = type.getBindingSupers();
        JavaGenericRefTypeInstance iterType = bindingSuperContainer.getBoundSuperForBase(TypeConstants.ITERABLE);
        JavaGenericRefTypeInstance boundIterable = iterType.getBoundInstance(typeBinder = GenericTypeBinder.extractBindings(iterType, generic));
        List<JavaTypeInstance> iterBindings = boundIterable.getGenericTypes();
        if (iterBindings.size() != 1) {
            return null;
        }
        JavaTypeInstance iteratedType = iterBindings.get(0);
        return iteratedType;
    }

    private static boolean detect(Op03SimpleStatement statement, BytecodeMeta bytecodeMeta) {
        JavaTypeInstance listType;
        JavaTypeInstance listIterType;
        boolean res = false;
        ForIterStatement forIterStatement = (ForIterStatement)statement.getStatement();
        LValue iterator = forIterStatement.getCreatedLValue();
        if (!(iterator instanceof LocalVariable)) {
            return res;
        }
        JavaTypeInstance iterType = iterator.getInferredJavaType().getJavaTypeInstance();
        InferredJavaType inferredListType = forIterStatement.getList().getInferredJavaType();
        LValue hiddenList = forIterStatement.getHiddenList();
        if (hiddenList != null && hiddenList.getInferredJavaType().isClash() && hiddenList instanceof LocalVariable) {
            bytecodeMeta.informLivenessClashes(Collections.singleton(((LocalVariable)hiddenList).getIdx()));
            res = true;
        }
        if ((listIterType = (listType = inferredListType.getJavaTypeInstance()) instanceof JavaArrayTypeInstance ? listType.removeAnArrayIndirection() : LoopLivenessClash.getIterableIterType(listType)) == null) {
            return res;
        }
        if (iterType.equals(listIterType)) {
            return res;
        }
        if (listIterType instanceof JavaGenericPlaceholderTypeInstance) {
            bytecodeMeta.takeIteratedTypeHint(inferredListType, iterType);
            return res;
        }
        boolean listIterTypeRaw = listIterType.isRaw();
        if (listIterTypeRaw ^ iterType.isRaw()) {
            JavaTypeInstance oth;
            JavaTypeInstance raw = listIterTypeRaw ? listIterType : iterType;
            JavaTypeInstance javaTypeInstance = oth = listIterTypeRaw ? iterType : listIterType;
            if (raw == RawJavaType.getUnboxedTypeFor(oth)) {
                return res;
            }
        }
        LocalVariable lvIter = (LocalVariable)iterator;
        Set<Integer> clashes = SetFactory.newSet();
        clashes.add(lvIter.getIdx());
        bytecodeMeta.informLivenessClashes(clashes);
        return true;
    }
}

