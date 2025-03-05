/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.util.Arrays;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.StringUtils;

public interface ClassFileRelocator {
    public String correctPath(String var1);

    public static class Configurator {
        ClassFileRelocator getCommonRoot(String filePath, String classPath) {
            String classRemovePrefix;
            int diffpt;
            String npath = filePath.replace('\\', '/');
            String[] fileParts = npath.split("/");
            String[] classParts = classPath.split("/");
            int fpLen = fileParts.length;
            int cpLen = classParts.length;
            int min = Math.min(fileParts.length, classParts.length);
            for (diffpt = 1; diffpt < min && fileParts[fpLen - diffpt - 1].equals(classParts[cpLen - diffpt - 1]); ++diffpt) {
            }
            String fname = fileParts[fpLen - 1];
            String cname = classParts[cpLen - 1];
            fileParts = Arrays.copyOfRange(fileParts, 0, fpLen - diffpt);
            classParts = Arrays.copyOfRange(classParts, 0, cpLen - diffpt);
            String pathPrefix = fileParts.length == 0 ? "" : StringUtils.join(fileParts, "/") + "/";
            String string = classRemovePrefix = classParts.length == 0 ? "" : StringUtils.join(classParts, "/") + "/";
            if (cname.equals(fname) || !cname.endsWith(".class")) {
                return new PrefixRelocator(pathPrefix, classRemovePrefix);
            }
            return new RenamingRelocator(new PrefixRelocator(pathPrefix, classRemovePrefix), classPath, filePath, cname, fname);
        }

        ClassFileRelocator configureWith(String usePath, String specPath) {
            if (usePath == null || specPath == null || specPath.equals(usePath)) {
                return NopRelocator.Instance;
            }
            if (usePath.endsWith(specPath)) {
                String pathPrefix = usePath.substring(0, usePath.length() - specPath.length());
                return new PrefixRelocator(pathPrefix, null);
            }
            return this.getCommonRoot(usePath, specPath);
        }
    }

    public static class RenamingRelocator
    implements ClassFileRelocator {
        private final String filePath;
        private final ClassFileRelocator base;
        private final FileDets classFile;
        private final FileDets file;

        public RenamingRelocator(ClassFileRelocator base, String classPath, String filePath, String className, String fileName) {
            this.base = base;
            this.filePath = filePath;
            this.classFile = this.getDets(classPath, className);
            this.file = this.getDets(filePath, fileName);
        }

        private FileDets getDets(String path, String name) {
            Pair<String, String> parts = RenamingRelocator.stripExt(name);
            return new FileDets(path.substring(0, path.length() - name.length()), parts.getFirst(), parts.getSecond());
        }

        static Pair<String, String> stripExt(String name) {
            int idx = name.lastIndexOf(46);
            if (idx <= 0) {
                return Pair.make(name, "");
            }
            return Pair.make(name.substring(0, idx), name.substring(idx));
        }

        @Override
        public String correctPath(String path) {
            if (path.startsWith(this.classFile.pre) && path.endsWith(this.classFile.ext)) {
                String p = path.substring(this.classFile.pre.length(), path.length() - this.classFile.ext.length());
                if (p.equals(this.classFile.name)) {
                    return this.filePath;
                }
                if (p.startsWith(this.classFile.name) && p.charAt(this.classFile.name.length()) == '$') {
                    return this.file.pre + this.file.name + p.substring(this.classFile.name.length()) + this.file.ext;
                }
            }
            return this.base.correctPath(path);
        }

        private static class FileDets {
            String pre;
            String name;
            String ext;

            public FileDets(String pre, String name, String ext) {
                this.pre = pre;
                this.name = name;
                this.ext = ext;
            }
        }
    }

    public static class PrefixRelocator
    implements ClassFileRelocator {
        private final String pathPrefix;
        private final String classRemovePrefix;

        public PrefixRelocator(String pathPrefix, String classRemovePrefix) {
            this.pathPrefix = pathPrefix;
            this.classRemovePrefix = classRemovePrefix;
        }

        @Override
        public String correctPath(String usePath) {
            if (this.classRemovePrefix != null && usePath.startsWith(this.classRemovePrefix)) {
                usePath = usePath.substring(this.classRemovePrefix.length());
            }
            usePath = this.pathPrefix + usePath;
            return usePath;
        }
    }

    public static class NopRelocator
    implements ClassFileRelocator {
        public static ClassFileRelocator Instance = new NopRelocator();

        @Override
        public String correctPath(String path) {
            return path;
        }
    }
}

