/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.variables;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.benf.cfr.reader.bytecode.analysis.variables.Ident;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariableDefault;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamer;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;

public class VariableNamerDefault
implements VariableNamer {
    private Map<Ident, NamedVariable> cached = MapFactory.newMap();
    private final Pattern indexedVarPattern = Pattern.compile("^(.*[^\\d]+)([\\d]+)$");

    @Override
    public NamedVariable getName(int originalRawOffset, Ident ident, long stackPosition, boolean clashed) {
        NamedVariable res = this.cached.get(ident);
        if (res == null) {
            res = new NamedVariableDefault("var" + ident);
            this.cached.put(ident, res);
        }
        return res;
    }

    @Override
    public void forceName(Ident ident, long stackPosition, String name) {
        NamedVariable res = this.cached.get(ident);
        if (res == null) {
            this.cached.put(ident, new NamedVariableDefault(name));
            return;
        }
        res.forceName(name);
    }

    @Override
    public List<NamedVariable> getNamedVariables() {
        return ListFactory.newList(this.cached.values());
    }

    @Override
    public void mutatingRenameUnClash(NamedVariable toRename) {
        Collection<NamedVariable> namedVars = this.cached.values();
        Map namedVariableMap = MapFactory.newMap();
        for (NamedVariable var : namedVars) {
            namedVariableMap.put(var.getStringName(), var);
        }
        String name = toRename.getStringName();
        Matcher m = this.indexedVarPattern.matcher(name);
        int start = 2;
        String prefix = name;
        if (m.matches()) {
            prefix = m.group(0);
            start = Integer.parseInt(m.group(1));
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
}

