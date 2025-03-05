/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.output;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.output.BytecodeDumpConsumer;
import org.benf.cfr.reader.util.output.DelegatingDumper;
import org.benf.cfr.reader.util.output.Dumper;

class BytecodeTrackingDumper
extends DelegatingDumper {
    private final Map<Method, MethodBytecode> perMethod = MapFactory.newLazyMap(MapFactory.newIdentityMap(), new UnaryFunction<Method, MethodBytecode>(){

        @Override
        public MethodBytecode invoke(Method arg) {
            return new MethodBytecode();
        }
    });
    private final BytecodeDumpConsumer consumer;

    BytecodeTrackingDumper(Dumper dumper, BytecodeDumpConsumer consumer) {
        super(dumper);
        this.consumer = consumer;
    }

    @Override
    public void informBytecodeLoc(HasByteCodeLoc loc) {
        BytecodeLoc locInfo = loc.getCombinedLoc();
        int currentDepth = this.delegate.getIndentLevel();
        int currentLine = this.delegate.getCurrentLine();
        for (Method method : locInfo.getMethods()) {
            this.perMethod.get(method).add(locInfo.getOffsetsForMethod(method), currentDepth, currentLine);
        }
    }

    @Override
    public void close() {
        List<BytecodeDumpConsumer.Item> result = ListFactory.newList();
        for (Map.Entry<Method, MethodBytecode> entry : this.perMethod.entrySet()) {
            final TreeMap<Integer, Integer> data = entry.getValue().getFinal();
            final Method method = entry.getKey();
            result.add(new BytecodeDumpConsumer.Item(){

                @Override
                public Method getMethod() {
                    return method;
                }

                @Override
                public NavigableMap<Integer, Integer> getBytecodeLocs() {
                    return data;
                }
            });
        }
        this.consumer.accept(result);
        this.delegate.close();
    }

    static class MethodBytecode {
        Map<Integer, LocAtLine> locAtLineMap = MapFactory.newTreeMap();

        MethodBytecode() {
        }

        public void add(Collection<Integer> offsetsForMethod, int currentDepth, int currentLine) {
            for (Integer offset : offsetsForMethod) {
                LocAtLine known = this.locAtLineMap.get(offset);
                if (known == null) {
                    this.locAtLineMap.put(offset, new LocAtLine(currentDepth, currentLine));
                    continue;
                }
                known.improve(currentDepth, currentLine);
            }
        }

        TreeMap<Integer, Integer> getFinal() {
            TreeMap<Integer, Integer> res = MapFactory.newTreeMap();
            Integer lastLine = -1;
            for (Map.Entry<Integer, LocAtLine> entry : this.locAtLineMap.entrySet()) {
                Integer line = entry.getValue().getCurrentLine();
                if (lastLine.equals(line)) continue;
                res.put(entry.getKey(), line);
                lastLine = line;
            }
            return res;
        }
    }

    static class LocAtLine {
        int depth;
        int currentLine;

        LocAtLine(int depth, int currentLine) {
            this.depth = depth;
            this.currentLine = currentLine;
        }

        public int getDepth() {
            return this.depth;
        }

        public int getCurrentLine() {
            return this.currentLine;
        }

        public void improve(int currentDepth, int currentLine) {
            if (currentDepth > this.depth) {
                this.depth = currentDepth;
                this.currentLine = currentLine;
            }
        }
    }
}

