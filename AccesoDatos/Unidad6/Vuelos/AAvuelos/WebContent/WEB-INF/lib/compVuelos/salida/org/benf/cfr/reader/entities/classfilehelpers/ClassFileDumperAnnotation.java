/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.classfilehelpers;

import java.util.List;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.classfilehelpers.AbstractClassFileDumper;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumper;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

public class ClassFileDumperAnnotation
extends AbstractClassFileDumper {
    private static final AccessFlag[] dumpableAccessFlagsInterface = new AccessFlag[]{AccessFlag.ACC_PUBLIC, AccessFlag.ACC_PRIVATE, AccessFlag.ACC_PROTECTED, AccessFlag.ACC_STATIC, AccessFlag.ACC_FINAL};

    public ClassFileDumperAnnotation(DCCommonState dcCommonState) {
        super(dcCommonState);
    }

    private void dumpHeader(ClassFile c, ClassFileDumper.InnerClassDumpType innerClassDumpType, Dumper d) {
        d.print(ClassFileDumperAnnotation.getAccessFlagsString(c.getAccessFlags(), dumpableAccessFlagsInterface));
        d.print("@interface ");
        c.dumpClassIdentity(d);
        d.print(" ");
    }

    @Override
    public Dumper dump(ClassFile classFile, ClassFileDumper.InnerClassDumpType innerClass, Dumper d) {
        if (!innerClass.isInnerClass()) {
            this.dumpTopHeader(classFile, d, true);
            this.dumpImports(d, classFile);
        }
        boolean first = true;
        this.dumpComments(classFile, d);
        this.dumpAnnotations(classFile, d);
        this.dumpHeader(classFile, innerClass, d);
        d.separator("{").newln();
        d.indent(1);
        List<ClassFileField> fields = classFile.getFields();
        for (ClassFileField field : fields) {
            field.dump(d, classFile);
            first = false;
        }
        this.dumpMethods(classFile, d, first, false);
        classFile.dumpNamedInnerClasses(d);
        d.indent(-1);
        d.print("}").newln();
        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }
}

