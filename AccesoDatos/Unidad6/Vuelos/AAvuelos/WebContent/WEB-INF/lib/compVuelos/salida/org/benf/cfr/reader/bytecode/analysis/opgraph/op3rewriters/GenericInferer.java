/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class GenericInferer {
    private static GenericInferData getGtbNullFiltered(MemberFunctionInvokation m) {
        List<Expression> args = m.getArgs();
        GenericTypeBinder res = m.getMethodPrototype().getTypeBinderFor(args);
        List<Boolean> nulls = m.getNulls();
        if (args.size() != nulls.size()) {
            return new GenericInferData(res);
        }
        boolean found = false;
        for (Boolean b : nulls) {
            if (!b.booleanValue()) continue;
            found = true;
            break;
        }
        if (!found) {
            return new GenericInferData(res);
        }
        Set nullBindings = null;
        int len = args.size();
        for (int x = 0; x < len; ++x) {
            JavaGenericPlaceholderTypeInstance placeholder;
            JavaTypeInstance t2;
            JavaTypeInstance t;
            if (!nulls.get(x).booleanValue() || !((t = args.get(x).getInferredJavaType().getJavaTypeInstance()) instanceof JavaGenericPlaceholderTypeInstance) || !(t2 = res.getBindingFor(placeholder = (JavaGenericPlaceholderTypeInstance)t)).equals(placeholder)) continue;
            if (nullBindings == null) {
                nullBindings = SetFactory.newSet();
            }
            res.removeBinding(placeholder);
            nullBindings.add(placeholder);
        }
        return new GenericInferData(res, nullBindings);
    }

    public static void inferGenericObjectInfoFromCalls(List<Op03SimpleStatement> statements) {
        List memberFunctionInvokations = ListFactory.newList();
        for (Op03SimpleStatement statement : statements) {
            Expression e;
            Statement contained = statement.getStatement();
            if (contained instanceof ExpressionStatement) {
                e = ((ExpressionStatement)contained).getExpression();
                if (!(e instanceof MemberFunctionInvokation)) continue;
                memberFunctionInvokations.add((MemberFunctionInvokation)e);
                continue;
            }
            if (!(contained instanceof AssignmentSimple) || !((e = contained.getRValue()) instanceof MemberFunctionInvokation)) continue;
            memberFunctionInvokations.add((MemberFunctionInvokation)e);
        }
        if (memberFunctionInvokations.isEmpty()) {
            return;
        }
        TreeMap byTypKey = MapFactory.newTreeMap();
        Functional.groupToMapBy(memberFunctionInvokations, byTypKey, new UnaryFunction<MemberFunctionInvokation, Integer>(){

            @Override
            public Integer invoke(MemberFunctionInvokation arg) {
                return arg.getObject().getInferredJavaType().getLocalId();
            }
        });
        block1: for (Map.Entry entry : byTypKey.entrySet()) {
            GenericInferData inferData;
            JavaGenericBaseInstance genericType;
            Expression obj0;
            JavaTypeInstance objectType;
            List invokations = (List)entry.getValue();
            if (invokations.isEmpty() || !((objectType = (obj0 = ((MemberFunctionInvokation)invokations.get(0)).getObject()).getInferredJavaType().getJavaTypeInstance()) instanceof JavaGenericBaseInstance) || !(genericType = (JavaGenericBaseInstance)objectType).hasUnbound() || !(inferData = GenericInferer.getGtbNullFiltered((MemberFunctionInvokation)invokations.get(0))).isValid()) continue;
            int len = invokations.size();
            for (int x = 1; x < len; ++x) {
                GenericInferData inferData1 = GenericInferer.getGtbNullFiltered((MemberFunctionInvokation)invokations.get(x));
                if (!(inferData = inferData.mergeWith(inferData1)).isValid()) continue block1;
            }
            InferredJavaType inferredJavaType = obj0.getInferredJavaType();
            GenericTypeBinder typeBinder = inferData.getTypeBinder();
            inferredJavaType.deGenerify(typeBinder.getBindingFor(objectType));
        }
    }

    private static class GenericInferData {
        GenericTypeBinder binder;
        Set<JavaGenericPlaceholderTypeInstance> nullPlaceholders;

        private GenericInferData(GenericTypeBinder binder, Set<JavaGenericPlaceholderTypeInstance> nullPlaceholders) {
            this.binder = binder;
            this.nullPlaceholders = nullPlaceholders;
        }

        private GenericInferData(GenericTypeBinder binder) {
            this.binder = binder;
            this.nullPlaceholders = null;
        }

        public boolean isValid() {
            return this.binder != null;
        }

        GenericInferData mergeWith(GenericInferData other) {
            if (!this.isValid()) {
                return this;
            }
            if (!other.isValid()) {
                return other;
            }
            GenericTypeBinder newBinder = this.binder.mergeWith(other.binder, true);
            if (newBinder == null) {
                return new GenericInferData(null);
            }
            Set<JavaGenericPlaceholderTypeInstance> newNullPlaceHolders = SetUtil.originalIntersectionOrNull(this.nullPlaceholders, other.nullPlaceholders);
            return new GenericInferData(newBinder, newNullPlaceHolders);
        }

        GenericTypeBinder getTypeBinder() {
            if (this.nullPlaceholders != null && !this.nullPlaceholders.isEmpty()) {
                for (JavaGenericPlaceholderTypeInstance onlyNull : this.nullPlaceholders) {
                    this.binder.suggestOnlyNullBinding(onlyNull);
                }
            }
            return this.binder;
        }
    }
}

