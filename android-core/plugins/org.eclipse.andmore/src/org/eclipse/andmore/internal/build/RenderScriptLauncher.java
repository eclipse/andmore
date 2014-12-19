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

import com.android.annotations.NonNull;
import com.android.sdklib.build.RenderScriptProcessor.CommandLineLauncher;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link SourceProcessor} for RenderScript files.
 */
public class RenderScriptLauncher implements CommandLineLauncher {

    /**
     * Single line llvm-rs-cc error: {@code <path>:<line>:<col>: <error>}
     */
    private static Pattern sLlvmPattern1 = Pattern.compile("^(.+?):(\\d+):(\\d+):\\s(.+)$"); //$NON-NLS-1$

    @NonNull
    private final IProject mProject;
    @NonNull
    private final IFolder mSourceOutFolder;
    @NonNull
    private final IFolder mResOutFolder;
    @NonNull
    private final IProgressMonitor mMonitor;
    private final boolean mVerbose;

    public RenderScriptLauncher(
            @NonNull IProject project,
            @NonNull IFolder sourceOutFolder,
            @NonNull IFolder resOutFolder,
            @NonNull IProgressMonitor monitor,
            boolean verbose) {
        mProject = project;
        mSourceOutFolder = sourceOutFolder;
        mResOutFolder = resOutFolder;
        mMonitor = monitor;
        mVerbose = verbose;
    }

    @Override
    public void launch(File executable, List<String> arguments, Map<String, String> envVariableMap)
            throws IOException, InterruptedException {
        // do the exec
        try {
            if (mVerbose) {
                StringBuilder sb = new StringBuilder(executable.getAbsolutePath());
                for (String c : arguments) {
                    sb.append(' ').append(c);
                }
                String cmd_line = sb.toString();
                AdtPlugin.printToConsole(mProject, cmd_line);
            }

            String[] commandArray = new String[1 + arguments.size()];
            commandArray[0] = executable.getAbsolutePath();
            System.arraycopy(arguments.toArray(), 0, commandArray, 1, arguments.size());

            ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
            Map<String, String> processEnvs = processBuilder.environment();
            for (Map.Entry<String, String> entry : envVariableMap.entrySet()) {
                processEnvs.put(entry.getKey(), entry.getValue());
            }

            Process p = processBuilder.start();

            // list to store each line of stderr
            ArrayList<String> stdErr = new ArrayList<String>();

            // get the output and return code from the process
            int returnCode = BuildHelper.grabProcessOutput(mProject, p, stdErr);

            if (stdErr.size() > 0) {
                // attempt to parse the error output
                boolean parsingError = parseLlvmOutput(stdErr);

                // If the process failed and we couldn't parse the output
                // we print a message, mark the project and exit
                if (returnCode != 0) {

                    if (parsingError || mVerbose) {
                        // display the message in the console.
                        if (parsingError) {
                            AdtPlugin.printErrorToConsole(mProject, stdErr.toArray());

                            // mark the project
                            BaseProjectHelper.markResource(mProject,
                                    AdtConstants.MARKER_RENDERSCRIPT,
                                    "Unparsed Renderscript error! Check the console for output.",
                                    IMarker.SEVERITY_ERROR);
                        } else {
                            AdtPlugin.printToConsole(mProject, stdErr.toArray());
                        }
                    }
                    return;
                }
            } else if (returnCode != 0) {
                // no stderr output but exec failed.
                String msg = String.format("Error executing Renderscript: Return code %1$d",
                        returnCode);

                BaseProjectHelper.markResource(mProject, AdtConstants.MARKER_AIDL,
                       msg, IMarker.SEVERITY_ERROR);

                return;
            }
        } catch (IOException e) {
            // mark the project and exit
            String msg = String.format(
                    "Error executing Renderscript. Please check %1$s is present at %2$s",
                    executable.getName(), executable.getAbsolutePath());
            AdtPlugin.log(IStatus.ERROR, msg);
            BaseProjectHelper.markResource(mProject, AdtConstants.MARKER_RENDERSCRIPT, msg,
                    IMarker.SEVERITY_ERROR);
            throw e;
        } catch (InterruptedException e) {
            // mark the project and exit
            String msg = String.format(
                    "Error executing Renderscript. Please check %1$s is present at %2$s",
                    executable.getName(), executable.getAbsolutePath());
            AdtPlugin.log(IStatus.ERROR, msg);
            BaseProjectHelper.markResource(mProject, AdtConstants.MARKER_RENDERSCRIPT, msg,
                    IMarker.SEVERITY_ERROR);
            throw e;
        }

        try {
            mSourceOutFolder.refreshLocal(IResource.DEPTH_ONE, mMonitor);
            mResOutFolder.refreshLocal(IResource.DEPTH_ONE, mMonitor);
        } catch (CoreException e) {
            AdtPlugin.log(e, "failed to refresh folders");
        }

        return;
    }

    /**
     * Parse the output of llvm-rs-cc and mark the file with any errors.
     * @param lines The output to parse.
     * @return true if the parsing failed, false if success.
     */
    private boolean parseLlvmOutput(ArrayList<String> lines) {
        // nothing to parse? just return false;
        if (lines.size() == 0) {
            return false;
        }

        // get the root folder for the project as we're going to ignore everything that's
        // not in the project
        String rootPath = mProject.getLocation().toOSString();
        int rootPathLength = rootPath.length();

        Matcher m;

        boolean parsing = false;

        for (int i = 0; i < lines.size(); i++) {
            String p = lines.get(i);

            m = sLlvmPattern1.matcher(p);
            if (m.matches()) {
                // get the file path. This may, or may not be the main file being compiled.
                String filePath = m.group(1);
                if (filePath.startsWith(rootPath) == false) {
                    // looks like the error in a non-project file. Keep parsing, but
                    // we'll return true
                    parsing = true;
                    continue;
                }

                // get the actual file.
                filePath = filePath.substring(rootPathLength);
                // remove starting separator since we want the path to be relative
                if (filePath.startsWith(File.separator)) {
                    filePath = filePath.substring(1);
                }

                // get the file
                IFile f = mProject.getFile(new Path(filePath));

                String lineStr = m.group(2);
                // ignore group 3 for now, this is the col number
                String msg = m.group(4);

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
                BaseProjectHelper.markResource(f, AdtConstants.MARKER_RENDERSCRIPT, msg, line,
                        IMarker.SEVERITY_ERROR);

                // success, go to the next line
                continue;
            }

            // invalid line format, flag as error, and keep going
            parsing = true;
        }

        return parsing;
    }
}
