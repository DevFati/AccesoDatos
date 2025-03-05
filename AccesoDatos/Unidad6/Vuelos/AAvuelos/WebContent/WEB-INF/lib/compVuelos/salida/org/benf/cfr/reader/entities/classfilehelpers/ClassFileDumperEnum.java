/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.classfilehelpers;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractConstructorInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.classfilehelpers.AbstractClassFileDumper;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumper;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.MiscUtils;
import org.benf.cfr.reader.util.output.Dumper;

public class ClassFileDumperEnum
extends AbstractClassFileDumper {
    private static final AccessFlag[] dumpableAccessFlagsEnum = new AccessFlag[]{AccessFlag.ACC_PUBLIC, AccessFlag.ACC_PRIVATE, AccessFlag.ACC_PROTECTED, AccessFlag.ACC_STRICT, AccessFlag.ACC_STATIC};
    private final List<Pair<StaticVariable, AbstractConstructorInvokation>> entries;

    public ClassFileDumperEnum(DCCommonState dcCommonState, List<Pair<StaticVariable, AbstractConstructorInvokation>> entries) {
        super(dcCommonState);
        this.entries = entries;
    }

    private static void dumpHeader(ClassFile c, ClassFileDumper.InnerClassDumpType innerClassDumpType, Dumper d) {
        d.print(ClassFileDumperEnum.getAccessFlagsString(c.getAccessFlags(), dumpableAccessFlagsEnum));
        d.print("enum ").dump(c.getThisClassConstpoolEntry().getTypeInstance()).print(" ");
        ClassSignature signature = c.getClassSignature();
        List<JavaTypeInstance> interfaces = signature.getInterfaces();
        if (!interfaces.isEmpty()) {
            d.print("implements ");
            int size = interfaces.size();
            for (int x = 0; x < size; ++x) {
                JavaTypeInstance iface = interfaces.get(x);
                d.dump(iface).print(x < size - 1 ? "," : "").newln();
            }
        }
    }

    private static void dumpEntry(Dumper d, Pair<StaticVariable, AbstractConstructorInvokation> entry, boolean last, JavaTypeInstance classType) {
        StaticVariable staticVariable = entry.getFirst();
        AbstractConstructorInvokation constructorInvokation = entry.getSecond();
        d.fieldName(staticVariable.getFieldName(), classType, false, true, true);
        if (constructorInvokation instanceof ConstructorInvokationSimple) {
            List<Expression> args = constructorInvokation.getArgs();
            if (args.size() > 2) {
                d.separator("(");
                int len = args.size();
                for (int x = 2; x < len; ++x) {
                    if (x > 2) {
                        d.print(", ");
                    }
                    d.dump(args.get(x));
                }
                d.separator(")");
            }
        } else if (constructorInvokation instanceof ConstructorInvokationAnonymousInner) {
            ((ConstructorInvokationAnonymousInner)constructorInvokation).dumpForEnum(d);
        } else {
            MiscUtils.handyBreakPoint();
        }
        if (last) {
            d.endCodeln();
        } else {
            d.print(",").newln();
        }
    }

    @Override
    public Dumper dump(ClassFile classFile, ClassFileDumper.InnerClassDumpType innerClass, Dumper d) {
        if (!innerClass.isInnerClass()) {
            this.dumpTopHeader(classFile, d, true);
            this.dumpImports(d, classFile);
        }
        this.dumpComments(classFile, d);
        this.dumpAnnotations(classFile, d);
        ClassFileDumperEnum.dumpHeader(classFile, innerClass, d);
        d.separator("{").newln();
        d.indent(1);
        JavaTypeInstance classType = classFile.getClassType();
        int len = this.entries.size();
        for (int x = 0; x < len; ++x) {
            ClassFileDumperEnum.dumpEntry(d, this.entries.get(x), x == len - 1, classType);
        }
        d.newln();
        List<ClassFileField> fields = classFile.getFields();
        for (ClassFileField field : fields) {
            if (field.shouldNotDisplay()) continue;
            field.dump(d, classFile);
        }
        List<Method> methods = classFile.getMethods();
        if (!methods.isEmpty()) {
            for (Method method : methods) {
                if (method.hiddenState() != Method.Visibility.Visible) continue;
                d.newln();
                method.dump(d, true);
            }
        }
        classFile.dumpNamedInnerClasses(d);
        d.indent(-1);
        d.print("}").newln();
        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (Pair<StaticVariable, AbstractConstructorInvokation> entry : this.entries) {
            collector.collectFrom(entry.getFirst());
            collector.collectFrom(entry.getSecond());
        }
    }
}

