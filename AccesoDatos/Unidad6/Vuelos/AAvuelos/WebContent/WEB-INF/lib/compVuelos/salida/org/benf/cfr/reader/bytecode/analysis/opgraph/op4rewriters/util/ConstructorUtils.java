/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.Method;

public class ConstructorUtils {
    public static MethodPrototype getDelegatingPrototype(Method constructor) {
        List<Op04StructuredStatement> statements = MiscStatementTools.getBlockStatements(constructor.getAnalysis());
        if (statements == null) {
            return null;
        }
        for (Op04StructuredStatement statement : statements) {
            StructuredStatement structuredStatement = statement.getStatement();
            if (structuredStatement instanceof StructuredComment) continue;
            if (!(structuredStatement instanceof StructuredExpressionStatement)) {
                return null;
            }
            StructuredExpressionStatement structuredExpressionStatement = (StructuredExpressionStatement)structuredStatement;
            WildcardMatch wcm1 = new WildcardMatch();
            StructuredExpressionStatement test = new StructuredExpressionStatement(BytecodeLoc.NONE, wcm1.getMemberFunction("m", null, true, (Expression)new LValueExpression(wcm1.getLValueWildCard("o")), null), false);
            if (((Object)test).equals(structuredExpressionStatement)) {
                MemberFunctionInvokation m = wcm1.getMemberFunction("m").getMatch();
                MethodPrototype prototype = m.getMethodPrototype();
                return prototype;
            }
            return null;
        }
        return null;
    }

    public static boolean isDelegating(Method constructor) {
        return ConstructorUtils.getDelegatingPrototype(constructor) != null;
    }
}

