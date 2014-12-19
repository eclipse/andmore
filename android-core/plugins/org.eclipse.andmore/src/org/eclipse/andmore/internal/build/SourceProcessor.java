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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.internal.build.builders.BaseBuilder;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Base class to handle generated java code.
 *
 * It provides management for modified source file list, deleted source file list, reconciliation
 * of previous lists, storing the current state of the build.
 *
 */
public abstract class SourceProcessor {

    public final static int COMPILE_STATUS_NONE = 0;
    public final static int COMPILE_STATUS_CODE = 0x1;
    public final static int COMPILE_STATUS_RES = 0x2;

    /** List of all source files, their dependencies, and their output. */
    private final Map<IFile, SourceFileData> mFiles = new HashMap<IFile, SourceFileData>();

    private final IJavaProject mJavaProject;
    private BuildToolInfo mBuildToolInfo;
    private final IFolder mGenFolder;
    private final DefaultSourceChangeHandler mDeltaVisitor;

    /** List of source files pending compilation at the next build */
    private final List<IFile> mToCompile = new ArrayList<IFile>();

    /** List of removed source files pending cleaning at the next build. */
    private final List<IFile> mRemoved = new ArrayList<IFile>();

    private int mLastCompilationStatus = COMPILE_STATUS_NONE;

    /**
     * Quotes a path inside "". If the platform is not windows, the path is returned as is.
     * @param path the path to quote
     * @return the quoted path.
     */
    public static String quote(String path) {
        if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_WINDOWS) {
            if (path.endsWith(File.separator)) {
                path = path.substring(0, path.length() -1);
            }
            return "\"" + path + "\"";
        }

        return path;
    }

    protected SourceProcessor(
            @NonNull IJavaProject javaProject,
            @NonNull BuildToolInfo buildToolInfo,
            @NonNull IFolder genFolder,
            @NonNull DefaultSourceChangeHandler deltaVisitor) {
        mJavaProject = javaProject;
        mBuildToolInfo = buildToolInfo;
        mGenFolder = genFolder;
        mDeltaVisitor = deltaVisitor;

        mDeltaVisitor.init(this);

        IProject project = javaProject.getProject();

        // get all the source files
        buildSourceFileList();

        // load the known dependencies
        loadOutputAndDependencies();

        boolean mustCompile = loadState(project);

        // if we stored that we have to compile some files, we build the list that will compile them
        // all. For now we have to reuse the full list since we don't know which files needed
        // compilation.
        if (mustCompile) {
            mToCompile.addAll(mFiles.keySet());
        }
    }

    protected SourceProcessor(
            @NonNull IJavaProject javaProject,
            @NonNull BuildToolInfo buildToolInfo,
            @NonNull IFolder genFolder) {
        this(javaProject, buildToolInfo, genFolder, new DefaultSourceChangeHandler());
    }

    public void setBuildToolInfo(BuildToolInfo buildToolInfo) {
        mBuildToolInfo = buildToolInfo;
    }


    /**
     * Returns whether the given file is an output of this processor by return the source
     * file that generated it.
     * @param file the file to test.
     * @return the source file that generated the given file or null.
     */
    IFile isOutput(IFile file) {
        for (SourceFileData data : mFiles.values()) {
            if (data.generated(file)) {
                return data.getSourceFile();
            }
        }

        return null;
    }

    /**
     * Returns whether the given file is a dependency for other files by returning a list
     * of file depending on the given file.
     * @param file the file to test.
     * @return a list of files that depend on the given file or an empty list if there
     *    are no matches.
     */
    List<IFile> isDependency(IFile file) {
        ArrayList<IFile> files = new ArrayList<IFile>();
        for (SourceFileData data : mFiles.values()) {
            if (data.dependsOn(file)) {
                files.add(data.getSourceFile());
            }
        }

        return files;
    }

    void addData(SourceFileData data) {
        mFiles.put(data.getSourceFile(), data);
    }

    SourceFileData getFileData(IFile file) {
        return mFiles.get(file);
    }

    Collection<SourceFileData> getAllFileData() {
        return mFiles.values();
    }

    public final DefaultSourceChangeHandler getChangeHandler() {
        return mDeltaVisitor;
    }

    final IJavaProject getJavaProject() {
        return mJavaProject;
    }

    final BuildToolInfo getBuildToolInfo() {
        return mBuildToolInfo;
    }

    final IFolder getGenFolder() {
        return mGenFolder;
    }

    final List<IFile> getToCompile() {
        return mToCompile;
    }

    final List<IFile> getRemovedFile() {
        return mRemoved;
    }

    final void addFileToCompile(IFile file) {
        mToCompile.add(file);
    }

    public final void prepareFullBuild(IProject project) {
        mDeltaVisitor.reset();

        mToCompile.clear();
        mRemoved.clear();

        // get all the source files
        buildSourceFileList();

        mToCompile.addAll(mFiles.keySet());

        saveState(project);
    }

    public final void doneVisiting(IProject project) {
        // merge the previous file modification lists and the new one.
        mergeFileModifications(mDeltaVisitor);

        mDeltaVisitor.reset();

        saveState(project);
    }

    /**
     * Returns the extension of the source files handled by this processor.
     * @return
     */
    protected abstract Set<String> getExtensions();

    protected abstract String getSavePropertyName();

    /**
     * Compiles the source files and return a status bitmask of the type of file that was generated.
     *
     */
    public final int compileFiles(BaseBuilder builder,
            IProject project, IAndroidTarget projectTarget,
            List<IPath> sourceFolders, List<File> libraryProjectsOut, IProgressMonitor monitor)
            throws CoreException {

        mLastCompilationStatus = COMPILE_STATUS_NONE;

        if (mToCompile.size() == 0 && mRemoved.size() == 0) {
            return mLastCompilationStatus;
        }

        // if a source file is being removed before we managed to compile it, it'll be in
        // both list. We *need* to remove it from the compile list or it'll never go away.
        for (IFile sourceFile : mRemoved) {
            int pos = mToCompile.indexOf(sourceFile);
            if (pos != -1) {
                mToCompile.remove(pos);
            }
        }

        // list of files that have failed compilation.
        List<IFile> stillNeedCompilation = new ArrayList<IFile>();

        doCompileFiles(mToCompile, builder, project, projectTarget, sourceFolders,
                stillNeedCompilation, libraryProjectsOut, monitor);

        mToCompile.clear();
        mToCompile.addAll(stillNeedCompilation);

        // Remove the files created from source files that have been removed.
        for (IFile sourceFile : mRemoved) {
            // look if we already know the output
            SourceFileData data = getFileData(sourceFile);
            if (data != null) {
                doRemoveFiles(data);
            }
        }

        // remove the associated file data.
        for (IFile removedFile : mRemoved) {
            mFiles.remove(removedFile);
        }

        mRemoved.clear();

        // store the build state. If there are any files that failed to compile, we will
        // force a full aidl compile on the next project open. (unless a full compilation succeed
        // before the project is closed/re-opened.)
        saveState(project);

        return mLastCompilationStatus;
    }

    protected abstract void doCompileFiles(
            List<IFile> filesToCompile, BaseBuilder builder,
            IProject project, IAndroidTarget projectTarget,
            List<IPath> sourceFolders, List<IFile> notCompiledOut,
            List<File> libraryProjectsOut, IProgressMonitor monitor) throws CoreException;

    /**
     * Adds a compilation status. It can be any of (in combination too):
     * <p/>
     * {@link #COMPILE_STATUS_CODE} means this processor created source code files.
     * {@link #COMPILE_STATUS_RES} means this process created resources.
     */
    protected void setCompilationStatus(int status) {
        mLastCompilationStatus |= status;
    }

    protected void doRemoveFiles(SourceFileData data) throws CoreException {
        List<IFile> outputFiles = data.getOutputFiles();
        for (IFile outputFile : outputFiles) {
            if (outputFile.exists()) {
                outputFile.getLocation().toFile().delete();
            }
        }
    }

    public final boolean loadState(IProject project) {
        return ProjectHelper.loadBooleanProperty(project, getSavePropertyName(),
                true /*defaultValue*/);
    }

    public final void saveState(IProject project) {
        // TODO: Optimize by saving only the files that need compilation
        ProjectHelper.saveStringProperty(project, getSavePropertyName(),
                Boolean.toString(mToCompile.size() > 0));
    }

    protected abstract void loadOutputAndDependencies();


    protected IPath getSourceFolderFor(IFile file) {
        // find the source folder for the class so that we can infer the package from the
        // difference between the file and its source folder.
        List<IPath> sourceFolders = BaseProjectHelper.getSourceClasspaths(getJavaProject());
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        for (IPath sourceFolderPath : sourceFolders) {
            IFolder sourceFolder = root.getFolder(sourceFolderPath);
            // we don't look in the 'gen' source folder as there will be no source in there.
            if (sourceFolder.exists() && sourceFolder.equals(getGenFolder()) == false) {
                // look for the source file parent, until we find this source folder.
                IResource parent = file;
                while ((parent = parent.getParent()) != null) {
                    if (parent.equals(sourceFolder)) {
                        return sourceFolderPath;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Goes through the build paths and fills the list of files to compile.
     *
     * @param project The project.
     * @param sourceFolderPathList The list of source folder paths.
     */
    private final void buildSourceFileList() {
        mFiles.clear();

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        List<IPath> sourceFolderPathList = BaseProjectHelper.getSourceClasspaths(mJavaProject);

        for (IPath sourceFolderPath : sourceFolderPathList) {
            IFolder sourceFolder = root.getFolder(sourceFolderPath);
            // we don't look in the 'gen' source folder as there will be no source in there.
            if (sourceFolder.exists() && sourceFolder.equals(getGenFolder()) == false) {
                scanFolderForSourceFiles(sourceFolder, sourceFolder);
            }
        }
    }

    /**
     * Scans a folder and fills the list of files to compile.
     * @param sourceFolder the root source folder.
     * @param folder The folder to scan.
     */
    private void scanFolderForSourceFiles(IFolder sourceFolder, IFolder folder) {
        try {
            IResource[] members = folder.members();
            for (IResource r : members) {
                // get the type of the resource
               switch (r.getType()) {
                   case IResource.FILE: {
                       // if this a file, check that the file actually exist
                       // and that it's the type of of file that's used in this processor
                       String extension = r.exists() ? r.getFileExtension() : null;
                       if (extension != null &&
                               getExtensions().contains(extension.toLowerCase(Locale.US))) {
                           mFiles.put((IFile) r, new SourceFileData((IFile) r));
                       }
                       break;
                   }
                   case IResource.FOLDER:
                       // recursively go through children
                       scanFolderForSourceFiles(sourceFolder, (IFolder)r);
                       break;
                   default:
                       // this would mean it's a project or the workspace root
                       // which is unlikely to happen. we do nothing
                       break;
               }
            }
        } catch (CoreException e) {
            // Couldn't get the members list for some reason. Just return.
        }
    }


    /**
     * Merge the current list of source file to compile/remove with the one coming from the
     * delta visitor
     * @param visitor the delta visitor.
     */
    private void mergeFileModifications(DefaultSourceChangeHandler visitor) {
        Set<IFile> toRemove = visitor.getRemovedFiles();
        Set<IFile> toCompile = visitor.getFilesToCompile();

        // loop through the new toRemove list, and add it to the old one,
        // plus remove any file that was still to compile and that are now
        // removed
        for (IFile r : toRemove) {
            if (mRemoved.indexOf(r) == -1) {
                mRemoved.add(r);
            }

            int index = mToCompile.indexOf(r);
            if (index != -1) {
                mToCompile.remove(index);
            }
        }

        // now loop through the new files to compile and add it to the list.
        // Also look for them in the remove list, this would mean that they
        // were removed, then added back, and we shouldn't remove them, just
        // recompile them.
        for (IFile r : toCompile) {
            if (mToCompile.indexOf(r) == -1) {
                mToCompile.add(r);
            }

            int index = mRemoved.indexOf(r);
            if (index != -1) {
                mRemoved.remove(index);
            }
        }
    }
}
