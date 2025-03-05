/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.LinkedList;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;

class TypeUsageUtils {
    TypeUsageUtils() {
    }

    static String generateInnerClassShortName(IllegalIdentifierDump iid, JavaRefTypeInstance clazz, JavaRefTypeInstance analysisType, boolean prefixAnalysisType) {
        int idx;
        String analysisTypeRawName;
        boolean first;
        JavaRefTypeInstance currentClass;
        boolean analysisTypeFound;
        LinkedList<JavaRefTypeInstance> classStack;
        block11: {
            InnerClassInfo innerClassInfo;
            String possible;
            classStack = ListFactory.newLinkedList();
            analysisTypeFound = false;
            if (clazz.getRawName().startsWith(analysisType.getRawName()) && !(possible = clazz.getRawName().substring(analysisType.getRawName().length())).isEmpty()) {
                switch (possible.charAt(0)) {
                    case '$': 
                    case '.': {
                        analysisTypeFound = true;
                    }
                }
            }
            currentClass = clazz;
            first = true;
            do {
                if (!(innerClassInfo = currentClass.getInnerClassHereInfo()).isAnonymousClass() || first) {
                    classStack.addFirst(currentClass);
                }
                first = false;
                if (!innerClassInfo.isInnerClass()) break block11;
            } while (!(currentClass = innerClassInfo.getOuterClass()).equals(analysisType));
            analysisTypeFound = true;
        }
        if (analysisTypeFound == currentClass.equals(analysisType)) {
            StringBuilder sb = new StringBuilder();
            first = true;
            if (prefixAnalysisType) {
                sb.append(analysisType.getRawShortName(iid));
                first = false;
            }
            for (JavaRefTypeInstance stackClass : classStack) {
                first = StringUtils.dot(first, sb);
                sb.append(stackClass.getRawShortName(iid));
            }
            return sb.toString();
        }
        String clazzRawName = clazz.getRawName(iid);
        if (clazzRawName.equals(analysisTypeRawName = analysisType.getRawName(iid)) && (idx = clazzRawName.lastIndexOf(46)) >= 1 && idx < clazzRawName.length() - 1) {
            return clazzRawName.substring(idx + 1);
        }
        if (analysisTypeRawName.length() >= clazzRawName.length() - 1) {
            return clazzRawName;
        }
        return clazzRawName.substring(analysisType.getRawName().length() + 1);
    }
}

