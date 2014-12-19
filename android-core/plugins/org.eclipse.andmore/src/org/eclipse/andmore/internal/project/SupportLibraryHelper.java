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

package org.eclipse.andmore.internal.project;

import static com.android.SdkConstants.FQCN_GRID_LAYOUT;
import static com.android.SdkConstants.FQCN_GRID_LAYOUT_V7;
import static com.android.SdkConstants.FQCN_SPACE;
import static com.android.SdkConstants.FQCN_SPACE_V7;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.actions.AddSupportJarAction;
import org.eclipse.andmore.internal.editors.manifest.ManifestInfo;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.internal.sdk.ProjectState.LibraryState;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Helper class for the Android Support Library. The support library provides
 * (for example) a backport of GridLayout, which must be used as a library
 * project rather than a jar library since it has resources. This class provides
 * support for finding the library project, or downloading and installing it on
 * demand if it does not, as well as translating tags such as
 * {@code <GridLayout>} into {@code <com.android.support.v7.GridLayout>} if it
 * does not.
 */
public class SupportLibraryHelper {
    /**
     * Returns the correct tag to use for the given view tag. This is normally
     * the same as the tag itself. However, for some views which are not available
     * on all platforms, this will:
     * <ul>
     *  <li> Check if the view is available in the compatibility library,
     *       and if so, if the support library is not installed, will offer to
     *       install it via the SDK manager.
     *  <li> (The tool may also offer to adjust the minimum SDK of the project
     *       up to a level such that the given tag is supported directly, and then
     *       this method will return the original tag.)
     *  <li> Check whether the compatibility library is included in the project, and
     *       if not, offer to copy it into the workspace and add a library dependency.
     *  <li> Return the alternative tag. For example, for "GridLayout", it will
     *       (if the minimum SDK is less than 14) return "com.android.support.v7.GridLayout"
     *       instead.
     * </ul>
     *
     * @param project the project to add the dependency into
     * @param tag the tag to look up, such as "GridLayout"
     * @return the tag to use in the layout, normally the same as the input tag but possibly
     *      an equivalent compatibility library tag instead.
     */
    @NonNull
    public static String getTagFor(@NonNull IProject project, @NonNull String tag) {
        boolean isGridLayout = tag.equals(FQCN_GRID_LAYOUT);
        boolean isSpace = tag.equals(FQCN_SPACE);
        if (isGridLayout || isSpace) {
            int minSdk = ManifestInfo.get(project).getMinSdkVersion();
            if (minSdk < 14) {
                // See if the support library is installed in the SDK area
                // See if there is a local project in the workspace providing the
                // project
                IProject supportProject = getSupportProjectV7();
                if (supportProject != null) {
                    // Make sure I have a dependency on it
                    ProjectState state = Sdk.getProjectState(project);
                    if (state != null) {
                        for (LibraryState library : state.getLibraries()) {
                            if (supportProject.equals(library.getProjectState().getProject())) {
                                // Found it: you have the compatibility library and have linked
                                // to it: use the alternative tag
                                return isGridLayout ? FQCN_GRID_LAYOUT_V7 : FQCN_SPACE_V7;
                            }
                        }
                    }
                }

                // Ask user to install it
                String message = String.format(
                        "%1$s requires API level 14 or higher, or a compatibility "
                                + "library for older versions.\n\n"
                                + " Do you want to install the compatibility library?", tag);
                MessageDialog dialog =
                        new MessageDialog(
                                Display.getCurrent().getActiveShell(),
                                "Warning",
                                null,
                                message,
                                MessageDialog.QUESTION,
                                new String[] {
                                        "Install", "Cancel"
                                },
                                1 /* default button: Cancel */);
                int answer = dialog.open();
                if (answer == 0) {
                    if (supportProject != null) {
                        // Just add library dependency
                        if (!AddSupportJarAction.addLibraryDependency(
                                supportProject,
                                project,
                                true /* waitForFinish */)) {
                            return tag;
                        }
                    } else {
                        // Install library AND add dependency
                        if (!AddSupportJarAction.installGridLayoutLibrary(
                                project,
                                true /* waitForFinish */)) {
                            return tag;
                        }
                    }

                    return isGridLayout ? FQCN_GRID_LAYOUT_V7 : FQCN_SPACE_V7;
                }
            }
        }

        return tag;
    }

    /** Cache for {@link #getSupportProjectV7()} */
    private static IProject sCachedProject;

    /**
     * Finds and returns the support project in the workspace, if any.
     *
     * @return the android support library project, or null if not found
     */
    @Nullable
    public static IProject getSupportProjectV7() {
        if (sCachedProject != null) {
            if (sCachedProject.isAccessible()) {
                return sCachedProject;
            } else {
                sCachedProject = null;
            }
        }

        sCachedProject = findSupportProjectV7();
        return sCachedProject;
    }

    @Nullable
    private static IProject findSupportProjectV7() {
        for (IJavaProject javaProject : AdtUtils.getOpenAndroidProjects()) {
            IProject project = javaProject.getProject();
            ProjectState state = Sdk.getProjectState(project);
            if (state != null && state.isLibrary()) {
                ManifestInfo manifestInfo = ManifestInfo.get(project);
                if (manifestInfo.getPackage().equals("android.support.v7.gridlayout")) { //$NON-NLS-1$
                    return project;
                }
            }
        }

        return null;
    }
}
