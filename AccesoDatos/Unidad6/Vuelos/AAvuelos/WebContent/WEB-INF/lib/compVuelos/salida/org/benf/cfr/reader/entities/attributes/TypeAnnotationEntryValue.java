/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.Map;
import org.benf.cfr.reader.entities.attributes.BadAttributeException;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryKind;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationLocation;
import org.benf.cfr.reader.util.collections.MapFactory;

public enum TypeAnnotationEntryValue {
    type_generic_class_interface(0, TypeAnnotationEntryKind.type_parameter_target, TypeAnnotationLocation.ClassFile),
    type_generic_method_constructor(1, TypeAnnotationEntryKind.type_parameter_target, TypeAnnotationLocation.method_info),
    type_extends_implements(16, TypeAnnotationEntryKind.supertype_target, TypeAnnotationLocation.ClassFile),
    type_type_parameter_class_interface(17, TypeAnnotationEntryKind.type_parameter_bound_target, TypeAnnotationLocation.ClassFile),
    type_type_parameter_method_constructor(18, TypeAnnotationEntryKind.type_parameter_bound_target, TypeAnnotationLocation.method_info),
    type_field(19, TypeAnnotationEntryKind.empty_target, TypeAnnotationLocation.field_info),
    type_ret_or_new(20, TypeAnnotationEntryKind.empty_target, TypeAnnotationLocation.method_info),
    type_receiver(21, TypeAnnotationEntryKind.empty_target, TypeAnnotationLocation.method_info),
    type_formal(22, TypeAnnotationEntryKind.method_formal_parameter_target, TypeAnnotationLocation.method_info),
    type_throws(23, TypeAnnotationEntryKind.throws_target, TypeAnnotationLocation.method_info),
    type_localvar(64, TypeAnnotationEntryKind.localvar_target, TypeAnnotationLocation.Code),
    type_resourcevar(65, TypeAnnotationEntryKind.localvar_target, TypeAnnotationLocation.Code),
    type_exceptionparameter(66, TypeAnnotationEntryKind.catch_target, TypeAnnotationLocation.Code),
    type_instanceof(67, TypeAnnotationEntryKind.offset_target, TypeAnnotationLocation.Code),
    type_new(68, TypeAnnotationEntryKind.offset_target, TypeAnnotationLocation.Code),
    type_methodrefnew(69, TypeAnnotationEntryKind.offset_target, TypeAnnotationLocation.Code),
    type_methodrefident(70, TypeAnnotationEntryKind.offset_target, TypeAnnotationLocation.Code),
    type_cast(71, TypeAnnotationEntryKind.type_argument_target, TypeAnnotationLocation.Code),
    type_generic_cons_new(72, TypeAnnotationEntryKind.type_argument_target, TypeAnnotationLocation.Code),
    type_generic_methodinvoke(73, TypeAnnotationEntryKind.type_argument_target, TypeAnnotationLocation.Code),
    type_generic_cons_methodrefnew(74, TypeAnnotationEntryKind.type_argument_target, TypeAnnotationLocation.Code),
    type_generic_methodrefident(75, TypeAnnotationEntryKind.type_argument_target, TypeAnnotationLocation.Code);

    private short value;
    private TypeAnnotationEntryKind type_parameter_target;
    private TypeAnnotationLocation location;
    private static final Map<Short, TypeAnnotationEntryValue> lut;

    private TypeAnnotationEntryValue(int value, TypeAnnotationEntryKind type_parameter_target, TypeAnnotationLocation location) {
        this.value = (short)value;
        this.type_parameter_target = type_parameter_target;
        this.location = location;
    }

    public TypeAnnotationEntryKind getKind() {
        return this.type_parameter_target;
    }

    public TypeAnnotationLocation getLocation() {
        return this.location;
    }

    public static TypeAnnotationEntryValue get(short value) {
        TypeAnnotationEntryValue res = lut.get(value);
        if (res != null) {
            return res;
        }
        throw new BadAttributeException();
    }

    static {
        lut = MapFactory.newMap();
        for (TypeAnnotationEntryValue value : TypeAnnotationEntryValue.values()) {
            lut.put(value.value, value);
        }
    }
}

