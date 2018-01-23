/*******************************************************************************
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial contributors:
 * Thiago Faustini Junqueira (Eldorado)
 * 
 * Contributors:
 * Carlos Alberto Souto Junior (Eldorado) - [315122] Improvements in the Android NDK support UI
 * Carlos Alberto Souto Junior (Eldorado) - [317327] Major UI bugfixes and improvements in Android Native support
 *******************************************************************************/

package org.eclipse.sequoyah.android.cdt.build.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.android.cdt.internal.build.core.CorePlugin;
import org.eclipse.sequoyah.android.cdt.internal.build.core.TemplatedInputStream;

/**
 * Class that provide utility methods
 * @author Thiago Faustini Junqueira
 */
public class NDKUtils
{
    /**
     * The default name of the JNI folder.
     */
    public static final String DEFAULT_JNI_FOLDER_NAME = "jni";

    /**
     * The default name of the makefile.
     */
    public static final String MAKEFILE_FILE_NAME = "Android.mk";

    private static final String MAKEFILE_SOURCE_FILES_VARIABLE = "LOCAL_SRC_FILES :=";

    private static final String MAKEFILE_SOURCE_FILES_SEPARATOR = " ";

    private static final String MAKEFINE_NEW_LINE = "\n";

    /**
     * Auxiliary method to set the NDK location in the Sequoyah framework
     * @param location - NDK location
     */
    public static void setNDKLocation(String location)
    {
        INDKService ndkService = CorePlugin.getService(INDKService.class);
        ndkService.setNDKLocation(location);
    }

    /**
     * Auxiliary method to re-generate the Android makefile when needed.
     * @param project - Project that will contain the makefile
     * @param libname - Library name
     */
    public static void generateAndroidMakeFile(final IProject project, final String libName)
    {
        // The operation to do all the dirty work
        IWorkspaceRunnable op = new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                // Create the source folders, if necessary. 
                // Shouldn't be the case, but you never know what the user is up to.
                IFolder sourceFolder = project.getFolder(DEFAULT_JNI_FOLDER_NAME);

                if (!sourceFolder.exists())
                {
                    sourceFolder.create(true, true, monitor);
                }

                InputStream makefileIn = null;
                InputStream templateIn = null;

                try
                {
                    // Generate the Android.mk file
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("lib", libName);

                    URL makefileURL = CorePlugin.getFile(new Path("templates/Android.mk"));
                    makefileIn = makefileURL.openStream();
                    templateIn = new TemplatedInputStream(makefileIn, map);

                    IFile makefile = sourceFolder.getFile("Android.mk");

                    if (makefile.exists())
                    {
                        // Empty file contents because delete does not work properly
                        FileOutputStream outputStream =
                                new FileOutputStream(makefile.getLocation().toFile(), false);
                        outputStream.close();

                        // Append content
                        makefile.appendContents(templateIn, true, false, monitor);
                    }
                    else
                    {
                        // Create new makefile
                        makefile.create(templateIn, true, monitor);
                    }

                    // Refresh parent folder
                    makefile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);

                }
                catch (IOException e)
                {
                    throw new CoreException(new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID,
                            e.getLocalizedMessage(), e));
                }
                finally
                {

                    if (templateIn != null)
                    {
                        try
                        {
                            templateIn.close();
                        }
                        catch (IOException e)
                        {
                            // Do nothing
                        }
                    }
                }

            }
        };

        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        try
        {
            workspace.run(op, null, IWorkspace.AVOID_UPDATE, null);
        }
        catch (CoreException e)
        {
            CorePlugin.getDefault().getLog().log(e.getStatus());
        }
    }

    /**
     * Adds the given source file name to the list of source files on
     * the given makefile.
     * This implementation assumes there is only one list of source files
     * on the makefile.
     * 
     * @param makefile The makefile to have the source files list updated
     * @param srcFileName The name of the new source file
     * 
     * @throws IOException If any problem occurs reading/writing the makefile
     */
    public static void addSourceFileToMakefile(IResource makefile, String srcFileName)
            throws IOException
    {
        IPath makefilePath = makefile.getLocation();
        File makefileFile = makefilePath.toFile();

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try
        {
            // read from original makefile
            reader = new BufferedReader(new FileReader(makefileFile));
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith(MAKEFILE_SOURCE_FILES_VARIABLE))
                {
                    // append given source file name to the list of source files
                    line = line.concat(MAKEFILE_SOURCE_FILES_SEPARATOR + srcFileName);
                }
                sb.append(line + MAKEFINE_NEW_LINE);
            }
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (IOException e)
            {
                // do nothing; only prevent errors
            }
        }

        try
        {
            // write new content to makefile
            String fileText = sb.toString();
            writer = new BufferedWriter(new FileWriter(makefileFile));
            writer.write(fileText);
        }
        finally
        {
            try
            {
                if (writer != null)
                {
                    writer.close();
                }
            }
            catch (IOException e)
            {
                // do nothing; only prevent errors
            }
        }
    }
}
