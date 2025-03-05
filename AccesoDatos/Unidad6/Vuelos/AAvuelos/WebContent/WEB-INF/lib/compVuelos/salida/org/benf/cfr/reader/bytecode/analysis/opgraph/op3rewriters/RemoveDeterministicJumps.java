/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.FinallyRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Op03Rewriters;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.TypeFilter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.CaseStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.GotoStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfExitingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.MonitorExitStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnNothingStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ReturnValueStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

public class RemoveDeterministicJumps {
    public static List<Op03SimpleStatement> apply(Method method, List<Op03SimpleStatement> statements) {
        boolean success = false;
        Set<BlockIdentifier> ignoreInThese = FinallyRewriter.getBlocksAffectedByFinally(statements);
        for (Op03SimpleStatement stm : statements) {
            if (!(stm.getStatement() instanceof AssignmentSimple) || SetUtil.hasIntersection(ignoreInThese, stm.getBlockIdentifiers())) continue;
            Map<LValue, Literal> display = MapFactory.newMap();
            success |= RemoveDeterministicJumps.propagateLiteralReturn(method, stm, display);
        }
        if (success) {
            statements = Cleaner.removeUnreachableCode(statements, true);
        }
        return statements;
    }

    private static boolean propagateLiteralReturn(Method method, Op03SimpleStatement original, Map<LValue, Literal> display) {
        Op03SimpleStatement current = original;
        Set seen = SetFactory.newSet();
        int nAssigns = 0;
        boolean adjustedOrig = false;
        int nAssignsAtAdjust = 0;
        while (current.getSources().size() == 1) {
            Boolean bool;
            if (!seen.add(current)) {
                return false;
            }
            Class<?> cls = current.getStatement().getClass();
            List<Op03SimpleStatement> curTargets = current.getTargets();
            int nTargets = curTargets.size();
            if (cls == Nop.class) {
                if (nTargets != 1) break;
                current = curTargets.get(0);
                continue;
            }
            if (cls == GotoStatement.class || cls == MonitorExitStatement.class || cls == CaseStatement.class) {
                if (nTargets != 1) break;
                current = curTargets.get(0);
                continue;
            }
            if (cls == AssignmentSimple.class) {
                Literal literal;
                AssignmentSimple assignmentSimple = (AssignmentSimple)current.getStatement();
                LValue lValue = assignmentSimple.getCreatedLValue();
                if (!(lValue instanceof StackSSALabel) && !(lValue instanceof LocalVariable) || (literal = assignmentSimple.getRValue().getComputedLiteral(display)) == null) break;
                display.put(lValue, literal);
                current = curTargets.get(0);
                ++nAssigns;
                continue;
            }
            if (cls != IfStatement.class) break;
            IfStatement ifStatement = (IfStatement)current.getStatement();
            Literal literal = ifStatement.getCondition().getComputedLiteral(display);
            Boolean bl = bool = literal == null ? null : literal.getValue().getMaybeBoolValue();
            if (bool == null) {
                if (adjustedOrig) break;
                adjustedOrig = true;
                nAssignsAtAdjust = nAssigns;
                original = current;
                current = curTargets.get(1);
                continue;
            }
            current = curTargets.get(bool != false ? 1 : 0);
        }
        Statement currentStatement = current.getStatement();
        Class<?> cls = current.getStatement().getClass();
        if (currentStatement instanceof ReturnStatement) {
            if (cls == ReturnNothingStatement.class) {
                RemoveDeterministicJumps.replace(original, adjustedOrig, new ReturnNothingStatement(currentStatement.getLoc()));
            } else if (cls == ReturnValueStatement.class) {
                ReturnValueStatement returnValueStatement = (ReturnValueStatement)current.getStatement();
                LValueUsageCollectorSimple collectorSimple = new LValueUsageCollectorSimple();
                Expression res = returnValueStatement.getReturnValue();
                res.collectUsedLValues(collectorSimple);
                if (SetUtil.hasIntersection(display.keySet(), collectorSimple.getUsedLValues())) {
                    return false;
                }
                Literal lit = res.getComputedLiteral(display);
                if (lit != null) {
                    res = lit;
                }
                RemoveDeterministicJumps.replace(original, adjustedOrig, new ReturnValueStatement(currentStatement.getLoc(), res, returnValueStatement.getFnReturnType()));
            } else {
                return false;
            }
            return true;
        }
        if (!adjustedOrig) {
            return false;
        }
        Op03SimpleStatement origTarget = original.getTargets().get(1);
        if (current == origTarget) {
            return false;
        }
        if (nAssigns != nAssignsAtAdjust || nAssigns == 0) {
            return false;
        }
        original.replaceTarget(origTarget, current);
        origTarget.removeSource(original);
        current.addSource(original);
        return true;
    }

    private static void replaceConditionalReturn(Op03SimpleStatement conditional, ReturnStatement returnStatement) {
        Op03SimpleStatement originalConditionalTarget = conditional.getTargets().get(1);
        IfStatement ifStatement = (IfStatement)conditional.getStatement();
        conditional.replaceStatement(new IfExitingStatement(ifStatement.getLoc(), ifStatement.getCondition(), returnStatement));
        conditional.removeTarget(originalConditionalTarget);
        originalConditionalTarget.removeSource(conditional);
    }

    private static void replaceAssignmentReturn(Op03SimpleStatement assignment, ReturnStatement returnStatement) {
        assignment.replaceStatement(returnStatement);
        Op03SimpleStatement tgt = assignment.getTargets().get(0);
        tgt.removeSource(assignment);
        assignment.removeTarget(tgt);
    }

    private static void replace(Op03SimpleStatement source, boolean isIf, ReturnStatement returnNothingStatement) {
        if (isIf) {
            RemoveDeterministicJumps.replaceConditionalReturn(source, returnNothingStatement);
        } else {
            RemoveDeterministicJumps.replaceAssignmentReturn(source, returnNothingStatement);
        }
    }

    private static boolean propagateLiteralReturn(Method method, Op03SimpleStatement original, Op03SimpleStatement orignext, LValue originalLValue, Expression originalRValue, Map<LValue, Literal> display) {
        Class<?> cls;
        Op03SimpleStatement current;
        block18: {
            current = orignext;
            Set seen = SetFactory.newSet();
            while (true) {
                Boolean bool;
                if (!seen.add(current)) {
                    return false;
                }
                cls = current.getStatement().getClass();
                List<Op03SimpleStatement> curTargets = current.getTargets();
                int nTargets = curTargets.size();
                if (cls == Nop.class) {
                    if (nTargets != 1) {
                        return false;
                    }
                    current = curTargets.get(0);
                    continue;
                }
                if (cls == ReturnNothingStatement.class || cls == ReturnValueStatement.class) break block18;
                if (cls == GotoStatement.class || cls == MonitorExitStatement.class) {
                    if (nTargets != 1) {
                        return false;
                    }
                    current = curTargets.get(0);
                    continue;
                }
                if (cls == AssignmentSimple.class) {
                    AssignmentSimple assignmentSimple = (AssignmentSimple)current.getStatement();
                    LValue lValue = assignmentSimple.getCreatedLValue();
                    if (!(lValue instanceof StackSSALabel) && !(lValue instanceof LocalVariable)) {
                        return false;
                    }
                    Literal literal = assignmentSimple.getRValue().getComputedLiteral(display);
                    if (literal == null) {
                        return false;
                    }
                    display.put(lValue, literal);
                    current = curTargets.get(0);
                    continue;
                }
                if (cls != IfStatement.class) break;
                IfStatement ifStatement = (IfStatement)current.getStatement();
                Literal literal = ifStatement.getCondition().getComputedLiteral(display);
                Boolean bl = bool = literal == null ? null : literal.getValue().getMaybeBoolValue();
                if (bool == null) {
                    return false;
                }
                current = curTargets.get(bool != false ? 1 : 0);
            }
            return false;
        }
        cls = current.getStatement().getClass();
        if (cls == ReturnNothingStatement.class) {
            if (!(originalRValue instanceof Literal)) {
                return false;
            }
            original.replaceStatement(new ReturnNothingStatement(BytecodeLoc.TODO));
            orignext.removeSource(original);
            original.removeTarget(orignext);
            return true;
        }
        if (cls == ReturnValueStatement.class) {
            ReturnValueStatement returnValueStatement = (ReturnValueStatement)current.getStatement();
            if (originalRValue instanceof Literal) {
                Literal e = returnValueStatement.getReturnValue().getComputedLiteral(display);
                if (e == null) {
                    return false;
                }
                original.replaceStatement(new ReturnValueStatement(BytecodeLoc.TODO, e, returnValueStatement.getFnReturnType()));
            } else {
                Expression ret = returnValueStatement.getReturnValue();
                if (!(ret instanceof LValueExpression)) {
                    return false;
                }
                LValue retLValue = ((LValueExpression)ret).getLValue();
                if (!retLValue.equals(originalLValue)) {
                    return false;
                }
                original.replaceStatement(new ReturnValueStatement(BytecodeLoc.TODO, originalRValue, returnValueStatement.getFnReturnType()));
            }
            orignext.removeSource(original);
            original.removeTarget(orignext);
            return true;
        }
        return false;
    }

    public static void propagateToReturn(Method method, List<Op03SimpleStatement> statements) {
        boolean success = false;
        List<Op03SimpleStatement> assignmentSimples = Functional.filter(statements, new TypeFilter<AssignmentSimple>(AssignmentSimple.class));
        Set<BlockIdentifier> affectedByFinally = FinallyRewriter.getBlocksAffectedByFinally(statements);
        for (Op03SimpleStatement stm : assignmentSimples) {
            if (SetUtil.hasIntersection(affectedByFinally, stm.getBlockIdentifiers())) continue;
            Statement inner = stm.getStatement();
            if (stm.getTargets().size() != 1) continue;
            AssignmentSimple assignmentSimple = (AssignmentSimple)inner;
            LValue lValue = assignmentSimple.getCreatedLValue();
            Expression rValue = assignmentSimple.getRValue();
            if (!(lValue instanceof StackSSALabel) && !(lValue instanceof LocalVariable)) continue;
            Map<LValue, Literal> display = MapFactory.newMap();
            if (rValue instanceof Literal) {
                display.put(lValue, (Literal)rValue);
            }
            Op03SimpleStatement next = stm.getTargets().get(0);
            success |= RemoveDeterministicJumps.propagateLiteralReturn(method, stm, next, lValue, rValue, display);
        }
        if (success) {
            Op03Rewriters.replaceReturningIfs(statements, true);
        }
    }
}

