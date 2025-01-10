/*******************************************************************************
 * Copyright (c) 2010 Wind River Systems and others.
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Doug Schaefer (WRS) - Initial API and implementation
 * Carlos Alberto Souto Junior (Eldorado) - [315122] Improvements in the Android NDK support UI
 * Carlos Alberto Souto Junior (Eldorado) - [317327] Major UI bugfixes and improvements in Android Native support
 *******************************************************************************/

package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.sequoyah.android.cdt.build.core.INDKService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

/**
 * @author dschaefer
 *
 */
public class AddNativeProjectPage extends WizardPage
{
    private final String ANDROID_NATIVE_PERSPECTIVE_ID =
            "org.eclipse.sequoyah.android.cdt.build.ui.perspective";

    @Override
    public boolean canFlipToNextPage()
    {
        return false;
    }

    private String libName = ""; //$NON-NLS-1$

    private Link setNDKLocationLink;

    private ProjectChooser projectChooser;

    private Text location;

    private Text libraryText;

    boolean isNewProjectWizardPage = false;

    private String projectName;

    public AddNativeProjectPage(String projectName, boolean isNewProjectWizardPage)
    {
        this(isNewProjectWizardPage);
        this.projectName = projectName;
        this.libName = projectName;
    }

    public AddNativeProjectPage(boolean isNewProjectWizardPage)
    {
        super("native_page"); //$NON-NLS-1$
        setTitle(Messages.AddNativeProjectPage_native_page_title);
        setDescription(Messages.AddNativeProjectPage_native_page_description);
        this.isNewProjectWizardPage = isNewProjectWizardPage;
    }

    public void createControl(Composite parent)
    {
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        comp.setLayout(layout);
        comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        if (!isNewProjectWizardPage)
        {
            addProjectChooser(comp);
            projectChooser.setText(projectName == null ? "" : projectName);
        }
        addNDKLocation(comp);
        addLibraryName(comp);

        addListeners();

        setControl(comp);
    }

    ModifyListener listener = new ModifyListener()
    {
        public void modifyText(ModifyEvent e)
        {
            if (!isProjectValid())
            {
                setErrorMessage(Messages.AddNativeProjectPage_project_invalid_msg);
            }
            else if (!isNDKLocationValid())
            {
                setErrorMessage(Messages.AddNativeProjectPage_ndk_invalid_path_msg);
            }
            else if (!isLibraryNameValid())
            {
                setErrorMessage(Messages.AddNativeProjectPage_empty_library_name_msg);
            }
            else
            {
                setErrorMessage(null);
            }
            getWizard().getContainer().updateButtons();

        }
    };

    private void addListeners()
    {
        if (!isNewProjectWizardPage)
        {
            projectChooser.addModifyListener(listener);
        }
        location.addModifyListener(listener);
        libraryText.addModifyListener(listener);
    }

    private void addProjectChooser(Composite comp)
    {
        Group projectGroup = new Group(comp, SWT.NONE);
        projectGroup.setText(Messages.AddNativeProjectPage_project_group_text);
        projectGroup.setLayout(new GridLayout());
        projectGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        projectChooser = new ProjectChooser(projectGroup, SWT.FILL);
    }

    private void addNDKLocation(Composite parent)
    {
        Group group = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        group.setLayout(layout);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        group.setText(Messages.AddNativeProjectPage_ndk_location_group_text);

        location = new Text(group, SWT.BORDER);
        location.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        location.setEditable(false);

        if (!isNDKLocationValid())
        {
            setErrorMessage(Messages.AddNativeProjectPage_ndk_invalid_path_msg);
        }

        String path = PlatformUI.getPreferenceStore().getString(UIPlugin.NDK_LOCATION_PREFERENCE);
        if (path != null)
        {
            location.setText(path);
        }

        setNDKLocationLink = new Link(group, SWT.NONE);
        GridData gridData = new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1);
        setNDKLocationLink.setLayoutData(gridData);
        setNDKLocationLink.setText(Messages.AddNativeProjectPage_ndk_preference_link_text);
        setNDKLocationLink.setEnabled(true);
        setNDKLocationLink.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
                {
                    public void run()
                    {
                        PreferenceManager prefManager =
                                PlatformUI.getWorkbench().getPreferenceManager();
                        PreferenceDialog dialog =
                                new PreferenceDialog(PlatformUI.getWorkbench()
                                        .getActiveWorkbenchWindow().getShell(), prefManager);

                        dialog.setSelectedNode("org.eclipse.sequoyah.android.cdt.build.ui.NDKPreferencePage"); //$NON-NLS-1$

                        dialog.create();
                        dialog.setBlockOnOpen(true);
                        dialog.open();
                        //blocks until user clicks ok
                        String path =
                                PlatformUI.getPreferenceStore().getString(
                                        UIPlugin.NDK_LOCATION_PREFERENCE);
                        location.setText(path);
                        location.update();
                    }
                });
            }
        });
    }

    public String getNDKLocation()
    {
        return location.getText();
    }

    private void addLibraryName(Composite parent)
    {
        Group group = new Group(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        group.setLayout(layout);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        group.setText(Messages.AddNativeProjectPage_library_name_group_text);

        libraryText = new Text(group, SWT.BORDER);
        libraryText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        libraryText.setText(libName == null ? "" : libName);
    }

    public String getLibraryName()
    {
        return libraryText.getText();
    }

    //returns null. There is no next page
    @Override
    public IWizardPage getNextPage()
    {
        return null;
    }

    //true when valid ndk path is set and library name specified
    @Override
    public boolean isPageComplete()
    {
        boolean isComplete = false;

        if (isNDKLocationValid() && isLibraryNameValid() && isProjectValid())
        {
            isComplete = true;
        }

        return isComplete;
    }

    public boolean isProjectValid()
    {
        boolean isValid = true;

        if (projectChooser != null)
        {
            // get the project Name
            String projectName = projectChooser.getText();

            // validate project name
            IStatus status = getProjectStatus(projectName);
            // add status to the list and check for error status, in case there is any
            if (status != null)
            {
                if (status.getSeverity() == IStatus.ERROR)
                {
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    private IStatus getProjectStatus(String projectName)
    {
        IStatus status = null;

        if ((projectName == null) || (projectName.length() == 0))
        {
            // there must be a selected project
            status = new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, "");
        }
        else
        {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            if (!project.exists())
            {
                status = new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, "");
            }
        }

        return status;
    }

    public boolean isNDKLocationValid()
    {
        boolean isValid = true;
        String path = PlatformUI.getPreferenceStore().getString(UIPlugin.NDK_LOCATION_PREFERENCE);

        if ((path.length() == 0) || !NDKPreferencePage.validateNDKDirectory(path))
        {
            isValid = false;
        }
        return isValid;
    }

    public boolean isLibraryNameValid()
    {
        boolean isValid = false;
        String libName = libraryText.getText();
        if (libName.trim().length() > 0)
        {
            isValid = true;
        }
        return isValid;
    }

    public void initializeLibraryField(String libName)
    {
        libraryText.setText(libName);
        libraryText.update();
    }

    public boolean performFinish(IWorkbenchWindow window, IProject project, IProgressMonitor monitor)
    {
        if (project == null)
        {
            project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectChooser.getText());
        }

        boolean success = true;
        try
        {
            // get monitor
            SubMonitor subMonitor = SubMonitor.convert(monitor, 10);

            subMonitor.beginTask(Messages.AddNativeProjectPage__Message_AddingNativeSupport, 10);

            // Switch to the C perspective

            // Grab the data from the pages
            final String libraryName = getLibraryName();

            subMonitor.worked(2);

            // Save the NDK location
            INDKService ndkService = UIPlugin.getService(INDKService.class);
            subMonitor.worked(1);
            ndkService.setNDKLocation(getNDKLocation());
            subMonitor.worked(6);

            if (libraryName.length() != 0)
            {
                // Add the native support
                ndkService.addNativeSupport(project, libraryName);
                if (!window.getActivePage().getPerspective().getId()
                        .equals(ANDROID_NATIVE_PERSPECTIVE_ID))
                {
                    if (MessageDialog.openQuestion(getShell(),
                            Messages.AddNativeProjectPage_ChangePerspectiveDialogTitle,
                            Messages.AddNativeProjectPage_ChangePerspectiveDialogQuestion))
                    {
                        window.getWorkbench().showPerspective(ANDROID_NATIVE_PERSPECTIVE_ID,
                                PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                    }
                }
            }
            try
            {
                project.setPersistentProperty(INDKService.libName, libraryName);
                IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
                IConfiguration configs[] = buildInfo.getManagedProject().getConfigurations();

                            
                if (Platform.getOS().equals(Platform.OS_WIN32))
                {
                    configs[0].setBuildCommand("bash");
                    configs[0].setBuildArguments(getNDKLocation() + "\\ndk-build");
                }
                subMonitor.worked(1);

            }
            catch (CoreException e)
            {
                UIPlugin.getDefault().getLog().log(e.getStatus());
                success = false;
            }
        }
        catch (WorkbenchException e)
        {
            UIPlugin.getDefault().getLog().log(e.getStatus());
            success = false;
        }
        return success;
    }
}
