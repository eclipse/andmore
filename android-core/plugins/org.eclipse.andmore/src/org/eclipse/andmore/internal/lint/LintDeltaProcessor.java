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
package org.eclipse.andmore.internal.lint;

import static com.android.SdkConstants.DOT_CLASS;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.EXT_JAVA;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IFileListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Delta processor for Java files, which runs single-file lints if it finds that
 * the currently active file has been updated.
 */
public class LintDeltaProcessor implements Runnable {
    private List<IResource> mFiles;
    private IFile mActiveFile;

    private LintDeltaProcessor() {
        // Get the active editor file, if any
        Display display = AdtPlugin.getDisplay();
        if (display == null || display.isDisposed()) {
            return;
        }
        if (display.getThread() != Thread.currentThread()) {
            display.syncExec(this);
        } else {
            run();
        }
    }

    /**
     * Creates a new {@link LintDeltaProcessor}
     *
     * @return a visitor
     */
    @NonNull
    public static LintDeltaProcessor create() {
        return new LintDeltaProcessor();
    }

    /**
     * Process the given delta: update lint on any Java source and class files found.
     *
     * @param delta the delta describing recently changed files
     */
    public void process(@NonNull IResourceDelta delta)  {
        if (mActiveFile == null || !mActiveFile.getName().endsWith(DOT_JAVA)) {
            return;
        }

        mFiles = new ArrayList<IResource>();
        gatherFiles(delta);

        if (!mFiles.isEmpty()) {
            EclipseLintRunner.startLint(mFiles, mActiveFile, null,
                    false /*fatalOnly*/, false /*show*/);
        }
    }

    /**
     * Process edits in the given file: update lint on the Java source provided
     * it's the active file.
     *
     * @param file the file that was changed
     */
    public void process(@NonNull IFile file)  {
        if (mActiveFile == null || !mActiveFile.getName().endsWith(DOT_JAVA)) {
            return;
        }

        if (file.equals(mActiveFile)) {
            mFiles = Collections.<IResource>singletonList(file);
            EclipseLintRunner.startLint(mFiles, mActiveFile, null,
                    false /*fatalOnly*/, false /*show*/);
        }
    }

    /**
     * Collect .java and .class files to be run in lint. Only collects files
     * that match the active editor.
     */
    private void gatherFiles(@NonNull IResourceDelta delta) {
        IResource resource = delta.getResource();
        String name = resource.getName();
        if (name.endsWith(DOT_JAVA)) {
            if (resource.equals(mActiveFile)) {
                mFiles.add(resource);
            }
        } else if (name.endsWith(DOT_CLASS)) {
            // Make sure this class corresponds to the .java file, meaning it has
            // the same basename, or that it is an inner class of a class that
            // matches the same basename. (We could potentially make sure the package
            // names match too, but it's unlikely that the class names match without a
            // package match, and there's no harm in including some extra classes here,
            // since lint will resolve full paths and the resource markers won't go
            // to the wrong place, we simply end up analyzing some extra files.)
            String className = mActiveFile.getName();
            if (name.regionMatches(0, className, 0, className.length() - DOT_JAVA.length())) {
                if (name.length() == className.length() - DOT_JAVA.length() + DOT_CLASS.length()
                        || name.charAt(className.length() - DOT_JAVA.length()) == '$') {
                    mFiles.add(resource);
                }
            }
        } else {
            IResourceDelta[] children = delta.getAffectedChildren();
            if (children != null && children.length > 0) {
                for (IResourceDelta d : children) {
                    gatherFiles(d);
                }
            }
        }
    }

    @Override
    public void run() {
        // Get the active file: this must be run on the GUI thread
        mActiveFile = AdtUtils.getActiveFile();
    }

    /**
     * Start listening to the resource monitor
     *
     * @param resourceMonitor the resource monitor
     */
    public static void startListening(@NonNull GlobalProjectMonitor resourceMonitor) {
        // Add a file listener which finds out when files have changed. This is listening
        // specifically for saves of Java files, in order to run incremental lint on them.
        // Note that the {@link PostCompilerBuilder} already handles incremental lint files
        // on Java files - and runs it for both the .java and .class files.
        //
        // However, if Project > Build Automatically is turned off, then the PostCompilerBuilder
        // isn't run after a save. THAT's what the below is for: it will run and *only*
        // run lint incrementally if build automatically is off.
        assert sListener == null; // Should only be called once on plugin activation
        sListener = new IFileListener() {
            @Override
            public void fileChanged(@NonNull IFile file,
                    @NonNull IMarkerDelta[] markerDeltas,
                    int kind, @Nullable String extension, int flags, boolean isAndroidProject) {
                if (!isAndroidProject || flags == IResourceDelta.MARKERS) {
                    // If not an Android project or ONLY the markers changed.
                    // Ignore these since they happen
                    // when we add markers for lint errors found in the current file,
                    // which would cause us to repeatedly enter this method over and over
                    // again.
                    return;
                }
                if (EXT_JAVA.equals(extension)
                        && !ResourceManager.isAutoBuilding()
                        && AdtPrefs.getPrefs().isLintOnSave()) {
                    LintDeltaProcessor.create().process(file);
                }
            }
        };
        resourceMonitor.addFileListener(sListener, IResourceDelta.ADDED | IResourceDelta.CHANGED);
    }

    /**
     * Stop listening to the resource monitor
     *
     * @param resourceMonitor the resource monitor
     */
    public static void stopListening(@NonNull GlobalProjectMonitor resourceMonitor) {
        assert sListener != null;
        resourceMonitor.removeFileListener(sListener);
        sListener = null;
    }

    private static IFileListener sListener;
}
