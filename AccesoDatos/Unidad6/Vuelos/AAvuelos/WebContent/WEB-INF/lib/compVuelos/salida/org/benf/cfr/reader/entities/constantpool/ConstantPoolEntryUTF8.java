/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.constantpool;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import org.benf.cfr.reader.entities.AbstractConstantPoolEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;

public class ConstantPoolEntryUTF8
extends AbstractConstantPoolEntry {
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static final long OFFSET_OF_LENGTH = 1L;
    private static final long OFFSET_OF_DATA = 3L;
    private final int length;
    private final String value;
    private static final AtomicInteger idx = new AtomicInteger();

    public ConstantPoolEntryUTF8(ConstantPool cp, ByteData data, Options options) {
        super(cp);
        this.length = data.getU2At(1L);
        byte[] bytes = data.getBytesAt(this.length, 3L);
        char[] outchars = new char[bytes.length];
        String tmpValue = null;
        int out = 0;
        try {
            for (int i = 0; i < bytes.length; ++i) {
                byte y;
                byte x = bytes[i];
                if ((x & 0x80) == 0) {
                    outchars[out++] = (char)x;
                    continue;
                }
                if ((x & 0xE0) == 192) {
                    if (((y = bytes[++i]) & 0xC0) == 128) {
                        int val = ((x & 0x1F) << 6) + (y & 0x3F);
                        outchars[out++] = (char)val;
                        continue;
                    }
                    throw new IllegalArgumentException();
                }
                if ((x & 0xF0) == 224) {
                    y = bytes[++i];
                    byte z = bytes[++i];
                    if ((y & 0xC0) == 128 && (z & 0xC0) == 128) {
                        int val = ((x & 0xF) << 12) + ((y & 0x3F) << 6) + (z & 0x3F);
                        outchars[out++] = (char)val;
                        continue;
                    }
                    throw new IllegalArgumentException();
                }
                throw new IllegalArgumentException();
            }
            tmpValue = new String(outchars, 0, out);
        }
        catch (IllegalArgumentException illegalArgumentException) {
        }
        catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            // empty catch block
        }
        if (tmpValue == null) {
            tmpValue = new String(bytes, UTF8_CHARSET);
        }
        if (tmpValue.length() > 512 && ((Boolean)options.getOption(OptionsImpl.HIDE_LONGSTRINGS)).booleanValue()) {
            tmpValue = "longStr" + idx.getAndIncrement() + "[" + tmpValue.substring(0, 10).replace('\r', '_').replace('\n', '_') + "]";
        }
        this.value = tmpValue;
    }

    @Override
    public long getRawByteLength() {
        return 3 + this.length;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public void dump(Dumper d) {
        d.print("CONSTANT_UTF8 value=" + this.value);
    }

    public String toString() {
        return "ConstantUTF8[" + this.value + "]";
    }
}

