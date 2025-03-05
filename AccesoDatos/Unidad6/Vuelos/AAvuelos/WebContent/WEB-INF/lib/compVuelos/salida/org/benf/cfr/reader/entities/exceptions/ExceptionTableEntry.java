/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.exceptions;

import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class ExceptionTableEntry
implements Comparable<ExceptionTableEntry> {
    private static final int OFFSET_INDEX_FROM = 0;
    private static final int OFFSET_INDEX_TO = 2;
    private static final int OFFSET_INDEX_HANDLER = 4;
    private static final int OFFSET_CATCH_TYPE = 6;
    private final int bytecode_index_from;
    private final int bytecode_index_to;
    private final int bytecode_index_handler;
    private final int catch_type;
    private final int priority;

    private ExceptionTableEntry(ByteData raw, int priority) {
        this(raw.getU2At(0L), raw.getU2At(2L), raw.getU2At(4L), raw.getU2At(6L), priority);
    }

    ExceptionTableEntry(int from, int to, int handler, int catchType, int priority) {
        this.bytecode_index_from = from;
        this.bytecode_index_to = to;
        this.bytecode_index_handler = handler;
        this.catch_type = catchType;
        this.priority = priority;
        if (to < from) {
            throw new IllegalStateException("Malformed exception block, to < from");
        }
    }

    JavaRefTypeInstance getCatchType(ConstantPool cp) {
        if (this.catch_type == 0) {
            return cp.getClassCache().getRefClassFor("java.lang.Throwable");
        }
        JavaRefTypeInstance refTypeInstance = (JavaRefTypeInstance)cp.getClassEntry(this.catch_type).getTypeInstance();
        if (refTypeInstance.getBindingSupers() == null) {
            BindingSuperContainer bsc = BindingSuperContainer.unknownThrowable(refTypeInstance);
            refTypeInstance.forceBindingSupers(bsc);
        }
        return refTypeInstance;
    }

    ExceptionTableEntry copyWithRange(int from, int to) {
        return new ExceptionTableEntry(from, to, this.bytecode_index_handler, this.catch_type, this.priority);
    }

    int getBytecodeIndexFrom() {
        return this.bytecode_index_from;
    }

    int getBytecodeIndexTo() {
        return this.bytecode_index_to;
    }

    int getBytecodeIndexHandler() {
        return this.bytecode_index_handler;
    }

    int getCatchType() {
        return this.catch_type;
    }

    int getPriority() {
        return this.priority;
    }

    ExceptionTableEntry aggregateWith(ExceptionTableEntry later) {
        if (this.bytecode_index_from >= later.bytecode_index_from || this.bytecode_index_to != later.bytecode_index_from) {
            throw new ConfusedCFRException("Can't aggregate exceptionTableEntries");
        }
        return new ExceptionTableEntry(this.bytecode_index_from, later.bytecode_index_to, this.bytecode_index_handler, this.catch_type, this.priority);
    }

    ExceptionTableEntry aggregateWithLenient(ExceptionTableEntry later) {
        if (this.bytecode_index_from >= later.bytecode_index_from) {
            throw new ConfusedCFRException("Can't aggregate exceptionTableEntries");
        }
        return new ExceptionTableEntry(this.bytecode_index_from, later.bytecode_index_to, this.bytecode_index_handler, this.catch_type, this.priority);
    }

    public static UnaryFunction<ByteData, ExceptionTableEntry> getBuilder() {
        return new ExceptionTableEntryBuilder();
    }

    @Override
    public int compareTo(ExceptionTableEntry other) {
        int res = this.bytecode_index_from - other.bytecode_index_from;
        if (res != 0) {
            return res;
        }
        res = this.bytecode_index_to - other.bytecode_index_to;
        if (res != 0) {
            return 0 - res;
        }
        res = this.bytecode_index_handler - other.bytecode_index_handler;
        return res;
    }

    public String toString() {
        return "ExceptionTableEntry " + this.priority + " : [" + this.bytecode_index_from + "->" + this.bytecode_index_to + ") : " + this.bytecode_index_handler;
    }

    private static class ExceptionTableEntryBuilder
    implements UnaryFunction<ByteData, ExceptionTableEntry> {
        int idx = 0;

        ExceptionTableEntryBuilder() {
        }

        @Override
        public ExceptionTableEntry invoke(ByteData arg) {
            return new ExceptionTableEntry(arg, this.idx++);
        }
    }
}

