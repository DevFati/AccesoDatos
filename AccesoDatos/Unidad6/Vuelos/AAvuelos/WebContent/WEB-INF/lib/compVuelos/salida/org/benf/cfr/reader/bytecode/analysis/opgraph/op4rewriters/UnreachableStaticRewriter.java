/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.classfilehelpers.OverloadMethodSet;
import org.benf.cfr.reader.state.TypeUsageCollectingDumper;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.Predicate;

public class UnreachableStaticRewriter {
    public static void rewrite(ClassFile classFile, TypeUsageCollectingDumper typeUsage) {
        TypeUsageInformation info = typeUsage.getRealTypeUsageInformation();
        final JavaRefTypeInstance thisType = classFile.getRefClassType();
        if (thisType == null) {
            return;
        }
        Pair<List<JavaRefTypeInstance>, List<JavaRefTypeInstance>> split = Functional.partition(info.getUsedClassTypes(), new Predicate<JavaRefTypeInstance>(){

            @Override
            public boolean test(JavaRefTypeInstance in) {
                return in.getInnerClassHereInfo().isTransitiveInnerClassOf(thisType);
            }
        });
        List<JavaRefTypeInstance> inners = split.getFirst();
        Map potentialClashes = MapFactory.newMap();
        for (JavaRefTypeInstance inner : inners) {
            StringBuilder sb = new StringBuilder();
            inner.getInnerClassHereInfo().getFullInnerPath(sb);
            sb.append(inner.getRawShortName());
            String name = sb.toString();
            potentialClashes.put(name, inner);
        }
        List<JavaRefTypeInstance> others = split.getSecond();
        Map inaccessibles = MapFactory.newMap();
        for (JavaRefTypeInstance type : others) {
            JavaRefTypeInstance clashFqn = (JavaRefTypeInstance)potentialClashes.get(type.getRawName());
            JavaRefTypeInstance clashShort = (JavaRefTypeInstance)potentialClashes.get(type.getRawShortName());
            if (clashFqn == null || clashShort == null) continue;
            inaccessibles.put(type, new Inaccessible(type, clashShort, clashFqn));
        }
        if (inaccessibles.isEmpty()) {
            return;
        }
        Rewriter usr = new Rewriter(thisType, typeUsage, inaccessibles);
        ExpressionRewriterTransformer trans = new ExpressionRewriterTransformer(usr);
        for (Method method : classFile.getMethods()) {
            if (!method.hasCodeAttribute()) continue;
            Op04StructuredStatement code = method.getAnalysis();
            trans.transform(code);
        }
    }

    private static class Rewriter
    extends AbstractExpressionRewriter {
        private JavaRefTypeInstance thisType;
        private TypeUsageCollectingDumper typeUsageCollector;
        private final TypeUsageInformation typeUsageInformation;
        private Map<JavaTypeInstance, Inaccessible> inaccessibles;

        private Rewriter(JavaRefTypeInstance thisType, TypeUsageCollectingDumper typeUsage, Map<JavaTypeInstance, Inaccessible> inaccessibles) {
            this.thisType = thisType;
            this.typeUsageCollector = typeUsage;
            this.typeUsageInformation = typeUsage.getRealTypeUsageInformation();
            this.inaccessibles = inaccessibles;
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            StaticFunctionInvokation sfe;
            Inaccessible inaccessible;
            if (expression instanceof StaticFunctionInvokation && (inaccessible = this.inaccessibles.get((sfe = (StaticFunctionInvokation)expression).getClazz().getDeGenerifiedType())) != null && !this.available(sfe, inaccessible)) {
                this.typeUsageCollector.addStaticUsage(inaccessible.external, sfe.getName());
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        private boolean available(StaticFunctionInvokation sfe, Inaccessible inaccessible) {
            if (this.defines(this.thisType, sfe)) {
                return true;
            }
            if (this.defines(inaccessible.localInner, sfe)) {
                return true;
            }
            return this.defines(inaccessible.fakeFqnInner, sfe);
        }

        private boolean defines(JavaRefTypeInstance type, StaticFunctionInvokation sfe) {
            ClassFile classFile = type.getClassFile();
            if (classFile == null) {
                return true;
            }
            OverloadMethodSet oms = classFile.getOverloadMethodSet(sfe.getMethodPrototype());
            if (oms == null) {
                return true;
            }
            return oms.size() != 1;
        }
    }

    private static class Inaccessible {
        final JavaRefTypeInstance external;
        final JavaRefTypeInstance localInner;
        final JavaRefTypeInstance fakeFqnInner;

        Inaccessible(JavaRefTypeInstance external, JavaRefTypeInstance localInner, JavaRefTypeInstance fakeFqnInner) {
            this.external = external;
            this.localInner = localInner;
            this.fakeFqnInner = fakeFqnInner;
        }
    }
}

