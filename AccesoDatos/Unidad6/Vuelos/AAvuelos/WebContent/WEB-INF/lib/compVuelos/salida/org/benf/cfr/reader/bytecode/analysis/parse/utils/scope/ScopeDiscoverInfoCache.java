/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils.scope;

import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.util.collections.MapFactory;

public class ScopeDiscoverInfoCache {
    private final Map<StructuredStatement, Boolean> tests = MapFactory.newIdentityMap();

    public Boolean get(StructuredStatement structuredStatement) {
        return this.tests.get(structuredStatement);
    }

    public void put(StructuredStatement structuredStatement, Boolean b) {
        this.tests.put(structuredStatement, b);
    }

    boolean anyFound() {
        for (Boolean value : this.tests.values()) {
            if (!value.booleanValue()) continue;
            return true;
        }
        return false;
    }
}

