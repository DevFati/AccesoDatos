/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.types.discovery;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;

public enum CastAction {
    None{

        @Override
        public Expression performCastAction(Expression orig, InferredJavaType tgtType) {
            return orig;
        }
    }
    ,
    InsertExplicit{

        @Override
        public Expression performCastAction(Expression orig, InferredJavaType tgtType) {
            if (tgtType.getJavaTypeInstance() == RawJavaType.BOOLEAN) {
                return orig;
            }
            return new CastExpression(BytecodeLoc.NONE, tgtType, orig);
        }
    };


    public abstract Expression performCastAction(Expression var1, InferredJavaType var2);
}

