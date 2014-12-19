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

package org.eclipse.andmore.internal.build;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceDelta;

import java.util.HashSet;
import java.util.Set;

/**
 * Base source change handler for the {@link SourceProcessor} classes.
 *
 * It can be used as is, as long as the matching {@link SourceProcessor} properly implements
 * its abstract methods, and the processor does not output resource files,
 * or can be extended to provide custom implementation for:
 * {@link #handleSourceFile(IFile, int)}
 * {@link #handleGeneratedFile(IFile, int)}
 * {@link #handleResourceFile(IFile, int)}
 * {@link #filterResourceFolder(IContainer)}
 *
 */
public class DefaultSourceChangeHandler implements SourceChangeHandler {

    private SourceProcessor mProcessor;

    /** List of source files found that are modified or new. */
    private final Set<IFile> mToCompile = new HashSet<IFile>();

    /** List of source files that have been removed. */
    private final Set<IFile> mRemoved = new HashSet<IFile>();

    @Override
    public boolean handleGeneratedFile(IFile file, int kind) {
        if (kind == IResourceDelta.REMOVED || kind == IResourceDelta.CHANGED) {
            IFile sourceFile = mProcessor.isOutput(file);
            if (sourceFile != null) {
                mToCompile.add(sourceFile);
                return true;
            }
        }

        return false;
    }

    @Override
    public void handleSourceFile(IFile file, int kind) {
        // first the file itself if this is a match for the processor's extension
        if (mProcessor.getExtensions().contains(file.getFileExtension())) {
            if (kind == IResourceDelta.REMOVED) {
                mRemoved.add(file);
            } else {
                mToCompile.add(file);
            }
        }

        // now the dependencies. In all case we compile the files that depend on the
        // added/changed/removed file.
        mToCompile.addAll(mProcessor.isDependency(file));
    }

    protected void addFileToCompile(IFile file) {
        mToCompile.add(file);
    }

    Set<IFile> getFilesToCompile() {
        return mToCompile;
    }

    protected void addRemovedFile(IFile file) {
        mRemoved.add(file);
    }

    Set<IFile> getRemovedFiles() {
        return mRemoved;
    }

    public void reset() {
        mToCompile.clear();
        mRemoved.clear();
    }

    protected SourceProcessor getProcessor() {
        return mProcessor;
    }

    void init(SourceProcessor processor) {
        mProcessor = processor;
    }
}
