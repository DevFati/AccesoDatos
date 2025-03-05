/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util;

import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class BoxingHelper {
    private static Set<Pair<String, String>> unboxing = SetFactory.newSet(Pair.make("java.lang.Integer", "intValue"), Pair.make("java.lang.Long", "longValue"), Pair.make("java.lang.Double", "doubleValue"), Pair.make("java.lang.Short", "shortValue"), Pair.make("java.lang.Byte", "byteValue"), Pair.make("java.lang.Boolean", "booleanValue"));
    private static Map<String, String> unboxingByRawName;
    private static Set<Pair<String, String>> boxing;

    public static Expression sugarUnboxing(MemberFunctionInvokation memberFunctionInvokation) {
        String name = memberFunctionInvokation.getName();
        JavaTypeInstance type = memberFunctionInvokation.getObject().getInferredJavaType().getJavaTypeInstance();
        String rawTypeName = type.getRawName();
        Pair<String, String> testPair = Pair.make(rawTypeName, name);
        if (unboxing.contains(testPair)) {
            Expression expression = memberFunctionInvokation.getObject();
            return expression;
        }
        return memberFunctionInvokation;
    }

    public static String getUnboxingMethodName(JavaTypeInstance type) {
        return unboxingByRawName.get(type.getRawName());
    }

    public static Expression sugarBoxing(StaticFunctionInvokation staticFunctionInvokation) {
        JavaTypeInstance argType;
        String name = staticFunctionInvokation.getName();
        JavaTypeInstance type = staticFunctionInvokation.getClazz();
        if (staticFunctionInvokation.getArgs().size() != 1) {
            return staticFunctionInvokation;
        }
        Expression arg1 = staticFunctionInvokation.getArgs().get(0);
        String rawTypeName = type.getRawName();
        Pair<String, String> testPair = Pair.make(rawTypeName, name);
        if (boxing.contains(testPair) && (argType = arg1.getInferredJavaType().getJavaTypeInstance()).implicitlyCastsTo(type, null)) {
            return staticFunctionInvokation.getArgs().get(0);
        }
        return staticFunctionInvokation;
    }

    public static boolean isBoxedTypeInclNumber(JavaTypeInstance type) {
        if (RawJavaType.getUnboxedTypeFor(type) != null) {
            return true;
        }
        return type.getRawName().equals("java.lang.Number");
    }

    public static boolean isBoxedType(JavaTypeInstance type) {
        return RawJavaType.getUnboxedTypeFor(type) != null;
    }

    static {
        boxing = SetFactory.newSet(Pair.make("java.lang.Integer", "valueOf"), Pair.make("java.lang.Long", "valueOf"), Pair.make("java.lang.Double", "valueOf"), Pair.make("java.lang.Short", "valueOf"), Pair.make("java.lang.Byte", "valueOf"), Pair.make("java.lang.Boolean", "valueOf"));
        unboxingByRawName = MapFactory.newMap();
        for (Pair<String, String> pair : unboxing) {
            unboxingByRawName.put(pair.getFirst(), pair.getSecond());
        }
    }
}

