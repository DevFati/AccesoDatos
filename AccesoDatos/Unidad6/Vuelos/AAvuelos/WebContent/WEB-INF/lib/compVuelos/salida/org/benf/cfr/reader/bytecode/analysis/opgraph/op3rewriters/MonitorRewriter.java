/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CommentStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.MonitorStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.SetFactory;

public class MonitorRewriter {
    public static void commentMonitors(List<Op03SimpleStatement> statements) {
        List<Op03SimpleStatement> monitors = Functional.filter(statements, new TypeFilter<MonitorStatement>(MonitorStatement.class));
        if (monitors.isEmpty()) {
            return;
        }
        for (Op03SimpleStatement monitor : monitors) {
            monitor.replaceStatement(new CommentStatement(monitor.getStatement()));
        }
        for (Op03SimpleStatement monitor : monitors) {
            Op03SimpleStatement target = monitor.getTargets().get(0);
            Set<BlockIdentifier> monitorLast = SetFactory.newSet(monitor.getBlockIdentifiers());
            monitorLast.removeAll(target.getBlockIdentifiers());
            if (monitorLast.isEmpty()) continue;
            for (Op03SimpleStatement source : ListFactory.newList(monitor.getSources())) {
                Set<BlockIdentifier> sourceBlocks = source.getBlockIdentifiers();
                if (sourceBlocks.containsAll(monitorLast)) continue;
                source.replaceTarget(monitor, target);
                monitor.removeSource(source);
                target.addSource(source);
            }
        }
    }
}

