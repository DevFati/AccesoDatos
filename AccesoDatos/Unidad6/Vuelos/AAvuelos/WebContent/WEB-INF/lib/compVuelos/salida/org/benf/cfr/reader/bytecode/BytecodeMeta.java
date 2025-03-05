/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

public class BytecodeMeta {
    private final EnumSet<CodeInfoFlag> flags = EnumSet.noneOf(CodeInfoFlag.class);
    private final Set<Integer> livenessClashes = SetFactory.newSet();
    private final Map<Integer, JavaTypeInstance> iteratedTypeHints = MapFactory.newMap();
    private final Options options;

    public BytecodeMeta(List<Op01WithProcessedDataAndByteJumps> op1s, AttributeCode code, Options options) {
        this.options = options;
        int flagCount = CodeInfoFlag.values().length;
        if (!code.getExceptionTableEntries().isEmpty()) {
            this.flags.add(CodeInfoFlag.USES_EXCEPTIONS);
        }
        for (Op01WithProcessedDataAndByteJumps op : op1s) {
            switch (op.getJVMInstr()) {
                case MONITOREXIT: 
                case MONITORENTER: {
                    this.flags.add(CodeInfoFlag.USES_MONITORS);
                    break;
                }
                case INVOKEDYNAMIC: {
                    this.flags.add(CodeInfoFlag.USES_INVOKEDYNAMIC);
                    break;
                }
                case TABLESWITCH: 
                case LOOKUPSWITCH: {
                    this.flags.add(CodeInfoFlag.SWITCHES);
                }
            }
            if (this.flags.size() != flagCount) continue;
            return;
        }
    }

    public boolean has(CodeInfoFlag flag) {
        return this.flags.contains((Object)flag);
    }

    public void set(CodeInfoFlag flag) {
        this.flags.add(flag);
    }

    public void informLivenessClashes(Set<Integer> slots) {
        this.flags.add(CodeInfoFlag.LIVENESS_CLASH);
        this.livenessClashes.addAll(slots);
    }

    public void takeIteratedTypeHint(InferredJavaType inferredJavaType, JavaTypeInstance itertype) {
        int bytecodeIdx = inferredJavaType.getTaggedBytecodeLocation();
        if (bytecodeIdx < 0) {
            return;
        }
        Integer key = bytecodeIdx;
        if (this.iteratedTypeHints.containsKey(key)) {
            JavaTypeInstance already = this.iteratedTypeHints.get(key);
            if (already == null) {
                return;
            }
            if (!itertype.equals(already)) {
                this.iteratedTypeHints.put(key, null);
            }
        } else {
            this.flags.add(CodeInfoFlag.ITERATED_TYPE_HINTS);
            this.iteratedTypeHints.put(key, itertype);
        }
    }

    public Map<Integer, JavaTypeInstance> getIteratedTypeHints() {
        return this.iteratedTypeHints;
    }

    public Set<Integer> getLivenessClashes() {
        return this.livenessClashes;
    }

    public static UnaryFunction<BytecodeMeta, Boolean> hasAnyFlag(CodeInfoFlag ... flag) {
        return new FlagTest(flag);
    }

    public static UnaryFunction<BytecodeMeta, Boolean> checkParam(final PermittedOptionProvider.Argument<Boolean> param) {
        return new UnaryFunction<BytecodeMeta, Boolean>(){

            @Override
            public Boolean invoke(BytecodeMeta arg) {
                return (Boolean)arg.options.getOption(param);
            }
        };
    }

    private static class FlagTest
    implements UnaryFunction<BytecodeMeta, Boolean> {
        private final CodeInfoFlag[] flags;

        private FlagTest(CodeInfoFlag[] flags) {
            this.flags = flags;
        }

        @Override
        public Boolean invoke(BytecodeMeta arg) {
            for (CodeInfoFlag flag : this.flags) {
                if (!arg.has(flag)) continue;
                return true;
            }
            return false;
        }
    }

    public static enum CodeInfoFlag {
        USES_MONITORS,
        USES_EXCEPTIONS,
        USES_INVOKEDYNAMIC,
        LIVENESS_CLASH,
        ITERATED_TYPE_HINTS,
        SWITCHES,
        STRING_SWITCHES,
        INSTANCE_OF_MATCHES,
        MALFORMED_SWITCH;

    }
}

