/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.classfilehelpers;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.classfilehelpers.AbstractClassFileDumper;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumper;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.output.Dumper;

public class ClassFileDumperInterface
extends AbstractClassFileDumper {
    private static final AccessFlag[] dumpableAccessFlagsInterface = new AccessFlag[]{AccessFlag.ACC_PUBLIC, AccessFlag.ACC_PRIVATE, AccessFlag.ACC_PROTECTED, AccessFlag.ACC_STRICT, AccessFlag.ACC_STATIC, AccessFlag.ACC_FINAL, AccessFlag.ACC_FAKE_SEALED, AccessFlag.ACC_FAKE_NON_SEALED};

    public ClassFileDumperInterface(DCCommonState dcCommonState) {
        super(dcCommonState);
    }

    private void dumpHeader(ClassFile c, ClassFileDumper.InnerClassDumpType innerClassDumpType, Dumper d) {
        d.print(ClassFileDumperInterface.getAccessFlagsString(c.getAccessFlags(), dumpableAccessFlagsInterface));
        d.print("interface ");
        c.dumpClassIdentity(d);
        d.newln();
        ClassSignature signature = c.getClassSignature();
        List<JavaTypeInstance> interfaces = signature.getInterfaces();
        if (!interfaces.isEmpty()) {
            d.print("extends ");
            int size = interfaces.size();
            for (int x = 0; x < size; ++x) {
                JavaTypeInstance iface = interfaces.get(x);
                d.dump(iface).print(x < size - 1 ? "," : "").newln();
            }
        }
        c.dumpPermitted(d);
        d.removePendingCarriageReturn().print(" ");
    }

    @Override
    public Dumper dump(ClassFile classFile, ClassFileDumper.InnerClassDumpType innerClass, Dumper d) {
        if (this.isPackageInfo(classFile, d)) {
            this.dumpPackageInfo(classFile, d);
            return d;
        }
        if (!innerClass.isInnerClass()) {
            this.dumpTopHeader(classFile, d, true);
            this.dumpImports(d, classFile);
        }
        this.dumpComments(classFile, d);
        this.dumpAnnotations(classFile, d);
        this.dumpHeader(classFile, innerClass, d);
        boolean first = true;
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

    private boolean isPackageInfo(ClassFile classFile, Dumper d) {
        JavaRefTypeInstance classType = classFile.getRefClassType();
        if (!"package-info".equals(classType.getRawShortName())) {
            return false;
        }
        List<Method> methods = classFile.getMethods();
        if (!methods.isEmpty()) {
            classFile.ensureDecompilerComments().addComment(DecompilerComment.PACKAGE_INFO_CODE);
            return false;
        }
        if (!classFile.getFields().isEmpty()) {
            classFile.ensureDecompilerComments().addComment(DecompilerComment.PACKAGE_INFO_CODE);
            return false;
        }
        return true;
    }

    private void dumpPackageInfo(ClassFile classFile, Dumper d) {
        this.dumpTopHeader(classFile, d, false);
        this.dumpAnnotations(classFile, d);
        d.packageName(classFile.getRefClassType());
        this.dumpImports(d, classFile);
        this.dumpComments(classFile, d);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }
}

