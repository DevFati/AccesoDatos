/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.ExpressionReplacingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SwitchExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CommentStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionYield;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredSwitch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredThrow;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class SwitchExpressionRewriter
extends AbstractExpressionRewriter
implements StructuredStatementTransformer {
    private final boolean experimental;
    private final Method method;
    private DecompilerComments comments;
    private final Set<StructuredStatement> classifiedEmpty = SetFactory.newIdentitySet();
    private static final Predicate<Op04StructuredStatement> notEmpty = new Predicate<Op04StructuredStatement>(){

        @Override
        public boolean test(Op04StructuredStatement in) {
            return !(in.getStatement() instanceof Nop) && !(in.getStatement() instanceof CommentStatement);
        }
    };

    public SwitchExpressionRewriter(DecompilerComments comments, Method method) {
        this.comments = comments;
        this.experimental = OptionsImpl.switchExpressionVersion.isExperimentalIn(method.getClassFile().getClassFileVersion());
        this.method = method;
    }

    public void transform(Op04StructuredStatement root) {
        this.doTransform(root);
        this.doAggressiveTransforms(root);
        this.rewriteBlockSwitches(root);
    }

    private void doTransform(Op04StructuredStatement root) {
        root.transform(this, new StructuredScope());
    }

    private void rewriteBlockSwitches(Op04StructuredStatement root) {
        BlockSwitchDiscoverer bsd = new BlockSwitchDiscoverer();
        root.transform(bsd, new StructuredScope());
        if (bsd.blockSwitches.isEmpty()) {
            return;
        }
        Set<StatementContainer> creators = SetFactory.newSet();
        for (List<Op04StructuredStatement> list : bsd.blockSwitches.values()) {
            creators.addAll(list);
        }
        LValueSingleUsageCheckingRewriter scr = new LValueSingleUsageCheckingRewriter(creators);
        root.transform(new ExpressionRewriterTransformer(scr), new StructuredScope());
        for (Map.Entry<StructuredStatement, List<Op04StructuredStatement>> entry : bsd.blockSwitches.entrySet()) {
            List<Op04StructuredStatement> switches = entry.getValue();
            StructuredStatement stm = entry.getKey();
            if (!(stm instanceof Block)) continue;
            List<Op04StructuredStatement> statements = ((Block)stm).getBlockStatements();
            Set<Op04StructuredStatement> usages = SetFactory.newOrderedSet();
            Set swtchSet = SetFactory.newSet();
            for (Op04StructuredStatement swtch : switches) {
                StructuredAssignment sa;
                if (!(swtch.getStatement() instanceof StructuredAssignment) || !(sa = (StructuredAssignment)swtch.getStatement()).isCreator(sa.getLvalue())) continue;
                swtchSet.add(swtch);
                Op04StructuredStatement usage = scr.usageSites.get(sa.getLvalue());
                if (usage == null) continue;
                usages.add(usage);
            }
            block3: for (Op04StructuredStatement usage : usages) {
                int usageIdx = statements.indexOf(usage);
                for (int x = usageIdx - 1; x >= 0; --x) {
                    StructuredStatement stss;
                    Op04StructuredStatement backstm = statements.get(x);
                    if (backstm.getStatement().isEffectivelyNOP()) continue;
                    if (!swtchSet.contains(backstm) || !((stss = backstm.getStatement()) instanceof StructuredAssignment)) continue block3;
                    StructuredAssignment sa = (StructuredAssignment)stss;
                    ExpressionReplacingRewriter err = new ExpressionReplacingRewriter(new LValueExpression(sa.getLvalue()), sa.getRvalue());
                    usage.getStatement().rewriteExpressions(err);
                    backstm.nopOut();
                }
            }
        }
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        if (in instanceof StructuredSwitch) {
            Op04StructuredStatement container = in.getContainer();
            this.rewrite(container, scope);
            return container.getStatement();
        }
        return in;
    }

    public void rewrite(Op04StructuredStatement root, StructuredScope scope) {
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) {
            return;
        }
        if (this.replaceSwitch(root, structuredStatements, scope) && this.experimental) {
            this.comments.addComment(DecompilerComment.EXPERIMENTAL_FEATURE);
        }
    }

    private boolean replaceSwitch(Op04StructuredStatement container, List<StructuredStatement> structuredStatements, StructuredScope scope) {
        int itm;
        StructuredStatement swat = structuredStatements.get(0);
        if (!(swat instanceof StructuredSwitch)) {
            return false;
        }
        StructuredSwitch swatch = (StructuredSwitch)swat;
        Op04StructuredStatement swBody = swatch.getBody();
        if (!(swBody.getStatement() instanceof Block)) {
            return false;
        }
        Block b = (Block)swBody.getStatement();
        List<Op04StructuredStatement> content = b.getBlockStatements();
        int size = content.size();
        List<Pair> extracted = ListFactory.newList();
        List<Pair<Op04StructuredStatement, StructuredStatement>> replacements = ListFactory.newList();
        LValue target = null;
        for (itm = 0; itm < size && target == null; ++itm) {
            target = this.extractSwitchLValue(swatch.getBlockIdentifier(), content.get(itm), itm == size - 1);
        }
        if (target == null) {
            return false;
        }
        for (itm = 0; itm < size; ++itm) {
            Pair<StructuredCase, Expression> e = this.extractSwitchEntryPair(target, swatch.getBlockIdentifier(), content.get(itm), replacements, itm == size - 1);
            if (e == null) {
                return false;
            }
            extracted.add(e);
        }
        StructuredStatement declarationContainer = scope.get(1);
        if (!(declarationContainer instanceof Block)) {
            return false;
        }
        List<Op04StructuredStatement> blockContent = ((Block)declarationContainer).getBlockStatements();
        Op04StructuredStatement definition = null;
        UsageCheck usageCheck = new UsageCheck(target);
        for (Op04StructuredStatement op04StructuredStatement : blockContent) {
            if (definition == null) {
                StructuredStatement stm = op04StructuredStatement.getStatement();
                if (!(stm instanceof StructuredDefinition) || !target.equals(((StructuredDefinition)stm).getLvalue())) continue;
                definition = op04StructuredStatement;
                continue;
            }
            if (op04StructuredStatement == container) break;
            op04StructuredStatement.getStatement().rewriteExpressions(usageCheck);
            if (!usageCheck.failed) continue;
            return false;
        }
        if (definition == null) {
            return false;
        }
        for (Pair pair : replacements) {
            ((Op04StructuredStatement)pair.getFirst()).replaceStatement((StructuredStatement)pair.getSecond());
        }
        List<SwitchExpression.Branch> items = ListFactory.newList();
        for (Pair e : extracted) {
            items.add(new SwitchExpression.Branch(((StructuredCase)e.getFirst()).getValues(), (Expression)e.getSecond()));
        }
        definition.nopOut();
        StructuredAssignment structuredAssignment = new StructuredAssignment(BytecodeLoc.TODO, target, new SwitchExpression(BytecodeLoc.TODO, target.getInferredJavaType(), swatch.getSwitchOn(), items));
        swat.getContainer().replaceStatement(structuredAssignment);
        Op04StructuredStatement switchStatementContainer = structuredAssignment.getContainer();
        structuredAssignment.markCreator(target, switchStatementContainer);
        return true;
    }

    private LValue extractSwitchLValue(BlockIdentifier blockIdentifier, Op04StructuredStatement item, boolean last) {
        SwitchExpressionSearcher ses = new SwitchExpressionSearcher(blockIdentifier);
        item.transform(ses, new StructuredScope());
        if (ses.found != null) {
            return ses.found;
        }
        if (last) {
            ses.checkLast();
        }
        return ses.found;
    }

    private Pair<StructuredCase, Expression> extractSwitchEntryPair(LValue target, BlockIdentifier blockIdentifier, Op04StructuredStatement item, List<Pair<Op04StructuredStatement, StructuredStatement>> replacements, boolean last) {
        StructuredStatement stm = item.getStatement();
        if (!(stm instanceof StructuredCase)) {
            return null;
        }
        StructuredCase sc = (StructuredCase)stm;
        Expression res = this.extractSwitchEntry(target, blockIdentifier, sc.getBody(), replacements, last);
        if (res == null) {
            return null;
        }
        return Pair.make(sc, res);
    }

    private Expression extractSwitchEntry(LValue target, BlockIdentifier blockIdentifier, Op04StructuredStatement body, List<Pair<Op04StructuredStatement, StructuredStatement>> replacements, boolean last) {
        SwitchExpressionTransformer transformer = new SwitchExpressionTransformer(target, blockIdentifier, replacements, last);
        body.transform(transformer, new StructuredScope());
        if (transformer.failed) {
            return null;
        }
        if (!transformer.lastMarked) {
            return null;
        }
        if (transformer.lastAssign && !last) {
            return null;
        }
        if (transformer.totalStatements == 1 && transformer.singleValue != null) {
            return transformer.singleValue;
        }
        return new StructuredStatementExpression(target.getInferredJavaType(), body.getStatement());
    }

    private RollState getRollState(Op04StructuredStatement body) {
        StructuredStatement s = body.getStatement();
        if (!(s instanceof Block)) {
            return new RollState();
        }
        Block b = (Block)s;
        List<Op04StructuredStatement> prequel = ListFactory.newList();
        LinkedList<ClassifiedStm> tt = ListFactory.newLinkedList();
        List<Op04StructuredStatement> others = ListFactory.newList();
        Iterator<Op04StructuredStatement> it = b.getBlockStatements().iterator();
        Set<Expression> directs = SetFactory.newSet();
        boolean found = false;
        boolean inPrequel = this.method.getClassFile().isInnerClass();
        while (it.hasNext()) {
            Op04StructuredStatement item = it.next();
            if (item.getStatement().isEffectivelyNOP()) continue;
            if (inPrequel) {
                if (this.prequelAssign(item, directs)) {
                    prequel.add(item);
                    continue;
                }
                inPrequel = false;
            }
            ClassifiedStm type = this.classify(item);
            if (type.type == ClassifyType.CHAINED_CONSTRUCTOR) {
                others.add(item);
                while (it.hasNext()) {
                    others.add(it.next());
                }
                found = true;
                continue;
            }
            tt.add(type);
        }
        if (!found) {
            return new RollState();
        }
        return new RollState(prequel, tt, others, b, directs);
    }

    private boolean prequelAssign(Op04StructuredStatement item, Set<Expression> directs) {
        StructuredStatement s = item.getStatement();
        if (!(s instanceof StructuredAssignment)) {
            return false;
        }
        LValue lv = ((StructuredAssignment)s).getLvalue();
        if (!(lv instanceof FieldVariable)) {
            return false;
        }
        FieldVariable fv = (FieldVariable)lv;
        if (!fv.objectIsThis()) {
            return false;
        }
        directs.add(new LValueExpression(lv));
        directs.add(((StructuredAssignment)s).getRvalue());
        return true;
    }

    private boolean rollOne(Op04StructuredStatement root, UnaryFunction<RollState, Boolean> apply) {
        RollState rollState = this.getRollState(root);
        if (!rollState.valid) {
            return false;
        }
        if (apply.invoke(rollState).booleanValue()) {
            List<Op04StructuredStatement> mutableBlockStatements = rollState.block.getBlockStatements();
            mutableBlockStatements.clear();
            mutableBlockStatements.addAll(rollState.prequel);
            for (ClassifiedStm t : rollState.switchdata) {
                mutableBlockStatements.add(t.stm);
            }
            mutableBlockStatements.addAll(rollState.remainder);
            this.doTransform(root);
        }
        return true;
    }

    private void doAggressiveTransforms(Op04StructuredStatement root) {
        if (!this.method.isConstructor()) {
            return;
        }
        if (!this.rollOne(root, new UnaryFunction<RollState, Boolean>(){

            @Override
            public Boolean invoke(RollState arg) {
                return SwitchExpressionRewriter.this.rollUpEmptySwitches(arg);
            }
        })) {
            return;
        }
        this.rollOne(root, new UnaryFunction<RollState, Boolean>(){

            @Override
            public Boolean invoke(RollState arg) {
                return SwitchExpressionRewriter.this.rollUpEmptySwitchCreation(arg);
            }
        });
        this.rollOne(root, new UnaryFunction<RollState, Boolean>(){

            @Override
            public Boolean invoke(RollState arg) {
                return SwitchExpressionRewriter.this.rollUpEmptySwitchAggregation(arg);
            }
        });
        this.rollOne(root, new UnaryFunction<RollState, Boolean>(){

            @Override
            public Boolean invoke(RollState arg) {
                return SwitchExpressionRewriter.this.rollSingleDefault(arg);
            }
        });
    }

    private boolean rollSingleDefault(RollState rollState) {
        int idx;
        if (rollState.switchdata.size() != 1) {
            return false;
        }
        ClassifiedStm t = rollState.switchdata.get(0);
        if (t.type != ClassifyType.EMPTY_SWITCH) {
            return false;
        }
        if (rollState.remainder.isEmpty()) {
            return false;
        }
        Op04StructuredStatement call = rollState.remainder.get(0);
        StructuredStatement s = call.getStatement();
        if (!(s instanceof StructuredExpressionStatement)) {
            return false;
        }
        Expression e = ((StructuredExpressionStatement)s).getExpression();
        List<Expression> args = null;
        if (e instanceof MemberFunctionInvokation && ((MemberFunctionInvokation)e).isInitMethod()) {
            args = ((MemberFunctionInvokation)e).getArgs();
        } else if (e instanceof SuperFunctionInvokation) {
            args = ((SuperFunctionInvokation)e).getArgs();
        }
        if (args == null) {
            return false;
        }
        for (idx = 0; idx < args.size() && rollState.directs.contains(args.get(idx)); ++idx) {
        }
        if (idx >= args.size()) {
            return false;
        }
        Expression tmpValue = args.get(idx);
        LocalVariable tmp = new LocalVariable("cfr_switch_hack2", tmpValue.getInferredJavaType());
        rollState.switchdata.add(0, new ClassifiedStm(ClassifyType.DEFINITION, new Op04StructuredStatement(new StructuredDefinition(tmp))));
        this.addToSwitch(t.stm, new Op04StructuredStatement(new StructuredAssignment(BytecodeLoc.TODO, tmp, tmpValue)));
        args.set(idx, new LValueExpression(tmp));
        return true;
    }

    private boolean rollUpEmptySwitchAggregation(RollState rollState) {
        LinkedList<ClassifiedStm> tt = rollState.switchdata;
        Iterator<ClassifiedStm> di = tt.descendingIterator();
        ClassifiedStm last = null;
        boolean doneWork = false;
        while (di.hasNext()) {
            ClassifiedStm curr = di.next();
            if (last != null && curr.type == ClassifyType.SWITCH_EXPRESSION && last.type == ClassifyType.EMPTY_SWITCH) {
                this.combineSwitchExpressionWithOther(curr, last);
                di.remove();
                doneWork = true;
                continue;
            }
            last = curr;
        }
        return doneWork;
    }

    private void combineSwitchExpressionWithOther(ClassifiedStm switchExpression, ClassifiedStm other) {
        StructuredAssignment assignment = (StructuredAssignment)switchExpression.stm.getStatement();
        LValue lv = assignment.getLvalue();
        Expression se = assignment.getRvalue();
        LinkedList<Op04StructuredStatement> newBlockContent = ListFactory.newLinkedList();
        LocalVariable tmp = new LocalVariable("cfr_switch_hack", lv.getInferredJavaType());
        newBlockContent.add(new Op04StructuredStatement(new StructuredAssignment(BytecodeLoc.TODO, tmp, se, true)));
        newBlockContent.add(other.stm);
        newBlockContent.add(new Op04StructuredStatement(new StructuredExpressionYield(BytecodeLoc.TODO, new LValueExpression(tmp))));
        Block newBlock = new Block(newBlockContent, true);
        SwitchExpression nse = new SwitchExpression(BytecodeLoc.TODO, lv.getInferredJavaType(), Literal.INT_ZERO, Collections.singletonList(new SwitchExpression.Branch(Collections.<Expression>emptyList(), new StructuredStatementExpression(lv.getInferredJavaType(), newBlock))));
        StructuredAssignment nsa = new StructuredAssignment(BytecodeLoc.TODO, lv, nse, true);
        other.type = ClassifyType.SWITCH_EXPRESSION;
        other.stm = new Op04StructuredStatement(nsa);
    }

    private boolean rollUpEmptySwitchCreation(RollState rollState) {
        LinkedList<ClassifiedStm> tt = rollState.switchdata;
        Iterator<ClassifiedStm> di = tt.descendingIterator();
        ClassifiedStm last = null;
        boolean doneWork = false;
        while (di.hasNext()) {
            ClassifiedStm curr = di.next();
            if (curr.type == ClassifyType.EMPTY_SWITCH && last != null && (last.type == ClassifyType.OTHER_CREATION || last.type == ClassifyType.SWITCH_EXPRESSION)) {
                this.combineEmptySwitchWithCreation(curr, last);
                doneWork = true;
                continue;
            }
            last = curr;
        }
        return doneWork;
    }

    private void combineEmptySwitchWithCreation(ClassifiedStm switchStm, ClassifiedStm assignStm) {
        StructuredAssignment stm = (StructuredAssignment)assignStm.stm.getStatement();
        Expression rhs = stm.getRvalue();
        LValue lhs = stm.getLvalue();
        this.addToSwitch(switchStm.stm, new Op04StructuredStatement(new StructuredAssignment(BytecodeLoc.TODO, lhs, rhs)));
        StructuredStatement swtch = switchStm.stm.getStatement();
        assignStm.stm.replaceStatement(swtch);
        assignStm.type = ClassifyType.EMPTY_SWITCH;
        switchStm.stm.replaceStatement(new StructuredDefinition(lhs));
        switchStm.type = ClassifyType.DEFINITION;
    }

    private boolean rollUpEmptySwitches(RollState rollState) {
        LinkedList<ClassifiedStm> tt = rollState.switchdata;
        List<ClassifiedStm> lt = ListFactory.newList(tt);
        boolean doneWork = false;
        for (int x = lt.size() - 2; x >= 0; --x) {
            ClassifiedStm last;
            ClassifiedStm curr = lt.get(x);
            ClassifiedStm classifiedStm = last = x + 1 < lt.size() ? lt.get(x + 1) : null;
            if (curr.type != ClassifyType.EMPTY_SWITCH || last == null || last.type != ClassifyType.OTHER && last.type != ClassifyType.EMPTY_SWITCH) continue;
            this.addToSwitch(curr.stm, last.stm);
            last.stm = curr.stm;
            last.type = ClassifyType.EMPTY_SWITCH;
            lt.remove(x);
            ++x;
            doneWork = true;
        }
        if (doneWork) {
            rollState.switchdata.clear();
            rollState.switchdata.addAll(lt);
        }
        return doneWork;
    }

    private void addToSwitch(Op04StructuredStatement swtch, Op04StructuredStatement add) {
        StructuredSwitch ss = (StructuredSwitch)swtch.getStatement();
        Block block = (Block)ss.getBody().getStatement();
        StructuredCase caseStm = (StructuredCase)block.getOneStatementIfPresent().getSecond().getStatement();
        Block block1 = (Block)caseStm.getBody().getStatement();
        block1.setIndenting(true);
        block1.addStatement(add);
    }

    private ClassifiedStm classify(Op04StructuredStatement item) {
        StructuredStatement stm = item.getStatement();
        if (stm instanceof StructuredDefinition) {
            return new ClassifiedStm(ClassifyType.DEFINITION, item);
        }
        if (stm instanceof StructuredAssignment) {
            StructuredAssignment as = (StructuredAssignment)stm;
            LValue lv = as.getLvalue();
            if (!as.isCreator(lv)) {
                return new ClassifiedStm(ClassifyType.OTHER, item);
            }
            Expression rv = as.getRvalue();
            if (rv instanceof SwitchExpression) {
                return new ClassifiedStm(ClassifyType.SWITCH_EXPRESSION, item);
            }
            return new ClassifiedStm(ClassifyType.OTHER_CREATION, item);
        }
        if (stm instanceof StructuredSwitch) {
            StructuredSwitch ss = (StructuredSwitch)stm;
            if (ss.isOnlyEmptyDefault() || this.classifiedEmpty.contains(ss)) {
                this.classifiedEmpty.add(ss);
                return new ClassifiedStm(ClassifyType.EMPTY_SWITCH, item);
            }
            return new ClassifiedStm(ClassifyType.OTHER, item);
        }
        if (this.isConstructorChain(item)) {
            return new ClassifiedStm(ClassifyType.CHAINED_CONSTRUCTOR, item);
        }
        return new ClassifiedStm(ClassifyType.OTHER, item);
    }

    private boolean isConstructorChain(Op04StructuredStatement item) {
        StructuredStatement s = item.getStatement();
        if (!(s instanceof StructuredExpressionStatement)) {
            return false;
        }
        Expression e = ((StructuredExpressionStatement)s).getExpression();
        if (e instanceof SuperFunctionInvokation) {
            return true;
        }
        if (!(e instanceof MemberFunctionInvokation)) {
            return false;
        }
        return ((MemberFunctionInvokation)e).isInitMethod();
    }

    private static enum ClassifyType {
        NONE,
        EMPTY_SWITCH,
        SWITCH_EXPRESSION,
        OTHER_CREATION,
        CHAINED_CONSTRUCTOR,
        DEFINITION,
        OTHER;

    }

    private class ClassifiedStm {
        ClassifyType type;
        Op04StructuredStatement stm;

        ClassifiedStm(ClassifyType type, Op04StructuredStatement stm) {
            this.type = type;
            this.stm = stm;
        }

        public String toString() {
            return "{" + (Object)((Object)this.type) + ", " + this.stm + '}';
        }
    }

    class RollState {
        boolean valid;
        List<Op04StructuredStatement> prequel;
        LinkedList<ClassifiedStm> switchdata;
        List<Op04StructuredStatement> remainder;
        Block block;
        private Set<Expression> directs;

        RollState() {
            this.valid = false;
        }

        RollState(List<Op04StructuredStatement> prequel, LinkedList<ClassifiedStm> switchdata, List<Op04StructuredStatement> remainder, Block block, Set<Expression> directs) {
            this.directs = directs;
            this.valid = true;
            this.prequel = prequel;
            this.switchdata = switchdata;
            this.remainder = remainder;
            this.block = block;
        }
    }

    static class SwitchExpressionTransformer
    implements StructuredStatementTransformer {
        private UsageCheck rewriter;
        private BlockIdentifier blockIdentifier;
        private List<Pair<Op04StructuredStatement, StructuredStatement>> replacements;
        private boolean last;
        private final LValue target;
        private boolean failed;
        private boolean lastAssign = true;
        private boolean lastMarked = false;
        private Expression singleValue = null;
        private int totalStatements;

        private SwitchExpressionTransformer(LValue target, BlockIdentifier blockIdentifier, List<Pair<Op04StructuredStatement, StructuredStatement>> replacements, boolean last) {
            this.target = target;
            this.rewriter = new UsageCheck(target);
            this.blockIdentifier = blockIdentifier;
            this.replacements = replacements;
            this.last = last;
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            if (this.failed) {
                return in;
            }
            this.lastMarked = false;
            this.lastAssign = true;
            if (in.isEffectivelyNOP()) {
                return in;
            }
            if (!(in instanceof Block)) {
                ++this.totalStatements;
            }
            if (in instanceof StructuredBreak) {
                BreakClassification bk = this.classifyBreak((StructuredBreak)in, scope);
                switch (bk) {
                    case CORRECT: {
                        this.lastMarked = true;
                        this.lastAssign = false;
                        --this.totalStatements;
                        this.replacements.add(Pair.make(in.getContainer(), StructuredComment.EMPTY_COMMENT));
                        return in;
                    }
                    case INNER: {
                        break;
                    }
                    case TOO_FAR: {
                        this.failed = true;
                        return in;
                    }
                }
            }
            if (in instanceof StructuredReturn) {
                this.failed = true;
                return in;
            }
            if (in instanceof StructuredAssignment && ((StructuredAssignment)in).getLvalue().equals(this.target)) {
                Set<Op04StructuredStatement> nextFallThrough = scope.getNextFallThrough(in);
                this.lastMarked = true;
                this.replacements.add(Pair.make(in.getContainer(), new StructuredExpressionYield(BytecodeLoc.TODO, ((StructuredAssignment)in).getRvalue())));
                this.singleValue = ((StructuredAssignment)in).getRvalue();
                boolean foundBreak = false;
                for (Op04StructuredStatement fall : nextFallThrough) {
                    StructuredStatement fallStatement = fall.getStatement();
                    if (fallStatement.isEffectivelyNOP()) continue;
                    if (fallStatement instanceof StructuredBreak) {
                        BreakClassification bk = this.classifyBreak((StructuredBreak)fallStatement, scope);
                        if (bk != BreakClassification.CORRECT) {
                            this.failed = true;
                            return in;
                        }
                        foundBreak = true;
                        continue;
                    }
                    this.failed = true;
                    return in;
                }
                if (!this.last && !foundBreak) {
                    this.failed = true;
                    return in;
                }
                return in;
            }
            if (in instanceof StructuredThrow) {
                this.replacements.add(Pair.make(in.getContainer(), in));
                this.singleValue = new StructuredStatementExpression(this.target.getInferredJavaType(), in);
                this.lastAssign = false;
                this.lastMarked = true;
            }
            in.rewriteExpressions(this.rewriter);
            if (this.rewriter.failed) {
                this.failed = true;
                return in;
            }
            in.transformStructuredChildren(this, scope);
            return in;
        }

        BreakClassification classifyBreak(StructuredBreak in, StructuredScope scope) {
            BlockIdentifier breakBlock = in.getBreakBlock();
            if (breakBlock == this.blockIdentifier) {
                return BreakClassification.CORRECT;
            }
            for (StructuredStatement stm : scope.getAll()) {
                BlockIdentifier block = stm.getBreakableBlockOrNull();
                if (block != breakBlock) continue;
                return BreakClassification.INNER;
            }
            return BreakClassification.TOO_FAR;
        }

        static enum BreakClassification {
            CORRECT,
            TOO_FAR,
            INNER;

        }
    }

    static class UsageCheck
    extends AbstractExpressionRewriter {
        private final LValue target;
        private boolean failed;

        UsageCheck(LValue target) {
            this.target = target;
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (this.target.equals(lValue)) {
                this.failed = true;
            }
            return super.rewriteExpression(lValue, ssaIdentifiers, statementContainer, flags);
        }
    }

    private static class SwitchExpressionSearcher
    implements StructuredStatementTransformer {
        StructuredStatement last = null;
        LValue found = null;
        private BlockIdentifier blockIdentifier;

        SwitchExpressionSearcher(BlockIdentifier blockIdentifier) {
            this.blockIdentifier = blockIdentifier;
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            if (this.found != null) {
                return in;
            }
            if (in instanceof Block) {
                in.transformStructuredChildren(this, scope);
                return in;
            }
            if (in instanceof StructuredBreak && this.blockIdentifier.equals(((StructuredBreak)in).getBreakBlock())) {
                this.checkLast();
                return in;
            }
            if (!in.isEffectivelyNOP()) {
                this.last = in;
            }
            in.transformStructuredChildren(this, scope);
            return in;
        }

        private void checkLast() {
            if (this.last instanceof StructuredAssignment) {
                this.found = ((StructuredAssignment)this.last).getLvalue();
            }
        }
    }

    private static class BlockSwitchDiscoverer
    implements StructuredStatementTransformer {
        Map<StructuredStatement, List<Op04StructuredStatement>> blockSwitches = MapFactory.newOrderedMap();

        private BlockSwitchDiscoverer() {
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            if (in instanceof StructuredAssignment && ((StructuredAssignment)in).getRvalue() instanceof SwitchExpression) {
                Op04StructuredStatement switchStatementContainer = in.getContainer();
                StructuredStatement parent = scope.get(0);
                if (parent != null) {
                    List<Op04StructuredStatement> targetPairs = this.blockSwitches.get(parent);
                    if (targetPairs == null) {
                        targetPairs = ListFactory.newList();
                        this.blockSwitches.put(parent, targetPairs);
                    }
                    targetPairs.add(switchStatementContainer);
                }
            }
            in.transformStructuredChildren(this, scope);
            return in;
        }
    }

    private static class LValueSingleUsageCheckingRewriter
    extends AbstractExpressionRewriter {
        Map<LValue, Boolean> usages = MapFactory.newMap();
        Map<LValue, Op04StructuredStatement> usageSites = MapFactory.newMap();
        private Set<StatementContainer> creators;

        LValueSingleUsageCheckingRewriter(Set<StatementContainer> creators) {
            this.creators = creators;
        }

        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            Boolean prev = this.usages.get(lValue);
            if (prev == Boolean.FALSE) {
                return lValue;
            }
            if (prev == null) {
                if (this.creators.contains(statementContainer)) {
                    return lValue;
                }
                this.usages.put(lValue, Boolean.TRUE);
                this.usageSites.put(lValue, (Op04StructuredStatement)statementContainer);
            } else {
                this.usages.put(lValue, Boolean.FALSE);
                this.usageSites.remove(lValue);
            }
            return lValue;
        }
    }
}

