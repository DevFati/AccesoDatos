/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.exceptions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.ComparableUnderEC;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.exceptions.ExceptionTableEntry;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.ListFactory;

public class ExceptionGroup {
    private int bytecodeIndexFrom;
    private int bytecodeIndexTo;
    private int minHandlerStart = Short.MAX_VALUE;
    private List<Entry> entries = ListFactory.newList();
    private final BlockIdentifier tryBlockIdentifier;
    private final ConstantPool cp;

    public ExceptionGroup(int bytecodeIndexFrom, BlockIdentifier blockIdentifier, ConstantPool cp) {
        this.bytecodeIndexFrom = bytecodeIndexFrom;
        this.tryBlockIdentifier = blockIdentifier;
        this.cp = cp;
    }

    public void add(ExceptionTableEntry entry) {
        if (entry.getBytecodeIndexHandler() == entry.getBytecodeIndexFrom()) {
            return;
        }
        if (entry.getBytecodeIndexHandler() < this.minHandlerStart) {
            this.minHandlerStart = entry.getBytecodeIndexHandler();
        }
        this.entries.add(new Entry(entry));
        if (entry.getBytecodeIndexTo() > this.bytecodeIndexTo) {
            this.bytecodeIndexTo = entry.getBytecodeIndexTo();
        }
    }

    public List<Entry> getEntries() {
        return this.entries;
    }

    public int getBytecodeIndexFrom() {
        return this.bytecodeIndexFrom;
    }

    public int getBytecodeIndexTo() {
        return this.bytecodeIndexTo;
    }

    public BlockIdentifier getTryBlockIdentifier() {
        return this.tryBlockIdentifier;
    }

    public void removeSynchronisedHandlers(Map<Integer, Integer> lutByOffset, Map<Integer, Integer> lutByIdx, List<Op01WithProcessedDataAndByteJumps> instrs) {
        Iterator<Entry> entryIterator = this.entries.iterator();
        while (entryIterator.hasNext()) {
            Entry entry = entryIterator.next();
            if (!this.isSynchronisedHandler(entry, lutByOffset, lutByIdx, instrs)) continue;
            entryIterator.remove();
        }
    }

    private boolean isSynchronisedHandler(Entry entry, Map<Integer, Integer> lutByOffset, Map<Integer, Integer> lutByIdx, List<Op01WithProcessedDataAndByteJumps> instrs) {
        ExceptionTableEntry tableEntry = entry.entry;
        Integer offset = lutByOffset.get(tableEntry.getBytecodeIndexHandler());
        if (offset == null) {
            return false;
        }
        int idx = offset;
        if (idx >= instrs.size()) {
            return false;
        }
        Op01WithProcessedDataAndByteJumps start = instrs.get(idx);
        Integer catchStore = start.getAStoreIdx();
        if (catchStore == null) {
            return false;
        }
        ++idx;
        int nUnlocks = 0;
        while (true) {
            Op01WithProcessedDataAndByteJumps next;
            JVMInstr instr;
            if (idx + 1 >= instrs.size()) {
                return false;
            }
            Op01WithProcessedDataAndByteJumps load = instrs.get(idx);
            Integer loadIdx = load.getALoadIdx();
            if (loadIdx == null && (instr = load.getJVMInstr()) != JVMInstr.LDC || (next = instrs.get(idx + 1)).getJVMInstr() != JVMInstr.MONITOREXIT) break;
            ++nUnlocks;
            idx += 2;
        }
        if (nUnlocks == 0) {
            return false;
        }
        Integer catchLoad = instrs.get(idx).getALoadIdx();
        if (!catchStore.equals(catchLoad)) {
            return false;
        }
        return instrs.get(++idx).getJVMInstr() == JVMInstr.ATHROW;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[egrp ").append(this.tryBlockIdentifier).append(" [");
        boolean bfirst = true;
        for (Entry e : this.entries) {
            bfirst = StringUtils.comma(bfirst, sb);
            sb.append(e.getPriority());
        }
        sb.append(" : ").append(this.bytecodeIndexFrom).append("->").append(this.bytecodeIndexTo).append(")]");
        return sb.toString();
    }

    public class ExtenderKey {
        private final JavaRefTypeInstance type;
        private final int handler;

        public ExtenderKey(JavaRefTypeInstance type, int handler) {
            this.type = type;
            this.handler = handler;
        }

        public JavaRefTypeInstance getType() {
            return this.type;
        }

        public int getHandler() {
            return this.handler;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            ExtenderKey that = (ExtenderKey)o;
            if (this.handler != that.handler) {
                return false;
            }
            return !(this.type != null ? !this.type.equals(that.type) : that.type != null);
        }

        public int hashCode() {
            int result = this.type != null ? this.type.hashCode() : 0;
            result = 31 * result + this.handler;
            return result;
        }
    }

    public class Entry
    implements ComparableUnderEC {
        private final ExceptionTableEntry entry;
        private final JavaRefTypeInstance refType;

        public Entry(ExceptionTableEntry entry) {
            this.entry = entry;
            this.refType = entry.getCatchType(ExceptionGroup.this.cp);
        }

        public int getBytecodeIndexTo() {
            return this.entry.getBytecodeIndexTo();
        }

        public int getBytecodeIndexHandler() {
            return this.entry.getBytecodeIndexHandler();
        }

        public boolean isJustThrowable() {
            JavaRefTypeInstance type = this.entry.getCatchType(ExceptionGroup.this.cp);
            return type.getRawName().equals("java.lang.Throwable");
        }

        public int getPriority() {
            return this.entry.getPriority();
        }

        public JavaRefTypeInstance getCatchType() {
            return this.refType;
        }

        public ExceptionGroup getExceptionGroup() {
            return ExceptionGroup.this;
        }

        public BlockIdentifier getTryBlockIdentifier() {
            return ExceptionGroup.this.getTryBlockIdentifier();
        }

        public String toString() {
            JavaRefTypeInstance name = this.getCatchType();
            return ExceptionGroup.this.toString() + " " + name.getRawName();
        }

        @Override
        public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (this.getClass() != o.getClass()) {
                return false;
            }
            Entry other = (Entry)o;
            if (!constraint.equivalent(this.entry, other.entry)) {
                return false;
            }
            return constraint.equivalent(this.refType, other.refType);
        }

        public ExtenderKey getExtenderKey() {
            return new ExtenderKey(this.refType, this.entry.getBytecodeIndexHandler());
        }
    }
}

