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
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.output.Dumper;

public class ClassFileDumperRecord
extends AbstractClassFileDumper {
    private static final AccessFlag[] dumpableAccessFlagsClass = new AccessFlag[]{AccessFlag.ACC_PUBLIC, AccessFlag.ACC_PRIVATE, AccessFlag.ACC_PROTECTED, AccessFlag.ACC_STRICT, AccessFlag.ACC_ABSTRACT};

    public ClassFileDumperRecord(DCCommonState dcCommonState) {
        super(dcCommonState);
    }

    private void dumpHeader(ClassFile c, ClassFileDumper.InnerClassDumpType innerClassDumpType, Dumper d) {
        d.keyword(ClassFileDumperRecord.getAccessFlagsString(c.getAccessFlags(), dumpableAccessFlagsClass));
        d.keyword("record ");
        c.dumpClassIdentity(d);
        d.print("(");
        List<ClassFileField> fields = Functional.filter(c.getFields(), new Predicate<ClassFileField>(){

            @Override
            public boolean test(ClassFileField in) {
                return !in.getField().testAccessFlag(AccessFlag.ACC_STATIC);
            }
        });
        boolean first = true;
        for (ClassFileField f : fields) {
            first = StringUtils.comma(first, d);
            f.dumpAsRecord(d, c);
        }
        d.print(") ");
        this.dumpImplements(d, c.getClassSignature());
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

