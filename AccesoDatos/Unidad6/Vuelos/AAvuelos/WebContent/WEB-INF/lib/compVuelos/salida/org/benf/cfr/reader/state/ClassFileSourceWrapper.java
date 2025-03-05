/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.state;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.state.JarContentImpl;
import org.benf.cfr.reader.util.AnalysisType;

public class ClassFileSourceWrapper
implements ClassFileSource2 {
    private final ClassFileSource classFileSource;

    public ClassFileSourceWrapper(ClassFileSource classFileSource) {
        this.classFileSource = classFileSource;
    }

    @Override
    public JarContent addJarContent(String jarPath, AnalysisType type) {
        return new JarContentImpl(this.classFileSource.addJar(jarPath), Collections.<String, String>emptyMap(), type);
    }

    @Override
    public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
        this.classFileSource.informAnalysisRelativePathDetail(usePath, classFilePath);
    }

    @Override
    public Collection<String> addJar(String jarPath) {
        return this.classFileSource.addJar(jarPath);
    }

    @Override
    public String getPossiblyRenamedPath(String path) {
        return this.classFileSource.getPossiblyRenamedPath(path);
    }

    @Override
    public Pair<byte[], String> getClassFileContent(String path) throws IOException {
        return this.classFileSource.getClassFileContent(path);
    }
}

