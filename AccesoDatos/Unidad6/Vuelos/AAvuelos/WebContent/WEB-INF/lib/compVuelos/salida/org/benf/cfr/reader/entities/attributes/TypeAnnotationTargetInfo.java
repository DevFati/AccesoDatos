/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.attributes;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.ListFactory;

public interface TypeAnnotationTargetInfo {

    public static class TypeAnnotationTypeArgumentTarget
    implements TypeAnnotationTargetInfo {
        private final int offset;
        private final short type_argument_index;

        private TypeAnnotationTypeArgumentTarget(int offset, short type_argument_index) {
            this.offset = offset;
            this.type_argument_index = type_argument_index;
        }

        static Pair<Long, TypeAnnotationTargetInfo> Read(ByteData raw, long offset) {
            int offset_val = raw.getU2At(offset);
            offset += 2L;
            short type_argument_index = raw.getU1At(offset++);
            return Pair.make(offset, new TypeAnnotationTypeArgumentTarget(offset_val, type_argument_index));
        }
    }

    public static class TypeAnnotationOffsetTarget
    implements TypeAnnotationTargetInfo {
        private final int offset;

        private TypeAnnotationOffsetTarget(int offset) {
            this.offset = offset;
        }

        static Pair<Long, TypeAnnotationTargetInfo> Read(ByteData raw, long offset) {
            int offset_val = raw.getU2At(offset);
            return Pair.make(offset += 2L, new TypeAnnotationOffsetTarget(offset_val));
        }
    }

    public static class TypeAnnotationCatchTarget
    implements TypeAnnotationTargetInfo {
        private final int exception_table_index;

        private TypeAnnotationCatchTarget(int exception_table_index) {
            this.exception_table_index = exception_table_index;
        }

        static Pair<Long, TypeAnnotationTargetInfo> Read(ByteData raw, long offset) {
            int exception_table_index = raw.getU2At(offset);
            return Pair.make(offset += 2L, new TypeAnnotationCatchTarget(exception_table_index));
        }
    }

    public static class TypeAnnotationLocalVarTarget
    implements TypeAnnotationTargetInfo {
        private final List<LocalVarTarget> targets;

        TypeAnnotationLocalVarTarget(List<LocalVarTarget> targets) {
            this.targets = targets;
        }

        public boolean matches(int offset, int slot, int tolerance) {
            for (LocalVarTarget tgt : this.targets) {
                if (!tgt.matches(offset, slot, tolerance)) continue;
                return true;
            }
            return false;
        }

        static Pair<Long, TypeAnnotationTargetInfo> Read(ByteData raw, long offset) {
            int count = raw.getU2At(offset);
            offset += 2L;
            List<LocalVarTarget> targetList = ListFactory.newList();
            for (int x = 0; x < count; ++x) {
                int start = raw.getU2At(offset);
                int length = raw.getU2At(offset += 2L);
                int index = raw.getU2At(offset += 2L);
                offset += 2L;
                targetList.add(new LocalVarTarget(start, length, index));
            }
            return Pair.make(offset, new TypeAnnotationLocalVarTarget(targetList));
        }
    }

    public static class LocalVarTarget {
        private final int start;
        private final int length;
        private final int index;

        LocalVarTarget(int start, int length, int index) {
            this.start = start;
            this.length = length;
            this.index = index;
        }

        public boolean matches(int offset, int slot, int tolerance) {
            return offset >= this.start - tolerance && offset < this.start + this.length && slot == this.index;
        }
    }

    public static class TypeAnnotationThrowsTarget
    implements TypeAnnotationTargetInfo {
        private final int throws_type_index;

        private TypeAnnotationThrowsTarget(int throws_type_index) {
            this.throws_type_index = throws_type_index;
        }

        static Pair<Long, TypeAnnotationTargetInfo> Read(ByteData raw, long offset) {
            int throws_type_index = raw.getU2At(offset);
            return Pair.make(offset += 2L, new TypeAnnotationThrowsTarget(throws_type_index));
        }

        public int getIndex() {
            return this.throws_type_index;
        }
    }

    public static class TypeAnnotationFormalParameterTarget
    implements TypeAnnotationTargetInfo {
        private final short formal_parameter_index;

        private TypeAnnotationFormalParameterTarget(short formal_parameter_index) {
            this.formal_parameter_index = formal_parameter_index;
        }

        static Pair<Long, TypeAnnotationTargetInfo> Read(ByteData raw, long offset) {
            short formal_parameter_index = raw.getU1At(offset++);
            return Pair.make(offset, new TypeAnnotationFormalParameterTarget(formal_parameter_index));
        }

        public int getIndex() {
            return this.formal_parameter_index;
        }
    }

    public static class TypeAnnotationEmptyTarget
    implements TypeAnnotationTargetInfo {
        private static TypeAnnotationTargetInfo INSTANCE = new TypeAnnotationEmptyTarget();

        static Pair<Long, TypeAnnotationTargetInfo> Read(ByteData raw, long offset) {
            return Pair.make(offset, INSTANCE);
        }

        public static TypeAnnotationTargetInfo getInstance() {
            return INSTANCE;
        }
    }

    public static class TypeAnnotationParameterBoundTarget
    implements TypeAnnotationTargetInfo {
        private final short type_parameter_index;
        private final short bound_index;

        private TypeAnnotationParameterBoundTarget(short type_parameter_index, short bound_index) {
            this.type_parameter_index = type_parameter_index;
            this.bound_index = bound_index;
        }

        static Pair<Long, TypeAnnotationTargetInfo> Read(ByteData raw, long offset) {
            short type_parameter_index = raw.getU1At(offset++);
            short bound_index = raw.getU1At(offset++);
            return Pair.make(offset, new TypeAnnotationParameterBoundTarget(type_parameter_index, bound_index));
        }

        public short getIndex() {
            return this.type_parameter_index;
        }
    }

    public static class TypeAnnotationSupertypeTarget
    implements TypeAnnotationTargetInfo {
        private final int supertype_index;

        private TypeAnnotationSupertypeTarget(int supertype_index) {
            this.supertype_index = supertype_index;
        }

        static Pair<Long, TypeAnnotationTargetInfo> Read(ByteData raw, long offset) {
            int supertype_index = raw.getU2At(offset);
            return Pair.make(offset += 2L, new TypeAnnotationSupertypeTarget(supertype_index));
        }
    }

    public static class TypeAnnotationParameterTarget
    implements TypeAnnotationTargetInfo {
        private final short type_parameter_index;

        TypeAnnotationParameterTarget(short type_parameter_index) {
            this.type_parameter_index = type_parameter_index;
        }

        static Pair<Long, TypeAnnotationTargetInfo> Read(ByteData raw, long offset) {
            short type_parameter_index = raw.getU1At(offset++);
            return Pair.make(offset, new TypeAnnotationParameterTarget(type_parameter_index));
        }

        public short getIndex() {
            return this.type_parameter_index;
        }
    }
}

