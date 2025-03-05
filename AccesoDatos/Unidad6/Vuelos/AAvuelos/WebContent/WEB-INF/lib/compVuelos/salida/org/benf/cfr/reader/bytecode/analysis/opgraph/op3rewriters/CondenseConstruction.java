/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.AnonymousClassUsage;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.CreationCollector;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;

class CondenseConstruction {
    CondenseConstruction() {
    }

    static void condenseConstruction(DCCommonState state, Method method, List<Op03SimpleStatement> statements, AnonymousClassUsage anonymousClassUsage) {
        CreationCollector creationCollector = new CreationCollector(anonymousClassUsage);
        for (Op03SimpleStatement statement : statements) {
            statement.findCreation(creationCollector);
        }
        creationCollector.condenseConstructions(method, state);
    }
}

