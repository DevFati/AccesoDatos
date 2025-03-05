/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.variables.Keywords;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.ClassCache;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class VariableNameTidier
implements StructuredStatementTransformer {
    private final Method method;
    private boolean classRenamed = false;
    private final JavaTypeInstance ownerClassType;
    private final Set<String> bannedNames;
    private final ClassCache classCache;

    public VariableNameTidier(Method method, Set<String> bannedNames, ClassCache classCache) {
        this.method = method;
        this.ownerClassType = method.getClassFile().getClassType();
        this.bannedNames = bannedNames;
        this.classCache = classCache;
    }

    public VariableNameTidier(Method method, ClassCache classCache) {
        this(method, new HashSet<String>(), classCache);
    }

    public void transform(Op04StructuredStatement root) {
        StructuredScopeWithVars structuredScopeWithVars = new StructuredScopeWithVars();
        structuredScopeWithVars.add(null);
        List<LocalVariable> params = this.method.getMethodPrototype().getComputedParameters();
        for (LocalVariable param : params) {
            structuredScopeWithVars.defineHere(null, param);
        }
        root.transform(this, structuredScopeWithVars);
    }

    public void renameToAvoidHiding(Set<String> avoid, List<LocalVariable> collisions) {
        StructuredScopeWithVars structuredScopeWithVars = new StructuredScopeWithVars();
        structuredScopeWithVars.add(null);
        structuredScopeWithVars.markInitiallyDefined(avoid);
        for (LocalVariable hider : collisions) {
            structuredScopeWithVars.defineHere(hider);
        }
    }

    public boolean isClassRenamed() {
        return this.classRenamed;
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        StructuredScopeWithVars structuredScopeWithVars = (StructuredScopeWithVars)scope;
        List<LValue> definedHere = in.findCreatedHere();
        if (definedHere != null) {
            for (LValue scopedEntity : definedHere) {
                if (scopedEntity instanceof LocalVariable) {
                    structuredScopeWithVars.defineHere(in, (LocalVariable)scopedEntity);
                }
                if (!(scopedEntity instanceof SentinelLocalClassLValue)) continue;
                structuredScopeWithVars.defineLocalClassHere((SentinelLocalClassLValue)scopedEntity);
            }
        }
        NameSimplifier simplifier = new NameSimplifier(this.ownerClassType, structuredScopeWithVars);
        in.rewriteExpressions(simplifier);
        in.transformStructuredChildren(this, scope);
        return in;
    }

    private class StructuredScopeWithVars
    extends StructuredScope {
        private final LinkedList<AtLevel> scope = ListFactory.newLinkedList();
        private final Map<String, Integer> nextPostFixed = MapFactory.newLazyMap(new UnaryFunction<String, Integer>(){

            @Override
            public Integer invoke(String arg) {
                return 2;
            }
        });

        private StructuredScopeWithVars() {
        }

        @Override
        public void remove(StructuredStatement statement) {
            super.remove(statement);
            this.scope.removeFirst();
        }

        @Override
        public void add(StructuredStatement statement) {
            super.add(statement);
            this.scope.addFirst(new AtLevel(statement));
        }

        private boolean alreadyDefined(String name) {
            return this.alreadyDefined(name, true);
        }

        private boolean alreadyDefined(String name, boolean checkClassCache) {
            if (VariableNameTidier.this.bannedNames.contains(name)) {
                return true;
            }
            for (AtLevel atLevel : this.scope) {
                if (!atLevel.isDefinedHere(name)) continue;
                return true;
            }
            return checkClassCache && VariableNameTidier.this.classCache.isClassName(name);
        }

        private String getNext(String base) {
            int postfix = this.nextPostFixed.get(base);
            this.nextPostFixed.put(base, postfix + 1);
            return base + postfix;
        }

        private String suggestByType(LocalVariable localVariable) {
            JavaTypeInstance type = localVariable.getInferredJavaType().getJavaTypeInstance();
            RawJavaType raw = RawJavaType.getUnboxedTypeFor(type);
            if (raw != null) {
                type = raw;
            }
            return type.suggestVarName();
        }

        private String mkLcMojo(String in) {
            return " class!" + in;
        }

        void defineLocalClassHere(SentinelLocalClassLValue localVariable) {
            String postfixedVarName;
            String lcMojo;
            JavaTypeInstance type = localVariable.getLocalClassType();
            String name = type.suggestVarName();
            if (name == null) {
                name = type.getRawName().replace('.', '_');
            }
            char[] chars = name.toCharArray();
            int len = chars.length;
            for (int idx = 0; idx < len; ++idx) {
                char c = chars[idx];
                if (c >= '0' && c <= '9') continue;
                chars[idx] = Character.toUpperCase(chars[idx]);
                name = new String(chars, idx, chars.length - idx);
                break;
            }
            if (!this.alreadyDefined(lcMojo = this.mkLcMojo(name))) {
                this.scope.getFirst().defineHere(lcMojo);
                VariableNameTidier.this.method.markUsedLocalClassType(type, name);
                return;
            }
            while (this.alreadyDefined(this.mkLcMojo(postfixedVarName = this.getNext(name)))) {
            }
            this.scope.getFirst().defineHere(this.mkLcMojo(postfixedVarName));
            VariableNameTidier.this.method.markUsedLocalClassType(type, postfixedVarName);
            VariableNameTidier.this.classRenamed = true;
        }

        void defineHere(StructuredStatement statement, LocalVariable localVariable) {
            String stringName;
            boolean illegalUnderscore;
            NamedVariable namedVariable = localVariable.getName();
            boolean bl = illegalUnderscore = namedVariable.getStringName().equals("_") && VariableNameTidier.this.method.getClassFile().getClassFileVersion().equalOrLater(ClassFileVersion.JAVA_9);
            if (!namedVariable.isGoodName() || illegalUnderscore) {
                String suggestion = null;
                if (statement != null) {
                    suggestion = statement.suggestName(localVariable, new Predicate<String>(){

                        @Override
                        public boolean test(String in) {
                            return StructuredScopeWithVars.this.alreadyDefined(in);
                        }
                    });
                }
                if (suggestion == null) {
                    suggestion = this.suggestByType(localVariable);
                }
                if (suggestion != null) {
                    if (suggestion.length() == 1 && suggestion.toUpperCase().equals(suggestion)) {
                        suggestion = suggestion.toLowerCase();
                    }
                    namedVariable.forceName(suggestion);
                }
            }
            if (Keywords.isAKeyword(stringName = namedVariable.getStringName())) {
                namedVariable.forceName(Keywords.getReplacement(stringName));
            }
            this.defineHere(localVariable);
        }

        void markInitiallyDefined(Set<String> names) {
            for (String name : names) {
                this.scope.getFirst().defineHere(name);
            }
        }

        boolean isDefined(String anyNameType) {
            return this.alreadyDefined(anyNameType, false);
        }

        void defineHere(LocalVariable localVariable) {
            String postfixedVarName;
            NamedVariable namedVariable = localVariable.getName();
            String base = namedVariable.getStringName();
            if (!this.alreadyDefined(base)) {
                this.scope.getFirst().defineHere(base);
                return;
            }
            while (this.alreadyDefined(postfixedVarName = this.getNext(base))) {
            }
            localVariable.getName().forceName(postfixedVarName);
            this.scope.getFirst().defineHere(postfixedVarName);
        }

        protected class AtLevel {
            StructuredStatement statement;
            Set<String> definedHere = SetFactory.newSet();
            int next;

            private AtLevel(StructuredStatement statement) {
                this.statement = statement;
                this.next = 0;
            }

            public String toString() {
                return this.statement.toString();
            }

            boolean isDefinedHere(String name) {
                return this.definedHere.contains(name);
            }

            void defineHere(String name) {
                this.definedHere.add(name);
            }
        }
    }

    private static class NameSimplifier
    extends AbstractExpressionRewriter {
        private final StructuredScopeWithVars localScope;
        private final JavaTypeInstance ownerClassType;

        private NameSimplifier(JavaTypeInstance ownerClassType, StructuredScopeWithVars localScope) {
            this.ownerClassType = ownerClassType;
            this.localScope = localScope;
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            StaticVariable staticVariable;
            String fieldName;
            if (lValue.getClass() == StaticVariable.class && !this.localScope.isDefined(fieldName = (staticVariable = (StaticVariable)lValue).getFieldName())) {
                JavaTypeInstance owningClassType = staticVariable.getOwningClassType();
                if (owningClassType.equals(this.ownerClassType)) {
                    if (!"class".equals(fieldName)) {
                        return staticVariable.getSimpleCopy();
                    }
                } else {
                    JavaRefTypeInstance clazz;
                    ClassFile classFile;
                    InnerClassInfo innerClassInfo = this.ownerClassType.getInnerClassHereInfo();
                    while (innerClassInfo.isInnerClass() && (classFile = (clazz = innerClassInfo.getOuterClass()).getClassFile()) != null) {
                        if (owningClassType.equals(clazz)) {
                            return staticVariable.getSimpleCopy();
                        }
                        if (classFile.hasLocalField(fieldName)) break;
                        innerClassInfo = clazz.getInnerClassHereInfo();
                    }
                }
            }
            return lValue;
        }
    }

    public static class NameDiscoverer
    extends AbstractExpressionRewriter
    implements StructuredStatementTransformer {
        private final Set<String> usedNames = SetFactory.newSet();
        private static final Set<String> EMPTY = SetFactory.newSet();

        private NameDiscoverer() {
        }

        private void addLValues(Collection<LValue> definedHere) {
            if (definedHere == null) {
                return;
            }
            for (LValue scopedEntity : definedHere) {
                if (!(scopedEntity instanceof LocalVariable)) continue;
                NamedVariable namedVariable = ((LocalVariable)scopedEntity).getName();
                this.usedNames.add(namedVariable.getStringName());
            }
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.rewriteExpressions(this);
            in.transformStructuredChildren(this, scope);
            return in;
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (expression instanceof LambdaExpression) {
                this.addLValues(((LambdaExpression)expression).getArgs());
                return expression;
            }
            return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        }

        public static Set<String> getUsedLambdaNames(BytecodeMeta bytecodeMeta, Op04StructuredStatement in) {
            if (!bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.USES_INVOKEDYNAMIC)) {
                return EMPTY;
            }
            NameDiscoverer discoverer = new NameDiscoverer();
            in.transform(discoverer, new StructuredScope());
            return discoverer.usedNames;
        }
    }
}

