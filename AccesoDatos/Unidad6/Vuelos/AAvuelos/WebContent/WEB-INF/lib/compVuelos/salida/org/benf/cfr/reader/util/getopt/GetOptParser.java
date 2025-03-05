/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util.getopt;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.CfrVersionInfo;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.BadParametersException;
import org.benf.cfr.reader.util.getopt.GetOptSinkFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

public class GetOptParser {
    private static final String argPrefix = "--";

    private static String getHelp(PermittedOptionProvider permittedOptionProvider) {
        StringBuilder sb = new StringBuilder();
        for (String string : permittedOptionProvider.getFlags()) {
            sb.append("   --").append(string).append("\n");
        }
        int max = 10;
        for (PermittedOptionProvider.ArgumentParam<?, ?> argumentParam : permittedOptionProvider.getArguments()) {
            int len = argumentParam.getName().length();
            max = len > max ? len : max;
        }
        max += 4;
        List<PermittedOptionProvider.ArgumentParam<?, ?>> list = permittedOptionProvider.getArguments();
        Collections.sort(list, new Comparator<PermittedOptionProvider.ArgumentParam<?, ?>>(){

            @Override
            public int compare(PermittedOptionProvider.ArgumentParam<?, ?> a1, PermittedOptionProvider.ArgumentParam<?, ?> a2) {
                if (a1.getName().equals("help")) {
                    return 1;
                }
                if (a2.getName().equals("help")) {
                    return -1;
                }
                return a1.getName().compareTo(a2.getName());
            }
        });
        for (PermittedOptionProvider.ArgumentParam<?, ?> param : list) {
            if (param.isHidden()) continue;
            String name = param.getName();
            int pad = max - name.length();
            sb.append("   --").append(param.getName());
            for (int x = 0; x < pad; ++x) {
                sb.append(' ');
            }
            sb.append(param.shortDescribe()).append("\n");
        }
        return sb.toString();
    }

    private static Map<String, OptData> buildOptTypeMap(PermittedOptionProvider optionProvider) {
        Map<String, OptData> optTypeMap = MapFactory.newMap();
        for (String string : optionProvider.getFlags()) {
            optTypeMap.put(string, new OptData(string));
        }
        for (PermittedOptionProvider.ArgumentParam argumentParam : optionProvider.getArguments()) {
            optTypeMap.put(argumentParam.getName(), new OptData(argumentParam));
        }
        return optTypeMap;
    }

    public <T> Pair<List<String>, T> parse(String[] args, GetOptSinkFactory<T> getOptSinkFactory) {
        Pair<List<String>, Map<String, String>> processed = this.process(args, getOptSinkFactory);
        List<String> positional = processed.getFirst();
        Map<String, String> named = processed.getSecond();
        if (positional.isEmpty() && (named.containsKey(OptionsImpl.HELP.getName()) || named.containsKey(OptionsImpl.VERSION.getName()))) {
            positional.add("ignoreMe.class");
        }
        T res = getOptSinkFactory.create(named);
        return Pair.make(positional, res);
    }

    private static void printErrHeader() {
        System.err.println("CFR " + CfrVersionInfo.VERSION_INFO + "\n");
    }

    private static void printUsage() {
        System.err.println("java -jar CFRJAR.jar class_or_jar_file [method] [options]\n");
    }

    private static void printHelpHint(boolean full) {
        System.err.println("Please specify " + (full ? "'--help' to get option list, or " : "") + "'--help optionname' for specifics, e.g.\n   --help " + OptionsImpl.PULL_CODE_CASE.getName());
    }

    public void showVersion() {
        GetOptParser.printErrHeader();
    }

    public void showHelp(Exception e) {
        GetOptParser.printErrHeader();
        GetOptParser.printUsage();
        System.err.println("Parameter error : " + e.getMessage() + "\n");
        GetOptParser.printHelpHint(true);
    }

    public void showOptionHelp(PermittedOptionProvider permittedOptionProvider, Options options, PermittedOptionProvider.ArgumentParam<String, Void> helpArg) {
        GetOptParser.printErrHeader();
        String relevantOption = options.getOption(helpArg);
        List<PermittedOptionProvider.ArgumentParam<?, ?>> possible = permittedOptionProvider.getArguments();
        for (PermittedOptionProvider.ArgumentParam<?, ?> opt : possible) {
            if (!opt.getName().equals(relevantOption)) continue;
            System.err.println(opt.describe());
            return;
        }
        System.err.println(GetOptParser.getHelp(permittedOptionProvider));
        if (relevantOption.equals("")) {
            GetOptParser.printHelpHint(false);
        } else {
            System.err.println("No such argument '" + relevantOption + "'");
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private Pair<List<String>, Map<String, String>> process(String[] in, PermittedOptionProvider optionProvider) {
        Map<String, OptData> optTypeMap = GetOptParser.buildOptTypeMap(optionProvider);
        Map<String, String> res = MapFactory.newMap();
        List positional = ListFactory.newList();
        OptionsImpl optionsSample = new OptionsImpl(res);
        int x = 0;
        while (true) {
            block8: {
                block6: {
                    String value;
                    OptData optData;
                    String name;
                    block11: {
                        block9: {
                            block10: {
                                String next;
                                block7: {
                                    if (x >= in.length) {
                                        return Pair.make(positional, res);
                                    }
                                    if (!in[x].startsWith(argPrefix)) break block6;
                                    name = in[x].substring(2);
                                    optData = optTypeMap.get(name);
                                    if (optData == null) {
                                        throw new IllegalArgumentException("Unknown argument " + name);
                                    }
                                    if (!optData.isFlag()) break block7;
                                    res.put(name, null);
                                    break block8;
                                }
                                String string = next = x >= in.length - 1 ? argPrefix : in[x + 1];
                                if (!next.startsWith(argPrefix)) break block9;
                                if (!name.equals(OptionsImpl.HELP.getName()) && !name.equals(OptionsImpl.VERSION.getName())) break block10;
                                value = "";
                                break block11;
                            }
                            value = optData.getArgument().getFn().getDefaultValue();
                            if (value == null) {
                                throw new BadParametersException("Requires argument", optData.getArgument());
                            }
                            break block8;
                        }
                        value = in[++x];
                    }
                    res.put(name, value);
                    try {
                        optData.getArgument().getFn().invoke(res.get(name), null, optionsSample);
                    }
                    catch (Exception e) {
                        throw new BadParametersException(e.toString(), optData.getArgument());
                    }
                }
                positional.add(in[x]);
            }
            ++x;
        }
    }

    private static class OptData {
        private final boolean isFlag;
        private final String name;
        private final PermittedOptionProvider.ArgumentParam<?, ?> argument;

        private OptData(String name) {
            this.name = name;
            this.isFlag = true;
            this.argument = null;
        }

        private OptData(PermittedOptionProvider.ArgumentParam<?, ?> argument) {
            this.argument = argument;
            this.isFlag = false;
            this.name = argument.getName();
        }

        boolean isFlag() {
            return this.isFlag;
        }

        public String getName() {
            return this.name;
        }

        public PermittedOptionProvider.ArgumentParam<?, ?> getArgument() {
            return this.argument;
        }
    }
}

