/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.state.ClassFileRelocator;
import org.benf.cfr.reader.state.ClassRenamer;
import org.benf.cfr.reader.state.JarContentImpl;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.Predicate;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

public class ClassFileSourceImpl
implements ClassFileSource2 {
    private final Set<String> explicitJars = SetFactory.newSet();
    private Map<String, JarSourceEntry> classToPathMap;
    private final Options options;
    private ClassRenamer classRenamer;
    private ClassFileRelocator classRelocator;
    private static final boolean JrtPresent = ClassFileSourceImpl.CheckJrt();
    private static final Map<String, String> packMap = JrtPresent ? ClassFileSourceImpl.getPackageToModuleMap() : new HashMap();

    private static boolean CheckJrt() {
        try {
            return Object.class.getResource("Object.class").getProtocol().equals("jrt");
        }
        catch (Exception e) {
            return false;
        }
    }

    public ClassFileSourceImpl(Options options) {
        this.options = options;
    }

    private byte[] getBytesFromFile(InputStream is, long length) throws IOException {
        int offset;
        int numRead;
        byte[] bytes = new byte[(int)length];
        for (offset = 0; offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0; offset += numRead) {
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file");
        }
        is.close();
        return bytes;
    }

    @Override
    public String getPossiblyRenamedPath(String path) {
        if (this.classRenamer == null) {
            return path;
        }
        String res = this.classRenamer.getRenamedClass(path + ".class");
        if (res == null) {
            return path;
        }
        return res.substring(0, res.length() - 6);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Pair<byte[], String> getClassFileContent(String inputPath) throws IOException {
        Map<String, JarSourceEntry> classPathFiles = this.getClassPathClasses();
        JarSourceEntry jarEntry = classPathFiles.get(inputPath);
        String path = inputPath;
        if (this.classRenamer != null) {
            path = this.classRenamer.getOriginalClass(path);
        }
        ZipFile zipFile = null;
        try {
            byte[] content;
            File file;
            String usePath = this.classRelocator.correctPath(path);
            boolean forceJar = jarEntry != null && this.explicitJars.contains(jarEntry.getPath());
            File file2 = file = forceJar ? null : new File(usePath);
            if (file != null && file.exists()) {
                FileInputStream is = new FileInputStream(file);
                long length = file.length();
                content = this.getBytesFromFile(is, length);
            } else if (jarEntry != null) {
                zipFile = new ZipFile(new File(jarEntry.getPath()), 1);
                if (jarEntry.analysisType == AnalysisType.WAR) {
                    path = "WEB-INF/classes/" + path;
                }
                ZipEntry zipEntry = zipFile.getEntry(path);
                long length = zipEntry.getSize();
                InputStream is = zipFile.getInputStream(zipEntry);
                content = this.getBytesFromFile(is, length);
            } else {
                content = this.getInternalContent(inputPath);
            }
            Pair<byte[], String> pair = Pair.make(content, inputPath);
            return pair;
        }
        finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    private static Map<String, String> getPackageToModuleMap() {
        Map<String, String> mapRes = MapFactory.newMap();
        try {
            Class<?> moduleLayerClass = Class.forName("java.lang.ModuleLayer");
            Method bootMethod = moduleLayerClass.getMethod("boot", new Class[0]);
            Object boot = bootMethod.invoke(null, new Object[0]);
            Method modulesMeth = boot.getClass().getMethod("modules", new Class[0]);
            Object modules = modulesMeth.invoke(boot, new Object[0]);
            Class<?> moduleClass = Class.forName("java.lang.Module");
            Method getPackagesMethod = moduleClass.getMethod("getPackages", new Class[0]);
            Method getNameMethod = moduleClass.getMethod("getName", new Class[0]);
            for (Object module : (Set)modules) {
                Set packageNames = (Set)getPackagesMethod.invoke(module, new Object[0]);
                String moduleName = (String)getNameMethod.invoke(module, new Object[0]);
                for (String packageName : packageNames) {
                    if (mapRes.containsKey(packageName)) {
                        mapRes.put(packageName, null);
                        continue;
                    }
                    mapRes.put(packageName, moduleName);
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return mapRes;
    }

    private byte[] getContentByFromReflectedClass(String inputPath) {
        try {
            Class<?> cls;
            byte[] res;
            String classPath = inputPath.replace("/", ".").substring(0, inputPath.length() - 6);
            Pair<String, String> packageAndClassNames = ClassNameUtils.getPackageAndClassNames(classPath);
            String packageName = packageAndClassNames.getFirst();
            String moduleName = packMap.get(packageName);
            if (moduleName != null && (res = this.getUrlContent(new URL("jrt:/" + moduleName + "/" + inputPath))) != null) {
                return res;
            }
            try {
                cls = Class.forName(classPath);
            }
            catch (IllegalStateException e) {
                return null;
            }
            int idx = inputPath.lastIndexOf("/");
            String name = idx < 0 ? inputPath : inputPath.substring(idx + 1);
            return this.getUrlContent(cls.getResource(name));
        }
        catch (Exception e) {
            return null;
        }
    }

    private byte[] getUrlContent(URL url) {
        int len;
        InputStream is;
        String protocol = url.getProtocol();
        if (!protocol.equals("jrt")) {
            return null;
        }
        try {
            URLConnection uc = url.openConnection();
            uc.connect();
            is = uc.getInputStream();
            len = uc.getContentLength();
        }
        catch (IOException ioe) {
            return null;
        }
        try {
            if (len >= 0) {
                byte[] b = new byte[len];
                int i = len;
                while (i > 0) {
                    if (i >= (i -= is.read(b, len - i, i))) continue;
                    i = -1;
                }
                if (i == 0) {
                    return b;
                }
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return null;
    }

    private byte[] getInternalContent(String inputPath) throws IOException {
        byte[] res;
        if (JrtPresent && (res = this.getContentByFromReflectedClass(inputPath)) != null) {
            return res;
        }
        throw new IOException("No such file " + inputPath);
    }

    @Override
    @Deprecated
    public Collection<String> addJar(String jarPath) {
        return this.addJarContent(jarPath, AnalysisType.JAR).getClassFiles();
    }

    @Override
    public JarContent addJarContent(String jarPath, AnalysisType analysisType) {
        String jarClassPath;
        this.getClassPathClasses();
        File file = new File(jarPath);
        if (!file.exists()) {
            throw new ConfusedCFRException("No such jar file " + jarPath);
        }
        jarPath = file.getAbsolutePath();
        JarContent jarContent = this.processClassPathFile(file, false, analysisType);
        if (jarContent == null) {
            throw new ConfusedCFRException("Failed to load jar " + jarPath);
        }
        JarSourceEntry sourceEntry = new JarSourceEntry(analysisType, jarPath);
        if (this.classRenamer != null) {
            this.classRenamer.notifyClassFiles(jarContent.getClassFiles());
        }
        if ((jarClassPath = jarContent.getManifestEntries().get("Class-Path")) != null) {
            this.addToRelativeClassPath(file, jarClassPath);
        }
        List output = ListFactory.newList();
        for (String classPath : jarContent.getClassFiles()) {
            if (!classPath.toLowerCase().endsWith(".class")) continue;
            if (this.classRenamer != null) {
                classPath = this.classRenamer.getRenamedClass(classPath);
            }
            this.classToPathMap.put(classPath, sourceEntry);
            output.add(classPath);
        }
        this.explicitJars.add(jarPath);
        return jarContent;
    }

    private Map<String, JarSourceEntry> getClassPathClasses() {
        if (this.classToPathMap == null) {
            String[] classPaths;
            String extraClassPath;
            boolean dump = (Boolean)this.options.getOption(OptionsImpl.DUMP_CLASS_PATH);
            this.classToPathMap = MapFactory.newMap();
            String classPath = System.getProperty("java.class.path");
            String sunBootClassPath = System.getProperty("sun.boot.class.path");
            if (sunBootClassPath != null) {
                classPath = classPath + File.pathSeparatorChar + sunBootClassPath;
            }
            if (dump) {
                System.out.println("/* ClassPath Diagnostic - searching :" + classPath);
            }
            if (null != (extraClassPath = (String)this.options.getOption(OptionsImpl.EXTRA_CLASS_PATH))) {
                classPath = classPath + File.pathSeparatorChar + extraClassPath;
            }
            this.classRenamer = ClassRenamer.create(this.options);
            for (String path : classPaths = classPath.split("" + File.pathSeparatorChar)) {
                this.processToClassPath(dump, path);
            }
            if (dump) {
                System.out.println(" */");
            }
        }
        return this.classToPathMap;
    }

    private void processToClassPath(boolean dump, String path) {
        File f;
        if (dump) {
            System.out.println(" " + path);
        }
        if ((f = new File(path)).exists()) {
            if (f.isDirectory()) {
                File[] files;
                if (dump) {
                    System.out.println(" (Directory)");
                }
                if ((files = f.listFiles()) != null) {
                    for (File file : files) {
                        this.processClassPathFile(file, file.getAbsolutePath(), this.classToPathMap, AnalysisType.JAR, dump);
                    }
                }
            } else {
                this.processClassPathFile(f, path, this.classToPathMap, AnalysisType.JAR, dump);
            }
        } else if (dump) {
            System.out.println(" (Can't access)");
        }
    }

    private void addToRelativeClassPath(File file, String jarClassPath) {
        String[] classPaths = jarClassPath.split(" ");
        boolean dump = (Boolean)this.options.getOption(OptionsImpl.DUMP_CLASS_PATH);
        String relative = null;
        try {
            relative = file.getParentFile().getCanonicalPath();
        }
        catch (IOException e) {
            return;
        }
        for (String path : classPaths) {
            this.processToClassPath(dump, relative + File.separatorChar + path);
        }
    }

    private void processClassPathFile(File file, String absolutePath, Map<String, JarSourceEntry> classToPathMap, AnalysisType analysisType, boolean dump) {
        JarContent content = this.processClassPathFile(file, dump, analysisType);
        if (content == null) {
            return;
        }
        JarSourceEntry sourceEntry = new JarSourceEntry(analysisType, absolutePath);
        for (String name : content.getClassFiles()) {
            classToPathMap.put(name, sourceEntry);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private JarContent processClassPathFile(File file, boolean dump, AnalysisType analysisType) {
        Map<String, String> manifest;
        List<String> content = ListFactory.newList();
        try {
            ZipFile zipFile = new ZipFile(file, 1);
            manifest = this.getManifestContent(zipFile);
            try {
                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry entry = enumeration.nextElement();
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    if (name.endsWith(".class")) {
                        if (dump) {
                            System.out.println("  " + name);
                        }
                        content.add(name);
                        continue;
                    }
                    if (!dump) continue;
                    System.out.println("  [ignoring] " + name);
                }
            }
            finally {
                zipFile.close();
            }
        }
        catch (IOException e) {
            return null;
        }
        if (analysisType == AnalysisType.WAR) {
            final int prefixLen = "WEB-INF/classes/".length();
            content = Functional.map(Functional.filter(content, new Predicate<String>(){

                @Override
                public boolean test(String in) {
                    return in.startsWith("WEB-INF/classes/");
                }
            }), new UnaryFunction<String, String>(){

                @Override
                public String invoke(String arg) {
                    return arg.substring(prefixLen);
                }
            });
        }
        return new JarContentImpl(content, manifest, analysisType);
    }

    private Map<String, String> getManifestContent(ZipFile zipFile) {
        try {
            Map<String, String> manifest;
            ZipEntry manifestEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
            if (manifestEntry == null) {
                manifest = MapFactory.newMap();
            } else {
                String line;
                InputStream is = zipFile.getInputStream(manifestEntry);
                BufferedReader bis = new BufferedReader(new InputStreamReader(is));
                manifest = MapFactory.newMap();
                while (null != (line = bis.readLine())) {
                    int idx = line.indexOf(58);
                    if (idx <= 0) continue;
                    manifest.put(line.substring(0, idx), line.substring(idx + 1).trim());
                }
                bis.close();
            }
            return manifest;
        }
        catch (Exception e) {
            return MapFactory.newMap();
        }
    }

    @Override
    public void informAnalysisRelativePathDetail(String usePath, String specPath) {
        this.classRelocator = new ClassFileRelocator.Configurator().configureWith(usePath, specPath);
    }

    private static class JarSourceEntry {
        private final AnalysisType analysisType;
        private final String path;

        JarSourceEntry(AnalysisType analysisType, String path) {
            this.analysisType = analysisType;
            this.path = path;
        }

        public AnalysisType getAnalysisType() {
            return this.analysisType;
        }

        public String getPath() {
            return this.path;
        }
    }
}

