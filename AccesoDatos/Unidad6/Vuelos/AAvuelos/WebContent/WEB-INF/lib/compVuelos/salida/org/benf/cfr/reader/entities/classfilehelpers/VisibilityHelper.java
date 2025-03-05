/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.classfilehelpers;

import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;

public class VisibilityHelper {
    private static boolean isInnerVisibleTo(JavaTypeInstance maybeCaller, ClassFile classFile) {
        JavaRefTypeInstance thisClass = classFile.getRefClassType();
        if (maybeCaller.getInnerClassHereInfo().isTransitiveInnerClassOf(thisClass)) {
            return true;
        }
        return thisClass.getInnerClassHereInfo().isTransitiveInnerClassOf(maybeCaller);
    }

    public static boolean isVisibleTo(JavaRefTypeInstance maybeCaller, ClassFile classFile, boolean accPublic, boolean accPrivate, boolean accProtected) {
        if (accPublic) {
            return true;
        }
        if (maybeCaller == null) {
            return false;
        }
        if (maybeCaller.equals(classFile.getClassType())) {
            return true;
        }
        if (accPrivate) {
            return VisibilityHelper.isInnerVisibleTo(maybeCaller, classFile);
        }
        if (accProtected) {
            BindingSuperContainer bindingSuperContainer = maybeCaller.getBindingSupers();
            if (bindingSuperContainer == null) {
                return false;
            }
            if (bindingSuperContainer.containsBase(classFile.getClassType())) {
                return true;
            }
            return VisibilityHelper.isInnerVisibleTo(maybeCaller, classFile);
        }
        return maybeCaller.getPackageName().equals(classFile.getRefClassType().getPackageName());
    }
}

