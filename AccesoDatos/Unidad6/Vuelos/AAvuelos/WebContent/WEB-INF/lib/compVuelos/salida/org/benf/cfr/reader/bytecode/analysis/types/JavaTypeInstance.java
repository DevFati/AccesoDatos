/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types;

import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.state.ObfuscationTypeMap;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.TypeContext;

public interface JavaTypeInstance {
    public JavaAnnotatedTypeInstance getAnnotatedInstance();

    public StackType getStackType();

    public boolean isComplexType();

    public boolean isUsableType();

    public RawJavaType getRawTypeOfSimpleType();

    public JavaTypeInstance removeAnArrayIndirection();

    public JavaTypeInstance getArrayStrippedType();

    public JavaTypeInstance getDeGenerifiedType();

    public int getNumArrayDimensions();

    public String getRawName();

    public String getRawName(IllegalIdentifierDump var1);

    public InnerClassInfo getInnerClassHereInfo();

    public BindingSuperContainer getBindingSupers();

    public boolean implicitlyCastsTo(JavaTypeInstance var1, GenericTypeBinder var2);

    public boolean impreciseCanCastTo(JavaTypeInstance var1, GenericTypeBinder var2);

    public boolean correctCanCastTo(JavaTypeInstance var1, GenericTypeBinder var2);

    public String suggestVarName();

    public void dumpInto(Dumper var1, TypeUsageInformation var2, TypeContext var3);

    public void collectInto(TypeUsageCollector var1);

    public boolean isObject();

    public JavaGenericRefTypeInstance asGenericRefInstance(JavaTypeInstance var1);

    public JavaTypeInstance directImplOf(JavaTypeInstance var1);

    public JavaTypeInstance deObfuscate(ObfuscationTypeMap var1);

    public boolean isRaw();
}

