/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import java.util.Map;
import org.benf.cfr.reader.bytecode.AnonymousClassUsage;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class ObjectTypeUsageRewriter
extends AbstractExpressionRewriter
implements StructuredStatementTransformer {
    private final Map<InferredJavaType, Boolean> isAnonVar = MapFactory.newIdentityMap();
    private boolean canHaveVar;

    public ObjectTypeUsageRewriter(AnonymousClassUsage anonymousClassUsage, ClassFile classFile) {
        this.canHaveVar = !anonymousClassUsage.isEmpty() && classFile.getClassFileVersion().equalOrLater(ClassFileVersion.JAVA_10);
    }

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        in.rewriteExpressions(this);
        return in;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression instanceof MemberFunctionInvokation) {
            expression = this.handleMemberFunction((MemberFunctionInvokation)expression);
        }
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (lValue instanceof FieldVariable) {
            lValue = this.handleFieldVariable((FieldVariable)lValue);
        }
        return super.rewriteExpression(lValue, ssaIdentifiers, statementContainer, flags);
    }

    private boolean needsReWrite(Expression lhsObject, JavaTypeInstance owningClassType, UnaryFunction<ClassFile, Boolean> checkVisible) {
        ClassFile classFile;
        ClassFile owningClassFile;
        if (owningClassType == null) {
            return false;
        }
        InferredJavaType ijtObject = lhsObject.getInferredJavaType();
        if (this.canHaveVar && !this.isAnonVar.containsKey(ijtObject)) {
            boolean isAnon = owningClassType.getInnerClassHereInfo().isAnonymousClass();
            if (isAnon) {
                ijtObject.confirmVarIfPossible();
                this.markLocalVar(lhsObject);
            }
            this.isAnonVar.put(ijtObject, isAnon);
        }
        JavaTypeInstance jtObj = ijtObject.getJavaTypeInstance();
        if (owningClassType.getInnerClassHereInfo().isAnonymousClass() || jtObj == owningClassType) {
            return false;
        }
        if ((owningClassType = owningClassType.getDeGenerifiedType()) instanceof JavaRefTypeInstance && (owningClassFile = ((JavaRefTypeInstance)owningClassType).getClassFile()) != null && owningClassFile.isInterface()) {
            return false;
        }
        JavaTypeInstance currentAsWas = jtObj.getDeGenerifiedType();
        BindingSuperContainer bindingSupers = currentAsWas.getBindingSupers();
        if (bindingSupers != null && !bindingSupers.containsBase(owningClassType)) {
            return true;
        }
        do {
            if (currentAsWas.equals(owningClassType)) {
                return false;
            }
            JavaTypeInstance current = currentAsWas;
            if (!(current instanceof JavaRefTypeInstance)) {
                return false;
            }
            classFile = ((JavaRefTypeInstance)current).getClassFile();
            if (classFile != null) continue;
            return false;
        } while (!checkVisible.invoke(classFile).booleanValue() && (currentAsWas = classFile.getBaseClassType().getDeGenerifiedType()) != null);
        return currentAsWas != null && !currentAsWas.equals(owningClassType);
    }

    private Expression handleMemberFunction(final MemberFunctionInvokation funcInv) {
        Expression lhsObject = funcInv.getObject();
        JavaTypeInstance owningClassType = funcInv.getMethodPrototype().getClassType();
        if (owningClassType == null) {
            return funcInv;
        }
        class MemberCheck
        implements UnaryFunction<ClassFile, Boolean> {
            MemberCheck() {
            }

            @Override
            public Boolean invoke(ClassFile classFile) {
                return classFile.getMethodByPrototypeOrNull(funcInv.getMethodPrototype()) != null;
            }
        }
        if (!this.needsReWrite(lhsObject, owningClassType, new MemberCheck())) {
            return funcInv;
        }
        return funcInv.withReplacedObject(new CastExpression(BytecodeLoc.NONE, new InferredJavaType(owningClassType, InferredJavaType.Source.FORCE_TARGET_TYPE), lhsObject));
    }

    private LValue handleFieldVariable(final FieldVariable fieldVariable) {
        JavaTypeInstance owningClassType;
        Expression lhsObject = fieldVariable.getObject();
        class FieldCheck
        implements UnaryFunction<ClassFile, Boolean> {
            FieldCheck() {
            }

            @Override
            public Boolean invoke(ClassFile classFile) {
                return classFile.hasLocalField(fieldVariable.getFieldName());
            }
        }
        if (!this.needsReWrite(lhsObject, owningClassType = fieldVariable.getOwningClassType(), new FieldCheck())) {
            return fieldVariable;
        }
        return fieldVariable.withReplacedObject(new CastExpression(BytecodeLoc.NONE, new InferredJavaType(owningClassType, InferredJavaType.Source.FORCE_TARGET_TYPE), lhsObject));
    }

    private void markLocalVar(Expression object) {
        if (!(object instanceof LValueExpression)) {
            return;
        }
        LValue lValue = ((LValueExpression)object).getLValue();
        lValue.markVar();
    }
}

