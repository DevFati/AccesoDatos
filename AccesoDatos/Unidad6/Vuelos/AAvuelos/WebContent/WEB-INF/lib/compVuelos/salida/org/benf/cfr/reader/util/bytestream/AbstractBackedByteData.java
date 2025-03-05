/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.bytestream;

import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.bytestream.ByteData;

public abstract class AbstractBackedByteData
implements ByteData {
    protected final byte[] d;

    protected AbstractBackedByteData(byte[] data) {
        this.d = data;
    }

    abstract int getRealOffset(int var1);

    @Override
    public int getS4At(long o) throws ConfusedCFRException {
        int a = this.getRealOffset((int)o);
        try {
            return (this.d[a] & 0xFF) << 24 | (this.d[a + 1] & 0xFF) << 16 | (this.d[a + 2] & 0xFF) << 8 | this.d[a + 3] & 0xFF;
        }
        catch (IndexOutOfBoundsException e) {
            throw new ConfusedCFRException(e);
        }
    }

    @Override
    public double getDoubleAt(long o) throws ConfusedCFRException {
        return Double.longBitsToDouble(this.getLongAt(o));
    }

    @Override
    public float getFloatAt(long o) throws ConfusedCFRException {
        return Float.intBitsToFloat(this.getS4At(o));
    }

    @Override
    public long getLongAt(long o) throws ConfusedCFRException {
        int a = this.getRealOffset((int)o);
        try {
            return ((long)(this.d[a + 0] & 0xFF) << 56) + ((long)(this.d[a + 1] & 0xFF) << 48) + ((long)(this.d[a + 2] & 0xFF) << 40) + ((long)(this.d[a + 3] & 0xFF) << 32) + ((long)(this.d[a + 4] & 0xFF) << 24) + (long)((this.d[a + 5] & 0xFF) << 16) + (long)((this.d[a + 6] & 0xFF) << 8) + (long)((this.d[a + 7] & 0xFF) << 0);
        }
        catch (IndexOutOfBoundsException e) {
            throw new ConfusedCFRException(e);
        }
    }

    @Override
    public short getS2At(long o) throws ConfusedCFRException {
        int a = this.getRealOffset((int)o);
        try {
            return (short)((this.d[a] & 0xFF) << 8 | this.d[a + 1] & 0xFF);
        }
        catch (IndexOutOfBoundsException e) {
            throw new ConfusedCFRException(e);
        }
    }

    @Override
    public int getU2At(long o) throws ConfusedCFRException {
        int a = this.getRealOffset((int)o);
        try {
            return (this.d[a] & 0xFF) << 8 | this.d[a + 1] & 0xFF;
        }
        catch (IndexOutOfBoundsException e) {
            throw new ConfusedCFRException(e);
        }
    }

    @Override
    public short getU1At(long o) throws ConfusedCFRException {
        int a = this.getRealOffset((int)o);
        try {
            return (short)(this.d[a] & 0xFF);
        }
        catch (IndexOutOfBoundsException e) {
            throw new ConfusedCFRException(e);
        }
    }

    @Override
    public byte getS1At(long o) {
        int a = this.getRealOffset((int)o);
        try {
            return this.d[a];
        }
        catch (IndexOutOfBoundsException e) {
            throw new ConfusedCFRException(e);
        }
    }

    @Override
    public byte[] getBytesAt(int count, long offset) {
        int a = this.getRealOffset((int)offset);
        byte[] res = new byte[count];
        System.arraycopy(this.d, a, res, 0, count);
        return res;
    }
}

