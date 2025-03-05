/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.constantpool;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamer;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamerDefault;
import org.benf.cfr.reader.entities.AbstractConstantPoolEntry;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryClass;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryNameAndType;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolUtils;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.output.Dumper;

public class ConstantPoolEntryMethodRef
extends AbstractConstantPoolEntry {
    private static final long OFFSET_OF_CLASS_INDEX = 1L;
    private static final long OFFSET_OF_NAME_AND_TYPE_INDEX = 3L;
    private final boolean interfaceMethod;
    private static final VariableNamer fakeNamer = new VariableNamerDefault();
    private MethodPrototype methodPrototype = null;
    private final int classIndex;
    private final int nameAndTypeIndex;

    public ConstantPoolEntryMethodRef(ConstantPool cp, ByteData data, boolean interfaceMethod) {
        super(cp);
        this.classIndex = data.getU2At(1L);
        this.nameAndTypeIndex = data.getU2At(3L);
        this.interfaceMethod = interfaceMethod;
    }

    @Override
    public long getRawByteLength() {
        return 5L;
    }

    @Override
    public void dump(Dumper d) {
        ConstantPool cp = this.getCp();
        d.print("Method " + cp.getNameAndTypeEntry(this.nameAndTypeIndex).getName().getValue() + ":" + cp.getNameAndTypeEntry(this.nameAndTypeIndex).getDescriptor().getValue());
    }

    @Override
    public ConstantPool getCp() {
        return super.getCp();
    }

    public String toString() {
        return "Method classIndex " + this.classIndex + " nameAndTypeIndex " + this.nameAndTypeIndex;
    }

    public ConstantPoolEntryClass getClassEntry() {
        return this.getCp().getClassEntry(this.classIndex);
    }

    public ConstantPoolEntryNameAndType getNameAndTypeEntry() {
        return this.getCp().getNameAndTypeEntry(this.nameAndTypeIndex);
    }

    public MethodPrototype getMethodPrototype() {
        if (this.methodPrototype == null) {
            MethodPrototype basePrototype;
            block8: {
                ConstantPool cp = this.getCp();
                JavaTypeInstance classType = cp.getClassEntry(this.classIndex).getTypeInstance();
                ConstantPoolEntryNameAndType nameAndType = cp.getNameAndTypeEntry(this.nameAndTypeIndex);
                ConstantPoolEntryUTF8 descriptor = nameAndType.getDescriptor();
                basePrototype = ConstantPoolUtils.parseJavaMethodPrototype(cp.getDCCommonState(), null, classType, this.getName(), false, Method.MethodConstructor.NOT, descriptor, cp, false, false, fakeNamer, descriptor.getValue());
                try {
                    MethodPrototype replacement;
                    JavaTypeInstance loadType = classType.getArrayStrippedType().getDeGenerifiedType();
                    ClassFile classFile = cp.getDCCommonState().getClassFile(loadType);
                    try {
                        replacement = classFile.getMethodByPrototype(basePrototype).getMethodPrototype();
                    }
                    catch (NoSuchMethodException e) {
                        BindingSuperContainer bindingSuperContainer;
                        if (basePrototype.getName().equals("<init>") || (bindingSuperContainer = classFile.getBindingSupers()) == null) break block8;
                        Set<JavaRefTypeInstance> supers = bindingSuperContainer.getBoundSuperClasses().keySet();
                        for (JavaTypeInstance javaTypeInstance : supers) {
                            loadType = javaTypeInstance.getDeGenerifiedType();
                            ClassFile superClassFile = cp.getDCCommonState().getClassFile(loadType);
                            try {
                                MethodPrototype baseReplacement = superClassFile.getMethodByPrototype(basePrototype).getMethodPrototype();
                                classFile = superClassFile;
                                replacement = baseReplacement;
                            }
                            catch (NoSuchMethodException noSuchMethodException) {
                            }
                        }
                        break block8;
                    }
                    {
                        basePrototype = replacement;
                        break block8;
                        break;
                    }
                }
                catch (CannotLoadClassException cannotLoadClassException) {
                    // empty catch block
                }
            }
            this.methodPrototype = basePrototype;
        }
        return this.methodPrototype;
    }

    public String getName() {
        return this.getCp().getNameAndTypeEntry(this.nameAndTypeIndex).getName().getValue();
    }

    public boolean isInitMethod() {
        String name = this.getName();
        return "<init>".equals(name);
    }
}

