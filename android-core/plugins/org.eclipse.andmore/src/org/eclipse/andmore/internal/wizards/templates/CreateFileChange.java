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

package org.eclipse.andmore.internal.wizards.templates;

import com.android.annotations.NonNull;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;

/** Change which lazily copies a file */
public class CreateFileChange extends ResourceChange {
    private String mName;
    private final IPath mPath;
    private final File mSource;

    CreateFileChange(@NonNull String name, @NonNull IPath workspacePath, File source) {
        mName = name;
        mPath = workspacePath;
        mSource = source;
    }

    @Override
    protected IResource getModifiedResource() {
      return ResourcesPlugin.getWorkspace().getRoot().getFile(mPath);
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        RefactoringStatus result = new RefactoringStatus();
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(mPath);
        URI location = file.getLocationURI();
        if (location == null) {
            result.addFatalError("Unknown location " + file.getFullPath().toString());
            return result;
        }
        return result;
    }

    @SuppressWarnings("resource") // Eclipse doesn't know about Guava's Closeables.closeQuietly
    @Override
    public Change perform(IProgressMonitor pm) throws CoreException {
        InputSupplier<FileInputStream> supplier = Files.newInputStreamSupplier(mSource);
        InputStream is = null;
        try {
            pm.beginTask("Creating file", 3);
            IFile file = (IFile) getModifiedResource();

            IContainer parent = file.getParent();
            if (parent != null && !parent.exists()) {
                IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(
                        parent.getFullPath());
                AdtUtils.ensureExists(folder);
            }

            is = supplier.getInput();
            file.create(is, false, new SubProgressMonitor(pm, 1));
            pm.worked(1);
        } catch (Exception ioe) {
            AdtPlugin.log(ioe, null);
        } finally {
            Closeables.closeQuietly(is);
            pm.done();
        }
        return null;
    }
}
