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
import com.android.repository.io.FileOpUtils;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.repository.io.FileOp;
import com.android.repository.io.FileOpUtils;

import org.eclipse.andmore.AndmoreAndroidConstants;
import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.build.builders.BaseBuilder;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.preferences.AdtPrefs.BuildVerbosity;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link SourceProcessor} for aidl files.
 *
 */
public class AidlProcessor extends SourceProcessor {

    private static final String PROPERTY_COMPILE_AIDL = "compileAidl"; //$NON-NLS-1$

    /**
     * Single line aidl error<br>
     * {@code <path>:<line>: <error>}<br>
     * or<br>
     * {@code <path>:<line> <error>}<br>
     */
    private static Pattern sAidlPattern1 = Pattern.compile("^(.+?):(\\d+):?\\s(.+)$"); //$NON-NLS-1$

    private final static Set<String> EXTENSIONS = Collections.singleton(SdkConstants.EXT_AIDL);

    private enum AidlType {
        UNKNOWN, INTERFACE, PARCELABLE;
    }

    // See comment in #getAidlType()
//  private final static Pattern sParcelablePattern = Pattern.compile(
//          "^\\s*parcelable\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*;\\s*$");
//
//  private final static Pattern sInterfacePattern = Pattern.compile(
//          "^\\s*interface\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:\\{.*)?$");


    public AidlProcessor(@NonNull IJavaProject javaProject, @NonNull BuildToolInfo buildToolInfo,
            @NonNull IFolder genFolder) {
        super(javaProject, buildToolInfo, genFolder);
    }

    @Override
    protected Set<String> getExtensions() {
        return EXTENSIONS;
    }

    @Override
    protected String getSavePropertyName() {
        return PROPERTY_COMPILE_AIDL;
    }

    @Override
    protected void doCompileFiles(List<IFile> sources, BaseBuilder builder,
            IProject project, IAndroidTarget projectTarget,
            List<IPath> sourceFolders, List<IFile> notCompiledOut, List<File> libraryProjectsOut,
            IProgressMonitor monitor) throws CoreException {
        // create the command line
        List<String> commandList = new ArrayList<String>(
                4 + sourceFolders.size() + libraryProjectsOut.size());
        commandList.add(getBuildToolInfo().getPath(BuildToolInfo.PathId.AIDL));
        commandList.add(quote("-p" + projectTarget.getPath(IAndroidTarget.ANDROID_AIDL))); //$NON-NLS-1$

        // since the path are relative to the workspace and not the project itself, we need
        // the workspace root.
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
        for (IPath p : sourceFolders) {
            IFolder f = wsRoot.getFolder(p);
            if (f.exists()) { // if the resource doesn't exist, getLocation will return null.
                commandList.add(quote("-I" + f.getLocation().toOSString())); //$NON-NLS-1$
            }
        }

        for (File libOut : libraryProjectsOut) {
            // FIXME: make folder configurable
            File aidlFile = new File(libOut, SdkConstants.FD_AIDL);
            if (aidlFile.isDirectory()) {
                commandList.add(quote("-I" + aidlFile.getAbsolutePath())); //$NON-NLS-1$
            }
        }

        // convert to array with 2 extra strings for the in/out file paths.
        int index = commandList.size();
        String[] commands = commandList.toArray(new String[index + 2]);

        boolean verbose = AdtPrefs.getPrefs().getBuildVerbosity() == BuildVerbosity.VERBOSE;

        // remove the generic marker from the project
        builder.removeMarkersFromResource(project, AndmoreAndroidConstants.MARKER_AIDL);

        // prepare the two output folders.
        IFolder genFolder = getGenFolder();
        IFolder projectOut = BaseProjectHelper.getAndroidOutputFolder(project);
        IFolder aidlOutFolder = projectOut.getFolder(SdkConstants.FD_AIDL);
        if (aidlOutFolder.exists() == false) {
            aidlOutFolder.create(true /*force*/, true /*local*/,
                    new SubProgressMonitor(monitor, 10));
        }

        boolean success = false;

        // loop until we've compile them all
        for (IFile sourceFile : sources) {
            if (verbose) {
                String name = sourceFile.getName();
                IPath sourceFolderPath = getSourceFolderFor(sourceFile);
                if (sourceFolderPath != null) {
                    // make a path to the source file relative to the source folder.
                    IPath relative = sourceFile.getFullPath().makeRelativeTo(sourceFolderPath);
                    name = relative.toString();
                }
                AndmoreAndroidPlugin.printToConsole(project, "AIDL: " + name);
            }

            // Remove the AIDL error markers from the aidl file
            builder.removeMarkersFromResource(sourceFile, AndmoreAndroidConstants.MARKER_AIDL);

            // get the path of the source file.
            IPath sourcePath = sourceFile.getLocation();
            String osSourcePath = sourcePath.toOSString();

            // look if we already know the output
            SourceFileData data = getFileData(sourceFile);
            if (data == null) {
                data = new SourceFileData(sourceFile);
                addData(data);
            }

            // if there's no output file yet, compute it.
            if (data.getOutput() == null) {
                IFile javaFile = getAidlOutputFile(sourceFile, genFolder,
                        true /*replaceExt*/, true /*createFolders*/, monitor);
                data.setOutputFile(javaFile);
            }

            // finish to set the command line.
            commands[index] = quote(osSourcePath);
            commands[index + 1] = quote(data.getOutput().getLocation().toOSString());

            // launch the process
            if (execAidl(builder, project, commands, sourceFile, verbose) == false) {
                // aidl failed. File should be marked. We add the file to the list
                // of file that will need compilation again.
                notCompiledOut.add(sourceFile);
            } else {
                // Success. we'll return that we generated code
                setCompilationStatus(COMPILE_STATUS_CODE);
                success = true;

                // Also copy the file to the bin folder.
                IFile aidlOutFile = getAidlOutputFile(sourceFile, aidlOutFolder,
                        false /*replaceExt*/, true /*createFolders*/, monitor);

                FileOp op = FileOpUtils.create();
                try {
                    op.copyFile(sourceFile.getLocation().toFile(),
                            aidlOutFile.getLocation().toFile());
                } catch (IOException e) {
                }
            }
        }

        if (success) {
            aidlOutFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
    }

    @Override
    protected void loadOutputAndDependencies() {
        IProgressMonitor monitor = new NullProgressMonitor();
        IFolder genFolder = getGenFolder();

        Collection<SourceFileData> dataList = getAllFileData();
        for (SourceFileData data : dataList) {
            try {
                IFile javaFile = getAidlOutputFile(data.getSourceFile(), genFolder,
                        true /*replaceExt*/, false /*createFolders*/, monitor);
                data.setOutputFile(javaFile);
            } catch (CoreException e) {
                // ignore, we're not asking to create the folder so this won't happen anyway.
            }

        }
    }

    /**
     * Execute the aidl command line, parse the output, and mark the aidl file
     * with any reported errors.
     * @param command the String array containing the command line to execute.
     * @param file The IFile object representing the aidl file being
     *      compiled.
     * @param verbose the build verbosity
     * @return false if the exec failed, and build needs to be aborted.
     */
    private boolean execAidl(BaseBuilder builder, IProject project, String[] command, IFile file,
            boolean verbose) {
        // do the exec
        try {
            if (verbose) {
                StringBuilder sb = new StringBuilder();
                for (String c : command) {
                    sb.append(c);
                    sb.append(' ');
                }
                String cmd_line = sb.toString();
                AndmoreAndroidPlugin.printToConsole(project, cmd_line);
            }

            Process p = Runtime.getRuntime().exec(command);

            // list to store each line of stderr
            ArrayList<String> stdErr = new ArrayList<String>();

            // get the output and return code from the process
            int returnCode = BuildHelper.grabProcessOutput(project, p, stdErr);

            if (stdErr.size() > 0) {
                // attempt to parse the error output
                boolean parsingError = parseAidlOutput(stdErr, file);

                // If the process failed and we couldn't parse the output
                // we print a message, mark the project and exit
                if (returnCode != 0) {

                    if (parsingError || verbose) {
                        // display the message in the console.
                        if (parsingError) {
                            AndmoreAndroidPlugin.printErrorToConsole(project, stdErr.toArray());

                            // mark the project
                            BaseProjectHelper.markResource(project, AndmoreAndroidConstants.MARKER_AIDL,
                                    Messages.Unparsed_AIDL_Errors, IMarker.SEVERITY_ERROR);
                        } else {
                            AndmoreAndroidPlugin.printToConsole(project, stdErr.toArray());
                        }
                    }
                    return false;
                }
            } else if (returnCode != 0) {
                // no stderr output but exec failed.
                String msg = String.format(Messages.AIDL_Exec_Error_d, returnCode);

                BaseProjectHelper.markResource(project, AndmoreAndroidConstants.MARKER_AIDL,
                       msg, IMarker.SEVERITY_ERROR);

                return false;
            }
        } catch (IOException e) {
            // mark the project and exit
            String msg = String.format(Messages.AIDL_Exec_Error_s, command[0]);
            BaseProjectHelper.markResource(project, AndmoreAndroidConstants.MARKER_AIDL, msg,
                    IMarker.SEVERITY_ERROR);
            return false;
        } catch (InterruptedException e) {
            // mark the project and exit
            String msg = String.format(Messages.AIDL_Exec_Error_s, command[0]);
            BaseProjectHelper.markResource(project, AndmoreAndroidConstants.MARKER_AIDL, msg,
                    IMarker.SEVERITY_ERROR);
            return false;
        }

        return true;
    }

    /**
     * Parse the output of aidl and mark the file with any errors.
     * @param lines The output to parse.
     * @param file The file to mark with error.
     * @return true if the parsing failed, false if success.
     */
    private boolean parseAidlOutput(ArrayList<String> lines, IFile file) {
        // nothing to parse? just return false;
        if (lines.size() == 0) {
            return false;
        }

        Matcher m;

        for (int i = 0; i < lines.size(); i++) {
            String p = lines.get(i);

            m = sAidlPattern1.matcher(p);
            if (m.matches()) {
                // we can ignore group 1 which is the location since we already
                // have a IFile object representing the aidl file.
                String lineStr = m.group(2);
                String msg = m.group(3);

                // get the line number
                int line = 0;
                try {
                    line = Integer.parseInt(lineStr);
                } catch (NumberFormatException e) {
                    // looks like the string we extracted wasn't a valid
                    // file number. Parsing failed and we return true
                    return true;
                }

                // mark the file
                BaseProjectHelper.markResource(file, AndmoreAndroidConstants.MARKER_AIDL, msg, line,
                        IMarker.SEVERITY_ERROR);

                // success, go to the next line
                continue;
            }

            // invalid line format, flag as error, and bail
            return true;
        }

        return false;
    }

    /**
     * Returns the {@link IFile} handle to the destination file for a given aidl source file
     * ({@link AidlData}).
     * @param sourceFile The source file
     * @param outputFolder the top level output folder (not including the package folders)
     * @param createFolders whether or not the parent folder of the destination should be created
     * if it does not exist.
     * @param monitor the progress monitor
     * @return the handle to the destination file.
     * @throws CoreException
     */
    private IFile getAidlOutputFile(IFile sourceFile, IFolder outputFolder, boolean replaceExt,
            boolean createFolders, IProgressMonitor monitor) throws CoreException {

        IPath sourceFolderPath = getSourceFolderFor(sourceFile);

        // this really shouldn't happen since the sourceFile must be in a source folder
        // since it comes from the delta visitor
        if (sourceFolderPath != null) {
            // make a path to the source file relative to the source folder.
            IPath relative = sourceFile.getFullPath().makeRelativeTo(sourceFolderPath);
            // remove the file name. This is now the destination folder.
            relative = relative.removeLastSegments(1);

            // get an IFolder for this path.
            IFolder destinationFolder = outputFolder.getFolder(relative);

            // create it if needed.
            if (destinationFolder.exists() == false && createFolders) {
                createFolder(destinationFolder, monitor);
            }

            // Build the Java file name from the aidl name.
            String javaName;
            if (replaceExt) {
                javaName = sourceFile.getName().replaceAll(
                        AndmoreAndroidConstants.RE_AIDL_EXT, SdkConstants.DOT_JAVA);
            } else {
                javaName = sourceFile.getName();
            }

            // get the resource for the java file.
            IFile javaFile = destinationFolder.getFile(javaName);
            return javaFile;
        }

        return null;
    }

    /**
     * Creates the destination folder. Because
     * {@link IFolder#create(boolean, boolean, IProgressMonitor)} only works if the parent folder
     * already exists, this goes and ensure that all the parent folders actually exist, or it
     * creates them as well.
     * @param destinationFolder The folder to create
     * @param monitor the {@link IProgressMonitor},
     * @throws CoreException
     */
    private void createFolder(IFolder destinationFolder, IProgressMonitor monitor)
            throws CoreException {

        // check the parent exist and create if necessary.
        IContainer parent = destinationFolder.getParent();
        if (parent.getType() == IResource.FOLDER && parent.exists() == false) {
            createFolder((IFolder)parent, monitor);
        }

        // create the folder.
        destinationFolder.create(true /*force*/, true /*local*/,
                new SubProgressMonitor(monitor, 10));
    }

    /**
     * Returns the type of the aidl file. Aidl files can either declare interfaces, or declare
     * parcelables. This method will attempt to parse the file and return the type. If the type
     * cannot be determined, then it will return {@link AidlType#UNKNOWN}.
     * @param file The aidl file
     * @return the type of the aidl.
     */
    private static AidlType getAidlType(IFile file) {
        // At this time, parsing isn't available, so we return UNKNOWN. This will force
        // a recompilation of all aidl file as soon as one is changed.
        return AidlType.UNKNOWN;

        // TODO: properly parse aidl file to determine type and generate dependency graphs.
//
//        String className = file.getName().substring(0,
//                file.getName().length() - SdkConstants.DOT_AIDL.length());
//
//        InputStream input = file.getContents(true /* force*/);
//        try {
//            BufferedReader reader = new BufferedReader(new InputStreateader(input));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                if (line.length() == 0) {
//                    continue;
//                }
//
//                Matcher m = sParcelablePattern.matcher(line);
//                if (m.matches() && m.group(1).equals(className)) {
//                    return AidlType.PARCELABLE;
//                }
//
//                m = sInterfacePattern.matcher(line);
//                if (m.matches() && m.group(1).equals(className)) {
//                    return AidlType.INTERFACE;
//                }
//            }
//        } catch (IOException e) {
//            throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
//                    "Error parsing aidl file", e));
//        } finally {
//            try {
//                input.close();
//            } catch (IOException e) {
//                throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
//                        "Error parsing aidl file", e));
//            }
//        }
//
//        return AidlType.UNKNOWN;
    }
}
