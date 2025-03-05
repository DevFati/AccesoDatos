/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.variables;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.benf.cfr.reader.bytecode.analysis.variables.Ident;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariableDefault;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariableFromHint;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamer;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamerDefault;
import org.benf.cfr.reader.entities.attributes.LocalVariableEntry;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.output.IllegalIdentifierReplacement;

public class VariableNamerHinted
implements VariableNamer {
    private final VariableNamer missingNamer = new VariableNamerDefault();
    private final OrderLocalVariables orderLocalVariable = new OrderLocalVariables();
    private final Map<Integer, TreeSet<LocalVariableEntry>> localVariableEntryTreeSet = MapFactory.newLazyMap(new UnaryFunction<Integer, TreeSet<LocalVariableEntry>>(){

        @Override
        public TreeSet<LocalVariableEntry> invoke(Integer arg) {
            return new TreeSet<LocalVariableEntry>(VariableNamerHinted.this.orderLocalVariable);
        }
    });
    private final Map<LocalVariableEntry, NamedVariable> cache = MapFactory.newMap();
    private final ConstantPool cp;

    VariableNamerHinted(List<LocalVariableEntry> entryList, ConstantPool cp) {
        for (LocalVariableEntry e : entryList) {
            this.localVariableEntryTreeSet.get(e.getIndex()).add(e);
        }
        this.cp = cp;
    }

    @Override
    public NamedVariable getName(int originalRawOffset, Ident ident, long stackPosition, boolean clashed) {
        originalRawOffset = originalRawOffset > 0 ? originalRawOffset + 2 : 0;
        int sstackPos = (int)stackPosition;
        if (clashed || !this.localVariableEntryTreeSet.containsKey(sstackPos)) {
            return this.missingNamer.getName(originalRawOffset, ident, sstackPos, clashed);
        }
        LocalVariableEntry tmp = new LocalVariableEntry(originalRawOffset, 1, -1, -1, (short)stackPosition);
        TreeSet<LocalVariableEntry> lveSet = this.localVariableEntryTreeSet.get(sstackPos);
        LocalVariableEntry lve = lveSet.floor(tmp);
        if (lve == null || originalRawOffset > lve.getEndPc() && null == lveSet.ceiling(tmp)) {
            return this.missingNamer.getName(originalRawOffset, ident, sstackPos, clashed);
        }
        NamedVariable namedVariable = this.cache.get(lve);
        if (namedVariable == null) {
            String name = this.cp.getUTF8Entry(lve.getNameIndex()).getValue();
            if (IllegalIdentifierReplacement.isIllegal(name)) {
                namedVariable = new NamedVariableDefault(name);
                if (name.equals("this") && ident.getIdx() == 0) {
                    namedVariable.forceName("this");
                }
            } else {
                int genIdx = 0;
                namedVariable = new NamedVariableFromHint(name, lve.getIndex(), genIdx);
            }
            this.cache.put(lve, namedVariable);
        }
        return namedVariable;
    }

    @Override
    public List<NamedVariable> getNamedVariables() {
        return ListFactory.newList(this.cache.values());
    }

    @Override
    public void forceName(Ident ident, long stackPosition, String name) {
        this.missingNamer.forceName(ident, stackPosition, name);
    }

    @Override
    public void mutatingRenameUnClash(NamedVariable toRename) {
        Map namedVariableMap = MapFactory.newMap();
        for (NamedVariable var : this.cache.values()) {
            namedVariableMap.put(var.getStringName(), var);
        }
        for (NamedVariable var : this.missingNamer.getNamedVariables()) {
            namedVariableMap.put(var.getStringName(), var);
        }
        String name = toRename.getStringName();
        Pattern p = Pattern.compile("^(.*[^\\d]+)([\\d]+)$");
        Matcher m = p.matcher(name);
        int start = 2;
        String prefix = name;
        if (m.matches()) {
            prefix = m.group(1);
            String numPart = m.group(2);
            start = Integer.parseInt(numPart);
            ++start;
        }
        while (true) {
            String name2;
            if (!namedVariableMap.containsKey(name2 = prefix + start)) {
                toRename.forceName(name2);
                return;
            }
            ++start;
        }
    }

    private static class OrderLocalVariables
    implements Comparator<LocalVariableEntry> {
        private OrderLocalVariables() {
        }

        @Override
        public int compare(LocalVariableEntry a, LocalVariableEntry b) {
            int x = a.getIndex() - b.getIndex();
            if (x != 0) {
                return x;
            }
            return a.getStartPc() - b.getStartPc();
        }
    }
}

