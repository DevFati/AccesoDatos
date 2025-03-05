/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import org.benf.cfr.reader.entities.attributes.TypePathPart;

public class TypePath {
    public final List<TypePathPart> segments;

    public TypePath(List<TypePathPart> segments) {
        this.segments = segments;
    }
}

