/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.BinaryPredicate;

public class SSAIdentifiers<KEYTYPE> {
    private final Map<KEYTYPE, SSAIdent> knownIdentifiersOnEntry;
    private final Map<KEYTYPE, SSAIdent> knownIdentifiersOnExit;
    private final Map<KEYTYPE, KEYTYPE> fixedHere;

    public SSAIdentifiers() {
        this.knownIdentifiersOnEntry = MapFactory.newMap();
        this.knownIdentifiersOnExit = MapFactory.newMap();
        this.fixedHere = MapFactory.newMap();
    }

    public SSAIdentifiers(SSAIdentifiers<KEYTYPE> other) {
        this.fixedHere = MapFactory.newMap();
        this.fixedHere.putAll(other.fixedHere);
        this.knownIdentifiersOnEntry = MapFactory.newMap();
        this.knownIdentifiersOnEntry.putAll(other.knownIdentifiersOnEntry);
        this.knownIdentifiersOnExit = MapFactory.newMap();
        this.knownIdentifiersOnExit.putAll(other.knownIdentifiersOnExit);
    }

    public SSAIdentifiers(KEYTYPE lValue, SSAIdentifierFactory<KEYTYPE, ?> ssaIdentifierFactory) {
        SSAIdent id = ssaIdentifierFactory.getIdent(lValue);
        this.knownIdentifiersOnEntry = MapFactory.newMap();
        this.knownIdentifiersOnExit = MapFactory.newMap();
        this.knownIdentifiersOnExit.put(lValue, id);
        this.fixedHere = MapFactory.newMap();
        this.fixedHere.put(lValue, lValue);
    }

    public SSAIdentifiers(Map<KEYTYPE, SSAIdent> precomputedIdentifiers) {
        this.knownIdentifiersOnEntry = MapFactory.newMap();
        this.knownIdentifiersOnExit = MapFactory.newMap();
        this.fixedHere = MapFactory.newMap();
        this.knownIdentifiersOnEntry.putAll(precomputedIdentifiers);
        this.knownIdentifiersOnExit.putAll(precomputedIdentifiers);
    }

    public boolean mergeWith(SSAIdentifiers<KEYTYPE> other) {
        return this.mergeWith(other, null);
    }

    private boolean registerChange(Map<KEYTYPE, SSAIdent> knownIdentifiers, KEYTYPE lValue, SSAIdent otherIdent) {
        Object k2;
        if (!knownIdentifiers.containsKey(lValue)) {
            knownIdentifiers.put(lValue, otherIdent);
            return true;
        }
        SSAIdent oldIdent = knownIdentifiers.get(lValue);
        Object k1 = oldIdent.getComparisonType();
        SSAIdent newIdent = k1 == (k2 = otherIdent.getComparisonType()) ? oldIdent.mergeWith(otherIdent) : SSAIdent.poison;
        if (!newIdent.equals(oldIdent)) {
            knownIdentifiers.put(lValue, newIdent);
            return true;
        }
        return false;
    }

    public void consumeExit(SSAIdentifiers<KEYTYPE> other) {
        this.consume(other.knownIdentifiersOnExit);
    }

    public void consumeEntry(SSAIdentifiers<KEYTYPE> other) {
        this.consume(other.knownIdentifiersOnEntry);
    }

    private void consume(Map<KEYTYPE, SSAIdent> others) {
        for (Map.Entry<KEYTYPE, SSAIdent> valueSetEntry : others.entrySet()) {
            KEYTYPE lValue = valueSetEntry.getKey();
            SSAIdent otherIdent = valueSetEntry.getValue();
            if (this.fixedHere.containsKey(lValue)) continue;
            this.knownIdentifiersOnExit.put(lValue, otherIdent);
        }
    }

    public boolean mergeWith(SSAIdentifiers<KEYTYPE> other, BinaryPredicate<KEYTYPE, KEYTYPE> pred) {
        boolean changed = false;
        for (Map.Entry<KEYTYPE, SSAIdent> valueSetEntry : other.knownIdentifiersOnExit.entrySet()) {
            boolean c2;
            KEYTYPE lValue = valueSetEntry.getKey();
            SSAIdent otherIdent = valueSetEntry.getValue();
            boolean c1 = this.registerChange(this.knownIdentifiersOnEntry, lValue, otherIdent);
            boolean skip = false;
            if (this.fixedHere.containsKey(lValue) && (pred == null || !pred.test(lValue, this.fixedHere.get(lValue)) || otherIdent == SSAIdent.poison)) {
                skip = true;
            }
            boolean bl = c2 = !skip && this.registerChange(this.knownIdentifiersOnExit, lValue, otherIdent);
            if (!c1 && !c2) continue;
            changed = true;
        }
        return changed;
    }

    void fixHere(Set<KEYTYPE> fixed) {
        for (KEYTYPE fix : fixed) {
            this.fixedHere.put(fix, fix);
        }
    }

    public Set<KEYTYPE> getFixedHere() {
        return this.fixedHere.keySet();
    }

    public boolean isValidReplacement(KEYTYPE lValue, SSAIdentifiers<KEYTYPE> other) {
        SSAIdent thisVersion = this.knownIdentifiersOnEntry.get(lValue);
        SSAIdent otherVersion = other.knownIdentifiersOnExit.get(lValue);
        if (thisVersion == null && otherVersion == null) {
            return true;
        }
        if (thisVersion == null || otherVersion == null) {
            return false;
        }
        boolean res = thisVersion.equals(otherVersion);
        if (res) {
            return true;
        }
        return thisVersion.isSuperSet(otherVersion);
    }

    boolean isValidReplacementOnExit(KEYTYPE lValue, SSAIdentifiers<KEYTYPE> other) {
        SSAIdent thisVersion = this.knownIdentifiersOnExit.get(lValue);
        SSAIdent otherVersion = other.knownIdentifiersOnExit.get(lValue);
        if (thisVersion == null && otherVersion == null) {
            return true;
        }
        if (thisVersion == null || otherVersion == null) {
            return false;
        }
        boolean res = thisVersion.equals(otherVersion);
        if (res) {
            return true;
        }
        return thisVersion.isSuperSet(otherVersion);
    }

    Set<KEYTYPE> getChanges() {
        Set result = SetFactory.newSet();
        for (Map.Entry<KEYTYPE, SSAIdent> entry : this.knownIdentifiersOnEntry.entrySet()) {
            SSAIdent after = this.knownIdentifiersOnExit.get(entry.getKey());
            if (after == null || after.equals(entry.getValue())) continue;
            result.add(entry.getKey());
        }
        return result;
    }

    public boolean unchanged(KEYTYPE lValue) {
        SSAIdent before = this.getSSAIdentOnEntry(lValue);
        SSAIdent after = this.getSSAIdentOnExit(lValue);
        if (before == null) {
            return after == null;
        }
        return before.equals(after);
    }

    public SSAIdent getSSAIdentOnExit(KEYTYPE lValue) {
        return this.knownIdentifiersOnExit.get(lValue);
    }

    public SSAIdent getSSAIdentOnEntry(KEYTYPE lValue) {
        return this.knownIdentifiersOnEntry.get(lValue);
    }

    public void removeEntryIdent(KEYTYPE key) {
        this.knownIdentifiersOnEntry.remove(key);
    }

    public void setKnownIdentifierOnExit(KEYTYPE lValue, SSAIdent ident) {
        this.knownIdentifiersOnExit.put(lValue, ident);
    }

    public void setKnownIdentifierOnEntry(KEYTYPE lValue, SSAIdent ident) {
        this.knownIdentifiersOnEntry.put(lValue, ident);
    }

    public Map<KEYTYPE, SSAIdent> getKnownIdentifiersOnExit() {
        return this.knownIdentifiersOnExit;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<KEYTYPE, SSAIdent> entry : this.knownIdentifiersOnEntry.entrySet()) {
            sb.append(entry.getKey()).append("@").append(entry.getValue()).append(" ");
        }
        sb.append(" -> ");
        for (Map.Entry<KEYTYPE, SSAIdent> entry : this.knownIdentifiersOnExit.entrySet()) {
            sb.append(entry.getKey()).append("@").append(entry.getValue()).append(" ");
        }
        return sb.toString();
    }
}

