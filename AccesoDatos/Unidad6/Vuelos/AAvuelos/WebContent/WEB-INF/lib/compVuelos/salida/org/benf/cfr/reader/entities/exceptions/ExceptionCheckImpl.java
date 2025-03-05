/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.exceptions;

import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

public class ExceptionCheckImpl
implements ExceptionCheck {
    private final Set<JavaRefTypeInstance> caughtChecked = SetFactory.newSet();
    private final Set<JavaRefTypeInstance> caughtUnchecked = SetFactory.newSet();
    private final boolean mightUseUnchecked;
    private final boolean missingInfo;
    private final DCCommonState dcCommonState;

    public ExceptionCheckImpl(DCCommonState dcCommonState, Set<JavaRefTypeInstance> caught) {
        this.dcCommonState = dcCommonState;
        JavaRefTypeInstance runtimeExceptionType = dcCommonState.getClassTypeOrNull("java/lang/RuntimeException.class");
        if (runtimeExceptionType == null) {
            this.mightUseUnchecked = true;
            this.missingInfo = true;
            return;
        }
        boolean lmightUseUnchecked = false;
        boolean lmissinginfo = false;
        for (JavaRefTypeInstance ref : caught) {
            BindingSuperContainer superContainer = ref.getBindingSupers();
            if (superContainer == null) {
                lmightUseUnchecked = true;
                lmissinginfo = true;
                continue;
            }
            Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> supers = superContainer.getBoundSuperClasses();
            if (supers == null) {
                lmightUseUnchecked = true;
                lmissinginfo = true;
                continue;
            }
            if (supers.containsKey(runtimeExceptionType)) {
                lmightUseUnchecked = true;
                this.caughtUnchecked.add(ref);
                continue;
            }
            this.caughtChecked.add(ref);
        }
        this.mightUseUnchecked = lmightUseUnchecked;
        this.missingInfo = lmissinginfo;
    }

    private boolean checkAgainstInternal(Set<? extends JavaTypeInstance> thrown) {
        if (thrown.isEmpty()) {
            return false;
        }
        for (JavaTypeInstance javaTypeInstance : thrown) {
            try {
                ClassFile thrownClassFile = this.dcCommonState.getClassFile(javaTypeInstance);
                if (thrownClassFile == null) {
                    return true;
                }
                BindingSuperContainer bindingSuperContainer = thrownClassFile.getBindingSupers();
                if (bindingSuperContainer == null) {
                    return true;
                }
                Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> boundSuperClasses = bindingSuperContainer.getBoundSuperClasses();
                if (boundSuperClasses == null) {
                    return true;
                }
                if (!SetUtil.hasIntersection(this.caughtChecked, boundSuperClasses.keySet())) continue;
                return true;
            }
            catch (CannotLoadClassException e) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean checkAgainst(Set<? extends JavaTypeInstance> thrown) {
        try {
            return this.checkAgainstInternal(thrown);
        }
        catch (Exception e) {
            return true;
        }
    }

    @Override
    public boolean checkAgainst(AbstractMemberFunctionInvokation functionInvokation) {
        if (this.mightUseUnchecked) {
            return true;
        }
        JavaTypeInstance type = functionInvokation.getClassTypeInstance();
        try {
            ClassFile classFile = this.dcCommonState.getClassFile(type);
            Method method = classFile.getMethodByPrototype(functionInvokation.getMethodPrototype());
            return this.checkAgainstInternal(method.getThrownTypes());
        }
        catch (NoSuchMethodException e) {
            return true;
        }
        catch (CannotLoadClassException e) {
            return true;
        }
    }

    @Override
    public boolean checkAgainstException(Expression expression) {
        Map<JavaRefTypeInstance, JavaGenericRefTypeInstance> boundSuperClasses;
        if (this.missingInfo) {
            return true;
        }
        if (!(expression instanceof ConstructorInvokationSimple)) {
            return true;
        }
        ConstructorInvokationSimple constructorInvokation = (ConstructorInvokationSimple)expression;
        JavaTypeInstance type = constructorInvokation.getTypeInstance();
        try {
            ClassFile classFile = this.dcCommonState.getClassFile(type);
            if (classFile == null) {
                return true;
            }
            BindingSuperContainer bindingSuperContainer = classFile.getBindingSupers();
            if (bindingSuperContainer == null) {
                return true;
            }
            boundSuperClasses = bindingSuperContainer.getBoundSuperClasses();
            if (boundSuperClasses == null) {
                return true;
            }
        }
        catch (CannotLoadClassException e) {
            return true;
        }
        Set<JavaRefTypeInstance> throwingBases = boundSuperClasses.keySet();
        if (SetUtil.hasIntersection(this.caughtChecked, throwingBases)) {
            return true;
        }
        return SetUtil.hasIntersection(this.caughtUnchecked, throwingBases);
    }

    @Override
    public boolean mightCatchUnchecked() {
        return this.mightUseUnchecked;
    }
}

