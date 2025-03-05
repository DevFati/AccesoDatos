/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.benf.cfr.reader.entities.attributes.Attribute;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryModuleInfo;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

public class AttributeModule
extends Attribute {
    public static final String ATTRIBUTE_NAME = "Module";
    private static final long OFFSET_OF_ATTRIBUTE_LENGTH = 2L;
    private static final long OFFSET_OF_MODULE_NAME = 6L;
    private static final long OFFSET_OF_MODULE_FLAGS = 8L;
    private static final long OFFSET_OF_MODULE_VERSION = 10L;
    private static final long OFFSET_OF_DYNAMIC_INFO = 12L;
    private final int nameIdx;
    private final int flags;
    private final int versionIdx;
    private final List<Require> requires;
    private final List<ExportOpen> exports;
    private final List<ExportOpen> opens;
    private final List<Use> uses;
    private final List<Provide> provides;
    private final int length;
    private ConstantPool cp;

    public Set<ModuleFlags> getFlags() {
        return ModuleFlags.build(this.flags);
    }

    public AttributeModule(ByteData raw, ConstantPool cp) {
        this.length = raw.getS4At(2L);
        this.cp = cp;
        this.nameIdx = raw.getU2At(6L);
        this.flags = raw.getU2At(8L);
        this.versionIdx = raw.getU2At(10L);
        long offset = 12L;
        this.requires = ListFactory.newList();
        this.exports = ListFactory.newList();
        this.opens = ListFactory.newList();
        this.uses = ListFactory.newList();
        this.provides = ListFactory.newList();
        offset = Require.read(raw, offset, this.requires);
        offset = ExportOpen.read(raw, offset, this.exports);
        offset = ExportOpen.read(raw, offset, this.opens);
        offset = Use.read(raw, offset, this.uses);
        Provide.read(raw, offset, this.provides);
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print(ATTRIBUTE_NAME);
    }

    @Override
    public long getRawByteLength() {
        return 8L + (long)this.length;
    }

    public String toString() {
        return ATTRIBUTE_NAME;
    }

    public List<Require> getRequires() {
        return this.requires;
    }

    public List<ExportOpen> getExports() {
        return this.exports;
    }

    public List<ExportOpen> getOpens() {
        return this.opens;
    }

    public List<Use> getUses() {
        return this.uses;
    }

    public List<Provide> getProvides() {
        return this.provides;
    }

    public ConstantPool getCp() {
        return this.cp;
    }

    public String getModuleName() {
        return ((ConstantPoolEntryModuleInfo)this.cp.getEntry(this.nameIdx)).getName().getValue();
    }

    public static class Provide {
        private final int index;
        private final int[] with_index;

        private Provide(int index, int[] with_index) {
            this.index = index;
            this.with_index = with_index;
        }

        public int getIndex() {
            return this.index;
        }

        public int[] getWithIndex() {
            return this.with_index;
        }

        private static void read(ByteData raw, long offset, List<Provide> tgt) {
            int num = raw.getU2At(offset);
            offset += 2L;
            for (int x = 0; x < num; ++x) {
                int index = raw.getU2At(offset);
                int count = raw.getU2At(offset + 2L);
                offset += 4L;
                int[] indices = new int[count];
                for (int y = 0; y < count; ++y) {
                    indices[y] = raw.getU2At(offset);
                    offset += 2L;
                }
                tgt.add(new Provide(index, indices));
            }
        }
    }

    public static class Use {
        int index;

        private Use(int index) {
            this.index = index;
        }

        private static long read(ByteData raw, long offset, List<Use> tgt) {
            int num = raw.getU2At(offset);
            offset += 2L;
            for (int x = 0; x < num; ++x) {
                int index = raw.getU2At(offset);
                tgt.add(new Use(index));
                offset += 2L;
            }
            return offset;
        }
    }

    public static class ExportOpen {
        private final int index;
        private final int flags;
        private final int[] to_index;

        private ExportOpen(int index, int flags, int[] to_index) {
            this.index = index;
            this.flags = flags;
            this.to_index = to_index;
        }

        public Set<ModuleContentFlags> getFlags() {
            return ModuleContentFlags.build(this.flags);
        }

        public int getIndex() {
            return this.index;
        }

        public int[] getToIndex() {
            return this.to_index;
        }

        private static long read(ByteData raw, long offset, List<ExportOpen> tgt) {
            int num = raw.getU2At(offset);
            offset += 2L;
            for (int x = 0; x < num; ++x) {
                int index = raw.getU2At(offset);
                int flags = raw.getU2At(offset + 2L);
                int count = raw.getU2At(offset + 4L);
                offset += 6L;
                int[] indices = new int[count];
                for (int y = 0; y < count; ++y) {
                    indices[y] = raw.getU2At(offset);
                    offset += 2L;
                }
                tgt.add(new ExportOpen(index, flags, indices));
            }
            return offset;
        }
    }

    public static class Require {
        private final int index;
        private final int flags;
        private final int version_index;

        public int getIndex() {
            return this.index;
        }

        public Set<ModuleContentFlags> getFlags() {
            return ModuleContentFlags.build(this.flags);
        }

        private Require(int index, int flags, int version_index) {
            this.index = index;
            this.flags = flags;
            this.version_index = version_index;
        }

        private static long read(ByteData raw, long offset, List<Require> tgt) {
            int num = raw.getU2At(offset);
            offset += 2L;
            for (int x = 0; x < num; ++x) {
                tgt.add(new Require(raw.getU2At(offset), raw.getU2At(offset + 2L), raw.getU2At(offset + 4L)));
                offset += 6L;
            }
            return offset;
        }
    }

    public static enum ModuleContentFlags {
        TRANSITIVE("/* transitive */"),
        STATIC_PHASE("/* static phase */"),
        SYNTHETIC("/* synthetic */"),
        MANDATED("/* mandated */");

        private final String comment;

        private ModuleContentFlags(String comment) {
            this.comment = comment;
        }

        public static Set<ModuleContentFlags> build(int raw) {
            TreeSet<ModuleContentFlags> res = new TreeSet<ModuleContentFlags>();
            if (0 != (raw & 0x20)) {
                res.add(TRANSITIVE);
            }
            if (0 != (raw & 0x40)) {
                res.add(STATIC_PHASE);
            }
            if (0 != (raw & 0x1000)) {
                res.add(SYNTHETIC);
            }
            if (0 != (raw & 0x8000)) {
                res.add(MANDATED);
            }
            return res;
        }

        public String toString() {
            return this.comment;
        }
    }

    public static enum ModuleFlags {
        OPEN("open"),
        SYNTHETIC("/* synthetic */"),
        MANDATED("/* mandated */");

        private final String comment;

        private ModuleFlags(String comment) {
            this.comment = comment;
        }

        public static Set<ModuleFlags> build(int raw) {
            TreeSet<ModuleFlags> res = new TreeSet<ModuleFlags>();
            if (0 != (raw & 0x20)) {
                res.add(OPEN);
            }
            if (0 != (raw & 0x1000)) {
                res.add(SYNTHETIC);
            }
            if (0 != (raw & 0x8000)) {
                res.add(MANDATED);
            }
            return res;
        }

        public String toString() {
            return this.comment;
        }
    }
}

