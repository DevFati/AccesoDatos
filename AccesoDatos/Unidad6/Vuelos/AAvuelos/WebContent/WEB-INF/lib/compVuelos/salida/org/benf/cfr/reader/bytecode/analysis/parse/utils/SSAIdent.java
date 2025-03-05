/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

import java.util.BitSet;

public class SSAIdent {
    public static SSAIdent poison = new SSAIdent(0, new Object());
    private final BitSet val;
    private final Object comparisonType;

    public SSAIdent(int idx, Object comparisonType) {
        this.val = new BitSet();
        this.val.set(idx);
        this.comparisonType = comparisonType;
    }

    private SSAIdent(BitSet content, Object comparisonType) {
        this.val = content;
        this.comparisonType = comparisonType;
    }

    public Object getComparisonType() {
        return this.comparisonType;
    }

    public SSAIdent mergeWith(SSAIdent other) {
        BitSet b1 = this.val;
        BitSet b2 = other.val;
        if (b1.equals(b2)) {
            return this;
        }
        b1 = (BitSet)b1.clone();
        b1.or(b2);
        return new SSAIdent(b1, this.comparisonType);
    }

    public boolean isSuperSet(SSAIdent other) {
        BitSet tmp = (BitSet)this.val.clone();
        tmp.or(other.val);
        if (tmp.cardinality() != this.val.cardinality()) {
            return false;
        }
        tmp.xor(other.val);
        return tmp.cardinality() > 0;
    }

    public int card() {
        return this.val.cardinality();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SSAIdent)) {
            return false;
        }
        SSAIdent other = (SSAIdent)o;
        return this.val.equals(other.val);
    }

    public int hashCode() {
        return this.val.hashCode();
    }

    public String toString() {
        if (this == poison) {
            return "POISON";
        }
        return this.val.toString();
    }

    public boolean isFirstIn(SSAIdent other) {
        int bit2;
        int bit1 = this.val.nextSetBit(0);
        return bit1 == (bit2 = other.val.nextSetBit(0));
    }
}

