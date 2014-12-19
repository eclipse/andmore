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

import org.eclipse.core.resources.IFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data for Android-specific source files. It contains a list of output files and a list
 * of dependencies.
 * The source file itself is a implied dependency and is not meant to be in the dependency list.
 */
public class SourceFileData {

    private final IFile mSourceFile;
    private final List<IFile> mOutputFiles = new ArrayList<IFile>();
    private final List<IFile> mDependencyFiles = new ArrayList<IFile>();

    public SourceFileData(IFile sourceFile) {
        this(sourceFile, null, null);
    }

    SourceFileData(IFile sourceFile,
            List<IFile> outputFiles, List<IFile> dependencyFiles) {
        mSourceFile = sourceFile;
        if (outputFiles != null) {
            mOutputFiles.addAll(outputFiles);
        }
        if (dependencyFiles != null) {
            mDependencyFiles.addAll(dependencyFiles);
        }
    }

    SourceFileData(IFile sourceFile, IFile outputFile) {
        mSourceFile = sourceFile;
        if (outputFile != null) {
            mOutputFiles.add(outputFile);
        }
    }

    /**
     * Returns the source file as an {@link IFile}
     */
    public IFile getSourceFile() {
        return mSourceFile;
    }

    /**
     * Returns whether the given file is a dependency for this source file.
     * <p/>Note that the source file itself is not tested against. Therefore if
     * {@code file.equals(getSourceFile()} returns {@code true}, this method will return
     * {@code false}.
     * @param file the file to check against
     * @return true if the given file is a dependency for this source file.
     */
    public boolean dependsOn(IFile file) {
        return mDependencyFiles.contains(file);
    }

    /**
     * Returns whether the given file is an ouput of this source file.
     * @param file the file to test.
     * @return true if the file is an output file.
     */
    public boolean generated(IFile file) {
        return mOutputFiles.contains(file);
    }

    void setOutputFiles(List<IFile> outputFiles) {
        mOutputFiles.clear();
        if (outputFiles != null) {
            mOutputFiles.addAll(outputFiles);
        }
    }

    void setOutputFile(IFile outputFile) {
        mOutputFiles.clear();
        if (outputFile != null) {
            mOutputFiles.add(outputFile);
        }
    }

    void setDependencyFiles(List<IFile> depFiles) {
        mDependencyFiles.clear();
        if (depFiles != null) {
            mDependencyFiles.addAll(depFiles);
        }
    }

    public List<IFile> getDependencyFiles() {
        return mDependencyFiles;
    }

    /**
     * Shortcut access to the first output file. This is useful for generator that only output
     * one file.
     */
    public IFile getOutput() {
        if (mOutputFiles.size() > 0) {
            return mOutputFiles.get(0);
        }

        return null;
    }

    public List<IFile> getOutputFiles() {
        return Collections.unmodifiableList(mOutputFiles);
    }

    @Override
    public String toString() {
        return "NonJavaFileBundle [mSourceFile=" + mSourceFile + ", mGeneratedFiles="
                + mOutputFiles + ", mDependencies=" + mDependencyFiles + "]";
    }
}
