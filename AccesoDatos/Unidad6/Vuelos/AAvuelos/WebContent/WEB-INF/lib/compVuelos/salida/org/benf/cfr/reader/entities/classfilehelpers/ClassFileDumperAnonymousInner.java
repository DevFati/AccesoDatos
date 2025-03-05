/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.classfilehelpers;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.classfilehelpers.AbstractClassFileDumper;
import org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumper;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class ClassFileDumperAnonymousInner
extends AbstractClassFileDumper {
    public ClassFileDumperAnonymousInner() {
        super(null);
    }

    @Override
    public Dumper dump(ClassFile classFile, ClassFileDumper.InnerClassDumpType innerClass, Dumper d) {
        return this.dumpWithArgs(classFile, null, ListFactory.<Expression>newList(), false, d);
    }

    public Dumper dumpWithArgs(ClassFile classFile, MethodPrototype usedMethod, List<Expression> args, boolean isEnum, Dumper d) {
        if (classFile == null) {
            d.print("/* Unavailable Anonymous Inner Class!! */");
            return d;
        }
        if (!d.canEmitClass(classFile.getClassType())) {
            d.print("/* invalid duplicate definition of identical inner class */");
            return d;
        }
        if (!isEnum) {
            JavaTypeInstance typeInstance = ClassFile.getAnonymousTypeBase(classFile);
            d.dump(typeInstance);
        }
        if (!isEnum || !args.isEmpty()) {
            d.separator("(");
            boolean first = true;
            int len = args.size();
            for (int i = 0; i < len; ++i) {
                if (usedMethod != null && usedMethod.isHiddenArg(i)) continue;
                Expression arg = args.get(i);
                first = StringUtils.comma(first, d);
                d.dump(arg);
            }
            d.separator(")");
        }
        d.separator("{").newln();
        d.indent(1);
        int outcrs = d.getOutputCount();
        List<ClassFileField> fields = classFile.getFields();
        for (ClassFileField field : fields) {
            if (field.shouldNotDisplay()) continue;
            field.dump(d, classFile);
        }
        List<Method> methods = classFile.getMethods();
        if (!methods.isEmpty()) {
            for (Method method : methods) {
                if (method.hiddenState() != Method.Visibility.Visible) continue;
                if (method.isConstructor()) {
                    Op04StructuredStatement stm;
                    AttributeCode anonymousConstructor = method.getCodeAttribute();
                    if (anonymousConstructor == null || (stm = anonymousConstructor.analyse()).isEmptyInitialiser()) continue;
                    anonymousConstructor.dump(d);
                    continue;
                }
                d.newln();
                method.dump(d, true);
            }
        }
        classFile.dumpNamedInnerClasses(d);
        d.indent(-1);
        if (d.getOutputCount() == outcrs) {
            d.removePendingCarriageReturn();
        }
        d.print("}").newln();
        return d;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
    }
}

