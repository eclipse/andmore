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

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.build.BuildHelper;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Delta visitor checking changed files against given glob-patterns.
 *
 * The visitor is given {@link ChangedFileSet} objects which contains patterns to detect change
 * in input and output files. (Output files are only tested if the delta indicate the file
 * was removed).
 *
 * After the visitor has visited the whole delta, it can be queried to see which ChangedFileSet
 * recognized a file change. (ChangedFileSet are immutable and do not record this info).
 */
class PatternBasedDeltaVisitor implements IResourceDeltaVisitor {

    private final static boolean DEBUG_LOG = "1".equals(              //$NON-NLS-1$
            System.getenv("ANDROID_VISITOR_DEBUG"));                  //$NON-NLS-1$

    private final IProject mMainProject;
    private final IProject mDeltaProject;

    private final List<ChangedFileSet> mSets = new ArrayList<ChangedFileSet>();
    private final Map<ChangedFileSet, Boolean> mResults =
            new IdentityHashMap<ChangedFileSet, Boolean>();

    private final String mLogName;

    PatternBasedDeltaVisitor(IProject mainProject, IProject deltaProject, String logName) {
        mMainProject = mainProject;
        mDeltaProject = deltaProject;
        mLogName = logName;
        if (DEBUG_LOG) {
            AdtPlugin.log(IStatus.INFO, "%s (%s): Delta for %s",               //$NON-NLS-1$
                    mMainProject.getName(), mLogName, mDeltaProject.getName());
        }
    }

    void addSet(ChangedFileSet bundle) {
        mSets.add(bundle);
    }

    boolean checkSet(ChangedFileSet bundle) {
        Boolean r = mResults.get(bundle);
        if (r != null) {
            return r.booleanValue();
        }

        return false;
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();

        if (resource.getType() == IResource.FOLDER) {
            // always visit the subfolders, unless the folder is not to be included
            return BuildHelper.checkFolderForPackaging((IFolder)resource);

        } else if (resource.getType() == IResource.FILE) {
            IPath path = resource.getFullPath().makeRelativeTo(mDeltaProject.getFullPath());
            String pathStr = path.toString();

            // FIXME: no need to loop through all the sets once they have all said they need something (return false below and above)
            for (ChangedFileSet set : mSets) {
                // FIXME: should ignore sets that have already returned true.

                if (set.isInput(pathStr, path)) {
                    mResults.put(set, Boolean.TRUE);

                    if (DEBUG_LOG) {
                        String cfs_logName = set.getLogName();

                        if (cfs_logName != null) {
                            AdtPlugin.log(IStatus.INFO, "%s (%s:%s): %s",              //$NON-NLS-1$
                                    mMainProject.getName(), mLogName, cfs_logName,
                                    resource.getFullPath().toString());
                        } else {
                            AdtPlugin.log(IStatus.INFO, "%s (%s): %s",                 //$NON-NLS-1$
                                    mMainProject.getName(), mLogName,
                                    resource.getFullPath().toString());
                        }
                    }

                } else if (delta.getKind() == IResourceDelta.REMOVED &&
                        set.isOutput(pathStr, path)) {
                    mResults.put(set, Boolean.TRUE);

                    if (DEBUG_LOG) {
                        String cfs_logName = set.getLogName();

                        if (cfs_logName != null) {
                            AdtPlugin.log(IStatus.INFO, "%s (%s:%s): %s",              //$NON-NLS-1$
                                    mMainProject.getName(), mLogName, cfs_logName,
                                    resource.getFullPath().toString());
                        } else {
                            AdtPlugin.log(IStatus.INFO, "%s (%s): %s",                 //$NON-NLS-1$
                                    mMainProject.getName(), mLogName,
                                    resource.getFullPath().toString());
                        }
                    }
                }
            }
        }

        return true;
    }
}
