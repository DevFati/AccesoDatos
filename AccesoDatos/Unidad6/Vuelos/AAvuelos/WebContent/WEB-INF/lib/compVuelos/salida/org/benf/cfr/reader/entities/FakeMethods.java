/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities;

import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.entities.FakeMethod;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class FakeMethods
implements TypeUsageCollectable {
    private final Map<Object, FakeMethod> fakes = MapFactory.newOrderedMap();
    private final Map<String, Integer> nameCounts = MapFactory.newLazyMap(new UnaryFunction<String, Integer>(){

        @Override
        public Integer invoke(String arg) {
            return 0;
        }
    });

    public FakeMethod add(Object key, String nameHint, UnaryFunction<String, FakeMethod> methodFactory) {
        FakeMethod method = this.fakes.get(key);
        if (method == null) {
            Integer idx = this.nameCounts.get(nameHint);
            this.nameCounts.put(nameHint, idx + 1);
            nameHint = "cfr_" + nameHint + "_" + idx;
            method = methodFactory.invoke(nameHint);
            this.fakes.put(key, method);
        }
        return method;
    }

    public List<FakeMethod> getMethods() {
        return ListFactory.newList(this.fakes.values());
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (FakeMethod method : this.fakes.values()) {
            collector.collectFrom(method);
        }
    }
}

