/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse.utils;

public class Triplet<X, Y, Z> {
    private final X x;
    private final Y y;
    private final Z z;

    public Triplet(X x, Y y, Z z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public X getFirst() {
        return this.x;
    }

    public Y getSecond() {
        return this.y;
    }

    public Z getThird() {
        return this.z;
    }

    public static <A, B, C> Triplet<A, B, C> make(A a, B b, C c) {
        return new Triplet<A, B, C>(a, b, c);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Triplet)) {
            return false;
        }
        Triplet other = (Triplet)o;
        if (this.x == null ? other.x != null : !this.x.equals(other.x)) {
            return false;
        }
        if (this.y == null ? other.y != null : !this.y.equals(other.y)) {
            return false;
        }
        return !(this.z == null ? other.z != null : !this.z.equals(other.z));
    }

    public int hashCode() {
        int hashCode = 1;
        if (this.x != null) {
            hashCode = this.x.hashCode();
        }
        if (this.y != null) {
            hashCode = hashCode * 31 + this.y.hashCode();
        }
        if (this.z != null) {
            hashCode = hashCode * 31 + this.z.hashCode();
        }
        return hashCode;
    }
}

