/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.parse;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.DeepCloneable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueAssignmentCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.output.DumpableWithPrecedence;
import org.benf.cfr.reader.util.output.Dumper;

public interface LValue
extends DumpableWithPrecedence,
DeepCloneable<LValue>,
TypeUsageCollectable {
    public int getNumberOfCreators();

    public <T> void collectLValueAssignments(Expression var1, StatementContainer<T> var2, LValueAssignmentCollector<T> var3);

    public boolean doesBlackListLValueReplacement(LValue var1, Expression var2);

    public void collectLValueUsage(LValueUsageCollector var1);

    public SSAIdentifiers<LValue> collectVariableMutation(SSAIdentifierFactory<LValue, ?> var1);

    public LValue replaceSingleUsageLValues(LValueRewriter var1, SSAIdentifiers var2, StatementContainer var3);

    public LValue applyExpressionRewriter(ExpressionRewriter var1, SSAIdentifiers var2, StatementContainer var3, ExpressionRewriterFlags var4);

    public InferredJavaType getInferredJavaType();

    public JavaAnnotatedTypeInstance getAnnotatedCreationType();

    public boolean canThrow(ExceptionCheck var1);

    public void markFinal();

    public boolean isFinal();

    public boolean isFakeIgnored();

    public void markVar();

    public boolean isVar();

    public boolean validIterator();

    public Dumper dump(Dumper var1, boolean var2);

    public static class Creation {
        public static Dumper dump(Dumper d, LValue lValue) {
            JavaAnnotatedTypeInstance annotatedCreationType = lValue.getAnnotatedCreationType();
            if (annotatedCreationType != null) {
                annotatedCreationType.dump(d);
            } else if (lValue.isVar()) {
                d.print("var");
            } else {
                InferredJavaType inferredJavaType = lValue.getInferredJavaType();
                JavaTypeInstance t = inferredJavaType.getJavaTypeInstance();
                d.dump(t);
            }
            d.separator(" ");
            lValue.dump(d, true);
            return d;
        }
    }
}

