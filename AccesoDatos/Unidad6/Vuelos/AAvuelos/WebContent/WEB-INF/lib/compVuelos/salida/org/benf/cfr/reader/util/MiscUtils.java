/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util;

import java.util.regex.Pattern;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.functors.Predicate;

public class MiscUtils {
    public static <T> boolean xor(T a, T b, T required) {
        boolean b1 = a.equals(required);
        boolean b2 = b.equals(required);
        return b1 ^ b2;
    }

    public static Predicate<String> mkRegexFilter(String pat, boolean anywhere) {
        boolean positive;
        if (pat == null) {
            return new Predicate<String>(){

                @Override
                public boolean test(String in) {
                    return true;
                }
            };
        }
        boolean bl = positive = !pat.startsWith("!");
        if (!positive) {
            pat = pat.substring(1);
        }
        if (anywhere) {
            pat = "^.*" + pat + ".*$";
        }
        final Pattern p = Pattern.compile(pat);
        return new Predicate<String>(){

            @Override
            public boolean test(String in) {
                boolean matches = p.matcher(in).matches();
                return positive == matches;
            }
        };
    }

    public static void handyBreakPoint() {
    }

    public static boolean isThis(Expression obj, JavaTypeInstance thisType) {
        if (!(obj instanceof LValueExpression)) {
            return false;
        }
        LValue thisExp = ((LValueExpression)obj).getLValue();
        return MiscUtils.isThis(thisExp, thisType);
    }

    public static boolean isThis(LValue thisExp, JavaTypeInstance thisType) {
        if (!(thisExp instanceof LocalVariable)) {
            return false;
        }
        LocalVariable lv = (LocalVariable)thisExp;
        if (lv.getIdx() != 0 || !"this".equals(lv.getName().getStringName())) {
            return false;
        }
        return thisType.equals(lv.getInferredJavaType().getJavaTypeInstance());
    }
}

