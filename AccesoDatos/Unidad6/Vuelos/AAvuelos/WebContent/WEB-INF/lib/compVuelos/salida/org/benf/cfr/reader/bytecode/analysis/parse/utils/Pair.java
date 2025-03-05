/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

public class Pair<X, Y> {
    private final X x;
    private final Y y;

    public Pair(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public X getFirst() {
        return this.x;
    }

    public Y getSecond() {
        return this.y;
    }

    public static <A, B> Pair<A, B> make(A a, B b) {
        return new Pair<A, B>(a, b);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair other = (Pair)o;
        if (this.x == null ? other.x != null : !this.x.equals(other.x)) {
            return false;
        }
        return !(this.y == null ? other.y != null : !this.y.equals(other.y));
    }

    public int hashCode() {
        int hashCode = 1;
        if (this.x != null) {
            hashCode = this.x.hashCode();
        }
        if (this.y != null) {
            hashCode = hashCode * 31 + this.y.hashCode();
        }
        return hashCode;
    }

    public String toString() {
        return "P[" + this.x + "," + this.y + "]";
    }
}

