/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.benf.cfr.reader.state.ClassNameFunction;
import org.benf.cfr.reader.util.collections.MapFactory;

public class ClassNameFunctionCase
implements ClassNameFunction {
    @Override
    public Map<String, String> apply(Map<String, String> names) {
        TreeSet<String> caseInTest = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> applied = MapFactory.newOrderedMap();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String original = entry.getKey();
            String used = entry.getValue();
            if (!caseInTest.add(used)) {
                used = ClassNameFunctionCase.deDup(used, caseInTest);
            }
            applied.put(original, used);
        }
        return applied;
    }

    private static String deDup(String potDup, Set<String> caseInTest) {
        String n = potDup.toLowerCase();
        String name = n.substring(0, n.length() - 6);
        int next = 0;
        if (!caseInTest.contains(n)) {
            return potDup;
        }
        String testName = name + "_" + next + ".class";
        while (caseInTest.contains(testName)) {
            testName = name + "_" + ++next + ".class";
        }
        caseInTest.add(testName);
        return testName;
    }
}

