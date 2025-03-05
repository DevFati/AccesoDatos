/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.entities.constantpool;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.stack.StackDelta;
import org.benf.cfr.reader.bytecode.analysis.stack.StackDeltaImpl;
import org.benf.cfr.reader.bytecode.analysis.types.ClassSignature;
import org.benf.cfr.reader.bytecode.analysis.types.FormalTypeParameter;
import org.benf.cfr.reader.bytecode.analysis.types.JavaArrayTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaWildcardTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.StackType;
import org.benf.cfr.reader.bytecode.analysis.types.StackTypes;
import org.benf.cfr.reader.bytecode.analysis.types.WildcardType;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableNamer;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolEntryUTF8;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.MalformedPrototypeException;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;

public class ConstantPoolUtils {
    private static JavaTypeInstance parseRefType(String tok, ConstantPool cp, boolean isTemplate) {
        int idxGen = tok.indexOf(60);
        int idxStart = 0;
        if (idxGen != -1) {
            List<JavaTypeInstance> genericTypes;
            StringBuilder already;
            block3: {
                tok = tok.replace(">.", ">$");
                already = new StringBuilder();
                do {
                    String pre = tok.substring(idxStart, idxGen);
                    already.append(pre);
                    String gen = tok.substring(idxGen + 1, tok.length() - 1);
                    Pair<List<JavaTypeInstance>, Integer> genericTypePair = ConstantPoolUtils.parseTypeList(gen, cp);
                    genericTypes = genericTypePair.getFirst();
                    idxStart = idxGen + genericTypePair.getSecond() + 1;
                    if (idxStart >= idxGen + gen.length()) break block3;
                    if (tok.charAt(idxStart) == '>') continue;
                    throw new IllegalStateException();
                } while ((idxGen = tok.indexOf(60, ++idxStart)) != -1);
                already.append(tok.substring(idxStart));
                return cp.getClassCache().getRefClassFor(already.toString());
            }
            JavaRefTypeInstance clazzType = cp.getClassCache().getRefClassFor(already.toString());
            return new JavaGenericRefTypeInstance(clazzType, genericTypes);
        }
        if (isTemplate) {
            return new JavaGenericPlaceholderTypeInstance(tok, cp);
        }
        return cp.getClassCache().getRefClassFor(tok);
    }

    public static JavaTypeInstance decodeTypeTok(String tok, ConstantPool cp) {
        JavaTypeInstance javaTypeInstance;
        int idx = 0;
        int numArrayDims = 0;
        char c = tok.charAt(idx);
        WildcardType wildcardType = WildcardType.NONE;
        if (c == '-' || c == '+') {
            wildcardType = c == '+' ? WildcardType.EXTENDS : WildcardType.SUPER;
            c = tok.charAt(++idx);
        }
        while (c == '[') {
            ++numArrayDims;
            c = tok.charAt(++idx);
        }
        switch (c) {
            case '*': {
                javaTypeInstance = new JavaGenericPlaceholderTypeInstance("?", cp);
                break;
            }
            case 'L': {
                javaTypeInstance = ConstantPoolUtils.parseRefType(tok.substring(idx + 1, tok.length() - 1), cp, false);
                break;
            }
            case 'T': {
                javaTypeInstance = ConstantPoolUtils.parseRefType(tok.substring(idx + 1, tok.length() - 1), cp, true);
                break;
            }
            case 'B': 
            case 'C': 
            case 'D': 
            case 'F': 
            case 'I': 
            case 'J': 
            case 'S': 
            case 'Z': {
                javaTypeInstance = ConstantPoolUtils.decodeRawJavaType(c);
                break;
            }
            default: {
                throw new ConfusedCFRException("Invalid type string " + tok);
            }
        }
        if (numArrayDims > 0) {
            javaTypeInstance = new JavaArrayTypeInstance(numArrayDims, javaTypeInstance);
        }
        if (wildcardType != WildcardType.NONE) {
            javaTypeInstance = new JavaWildcardTypeInstance(wildcardType, javaTypeInstance);
        }
        return javaTypeInstance;
    }

    public static RawJavaType decodeRawJavaType(char c) {
        RawJavaType javaTypeInstance;
        switch (c) {
            case 'B': {
                javaTypeInstance = RawJavaType.BYTE;
                break;
            }
            case 'C': {
                javaTypeInstance = RawJavaType.CHAR;
                break;
            }
            case 'I': {
                javaTypeInstance = RawJavaType.INT;
                break;
            }
            case 'S': {
                javaTypeInstance = RawJavaType.SHORT;
                break;
            }
            case 'Z': {
                javaTypeInstance = RawJavaType.BOOLEAN;
                break;
            }
            case 'F': {
                javaTypeInstance = RawJavaType.FLOAT;
                break;
            }
            case 'D': {
                javaTypeInstance = RawJavaType.DOUBLE;
                break;
            }
            case 'J': {
                javaTypeInstance = RawJavaType.LONG;
                break;
            }
            default: {
                throw new ConfusedCFRException("Illegal raw java type");
            }
        }
        return javaTypeInstance;
    }

    private static String getNextTypeTok(String proto, int curridx) {
        int startidx = curridx;
        char c = proto.charAt(curridx);
        if (c == '-' || c == '+') {
            c = proto.charAt(++curridx);
        }
        while (c == '[') {
            c = proto.charAt(++curridx);
        }
        switch (c) {
            case '*': {
                ++curridx;
                break;
            }
            case 'L': 
            case 'T': {
                int openBra = 0;
                do {
                    c = proto.charAt(++curridx);
                    switch (c) {
                        case '<': {
                            ++openBra;
                            break;
                        }
                        case '>': {
                            --openBra;
                        }
                    }
                } while (openBra > 0 || c != ';');
                ++curridx;
                break;
            }
            case 'B': 
            case 'C': 
            case 'D': 
            case 'F': 
            case 'I': 
            case 'J': 
            case 'S': 
            case 'Z': {
                ++curridx;
                break;
            }
            default: {
                throw new ConfusedCFRException("Can't parse proto : " + proto + " starting " + proto.substring(startidx));
            }
        }
        return proto.substring(startidx, curridx);
    }

    private static String getNextFormalTypeTok(String proto, int curridx) {
        int startidx = curridx;
        while (proto.charAt(curridx) != ':') {
            ++curridx;
        }
        if (proto.charAt(++curridx) != ':') {
            String classBound = ConstantPoolUtils.getNextTypeTok(proto, curridx);
            curridx += classBound.length();
        }
        if (proto.charAt(curridx) == ':') {
            String interfaceBound = ConstantPoolUtils.getNextTypeTok(proto, ++curridx);
            curridx += interfaceBound.length();
        }
        return proto.substring(startidx, curridx);
    }

    private static FormalTypeParameter decodeFormalTypeTok(String tok, ConstantPool cp) {
        int idx = 0;
        while (tok.charAt(idx) != ':') {
            ++idx;
        }
        String name = tok.substring(0, idx);
        JavaTypeInstance classBound = null;
        if (tok.charAt(++idx) != ':') {
            String classBoundTok = ConstantPoolUtils.getNextTypeTok(tok, idx);
            classBound = ConstantPoolUtils.decodeTypeTok(classBoundTok, cp);
            idx += classBoundTok.length();
        }
        JavaTypeInstance interfaceBound = null;
        if (idx < tok.length() && tok.charAt(idx) == ':') {
            String interfaceBoundTok = ConstantPoolUtils.getNextTypeTok(tok, ++idx);
            interfaceBound = ConstantPoolUtils.decodeTypeTok(interfaceBoundTok, cp);
        }
        return new FormalTypeParameter(name, classBound, interfaceBound);
    }

    public static ClassSignature parseClassSignature(ConstantPoolEntryUTF8 signature, ConstantPool cp) {
        String sig = signature.getValue();
        int curridx = 0;
        Pair<Integer, List<FormalTypeParameter>> formalTypeParametersRes = ConstantPoolUtils.parseFormalTypeParameters(sig, cp, curridx);
        curridx = formalTypeParametersRes.getFirst();
        List<FormalTypeParameter> formalTypeParameters = formalTypeParametersRes.getSecond();
        String superClassSignatureTok = ConstantPoolUtils.getNextTypeTok(sig, curridx);
        curridx += superClassSignatureTok.length();
        JavaTypeInstance superClassSignature = ConstantPoolUtils.decodeTypeTok(superClassSignatureTok, cp);
        List<JavaTypeInstance> interfaceClassSignatures = ListFactory.newList();
        while (curridx < sig.length()) {
            String interfaceSignatureTok = ConstantPoolUtils.getNextTypeTok(sig, curridx);
            curridx += interfaceSignatureTok.length();
            interfaceClassSignatures.add(ConstantPoolUtils.decodeTypeTok(interfaceSignatureTok, cp));
        }
        return new ClassSignature(formalTypeParameters, superClassSignature, interfaceClassSignatures);
    }

    private static Pair<Integer, List<FormalTypeParameter>> parseFormalTypeParameters(String proto, ConstantPool cp, int curridx) {
        List formalTypeParameters = null;
        FormalTypeParameter last = null;
        if (proto.charAt(curridx) == '<') {
            formalTypeParameters = ListFactory.newList();
            ++curridx;
            while (proto.charAt(curridx) != '>') {
                String formalTypeTok = ConstantPoolUtils.getNextFormalTypeTok(proto, curridx);
                FormalTypeParameter typeTok = ConstantPoolUtils.decodeFormalTypeTok(formalTypeTok, cp);
                if (typeTok.getName().equals("")) {
                    if (last != null) {
                        last.add(typeTok);
                    }
                } else {
                    formalTypeParameters.add(typeTok);
                    last = typeTok;
                }
                curridx += formalTypeTok.length();
            }
            ++curridx;
        }
        return Pair.make(curridx, formalTypeParameters);
    }

    public static MethodPrototype parseJavaMethodPrototype(DCCommonState state, ClassFile classFile, JavaTypeInstance classType, String name, boolean instanceMethod, Method.MethodConstructor constructorFlag, ConstantPoolEntryUTF8 prototype, ConstantPool cp, boolean varargs, boolean synthetic, VariableNamer variableNamer, String originalDescriptor) {
        String proto = prototype.getValue();
        try {
            Map<Object, Object> ftpMap;
            int curridx = 0;
            Pair<Integer, List<FormalTypeParameter>> formalTypeParametersRes = ConstantPoolUtils.parseFormalTypeParameters(proto, cp, curridx);
            curridx = formalTypeParametersRes.getFirst();
            List<FormalTypeParameter> formalTypeParameters = formalTypeParametersRes.getSecond();
            if (formalTypeParameters == null) {
                ftpMap = Collections.emptyMap();
            } else {
                ftpMap = MapFactory.newMap();
                for (FormalTypeParameter ftp : formalTypeParameters) {
                    ftpMap.put(ftp.getName(), ftp.getBound());
                }
            }
            if (proto.charAt(curridx) != '(') {
                throw new ConfusedCFRException("Prototype " + proto + " is invalid");
            }
            ++curridx;
            List<JavaTypeInstance> args = ListFactory.newList();
            while (proto.charAt(curridx) != ')') {
                curridx = ConstantPoolUtils.processTypeEntry(cp, proto, curridx, ftpMap, args);
            }
            JavaTypeInstance resultType = RawJavaType.VOID;
            if (proto.charAt(++curridx) == 'V') {
                ++curridx;
            } else {
                String resTypeTok = ConstantPoolUtils.getNextTypeTok(proto, curridx);
                curridx += resTypeTok.length();
                resultType = ConstantPoolUtils.decodeTypeTok(resTypeTok, cp);
            }
            List<JavaTypeInstance> exceptions = Collections.emptyList();
            if (curridx < proto.length()) {
                exceptions = ListFactory.newList();
                while (curridx < proto.length() && proto.charAt(curridx) == '^') {
                    ++curridx;
                    curridx = ConstantPoolUtils.processTypeEntry(cp, proto, curridx, ftpMap, exceptions);
                }
            }
            return new MethodPrototype(state, classFile, classType, name, instanceMethod, constructorFlag, formalTypeParameters, args, resultType, exceptions, varargs, variableNamer, synthetic, originalDescriptor);
        }
        catch (StringIndexOutOfBoundsException e) {
            throw new MalformedPrototypeException(proto, e);
        }
    }

    private static int processTypeEntry(ConstantPool cp, String proto, int curridx, Map<String, JavaTypeInstance> ftpMap, List<JavaTypeInstance> args) {
        String typeTok = ConstantPoolUtils.getNextTypeTok(proto, curridx);
        JavaTypeInstance type = ConstantPoolUtils.decodeTypeTok(typeTok, cp);
        if (type instanceof JavaGenericPlaceholderTypeInstance) {
            type = ((JavaGenericPlaceholderTypeInstance)type).withBound(ftpMap.get(type.getRawName()));
        }
        args.add(type);
        return curridx += typeTok.length();
    }

    private static Pair<List<JavaTypeInstance>, Integer> parseTypeList(String proto, ConstantPool cp) {
        int curridx;
        String typeTok;
        int len = proto.length();
        List res = ListFactory.newList();
        for (curridx = 0; curridx < len && proto.charAt(curridx) != '>'; curridx += typeTok.length()) {
            typeTok = ConstantPoolUtils.getNextTypeTok(proto, curridx);
            res.add(ConstantPoolUtils.decodeTypeTok(typeTok, cp));
        }
        return Pair.make(res, curridx);
    }

    static StackDelta parseMethodPrototype(boolean member, ConstantPoolEntryUTF8 prototype, ConstantPool cp) {
        String proto = prototype.getValue();
        int curridx = 1;
        if (!proto.startsWith("(")) {
            throw new ConfusedCFRException("Prototype " + proto + " is invalid");
        }
        StackTypes argumentTypes = new StackTypes(new StackType[0]);
        if (member) {
            argumentTypes.add(StackType.REF);
        }
        while (proto.charAt(curridx) != ')') {
            String typeTok = ConstantPoolUtils.getNextTypeTok(proto, curridx);
            argumentTypes.add(ConstantPoolUtils.decodeTypeTok(typeTok, cp).getStackType());
            curridx += typeTok.length();
        }
        StackTypes resultType = StackTypes.EMPTY;
        switch (proto.charAt(++curridx)) {
            case 'V': {
                break;
            }
            default: {
                resultType = ConstantPoolUtils.decodeTypeTok(ConstantPoolUtils.getNextTypeTok(proto, curridx), cp).getStackType().asList();
            }
        }
        StackDeltaImpl res = new StackDeltaImpl(argumentTypes, resultType);
        return res;
    }
}

