/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.Collection;
import java.util.Map;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.attributes.AttributeUnknown;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.collections.MapFactory;

public class AttributeMap
implements TypeUsageCollectable {
    private final Map<String, Attribute> attributes = MapFactory.newMap();

    public AttributeMap(Collection<Attribute> tmpAttributes) {
        for (Attribute a : tmpAttributes) {
            this.attributes.put(a.getRawName(), a);
        }
    }

    public <T extends Attribute> T getByName(String name) {
        Attribute attribute = this.attributes.get(name);
        if (attribute == null) {
            return null;
        }
        if (attribute instanceof AttributeUnknown) {
            return null;
        }
        Attribute tmp = attribute;
        return (T)tmp;
    }

    public boolean containsKey(String attributeName) {
        return this.attributes.containsKey(attributeName);
    }

    public void clear() {
        this.attributes.clear();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        for (Attribute attribute : this.attributes.values()) {
            attribute.collectTypeUsages(collector);
        }
    }

    public boolean any(String ... attributeNames) {
        for (String name : attributeNames) {
            if (!this.attributes.containsKey(name)) continue;
            return true;
        }
        return false;
    }
}

