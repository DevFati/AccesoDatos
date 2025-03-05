/*
 * Decompiled with CFR 0.152.
 */
package org.benf.cfr.reader.util;

import java.util.regex.Pattern;

public interface MiscConstants {
    public static final String CFR_HEADER_BRA = "Decompiled with CFR";
    public static final String INIT_METHOD = "<init>";
    public static final String STATIC_INIT_METHOD = "<clinit>";
    public static final String DOT_THIS = ".this";
    public static final String THIS = "this";
    public static final String CLASS = "class";
    public static final String NEW = "new";
    public static final String EQUALS = "equals";
    public static final String HASHCODE = "hashCode";
    public static final String TOSTRING = "toString";
    public static final String PACKAGE_INFO = "package-info";
    public static final String UNBOUND_GENERIC = "?";
    public static final char INNER_CLASS_SEP_CHAR = '$';
    public static final String INNER_CLASS_SEP_STR = "$";
    public static final String DESERIALISE_LAMBDA_METHOD = "$deserializeLambda$";
    public static final String SCALA_SERIAL_VERSION = "serialVersionUID";
    public static final String GET_CLASS_NAME = "getClass";
    public static final String REQUIRE_NON_NULL = "requireNonNull";
    public static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
    public static final String MULTI_RELEASE_KEY = "Multi-Release";
    public static final String MANIFEST_CLASS_PATH = "Class-Path";
    public static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";
    public static final String WAR_PREFIX = "WEB-INF/classes/";
    public static final Pattern MULTI_RELEASE_PATH_PATTERN = Pattern.compile("^META-INF/versions/(\\d+)/(.*)$");
}

