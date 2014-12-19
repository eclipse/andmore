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
package org.eclipse.andmore.internal.editors.layout.gle2;

import com.android.annotations.NonNull;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.resources.ResourceFolderType;
import com.google.common.base.Charsets;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationChooser;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/** Job which creates a new layout file for a given configuration */
class CreateNewConfigJob extends Job {
    private final GraphicalEditorPart mEditor;
    private final IFile mFromFile;
    private final FolderConfiguration mConfig;

    CreateNewConfigJob(
            @NonNull GraphicalEditorPart editor,
            @NonNull IFile fromFile,
            @NonNull FolderConfiguration config) {
        super("Create Alternate Layout");
        mEditor = editor;
        mFromFile = fromFile;
        mConfig = config;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        // get the folder name
        String folderName = mConfig.getFolderName(ResourceFolderType.LAYOUT);
        try {
            // look to see if it exists.
            // get the res folder
            IFolder res = (IFolder) mFromFile.getParent().getParent();

            IFolder newParentFolder = res.getFolder(folderName);
            AdtUtils.ensureExists(newParentFolder);
            final IFile file = newParentFolder.getFile(mFromFile.getName());
            if (file.exists()) {
                String message = String.format("File 'res/%1$s/%2$s' already exists!",
                        folderName, mFromFile.getName());
                return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, message);
            }

            // Read current document contents instead of from file: mFromFile.getContents()
            String text = mEditor.getEditorDelegate().getEditor().getStructuredDocument().get();
            ByteArrayInputStream input = new ByteArrayInputStream(text.getBytes(Charsets.UTF_8));
            file.create(input, false, monitor);
            input.close();

            // Ensure that the project resources updates itself to notice the new configuration.
            // In theory, this shouldn't be necessary, but we need to make sure the
            // resource manager knows about this immediately such that the call below
            // to find the best configuration takes the new folder into account.
            ResourceManager resourceManager = ResourceManager.getInstance();
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IFolder folder = root.getFolder(newParentFolder.getFullPath());
            resourceManager.getResourceFolder(folder);

            // Switch to the new file
            Display display = mEditor.getConfigurationChooser().getDisplay();
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    // The given old layout has been forked into a new layout
                    // for a given configuration. This means that the old layout
                    // is no longer a match for the configuration, which is
                    // probably what it is still showing. We have to modify
                    // its configuration to no longer be an impossible
                    // configuration.
                    ConfigurationChooser chooser = mEditor.getConfigurationChooser();
                    chooser.onAlternateLayoutCreated();

                    // Finally open the new layout
                    try {
                        AdtPlugin.openFile(file, null, false);
                    } catch (PartInitException e) {
                        AdtPlugin.log(e, null);
                    }
                }
            });
        } catch (IOException e2) {
            String message = String.format(
                    "Failed to create File 'res/%1$s/%2$s' : %3$s",
                    folderName, mFromFile.getName(), e2.getMessage());
            AdtPlugin.displayError("Layout Creation", message);

            return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                    message, e2);
        } catch (CoreException e2) {
            String message = String.format(
                    "Failed to create File 'res/%1$s/%2$s' : %3$s",
                    folderName, mFromFile.getName(), e2.getMessage());
            AdtPlugin.displayError("Layout Creation", message);

            return e2.getStatus();
        }

        return Status.OK_STATUS;
    }
}
