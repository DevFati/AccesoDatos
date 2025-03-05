/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeStackMapTable
extends Attribute {
    public static final String ATTRIBUTE_NAME = "StackMapTable";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_REMAINDER = 6L;
    private static final long OFFSET_OF_NUMBER_OF_ENTRIES = 6L;
    private static final long OFFSET_OF_STACK_MAP_FRAMES = 8L;
    private final int length;
    private final boolean valid;
    private final List<StackMapFrame> stackMapFrames;

    public AttributeStackMapTable(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(2L);
        this.valid = false;
        this.stackMapFrames = null;
    }

    public AttributeStackMapTable(ByteData raw, ConstantPool cp, ClassFileVersion classFileVersion) {
        this.length = raw.getS4At(2L);
        int numEntries = raw.getU2At(6L);
        long offset = 8L;
        List frames = ListFactory.newList();
        boolean isValid = true;
        OffsettingByteData data = raw.getOffsettingOffsetData(offset);
        try {
            for (int x = 0; x < numEntries; ++x) {
                StackMapFrame frame = AttributeStackMapTable.readStackMapFrame(data);
                frames.add(frame);
            }
        }
        catch (Exception e) {
            isValid = false;
        }
        this.stackMapFrames = frames;
        this.valid = isValid;
    }

    public boolean isValid() {
        return this.valid;
    }

    public List<StackMapFrame> getStackMapFrames() {
        return this.stackMapFrames;
    }

    private static StackMapFrame readStackMapFrame(OffsettingByteData raw) {
        short frameType = raw.getU1At(0L);
        raw.advance(1L);
        if (frameType < 64) {
            return new StackMapFrameSameFrame(frameType);
        }
        if (frameType < 127) {
            return AttributeStackMapTable.same_locals_1_stack_item_frame(frameType, raw);
        }
        if (frameType < 247) {
            throw new IllegalStateException();
        }
        switch (frameType) {
            case 247: {
                return AttributeStackMapTable.same_locals_1_stack_item_frame_extended(raw);
            }
            case 248: 
            case 249: 
            case 250: {
                return AttributeStackMapTable.chop_frame(frameType, raw);
            }
            case 251: {
                return AttributeStackMapTable.same_frame_extended(raw);
            }
            case 252: 
            case 253: 
            case 254: {
                return AttributeStackMapTable.append_frame(frameType, raw);
            }
            case 255: {
                return AttributeStackMapTable.full_frame(raw);
            }
        }
        throw new IllegalStateException();
    }

    private static StackMapFrame same_locals_1_stack_item_frame(short type, OffsettingByteData raw) {
        VerificationInfo verificationInfo = AttributeStackMapTable.readVerificationInfo(raw);
        return new StackMapFrameSameLocals1SameItemFrame(type, verificationInfo);
    }

    private static StackMapFrame same_locals_1_stack_item_frame_extended(OffsettingByteData raw) {
        int offset_delta = raw.getU2At(0L);
        raw.advance(2L);
        VerificationInfo verificationInfo = AttributeStackMapTable.readVerificationInfo(raw);
        return new StackMapFrameSameLocals1SameItemFrameExtended(offset_delta, verificationInfo);
    }

    private static StackMapFrame chop_frame(short frame_type, OffsettingByteData raw) {
        int offset_delta = raw.getU2At(0L);
        raw.advance(2L);
        return new StackMapFrameChopFrame(frame_type, offset_delta);
    }

    private static StackMapFrame same_frame_extended(OffsettingByteData raw) {
        int offset_delta = raw.getU2At(0L);
        raw.advance(2L);
        return new StackMapFrameSameFrameExtended(offset_delta);
    }

    private static StackMapFrame append_frame(short frame_type, OffsettingByteData raw) {
        int offset_delta = raw.getU2At(0L);
        raw.advance(2L);
        int num_ver = frame_type - 251;
        VerificationInfo[] verificationInfos = new VerificationInfo[num_ver];
        for (int x = 0; x < num_ver; ++x) {
            verificationInfos[x] = AttributeStackMapTable.readVerificationInfo(raw);
        }
        return new StackMapFrameAppendFrame(frame_type, offset_delta, verificationInfos);
    }

    private static StackMapFrame full_frame(OffsettingByteData raw) {
        int offset_delta = raw.getU2At(0L);
        raw.advance(2L);
        int number_of_locals = raw.getU2At(0L);
        raw.advance(2L);
        long offset = 5L;
        VerificationInfo[] verificationLocals = new VerificationInfo[number_of_locals];
        for (int x = 0; x < number_of_locals; ++x) {
            verificationLocals[x] = AttributeStackMapTable.readVerificationInfo(raw);
        }
        int number_of_stack_items = raw.getU2At(0L);
        raw.advance(2L);
        VerificationInfo[] verificationStackItems = new VerificationInfo[number_of_stack_items];
        for (int x = 0; x < number_of_stack_items; ++x) {
            verificationStackItems[x] = AttributeStackMapTable.readVerificationInfo(raw);
        }
        return new StackMapFrameFullFrame(offset_delta, verificationLocals, verificationStackItems);
    }

    private static VerificationInfo readVerificationInfo(OffsettingByteData raw) {
        short type = raw.getU1At(0L);
        raw.advance(1L);
        switch (type) {
            case 0: {
                return VerificationInfoTop.INSTANCE;
            }
            case 1: {
                return VerificationInfoInteger.INSTANCE;
            }
            case 2: {
                return VerificationInfoFloat.INSTANCE;
            }
            case 3: {
                return VerificationInfoDouble.INSTANCE;
            }
            case 4: {
                return VerificationInfoLong.INSTANCE;
            }
            case 5: {
                return VerificationInfoNull.INSTANCE;
            }
            case 6: {
                return VerificationInfoUninitializedThis.INSTANCE;
            }
            case 7: {
                int u2 = raw.getU2At(0L);
                raw.advance(2L);
                return new VerificationInfoObject(u2);
            }
            case 8: {
                int u2 = raw.getU2At(0L);
                raw.advance(2L);
                return new VerificationInfoUninitialized(u2);
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d;
    }

    @Override
    public long getRawByteLength() {
        return 6L + (long)this.length;
    }

    private static class VerificationInfoUninitialized
    implements VerificationInfo {
        private static final char TYPE = '\b';
        private final int offset;

        private VerificationInfoUninitialized(int offset) {
            this.offset = offset;
        }
    }

    private static class VerificationInfoObject
    implements VerificationInfo {
        private static final char TYPE = '\u0007';
        private final int cpool_index;

        private VerificationInfoObject(int cpool_index) {
            this.cpool_index = cpool_index;
        }
    }

    private static class VerificationInfoUninitializedThis
    extends AbstractVerificationInfo {
        private static final char TYPE = '\u0006';
        private static VerificationInfo INSTANCE = new VerificationInfoUninitializedThis();

        private VerificationInfoUninitializedThis() {
        }
    }

    private static class VerificationInfoNull
    extends AbstractVerificationInfo {
        private static final char TYPE = '\u0005';
        private static VerificationInfo INSTANCE = new VerificationInfoNull();

        private VerificationInfoNull() {
        }
    }

    private static class VerificationInfoLong
    extends AbstractVerificationInfo {
        private static final char TYPE = '\u0004';
        private static VerificationInfo INSTANCE = new VerificationInfoLong();

        private VerificationInfoLong() {
        }
    }

    private static class VerificationInfoDouble
    extends AbstractVerificationInfo {
        private static final char TYPE = '\u0003';
        private static VerificationInfo INSTANCE = new VerificationInfoDouble();

        private VerificationInfoDouble() {
        }
    }

    private static class VerificationInfoFloat
    extends AbstractVerificationInfo {
        private static final char TYPE = '\u0002';
        private static VerificationInfo INSTANCE = new VerificationInfoFloat();

        private VerificationInfoFloat() {
        }
    }

    private static class VerificationInfoInteger
    extends AbstractVerificationInfo {
        private static final char TYPE = '\u0001';
        private static VerificationInfo INSTANCE = new VerificationInfoInteger();

        private VerificationInfoInteger() {
        }
    }

    private static class VerificationInfoTop
    extends AbstractVerificationInfo {
        private static final char TYPE = '\u0000';
        private static VerificationInfo INSTANCE = new VerificationInfoTop();

        private VerificationInfoTop() {
        }
    }

    private static class AbstractVerificationInfo
    implements VerificationInfo {
        private AbstractVerificationInfo() {
        }
    }

    private static interface VerificationInfo {
    }

    private static class StackMapFrameFullFrame
    implements StackMapFrame {
        private final int offset_delta;
        private final VerificationInfo[] verificationLocals;
        private final VerificationInfo[] verificationStackItems;

        private StackMapFrameFullFrame(int offset_delta, VerificationInfo[] verificationLocals, VerificationInfo[] verificationStackItems) {
            this.offset_delta = offset_delta;
            this.verificationLocals = verificationLocals;
            this.verificationStackItems = verificationStackItems;
        }
    }

    private static class StackMapFrameAppendFrame
    implements StackMapFrame {
        private final short frame_type;
        private final int offset_delta;
        private final VerificationInfo[] verificationInfos;

        private StackMapFrameAppendFrame(short frame_type, int offset_delta, VerificationInfo[] verificationInfos) {
            this.frame_type = frame_type;
            this.offset_delta = offset_delta;
            this.verificationInfos = verificationInfos;
        }
    }

    private static class StackMapFrameSameFrameExtended
    implements StackMapFrame {
        private final int offset_delta;

        private StackMapFrameSameFrameExtended(int offset_delta) {
            this.offset_delta = offset_delta;
        }
    }

    private static class StackMapFrameChopFrame
    implements StackMapFrame {
        private final short frame_type;
        private final int offset_delta;

        private StackMapFrameChopFrame(short frame_type, int offset_delta) {
            this.frame_type = frame_type;
            this.offset_delta = offset_delta;
        }
    }

    private static class StackMapFrameSameLocals1SameItemFrameExtended
    implements StackMapFrame {
        private final int offset_delta;
        private final VerificationInfo verificationInfo;

        private StackMapFrameSameLocals1SameItemFrameExtended(int offset_delta, VerificationInfo verificationInfo) {
            this.offset_delta = offset_delta;
            this.verificationInfo = verificationInfo;
        }
    }

    private static class StackMapFrameSameLocals1SameItemFrame
    implements StackMapFrame {
        private final short id;
        private final VerificationInfo verificationInfo;

        private StackMapFrameSameLocals1SameItemFrame(short id, VerificationInfo verificationInfo) {
            this.id = id;
            this.verificationInfo = verificationInfo;
        }
    }

    private static class StackMapFrameSameFrame
    implements StackMapFrame {
        private final short id;

        private StackMapFrameSameFrame(short id) {
            this.id = id;
        }
    }

    private static interface StackMapFrame {
    }
}

