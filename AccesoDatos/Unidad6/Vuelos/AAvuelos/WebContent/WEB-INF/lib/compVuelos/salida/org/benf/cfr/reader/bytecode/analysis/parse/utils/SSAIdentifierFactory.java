/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdent;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class SSAIdentifierFactory<KEYTYPE, CMPTYPE> {
    private final Map<KEYTYPE, Integer> nextIdentFor = MapFactory.newLazyMap(MapFactory.newOrderedMap(), new UnaryFunction<KEYTYPE, Integer>(){

        @Override
        public Integer invoke(KEYTYPE ignore) {
            return 0;
        }
    });
    private final UnaryFunction<KEYTYPE, CMPTYPE> typeComparisonFunction;

    public SSAIdentifierFactory(UnaryFunction<KEYTYPE, CMPTYPE> typeComparisonFunction) {
        this.typeComparisonFunction = typeComparisonFunction;
    }

    public SSAIdent getIdent(KEYTYPE lValue) {
        int val = this.nextIdentFor.get(lValue);
        this.nextIdentFor.put(lValue, val + 1);
        return new SSAIdent(val, this.typeComparisonFunction == null ? null : this.typeComparisonFunction.invoke(lValue));
    }
}

