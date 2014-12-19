/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.eclipse.andmore.internal.build.builders;

import com.android.SdkConstants;
import com.android.annotations.NonNull;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to generate {@link ChangedFileSet} for given projects.
 *
 * Also contains non project specific {@link ChangedFileSet} such as {@link #MANIFEST}
 * and {@link #NATIVE_LIBS}
 */
class ChangedFileSetHelper {

    final static ChangedFileSet MANIFEST;
    final static ChangedFileSet NATIVE_LIBS;

    static {
        MANIFEST = new ChangedFileSet("manifest",                                  //$NON-NLS-1$
                SdkConstants.FN_ANDROID_MANIFEST_XML);

        // FIXME: move compiled native libs to bin/libs/
        NATIVE_LIBS = new ChangedFileSet(
                "nativeLibs",
                SdkConstants.FD_NATIVE_LIBS + "/*/*.so",                           //$NON-NLS-1$
                SdkConstants.FD_NATIVE_LIBS + "/*/" + SdkConstants.FN_GDBSERVER);  //$NON-NLS-1$
    }

    /**
     * Returns a ChangedFileSet for Java resources inside a given project's source folders.
     * @param project the project.
     * @return a ChangedFileSet
     */
    static ChangedFileSet getJavaResCfs(@NonNull IProject project) {

        // get the source folder for the given project.
        IPath projectPath = project.getFullPath();

        // get the source folders.
        List<IPath> srcPaths = BaseProjectHelper.getSourceClasspaths(project);
        List<String> paths = new ArrayList<String>(srcPaths.size());

        // create a pattern for each of them.
        for (IPath path : srcPaths) {
            paths.add(path.makeRelativeTo(projectPath).toString() + "/**");        //$NON-NLS-1$
        }

        // custom ChangedFileSet to ignore .java files.
        return new JavaResChangedSet("javaRes",                                    //$NON-NLS-1$
                paths.toArray(new String[paths.size()]));
    }

    /**
     * Returns a {@link ChangedFileSet} for all the resources (included assets), and the output
     * file (compiled resources
     * @param project the project
     * @return a ChangeFileSet
     */
    static ChangedFileSet getResCfs(@NonNull IProject project) {
        // generated res is inside the project's android output folder
        String path = getRelativeAndroidOut(project);

        ChangedFileSet set = new ChangedFileSet(
                "resources",                                                       //$NON-NLS-1$
                SdkConstants.FD_RES + "/**",                                       //$NON-NLS-1$
                SdkConstants.FD_ASSETS + "/**",                                    //$NON-NLS-1$
                path + '/' + AdtConstants.WS_BIN_RELATIVE_BC + "/**");             //$NON-NLS-1$

        // output file is based on the project's android output folder
        set.setOutput(path + '/' + AdtConstants.FN_RESOURCES_AP_);

        return set;
    }

    /**
     * Returns a {@link ChangedFileSet} for all the resources (included assets), and the output
     * file (compiled resources
     * @param project the project
     * @return a ChangeFileSet
     */
    static ChangedFileSet getMergedManifestCfs(@NonNull IProject project) {
        // input path is inside the project's android output folder
        String path = getRelativeAndroidOut(project);

        ChangedFileSet set = new ChangedFileSet(
                "mergedManifest",                                                 //$NON-NLS-1$
                path + '/' + SdkConstants.FN_ANDROID_MANIFEST_XML);

        return set;
    }

    /**
     * Returns a {@link ChangedFileSet} for the generated R.txt file
     * @param project the project
     * @return a ChangeFileSet
     */
    static ChangedFileSet getTextSymbols(@NonNull IProject project) {
        // input path is inside the project's android output folder
        String path = getRelativeAndroidOut(project);

        ChangedFileSet set = new ChangedFileSet(
                "textSymbols",                                                   //$NON-NLS-1$
                path + '/' + SdkConstants.FN_RESOURCE_TEXT);

        return set;
    }

    /**
     * Returns a {@link ChangedFileSet} for a project's javac output.
     * @param project the project
     * @return a ChangedFileSet
     */
    static ChangedFileSet getByteCodeCfs(@NonNull IProject project) {
        // input pattern is based on the project's Java compiler's output folder
        String path = getRelativeJavaCOut(project);

        ChangedFileSet set = new ChangedFileSet("compiledCode",                   //$NON-NLS-1$
                path + "/**/*" + SdkConstants.DOT_CLASS);                         //$NON-NLS-1$

        return set;
    }

    /**
     * Returns a {@link ChangedFileSet} for a project's complete resources, including
     * generated resources and crunch cache.
     * @param project the project
     * @return a ChangeFileSet
     */
    static ChangedFileSet getFullResCfs(@NonNull IProject project) {
        // generated res are in the project's android output folder
        String path = getRelativeAndroidOut(project);

        ChangedFileSet set = new ChangedFileSet("libResources",                   //$NON-NLS-1$
                SdkConstants.FD_RES + "/**",                                      //$NON-NLS-1$
                path + '/' + SdkConstants.FD_RES + "/**");                        //$NON-NLS-1$

        return set;
    }

    /**
     * Returns a {@link ChangedFileSet} for a project's whole code, including
     * compiled bytecode, 3rd party libs, and the output file containing the Dalvik
     * bytecode file.
     * @param project the project
     * @return a ChangeFileSet
     */
    static ChangedFileSet getCodeCfs(@NonNull IProject project) {
        // input pattern is based on the project's Java compiler's output folder
        String path = getRelativeJavaCOut(project);

        ChangedFileSet set = new ChangedFileSet("classAndJars",                    //$NON-NLS-1$
                path + "/**/*" + SdkConstants.DOT_CLASS,                           //$NON-NLS-1$
                SdkConstants.FD_NATIVE_LIBS + "/*" + SdkConstants.DOT_JAR);        //$NON-NLS-1$

        // output file is based on the project's android output folder
        path = getRelativeAndroidOut(project);
        set.setOutput(path + '/' + SdkConstants.FN_APK_CLASSES_DEX);

        return set;
    }

    private static String getRelativePath(@NonNull IProject project, @NonNull IResource resource) {
        return resource.getFullPath().makeRelativeTo(project.getFullPath()).toString();
    }

    private static String getRelativeAndroidOut(@NonNull IProject project) {
        IFolder folder = BaseProjectHelper.getAndroidOutputFolder(project);
        return getRelativePath(project, folder);
    }

    private static String getRelativeJavaCOut(@NonNull IProject project) {
        IFolder folder = BaseProjectHelper.getJavaOutputFolder(project);
        return getRelativePath(project, folder);
    }
}
