/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore;

import static com.android.SdkConstants.DOT_AIDL;
import static com.android.SdkConstants.DOT_DEP;
import static com.android.SdkConstants.DOT_FS;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.DOT_RS;

import com.android.SdkConstants;

import org.eclipse.andmore.internal.build.builders.PostCompilerBuilder;
import org.eclipse.andmore.internal.build.builders.PreCompilerBuilder;
import org.eclipse.andmore.internal.build.builders.ResourceManagerBuilder;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;

/**
 * Constant definition class.<br>
 * <br>
 * Most constants have a prefix defining the content.
 * <ul>
 * <li><code>WS_</code> Workspace path constant. Those are absolute paths,
 * from the project root.</li>
 * <li><code>OS_</code> OS path constant. These paths are different depending on the platform.</li>
 * <li><code>FN_</code> File name constant.</li>
 * <li><code>FD_</code> Folder name constant.</li>
 * <li><code>MARKER_</code> Resource Marker Ids constant.</li>
 * <li><code>EXT_</code> File extension constant. This does NOT include a dot.</li>
 * <li><code>DOT_</code> File extension constant. This start with a dot.</li>
 * <li><code>RE_</code> Regexp constant.</li>
 * <li><code>NS_</code> Namespace constant.</li>
 * <li><code>CLASS_</code> Fully qualified class name.</li>
 * </ul>
 *
 */
public class AdtConstants {
    /**
     * The old Editors Plugin ID. It is still used in some places for compatibility.
     * Please do not use for new features.
     */
    public static final String EDITORS_NAMESPACE = "org.eclipse.andmore.editors"; //$NON-NLS-1$

    /** Nature of default Android projects */
    public final static String NATURE_DEFAULT = "org.eclipse.andmore.adt.AndroidNature"; //$NON-NLS-1$

    /** The container id for the android framework jar file */
    public final static String CONTAINER_FRAMEWORK =
        "org.eclipse.andmore.ANDROID_FRAMEWORK"; //$NON-NLS-1$

    /** The container id for the libraries */
    public final static String CONTAINER_PRIVATE_LIBRARIES = "org.eclipse.andmore.LIBRARIES"; //$NON-NLS-1$
    public final static String CONTAINER_DEPENDENCIES = "org.eclipse.andmore.DEPENDENCIES";


    /** Separator for workspace path, i.e. "/". */
    public final static String WS_SEP = "/"; //$NON-NLS-1$
    /** Separator character for workspace path, i.e. '/'. */
    public final static char WS_SEP_CHAR = '/';

    /** aapt's proguard output */
    public final static String FN_AAPT_PROGUARD = "proguard.txt"; //$NON-NLS-1$

    /** Temporary packaged resources file name, i.e. "resources.ap_" */
    public final static String FN_RESOURCES_AP_ = "resources.ap_"; //$NON-NLS-1$

    public final static String FN_TRACEVIEW =
        (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_WINDOWS) ?
            "traceview.bat" : "traceview"; //$NON-NLS-1$ //$NON-NLS-2$

    public final static String FN_HPROF_CONV =
        (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_WINDOWS) ?
            "hprof-conv.exe" : "hprof-conv"; //$NON-NLS-1$ //$NON-NLS-2$

    /** Absolute path of the workspace root, i.e. "/" */
    public final static String WS_ROOT = WS_SEP;

    /** Absolute path of the resource folder, e.g. "/res".<br> This is a workspace path. */
    public final static String WS_RESOURCES = WS_SEP + SdkConstants.FD_RESOURCES;

    public final static String FD_CRUNCH = "crunch";  //$NON-NLS-1$
    public final static String FD_BC = "bc";  //$NON-NLS-1$

    /** Path of crunch cache folder relative to the output folder.<br> This is a workspace path. */
    public final static String WS_BIN_RELATIVE_CRUNCHCACHE = SdkConstants.FD_RESOURCES
                                                                + WS_SEP + FD_CRUNCH;

    /** Path of bc output folder relative to the output folder.<br> This is a workspace path. */
    public final static String WS_BIN_RELATIVE_BC = SdkConstants.FD_RESOURCES
                                                        + WS_SEP + FD_BC;

    /** Path of rs libs output folder relative to the output folder.<br> This is a workspace path. */
    public final static String WS_BIN_RELATIVE_RS_LIBS = SdkConstants.FD_RS_LIBS;

    /** Absolute path of the resource folder, e.g. "/assets".<br> This is a workspace path. */
    public final static String WS_ASSETS = WS_SEP + SdkConstants.FD_ASSETS;

    /** Absolute path of the layout folder, e.g. "/res/layout".<br> This is a workspace path. */
    public final static String WS_LAYOUTS = WS_RESOURCES + WS_SEP + SdkConstants.FD_RES_LAYOUT;

    /** Leaf of the javaDoc folder. Does not start with a separator. */
    public final static String WS_JAVADOC_FOLDER_LEAF = SdkConstants.FD_DOCS + "/" + //$NON-NLS-1$
            SdkConstants.FD_DOCS_REFERENCE;

    /** Path of the samples directory relative to the sdk folder.
     *  This is an OS path, ending with a separator.
     *  FIXME: remove once the NPW is fixed. */
    public final static String OS_SDK_SAMPLES_FOLDER = SdkConstants.FD_SAMPLES + File.separator;

    public final static String RE_DOT = "\\."; //$NON-NLS-1$
    /** Regexp for java extension, i.e. "\.java$" */
    public final static String RE_JAVA_EXT = "\\" + DOT_JAVA + "$"; //$NON-NLS-1$ //$NON-NLS-2$
    /** Regexp for aidl extension, i.e. "\.aidl$" */
    public final static String RE_AIDL_EXT = "\\" + DOT_AIDL + "$"; //$NON-NLS-1$ //$NON-NLS-2$
    /** Regexp for rs extension, i.e. "\.rs$" */
    public final static String RE_RS_EXT = "\\" + DOT_RS + "$"; //$NON-NLS-1$ //$NON-NLS-2$
    /** Regexp for rs extension, i.e. "\.fs$" */
    public final static String RE_FS_EXT = "\\" + DOT_FS + "$"; //$NON-NLS-1$ //$NON-NLS-2$
    /** Regexp for .d extension, i.e. "\.d$" */
    public final static String RE_DEP_EXT = "\\" + DOT_DEP + "$"; //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Namespace pattern for the custom resource XML, i.e. "http://schemas.android.com/apk/res/%s"
     * <p/>
     * This string contains a %s. It must be combined with the desired Java package, e.g.:
     * <pre>
     *    String.format(SdkConstants.NS_CUSTOM_RESOURCES, "android");
     *    String.format(SdkConstants.NS_CUSTOM_RESOURCES, "com.test.mycustomapp");
     * </pre>
     *
     * Note: if you need an URI specifically for the "android" namespace, consider using
     * {@link SdkConstants#NS_RESOURCES} instead.
     */
    // TODO rename NS_CUSTOM_RESOURCES to NS_CUSTOM_RESOURCES_S (denoting it takes a %s) in
    // another CL.
    public final static String NS_CUSTOM_RESOURCES = "http://schemas.android.com/apk/res/%1$s"; //$NON-NLS-1$

    /** The old common plug-in ID. Please do not use for new features. */
    private static final String LEGACY_PLUGIN_ID = "org.eclipse.andmore.common"; //$NON-NLS-1$

    /** Generic marker for ADT errors, only to be used in the {@link ResourceManagerBuilder} */
    public final static String MARKER_ADT = AdtPlugin.PLUGIN_ID + ".adtProblem"; //$NON-NLS-1$

    /** Marker for Android Target errors.
     * This is not cleared on each build like other markers. Instead, it's cleared
     * when an AndroidClasspathContainerInitializer has succeeded in creating an
     * AndroidClasspathContainer */
    public final static String MARKER_TARGET = AdtPlugin.PLUGIN_ID + ".targetProblem"; //$NON-NLS-1$
    /** Marker for Android Build Tools errors.
     * This is not cleared on each build like other markers. Instead, it's cleared
     * when the build tools are setup in the projectState. */
    public final static String MARKER_BUILD_TOOLS = AdtPlugin.PLUGIN_ID + ".buildToolsProblem"; //$NON-NLS-1$
    /** Marker for Android Dependency errors.
     * This is not cleared on each build like other markers. Instead, it's cleared
     * when a LibraryClasspathContainerInitializer has succeeded in creating a
     * LibraryClasspathContainer */
    public final static String MARKER_DEPENDENCY = AdtPlugin.PLUGIN_ID + ".dependencyProblem"; //$NON-NLS-1$


    /** aapt marker error when running the compile command, only to be used
     * in {@link PreCompilerBuilder} */
    public final static String MARKER_AAPT_COMPILE = LEGACY_PLUGIN_ID + ".aaptProblem"; //$NON-NLS-1$

    /** XML marker error, only to be used in {@link PreCompilerBuilder} */
    public final static String MARKER_XML = LEGACY_PLUGIN_ID + ".xmlProblem"; //$NON-NLS-1$

    /** aidl marker error, only to be used in {@link PreCompilerBuilder} */
    public final static String MARKER_AIDL = LEGACY_PLUGIN_ID + ".aidlProblem"; //$NON-NLS-1$

    /** renderscript marker error, only to be used in {@link PreCompilerBuilder} */
    public final static String MARKER_RENDERSCRIPT = LEGACY_PLUGIN_ID + ".rsProblem"; //$NON-NLS-1$

    /** android marker error, only to be used in the Manifest parsing
     * from the {@link PreCompilerBuilder} */
    public final static String MARKER_ANDROID = LEGACY_PLUGIN_ID + ".androidProblem"; //$NON-NLS-1$


    /** aapt marker error when running the package command, only to be used in
     * {@link PostCompilerBuilder} */
    public final static String MARKER_AAPT_PACKAGE = LEGACY_PLUGIN_ID + ".aapt2Problem"; //$NON-NLS-1$

    /** final packaging error marker, only to be used in {@link PostCompilerBuilder} */
    public final static String MARKER_PACKAGING = AdtPlugin.PLUGIN_ID + ".packagingProblem"; //$NON-NLS-1$

    /** manifest merger error, only to be used in {@link PreCompilerBuilder} */
    public final static String MARKER_MANIFMERGER = AdtPlugin.PLUGIN_ID + ".manifMergerProblem"; //$NON-NLS-1$

    /** Marker for lint errors */
    public final static String MARKER_LINT = AdtPlugin.PLUGIN_ID + ".lintProblem"; //$NON-NLS-1$

    /** Name for the "type" marker attribute */
    public final static String MARKER_ATTR_TYPE = "android.type"; //$NON-NLS-1$
    /** Name for the "class" marker attribute */
    public final static String MARKER_ATTR_CLASS = "android.class"; //$NON-NLS-1$
    /** activity value for marker attribute "type" */
    public final static String MARKER_ATTR_TYPE_ACTIVITY = "activity"; //$NON-NLS-1$
    /** service value for marker attribute "type" */
    public final static String MARKER_ATTR_TYPE_SERVICE = "service"; //$NON-NLS-1$
    /** receiver value for marker attribute "type" */
    public final static String MARKER_ATTR_TYPE_RECEIVER = "receiver"; //$NON-NLS-1$
    /** provider value for marker attribute "type" */
    public final static String MARKER_ATTR_TYPE_PROVIDER = "provider"; //$NON-NLS-1$

    /**
     * Preferred compiler level, i.e. "1.6".
     */
    public final static String COMPILER_COMPLIANCE_PREFERRED = JavaCore.VERSION_1_6;
    /**
     * List of valid compiler level, i.e. "1.5" and "1.6"
     */
    public final static String[] COMPILER_COMPLIANCE = {
        JavaCore.VERSION_1_5,
        JavaCore.VERSION_1_6,
    };

    /** The base URL where to find the Android class & manifest documentation */
    public static final String CODESITE_BASE_URL = "http://code.google.com/android";  //$NON-NLS-1$

    public static final String LIBRARY_TEST_RUNNER = "android.test.runner"; //$NON-NLS-1$

    /** Documentation marker for elements, attributes etc that should be hidden */
    public static final String DOC_HIDE = "@hide"; //$NON-NLS-1$

    public static final String DEX_OPTIONS_FORCEJUMBO = "dex.force.jumbo"; //$NON-NLS-1$
    public static final String DEX_OPTIONS_DISABLE_MERGER = "dex.disable.merger"; //$NON-NLS-1$
}
