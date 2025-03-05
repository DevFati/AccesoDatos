/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.List;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.Op04Rewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.AbstractMatchResultIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.CollectMatch;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Field;

public class InnerClassConstructorRewriter
implements Op04Rewriter {
    private final ClassFile classFile;
    private final LocalVariable outerArg;
    private FieldVariable matchedField;
    private StructuredStatement assignmentStatement;

    public InnerClassConstructorRewriter(ClassFile classFile, LocalVariable outerArg) {
        this.outerArg = outerArg;
        this.classFile = classFile;
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        WildcardMatch wcm1 = new WildcardMatch();
        CollectMatch m = new CollectMatch("ass1", new StructuredAssignment(BytecodeLoc.NONE, wcm1.getLValueWildCard("outercopy"), new LValueExpression(this.outerArg)));
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);
        ConstructResultCollector collector = new ConstructResultCollector();
        while (mi.hasNext()) {
            mi.advance();
            if (!m.match(mi, (MatchResultCollector)collector)) continue;
            LValue lValue = wcm1.getLValueWildCard("outercopy").getMatch();
            if (lValue instanceof FieldVariable) {
                try {
                    FieldVariable fieldVariable = (FieldVariable)lValue;
                    ClassFileField classField = this.classFile.getFieldByName(fieldVariable.getRawFieldName(), fieldVariable.getInferredJavaType().getJavaTypeInstance());
                    Field field = classField.getField();
                    if (field.testAccessFlag(AccessFlag.ACC_SYNTHETIC) && field.testAccessFlag(AccessFlag.ACC_FINAL)) {
                        this.assignmentStatement = collector.assignmentStatement;
                        this.matchedField = (FieldVariable)lValue;
                    }
                }
                catch (NoSuchFieldException noSuchFieldException) {
                    // empty catch block
                }
            }
            return;
        }
    }

    public FieldVariable getMatchedField() {
        return this.matchedField;
    }

    public StructuredStatement getAssignmentStatement() {
        return this.assignmentStatement;
    }

    private static class ConstructResultCollector
    extends AbstractMatchResultIterator {
        private StructuredStatement assignmentStatement;

        private ConstructResultCollector() {
        }

        @Override
        public void clear() {
            this.assignmentStatement = null;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            this.assignmentStatement = statement;
        }
    }
}

