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
 * Thiago Faustini Junqueira (Eldorado) - [314314] NDK - Re-generate Makefile when project property value is changed
 * Carlos Alberto Souto Junior (Eldorado) - [315122] Improvements in the Android NDK support UI
 * Carlos Alberto Souto Junior (Eldorado) - [317327] Major UI bugfixes and improvements in Android Native support
 *******************************************************************************/
package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * @author dschaefer
 *
 */
public class AddNativeWizard extends Wizard
{
	private final IWorkbenchWindow window;
	private final IProject project; 
	
	private AddNativeProjectPage projectPage;
	
    public AddNativeWizard(IWorkbenchWindow window, IProject project)
    {
        this.window = window;
        this.project = project;

        setWindowTitle(Messages.AddNativeWizard_native_wizard_title);
        setNeedsProgressMonitor(true);
        setDialogSettings(UIPlugin.getDefault().getDialogSettings());
        ImageDescriptor img = new ImageDescriptor()
        {
            
            @Override
            public ImageData getImageData()
            {
                ImageData data = new ImageData(getClass().getResourceAsStream("/icons/android_native_64x64.png"));
                return data;
            }
        };
        setDefaultPageImageDescriptor(img);
    }

    @Override
    public void addPages()
    {
        projectPage = new AddNativeProjectPage(project == null ? null : project.getName(), false);
        addPage(projectPage);
    }

    @Override
    public boolean canFinish()
    {
        return projectPage.isPageComplete();
    }

    @Override
    public boolean performFinish()
    {
        // variable for perform finish
        final boolean[] isOKPerformFinish = new boolean[1];

        // execute finish processes with progress monitor
        try
        {
            getContainer().run(false, false, new IRunnableWithProgress()
            {

                public void run(final IProgressMonitor monitor) throws InvocationTargetException,
                        InterruptedException
                {
                    // get monitor
                    final SubMonitor subMonitor = SubMonitor.convert(monitor, 10);
                    // finish up
                    isOKPerformFinish[0] =
                            projectPage.performFinish(window, project, subMonitor.newChild(10));
                }
            });
        }
        catch (Exception ex)
        {
            // treat error - log, show the error message and set the flag for performing finish
            UIPlugin.getDefault().getLog()
                    .log(new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, ex.getMessage(), ex));
            MessageDialog.openError(getShell(), Messages.AddNativeWizard_native_wizard_title,
                    Messages.AddNativeWizard__Message_UnexpectedErrorWhileAddingNativeSupport);
            isOKPerformFinish[0] = false;
        }

        // return performing finish status
        return isOKPerformFinish[0];
    }
}
