/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class SealedClassChecker {
    private static boolean isSealed(JavaTypeInstance t, DCCommonState state) {
        try {
            ClassFile i = state.getClassFile(t);
            return i.getAccessFlags().contains((Object)AccessFlag.ACC_FAKE_SEALED);
        }
        catch (CannotLoadClassException e) {
            return false;
        }
    }

    private static boolean anySealed(ClassSignature sig, DCCommonState state) {
        if (SealedClassChecker.isSealed(sig.getSuperClass(), state)) {
            return true;
        }
        for (JavaTypeInstance t : sig.getInterfaces()) {
            if (!SealedClassChecker.isSealed(t, state)) continue;
            return true;
        }
        return false;
    }

    public static void rewrite(ClassFile classFile, DCCommonState state) {
        Set<AccessFlag> accessFlags = classFile.getAccessFlags();
        if (accessFlags.contains((Object)AccessFlag.ACC_FAKE_SEALED)) {
            SealedClassChecker.markExperimental(classFile, state);
            return;
        }
        if (accessFlags.contains((Object)AccessFlag.ACC_FINAL)) {
            return;
        }
        if (SealedClassChecker.anySealed(classFile.getClassSignature(), state)) {
            SealedClassChecker.markExperimental(classFile, state);
            accessFlags.add(AccessFlag.ACC_FAKE_NON_SEALED);
        }
    }

    public static void markExperimental(ClassFile classFile, DCCommonState state) {
        if (!state.getOptions().optionIsSet(OptionsImpl.SEALED) && OptionsImpl.sealedExpressionVersion.isExperimentalIn(classFile.getClassFileVersion())) {
            classFile.addComment(DecompilerComment.EXPERIMENTAL_FEATURE);
        }
    }
}

