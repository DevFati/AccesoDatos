/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.AnonymousClassUsage;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.InstrIndex;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewObject;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AbstractStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.stack.StackEntry;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class CreationCollector {
    private final List<Pair<LValue, StatementPair<MemberFunctionInvokation>>> collectedConstructions = ListFactory.newList();
    private final Map<LValue, List<StatementContainer>> collectedCreations = MapFactory.newLazyMap(new UnaryFunction<LValue, List<StatementContainer>>(){

        @Override
        public List<StatementContainer> invoke(LValue arg) {
            return ListFactory.newList();
        }
    });
    private final AnonymousClassUsage anonymousClassUsage;

    public CreationCollector(AnonymousClassUsage anonymousClassUsage) {
        this.anonymousClassUsage = anonymousClassUsage;
    }

    public void collectCreation(LValue lValue, Expression rValue, StatementContainer container) {
        if (!(rValue instanceof NewObject)) {
            return;
        }
        if (!(lValue instanceof StackSSALabel) && !(lValue instanceof LocalVariable)) {
            return;
        }
        this.collectedCreations.get(lValue).add(container);
    }

    public void collectConstruction(Expression expression, MemberFunctionInvokation rValue, StatementContainer container) {
        if (expression instanceof StackValue) {
            StackSSALabel lValue = ((StackValue)expression).getStackValue();
            this.markConstruction(lValue, rValue, container);
            return;
        }
        if (expression instanceof LValueExpression) {
            LValue lValue = ((LValueExpression)expression).getLValue();
            this.markConstruction(lValue, rValue, container);
            return;
        }
        if (expression instanceof NewObject) {
            this.markConstruction(null, rValue, container);
            return;
        }
    }

    private void markConstruction(LValue lValue, MemberFunctionInvokation rValue, StatementContainer container) {
        this.collectedConstructions.add(Pair.make(lValue, new StatementPair(rValue, container)));
    }

    public void condenseConstructions(Method method, DCCommonState dcCommonState) {
        LValue lValue;
        Map constructionTargets = MapFactory.newMap();
        for (Pair<LValue, StatementPair<MemberFunctionInvokation>> pair : this.collectedConstructions) {
            lValue = pair.getFirst();
            StatementPair<MemberFunctionInvokation> constructionValue = pair.getSecond();
            if (constructionValue == null) continue;
            InstrIndex idx = ((StatementPair)constructionValue).getLocation().getIndex();
            if (lValue != null) {
                if (!this.collectedCreations.containsKey(lValue)) continue;
                List<StatementContainer> creations = this.collectedCreations.get(lValue);
                boolean found = false;
                for (StatementContainer creation : creations) {
                    if (!creation.getIndex().isBackJumpFrom(idx)) continue;
                    found = true;
                    break;
                }
                if (!found) continue;
            }
            MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation)((StatementPair)constructionValue).getValue();
            JavaTypeInstance lValueType = memberFunctionInvokation.getClassTypeInstance();
            InferredJavaType inferredJavaType = lValue == null ? memberFunctionInvokation.getInferredJavaType() : lValue.getInferredJavaType();
            AbstractExpression constructorInvokation = null;
            InnerClassInfo innerClassInfo = lValueType.getInnerClassHereInfo();
            if (innerClassInfo.isAnonymousClass()) {
                ConstructorInvokationAnonymousInner constructorInvokationAnonymousInner = new ConstructorInvokationAnonymousInner(BytecodeLoc.NONE, memberFunctionInvokation, inferredJavaType, memberFunctionInvokation.getArgs(), dcCommonState, lValueType);
                constructorInvokation = constructorInvokationAnonymousInner;
                ClassFile classFile = constructorInvokationAnonymousInner.getClassFile();
                if (classFile != null) {
                    this.anonymousClassUsage.note(classFile, constructorInvokationAnonymousInner);
                    JavaTypeInstance anonymousTypeBase = ClassFile.getAnonymousTypeBase(classFile);
                    inferredJavaType.forceDelegate(new InferredJavaType(anonymousTypeBase, InferredJavaType.Source.UNKNOWN));
                    if (classFile.getClassFileVersion().equalOrLater(ClassFileVersion.JAVA_10)) {
                        inferredJavaType.shallowSetCanBeVar();
                    }
                } else {
                    BindingSuperContainer bindingSuperContainer = lValueType.getBindingSupers();
                    if (bindingSuperContainer != null) {
                        JavaTypeInstance bestGuess = bindingSuperContainer.getMostLikelyAnonymousType(lValueType);
                        inferredJavaType.forceDelegate(new InferredJavaType(bestGuess, InferredJavaType.Source.UNKNOWN));
                    }
                }
            }
            if (constructorInvokation == null) {
                InferredJavaType constructionType = new InferredJavaType(lValueType, InferredJavaType.Source.CONSTRUCTOR);
                if (inferredJavaType.getJavaTypeInstance() instanceof JavaGenericBaseInstance) {
                    constructionType = inferredJavaType;
                }
                ConstructorInvokationSimple cis = new ConstructorInvokationSimple(BytecodeLoc.NONE, memberFunctionInvokation, inferredJavaType, constructionType, memberFunctionInvokation.getArgs());
                constructorInvokation = cis;
                if (innerClassInfo.isMethodScopedClass()) {
                    method.markUsedLocalClassType(lValueType);
                    try {
                        ClassFile cls = dcCommonState.getClassFile(lValueType);
                        this.anonymousClassUsage.noteMethodClass(cls, cis);
                    }
                    catch (CannotLoadClassException cls) {
                        // empty catch block
                    }
                }
            }
            AbstractStatement replacement = null;
            if (lValue == null) {
                replacement = new ExpressionStatement(constructorInvokation);
            } else {
                replacement = new AssignmentSimple(constructorInvokation.getLoc(), lValue, constructorInvokation);
                if (lValue instanceof StackSSALabel) {
                    StackSSALabel stackSSALabel = (StackSSALabel)lValue;
                    StackEntry stackEntry = stackSSALabel.getStackEntry();
                    stackEntry.decrementUsage();
                    stackEntry.incSourceCount();
                }
            }
            StatementContainer constructionContainer = ((StatementPair)constructionValue).getLocation();
            constructionContainer.replaceStatement(replacement);
            if (lValue == null) continue;
            constructionTargets.put(lValue, constructionContainer);
        }
        for (Map.Entry entry : this.collectedCreations.entrySet()) {
            lValue = (LValue)entry.getKey();
            for (StatementContainer statementContainer : (List)entry.getValue()) {
                StatementContainer x;
                if (lValue instanceof StackSSALabel) {
                    StackEntry stackEntry = ((StackSSALabel)lValue).getStackEntry();
                    stackEntry.decSourceCount();
                }
                if ((x = (StatementContainer)constructionTargets.get(lValue)) != null) {
                    this.moveDupPostCreation(lValue, statementContainer, x);
                }
                statementContainer.nopOut();
            }
        }
    }

    private void moveDupPostCreation(LValue lValue, StatementContainer oldCreation, StatementContainer oldConstruction) {
        Op03SimpleStatement constr = (Op03SimpleStatement)oldConstruction;
        Op03SimpleStatement creatr = (Op03SimpleStatement)oldCreation;
        Op03SimpleStatement cretgt = creatr.getTargets().get(0);
        if (constr == cretgt) {
            return;
        }
        if (cretgt.getSources().size() != 1) {
            return;
        }
        Statement s = cretgt.getStatement();
        if (s instanceof ExpressionStatement) {
            Expression e = ((ExpressionStatement)s).getExpression();
            if (!(e instanceof StackValue)) {
                return;
            }
        } else if (s instanceof AssignmentSimple) {
            Expression rv = s.getRValue();
            if (!(rv instanceof StackValue)) {
                return;
            }
            if (!((StackValue)rv).getStackValue().equals(lValue)) {
                return;
            }
            if (!(s.getCreatedLValue() instanceof StackSSALabel)) {
                return;
            }
        } else {
            return;
        }
        cretgt.splice(constr);
    }

    private static class StatementPair<X> {
        private final X value;
        private final StatementContainer location;

        private StatementPair(X value, StatementContainer location) {
            this.value = value;
            this.location = location;
        }

        private X getValue() {
            return this.value;
        }

        private StatementContainer getLocation() {
            return this.location;
        }
    }
}

