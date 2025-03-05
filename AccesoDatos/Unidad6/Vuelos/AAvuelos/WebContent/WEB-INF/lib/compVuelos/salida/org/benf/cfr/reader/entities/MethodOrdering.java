/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.attributes.AttributeLineNumberTable;

public class MethodOrdering {
    public static List<Method> sort(List<Method> methods) {
        ArrayList<OrderData> od = new ArrayList<OrderData>();
        boolean hasLineNumbers = false;
        int len = methods.size();
        for (int x = 0; x < len; ++x) {
            AttributeLineNumberTable lineNumberTable;
            Method method = methods.get(x);
            boolean hasLineNumber = false;
            int idx = x - 100000;
            AttributeCode codeAttribute = method.getCodeAttribute();
            if (codeAttribute != null && (lineNumberTable = codeAttribute.getLineNumberTable()) != null && lineNumberTable.hasEntries()) {
                hasLineNumber = true;
                hasLineNumbers = true;
                idx = lineNumberTable.getStartLine();
            }
            od.add(new OrderData(method, hasLineNumber, idx));
        }
        if (!hasLineNumbers) {
            return methods;
        }
        Collections.sort(od);
        ArrayList<Method> res = new ArrayList<Method>(methods.size());
        for (OrderData o : od) {
            res.add(o.method);
        }
        return res;
    }

    private static class OrderData
    implements Comparable<OrderData> {
        private final Method method;
        private final boolean hasLineNumber;
        private final int origIdx;

        private OrderData(Method method, boolean hasLineNumber, int origIdx) {
            this.method = method;
            this.hasLineNumber = hasLineNumber;
            this.origIdx = origIdx;
        }

        @Override
        public int compareTo(OrderData o) {
            if (this.hasLineNumber != o.hasLineNumber) {
                return this.hasLineNumber ? -1 : 1;
            }
            return this.origIdx - o.origIdx;
        }
    }
}

