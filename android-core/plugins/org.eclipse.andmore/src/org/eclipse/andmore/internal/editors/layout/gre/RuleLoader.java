/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout.gre;

import com.android.ide.common.api.IViewRule;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link RuleLoader} is responsible for loading (and unloading)
 * {@link IViewRule} classes. There is typically one {@link RuleLoader}
 * per project.
 */
public class RuleLoader {
    /**
     * Qualified name for the per-project non-persistent property storing the
     * {@link RuleLoader} for this project
     */
    private final static QualifiedName RULE_LOADER = new QualifiedName(AdtPlugin.PLUGIN_ID,
            "ruleloader"); //$NON-NLS-1$

    private final IProject mProject;
    private ClassLoader mUserClassLoader;
    private List<Pair<File, Long>> mUserJarTimeStamps;
    private long mLastCheckTimeStamp;

    /**
     * Flag set when we've attempted to initialize the {@link #mUserClassLoader}
     * already
     */
    private boolean mUserClassLoaderInited;

    /**
     * Returns the {@link RuleLoader} for the given project
     *
     * @param project the project the loader is associated with
     * @return an {@RuleLoader} for the given project,
     *         never null
     */
    public static RuleLoader get(IProject project) {
        RuleLoader loader = null;
        try {
            loader = (RuleLoader) project.getSessionProperty(RULE_LOADER);
        } catch (CoreException e) {
            // Not a problem; we will just create a new one
        }
        if (loader == null) {
            loader = new RuleLoader(project);
            try {
                project.setSessionProperty(RULE_LOADER, loader);
            } catch (CoreException e) {
                AdtPlugin.log(e, "Can't store RuleLoader");
            }
        }
        return loader;
    }

    /** Do not call; use the {@link #get} factory method instead. */
    private RuleLoader(IProject project) {
        mProject = project;
    }

    /**
     * Find out whether the given project has 3rd party ViewRules, and if so
     * return a ClassLoader which can locate them. If not, return null.
     * @param project The project to load user rules from
     * @return A class loader which can user view rules, or otherwise null
     */
    private ClassLoader computeUserClassLoader(IProject project) {
        // Default place to locate layout rules. The user may also add to this
        // path by defining a config property specifying
        // additional .jar files to search via a the layoutrules.jars property.
        ProjectState state = Sdk.getProjectState(project);
        ProjectProperties projectProperties = state.getProperties();

        // Ensure we have the latest & greatest version of the properties.
        // This allows users to reopen editors in a running Eclipse instance
        // to get updated view rule jars
        projectProperties.reload();

        String path = projectProperties.getProperty(
                ProjectProperties.PROPERTY_RULES_PATH);

        if (path != null && path.length() > 0) {

            mUserJarTimeStamps = new ArrayList<Pair<File, Long>>();
            mLastCheckTimeStamp = System.currentTimeMillis();

            List<URL> urls = new ArrayList<URL>();
            String[] pathElements = path.split(File.pathSeparator);
            for (String pathElement : pathElements) {
                pathElement = pathElement.trim(); // Avoid problems with trailing whitespace etc
                File pathFile = new File(pathElement);
                if (!pathFile.isAbsolute()) {
                    pathFile = new File(project.getLocation().toFile(), pathElement);
                }
                // Directories and jar files are okay. Do we need to
                // validate the files here as .jar files?
                if (pathFile.isFile() || pathFile.isDirectory()) {
                    URL url;
                    try {
                        url = pathFile.toURI().toURL();
                        urls.add(url);

                        mUserJarTimeStamps.add(Pair.of(pathFile, pathFile.lastModified()));
                    } catch (MalformedURLException e) {
                        AdtPlugin.log(IStatus.WARNING,
                                "Invalid URL: %1$s", //$NON-NLS-1$
                                e.toString());
                    }
                }
            }

            if (urls.size() > 0) {
                return new URLClassLoader(urls.toArray(new URL[urls.size()]),
                        RulesEngine.class.getClassLoader());
            }
        }

        return null;
    }

    /**
     * Return the class loader to use for custom views, or null if no custom
     * view rules are registered for the project. Note that this class loader
     * can change over time (if the jar files are updated), so callers should be
     * prepared to unload previous instances.
     *
     * @return a class loader to use for custom view rules, or null
     */
    public ClassLoader getClassLoader() {
        if (mUserClassLoader == null) {
            // Only attempt to load rule paths once.
            // TODO: Check the timestamp on the project.properties file so we can dynamically
            // pick up cases where the user edits the path
            if (!mUserClassLoaderInited) {
                mUserClassLoaderInited = true;
                mUserClassLoader = computeUserClassLoader(mProject);
            }
        } else {
            // Check the timestamp on the jar files in the custom view path to see if we
            // need to reload the classes (but only do this at most every 3 seconds)
            if (mUserJarTimeStamps != null) {
                long time = System.currentTimeMillis();
                if (time - mLastCheckTimeStamp > 3000) {
                    mLastCheckTimeStamp = time;
                    for (Pair<File, Long> pair : mUserJarTimeStamps) {
                        File file = pair.getFirst();
                        Long prevModified = pair.getSecond();
                        long modified = file.lastModified();
                        if (prevModified.longValue() != modified) {
                            mUserClassLoaderInited = true;
                            mUserJarTimeStamps = null;
                            mUserClassLoader = computeUserClassLoader(mProject);
                        }
                    }
                }
            }
        }

        return mUserClassLoader;
    }
}
