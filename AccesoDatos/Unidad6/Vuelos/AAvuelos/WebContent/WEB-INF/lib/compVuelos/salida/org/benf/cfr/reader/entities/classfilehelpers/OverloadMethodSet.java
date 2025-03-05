/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.classfilehelpers;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.LazyMap;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;

public class OverloadMethodSet {
    private final ClassFile classFile;
    private final MethodData actualPrototype;
    private final List<MethodData> allPrototypes;

    public OverloadMethodSet(ClassFile classFile, MethodPrototype actualPrototype, List<MethodPrototype> allPrototypes) {
        this.classFile = classFile;
        UnaryFunction<MethodPrototype, MethodData> mk = new UnaryFunction<MethodPrototype, MethodData>(){

            @Override
            public MethodData invoke(MethodPrototype arg) {
                return new MethodData(arg, arg.getArgs());
            }
        };
        this.actualPrototype = (MethodData)mk.invoke(actualPrototype);
        this.allPrototypes = Functional.map(allPrototypes, mk);
    }

    private OverloadMethodSet(ClassFile classFile, MethodData actualPrototype, List<MethodData> allPrototypes) {
        this.classFile = classFile;
        this.actualPrototype = actualPrototype;
        this.allPrototypes = allPrototypes;
    }

    public OverloadMethodSet specialiseTo(JavaGenericRefTypeInstance type) {
        final GenericTypeBinder genericTypeBinder = this.classFile.getGenericTypeBinder(type);
        if (genericTypeBinder == null) {
            return null;
        }
        UnaryFunction<MethodData, MethodData> mk = new UnaryFunction<MethodData, MethodData>(){

            @Override
            public MethodData invoke(MethodData arg) {
                return arg.getBoundVersion(genericTypeBinder);
            }
        };
        return new OverloadMethodSet(this.classFile, (MethodData)mk.invoke(this.actualPrototype), Functional.map(this.allPrototypes, mk));
    }

    public JavaTypeInstance getArgType(int idx, JavaTypeInstance used) {
        return this.actualPrototype.getArgType(idx, used);
    }

    public List<JavaTypeInstance> getPossibleArgTypes(int idx, JavaTypeInstance used) {
        List<JavaTypeInstance> res = ListFactory.newList();
        for (MethodData proto : this.allPrototypes) {
            res.add(proto.getArgType(idx, used));
        }
        return res;
    }

    public boolean callsCorrectEntireMethod(List<Expression> args, GenericTypeBinder gtb) {
        final int argCount = args.size();
        Set<MethodData> possibleMatches = SetFactory.newOrderedSet(Functional.filter(this.allPrototypes, new Predicate<MethodData>(){

            @Override
            public boolean test(MethodData in) {
                return in.methodArgs.size() <= argCount;
            }
        }));
        LazyMap<Integer, Set<MethodData>> weakMatches = MapFactory.newLazyMap(new UnaryFunction<Integer, Set<MethodData>>(){

            @Override
            public Set<MethodData> invoke(Integer arg) {
                return SetFactory.newSet();
            }
        });
        Iterator<MethodData> possiter = possibleMatches.iterator();
        MethodData perfectMatch = null;
        block0: while (possiter.hasNext()) {
            boolean perfect = true;
            MethodData prototype = possiter.next();
            int len = args.size();
            for (int x = 0; x < len; ++x) {
                Expression arg = args.get(x);
                boolean isNull = Literal.NULL.equals(arg);
                JavaTypeInstance origActual = arg.getInferredJavaType().getJavaTypeInstance();
                JavaTypeInstance actual = origActual.getDeGenerifiedType();
                JavaTypeInstance origArgType = prototype.getArgType(x, actual);
                JavaTypeInstance argType = origArgType;
                if (argType != null) {
                    JavaTypeInstance bound;
                    JavaTypeInstance unboxedExactArg;
                    argType = argType.getDeGenerifiedType();
                    if (isNull) {
                        perfect = false;
                        if (argType.isObject()) {
                            if (!TypeConstants.OBJECT.equals(argType)) continue;
                            ((Set)weakMatches.get(x)).add(prototype);
                            continue;
                        }
                        possiter.remove();
                        continue block0;
                    }
                    JavaTypeInstance unboxedExactActual = this.unbox(actual);
                    if (unboxedExactActual.equals(unboxedExactArg = this.unbox(argType))) {
                        if (!perfect || x != len - 1) continue;
                        perfectMatch = perfectMatch == null ? prototype : MethodData.POISON;
                        continue;
                    }
                    if (actual.implicitlyCastsTo(argType, gtb) && actual.impreciseCanCastTo(argType, gtb)) {
                        perfect = false;
                        continue;
                    }
                    if (gtb != null && (bound = gtb.getBindingFor(origArgType)).equals(actual)) {
                        perfect = false;
                        continue;
                    }
                }
                possiter.remove();
                continue block0;
            }
        }
        if (possibleMatches.isEmpty()) {
            return false;
        }
        if (perfectMatch != null && perfectMatch != MethodData.POISON) {
            return perfectMatch.methodPrototype.equals(this.actualPrototype.methodPrototype);
        }
        if (possibleMatches.size() > 1 && !weakMatches.isEmpty()) {
            int len = args.size();
            for (int x = 0; x < len; ++x) {
                Set weakMatchedMethods;
                List<MethodData> remaining;
                if (!weakMatches.containsKey(x) || (remaining = SetUtil.differenceAtakeBtoList(possibleMatches, weakMatchedMethods = (Set)weakMatches.get(x))).size() != 1) continue;
                possibleMatches.clear();
                possibleMatches.addAll(remaining);
                break;
            }
        }
        if (possibleMatches.size() > 1) {
            List<MethodData> remaining = ListFactory.newList(possibleMatches);
            int len = args.size();
            for (int x = 0; x < len; ++x) {
                boolean unkNull = false;
                JavaTypeInstance argTypeUsed = args.get(x).getInferredJavaType().getJavaTypeInstance();
                if (argTypeUsed == RawJavaType.NULL) {
                    unkNull = true;
                    argTypeUsed = TypeConstants.OBJECT;
                }
                JavaTypeInstance mostDefined = null;
                int best = -1;
                int len2 = remaining.size();
                for (int y = 0; y < len2; ++y) {
                    boolean bina;
                    BindingSuperContainer t2bs;
                    JavaTypeInstance t = remaining.get(y).getArgType(x, argTypeUsed);
                    BindingSuperContainer bindingSuperContainer = t2bs = t == null ? null : t.getBindingSupers();
                    if (t2bs == null) {
                        best = -1;
                        break;
                    }
                    if (mostDefined == null) {
                        mostDefined = t;
                        best = 0;
                        continue;
                    }
                    boolean ainb = t2bs.containsBase(mostDefined);
                    if (ainb ^ (bina = mostDefined.getBindingSupers().containsBase(t))) {
                        if (ainb) {
                            mostDefined = t;
                            best = y;
                            continue;
                        }
                        if (!unkNull) continue;
                        best = -1;
                        break;
                    }
                    best = -1;
                    break;
                }
                if (best == -1) continue;
                MethodData match = remaining.get(best);
                possibleMatches.clear();
                possibleMatches.add(match);
                break;
            }
        }
        if (possibleMatches.size() == 1) {
            MethodData methodData = possibleMatches.iterator().next();
            return methodData.methodPrototype.equals(this.actualPrototype.methodPrototype);
        }
        return false;
    }

    public int size() {
        return this.allPrototypes.size();
    }

    private JavaTypeInstance unbox(JavaTypeInstance actual) {
        RawJavaType unboxed = RawJavaType.getUnboxedTypeFor(actual);
        return unboxed == null ? actual : unboxed;
    }

    public boolean callsCorrectMethod(Expression newArg, int idx, GenericTypeBinder gtb) {
        JavaTypeInstance newArgType = newArg.getInferredJavaType().getJavaTypeInstance();
        Set exactMatches = SetFactory.newSet();
        for (MethodData prototype : this.allPrototypes) {
            JavaTypeInstance type = prototype.getArgType(idx, newArgType);
            if (type == null || !type.equals(newArgType)) continue;
            exactMatches.add(prototype.methodPrototype);
        }
        if (exactMatches.contains(this.actualPrototype.methodPrototype)) {
            return true;
        }
        JavaTypeInstance expectedArgType = this.actualPrototype.getArgType(idx, newArgType);
        if (expectedArgType instanceof RawJavaType) {
            return this.callsCorrectApproxRawMethod(newArgType, idx, gtb);
        }
        return this.callsCorrectApproxObjMethod(newArg, newArgType, idx, gtb);
    }

    private boolean callsCorrectApproxRawMethod(JavaTypeInstance actual, int idx, GenericTypeBinder gtb) {
        List matches = ListFactory.newList();
        for (MethodData prototype : this.allPrototypes) {
            JavaTypeInstance arg = prototype.getArgType(idx, actual);
            if (!actual.implicitlyCastsTo(arg, null) || !actual.impreciseCanCastTo(arg, gtb)) continue;
            matches.add(prototype);
        }
        if (matches.isEmpty()) {
            return false;
        }
        if (matches.size() == 1 && ((MethodData)matches.get(0)).is(this.actualPrototype)) {
            return true;
        }
        MethodData lowest = (MethodData)matches.get(0);
        JavaTypeInstance lowestType = lowest.getArgType(idx, actual);
        for (int x = 1; x < matches.size(); ++x) {
            MethodData next = (MethodData)matches.get(x);
            JavaTypeInstance nextType = next.getArgType(idx, actual);
            if (nextType == null || !nextType.implicitlyCastsTo(lowestType, null)) continue;
            lowest = next;
            lowestType = nextType;
        }
        return lowest.is(this.actualPrototype);
    }

    /*
     * WARNING - void declaration
     */
    private boolean callsCorrectApproxObjMethod(Expression newArg, final JavaTypeInstance actual, final int idx, GenericTypeBinder gtb) {
        boolean isPOD;
        boolean onlyMatchPod;
        List<Object> matches = ListFactory.newList();
        boolean podMatchExists = false;
        boolean nonPodMatchExists = false;
        for (MethodData prototype : this.allPrototypes) {
            JavaTypeInstance arg = prototype.getArgType(idx, actual);
            if (arg == null || !actual.implicitlyCastsTo(arg, null) || !actual.impreciseCanCastTo(arg, gtb)) continue;
            if (arg instanceof RawJavaType) {
                podMatchExists = true;
            } else {
                nonPodMatchExists = true;
            }
            matches.add(prototype);
        }
        if (matches.isEmpty()) {
            return false;
        }
        if (matches.size() == 1 && ((MethodData)matches.get(0)).is(this.actualPrototype)) {
            return true;
        }
        Literal nullLit = new Literal(TypedLiteral.getNull());
        if (newArg.equals(nullLit) && actual == RawJavaType.NULL) {
            MethodData best = null;
            JavaTypeInstance bestType = null;
            for (MethodData methodData : matches) {
                JavaTypeInstance arg = methodData.getArgType(idx, actual);
                if (arg == null || arg.equals(TypeConstants.OBJECT)) continue;
                if (best == null) {
                    best = methodData;
                    bestType = arg;
                    continue;
                }
                if (arg.implicitlyCastsTo(bestType, null)) {
                    best = methodData;
                    bestType = arg;
                    continue;
                }
                if (bestType.implicitlyCastsTo(arg, null)) continue;
                return false;
            }
            if (best != null) {
                return best.is(this.actualPrototype);
            }
        }
        boolean bl = onlyMatchPod = (isPOD = actual instanceof RawJavaType) && podMatchExists;
        if (onlyMatchPod) {
            matches = Functional.filter(matches, new Predicate<MethodData>(){

                @Override
                public boolean test(MethodData in) {
                    return in.getArgType(idx, actual) instanceof RawJavaType;
                }
            });
        }
        if (!isPOD) {
            Pair<List<MethodData>, List<MethodData>> partition = Functional.partition(matches, new Predicate<MethodData>(){

                @Override
                public boolean test(MethodData in) {
                    return !(in.getArgType(idx, actual) instanceof RawJavaType);
                }
            });
            matches.clear();
            matches.addAll((Collection<Object>)partition.getFirst());
            if (!nonPodMatchExists) {
                matches.addAll((Collection<Object>)partition.getSecond());
            }
        }
        if (matches.isEmpty()) {
            return false;
        }
        MethodData lowest = (MethodData)matches.get(0);
        JavaTypeInstance javaTypeInstance = lowest.getArgType(idx, actual);
        for (int x = 0; x < matches.size(); ++x) {
            void var12_16;
            MethodData next = (MethodData)matches.get(x);
            JavaTypeInstance nextType = next.getArgType(idx, actual);
            if (nextType == null || !nextType.implicitlyCastsTo((JavaTypeInstance)var12_16, null)) continue;
            lowest = next;
            JavaTypeInstance javaTypeInstance2 = nextType;
        }
        return lowest.is(this.actualPrototype);
    }

    private static class MethodData {
        private final MethodPrototype methodPrototype;
        private final List<JavaTypeInstance> methodArgs;
        private final int size;
        private static MethodData POISON = new MethodData();

        private MethodData() {
            this.methodPrototype = null;
            this.methodArgs = null;
            this.size = 0;
        }

        private MethodData(MethodPrototype methodPrototype, List<JavaTypeInstance> methodArgs) {
            this.methodPrototype = methodPrototype;
            this.methodArgs = methodArgs;
            this.size = methodArgs.size();
        }

        private JavaTypeInstance getArgType(int idx, JavaTypeInstance used) {
            if (idx >= this.size - 1 && this.methodPrototype.isVarArgs()) {
                JavaTypeInstance res = this.methodArgs.get(this.size - 1);
                if (res.getNumArrayDimensions() == used.getNumArrayDimensions() + 1) {
                    return res.removeAnArrayIndirection();
                }
                return res;
            }
            if (idx >= this.size) {
                return null;
            }
            return this.methodArgs.get(idx);
        }

        public boolean isVararg(int idx) {
            return idx >= this.size - 1 && this.methodPrototype.isVarArgs();
        }

        public boolean is(MethodData other) {
            return this.methodPrototype == other.methodPrototype;
        }

        public String toString() {
            return this.methodPrototype.toString();
        }

        private MethodData getBoundVersion(final GenericTypeBinder genericTypeBinder) {
            List<JavaTypeInstance> rebound = Functional.map(this.methodArgs, new UnaryFunction<JavaTypeInstance, JavaTypeInstance>(){

                @Override
                public JavaTypeInstance invoke(JavaTypeInstance arg) {
                    if (arg instanceof JavaGenericBaseInstance) {
                        return ((JavaGenericBaseInstance)arg).getBoundInstance(genericTypeBinder);
                    }
                    return arg;
                }
            });
            return new MethodData(this.methodPrototype, rebound);
        }
    }
}

