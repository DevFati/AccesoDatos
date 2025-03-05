/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractAssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ForIterStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaWildcardTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.MapFactory;

public class NarrowingTypeRewriter {
    private static final JavaTypeInstance BAD_SENTINEL = new JavaWildcardTypeInstance(null, null);

    private static JavaTypeInstance getListType(Expression e) {
        if (e == null) {
            return BAD_SENTINEL;
        }
        JavaTypeInstance listtype = e.getInferredJavaType().getJavaTypeInstance();
        if (listtype instanceof JavaArrayTypeInstance) {
            return listtype.removeAnArrayIndirection();
        }
        return BAD_SENTINEL;
    }

    public static void rewrite(Method method, List<Op03SimpleStatement> statements) {
        LValueAssignmentCollector collector = new LValueAssignmentCollector();
        for (LocalVariable lv : method.getMethodPrototype().getComputedParameters()) {
            collector.collect(lv, BAD_SENTINEL);
        }
        for (Op03SimpleStatement statement : statements) {
            Statement stm = statement.getStatement();
            LValue created = stm.getCreatedLValue();
            if (created != null) {
                JavaTypeInstance type;
                Expression rValue = stm.getRValue();
                JavaTypeInstance javaTypeInstance = type = rValue == null ? BAD_SENTINEL : rValue.getInferredJavaType().getJavaTypeInstance();
                if (stm instanceof ForIterStatement) {
                    Expression list = ((ForIterStatement)stm).getList();
                    type = NarrowingTypeRewriter.getListType(list);
                }
                collector.collect(created, type);
            }
            stm.rewriteExpressions(collector, statement.getSSAIdentifiers());
        }
        Map<LocalVariable, JavaTypeInstance> updatable = collector.getUsable();
        for (Map.Entry<LocalVariable, JavaTypeInstance> entry : updatable.entrySet()) {
            LocalVariable lv = entry.getKey();
            JavaTypeInstance tgt = entry.getValue();
            InferredJavaType lvt = lv.getInferredJavaType();
            if (lvt.getJavaTypeInstance() != TypeConstants.OBJECT) continue;
            lvt.forceType(tgt, true);
        }
    }

    private static class LValueAssignmentCollector
    extends AbstractExpressionRewriter {
        private final Map<LocalVariable, JavaTypeInstance> usable = MapFactory.newMap();

        private LValueAssignmentCollector() {
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (expression instanceof AbstractAssignmentExpression) {
                AbstractAssignmentExpression aae = (AbstractAssignmentExpression)expression;
                LValue lValue = aae.getUpdatedLValue();
                this.collect(lValue, BAD_SENTINEL);
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        public void collect(LValue lValue, JavaTypeInstance type) {
            if (!(lValue instanceof LocalVariable)) {
                return;
            }
            LocalVariable lv = (LocalVariable)lValue;
            JavaTypeInstance b = this.usable.get(lv);
            if (type == null) {
                type = BAD_SENTINEL;
            }
            if (b == null) {
                this.usable.put(lv, type);
                return;
            }
            if (b != BAD_SENTINEL) {
                this.usable.put(lv, BAD_SENTINEL);
            }
        }

        Map<LocalVariable, JavaTypeInstance> getUsable() {
            Map<LocalVariable, JavaTypeInstance> res = MapFactory.newMap();
            for (Map.Entry<LocalVariable, JavaTypeInstance> entry : this.usable.entrySet()) {
                if (entry.getValue() == BAD_SENTINEL) continue;
                res.put(entry.getKey(), entry.getValue());
            }
            return res;
        }
    }
}

