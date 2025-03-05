/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.state.ClassNameFunction;
import org.benf.cfr.reader.state.ClassNameFunctionCase;
import org.benf.cfr.reader.state.ClassNameFunctionInvalid;
import org.benf.cfr.reader.state.OsInfo;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class ClassRenamer {
    private Map<String, String> classCollisionRenamerToReal = MapFactory.newMap();
    private Map<String, String> classCollisionRenamerFromReal = MapFactory.newMap();
    private List<ClassNameFunction> renamers;

    private ClassRenamer(List<ClassNameFunction> renamers) {
        this.renamers = renamers;
    }

    public static ClassRenamer create(Options options) {
        Set<String> invalidNames = OsInfo.OS().getIllegalNames();
        boolean renameCase = (Boolean)options.getOption(OptionsImpl.CASE_INSENSITIVE_FS_RENAME);
        List<ClassNameFunction> functions = ListFactory.newList();
        if (!invalidNames.isEmpty()) {
            functions.add(new ClassNameFunctionInvalid(renameCase, invalidNames));
        }
        if (renameCase) {
            functions.add(new ClassNameFunctionCase());
        }
        if (functions.isEmpty()) {
            return null;
        }
        return new ClassRenamer(functions);
    }

    String getRenamedClass(String name) {
        String res = this.classCollisionRenamerFromReal.get(name);
        return res == null ? name : res;
    }

    String getOriginalClass(String name) {
        String res = this.classCollisionRenamerToReal.get(name);
        return res == null ? name : res;
    }

    void notifyClassFiles(Collection<String> names) {
        Map<String, String> originalToXfrm = MapFactory.newOrderedMap();
        for (String string : names) {
            originalToXfrm.put(string, string);
        }
        for (ClassNameFunction classNameFunction : this.renamers) {
            originalToXfrm = classNameFunction.apply(originalToXfrm);
        }
        for (Map.Entry entry : originalToXfrm.entrySet()) {
            String rename;
            String original = (String)entry.getKey();
            if (original.equals(rename = (String)entry.getValue())) continue;
            this.classCollisionRenamerFromReal.put(original, rename);
            this.classCollisionRenamerToReal.put(rename, original);
        }
    }
}

