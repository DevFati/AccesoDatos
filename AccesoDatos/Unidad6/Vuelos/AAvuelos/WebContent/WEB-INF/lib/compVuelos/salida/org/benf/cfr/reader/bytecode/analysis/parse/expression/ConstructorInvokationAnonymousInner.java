/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractConstructorInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperAnonymousInner;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.output.Dumper;

public class ConstructorInvokationAnonymousInner
extends AbstractConstructorInvokation {
    private final MemberFunctionInvokation constructorInvokation;
    private final ClassFile classFile;
    private final JavaTypeInstance anonymousTypeInstance;

    public ConstructorInvokationAnonymousInner(BytecodeLoc loc, MemberFunctionInvokation constructorInvokation, InferredJavaType inferredJavaType, List<Expression> args, DCCommonState dcCommonState, JavaTypeInstance anonymousTypeInstance) {
        super(loc, inferredJavaType, constructorInvokation.getFunction(), args);
        this.constructorInvokation = constructorInvokation;
        this.anonymousTypeInstance = anonymousTypeInstance;
        ClassFile classFile = null;
        try {
            classFile = dcCommonState.getClassFile(constructorInvokation.getMethodPrototype().getReturnType().getDeGenerifiedType());
        }
        catch (CannotLoadClassException cannotLoadClassException) {
            // empty catch block
        }
        this.classFile = classFile;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return this.constructorInvokation.getCombinedLoc();
    }

    private ConstructorInvokationAnonymousInner(ConstructorInvokationAnonymousInner other, CloneHelper cloneHelper) {
        super(other.getLoc(), other, cloneHelper);
        this.constructorInvokation = (MemberFunctionInvokation)cloneHelper.replaceOrClone(other.constructorInvokation);
        this.classFile = other.classFile;
        this.anonymousTypeInstance = other.anonymousTypeInstance;
    }

    public ClassFile getClassFile() {
        return this.classFile;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new ConstructorInvokationAnonymousInner(this, cloneHelper);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        MethodPrototype prototype = this.improveMethodPrototype(d);
        ClassFileDumperAnonymousInner cfd = new ClassFileDumperAnonymousInner();
        List<Expression> args = this.getArgs();
        cfd.dumpWithArgs(this.classFile, prototype, args, false, d);
        d.removePendingCarriageReturn();
        return d;
    }

    private MethodPrototype improveMethodPrototype(Dumper d) {
        ClassFile anonymousClassFile;
        ConstantPool cp = this.constructorInvokation.getCp();
        try {
            anonymousClassFile = cp.getDCCommonState().getClassFile(this.anonymousTypeInstance);
        }
        catch (CannotLoadClassException e) {
            anonymousClassFile = this.classFile;
        }
        if (anonymousClassFile != this.classFile) {
            throw new IllegalStateException("Inner class got unexpected class file - revert this change");
        }
        d.keyword("new ");
        MethodPrototype prototype = this.constructorInvokation.getMethodPrototype();
        try {
            if (this.classFile != null) {
                prototype = this.classFile.getMethodByPrototype(prototype).getMethodPrototype();
            }
        }
        catch (NoSuchMethodException noSuchMethodException) {
            // empty catch block
        }
        return prototype;
    }

    public void dumpForEnum(Dumper d) {
        ClassFileDumperAnonymousInner cfd = new ClassFileDumperAnonymousInner();
        List<Expression> args = this.getArgs();
        cfd.dumpWithArgs(this.classFile, null, args.subList(2, args.size()), true, d);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof ConstructorInvokationAnonymousInner)) {
            return false;
        }
        ConstructorInvokationAnonymousInner other = (ConstructorInvokationAnonymousInner)o;
        if (this.getClassFile() != other.getClassFile()) {
            return false;
        }
        if (!this.getTypeInstance().equals(other.getTypeInstance())) {
            return false;
        }
        return this.getArgs().equals(other.getArgs());
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (!(o instanceof ConstructorInvokationAnonymousInner)) {
            return false;
        }
        if (!super.equivalentUnder(o, constraint)) {
            return false;
        }
        ConstructorInvokationAnonymousInner other = (ConstructorInvokationAnonymousInner)o;
        return constraint.equivalent(this.constructorInvokation, other.constructorInvokation);
    }
}

