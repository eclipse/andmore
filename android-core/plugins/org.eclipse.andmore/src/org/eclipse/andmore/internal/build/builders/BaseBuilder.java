/*
 * Copyright (C) 2007 The Android Open Source Project
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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.sdk.LoadStatus;
import com.android.io.IAbstractFile;
import com.android.io.StreamException;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import org.eclipse.andmore.AndmoreAndroidConstants;
import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.build.BuildHelper;
import org.eclipse.andmore.internal.build.Messages;
import org.eclipse.andmore.internal.preferences.AdtPrefs.BuildVerbosity;
import org.eclipse.andmore.internal.project.BaseProjectHelper;
import org.eclipse.andmore.internal.project.ProjectHelper;
import org.eclipse.andmore.internal.project.XmlErrorHandler;
import org.eclipse.andmore.internal.project.XmlErrorHandler.XmlErrorListener;
import org.eclipse.andmore.internal.sdk.ProjectState;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.andmore.io.IFileWrapper;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.xml.sax.SAXException;

import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Base builder for XML files. This class allows for basic XML parsing with
 * error checking and marking the files for errors/warnings.
 */
public abstract class BaseBuilder extends IncrementalProjectBuilder {

    protected static final boolean DEBUG_LOG = "1".equals(              //$NON-NLS-1$
            System.getenv("ANDROID_BUILD_DEBUG"));                      //$NON-NLS-1$

    /** SAX Parser factory. */
    private SAXParserFactory mParserFactory;

    /**
     * The build tool to use to build. This is guaranteed to be non null after a call to
     * {@link #abortOnBadSetup(IJavaProject, ProjectState)} since this will throw if it can't be
     * queried.
     */
    protected BuildToolInfo mBuildToolInfo;

    /**
     * Base Resource Delta Visitor to handle XML error
     */
    protected static class BaseDeltaVisitor implements XmlErrorListener {

        /** The Xml builder used to validate XML correctness. */
        protected BaseBuilder mBuilder;

        /**
         * XML error flag. if true, we keep parsing the ResourceDelta but the
         * compilation will not happen (we're putting markers)
         */
        public boolean mXmlError = false;

        public BaseDeltaVisitor(BaseBuilder builder) {
            mBuilder = builder;
        }

        /**
         * Finds a matching Source folder for the current path. This checks if the current path
         * leads to, or is a source folder.
         * @param sourceFolders The list of source folders
         * @param pathSegments The segments of the current path
         * @return The segments of the source folder, or null if no match was found
         */
        protected static String[] findMatchingSourceFolder(ArrayList<IPath> sourceFolders,
                String[] pathSegments) {

            for (IPath p : sourceFolders) {
                // check if we are inside one of those source class path

                // get the segments
                String[] srcSegments = p.segments();

                // compare segments. We want the path of the resource
                // we're visiting to be
                boolean valid = true;
                int segmentCount = pathSegments.length;

                for (int i = 0 ; i < segmentCount; i++) {
                    String s1 = pathSegments[i];
                    String s2 = srcSegments[i];

                    if (s1.equalsIgnoreCase(s2) == false) {
                        valid = false;
                        break;
                    }
                }

                if (valid) {
                    // this folder, or one of this children is a source
                    // folder!
                    // we return its segments
                    return srcSegments;
                }
            }

            return null;
        }

        /**
         * Sent when an XML error is detected.
         * @see XmlErrorListener
         */
        @Override
        public void errorFound() {
            mXmlError = true;
        }
    }

    protected static class AbortBuildException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public BaseBuilder() {
        super();
        mParserFactory = SAXParserFactory.newInstance();

        // FIXME when the compiled XML support for namespace is in, set this to true.
        mParserFactory.setNamespaceAware(false);
    }

    /**
     * Checks an Xml file for validity. Errors/warnings will be marked on the
     * file
     * @param resource the resource to check
     * @param visitor a valid resource delta visitor
     */
    protected final void checkXML(IResource resource, BaseDeltaVisitor visitor) {

        // first make sure this is an xml file
        if (resource instanceof IFile) {
            IFile file = (IFile)resource;

            // remove previous markers
            removeMarkersFromResource(file, AndmoreAndroidConstants.MARKER_XML);

            // create  the error handler
            XmlErrorHandler reporter = new XmlErrorHandler(file, visitor);
            try {
                // parse
                getParser().parse(file.getContents(), reporter);
            } catch (Exception e1) {
            }
        }
    }

    /**
     * Returns the SAXParserFactory, instantiating it first if it's not already
     * created.
     * @return the SAXParserFactory object
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    protected final SAXParser getParser() throws ParserConfigurationException,
            SAXException {
        return mParserFactory.newSAXParser();
    }

    /**
     * Adds a marker to the current project.  This methods catches thrown {@link CoreException},
     * and returns null instead.
     *
     * @param markerId The id of the marker to add.
     * @param message the message associated with the mark
     * @param severity the severity of the marker.
     * @return the marker that was created (or null if failure)
     * @see IMarker
     */
    protected final IMarker markProject(String markerId, String message, int severity) {
        return BaseProjectHelper.markResource(getProject(), markerId, message, severity);
    }

    /**
     * Removes markers from a resource and only the resource (not its children).
     * @param file The file from which to delete the markers.
     * @param markerId The id of the markers to remove. If null, all marker of
     * type <code>IMarker.PROBLEM</code> will be removed.
     */
    public final void removeMarkersFromResource(IResource resource, String markerId) {
        try {
            if (resource.exists()) {
                resource.deleteMarkers(markerId, true, IResource.DEPTH_ZERO);
            }
        } catch (CoreException ce) {
            String msg = String.format(Messages.Marker_Delete_Error, markerId, resource.toString());
            AndmoreAndroidPlugin.printErrorToConsole(getProject(), msg);
        }
    }

    /**
     * Removes markers from a container and its children.
     * @param folder The container from which to delete the markers.
     * @param markerId The id of the markers to remove. If null, all marker of
     * type <code>IMarker.PROBLEM</code> will be removed.
     */
    protected final void removeMarkersFromContainer(IContainer folder, String markerId) {
        try {
            if (folder.exists()) {
                folder.deleteMarkers(markerId, true, IResource.DEPTH_INFINITE);
            }
        } catch (CoreException ce) {
            String msg = String.format(Messages.Marker_Delete_Error, markerId, folder.toString());
            AndmoreAndroidPlugin.printErrorToConsole(getProject(), msg);
        }
    }

    /**
     * Get the stderr output of a process and return when the process is done.
     * @param process The process to get the ouput from
     * @param stdErr The array to store the stderr output
     * @return the process return code.
     * @throws InterruptedException
     */
    protected final int grabProcessOutput(final Process process,
            final ArrayList<String> stdErr) throws InterruptedException {
        return BuildHelper.grabProcessOutput(getProject(), process, stdErr);
    }



    /**
     * Saves a String property into the persistent storage of the project.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param value the value to save
     * @return true if the save succeeded.
     */
    protected boolean saveProjectStringProperty(String propertyName, String value) {
        IProject project = getProject();
        return ProjectHelper.saveStringProperty(project, propertyName, value);
    }


    /**
     * Loads a String property from the persistent storage of the project.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @return the property value or null if it was not found.
     */
    protected String loadProjectStringProperty(String propertyName) {
        IProject project = getProject();
        return ProjectHelper.loadStringProperty(project, propertyName);
    }

    /**
     * Saves a property into the persistent storage of the project.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param value the value to save
     * @return true if the save succeeded.
     */
    protected boolean saveProjectBooleanProperty(String propertyName, boolean value) {
        IProject project = getProject();
        return ProjectHelper.saveStringProperty(project, propertyName, Boolean.toString(value));
    }

    /**
     * Loads a boolean property from the persistent storage of the project.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param defaultValue The default value to return if the property was not found.
     * @return the property value or the default value if the property was not found.
     */
    protected boolean loadProjectBooleanProperty(String propertyName, boolean defaultValue) {
        IProject project = getProject();
        return ProjectHelper.loadBooleanProperty(project, propertyName, defaultValue);
    }

    /**
     * Aborts the build if the SDK/project setups are broken. This does not
     * display any errors.
     *
     * @param javaProject The {@link IJavaProject} being compiled.
     * @param projectState the project state, optional. will be queried if null.
     * @throws CoreException
     */
    protected void abortOnBadSetup(@NonNull IJavaProject javaProject,
            @Nullable ProjectState projectState) throws AbortBuildException, CoreException {
        IProject iProject = javaProject.getProject();
        // check if we have finished loading the project target.
        Sdk sdk = Sdk.getCurrent();
        if (sdk == null) {
            throw new AbortBuildException();
        }

        if (projectState == null) {
            projectState = Sdk.getProjectState(javaProject.getProject());
        }

        if (projectState == null) {
            throw new AbortBuildException();
        }
        // get the target for the project
        IAndroidTarget target = projectState.getTarget();

        if (target == null) {
            throw new AbortBuildException();
        }

        // check on the target data.
        if (sdk.checkAndLoadTargetData(target, javaProject) != LoadStatus.LOADED) {
            throw new AbortBuildException();
       }

        mBuildToolInfo = projectState.getBuildToolInfo();
        if (mBuildToolInfo == null) {
            mBuildToolInfo = sdk.getLatestBuildTool();

            if (mBuildToolInfo == null) {
                AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, iProject,
                        "No \"Build Tools\" package available; use SDK Manager to install one.");
                throw new AbortBuildException();
            } else {
                AndmoreAndroidPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, iProject,
                        String.format("Using default Build Tools revision %s",
                                mBuildToolInfo.getRevision())
                        );
            }
        }

        // abort if there are TARGET or ADT type markers
        stopOnMarker(iProject, AndmoreAndroidConstants.MARKER_TARGET, IResource.DEPTH_ZERO,
                false /*checkSeverity*/);
        stopOnMarker(iProject, AndmoreAndroidConstants.MARKER_ADT, IResource.DEPTH_ZERO,
                false /*checkSeverity*/);
    }

    protected void stopOnMarker(IProject project, String markerType, int depth,
            boolean checkSeverity)
            throws AbortBuildException {
        try {
            IMarker[] markers = project.findMarkers(markerType, false /*includeSubtypes*/, depth);

            if (markers.length > 0) {
                if (checkSeverity == false) {
                    throw new AbortBuildException();
                } else {
                    for (IMarker marker : markers) {
                        int severity = marker.getAttribute(IMarker.SEVERITY, -1 /*defaultValue*/);
                        if (severity == IMarker.SEVERITY_ERROR) {
                            throw new AbortBuildException();
                        }
                    }
                }
            }
        } catch (CoreException e) {
            // don't stop, something's really screwed up and the build will break later with
            // a better error message.
        }
    }

    /**
     * Handles a {@link StreamException} by logging the info and marking the project.
     * This should generally be followed by exiting the build process.
     *
     * @param e the exception
     */
    protected void handleStreamException(StreamException e) {
        IAbstractFile file = e.getFile();

        String msg;

        IResource target = getProject();
        if (file instanceof IFileWrapper) {
            target = ((IFileWrapper) file).getIFile();

            if (e.getError() == StreamException.Error.OUTOFSYNC) {
                msg = "File is Out of sync";
            } else {
                msg = "Error reading file. Read log for details";
            }

        } else {
            if (e.getError() == StreamException.Error.OUTOFSYNC) {
                msg = String.format("Out of sync file: %s", file.getOsLocation());
            } else {
                msg = String.format("Error reading file %s. Read log for details",
                        file.getOsLocation());
            }
        }

        AndmoreAndroidPlugin.logAndPrintError(e, getProject().getName(), msg);
        BaseProjectHelper.markResource(target, AndmoreAndroidConstants.MARKER_ADT, msg,
                IMarker.SEVERITY_ERROR);
    }

    /**
     * Handles a generic {@link Throwable} by logging the info and marking the project.
     * This should generally be followed by exiting the build process.
     *
     * @param t the {@link Throwable}.
     * @param message the message to log and to associate with the marker.
     */
    protected void handleException(Throwable t, String message) {
        AndmoreAndroidPlugin.logAndPrintError(t, getProject().getName(), message);
        markProject(AndmoreAndroidConstants.MARKER_ADT, message, IMarker.SEVERITY_ERROR);
    }

    /**
     * Recursively delete all the derived resources from a root resource. The root resource is not
     * deleted.
     * @param rootResource the root resource
     * @param monitor a progress monitor.
     * @throws CoreException
     *
     */
    protected void removeDerivedResources(IResource rootResource, IProgressMonitor monitor)
            throws CoreException {
        removeDerivedResources(rootResource, false, monitor);
    }

    /**
     * delete a resource and its children. returns true if the root resource was deleted. All
     * sub-folders *will* be deleted if they were emptied (not if they started empty).
     * @param rootResource the root resource
     * @param deleteRoot whether to delete the root folder.
     * @param monitor a progress monitor.
     * @throws CoreException
     */
    private void removeDerivedResources(IResource rootResource, boolean deleteRoot,
            IProgressMonitor monitor) throws CoreException {
        if (rootResource.exists()) {
            // if it's a folder, delete derived member.
            if (rootResource.getType() == IResource.FOLDER) {
                IFolder folder = (IFolder)rootResource;
                IResource[] members = folder.members();
                boolean wasNotEmpty = members.length > 0;
                for (IResource member : members) {
                    removeDerivedResources(member, true /*deleteRoot*/, monitor);
                }

                // if the folder had content that is now all removed, delete the folder.
                if (deleteRoot && wasNotEmpty && folder.members().length == 0) {
                    rootResource.getLocation().toFile().delete();
                }
            }

            // if the root resource is derived, delete it.
            if (rootResource.isDerived()) {
                rootResource.getLocation().toFile().delete();
            }
        }
    }

    protected void launchJob(Job newJob) {
        newJob.setPriority(Job.BUILD);
        newJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
        newJob.schedule();
    }
}
