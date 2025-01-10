/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Vinicius Hernandes (Motorola)
 * Matheus Tait Lima (Eldorado)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) - Bug [289146] - Performance and Usability Issues
 * Marcelo Marzola Bossoni (Eldorado) - Bug (289236) - Editor Permitting create 2 columns with same id
 * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 * Daniel Barboza Franco (Eldorado) - Bug [290058] - fixing NullPointerException's while listening changes made from outside Eclipse
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.localization.tools.LocalizationToolsPlugin;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationProject;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ILocalizationSchema;
import org.eclipse.sequoyah.localization.tools.extensions.providers.LocalizationSchemaProvider;
import org.eclipse.ui.ide.IDE;

/**
 * This is the main manager of the Localization Framework
 * 
 * It's intended to be able to provide information like:
 * 
 * - The defined Localization Schemas for each project nature - The Project
 * Localization Managers, since each Project has its own manager
 * 
 */
public class LocalizationManager
{

    /**
     * Listeners that aim be notified when the content of a localization file
     * changes must implement this interface and register themselves by calling
     * addFileChangeListener
     */
    public interface IFileChangeListener
    {

        public void fileChanged(IFile file);

        public IProject getProject();

    }

    /*
     * The Localization Schemas, indexed by Project Nature Name
     */
    private final Map<String, ILocalizationSchema> localizationSchemas;

    /*
     * The list of all natures supported, ordered by precedence
     */
    private final List<String> natures;

    /*
     * The Managers for each Project
     */
    private final Map<IProject, ProjectLocalizationManager> projectLocalizationManagers =
            new HashMap<IProject, ProjectLocalizationManager>();

    /*
     * A reference to the Localization Schema Provider
     */
    private final LocalizationSchemaProvider localizationSchemaProvider;

    /*
     * Singleton instance
     */
    private static LocalizationManager instance = null;

    private final Map<IProject, IFileChangeListener> fileChangeListeners =
            new HashMap<IProject, IFileChangeListener>();

    class LocalizationDeltaVisitor implements IResourceDeltaVisitor
    {
        public boolean visit(IResourceDelta delta)
        {
            IResource resource = delta.getResource();
            if (resource != null)
            {
                IProject project = resource.getProject();
                if (project != null)
                {
                    ILocalizationSchema localizationSchema = getLocalizationSchema(project);
                    if (localizationSchema != null)
                    {
                        if ((resource instanceof IFile)
                                && (localizationSchema.isLocalizationFile((IFile) resource)))
                        {
                            if (delta.getKind() == IResourceDelta.ADDED)
                            {
                                handleFileAddition((IFile) resource);
                            }
                            else if (delta.getKind() == IResourceDelta.CHANGED)
                            {
                                handleFileChange((IFile) resource, delta);
                            }
                            else if (delta.getKind() == IResourceDelta.REMOVED)
                            {
                                handleFileDeletion((IFile) resource);
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    private final IResourceChangeListener resourceChangelistener = new IResourceChangeListener()
    {
        public void resourceChanged(IResourceChangeEvent event)
        {
            IResourceDelta delta = event.getDelta();
            LocalizationDeltaVisitor localizationDeltaVisitor = new LocalizationDeltaVisitor();
            try
            {
                if (delta != null)
                {
                    delta.accept(localizationDeltaVisitor);
                }
            }
            catch (CoreException e)
            {
                BasePlugin.logError("Could not handle changes in localization file"); //$NON-NLS-1$
            }

        }
    };

    /**
     * Singleton
     * 
     * @return LocalizationManager
     */
    public static LocalizationManager getInstance()
    {
        if (instance == null)
        {
            instance = new LocalizationManager();
        }
        return instance;
    }

    /**
     * Constructor
     * 
     * It's responsible for: - setting the Localization Schema Provider -
     * getting the Localization Schemas available - adding a workspace listener
     * to handle localization files deletions
     * 
     */
    public LocalizationManager()
    {
        localizationSchemaProvider = LocalizationSchemaProvider.getInstance();
        localizationSchemas = localizationSchemaProvider.getLocalizationSchemas();
        natures = getNaturesInOrder();
    }

    /**
     * Get the Localization Manager for a specific project
     * 
     * @param project
     *            the project that will be handled by the manager
     * @return the manager of the localization for the project passed as a
     *         parameter
     * @throws SequoyahException
     */
    public ProjectLocalizationManager getProjectLocalizationManager(IProject project, boolean force)
            throws IOException, SequoyahException
    {

        ProjectLocalizationManager projectManager = projectLocalizationManagers.get(project);
        if (projectManager == null)
        {
            ILocalizationSchema schema = getLocalizationSchema(project);
            if (schema != null)
            {
                projectManager = new ProjectLocalizationManager(project, schema);
                projectLocalizationManagers.put(project, projectManager);
            }
        }
        if ((projectManager != null) && (projectManager.getLocalizationProject() == null))
        {
            ILocalizationSchema schema = getLocalizationSchema(project);
            if (schema != null)
            {
                projectManager.setProject(project);
                List<LocalizationFile> locFiles = new ArrayList<LocalizationFile>();
                locFiles.addAll(schema.loadAllFiles(project).values());
                projectManager.setLocalizationProject(new LocalizationProject(project, locFiles));

            }
        }
        else if (force)
        {
            projectManager.reload(project);
        }

        return projectManager;
    }

    /**
     * Unload the Localization Manager for a specific project It can happen when
     * the model is not used anymore
     * 
     * @param project
     *            the project that was handled by the manager
     */
    public void unloadProjectLocalizationManager(IProject project)
    {

        projectLocalizationManagers.remove(project);

    }

    /**
     * Get the Localization Schema for a specific project, based on its nature
     * 
     * @return the localization schema for the project passed as a parameter
     */
    public ILocalizationSchema getLocalizationSchema(IProject project)
    {
        ILocalizationSchema localizationSchemaForProject = null;

        try
        {
            for (String nature : natures)
            {
                if (projectHasNature(project, nature))
                {
                    localizationSchemaForProject = localizationSchemas.get(nature);
                }

            }
        }
        catch (CoreException e)
        {
            BasePlugin.logError(this.getClass().getName() + ": Error getting Localization Schema"); //$NON-NLS-1$
        }

        return localizationSchemaForProject;
    }

    /**
     * Check whether the nature is part of the project description No matter if
     * it has been added to the described project or not
     * 
     * @param project
     *            the project
     * @param projectNature
     *            the nature to be tested
     * @return true if the nature is part of the project description, false
     *         otherwise
     * @throws CoreException
     */
    private boolean projectHasNature(IProject project, String projectNature) throws CoreException
    {
        boolean projectHasNature = false;

        String[] natureIds = project.getDescription().getNatureIds();
        for (String natureId : natureIds)
        {
            if (natureId.equals(projectNature))
            {
                projectHasNature = true;
                break;
            }
        }

        return projectHasNature;
    }

    /**
     * Get the Localization Schema for a specific nature
     * 
     * @param nature
     *            the project nature
     * @return the localization schema for the nature passed as a parameter
     */
    public ILocalizationSchema getLocalizationSchema(String nature)
    {
        return localizationSchemas.get(nature);
    }

    /**
     * Add a workspace listener which will be responsible for recognizing when a
     * localization file is deleted
     */
    private void addWorkspaceListener()
    {

        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        workspace.removeResourceChangeListener(resourceChangelistener);
        workspace.addResourceChangeListener(resourceChangelistener,
                IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE);

    }

    /**
     * Do the actions needed when a localization file is deleted
     * 
     * @param file
     *            the file deleted
     */
    private void handleFileDeletion(IFile file)
    {
        ProjectLocalizationManager projectLocalizationManager = null;
        try
        {
            try
            {
                projectLocalizationManager =
                        getProjectLocalizationManager(file.getProject(), false);
            }
            catch (SequoyahException e)
            {

            }
        }
        catch (IOException e)
        {

        }
        if (projectLocalizationManager != null)
        {
            projectLocalizationManager.deleteFileMetaExtraData(file);
        }
    }

    /**
     * Set the localization editor as the default Notify listeners if the file
     * content has changed
     * 
     * @param file
     *            the file modified
     * @param localizationSchema
     *            the localization schema / editor to be used
     */
    private void handleFileChange(IFile file, IResourceDelta delta)
    {
        IDE.setDefaultEditor(file, LocalizationToolsPlugin.EDITOR_ID);

        ProjectLocalizationManager projectLocalizationManager = null;
        try
        {
            try
            {
                projectLocalizationManager =
                        LocalizationManager.getInstance().getProjectLocalizationManager(
                                file.getProject(), false);
            }
            catch (SequoyahException e)
            {

            }
        }
        catch (IOException e)
        {

        }

        if (projectLocalizationManager != null)
        {
            LocalizationFile locFile =
                    projectLocalizationManager.getLocalizationProject().getLocalizationFile(file);
            if ((locFile != null) && (!locFile.isToBeDeleted())
                    && (hasFileChanged(file, locFile, projectLocalizationManager)))
            {
                notifyInputChange(file);
            }
        }

    }

    /**
     * Check if a localization file has changed by comparing its old and new
     * content
     * 
     * @param file
     *            the file modified
     * @param locFile
     *            localization file that represents the file
     * @param projectLocalizationManager
     *            the project localization manager
     * @return true if the file has changed, false otherwise
     */
    private boolean hasFileChanged(IFile file, LocalizationFile locFile,
            ProjectLocalizationManager projectLocalizationManager)
    {
        boolean result = false;

        LocalizationFile newLocalizationFile;
        try
        {
            String type = locFile.getClass().getName(); // type =
                                                        // <Type>LocalizationFile.class
            // type.substring(0, type.length()-22);
            newLocalizationFile =
                    projectLocalizationManager.getProjectLocalizationSchema().loadFile(type, file);

            if (!locFile.equals(newLocalizationFile))
            {
                result = true;
            }
        }
        catch (SequoyahException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Set the localization editor as the default
     * 
     * @param file
     *            the file modified
     */
    private void handleFileAddition(IFile file)
    {
        try
        {
            //set LFE as default editor when a new strings.xml file is added
            IDE.setDefaultEditor(file, LocalizationToolsPlugin.EDITOR_ID);

            //also persist this change until the user explicitly open the file with another editor using "open with"
            file.setPersistentProperty(IDE.EDITOR_KEY, LocalizationToolsPlugin.EDITOR_ID);
        }
        catch (CoreException e)
        {
            //do nothing...
        }
    }

    private List<String> getNaturesInOrder()
    {
        List<String> natures = new ArrayList<String>();
        for (ILocalizationSchema schema : localizationSchemas.values())
        {
            natures.add(schema.getNatureName());
        }
        return natures;
    }

    /**
     * Configure the editors to open the localization files
     */
    public void initialize()
    {
        /*
         * Workspace listener to configure the editor in further situations
         */
        addWorkspaceListener();
    }

    /**
     * Retrieve the projects that have a localization schema attached
     * 
     * @return the projects that are supported
     */
    public List<IProject> getSupportedProjects()
    {
        List<IProject> supportedProjects = new ArrayList<IProject>();
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects)
        {
            if (getLocalizationSchema(project) != null)
            {
                supportedProjects.add(project);
            }
        }
        return supportedProjects;
    }

    /**
     * Add a listener that will be notified when the content of a localization
     * file changes
     * 
     * @param fileChangeListener
     *            the IFileChangeListener listener reference
     */
    public void addFileChangeListener(IFileChangeListener fileChangeListener)
    {
        fileChangeListeners.put(fileChangeListener.getProject(), fileChangeListener);
    }

    /**
     * Remove a listener from that list of listeners which are notified when the
     * content of a localization file changes
     * 
     * @param fileChangeListener
     *            the IFileChangeListener listener reference
     */
    public void removeFileChangeListener(IFileChangeListener fileChangeListener)
    {
        fileChangeListeners.remove(fileChangeListener.getProject());
    }

    /**
     * Notify the registered listeners that there was a change in a localization
     * file
     * 
     * @param file
     */
    public void notifyInputChange(IFile file)
    {

        IFileChangeListener fileChangeListener = fileChangeListeners.get(file.getProject());
        if (fileChangeListener != null)
        {
            fileChangeListener.fileChanged(file);
        }
    }

}
