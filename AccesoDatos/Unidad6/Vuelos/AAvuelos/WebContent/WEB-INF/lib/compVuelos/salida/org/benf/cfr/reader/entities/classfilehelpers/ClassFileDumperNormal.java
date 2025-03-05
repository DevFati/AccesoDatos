/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.classfilehelpers;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.classfilehelpers.AbstractClassFileDumper;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumper;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class ClassFileDumperNormal
extends AbstractClassFileDumper {
    private static final AccessFlag[] dumpableAccessFlagsClass = new AccessFlag[]{AccessFlag.ACC_PUBLIC, AccessFlag.ACC_PRIVATE, AccessFlag.ACC_PROTECTED, AccessFlag.ACC_STRICT, AccessFlag.ACC_STATIC, AccessFlag.ACC_FINAL, AccessFlag.ACC_ABSTRACT, AccessFlag.ACC_FAKE_SEALED, AccessFlag.ACC_FAKE_NON_SEALED};
    private static final AccessFlag[] dumpableAccessFlagsInlineClass = new AccessFlag[]{AccessFlag.ACC_PUBLIC, AccessFlag.ACC_PRIVATE, AccessFlag.ACC_PROTECTED, AccessFlag.ACC_STRICT, AccessFlag.ACC_FINAL, AccessFlag.ACC_ABSTRACT, AccessFlag.ACC_FAKE_SEALED, AccessFlag.ACC_FAKE_NON_SEALED};

    public ClassFileDumperNormal(DCCommonState dcCommonState) {
        super(dcCommonState);
    }

    private void dumpHeader(ClassFile c, ClassFileDumper.InnerClassDumpType innerClassDumpType, Dumper d) {
        AccessFlag[] accessFlagsToDump = innerClassDumpType == ClassFileDumper.InnerClassDumpType.INLINE_CLASS ? dumpableAccessFlagsInlineClass : dumpableAccessFlagsClass;
        d.keyword(ClassFileDumperNormal.getAccessFlagsString(c.getAccessFlags(), accessFlagsToDump));
        d.keyword("class ");
        c.dumpClassIdentity(d);
        d.newln();
        ClassSignature signature = c.getClassSignature();
        JavaTypeInstance superClass = signature.getSuperClass();
        if (superClass != null && !superClass.getRawName().equals("java.lang.Object")) {
            d.keyword("extends ").dump(superClass).newln();
        }
        this.dumpImplements(d, signature);
        this.dumpPermitted(c, d);
        d.removePendingCarriageReturn().print(" ");
    }

    @Override
    public Dumper dump(ClassFile classFile, ClassFileDumper.InnerClassDumpType innerClass, Dumper d) {
        if (!d.canEmitClass(classFile.getClassType())) {
            return d;
        }
        if (!innerClass.isInnerClass()) {
            this.dumpTopHeader(classFile, d, true);
            this.dumpImports(d, classFile);
        }
        this.dumpComments(classFile, d);
        this.dumpAnnotations(classFile, d);
        this.dumpHeader(classFile, innerClass, d);
        d.separator("{").newln();
        d.indent(1);
        boolean first = true;
        List<ClassFileField> fields = classFile.getFields();
        for (ClassFileField field : fields) {
            if (field.shouldNotDisplay()) continue;
            field.dump(d, classFile);
            first = false;
        }
        this.dumpMethods(classFile, d, first, true);
        classFile.dumpNamedInnerClasses(d);
        d.indent(-1);
        d.separator("}").newln();
        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }
}

