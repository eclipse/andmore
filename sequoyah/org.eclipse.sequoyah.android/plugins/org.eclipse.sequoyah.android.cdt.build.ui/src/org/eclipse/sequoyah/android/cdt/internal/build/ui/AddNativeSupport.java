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
 * Carlos Alberto Souto Junior (Eldorado) - [317327] Major UI bugfixes and improvements in Android Native support
 * Marcelo Marzola Bossoni - [318712] Make run wizard an static method and make the add native support calls this method
 *******************************************************************************/
package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * 
 */
public class AddNativeSupport implements IObjectActionDelegate {

	private IWorkbenchPart targetPart;

	private IProject project;

	public void run(IAction action) {
		runWizard(project, targetPart);
	}

	public static void runWizard(IProject project, IWorkbenchPart part) {
		// check windows project location restrictions (cygwin does not work for
		// project with
		// whitespaces on its path)
		if ((project != null)
				&& project.getLocation().toOSString().contains(" ")) {
			MessageUtils
					.showErrorDialog(
							Messages.AddNativeProjectAction_InvalidProjectLocation_Title,
							Messages.bind(
									Messages.AddNativeProjectAction_InvalidProjectLocation_Message,
									project.getLocation().toOSString()));
		} else {
			// Run the wizard
			AddNativeWizard wizard = new AddNativeWizard(part.getSite()
					.getWorkbenchWindow(), project);
			WizardDialog dialog = new WizardDialog(part.getSite().getShell(),
					wizard);
			dialog.open();
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object selected = ((IStructuredSelection) selection)
					.getFirstElement();
			if (selected instanceof IProject) {
				project = (IProject) selected;
			} else if (selected instanceof PlatformObject) {
				project = (IProject) ((PlatformObject) selected)
						.getAdapter(IProject.class);
			} else {
				project = null;
			}
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

}
